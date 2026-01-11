package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rssfetcher.demo.entities.MonitoringJob;
import rssfetcher.demo.repositories.MonitoringJobRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class WorkerService {

    private final MonitoringJobRepository jobRepository;
    private final MonitorService monitorService;
    private final TransactionTemplate tx;
    private final DataSource dataSource;

    private final String workerId = getWorkerId() + "-" + UUID.randomUUID();

    // Retry policy
    private static final int MAX_ATTEMPTS = 5;

    // Idle timeout before going into LISTEN mode
    private static final long IDLE_CHECK_MS = 1000;
    private static final int MAX_IDLE_CHECKS = 3;

    // Shutdown management
    private volatile boolean running = false;
    private Thread workerThread;
    private Connection listenConnection;

    public WorkerService(MonitoringJobRepository jobRepository,
                         MonitorService monitorService,
                         PlatformTransactionManager transactionManager,
                         DataSource dataSource) {
        this.jobRepository = jobRepository;
        this.monitorService = monitorService;
        this.tx = new TransactionTemplate(transactionManager);
        this.dataSource = dataSource;
    }

    /**
     * Start worker in separate thread so Spring can finish starting up
     */
    @PostConstruct
    public synchronized void start() {
        if (running) {
            log.warn("Worker already running");
            return;
        }

        running = true;
        workerThread = new Thread(this::run, "rss-worker-" + workerId);
        workerThread.start();
        log.info("Worker started: {}", workerId);
    }

    /**
     * Main worker loop: active processing -> LISTEN mode -> wake on notification
     */
    private void run() {
        int consecutiveFailures = 0;

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Phase 1: Process all available jobs
                boolean foundWork = processAvailableJobs();
                consecutiveFailures = 0;

                // Phase 2: If no work, enter LISTEN mode
                if (!foundWork) {
                    log.info("No pending jobs, entering LISTEN mode");
                    waitForNotification();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker interrupted, shutting down");
                break;
            } catch (Exception e) {
                consecutiveFailures++;
                log.error("Unexpected error in worker loop (failure {})", consecutiveFailures, e);

                if (consecutiveFailures > 3) {
                    try {
                        sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("Worker interrupted during error backoff, shutting down");
                        break;
                    }
                }
            }
        }

        log.info("Worker stopped: {}", workerId);
    }

    /**
     * Process all available jobs until queue is empty.
     * Returns true if any work was found.
     */
    private boolean processAvailableJobs() throws InterruptedException {
        boolean foundAnyWork = false;
        int idleChecks = 0;

        while (running && !Thread.currentThread().isInterrupted()) {
            Optional<MonitoringJob> jobOpt = claimNextJob();

            if (jobOpt.isEmpty()) {
                idleChecks++;
                if (idleChecks >= MAX_IDLE_CHECKS) {
                    break; // Queue is empty
                }
                sleep(IDLE_CHECK_MS);
                continue;
            }

            foundAnyWork = true;
            idleChecks = 0;
            processJob(jobOpt.get());
        }

        return foundAnyWork;
    }

    /**
     * Wait for PostgreSQL notification that new jobs are available
     */
    private void waitForNotification() throws InterruptedException {
        try {
            // Establish LISTEN connection if needed
            if (listenConnection == null || listenConnection.isClosed()) {
                setupListenConnection();
            }

            PGConnection pgConn = listenConnection.unwrap(PGConnection.class);

            log.debug("Waiting for job notifications...");

            // Block until notification arrives (with periodic checks for shutdown)
            while (running && !Thread.currentThread().isInterrupted()) {
                PGNotification[] notifications = pgConn.getNotifications(5000); // 5s timeout

                if (notifications != null && notifications.length > 0) {
                    log.info("Received {} job notification(s), resuming work", notifications.length);
                    break;
                }
            }

        } catch (SQLException e) {
            log.error("Error in LISTEN mode, will retry", e);
            closeListenConnection();
            sleep(5000);
        }
    }

    /**
     * Set up PostgreSQL LISTEN connection
     */
    private void setupListenConnection() throws SQLException {
        listenConnection = dataSource.getConnection();
        listenConnection.setAutoCommit(true);

        try (Statement stmt = listenConnection.createStatement()) {
            stmt.execute("LISTEN new_monitoring_jobs");
            log.info("Listening for job notifications on channel 'new_monitoring_jobs'");
        }
    }

    /**
     * Process a single job with error handling
     */
    private void processJob(MonitoringJob job) {
        Long jobId = job.getId();

        log.info("Processing job {} (attempt {}/{}): {}",
                jobId, job.getAttempts(), MAX_ATTEMPTS, job.getRssUrl());

        try {
            // Do the work - if this hangs, the worker hangs
            monitorService.processRssUrl(job.getRssUrl());

            // Mark as done
            tx.executeWithoutResult(s -> {
                jobRepository.markDone(jobId);
                log.info("Job {} completed successfully", jobId);
            });

        } catch (Exception e) {
            handleJobFailure(job, e.toString());
        }
    }

    /**
     * Handle job failure with retry or final failure marking
     */
    private void handleJobFailure(MonitoringJob job, String errorMessage) {
        Long jobId = job.getId();
        int attempts = job.getAttempts();
        String truncatedError = truncate(errorMessage, 2000);

        if (attempts < MAX_ATTEMPTS) {
            tx.executeWithoutResult(s -> {
                jobRepository.requeue(jobId, truncatedError);
                log.warn("Job {} failed (attempt {}/{}), requeued: {}",
                        jobId, attempts, MAX_ATTEMPTS, truncatedError);
            });
        } else {
            tx.executeWithoutResult(s -> {
                jobRepository.markFailed(jobId, truncatedError);
                log.error("Job {} permanently failed after {} attempts: {}",
                        jobId, MAX_ATTEMPTS, truncatedError);
            });
        }
    }

    /**
     * Claim next available job in a short transaction
     */
    private Optional<MonitoringJob> claimNextJob() {
        try {
            return tx.execute(status -> jobRepository.claimNextJob(workerId));
        } catch (Exception e) {
            log.error("Failed to claim job", e);
            return Optional.empty();
        }
    }

    /**
     * Sleep with proper interrupt handling
     */
    private void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    /**
     * Graceful shutdown
     */
    @PreDestroy
    public synchronized void shutdown() {
        if (!running) {
            return;
        }

        log.info("Shutting down worker: {}", workerId);
        running = false;

        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(10000);
                if (workerThread.isAlive()) {
                    log.warn("Worker did not stop gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        closeListenConnection();

        log.info("Worker shutdown complete: {}", workerId);
    }

    private void closeListenConnection() {
        if (listenConnection != null) {
            try {
                listenConnection.close();
            } catch (SQLException e) {
                log.warn("Error closing LISTEN connection", e);
            }
            listenConnection = null;
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String getWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}