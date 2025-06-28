package app.demo.repositories;

import app.demo.entities.Article;
import app.demo.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByTopic(Topic topic);

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
    List<Article>  findBySourceName(@Param("sourceName") String sourceName);

    @Query(value = """
    SELECT * FROM articles
    WHERE to_tsvector('english', title || ' ' || content)
          @@ plainto_tsquery('english', :searchRequest)
    """, nativeQuery = true)
    List<Article> searchByKeyword(@Param("searchRequest") String searchRequest);
}