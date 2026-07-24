package news.media.monitor.manager.repositories;

import news.media.monitor.manager.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndSeenFalse(Long userId, Pageable pageable);

    @Modifying
    @Query("update Notification n set n.seen = true where n.user.id = :userId and n.id in :ids")
    int markSeen(@Param("userId") Long userId, @Param("ids") List<String> ids);

    long deleteByUserIdAndIdIn(Long userId, List<String> ids);
}
