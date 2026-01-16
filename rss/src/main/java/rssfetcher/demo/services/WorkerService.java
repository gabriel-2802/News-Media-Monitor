package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rssfetcher.demo.entities.NewsSource;
import rssfetcher.demo.repositories.FetchCycleStateRepository;
import rssfetcher.demo.repositories.NewsSourceRepository;

import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class WorkerService {

    private final NewsSourceRepository newsSourceRepository;
    private final FetchCycleStateRepository fetchCycleStateRepository;
    private final MonitorService monitorService;
    private final ClusterService clusterService;
    private final TransactionTemplate tx;
    private final String workerId = generateWorkerId();

    // retry policy for individual sources
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    public WorkerService(NewsSourceRepository newsSourceRepository,
                         FetchCycleStateRepository fetchCycleStateRepository,
                         MonitorService monitorService,
                         ClusterService clusterService,
                         PlatformTransactionManager transactionManager) {
        this.newsSourceRepository = newsSourceRepository;
        this.fetchCycleStateRepository = fetchCycleStateRepository;
        this.monitorService = monitorService;
        this.clusterService = clusterService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * Scheduled job that runs on startup and then every X hours.
     * Each worker will claim and process news sources one by one until no more are available.
     * The FOR UPDATE SKIP LOCKED ensures no two workers process the same source.
     * When the last source is processed, exactly one worker triggers clustering.
     */
    @Scheduled(fixedDelayString = "${worker.fetch-interval-ms:10800000}")
    public void scheduledFetch() {
        log.info("=== Starting scheduled fetch cycle, worker: {} ===", workerId);
        int processedCount = 0;
        int failedCount = 0;

        try {
            while (true) {
                Optional<NewsSource> sourceOpt = claimNextSource();

                if (sourceOpt.isEmpty()) {
                    log.info("No more sources to process in this cycle");
                    // Try to trigger clustering (only one worker will succeed)
                    tryTriggerClustering();
                    break;
                }

                NewsSource source = sourceOpt.get();
                boolean success = processSource(source);

                if (success) {
                    processedCount++;
                } else {
                    failedCount++;
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in fetch cycle", e);
        } finally {
            log.info("=== Fetch cycle completed: {} processed, {} failed ===", processedCount, failedCount);
        }
    }

    /**
     * Attempts to claim the right to trigger clustering.
     * Only one worker across all instances will succeed due to atomic update.
     */
    private void tryTriggerClustering() {
        try {
            Boolean claimed = tx.execute(status -> {
                // Check if there are still unfetched sources (maybe another worker is processing)
                long unfetched = newsSourceRepository.countUnfetchedSources();
                if (unfetched > 0) {
                    log.debug("Still {} unfetched sources, not triggering clustering yet", unfetched);
                    return false;
                }

                // Try to atomically claim the clustering trigger
                int updated = fetchCycleStateRepository.claimClusteringTrigger(workerId, Instant.now());
                return updated > 0;
            });

            if (Boolean.TRUE.equals(claimed)) {
                log.info("This worker claimed clustering trigger, starting clustering...");
                clusterService.cluster();

                // Reset all sources for the next cycle after clustering completes
                tx.executeWithoutResult(status -> {
                    int reset = newsSourceRepository.resetAllForNewCycle();
                    fetchCycleStateRepository.startNewCycle(Instant.now());
                    log.info("Reset {} sources for next fetch cycle", reset);
                });
            } else {
                log.debug("Another worker will trigger clustering (or already did)");
            }
        } catch (Exception e) {
            log.error("Error while trying to trigger clustering", e);
        }
    }

    /**
     * Claim and lock the next available news source for processing.
     * Uses FOR UPDATE SKIP LOCKED to ensure no two workers claim the same source.
     */
    private Optional<NewsSource> claimNextSource() {
        try {
            return tx.execute(status -> {
                Optional<NewsSource> sourceOpt = newsSourceRepository.findNextAvailable();
                if (sourceOpt.isPresent()) {
                    NewsSource source = sourceOpt.get();
                    newsSourceRepository.lockSource(source.getId(), workerId, Instant.now());
                    log.debug("Claimed source: {} ({})", source.getName(), source.getRssUrl());
                }
                return sourceOpt;
            });
        } catch (Exception e) {
            log.error("Failed to claim source", e);
            return Optional.empty();
        }
    }

    /**
     * Process a single news source - fetch RSS and save articles.
     * Returns true on success, false on failure.
     */
    private boolean processSource(NewsSource source) {
        Long sourceId = source.getId();
        String sourceName = source.getName();
        String rssUrl = source.getRssUrl();

        log.info("Processing source: {} ({})", sourceName, rssUrl);

        try {
            // actual RSS fetching and article processing
            monitorService.processRssUrl(rssUrl);

            // Mark as successful
            Instant now = Instant.now();
            tx.executeWithoutResult(s -> {
                newsSourceRepository.markSuccess(sourceId, now);
                log.info("Source {} completed successfully", sourceName);
            });

            return true;

        } catch (Exception e) {
            handleSourceFailure(source, e.getMessage());
            return false;
        }
    }

    /**
     * Handle source processing failure with tracking
     */
    private void handleSourceFailure(NewsSource source, String errorMessage) {
        Long sourceId = source.getId();
        String sourceName = source.getName();
        int failures = (source.getConsecutiveFailures() != null ? source.getConsecutiveFailures() : 0) + 1;
        String truncatedError = truncate(errorMessage, 2000);

        tx.executeWithoutResult(s -> {
            newsSourceRepository.markFailed(sourceId, truncatedError);

            if (failures >= MAX_CONSECUTIVE_FAILURES) {
                log.error("Source {} has {} consecutive failures: {}",
                        sourceName, failures, truncatedError);
            } else {
                log.warn("Source {} failed (failure {}/{}): {}",
                        sourceName, failures, MAX_CONSECUTIVE_FAILURES, truncatedError);
            }
        });
    }

    /**
     * Release all locks held by this worker on graceful shutdown
     */
    @PreDestroy
    public void releaseLocksOnShutdown() {
        try {
            tx.executeWithoutResult(s -> {
                int released = newsSourceRepository.releaseAllLocks(workerId);
                if (released > 0) {
                    log.info("Released {} source lock(s) on shutdown", released);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to release locks on shutdown", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String generateWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            return "worker-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}