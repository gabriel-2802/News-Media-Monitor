package data.provider.models;

import data.provider.util.UuidStringIdGenerator;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Subscription")
@Data
@Builder
public class Subscription {
    @Id
    @GeneratedValue(UuidStringIdGenerator.class)
    private String id;

    private int count;

    @Relationship(type = "SUBSCRIBES_TO", direction = Relationship.Direction.OUTGOING)
    private Story story;

    @Relationship(type = "SUBSCRIBES_TO", direction = Relationship.Direction.OUTGOING)
    private Topic topic;
}
