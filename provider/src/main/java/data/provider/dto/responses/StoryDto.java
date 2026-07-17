package data.provider.dto.responses;

import data.provider.models.Story;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "A story cluster grouping related articles.")
public record StoryDto(

        @Schema(description = "Unique identifier of the story.", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Title derived from the first attached article.", example = "Senate passes new budget bill")
        String title,

        @Schema(description = "When the story was first created.")
        Instant createdAt,

        @Schema(description = "When the last article was attached to this story.")
        Instant lastUpdated,

        @Schema(description = "Total number of articles attached to this story.", example = "12")
        int articleCount,

        @Schema(description = "Number of distinct sources that contributed to this story.", example = "5")
        int sourceCount,

        @Schema(description = "Precomputed trending score (recency + velocity + source diversity).", example = "3.14")
        double trendingScore,

        @ArraySchema(schema = @Schema(implementation = ArticleDto.class))
        @Schema(description = "Articles belonging to this story. Empty unless explicitly populated by the caller (e.g. the plain story list endpoint) — not fetched for every story mapping to avoid unnecessary load.")
        List<ArticleDto> articles) {

    public StoryDto(Story story) {
        this(story, List.of());
    }

    public StoryDto(Story story, List<ArticleDto> articles) {
        this(story.getId(), story.getTitle(), story.getCreatedAt(), story.getLastUpdated(),
                story.getArticleCount(), story.getSourceCount(), story.getTrendingScore(), articles);
    }
}