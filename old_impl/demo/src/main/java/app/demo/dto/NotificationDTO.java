package app.demo.dto;

import java.time.Instant;

public record NotificationDTO(
        Long id,
        String message,
        Instant createdAt,
        boolean isRead,
        Long articleId,
        String articleTitle
) {}
