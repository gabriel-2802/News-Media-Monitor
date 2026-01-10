package rssfetcher.demo.services.engines;

import rssfetcher.demo.entities.Article;

import java.util.List;

/**
 * Interface for engines that classify articles into topics.
 */
public interface ClassificationEngine {
    /**
     * Classifies the given article into one of the provided topics.
     *
     * @param article the article to classify
     * @param topics the set of possible topic names
     * @return the name of the topic assigned to the article
     */
    String classify(Article article, List<String> topics);
}