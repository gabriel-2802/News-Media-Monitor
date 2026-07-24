package news.media.monitor.manager.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        String email,
        String password,
        @Schema(hidden = true) String systemCode
) {}