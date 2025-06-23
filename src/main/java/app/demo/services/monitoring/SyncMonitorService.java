package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/* * SingleThreadedMonitorService is responsible for monitoring articles from various news sources in a single-threaded manner.
 * It fetches articles synchronously, assigns topics to each article, and saves them to the repository.
 */
@Service
public class SyncMonitorService extends MonitorService {

    public SyncMonitorService(RssFetcher rssFetcher, ArticleRepository articleRepository,
                              NewsSourceRepository newsSourceRepository, TopicAssigner topicAssigner) {
        super(rssFetcher, articleRepository, newsSourceRepository, topicAssigner);
    }

    @Override
    public void startMonitoring() {
        List<Article> articles = new ArrayList<>();
        List<NewsSource> newsSources = newsSourceRepository.findAll();
        for (NewsSource newsSource : newsSources) {
            List<Article> fetchedArticles = rssFetcher.fetchFrom(newsSource);
            fetchedArticles.forEach(topicAssigner::assignTopic);
            articles.addAll(fetchedArticles);
        }
        try {
            articleRepository.saveAll(articles);
        } catch (DataIntegrityViolationException ignored) {}
    }
}
