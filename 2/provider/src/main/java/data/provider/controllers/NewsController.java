package data.provider.controllers;

import data.provider.dto.requests.NewsSourceRequest;
import data.provider.dto.responses.ErrorResponse;
import data.provider.dto.responses.NewsSourceDto;
import data.provider.services.NewsService;
import data.provider.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping(Constants.NEWS_SOURCES_BASE_PATH)
@Tag(name = "News Sources", description = "Registering and managing news sources to be scraped.")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    @Operation(summary = "List all news sources", description = "Returns a paginated list of all registered news sources, including their article counts.")
    @ApiResponse(responseCode = "200", description = "News sources retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = NewsSourceDto.class))))
    public ResponseEntity<List<NewsSourceDto>> getAllNewsSources(
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of sources per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(newsService.getAllNewsSources(page, count));
    }

    @GetMapping(Constants.NEWS_SOURCE_BY_NAME_PATH)
    @Operation(summary = "Get a news source by name", description = "Returns a single registered news source, including its article count.")
    @ApiResponse(responseCode = "200", description = "News source retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NewsSourceDto.class)))
    @ApiResponse(responseCode = "400", description = "News source does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NewsSourceDto> getNewsSourceByName(
            @Parameter(description = "Name of the news source.", example = "example-news", required = true)
            @PathVariable final String sourceName) {
        return ResponseEntity.ok(newsService.getNewsSourceByName(sourceName));
    }

    @PostMapping
    @Operation(summary = "Register a new news source", description = "Adds a news source to be scraped. Fails if a source with the same name, "
            + "base URL, or RSS URL already exists, or if either URL is unreachable.")
    @ApiResponse(responseCode = "201", description = "News source created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NewsSourceDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed, a source with the same name/baseUrl/rssUrl already exists, "
            + "or the source's URLs are unreachable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NewsSourceDto> addNewsSource(@Valid @RequestBody final NewsSourceRequest newsSourceRequest) {
        final NewsSourceDto savedSource = newsService.addNewsSource(newsSourceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSource);
    }

    @PutMapping(Constants.NEWS_SOURCE_BY_NAME_PATH)
    @Operation(summary = "Update a news source", description = "Replaces the news source's data with the desired final state described in the "
            + "request body. Fails if the new name, base URL, or RSS URL already belongs to a different source, or if either URL is unreachable.")
    @ApiResponse(responseCode = "200", description = "News source updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NewsSourceDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed, the source does not exist, the new name/baseUrl/rssUrl already belongs "
            + "to a different source, or the source's URLs are unreachable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NewsSourceDto> updateNewsSource(
            @Parameter(description = "Name of the news source.", example = "example-news", required = true)
            @PathVariable final String sourceName,
            @Valid @RequestBody final NewsSourceRequest newsSourceRequest) {
        return ResponseEntity.ok(newsService.updateNewsSource(sourceName, newsSourceRequest));
    }

    @PatchMapping(Constants.NEWS_SOURCE_FAILURE_PATH)
    @Operation(summary = "Increment a source's failure count", description = "Increments the consecutive scrape-failure counter for the given source. "
            + "Used by the scraper to track sources that repeatedly fail to be reached.")
    @ApiResponse(responseCode = "200", description = "Failure count incremented successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NewsSourceDto.class)))
    @ApiResponse(responseCode = "400", description = "News source does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NewsSourceDto> incrementFailureCount(
            @Parameter(description = "Name of the news source.", example = "example-news", required = true)
            @PathVariable final String sourceName) {
        return ResponseEntity.ok(newsService.incrementFailureCount(sourceName));
    }

    @PatchMapping(Constants.NEWS_SOURCE_RESET_PATH)
    @Operation(summary = "Reset a source's failure count", description = "Resets the consecutive scrape-failure counter to 0 and re-enables the "
            + "source if it had been disabled.")
    @ApiResponse(responseCode = "200", description = "Failure count reset successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NewsSourceDto.class)))
    @ApiResponse(responseCode = "400", description = "News source does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<NewsSourceDto> resetFailureCount(
            @Parameter(description = "Name of the news source.", example = "example-news", required = true)
            @PathVariable final String sourceName) {
        return ResponseEntity.ok(newsService.resetFailureCount(sourceName));
    }
}