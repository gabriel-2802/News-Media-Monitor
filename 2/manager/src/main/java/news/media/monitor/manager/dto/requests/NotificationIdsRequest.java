package news.media.monitor.manager.dto.requests;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record NotificationIdsRequest(
        @NotEmpty List<String> ids
) {}
