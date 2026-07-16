package data.provider.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to tag an article with a topic.")
public record TopicSetRequest(
        @Schema(description = "Canonical URL of the article to tag.", example = "https://www.bbc.co.uk/news/articles/ckg8m2xkg84o")
        @NotNull @NotEmpty
        String url,

        @Schema(description = "Topic name to assign to the article.", example = "religion")
        @NotNull @NotEmpty
        String topic) {}