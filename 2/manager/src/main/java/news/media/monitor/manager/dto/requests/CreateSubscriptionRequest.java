package news.media.monitor.manager.dto.requests;

import news.media.monitor.manager.models.SubscriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubscriptionRequest(
        @NotNull SubscriptionType type,
        @NotBlank @Size(max = 255) String targetId
) {}
