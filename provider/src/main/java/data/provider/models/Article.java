package data.provider.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
import java.time.LocalDateTime;

@Node("Article")
@Data
@Builder
public class Article {

    @Id
    @GeneratedValue
    private String id;

    private String author;
    private String title;
    private String url;
    private String bodyText;
    private LocalDateTime publishedAt;

    @Relationship(type = "PUBLISHED", direction = Relationship.Direction.INCOMING)
    private NewsSource newsSource;

    @Relationship(type = "HAS_TOPIC", direction = Relationship.Direction.OUTGOING)
    private Topic topic;
}