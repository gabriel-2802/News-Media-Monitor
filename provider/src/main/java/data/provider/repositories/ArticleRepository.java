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
                RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r]
                ORDER BY a.publishedAt DESC
                """,
            countQuery = """
                MATCH (:NewsSource)-[:PUBLISHED]->(a:Article)
                RETURN count(a)
                """
    )
    Page<Article> findAllWithSource(Pageable pageable);

    @Query("""
            MATCH (n:NewsSource {name: $sourceName})-[:PUBLISHED]->(a:Article)
            RETURN count(a)
            """)
    long countArticlesBySource(String sourceName);

    @Query(
            value = """
                MATCH (s:NewsSource {name: $sourceName})-[:PUBLISHED]->(a:Article)
                RETURN a, s, [(s)-[r:PUBLISHED]->(a) | r]
                ORDER BY a.publishedAt DESC
                """,
            countQuery = """
                MATCH (:NewsSource {name: $sourceName})-[:PUBLISHED]->(a:Article)
                RETURN count(a)
                """
    )
    Page<Article> findBySourceName(String sourceName, Pageable pageable);

    boolean existsByUrl(String url);
}