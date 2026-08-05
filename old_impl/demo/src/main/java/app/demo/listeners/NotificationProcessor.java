package app.demo.listeners;

import app.demo.entities.Article;
import app.demo.entities.Notification;
import app.demo.repositories.ArticleRepository;
import app.demo.services.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes new articles and creates notifications for subscribed users.
 * Separated from NotificationListener to ensure proper Spring transaction management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationProcessor {

    private final ArticleRepository articleRepository;
    private final NotificationService notificationService;

    /**
     * Process all unnotified articles in a single transaction.
     * Uses pessimistic locking (FOR UPDATE SKIP LOCKED) to prevent race conditions
     * when multiple application instances are running.
     */
    @Transactional
    public void processNewArticles() {
        try {
            // fetch unnotified articles with pessimistic lock
            // SKIP LOCKED ensures we don't wait for other instances
            List<Article> newArticles = articleRepository.findUnnotifiedWithLock();

            if (newArticles.isEmpty()) {
                log.debug("No new articles to process");
                return;
            }

            log.info("Processing {} new articles for notifications", newArticles.size());

            Map<Long, List<Long>> articleToSubscribers =
                    notificationService.findSubscribersByArticleObjects(newArticles);

            // collect all notifications to save in one batch
            List<Notification> allNotifications = new ArrayList<>();
            int articleWithSubscribers = 0;

            for (Article article : newArticles) {
                List<Long> subscriberIds = articleToSubscribers
                        .getOrDefault(article.getId(), List.of());

                if (!subscriberIds.isEmpty()) {
                    // create notification entities (using User proxies, no DB queries)
                    List<Notification> notifications =
                            notificationService.createNotificationEntities(article, subscriberIds);
                    allNotifications.addAll(notifications);
                    articleWithSubscribers++;
                }

                // mark article as notified
                article.setNotified(true);
            }

            // batch save all notifications at once
            if (!allNotifications.isEmpty()) {
                notificationService.saveAllNotifications(allNotifications);
            }

            // hatch save all updated articles
            articleRepository.saveAll(newArticles);

            log.info("Completed processing: {} articles, {} with subscribers, {} total notifications created",
                    newArticles.size(), articleWithSubscribers, allNotifications.size());

        } catch (Exception e) {
            log.error("Error processing new articles", e);
            // transaction will roll back automatically
            throw e;
        }
    }

    /**
     * Process a single article (useful for testing or manual triggers)
     */
    @Transactional
    public void processSingleArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleId));

        if (article.isNotified()) {
            log.info("Article {} already notified", articleId);
            return;
        }

        List<Long> subscriberIds = notificationService
                .findUsersSubscrubedTo(article.getTopic());

        if (subscriberIds.isEmpty()) {
            log.info("No subscribers for article {}", articleId);
        } else {
            List<Notification> notifications = notificationService
                    .createNotificationEntities(article, subscriberIds);
            notificationService.saveAllNotifications(notifications);
            log.info("Created {} notifications for article {}", notifications.size(), articleId);
        }

        article.setNotified(true);
        articleRepository.save(article);
    }
}