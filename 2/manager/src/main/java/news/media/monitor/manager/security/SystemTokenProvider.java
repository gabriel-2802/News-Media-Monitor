package news.media.monitor.manager.security;

import news.media.monitor.manager.models.RoleName;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates a ROLE_SYSTEM JWT once at application startup, for the manager to
 * authenticate its own outbound calls to the news-provider service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemTokenProvider {

    private static final String SYSTEM_TOKEN_SUBJECT = "system";
    private static final String LOG_SYSTEM_TOKEN_GENERATED = "Generated system JWT for calling news-provider";

    private final JwtTokenProvider tokenProvider;

    @Getter
    private String token;

    @PostConstruct
    void generate() {
        token = tokenProvider.generateToken(SYSTEM_TOKEN_SUBJECT, List.of(RoleName.SYSTEM));
        log.info(LOG_SYSTEM_TOKEN_GENERATED);
    }
}