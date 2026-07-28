package rssfetcher.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rssfetcher.demo.entities.FetchCycleState;

import java.time.Instant;

@Repository
public interface FetchCycleStateRepository extends JpaRepository<FetchCycleState, Long> {

    /**
     * Atomically starts a new cycle: increments cycle_id, resets clustering_triggered flag.
     * Returns the number of updated rows (1 if successful).
     */
    @Modifying
    @Query(value = """
        UPDATE fetch_cycle_state 
        SET cycle_id = cycle_id + 1,
            clustering_triggered = false,
            clustering_triggered_by = NULL,
            clustering_triggered_at = NULL,
            last_cycle_started_at = :now
        WHERE id = 1
        """, nativeQuery = true)
    void startNewCycle(@Param("now") Instant now);

    /**
     * Atomically claims the right to trigger clustering for this cycle.
     * Uses optimistic locking: only succeeds if clustering hasn't been triggered yet.
     * Returns 1 if this worker claimed it, 0 if another worker already did.
     */
    @Modifying
    @Query(value = """
        UPDATE fetch_cycle_state 
        SET clustering_triggered = true,
            clustering_triggered_by = :workerId,
            clustering_triggered_at = :now
        WHERE id = 1 AND clustering_triggered = false
        """, nativeQuery = true)
    int claimClusteringTrigger(@Param("workerId") String workerId, @Param("now") Instant now);
}
