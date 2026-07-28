package data.provider.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for attaching an article to a story.")
public record AttachArticleRequest(

        @NotNull @NotEmpty
        @Schema(description = "Canonical URL of the article to attach. Must already be ingested.",
                example = "https://example.com/news/senate-passes-budget", requiredMode = Schema.RequiredMode.REQUIRED)
        String articleUrl) {}