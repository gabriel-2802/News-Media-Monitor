package news.media.monitor.manager.controllers;

import news.media.monitor.manager.dto.requests.LoginRequest;
import news.media.monitor.manager.dto.requests.RegisterRequest;
import news.media.monitor.manager.dto.responses.AuthResponse;
import news.media.monitor.manager.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Endpoints for user registration and authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with ROLE_USER. Optionally grants ROLE_ADMIN if a valid admin code is provided.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User registered successfully, JWT returned"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "409", description = "Email is already registered")
            }
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user or service",
            description = "Validates email/password credentials and returns a signed JWT token along with the authenticated "
                    + "user's profile. Alternatively, if no password is provided but a valid systemCode is, returns a "
                    + "JWT token carrying ROLE_SYSTEM (no user profile attached) for service-to-service calls.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication successful, JWT returned"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "401", description = "Invalid email/password or system code")
            }
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}