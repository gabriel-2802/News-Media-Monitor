package data.provider.repositories;

import data.provider.models.Article;
import data.provider.util.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

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

    @Query(
            value = """
    MATCH (s:NewsSource)-[:PUBLISHED]->(a:Article {url: $url})
    OPTIONAL MATCH (a)-[:HAS_TOPIC]->(t:Topic)
    RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r], t, [(a)-[ht:HAS_TOPIC]->(t) | ht]
    """
    )
    Optional<Article> findByUrlWithSourceAndTopic(String url);

    @Query("""
        MATCH (s:NewsSource)-[:PUBLISHED]->(a:Article {url: $url})
        OPTIONAL MATCH (a)-[oldTopic:HAS_TOPIC]->(:Topic)
        DELETE oldTopic
        MERGE (t:Topic {name: $topicName})
        MERGE (a)-[:HAS_TOPIC]->(t)
        RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r], t, [(a)-[ht:HAS_TOPIC]->(t) | ht]
        """)
    Article setTopic(String url, String topicName);


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

    @Query(
            value = """
            MATCH (s:NewsSource)-[:PUBLISHED]->(a:Article)-[:BELONGS_TO]->(story:Story {id: $storyId})
            OPTIONAL MATCH (a)-[:HAS_TOPIC]->(t:Topic)
            RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r], t, [(a)-[ht:HAS_TOPIC]->(t) | ht]
            ORDER BY a.publishedAt DESC
            SKIP $skip LIMIT $limit
            """,
            countQuery = """
            MATCH (:Article)-[:BELONGS_TO]->(:Story {id: $storyId})
            RETURN count(*)
            """
    )
    Page<Article> findByStoryId(String storyId, Pageable pageable);

    boolean existsByUrl(String url);

    @Query(
            value = """
                    CALL db.index.fulltext.queryNodes('""" + Constants.ARTICLE_SEARCH_FULLTEXT_IDX + """
                    ', $query) YIELD node AS a, score
                    MATCH (s:NewsSource)-[:PUBLISHED]->(a)
                    OPTIONAL MATCH (a)-[:HAS_TOPIC]->(t:Topic)
                    RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r], t, [(a)-[ht:HAS_TOPIC]->(t) | ht]
                    ORDER BY score DESC
                    SKIP $skip LIMIT $limit
                    """,
            countQuery = """
                    CALL db.index.fulltext.queryNodes('""" + Constants.ARTICLE_SEARCH_FULLTEXT_IDX + """
                    ', $query) YIELD node
                    RETURN count(node)
                    """
    )
    Page<Article> searchByTitleOrBody(String query, Pageable pageable);
}