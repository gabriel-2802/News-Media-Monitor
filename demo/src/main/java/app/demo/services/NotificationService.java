package app.demo.services;


import app.demo.entities.Article;
import app.demo.entities.Notification;
import app.demo.entities.Topic;
import app.demo.entities.User;
import app.demo.repositories.NotificationRepository;
import app.demo.repositories.UserRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Data
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Find users who should be notified about this article based on topic subscriptions
     */
    public List<Long> findUsersSubscrubedTo(Topic topic) {
        if (topic == null) {
            return List.of();
        }

        // Find all users subscribed to this topic
        List<User> subscribedUsers = userRepository.findBySubscribedTopicsContaining(topic);

        return subscribedUsers.stream()
                .map(User::getId)
                .toList();
    }

    /**
     * Create notifications for multiple users about an article
     */
    @Transactional
    public void createNotificationsForArticle(Article article, List<Long> userIds) {
        if (article == null || userIds == null || userIds.isEmpty()) {
            return;
        }

        List<Notification> notifications = new ArrayList<>();

        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found: {}", userId);
                continue;
            }

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setArticle(article);
            notification.setMessage(buildNotificationMessage(article));
            notification.setRead(false);

            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.debug("Created {} notifications for article {}", notifications.size(), article.getId());
        }
    }

    /**
     * Build a notification message for an article
     */
    private String buildNotificationMessage(Article article) {
        String topicName = article.getTopic() != null ? article.getTopic().getName() : "Unknown";
        return String.format("New article in %s: %s", topicName, article.getTitle());
    }
}