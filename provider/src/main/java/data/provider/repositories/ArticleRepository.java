package data.provider.repositories;

import data.provider.models.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

public interface ArticleRepository extends Neo4jRepository<Article, String> {

    @Query(
            value = """
        MATCH (s:NewsSource)-[:PUBLISHED]->(a:Article)
        OPTIONAL MATCH (a)-[:HAS_TOPIC]->(t:Topic)
        RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r], t, [(a)-[ht:HAS_TOPIC]->(t) | ht]
        ORDER BY a.publishedAt DESC
        SKIP $skip LIMIT $limit
        """,
            countQuery = """
        MATCH (:NewsSource)-[:PUBLISHED]->(a:Article)
        RETURN count(a)
        """
    )
    Page<Article> findAllWithSourceAndTopic(Pageable pageable);

    @Query(
            value = """
        MATCH (s:NewsSource {name: $sourceName})-[:PUBLISHED]->(a:Article)
        OPTIONAL MATCH (a)-[:HAS_TOPIC]->(t:Topic)
        RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r], t, [(a)-[ht:HAS_TOPIC]->(t) | ht]
        ORDER BY a.publishedAt DESC
        SKIP $skip LIMIT $limit
        """,
            countQuery = """
        MATCH (:NewsSource {name: $sourceName})-[:PUBLISHED]->(a:Article)
        RETURN count(a)
        """
    )
    Page<Article> findBySourceNameWithTopic(String sourceName, Pageable pageable);

    @Query(
            value = """
        MATCH (t:Topic {name: $topicName})<-[:HAS_TOPIC]-(a:Article)
        OPTIONAL MATCH (a)<-[:PUBLISHED]-(s:NewsSource)
        RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r], t, [(a)-[ht:HAS_TOPIC]->(t) | ht]
        ORDER BY a.publishedAt DESC
        SKIP $skip LIMIT $limit
        """,
            countQuery = """
        MATCH (:Topic {name: $topicName})<-[:HAS_TOPIC]-(a:Article)
        RETURN count(a)
        """
    )
    Page<Article> findByTopicNameWithName(String topicName, Pageable pageable);


    @Query("""
            MATCH (n:NewsSource {name: $sourceName})-[:PUBLISHED]->(a:Article)
            RETURN count(a)
            """)
    long countArticlesBySource(String sourceName);

    @Query("""
        MATCH (:Topic {name: $topicName})<-[:HAS_TOPIC]-(a:Article)
        RETURN count(a)
        """)
    long countByTopic(String topicName);

    boolean existsByUrl(String url);
}