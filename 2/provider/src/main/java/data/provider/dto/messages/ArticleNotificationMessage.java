package data.provider.dto.messages;

public record ArticleNotificationMessage(
        String name,
        String articleUrl,
        NotificationType type) {
}
