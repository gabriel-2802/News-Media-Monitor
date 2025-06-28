package app.demo.services.monitoring.strategy;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.services.monitoring.RssFetcher;
import app.demo.services.monitoring.TopicAssigner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * sequential implementation of {@link MonitorStrategy} that processes news sources one at a time.
 * <p>
 * this strategy fetches articles and assigns topics in a simple, non-parallel manner.
 * it's useful for debugging, testing, or when simplicity is preferred over performance.
 */
@Component
public class SequentialMonitorStrategy extends MonitorStrategy {

    public SequentialMonitorStrategy(RssFetcher rssFetcher, TopicAssigner topicAssigner) {
        super(rssFetcher, topicAssigner);
    }

    @Override
    public List<Article> monitor(List<NewsSource> newsSources) {
        List<Article> articles = new ArrayList<>();
        for (NewsSource newsSource : newsSources) {
            List<Article> fetchedArticles = rssFetcher.fetchFrom(newsSource);
            fetchedArticles.forEach(topicAssigner::assignTopic);
            articles.addAll(fetchedArticles);
        }
        return articles;
    }
}
