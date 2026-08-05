package rssfetcher.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Tracks fetch cycles to coordinate clustering trigger across multiple workers.
 * Only one record exists, used as a distributed lock.
 */
@Entity
@Table(name = "fetch_cycle_state")
@Data
public class FetchCycleState {
    @Id
    private Long id = 1L; // Single row

    @Column(name = "cycle_id")
    private Long cycleId = 0L;

    @Column(name = "clustering_triggered")
    private Boolean clusteringTriggered = false;

    @Column(name = "clustering_triggered_by")
    private String clusteringTriggeredBy;

    @Column(name = "clustering_triggered_at")
    private Instant clusteringTriggeredAt;

    @Column(name = "last_cycle_started_at")
    private Instant lastCycleStartedAt;
}
