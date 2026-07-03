package data.provider.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for registering a news source to scrape.")
public record NewsSourceRequest(

        @NotNull @NotEmpty
        @Schema(description = "Unique, human-readable name for the source.", example = "example-news",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotNull @NotEmpty
        @Schema(description = "Base URL of the source's website. Articles must be hosted under this URL.",
                example = "https://example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String baseUrl,

        @NotNull @NotEmpty
        @Schema(description = "RSS feed URL used to discover new articles for this source.",
                example = "https://example.com/rss", requiredMode = Schema.RequiredMode.REQUIRED)
        String rssUrl) {}
