package app.demo.repositories;

import app.demo.entities.Article;
import app.demo.entities.Topic;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByTopic(Topic topic);

    List<Article> findByClusterId(Long clusterId);

    @Query(value = """
    SELECT articles.* FROM articles
    INNER JOIN topics ON articles.topic_id = topics.id
    WHERE topics.name = :topicName
    """, nativeQuery = true)
    List<Article> findByTopicName(@Param("topicName") String topicName);

    @Query(value = """
    SELECT articles.* FROM articles
    WHERE articles.source = :sourceName
    """, nativeQuery = true)
    List<Article> findBySourceName(@Param("sourceName") String sourceName);

    @Query(value = """
    SELECT * FROM articles
    WHERE to_tsvector('english', title || ' ' || content)
          @@ plainto_tsquery('english', :searchRequest)
    """, nativeQuery = true)
    List<Article> searchByKeyword(@Param("searchRequest") String searchRequest);

    @Query("SELECT a FROM Article a WHERE a.notified = false ORDER BY a.published DESC")
    List<Article> findUnnotified();

    /**
     * Find unnotified articles with pessimistic write lock and SKIP LOCKED.
     * This prevents race conditions when multiple instances process notifications.
     * SKIP LOCKED ensures that if another transaction has locked some rows,
     * this query will skip them rather than waiting.
     */
    @Query(value = """
        SELECT * FROM articles
        WHERE notified = false
        ORDER BY published DESC
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Article> findUnnotifiedWithLock();

    /**
     * Batch fetch articles with their topics eagerly loaded to avoid N+1 queries.
     * Use this when you need to access article topics in a loop.
     */
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.topic WHERE a.id IN :articleIds")
    List<Article> findByIdInWithTopic(@Param("articleIds") List<Long> articleIds);

    /**
     * Find all unnotified articles with topics eagerly loaded.
     * More efficient than findUnnotified() when you need topic information.
     */
    @Query("SELECT a FROM Article a LEFT JOIN FETCH a.topic WHERE a.notified = false ORDER BY a.published DESC")
    List<Article> findUnnotifiedWithTopic();

    /**
     * Combination: fetch unnotified with topic and lock.
     * Best option for the notification processor.
     */
    @Query(value = """
        SELECT a.* FROM articles a
        LEFT JOIN topics t ON a.topic_id = t.id
        WHERE a.notified = false
        ORDER BY a.published DESC
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Article> findUnnotifiedWithTopicAndLock();
}