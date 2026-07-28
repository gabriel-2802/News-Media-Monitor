package news.media.monitor.manager.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Email String email,
        @Size(max = 255) String name,
        @Pattern(regexp = "ADMIN|MANAGER|USER", message = "role must be ADMIN, MANAGER, or USER") String role
) {}
