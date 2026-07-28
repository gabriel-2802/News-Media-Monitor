package data.provider.models;

import data.provider.util.UuidStringIdGenerator;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Data for disabling a source which cannot be fetched from
     */
    @Builder.Default
    private Integer failureCount = 0;
    @Builder.Default
    private Boolean isDisabled = false;

    /**
     * Data for political bias
     */
    private String politicalView;
    private String notes;
    @Builder.Default
    private List<String> sources  = new ArrayList<>();
    @CompositeProperty
    @Builder.Default
    private Map<String, String> biasScores  = new HashMap<>();


}
