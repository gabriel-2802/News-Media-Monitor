package app.demo.services.monitoring.engines;

import app.demo.entities.Article;

/*
 * This interface defines methods for checking if two articles are duplicates
 * and for computing the similarity score between them to serve cluster management.
 */
public interface SimilarityEngine {
    boolean isDuplicate(Article a1, Article a2);
    double computeSimilarity(Article a1, Article a2);
}
