package news.media.monitor.manager.controllers;

import news.media.monitor.manager.dto.requests.ResetPasswordRequest;
import news.media.monitor.manager.dto.requests.UpdateUserRequest;
import news.media.monitor.manager.dto.responses.PageResponse;
import news.media.monitor.manager.dto.responses.UserResponse;
import news.media.monitor.manager.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE;
import static news.media.monitor.manager.utils.Constants.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Endpoints for administrators to manage users, roles, and account state")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by ID",
            description = "Returns the full profile of a user by their ID. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found and returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN"),
                    @ApiResponse(responseCode = "404", description = "No user exists with the given ID")
            }
    )
    public ResponseEntity<UserResponse> getById(
            @Parameter(description = "ID of the user to retrieve", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns a paginated list of all registered users, sorted by creation date descending. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated user list returned"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN")
            }
    )
    public ResponseEntity<PageResponse<UserResponse>> getAll(
            @Parameter(description = "Zero-based page index (default: 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (default: 20)")
            @RequestParam(required = false) Integer size) {

        int p = Objects.nonNull(page) ? page : DEFAULT_PAGE;
        int s = Objects.nonNull(size) ? size : DEFAULT_PAGE_SIZE;

        return ResponseEntity.ok(PageResponse.from(userService.getAll(p, s)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update user",
            description = "Updates a user's email, name, and optionally their role. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN"),
                    @ApiResponse(responseCode = "404", description = "No user exists with the given ID"),
                    @ApiResponse(responseCode = "409", description = "Email is already in use by another account")
            }
    )
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID of the user to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/password")
    @Operation(
            summary = "Reset user password",
            description = "Replaces a user's password with the provided value. The new password is stored hashed. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password reset successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN"),
                    @ApiResponse(responseCode = "404", description = "No user exists with the given ID")
            }
    )
    public ResponseEntity<UserResponse> resetPassword(
            @Parameter(description = "ID of the user whose password to reset", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete user",
            description = "Permanently deletes a user account and all associated data. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN"),
                    @ApiResponse(responseCode = "404", description = "No user exists with the given ID")
            }
    )
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to delete", required = true)
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles/admin")
    @Operation(
            summary = "Grant admin role",
            description = "Adds ROLE_ADMIN to the specified user. Has no effect if the user already holds ROLE_ADMIN. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ROLE_ADMIN granted successfully"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN"),
                    @ApiResponse(responseCode = "404", description = "No user exists with the given ID")
            }
    )
    public ResponseEntity<UserResponse> grantAdmin(
            @Parameter(description = "ID of the user to promote", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.grantAdmin(id));
    }

    @DeleteMapping("/{id}/roles/admin")
    @Operation(
            summary = "Revoke admin role",
            description = "Removes ROLE_ADMIN from the specified user. Has no effect if the user does not hold ROLE_ADMIN. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ROLE_ADMIN revoked successfully"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN"),
                    @ApiResponse(responseCode = "404", description = "No user exists with the given ID")
            }
    )
    public ResponseEntity<UserResponse> revokeAdmin(
            @Parameter(description = "ID of the user to demote", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.revokeAdmin(id));
    }

    @PatchMapping("/{id}/enabled")
    @Operation(
            summary = "Set user enabled state",
            description = "Enables or disables a user account. Disabled users cannot authenticate. Restricted to administrators.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User enabled state updated successfully"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token"),
                    @ApiResponse(responseCode = "403", description = "Caller does not have ROLE_ADMIN"),
                    @ApiResponse(responseCode = "404", description = "No user exists with the given ID")
            }
    )
    public ResponseEntity<UserResponse> setEnabled(
            @Parameter(description = "ID of the user to enable or disable", required = true)
            @PathVariable Long id,
            @Parameter(description = "Set to true to enable the account, false to disable", required = true)
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setEnabled(id, enabled));
    }
}
