package data.provider.controllers;

import data.provider.dto.requests.AttachArticleRequest;
import data.provider.dto.responses.ErrorResponse;
import data.provider.dto.responses.StoryDto;
import data.provider.services.StoryService;
import data.provider.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
@RequestMapping(Constants.STORIES_BASE_PATH)
@Tag(name = "Stories", description = "Managing story clusters and their article membership.")
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    @Operation(summary = "List all stories", description = "Returns a paginated list of all story clusters, ordered by most recently updated.")
    @ApiResponse(responseCode = "200", description = "Stories retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = StoryDto.class))))
    public ResponseEntity<List<StoryDto>> getStories(
            @Parameter(description = "Zero-based page index.", example = "0")
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE) final int page,
            @Parameter(description = "Number of stories per page.", example = "20")
            @RequestParam(defaultValue = Constants.DEFAULT_COUNT) final int count) {
        return ResponseEntity.ok(storyService.getStories(page, count));
    }

    @GetMapping(Constants.STORIES_RECENT_PATH)
    @Operation(summary = "List recent stories", description = "Returns stories that had activity within the last N days. Used to identify live clustering candidates.")
    @ApiResponse(responseCode = "200", description = "Recent stories retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = StoryDto.class))))
    public ResponseEntity<List<StoryDto>> getRecentStories(
            @Parameter(description = "Look-back window in days.", example = "5")
            @RequestParam(defaultValue = "7") final int days) {
        return ResponseEntity.ok(storyService.getRecentStories(days));
    }

    @PostMapping
    @Operation(summary = "Create a new story", description = "Creates a new empty story cluster with the given title. Returns the generated story ID and metadata.")
    @ApiResponse(responseCode = "201", description = "Story created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StoryDto.class)))
    @ApiResponse(responseCode = "400", description = "Title is blank",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<StoryDto> createStory(
            @Parameter(description = "Title for the new story cluster.", example = "Senate passes new budget bill", required = true)
            @RequestParam @NotBlank final String title) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storyService.createStory(title));
    }

    @PatchMapping(Constants.STORIES_ATTACH_PATH)
    @Operation(summary = "Attach an article to a story", description = "Creates a BELONGS_TO edge from the article to the story. "
            + "Increments articleCount unconditionally and sourceCount only if the article's source has not previously contributed to this story. "
            + "Idempotent — re-attaching the same article has no effect.")
    @ApiResponse(responseCode = "200", description = "Article attached successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = StoryDto.class)))
    @ApiResponse(responseCode = "400", description = "Story or article does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<StoryDto> attachArticle(
            @Parameter(description = "ID of the target story.", required = true)
            @PathVariable final String storyId,
            @Valid @RequestBody final AttachArticleRequest request) {
        return ResponseEntity.ok(storyService.attachArticle(storyId, request));
    }
}