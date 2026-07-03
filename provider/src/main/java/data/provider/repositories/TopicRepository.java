package data.provider.repositories;

import data.provider.models.Topic;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface TopicRepository extends Neo4jRepository<Topic, String> {
    Optional<Topic> findByName(String name);
}
