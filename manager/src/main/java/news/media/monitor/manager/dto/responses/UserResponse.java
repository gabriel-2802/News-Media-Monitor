package news.media.monitor.manager.dto.responses;

import news.media.monitor.manager.models.Role;
import news.media.monitor.manager.models.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String email,
        String name,
        boolean enabled,
        Set<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toUnmodifiableSet()),
                user.getCreatedAt()
        );
    }
}
