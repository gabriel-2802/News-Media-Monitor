package data.provider.repositories;

import data.provider.models.NewsSource;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface NewsSourceRepository extends Neo4jRepository<NewsSource, String> {
    boolean existsByBaseUrl(String baseUrl);

    boolean existsByName(String name);

    Optional<NewsSource> findByName(String name);

    boolean existsByRssUrl(String rssUrl);

    @Query("""
            MATCH (n:NewsSource {name: $sourceName})
            SET n.failureCount = n.failureCount + 1,
                n.isDisabled = CASE WHEN n.failureCount + 1 >= 3 THEN true ELSE n.isDisabled END
            RETURN n
            """)
    Optional<NewsSource> incrementFailureCount(String sourceName);

    @Query("""
            MATCH (n:NewsSource {name: $sourceName})
            SET n.failureCount = 0,
                n.isDisabled = false
            RETURN n
            """)
    Optional<NewsSource> resetFailureCount(String sourceName);
}
