package data.provider.repositories;

import data.provider.models.Story;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StoryRepository extends Neo4jRepository<Story, String> {

    @Query(
            value = """
            MATCH (story:Story)
            RETURN story
            ORDER BY story.lastUpdated DESC
            SKIP $skip LIMIT $limit
            """,
            countQuery = "MATCH (story:Story) RETURN count(story)"
    )
    Page<Story> findAllPaginated(Pageable pageable);

    @Query("""
            MATCH (story:Story)
            WHERE story.lastUpdated >= $since
            RETURN story
            ORDER BY story.lastUpdated DESC
            """)
    List<Story> findActiveSince(Instant since);

    @Query("""
            MATCH (story:Story {id: $storyId})
            MATCH (src:NewsSource)-[:PUBLISHED]->(a:Article {url: $articleUrl})
            OPTIONAL MATCH (src)-[:PUBLISHED]->(existing:Article)-[:BELONGS_TO]->(story)
              WHERE existing <> a
            WITH story, a, count(existing) AS existingFromSource
            MERGE (a)-[:BELONGS_TO]->(story)
            ON CREATE SET
              story.articleCount   = story.articleCount + 1,
              story.lastUpdated    = $now,
              story.sourceCount    = story.sourceCount + CASE WHEN existingFromSource = 0 THEN 1 ELSE 0 END
            RETURN story
            """)
    Optional<Story> attachArticle(String storyId, String articleUrl, Instant now);
}