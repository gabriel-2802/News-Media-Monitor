package app.demo.services.monitoring.engines;

import app.demo.entities.Article;
import org.springframework.stereotype.Component;

@Component("hashBasedSimilarityEngine")
public class HashBasedSimilarityEngine implements SimilarityEngine {
    private static final int SIM_HASH_THRESHOLD = 3;

    @Override
    public boolean isDuplicate(Article a1, Article a2) {
        if (a1.getSha256Hash().equals(a2.getSha256Hash())) {
            return true;
        }
        return hammingDistance(a1.getSimHash(), a2.getSimHash()) <= SIM_HASH_THRESHOLD;
    }

    @Override
    public double computeSimilarity(Article a1, Article a2) {
        return 1.0 - (double) hammingDistance(a1.getSimHash(), a2.getSimHash()) / Long.SIZE;
    }

    private int hammingDistance(long x, long y) {
        return Long.bitCount(x ^ y);
    }
}
