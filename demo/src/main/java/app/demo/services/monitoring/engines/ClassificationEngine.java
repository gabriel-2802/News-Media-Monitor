package app.demo.services.monitoring.engines;

import app.demo.entities.Article;

import java.util.List;

/**
 * interface for engines that classify articles into topics.
 */
public interface ClassificationEngine {
    /**
     * classifies the given article into one of the provided topics.
     *
     * @param article the article to classify
     * @param topics the set of possible topic names
     * @return the name of the topic assigned to the article
     */
    String classify(Article article, List<String> topics);
}