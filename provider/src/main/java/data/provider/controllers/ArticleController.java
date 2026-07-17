package data.provider.controllers;

import data.provider.dto.requests.ArticleRequest;
import data.provider.dto.requests.TopicSetRequest;
import data.provider.dto.responses.ArticleDto;
import data.provider.dto.responses.ErrorResponse;
import data.provider.services.ArticleService;
import data.provider.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(Constants.ARTICLES_BASE_PATH)
@Tag(name = "Articles", description = "Persisting and querying scraped articles.")
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    @Operation(summary = "List all articles", description = "Returns a paginated list of all articles, across all sources, ordered as stored.")
    @ApiResponse(responseCode = "200", description = "Articles retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = ArticleDto.class))))
    public ResponseEntity<List<ArticleDto>> getAllArticles(
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of articles per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(articleService.getAllArticles(page, count));
    }

    @GetMapping(Constants.ARTICLES_BY_SOURCE_PATH)
    @Operation(summary = "List articles from a source", description = "Returns a paginated list of articles published by the given news source.")
    @ApiResponse(responseCode = "200", description = "Articles retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = ArticleDto.class))))
    public ResponseEntity<List<ArticleDto>> getAllArticlesFromSource(
            @Parameter(description = "Name of the news source.", example = "example-news", required = true)
            @PathVariable final String sourceName,
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of articles per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(articleService.getAllArticlesFromSource(page, count, sourceName));
    }

    @GetMapping(Constants.ARTICLES_BY_STORY_PATH)
    @Operation(summary = "List articles in a story", description = "Returns a paginated list of all articles that belong to the given story cluster.")
    @ApiResponse(responseCode = "200", description = "Articles retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = ArticleDto.class))))
    public ResponseEntity<List<ArticleDto>> getAllArticlesInStory(
            @Parameter(description = "ID of the story cluster.", required = true)
            @PathVariable final String storyId,
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of articles per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(articleService.getArticlesByStory(page, count, storyId));
    }

    @GetMapping(Constants.ARTICLES_BY_TOPIC_PATH)
    @Operation(summary = "List articles by topic", description = "Returns a paginated list of articles tagged with the given topic.")
    @ApiResponse(responseCode = "200", description = "Articles retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = ArticleDto.class))))
    public ResponseEntity<List<ArticleDto>> getAllArticlesWithTopic(
            @Parameter(description = "Name of the topic.", example = "Politics", required = true)
            @PathVariable final String topicName,
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of articles per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(articleService.getAllArticlesWithTopic(page, count, topicName));
    }

    @PostMapping
    @Operation(summary = "Save a new article", description = "Persists a scraped article. Fails if an article with the same URL already exists, "
            + "if the referenced source is not registered, or if the URL does not belong to the source's base URL.")
    @ApiResponse(responseCode = "201", description = "Article created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ArticleDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed, article URL already exists, source does not exist, "
            + "or the URL does not match the source's base URL",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ArticleDto> saveArticle(@Valid @RequestBody final ArticleRequest articleRequest) {
        final ArticleDto savedArticle = articleService.saveArticle(articleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedArticle);
    }

    @GetMapping(Constants.ARTICLES_EXISTS_PATH)
    @Operation(summary = "Check article existence", description = "Returns whether an article with the given URL has already been ingested.")
    @ApiResponse(responseCode = "200", description = "Existence check result",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Boolean.class)))
    public ResponseEntity<Boolean> existsByURL(
            @Parameter(description = "Canonical URL to check.", example = "https://example.com/news/senate-passes-budget", required = true)
            @RequestParam final String url) {
        return ResponseEntity.ok(articleService.existsByURL(url));
    }

    @GetMapping(Constants.ARTICLES_BY_URL_PATH)
    @Operation(summary = "Get an article by URL", description = "Returns a single article by its canonical URL.")
    @ApiResponse(responseCode = "200", description = "Article retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ArticleDto.class)))
    @ApiResponse(responseCode = "400", description = "Article does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ArticleDto> getArticleByUrl(
            @Parameter(description = "Canonical URL of the article.", example = "https://example.com/news/senate-passes-budget", required = true)
            @RequestParam final String url) {
        return ResponseEntity.ok(articleService.getArticleByUrl(url));
    }

    @PatchMapping(Constants.ARTICLES_SET_TOPIC_PATH)
    @Operation(summary = "Set an article's topic", description = "Tags the article at the given URL with a topic, replacing any topic it already had.")
    @ApiResponse(responseCode = "200", description = "Topic set successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ArticleDto.class)))
    @ApiResponse(responseCode = "400", description = "Article does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ArticleDto> setTopic(@Valid @RequestBody final TopicSetRequest topicSetRequest) {
        return ResponseEntity.ok(articleService.setTopic(topicSetRequest));
    }

    @GetMapping(Constants.ARTICLES_SEARCH_PATH)
    @Operation(summary = "Search articles by title or body", description = "Full-text search over article titles and body text, ordered by "
            + "relevance. Matching is prefix-based per term (e.g. \"sen bud\" matches text containing words starting with sen and bud).")
    @ApiResponse(responseCode = "200", description = "Matching articles retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = ArticleDto.class))))
    public ResponseEntity<List<ArticleDto>> searchArticles(
            @Parameter(description = "Free-text search query.", example = "senate budget", required = true)
            @RequestParam @NotBlank final String q,
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of articles per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(articleService.searchArticles(q, page, count));
    }
}