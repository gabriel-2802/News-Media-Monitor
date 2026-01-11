package app.demo.listeners;

import app.demo.entities.Article;
import app.demo.entities.Notification;
import app.demo.entities.Topic;
import app.demo.repositories.ArticleRepository;
import app.demo.services.NotificationService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Service
@Slf4j
@Data
public class NotificationListener {

    private final DataSource dataSource;
    private final NotificationService notificationService;
    private final ArticleRepository articleRepository;

    private Connection listenConnection;
    private volatile boolean running = false;
    private Thread listenerThread;

    @PostConstruct
    public void startListening() {
        running = true;
        listenerThread = new Thread(this::listen, "article-notification-listener");
        listenerThread.start();
    }

    private void listen() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                if (listenConnection == null || listenConnection.isClosed()) {
                    setupConnection();
                }

                PGConnection pgConn = listenConnection.unwrap(PGConnection.class);
                PGNotification[] notifications = pgConn.getNotifications(5000);

                if (notifications != null && notifications.length > 0) {
                    log.info("Received {} article notification(s)", notifications.length);
                    processNewArticles();
                }

            } catch (SQLException e) {
                log.error("Error in notification listener, reconnecting...", e);
                closeConnection();
                sleep(5000);
            } catch (Exception e) {
                log.error("Unexpected error in listener", e);
                sleep(5000);
            }
        }

        log.info("Article notification listener stopped");
    }

    private void setupConnection() throws SQLException {
        listenConnection = dataSource.getConnection();
        listenConnection.setAutoCommit(true);

        try (Statement stmt = listenConnection.createStatement()) {
            stmt.execute("LISTEN new_articles_ready");
            log.info("Listening on channel 'new_articles_ready'");
        }
    }

    @Transactional
    protected void processNewArticles() {
        try {
            // Find all unnotified articles
            List<Article> newArticles = articleRepository
                    .findUnnotified();

            if (newArticles.isEmpty()) {
                log.debug("No new articles to process");
                return;
            }

            log.info("Processing {} new articles for notifications", newArticles.size());

            for (Article article : newArticles) {
                try {
                    notifyUsersAboutArticle(article);

                    // Mark as notified
                    article.setNotified(true);

                } catch (Exception e) {
                    log.error("Failed to notify users about article {}", article.getId(), e);
                }
            }

            articleRepository.saveAll(newArticles);
            log.info("Completed processing {} articles", newArticles.size());

        } catch (Exception e) {
            log.error("Error processing new articles", e);
        }
    }

    private void notifyUsersAboutArticle(Article article) {
        // Find users interested in this article
        Topic topic = article.getTopic();

        if (topic == null) {
            log.debug("Article has no topic: {}", article.getTitle());
            return;
        }

        List<Long> interestedUserIds = notificationService.findUsersSubscrubedTo(topic);

        if (interestedUserIds.isEmpty()) {
            log.debug("No users interested in article: {}", article.getTitle());
            return;
        }

        // Create and save notifications for each user
        notificationService.createNotificationsForArticle(article, interestedUserIds);

        log.info("Notified {} users about article: {}",
                interestedUserIds.size(), article.getTitle());
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;

        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        closeConnection();
        log.info("Article notification listener shutdown complete");
    }

    private void closeConnection() {
        if (listenConnection != null) {
            try {
                listenConnection.close();
            } catch (SQLException e) {
                log.warn("Error closing connection", e);
            }
            listenConnection = null;
        }
    }
}