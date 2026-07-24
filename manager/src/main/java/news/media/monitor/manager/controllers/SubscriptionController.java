package news.media.monitor.manager.controllers;

import news.media.monitor.manager.dto.requests.CreateSubscriptionRequest;
import news.media.monitor.manager.dto.responses.PageResponse;
import news.media.monitor.manager.dto.responses.SubscriptionResponse;
import news.media.monitor.manager.services.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE;
import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/users/me/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Endpoints for the currently authenticated user's own topic and story subscriptions")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(
            summary = "List current user's subscriptions",
            description = "Returns a paginated list of the currently authenticated user's topic and story subscriptions, "
                    + "sorted by creation date descending.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated subscription list returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<PageResponse<SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal String email,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default: 15)")
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(subscriptionService.getAll(email, pageOf(page), sizeOf(size))));
    }

    @PostMapping
    @Operation(
            summary = "Subscribe to a topic or story",
            description = "Subscribes the currently authenticated user to a topic or story defined in the news-provider "
                    + "service. The target is verified to exist in that service before the subscription is created.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "404", description = "The referenced topic or story does not exist"),
                    @ApiResponse(responseCode = "409", description = "Already subscribed to this topic or story"),
                    @ApiResponse(responseCode = "502", description = "The news-provider service is unavailable")
            }
    )
    public ResponseEntity<SubscriptionResponse> subscribe(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(email, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Unsubscribe",
            description = "Removes one of the currently authenticated user's own subscriptions.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Unsubscribed successfully"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "404", description = "No subscription exists with the given ID for this user")
            }
    )
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal String email,
            @Parameter(description = "ID of the subscription to remove", required = true)
            @PathVariable String id) {
        subscriptionService.unsubscribe(email, id);
        return ResponseEntity.noContent().build();
    }

    private int pageOf(Integer page) {
        return Objects.nonNull(page) ? page : DEFAULT_PAGE;
    }

    private int sizeOf(Integer size) {
        return Objects.nonNull(size) ? size : DEFAULT_PAGE_SIZE;
    }
}
