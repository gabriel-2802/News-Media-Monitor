package data.provider.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "Payload for registering or updating a news source to scrape.")
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
        String rssUrl,

        @Schema(description = "Free-text notes about this source.")
        String notes,

        @Schema(description = "Political leaning/bias classification of this source.",
                example = "center-left")
        String politicalView,

        @Schema(description = "Reference sources/citations backing the bias classification.")
        List<String> sources,

        @Schema(description = "Bias scores keyed by rating provider or methodology.",
                example = "{\"AllSides\": \"Lean Left\"}")
        Map<String, String> biasScores

) {}