package data.provider.services.classifiers;

import java.util.List;

/**
 * Interface for engines that classify articles into topics.
 */
public interface ClassificationEngine {
    /**
     * Classifies the given article into one of the provided topics.
     *
     * @param text the article to classify
     * @param topics the set of possible topic names
     * @return the name of the class assigned to the text
     */
    String classify(String text, List<String> topics);
}