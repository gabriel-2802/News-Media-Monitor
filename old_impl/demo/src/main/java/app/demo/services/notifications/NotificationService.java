package app.demo.services.notifications;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<User> subscribedUsers = userRepository.findBySubscribedTopicsContaining(topic);

        return subscribedUsers.stream()
                .map(User::getId)
                .toList();
    }

    /**
     * Batch method: Find subscribers for multiple articles at once
     * Groups articles by topic to minimize database queries
     */
    public Map<Long, List<Long>> findSubscribersByArticleObjects(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<Long>> result = new HashMap<>();

        // Group articles by topic to minimize queries
        Map<Topic, List<Article>> articlesByTopic = articles.stream()
                .filter(a -> a.getTopic() != null)
                .collect(Collectors.groupingBy(Article::getTopic));

        // For each topic, find subscribers once and map to all articles
        for (Map.Entry<Topic, List<Article>> entry : articlesByTopic.entrySet()) {
            Topic topic = entry.getKey();
            List<Article> topicArticles = entry.getValue();

            List<Long> subscriberIds = findUsersSubscrubedTo(topic);

            // Map the same subscriber list to all articles in this topic
            for (Article article : topicArticles) {
                result.put(article.getId(), new ArrayList<>(subscriberIds));
            }
        }

        // Handle articles without topics
        articles.stream()
                .filter(a -> a.getTopic() == null)
                .forEach(a -> result.put(a.getId(), List.of()));

        return result;
    }

    /**
     * Create notification entities without saving them
     * Uses getReferenceById to avoid fetching User entities from database
     */
    public List<Notification> createNotificationEntities(Article article, List<Long> userIds) {
        if (article == null || userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        List<Notification> notifications = new ArrayList<>();
        String message = buildNotificationMessage(article);

        for (Long userId : userIds) {
            Notification notification = new Notification();

            // getReferenceById creates a proxy without hitting the database
            // The actual User won't be fetched unless you access its properties
            User userReference = userRepository.getReferenceById(userId);
            notification.setUser(userReference);

            notification.setArticle(article);
            notification.setMessage(message);
            notification.setRead(false);

            notifications.add(notification);
        }

        return notifications;
    }

    /**
     * Batch save notifications
     */
    @Transactional
    public void saveAllNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        notificationRepository.saveAll(notifications);
        log.info("Saved {} notifications", notifications.size());
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use createNotificationEntities + saveAllNotifications for better performance
     */
    @Transactional
    @Deprecated
    public void createNotificationsForArticle(Article article, List<Long> userIds) {
        List<Notification> notifications = createNotificationEntities(article, userIds);
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