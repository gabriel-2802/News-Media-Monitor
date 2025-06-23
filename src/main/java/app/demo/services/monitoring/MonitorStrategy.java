package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;

import java.util.List;

public abstract class MonitorStrategy {
    protected final RssFetcher rssFetcher;
    protected final TopicAssigner topicAssigner;

    public MonitorStrategy(RssFetcher rssFetcher, TopicAssigner topicAssigner) {
        this.rssFetcher = rssFetcher;
        this.topicAssigner = topicAssigner;
    }

    public abstract List<Article> getArticles(List<NewsSource> sources);
}
