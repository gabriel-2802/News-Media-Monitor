package app.demo.services;

import app.demo.entities.NewsSource;
import app.demo.repositories.NewsSourceRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

@Service
@Data
public class MonitoringService {
    private final ClusterService clusterService;
    private final NewsSourceRepository newsSourceRepository;
    private final JdbcTemplate jdbc;

    @SuppressWarnings("SqlResolve")
    @Transactional
    public int createMonitoringJobs() {
        List<NewsSource> sources = newsSourceRepository.findAll();

        // batch insert
        String sql = """
        INSERT INTO monitoring_jobs (rss_url, status)
        VALUES (?, 'pending')
        ON CONFLICT DO NOTHING
        """;

        int[] results = jdbc.batchUpdate(sql,
                sources.stream()
                        .map(NewsSource::getRssUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .map(url -> new Object[]{url})
                        .toList()
        );

        int inserted = Arrays.stream(results).sum();

        if (inserted > 0) {
            jdbc.execute("NOTIFY new_monitoring_jobs");
        }

        return inserted;
    }

    @SuppressWarnings("SqlResolve")
    @Transactional
    public int requeueStuckJobs(int minutesStuck, int maxAttempts) {
        // defensive defaults if caller passes weird values
        if (minutesStuck <= 0) minutesStuck = 10;
        if (maxAttempts <= 0) maxAttempts = 5;

        return jdbc.update("""
        UPDATE monitoring_jobs
        SET status='pending',
            locked_by=NULL,
            locked_at=NULL,
            updated_at=now()
        WHERE status='processing'
          AND locked_at IS NOT NULL
          AND locked_at < now() - (? * interval '1 minute')
          AND attempts < ?
        """, minutesStuck, maxAttempts);
    }

    public void triggerClustering() {
        clusterService.cluster();
    }
}
