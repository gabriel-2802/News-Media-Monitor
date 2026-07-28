package news.media.monitor.manager.services;

import news.media.monitor.manager.dto.requests.ResetPasswordRequest;
import news.media.monitor.manager.dto.requests.UpdateAccountRequest;
import news.media.monitor.manager.dto.requests.UpdateUserRequest;
import news.media.monitor.manager.dto.responses.UserResponse;
import news.media.monitor.manager.exceptions.exceptions.DuplicateEmailException;
import news.media.monitor.manager.exceptions.exceptions.ResourceNotFoundException;
import news.media.monitor.manager.models.RoleName;
import news.media.monitor.manager.models.User;
import news.media.monitor.manager.repositories.RoleRepository;
import news.media.monitor.manager.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ROLE_USER_MISSING     = "ROLE_USER missing";
    private static final String ROLE_ADMIN_MISSING    = "ROLE_ADMIN missing";
    private static final String USER_NOT_FOUND        = "User not found: ";
    private static final String UNKNOWN_ROLE          = "Unknown role: ";
    private static final String SORT_FIELD_CREATED_AT = "createdAt";

    private static final String LOG_USER_UPDATED      = "User {} updated — email: {}, name: {}";
    private static final String LOG_PASSWORD_RESET    = "Password reset for user {}";
    private static final String LOG_USER_DELETED      = "User {} deleted";
    private static final String LOG_ADMIN_GRANTED     = "ROLE_ADMIN granted to user {}";
    private static final String LOG_ADMIN_REVOKED     = "ROLE_ADMIN revoked from user {}";
    private static final String LOG_ENABLED_SET       = "User {} enabled state set to {}";
    private static final String LOG_ROLE_APPLIED      = "Role '{}' applied to user {}";

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + email));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, SORT_FIELD_CREATED_AT));
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findOrThrow(id);

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        user.setEmail(request.email());
        user.setName(request.name());

        if (Objects.nonNull(request.role())) {
            applyRole(user, request.role());
        }

        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info(LOG_USER_UPDATED, id, request.email(), request.name());
        return response;
    }

    @Transactional
    public UserResponse updateOwnAccount(String email, UpdateAccountRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + email));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        user.setEmail(request.email());
        user.setName(request.name());

        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info(LOG_USER_UPDATED, user.getId(), request.email(), request.name());
        return response;
    }

    @Transactional
    public UserResponse changeOwnPassword(String email, ResetPasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + email));
        user.setPassword(passwordEncoder.encode(request.password()));
        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info(LOG_PASSWORD_RESET, user.getId());
        return response;
    }

    @Transactional
    public UserResponse resetPassword(Long id, ResetPasswordRequest request) {
        User user = findOrThrow(id);
        user.setPassword(passwordEncoder.encode(request.password()));
        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info(LOG_PASSWORD_RESET, id);
        return response;
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(findOrThrow(id));
        log.info(LOG_USER_DELETED, id);
    }

    @Transactional
    public UserResponse grantAdmin(Long id) {
        User user = findOrThrow(id);
        var adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(ROLE_ADMIN_MISSING));
        user.getRoles().add(adminRole);
        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info(LOG_ADMIN_GRANTED, id);
        return response;
    }

    @Transactional
    public UserResponse revokeAdmin(Long id) {
        User user = findOrThrow(id);
        user.getRoles().removeIf(r -> r.getName().equals(RoleName.ADMIN));
        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info(LOG_ADMIN_REVOKED, id);
        return response;
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = findOrThrow(id);
        user.setEnabled(enabled);
        UserResponse response = UserResponse.from(userRepository.save(user));
        log.info(LOG_ENABLED_SET, id, enabled);
        return response;
    }

    private void applyRole(User user, String role) {
        var userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(ROLE_USER_MISSING));
        user.getRoles().clear();
        user.getRoles().add(userRole);

        switch (role) {
            case RoleName.ADMIN_SHORT   -> roleRepository.findByName(RoleName.ADMIN).ifPresent(user.getRoles()::add);
            case RoleName.SYSTEM_SHORT  -> roleRepository.findByName(RoleName.SYSTEM).ifPresent(user.getRoles()::add);
            case RoleName.USER_SHORT    -> { /* ROLE_USER already added above */ }
            default                     -> throw new IllegalArgumentException(UNKNOWN_ROLE + role);
        }

        log.debug(LOG_ROLE_APPLIED, role, user.getId());
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
    }
}