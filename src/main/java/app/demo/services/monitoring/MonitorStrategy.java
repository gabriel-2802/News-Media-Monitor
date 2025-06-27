package app.demo.services.monitoring;

import app.demo.entities.Article;
import app.demo.entities.NewsSource;

import java.util.List;

/**
 * Abstract strategy for monitoring news sources and retrieving articles.
 * Subclasses should implement specific monitoring logic.
 */
public abstract class MonitorStrategy {
    /**
     * Fetcher for retrieving RSS feeds.
     */
    protected final RssFetcher rssFetcher;

    /**
     * Assigner for determining article topics.
     */
    protected final TopicAssigner topicAssigner;

    /**
     * Constructs a MonitorStrategy with the given RSS fetcher and topic assigner.
     *
     * @param rssFetcher    the RSS fetcher to use
     * @param topicAssigner the topic assigner to use
     */
    public MonitorStrategy(RssFetcher rssFetcher, TopicAssigner topicAssigner) {
        this.rssFetcher = rssFetcher;
        this.topicAssigner = topicAssigner;
    }

    /**
     * Retrieves articles from the given list of news sources.
     *
     * @param sources the list of news sources to monitor
     * @return a list of articles retrieved from the sources
     */
    public abstract List<Article> monitor(List<NewsSource> sources);
}