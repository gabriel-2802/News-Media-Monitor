package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.repositories.ArticleRepository;
import app.demo.repositories.NewsSourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Component
public class SyncMonitorStrategy extends MonitorStrategy {

    public SyncMonitorStrategy(RssFetcher rssFetcher, TopicAssigner topicAssigner) {
        super(rssFetcher, topicAssigner);
    }

    @Override
    public List<Article> getArticles(List<NewsSource> newsSources) {
        List<Article> articles = new ArrayList<>();
        for (NewsSource newsSource : newsSources) {
            List<Article> fetchedArticles = rssFetcher.fetchFrom(newsSource);
            fetchedArticles.forEach(topicAssigner::assignTopic);
            articles.addAll(fetchedArticles);
        }
        return articles;
    }
}
