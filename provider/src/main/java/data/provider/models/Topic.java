package data.provider.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Topic")
@Data
@Builder
public class Topic {
    @Id
    @GeneratedValue
    private String id;

    private String name;
}
