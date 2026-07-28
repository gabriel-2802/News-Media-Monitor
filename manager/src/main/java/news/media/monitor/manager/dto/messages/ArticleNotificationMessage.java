package news.media.monitor.manager.dto.messages;

import news.media.monitor.manager.models.SubscriptionType;

public record ArticleNotificationMessage(
        String name,
        String articleUrl,
        SubscriptionType type
) {}
