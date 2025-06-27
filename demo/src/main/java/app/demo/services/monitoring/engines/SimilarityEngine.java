package app.demo.services.monitoring.engines;

import app.demo.entities.Article;

/**
 * Interface for engines that determine article similarity and duplication.
 * Used for cluster management and duplicate detection.
 */
public interface SimilarityEngine {
    /**
     * Checks if two articles are considered duplicates.
     *
     * @param a1 the first article
     * @param a2 the second article
     * @return true if the articles are duplicates, false otherwise
     */
    boolean isDuplicate(Article a1, Article a2);

    /**
     * Computes a similarity score between two articles.
     *
     * @param a1 the first article
     * @param a2 the second article
     * @return a double value representing the similarity score (higher means more similar)
     */
    double computeSimilarity(Article a1, Article a2);
}