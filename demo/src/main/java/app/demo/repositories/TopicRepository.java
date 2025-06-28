package app.demo.repositories;

import app.demo.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    boolean existsByName(String name);

    Optional<Topic> findByName(String topicName);

    default Topic getDefaultTopic() {
        return findByName("world news")
                .orElseThrow(() -> new IllegalStateException("Default topic not found"));
    }

    @Query(value = """
        SELECT * FROM topics
        ORDER BY CASE WHEN name = 'world news' THEN 0 ELSE 1 END, name
        """, nativeQuery = true)
    List<Topic> findAllOrdered();
}
