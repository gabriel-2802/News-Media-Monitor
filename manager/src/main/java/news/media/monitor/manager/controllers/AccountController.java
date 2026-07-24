package news.media.monitor.manager.controllers;

import news.media.monitor.manager.dto.requests.ResetPasswordRequest;
import news.media.monitor.manager.dto.requests.UpdateAccountRequest;
import news.media.monitor.manager.dto.responses.UserResponse;
import news.media.monitor.manager.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Endpoints for the currently authenticated user's own account")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Returns the profile of the currently authenticated user based on the Bearer token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authenticated user returned successfully"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid Bearer token")
            }
    )
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.getByEmail(email));
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
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(userService.updateOwnAccount(email, request));
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
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.changeOwnPassword(email, request));
    }
}
