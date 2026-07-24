package news.media.monitor.manager.messaging;

import news.media.monitor.manager.dto.messages.ArticleNotificationMessage;
import news.media.monitor.manager.repositories.SubscriptionRepository;
import news.media.monitor.manager.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleNotificationListener {

    private static final String MESSAGE_TEMPLATE = "New article for %s '%s': %s";
    private static final String LOG_RECEIVED = "Received article notification for {} '{}'";
    private static final String LOG_NO_SUBSCRIBERS = "No subscribers found for {} '{}'";

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.article-notifications-queue}")
    public void onArticleNotification(ArticleNotificationMessage message) {
        log.info(LOG_RECEIVED, message.type(), message.name());

        List<Long> userIds = subscriptionRepository.findUserIdsByTargetIdAndType(message.name(), message.type());
        if (userIds.isEmpty()) {
            log.debug(LOG_NO_SUBSCRIBERS, message.type(), message.name());
            return;
        }

        String text = MESSAGE_TEMPLATE.formatted(message.type(), message.name(), message.articleUrl());
        notificationService.createForUsers(userIds, text);
    }
}
