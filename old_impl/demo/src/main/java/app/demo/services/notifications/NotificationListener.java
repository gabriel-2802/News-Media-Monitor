package app.demo.services.notifications;

import app.demo.listeners.NotificationProcessor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Listens for PostgreSQL notifications about new articles and delegates
 * processing to NotificationProcessor for proper transaction management
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Data
public class NotificationListener {

    private final DataSource dataSource;
    private final NotificationProcessor notificationProcessor;

    private Connection listenConnection;
    private volatile boolean running = false;
    private Thread listenerThread;

    @PostConstruct
    public void startListening() {
        running = true;
        listenerThread = new Thread(this::listen, "article-notification-listener");
        listenerThread.setDaemon(false);
        listenerThread.start();
        log.info("Article notification listener started");
    }

    private void listen() {
        int errorCount = 0;
        final int MAX_ERRORS = 5;

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                ensureConnection();

                PGConnection pgConn = listenConnection.unwrap(PGConnection.class);
                PGNotification[] notifications = pgConn.getNotifications(5000);

                if (notifications != null && notifications.length > 0) {
                    log.info("Received {} notification(s)", notifications.length);

                    // delegate to Spring-managed bean for proper transaction handling
                    notificationProcessor.processNewArticles();

                    // reset error count on successful processing
                    errorCount = 0;
                }

            } catch (SQLException e) {
                errorCount++;
                log.error("Database error in listener (attempt {}/{}), reconnecting...",
                        errorCount, MAX_ERRORS, e);
                closeConnection();

                if (errorCount >= MAX_ERRORS) {
                    log.error("Max error count reached, stopping listener");
                    running = false;
                    break;
                }

                sleep(calculateBackoff(errorCount));

            } catch (Exception e) {
                errorCount++;
                log.error("Unexpected error in listener (attempt {}/{})",
                        errorCount, MAX_ERRORS, e);

                if (errorCount >= MAX_ERRORS) {
                    log.error("Max error count reached, stopping listener");
                    running = false;
                    break;
                }

                sleep(calculateBackoff(errorCount));
            }
        }

        log.info("Article notification listener stopped");
    }

    private void ensureConnection() throws SQLException {
        if (listenConnection == null || listenConnection.isClosed()) {
            listenConnection = dataSource.getConnection();
            listenConnection.setAutoCommit(true);

            try (Statement stmt = listenConnection.createStatement()) {
                stmt.execute("LISTEN new_articles_ready");
                log.info("Listening on channel 'new_articles_ready'");
            }
        }
    }

    /**
     * Calculate exponential backoff with jitter
     */
    private long calculateBackoff(int errorCount) {
        long baseDelay = 1000; // 1 second
        long maxDelay = 30000; // 30 seconds
        long delay = Math.min(baseDelay * (long) Math.pow(2, errorCount - 1), maxDelay);

        // Add jitter (±25%)
        long jitter = (long) (delay * 0.25 * (Math.random() - 0.5));
        return delay + jitter;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Sleep interrupted, shutting down listener");
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down article notification listener...");
        running = false;

        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(5000);
                if (listenerThread.isAlive()) {
                    log.warn("Listener thread did not stop within timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for listener thread to stop");
            }
        }

        closeConnection();
        log.info("Article notification listener shutdown complete");
    }

    private void closeConnection() {
        if (listenConnection != null) {
            try {
                listenConnection.close();
                log.debug("Closed database connection");
            } catch (SQLException e) {
                log.warn("Error closing connection", e);
            }
            listenConnection = null;
        }
    }
}