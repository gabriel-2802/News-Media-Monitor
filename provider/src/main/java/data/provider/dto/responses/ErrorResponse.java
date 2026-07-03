package data.provider.dto.responses;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Schema(description = "Standard error payload returned for all non-2xx responses.")
public record ErrorResponse(

        @Schema(description = "When the error occurred.", example = "2026-07-03T10:15:30")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code.", example = "400")
        int status,

        @Schema(description = "Short HTTP status reason.", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable error message.", example = "Validation failed")
        String message,

        @Schema(description = "Field-level validation error details, keyed by field name. Empty when not applicable.")
        Map<String, String> details
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, Collections.emptyMap());
    }

    public static ErrorResponse of(int status, String error, String message, Map<String, String> details) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, details);
    }
}
