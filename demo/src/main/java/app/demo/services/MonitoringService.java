package app.demo.services;

import app.demo.entities.NewsSource;
import app.demo.repositories.NewsSourceRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

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
        int inserted = 0;

        for (var src : sources) {
            String rssUrl = src.getRssUrl();
            if (rssUrl == null || rssUrl.isBlank()) continue;

            int n = jdbc.update("""
                INSERT INTO monitoring_jobs (rss_url, status)
                VALUES (?, 'pending')
                ON CONFLICT DO NOTHING
                """, rssUrl);

            inserted += n;
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
