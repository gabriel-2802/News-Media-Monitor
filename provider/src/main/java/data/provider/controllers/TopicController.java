package data.provider.controllers;

import data.provider.dto.responses.TopicDto;
import data.provider.services.TopicService;
import data.provider.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(Constants.TOPICS_BASE_PATH)
@Tag(name = "Topics", description = "Querying topics derived from tagged articles.")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "List all topics", description = "Returns every topic that has been assigned to at least one article, along with its article count.")
    @ApiResponse(responseCode = "200", description = "Topics retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = TopicDto.class))))
    public ResponseEntity<List<TopicDto>> getAllTopics() {
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping(Constants.TOPICS_EXISTS_PATH)
    @Operation(summary = "Check topic existence", description = "Returns whether a topic with the given name already exists.")
    @ApiResponse(responseCode = "200", description = "Existence check result",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Boolean.class)))
    public ResponseEntity<Boolean> exists(
            @Parameter(description = "Name of the topic to check.", example = "Politics", required = true)
            @RequestParam final String name) {
        return ResponseEntity.ok(topicService.exists(name));
    }
}
