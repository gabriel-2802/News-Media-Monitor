package data.provider.models;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Node("NewsSource")
@Data
@Builder
public class NewsSource {
    @Id
    @GeneratedValue
    private String id;

    private String name;
    private String baseUrl;
    private String rssUrl;
    private Integer failureCount = 0;
    private Boolean isDisabled = false;
}
