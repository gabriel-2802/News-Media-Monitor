package news.media.monitor.manager.services;

import news.media.monitor.manager.dto.requests.LoginRequest;
import news.media.monitor.manager.dto.requests.RegisterRequest;
import news.media.monitor.manager.dto.responses.AuthResponse;
import news.media.monitor.manager.dto.responses.UserResponse;
import news.media.monitor.manager.exceptions.exceptions.DuplicateEmailException;
import news.media.monitor.manager.exceptions.exceptions.InvalidCredentialsException;
import news.media.monitor.manager.models.RoleName;
import news.media.monitor.manager.models.User;
import news.media.monitor.manager.repositories.RoleRepository;
import news.media.monitor.manager.repositories.UserRepository;
import news.media.monitor.manager.security.JwtTokenProvider;
import news.media.monitor.manager.security.UserDetailsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class AuthService {

    private static final String ROLE_USER_NOT_FOUND = "ROLE_USER not found — check DB migrations";
    private static final String LOG_ADMIN_GRANTED   = "Granted ROLE_ADMIN to '{}'";
    private static final String LOG_USER_REGISTERED = "Registered new user '{}'";
    private static final String LOG_TOKEN_ISSUED    = "Issued token for '{}'";
    private static final String LOG_SYSTEM_TOKEN_ISSUED = "Issued ROLE_SYSTEM token";
    private static final String AUTHENTICATION_PRINCIPAL_NON_NULL = "Authentication principal must not be null";
    private static final String SYSTEM_TOKEN_SUBJECT = "system";

    private static final String PROP_ADMIN_CODE     = "${app.admin.register-code:devcode123}";
    private static final String PROP_SYSTEM_CODE    = "${app.system.code:devsystemcode123}";
    private static final String PROP_JWT_EXPIRATION = "${jwt.expiration:2592000000}";

    private final UserRepository       userRepository;
    private final RoleRepository       roleRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider     tokenProvider;
    private final String               adminRegisterCode;
    private final String               systemCode;
    private final long                 jwtExpirationMs;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       @Value(PROP_ADMIN_CODE)     String adminRegisterCode,
                       @Value(PROP_SYSTEM_CODE)    String systemCode,
                       @Value(PROP_JWT_EXPIRATION) long jwtExpirationMs) {
        this.userRepository        = userRepository;
        this.roleRepository        = roleRepository;
        this.passwordEncoder       = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider         = tokenProvider;
        this.adminRegisterCode     = adminRegisterCode;
        this.systemCode            = systemCode;
        this.jwtExpirationMs       = jwtExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        User user = buildUser(request);

        if (StringUtils.hasText(request.adminCode()) && isAdminCode(request.adminCode())) {
            roleRepository.findByName(RoleName.ADMIN).ifPresent(user.getRoles()::add);
            log.info(LOG_ADMIN_GRANTED, request.email());
        }

        userRepository.save(user);
        log.info(LOG_USER_REGISTERED, request.email());

        UserDetails userDetails = UserDetailsServiceImpl.toUserDetails(user);
        String token = tokenProvider.generateToken(userDetails, user.getId());
        return AuthResponse.of(token, jwtExpirationMs, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (StringUtils.hasText(request.systemCode()) && !StringUtils.hasText(request.password())) {
            return loginSystem(request.systemCode());
        }

        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new InvalidCredentialsException();
        }

        UserDetails userDetails;
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            userDetails = Objects.requireNonNull(
                    (UserDetails) authentication.getPrincipal(),
                    AUTHENTICATION_PRINCIPAL_NON_NULL
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        String token = tokenProvider.generateToken(userDetails, user.getId());
        log.debug(LOG_TOKEN_ISSUED, request.email());
        return AuthResponse.of(token, jwtExpirationMs, UserResponse.from(user));
    }

    private AuthResponse loginSystem(String providedCode) {
        if (!isSystemCode(providedCode)) {
            throw new InvalidCredentialsException();
        }

        String token = tokenProvider.generateToken(SYSTEM_TOKEN_SUBJECT, List.of(RoleName.SYSTEM));
        log.info(LOG_SYSTEM_TOKEN_ISSUED);
        return AuthResponse.of(token, jwtExpirationMs, null);
    }

    private boolean isSystemCode(String provided) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                systemCode.getBytes(StandardCharsets.UTF_8)
        );
    }

    private User buildUser(RegisterRequest request) {
        var userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException(ROLE_USER_NOT_FOUND));

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.getRoles().add(userRole);
        return user;
    }

    private boolean isAdminCode(String provided) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                adminRegisterCode.getBytes(StandardCharsets.UTF_8)
        );
    }
}