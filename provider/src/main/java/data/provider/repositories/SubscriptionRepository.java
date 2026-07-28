package data.provider.repositories;

import data.provider.models.Subscription;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Optional;

public interface SubscriptionRepository extends Neo4jRepository<Subscription, String> {

    @Query("""
            MATCH (s:Subscription)-[:SUBSCRIBES_TO]->(story:Story {id: $storyId})
            RETURN s, story, [(s)-[r:SUBSCRIBES_TO]->(story) | r]
            """)
    Optional<Subscription> findByStoryId(String storyId);

    @Query("""
            MATCH (s:Subscription)-[:SUBSCRIBES_TO]->(topic:Topic {name: $topicName})
            RETURN s, topic, [(s)-[r:SUBSCRIBES_TO]->(topic) | r]
            """)
    Optional<Subscription> findByTopicName(String topicName);

    @Query("""
            MATCH (s:Subscription)-[:SUBSCRIBES_TO]->(:Story {id: $storyId})
            RETURN count(s) > 0
            """)
    boolean existsByStoryId(String storyId);

    @Query("""
            MATCH (s:Subscription)-[:SUBSCRIBES_TO]->(:Topic {name: $topicName})
            RETURN count(s) > 0
            """)
    boolean existsByTopicName(String topicName);
}
