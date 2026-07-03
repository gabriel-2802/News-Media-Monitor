package data.provider.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Payload for persisting a scraped article.")
public record ArticleRequest(

        @Schema(description = "Byline reported by the source, if any.", example = "Jane Doe")
        String author,

        @NotNull @NotEmpty
        @Schema(description = "Article headline.", example = "Senate passes new budget bill", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotNull @NotEmpty
        @Schema(description = "Canonical URL of the article. Must belong to the domain of the referenced source.",
                example = "https://example.com/news/senate-passes-budget", requiredMode = Schema.RequiredMode.REQUIRED)
        String url,

        @NotNull @NotEmpty
        @Schema(description = "Full extracted article text.", requiredMode = Schema.RequiredMode.REQUIRED)
        String bodyText,

        @NotNull @NotEmpty
        @Schema(description = "Timestamp the article was published by the source.", example = "2026-07-01T14:30:00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime publishedAt,

        @NotNull @NotEmpty
        @Schema(description = "Name of the news source this article was scraped from. Must already be registered.",
                example = "example-news", requiredMode = Schema.RequiredMode.REQUIRED)
        String sourceName) {}
