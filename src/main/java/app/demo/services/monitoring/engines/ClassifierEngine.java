package app.demo.services.monitoring.engines;

import app.demo.entities.Article;
import app.demo.entities.Topic;

import java.util.Set;

/**
 * Interface for engines that classify articles into topics.
 */
public interface ClassifierEngine {
    /**
     * Classifies the given article into one of the provided topics.
     *
     * @param article the article to classify
     * @param topics the set of possible topic names
     * @return the name of the topic assigned to the article
     */
    String classify(Article article, Set<String> topics);
}