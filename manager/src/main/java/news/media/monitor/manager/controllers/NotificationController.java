package news.media.monitor.manager.controllers;

import news.media.monitor.manager.dto.requests.NotificationIdsRequest;
import news.media.monitor.manager.dto.responses.NotificationResponse;
import news.media.monitor.manager.dto.responses.PageResponse;
import news.media.monitor.manager.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE;
import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/users/me/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for the currently authenticated user's own notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "List current user's notifications",
            description = "Returns a paginated list of all notifications belonging to the currently authenticated user, "
                    + "seen and unseen, sorted by creation date descending.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated notification list returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal String email,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default: 15)")
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(notificationService.getAll(email, pageOf(page), sizeOf(size))));
    }

    @GetMapping("/unseen")
    @Operation(
            summary = "List current user's unseen notifications",
            description = "Returns a paginated list of notifications belonging to the currently authenticated user "
                    + "that have not been marked as seen, sorted by creation date descending.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated unseen notification list returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<PageResponse<NotificationResponse>> getMyUnseenNotifications(
            @AuthenticationPrincipal String email,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default: 15)")
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(PageResponse.from(notificationService.getUnseen(email, pageOf(page), sizeOf(size))));
    }

    @PatchMapping("/seen")
    @Operation(
            summary = "Mark notifications as seen",
            description = "Marks one or more of the currently authenticated user's own notifications as seen. "
                    + "IDs that don't exist or don't belong to the caller are silently ignored.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Notifications marked as seen successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<Void> markNotificationsSeen(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody NotificationIdsRequest request) {
        notificationService.markSeen(email, request.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(
            summary = "Delete notifications",
            description = "Permanently deletes one or more of the currently authenticated user's own notifications. "
                    + "IDs that don't exist or don't belong to the caller are silently ignored.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Notifications deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<Void> deleteNotifications(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody NotificationIdsRequest request) {
        notificationService.delete(email, request.ids());
        return ResponseEntity.noContent().build();
    }

    private int pageOf(Integer page) {
        return Objects.nonNull(page) ? page : DEFAULT_PAGE;
    }

    private int sizeOf(Integer size) {
        return Objects.nonNull(size) ? size : DEFAULT_PAGE_SIZE;
    }
}
