package news.media.monitor.manager.repositories;

import news.media.monitor.manager.models.Subscription;
import news.media.monitor.manager.models.SubscriptionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    Page<Subscription> findByUserId(Long userId, Pageable pageable);

    Optional<Subscription> findByIdAndUserId(String id, Long userId);

    boolean existsByUserIdAndTypeAndTargetId(Long userId, SubscriptionType type, String targetId);

    @Query("select s.user.id from Subscription s where s.targetId = :targetId and s.type = :type")
    List<Long> findUserIdsByTargetIdAndType(@Param("targetId") String targetId, @Param("type") SubscriptionType type);
}
