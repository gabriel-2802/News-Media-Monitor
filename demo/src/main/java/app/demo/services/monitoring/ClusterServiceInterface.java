package app.demo.services.monitoring;

/**
 * service interface for clustering articles stored in the database.
 * implementations should connect to the database, perform embedding, and group articles into clusters.
 */
public interface ClusterServiceInterface {

    /**
     * triggers the clustering process for unclustered articles.
     */
    void cluster();
}

