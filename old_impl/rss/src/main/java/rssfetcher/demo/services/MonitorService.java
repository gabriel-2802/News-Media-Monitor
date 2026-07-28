package rssfetcher.demo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rssfetcher.demo.entities.Article;
import rssfetcher.demo.entities.NewsSource;
import rssfetcher.demo.repositories.NewsSourceRepository;
import rssfetcher.demo.services.monitor.RssFetcher;
import rssfetcher.demo.services.monitor.TopicAssigner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MonitorService {
    private final NewsSourceRepository newsSourceRepository;
    private final RssFetcher rssFetcher;
    private final TopicAssigner topicAssigner;
    private final JdbcTemplate jdbc;

    public MonitorService(NewsSourceRepository newsSourceRepository,
                          RssFetcher rssFetcher,
                          TopicAssigner topicAssigner,
                          JdbcTemplate jdbc) {
        this.newsSourceRepository = newsSourceRepository;
        this.rssFetcher = rssFetcher;
        this.topicAssigner = topicAssigner;
        this.jdbc = jdbc;
    }

    @Transactional
    public void processRssUrl(String rssUrl) {
        Optional<NewsSource> ns = newsSourceRepository.findByRssUrl(rssUrl);
        if (ns.isEmpty()) {
            log.warn("News source not found for RSS URL: {}", rssUrl);
            return;
        }

        List<Article> fetchedArticles = rssFetcher.fetchFrom(ns.get());

        if (fetchedArticles.isEmpty()) {
            log.info("No articles fetched from {}", rssUrl);
            return;
        }

        fetchedArticles.forEach(topicAssigner::assignTopic);

        int inserted = batchInsertArticles(fetchedArticles, ns.get());

        if (inserted > 0) {
            jdbc.execute("NOTIFY new_articles_ready");
            log.info("Inserted {} new articles from {} (fetched {})",
                    inserted, rssUrl, fetchedArticles.size());
        } else {
            log.debug("All {} articles from {} already exist",
                    fetchedArticles.size(), rssUrl);
        }
    }

    private int batchInsertArticles(List<Article> articles, NewsSource newsSource) {
        if (articles.isEmpty()) return 0;

        String sql = """
        INSERT INTO articles (
            title, content, source, url, published, summary, topic_topic_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (url) DO NOTHING
        """;

        int[][] results = jdbc.batchUpdate(
                sql,
                articles,
                100,
                (ps, article) -> {
                    ps.setString(1, article.getTitle());
                    ps.setString(2, article.getContent());
                    ps.setString(3, newsSource.getName());
                    ps.setString(4, article.getUrl());
                    ps.setTimestamp(5, article.getPublished() != null ?
                            new Timestamp(article.getPublished().getTime()) : null);
                    ps.setString(6, article.getSummary());
                    if (article.getTopic() != null) {
                        ps.setLong(7, article.getTopic().getId());
                    } else {
                        ps.setNull(7, java.sql.Types.BIGINT);
                    }
                }
        );

        return Arrays.stream(results).flatMapToInt(Arrays::stream).sum();
    }
}