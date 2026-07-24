package news.media.monitor.manager.controllers;

import news.media.monitor.manager.dto.requests.NotificationIdsRequest;
import news.media.monitor.manager.dto.requests.ResetPasswordRequest;
import news.media.monitor.manager.dto.requests.UpdateAccountRequest;
import news.media.monitor.manager.dto.responses.NotificationResponse;
import news.media.monitor.manager.dto.responses.UserResponse;
import news.media.monitor.manager.services.NotificationService;
import news.media.monitor.manager.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE;
import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Endpoints for the currently authenticated user's own account")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final UserService         userService;
    private final NotificationService notificationService;

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Returns the profile of the currently authenticated user based on the Bearer token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authenticated user returned successfully"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getByEmail(userDetails.getUsername()));
    }

    @PutMapping("/me")
    @Operation(
            summary = "Update current user",
            description = "Updates the email and name of the currently authenticated user's own account.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "409", description = "Email is already in use by another account")
            }
    )
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(userService.updateOwnAccount(userDetails.getUsername(), request));
    }

    @PatchMapping("/me/password")
    @Operation(
            summary = "Change current user's password",
            description = "Replaces the currently authenticated user's own password with the provided value. The new password is stored hashed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password changed successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<UserResponse> changeMyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.changeOwnPassword(userDetails.getUsername(), request));
    }

    @GetMapping("/me/notifications")
    @Operation(
            summary = "List current user's notifications",
            description = "Returns a paginated list of all notifications belonging to the currently authenticated user, "
                    + "seen and unseen, sorted by creation date descending.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated notification list returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default: 15)")
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(notificationService.getAll(userDetails.getUsername(), pageOf(page), sizeOf(size)));
    }

    @GetMapping("/me/notifications/unseen")
    @Operation(
            summary = "List current user's unseen notifications",
            description = "Returns a paginated list of notifications belonging to the currently authenticated user "
                    + "that have not been marked as seen, sorted by creation date descending.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated unseen notification list returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<Page<NotificationResponse>> getMyUnseenNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default: 15)")
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(notificationService.getUnseen(userDetails.getUsername(), pageOf(page), sizeOf(size)));
    }

    @PatchMapping("/me/notifications/seen")
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
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NotificationIdsRequest request) {
        notificationService.markSeen(userDetails.getUsername(), request.ids());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/notifications")
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
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NotificationIdsRequest request) {
        notificationService.delete(userDetails.getUsername(), request.ids());
        return ResponseEntity.noContent().build();
    }

    private int pageOf(Integer page) {
        return Objects.nonNull(page) ? page : DEFAULT_PAGE;
    }

    private int sizeOf(Integer size) {
        return Objects.nonNull(size) ? size : DEFAULT_PAGE_SIZE;
    }
}
