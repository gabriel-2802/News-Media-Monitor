package rssfetcher.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rssfetcher.demo.entities.NewsSource;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {
    Optional<NewsSource> findByRssUrl(String rssUrl);

    /**
     * Claims the next available news source for processing.
     * Uses FOR UPDATE SKIP LOCKED to prevent multiple workers from claiming the same source.
     * Only claims sources that are:
     * - enabled
     * - not yet fetched this cycle (fetched_this_cycle = false)
     * - not currently locked by another worker (or lock expired)
     */
    @Query(value = """
        SELECT * FROM news_sources
        WHERE enabled = true
          AND fetched_this_cycle = false
          AND (locked_by IS NULL OR locked_at < NOW() - INTERVAL '5 minutes')
        ORDER BY last_fetched_at NULLS FIRST, id
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<NewsSource> findNextAvailable();

    /**
     * Locks a news source for processing by a specific worker
     */
    @Modifying
    @Query("UPDATE NewsSource ns SET ns.lockedBy = :workerId, ns.lockedAt = :now WHERE ns.id = :id")
    int lockSource(@Param("id") Long id, @Param("workerId") String workerId, @Param("now") Instant now);

    /**
     * Marks a source as successfully processed in this cycle
     */
    @Modifying
    @Query("""
        UPDATE NewsSource ns SET
            ns.lockedBy = NULL,
            ns.lockedAt = NULL,
            ns.lastFetchedAt = :now,
            ns.fetchedThisCycle = true,
            ns.consecutiveFailures = 0,
            ns.lastError = NULL
        WHERE ns.id = :id
        """)
    int markSuccess(@Param("id") Long id, @Param("now") Instant now);

    /**
     * Marks a source as failed in this cycle
     */
    @Modifying
    @Query("""
        UPDATE NewsSource ns SET
            ns.lockedBy = NULL,
            ns.lockedAt = NULL,
            ns.fetchedThisCycle = true,
            ns.consecutiveFailures = ns.consecutiveFailures + 1,
            ns.lastError = :error
        WHERE ns.id = :id
        """)
    int markFailed(@Param("id") Long id, @Param("error") String error);

    /**
     * Resets all sources at the start of a new fetch cycle
     */
    @Modifying
    @Query("UPDATE NewsSource ns SET ns.fetchedThisCycle = false, ns.lockedBy = NULL, ns.lockedAt = NULL")
    int resetAllForNewCycle();

    /**
     * Releases lock without updating fetch time (for graceful shutdown)
     */
    @Modifying
    @Query("UPDATE NewsSource ns SET ns.lockedBy = NULL, ns.lockedAt = NULL WHERE ns.lockedBy = :workerId")
    int releaseAllLocks(@Param("workerId") String workerId);

    /**
     * Counts how many enabled sources have NOT been fetched this cycle
     */
    @Query("SELECT COUNT(ns) FROM NewsSource ns WHERE ns.enabled = true AND ns.fetchedThisCycle = false")
    long countUnfetchedSources();
}
