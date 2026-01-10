package rssfetcher.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rssfetcher.demo.entities.MonitoringJob;

import java.util.Optional;

/**
 * Repository for managing MonitoringJob entities with advanced job queue operations.
 * Includes methods for claiming jobs, updating statuses, and handling retries.
 */
public interface MonitoringJobRepository extends JpaRepository<MonitoringJob, Long> {

    /**
     * Atomically claim the next pending job with SKIP LOCKED for concurrency.
     * Returns the full job entity with updated attempts counter.
     */
    @Query(value = """
        WITH next_job AS (
          SELECT id
          FROM monitoring_jobs
          WHERE status = 'pending'
          ORDER BY created_at ASC, id ASC
          FOR UPDATE SKIP LOCKED
          LIMIT 1
        )
        UPDATE monitoring_jobs j
        SET status = 'processing',
            locked_at = now(),
            locked_by = :workerId,
            attempts = attempts + 1,
            updated_at = now()
        FROM next_job
        WHERE j.id = next_job.id
        RETURNING j.*
        """, nativeQuery = true)
    Optional<MonitoringJob> claimNextJob(@Param("workerId") String workerId);

    /**
     * Mark a job as successfully completed
     */
    @Modifying
    @Query(value = """
        UPDATE monitoring_jobs 
        SET status = 'done',
            completed_at = now(),
            updated_at = now()
        WHERE id = :id
        """, nativeQuery = true)
    int markDone(@Param("id") Long id);

    /**
     * Requeue a failed job for retry (moves back to pending status)
     */
    @Modifying
    @Query(value = """
        UPDATE monitoring_jobs 
        SET status = 'pending',
            last_error = :error,
            locked_at = NULL,
            locked_by = NULL,
            updated_at = now()
        WHERE id = :id
        """, nativeQuery = true)
    int requeue(@Param("id") Long id, @Param("error") String error);

    /**
     * Mark a job as permanently failed after max retries
     */
    @Modifying
    @Query(value = """
        UPDATE monitoring_jobs 
        SET status = 'failed',
            last_error = :error,
            failed_at = now(),
            updated_at = now()
        WHERE id = :id
        """, nativeQuery = true)
    int markFailed(@Param("id") Long id, @Param("error") String error);

}