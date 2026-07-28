package news.media.monitor.manager.dto.responses;

import news.media.monitor.manager.models.Subscription;
import news.media.monitor.manager.models.SubscriptionType;

import java.time.Instant;

public record SubscriptionResponse(
        String id,
        SubscriptionType type,
        String targetId,
        Instant createdAt
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getType(),
                subscription.getTargetId(),
                subscription.getCreatedAt()
        );
    }
}
