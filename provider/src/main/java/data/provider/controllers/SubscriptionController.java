package data.provider.controllers;

import data.provider.dto.responses.ErrorResponse;
import data.provider.dto.responses.SubscriptionDto;
import data.provider.services.SubscriptionService;
import data.provider.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(Constants.SUBSCRIPTIONS_BASE_PATH)
@Tag(name = "Subscriptions", description = "Subscribing to and unsubscribing from stories and topics.")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping(Constants.SUBSCRIPTIONS_STORY_PATH)
    @Operation(summary = "Subscribe to a story", description = "Creates a subscription to the given story with count 1, or increments the "
            + "count if one already exists.")
    @ApiResponse(responseCode = "200", description = "Subscribed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SubscriptionDto.class)))
    @ApiResponse(responseCode = "400", description = "Story does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SubscriptionDto> subscribeToStory(
            @Parameter(description = "ID of the story to subscribe to.", required = true)
            @PathVariable final String storyId) {
        return ResponseEntity.ok(subscriptionService.subscribeToStory(storyId));
    }

    @DeleteMapping(Constants.SUBSCRIPTIONS_STORY_PATH)
    @Operation(summary = "Unsubscribe from a story", description = "Decrements the subscription count for the given story. The subscription "
            + "is deleted once the count reaches 0.")
    @ApiResponse(responseCode = "204", description = "Unsubscribed successfully")
    @ApiResponse(responseCode = "400", description = "No subscription exists for this story",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> unsubscribeFromStory(
            @Parameter(description = "ID of the story to unsubscribe from.", required = true)
            @PathVariable final String storyId) {
        subscriptionService.unsubscribeFromStory(storyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(Constants.SUBSCRIPTIONS_TOPIC_PATH)
    @Operation(summary = "Subscribe to a topic", description = "Creates a subscription to the given topic with count 1, or increments the "
            + "count if one already exists.")
    @ApiResponse(responseCode = "200", description = "Subscribed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SubscriptionDto.class)))
    @ApiResponse(responseCode = "400", description = "Topic does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SubscriptionDto> subscribeToTopic(
            @Parameter(description = "Name of the topic to subscribe to.", example = "Politics", required = true)
            @PathVariable final String topicName) {
        return ResponseEntity.ok(subscriptionService.subscribeToTopic(topicName));
    }

    @DeleteMapping(Constants.SUBSCRIPTIONS_TOPIC_PATH)
    @Operation(summary = "Unsubscribe from a topic", description = "Decrements the subscription count for the given topic. The subscription "
            + "is deleted once the count reaches 0.")
    @ApiResponse(responseCode = "204", description = "Unsubscribed successfully")
    @ApiResponse(responseCode = "400", description = "No subscription exists for this topic",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> unsubscribeFromTopic(
            @Parameter(description = "Name of the topic to unsubscribe from.", example = "Politics", required = true)
            @PathVariable final String topicName) {
        subscriptionService.unsubscribeFromTopic(topicName);
        return ResponseEntity.noContent().build();
    }
}
