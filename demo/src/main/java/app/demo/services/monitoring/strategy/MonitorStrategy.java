package app.demo.services.monitoring.strategy;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;
import app.demo.services.monitoring.RssFetcher;
import app.demo.services.monitoring.TopicAssigner;

import java.util.List;

/**
 * abstract strategy for monitoring news sources and retrieving articles.
 * subclasses should implement specific monitoring logic (e.g., RSS-based or HTML-based).
 */
public abstract class MonitorStrategy {
    /**
     * fetcher for retrieving RSS feeds.
     */
    protected final RssFetcher rssFetcher;

    /**
     * assigner for determining article topics.
     */
    protected final TopicAssigner topicAssigner;

    /**
     * constructs a MonitorStrategy with the given RSS fetcher and topic assigner.
     *
     * @param rssFetcher    the RSS fetcher to use
     * @param topicAssigner the topic assigner to use
     */
    public MonitorStrategy(RssFetcher rssFetcher, TopicAssigner topicAssigner) {
        this.rssFetcher = rssFetcher;
        this.topicAssigner = topicAssigner;
    }

    /**
     * retrieves articles from the given list of news sources.
     *
     * @param sources the list of news sources to monitor
     * @return a list of articles retrieved from the sources
     */
    public abstract List<Article> monitor(List<NewsSource> sources);
}
