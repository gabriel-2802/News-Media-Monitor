package data.provider.models;

import data.provider.util.UuidStringIdGenerator;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Node("NewsSource")
@Data
@Builder
public class NewsSource {
    @Id
    @GeneratedValue(UuidStringIdGenerator.class)
    private String id;

    private String name;
    private String baseUrl;
    private String rssUrl;
    @Builder.Default
    private Integer failureCount = 0;
    @Builder.Default
    private Boolean isDisabled = false;
}
