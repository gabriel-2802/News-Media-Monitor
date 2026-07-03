package data.provider.dto.responses;

import data.provider.models.NewsSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A registered news source and its current stats.")
public record NewsSourceDto(

        @Schema(description = "Unique, human-readable name for the source.", example = "example-news")
        String name,

        @Schema(description = "Base URL of the source's website.", example = "https://example.com")
        String baseUrl,

        @Schema(description = "RSS feed URL used to discover new articles for this source.", example = "https://example.com/rss")
        String rssUrl,

        @Schema(description = "Number of consecutive scrape failures recorded for this source.", example = "0")
        int failureCount,

        @Schema(description = "Whether the source has been disabled from further scraping.", example = "false")
        boolean disabled,

        @Schema(description = "Total number of articles ingested from this source.", example = "42")
        Long articleCount) {

    public NewsSourceDto(NewsSource newsSource, long articleCount) {
        this(newsSource.getName(), newsSource.getBaseUrl(), newsSource.getRssUrl(),
                newsSource.getFailureCount(), newsSource.getIsDisabled(), articleCount);
    }
}
