package news.media.monitor.manager.dto.responses;

import news.media.monitor.manager.models.Notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String message,
        boolean seen,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.isSeen(),
                notification.getCreatedAt()
        );
    }
}
