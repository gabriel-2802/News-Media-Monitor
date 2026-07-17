package data.provider.models;

import data.provider.util.UuidStringIdGenerator;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Node("Story")
@Data
@Builder
public class Story {

    @Id
    @GeneratedValue(UuidStringIdGenerator.class)
    private String id;

    /**
     * Title of the story is the title of first article attached to it.
     */
    private String title;

    /**
     * When this Story was first created (i.e. when its first article was
     * clustered). Used for the recency-window filter when searching for
     * candidate stories in the clustering worker.
     */
    private Instant createdAt;

    /**
     * Bumped every time a new article is attached. Drives both the
     * recency-window filter and the trending score.
     */
    private Instant lastUpdated;

    /**
     * Denormalized count, kept in sync on every attach — avoids a
     * relationship count query on every feed read.
     */
    private int articleCount = 0;

    /**
     * Distinct source count. Only incremented when an attached article's
     * source hasn't contributed to this Story before — this is what lets
     * the feed distinguish "5 reports from 5 outlets" from "5 reprints of
     * one wire story."
     */
    private int sourceCount = 0;

    /**
     * Precomputed ranking signal for the feed (recency + article velocity +
     * source diversity — see architecture plan §10/§13 for the formula).
     * Recomputed on every attach rather than at read time, since feed reads
     * happen far more often than attaches.
     */
    private double trendingScore = 0.0;

    /**
     * Articles belonging to this Story. Lazy by default in SDN — don't
     * fetch this for feed list views (§10 /feed), only for the
     * /feed/story/{id} detail view where you actually need every article.
     */
    @Builder.Default
    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.INCOMING)
    private List<Article> articles = new ArrayList<>();
}