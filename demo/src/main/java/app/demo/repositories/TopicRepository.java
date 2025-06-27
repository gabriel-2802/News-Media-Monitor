package app.demo.repositories;

import app.demo.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    boolean existsByName(String name);

    Optional<Topic> findByName(String topicName);

    default Topic getDefaultTopic() {
        return findByName("world news")
                .orElseThrow(() -> new IllegalStateException("Default topic not found"));
    }
}
