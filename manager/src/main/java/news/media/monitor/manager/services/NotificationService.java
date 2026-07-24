package news.media.monitor.manager.services;

import news.media.monitor.manager.dto.responses.NotificationResponse;
import news.media.monitor.manager.exceptions.exceptions.ResourceNotFoundException;
import news.media.monitor.manager.repositories.NotificationRepository;
import news.media.monitor.manager.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String USER_NOT_FOUND        = "User not found: ";
    private static final String SORT_FIELD_CREATED_AT = "createdAt";

    private static final String LOG_MARKED_SEEN = "Marked {} notification(s) as seen for user {}";
    private static final String LOG_DELETED     = "Deleted {} notification(s) for user {}";

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAll(String email, int page, int size) {
        Long userId = resolveUserId(email);
        return notificationRepository.findByUserId(userId, pageable(page, size))
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnseen(String email, int page, int size) {
        Long userId = resolveUserId(email);
        return notificationRepository.findByUserIdAndSeenFalse(userId, pageable(page, size))
                .map(NotificationResponse::from);
    }

    @Transactional
    public void markSeen(String email, List<String> ids) {
        Long userId = resolveUserId(email);
        int updated = notificationRepository.markSeen(userId, ids);
        log.info(LOG_MARKED_SEEN, updated, userId);
    }

    @Transactional
    public void delete(String email, List<String> ids) {
        Long userId = resolveUserId(email);
        long deleted = notificationRepository.deleteByUserIdAndIdIn(userId, ids);
        log.info(LOG_DELETED, deleted, userId);
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, SORT_FIELD_CREATED_AT));
    }

    private Long resolveUserId(String email) {
        return userRepository.findIdByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + email));
    }
}
