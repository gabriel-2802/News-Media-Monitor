package data.provider.controllers;

import data.provider.dto.requests.ArticleRequest;
import data.provider.dto.responses.ArticleDto;
import data.provider.dto.responses.ErrorResponse;
import data.provider.services.ArticleService;
import data.provider.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Articles retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = ArticleDto.class))))
    })
    public ResponseEntity<List<ArticleDto>> getAllArticles(
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of articles per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(articleService.getAllArticles(page, count));
    }

    @GetMapping(Constants.ARTICLES_BY_SOURCE_PATH)
    @Operation(summary = "List articles from a source", description = "Returns a paginated list of articles published by the given news source.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Articles retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @Schema(implementation = ArticleDto.class))))
    })
    public ResponseEntity<List<ArticleDto>> getAllArticlesFromSource(
            @Parameter(description = "Name of the news source.", example = "example-news", required = true)
            @PathVariable final String sourceName,
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of articles per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(articleService.getAllArticlesFromSource(page, count, sourceName));
    }

    @PostMapping
    @Operation(summary = "Save a new article", description = "Persists a scraped article. Fails if an article with the same URL already exists, "
            + "if the referenced source is not registered, or if the URL does not belong to the source's base URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Article created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ArticleDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed, article URL already exists, source does not exist, "
                    + "or the URL does not match the source's base URL",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ArticleDto> saveArticle(@Valid @RequestBody final ArticleRequest articleRequest) {
        final ArticleDto savedArticle = articleService.saveArticle(articleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedArticle);
    }

    @GetMapping(Constants.ARTICLES_EXISTS_PATH)
    @Operation(summary = "Check article existence", description = "Returns whether an article with the given URL has already been ingested.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Existence check result",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Boolean.class)))
    })
    public ResponseEntity<Boolean> existsByURL(
            @Parameter(description = "Canonical URL to check.", example = "https://example.com/news/senate-passes-budget", required = true)
            @RequestParam final String url) {
        return ResponseEntity.ok(articleService.existsByURL(url));
    }
}
