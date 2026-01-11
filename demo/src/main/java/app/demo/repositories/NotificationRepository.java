package app.demo.repositories;

import app.demo.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
        SELECT n
        FROM Notification n
        JOIN n.user u
        LEFT JOIN FETCH n.article
        WHERE u.username = :username
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByUsernameWithArticle(@Param("username") String username);
}
