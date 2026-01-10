package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rssfetcher.demo.entities.MonitoringJob;
import rssfetcher.demo.repositories.MonitoringJobRepository;

import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
public class WorkerService {

    private final MonitoringJobRepository jobRepository;
    private final MonitorService monitorService;
    private final TransactionTemplate tx;
    private final ExecutorService timeoutExecutor;

    private final String workerId = getWorkerId() + "-" + UUID.randomUUID();

    // backoff tuning
    private static final long MIN_BACKOFF_MS = 25;
    private static final long MAX_BACKOFF_MS = 2000;
    private static final double BACKOFF_JITTER = 0.1;

    // retry policy
    private static final int MAX_ATTEMPTS = 5;

    // processing timeout
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    // shutdown management
    private volatile boolean running = false;
    private Thread workerThread;

    public WorkerService(MonitoringJobRepository jobRepository,
                         MonitorService monitorService,
                         PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.monitorService = monitorService;
        this.tx = new TransactionTemplate(transactionManager);
        this.timeoutExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rss-worker-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the worker loop. Should be called once from @PostConstruct or manually.
     */
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
     * Main worker loop with graceful shutdown support
     */
    private void run() {
        long backoff = MIN_BACKOFF_MS;
        int consecutiveFailures = 0;

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // single query returns the full job with updated attempts
                Optional<MonitoringJob> jobOpt = claimNextJob();

                if (jobOpt.isEmpty()) {
                    sleep(backoff);
                    backoff = calculateBackoff(backoff);
                    continue;
                }

                // reset backoff when work is found
                backoff = MIN_BACKOFF_MS;
                consecutiveFailures = 0;

                processJob(jobOpt.get());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker interrupted, shutting down");
                break;
            } catch (Exception e) {
                consecutiveFailures++;
                log.error("Unexpected error in worker loop (failure {})", consecutiveFailures, e);

                // emergency backoff if repeatedly failing
                if (consecutiveFailures > 3) {
                    try {
                        sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.info("Worker interrupted during emergency backoff, shutting down");
                        break;
                    }
                }
            }
        }

        log.info("Worker stopped: {}", workerId);
    }

    /**
     * Process a single job with timeout and proper error handling
     */
    private void processJob(MonitoringJob job) throws InterruptedException {
        Long jobId = job.getId();

        log.info("Processing job {} (attempt {}/{}): {}",
                jobId, job.getAttempts(), MAX_ATTEMPTS, job.getRssUrl());

        try {
            // Execute work with timeout protection
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> monitorService.processRssUrl(job.getRssUrl()),
                    timeoutExecutor
            );

            future.get(PROCESSING_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            // Mark as done in a short transaction
            tx.executeWithoutResult(s -> {
                jobRepository.markDone(jobId);
                log.info("Job {} completed successfully", jobId);
            });

        } catch (TimeoutException e) {
            handleJobFailure(job, "Processing timeout after " + PROCESSING_TIMEOUT);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            handleJobFailure(job, cause.toString());

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
     * Claim next available job in a short transaction.
     * Returns the full job entity with accurate attempts counter.
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
     * Calculate backoff with exponential increase and jitter
     */
    private long calculateBackoff(long currentBackoff) {
        long doubled = Math.min(MAX_BACKOFF_MS, currentBackoff * 2);
        long jitter = (long) (Math.random() * doubled * BACKOFF_JITTER);
        return doubled + jitter;
    }

    /**
     * Sleep with proper interrupt handling
     */
    private void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    /**
     * Graceful shutdown with timeout
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
                workerThread.join(10000); // Wait up to 10s
                if (workerThread.isAlive()) {
                    log.warn("Worker did not stop gracefully");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Worker shutdown complete: {}", workerId);
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