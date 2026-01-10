package rssfetcher.demo.services.cluster;

/**
 * Service interface for clustering articles stored in the database.
 * Implementations should connect to the database, perform embedding, and group articles into clusters.
 */
public interface ClusterService {
    void cluster();
}

