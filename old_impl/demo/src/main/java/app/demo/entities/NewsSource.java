package app.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "news_sources")
@Data
public class NewsSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String baseUrl;
    @Column(unique = true)
    private String rssUrl;

    // Scheduling fields
    @Column(name = "last_fetched_at")
    private Instant lastFetchedAt;

    @Column(name = "fetched_this_cycle")
    private Boolean fetchedThisCycle = false;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;


    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "enabled")
    private Boolean enabled = true;
}

