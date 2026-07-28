package data.provider.dto.responses;

import data.provider.models.Topic;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A topic derived from ingested articles, with the number of articles tagged under it.")
public record TopicDto(
        @Schema(description = "Topic name.", example = "Politics")
        String name,

        @Schema(description = "Number of articles currently tagged with this topic.", example = "17")
        long articleCount
) {
    public TopicDto(Topic topic, long articleCount) {
        this(topic.getName(), articleCount);
    }
}