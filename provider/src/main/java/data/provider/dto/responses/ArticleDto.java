package data.provider.dto.responses;

import data.provider.models.Article;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "An article as stored in the system.")
public record ArticleDto(

        @Schema(description = "Byline reported by the source, if any.", example = "Jane Doe")
        String author,

        @Schema(description = "Article headline.", example = "Senate passes new budget bill")
        String title,

        @Schema(description = "Canonical URL of the article.", example = "https://example.com/news/senate-passes-budget")
        String url,

        @Schema(description = "Full extracted article text.")
        String bodyText,

        @Schema(description = "Timestamp the article was published by the source.", example = "2026-07-01T14:30:00")
        LocalDateTime publishedAt,

        @Schema(description = "Name of the topic of this article", example = "politics")
        String topic,

        @Schema(description = "Name of the news source this article was scraped from.", example = "example-news")
        String source) {

    public ArticleDto(Article article) {
        this(article.getAuthor(), article.getTitle(), article.getUrl(),
                article.getBodyText(), article.getPublishedAt(), article.getTopic().getName(), article.getNewsSource().getName());
    }
}
