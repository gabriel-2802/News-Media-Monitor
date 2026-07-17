package data.provider.models;

import data.provider.util.UuidStringIdGenerator;
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
    @GeneratedValue(UuidStringIdGenerator.class)
    private String id;

    private String name;
}
