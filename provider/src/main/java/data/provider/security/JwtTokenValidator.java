package data.provider.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenValidator {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final String ERR_SECRET_NULL = "jwt.secret must not be null";
    private static final String ERR_SECRET_TOO_SHORT = "jwt.secret must be at least 32 characters for HMAC-SHA256.";
    private static final String LOG_JWT_PARSE_FAILED = "JWT parsing failed: {}";

    private final String jwtSecret;
    private SecretKey signingKey;

    public JwtTokenValidator(@Value("${jwt.secret}") final String jwtSecret) {
        this.jwtSecret = Objects.requireNonNull(jwtSecret, ERR_SECRET_NULL);
    }

    @PostConstruct
    void init() {
        final byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(ERR_SECRET_TOO_SHORT);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Optional<Claims> getClaims(final String token) {
        if (Objects.isNull(token) || token.isBlank()) {
            return Optional.empty();
        }
        try {
            final Claims payload = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(payload);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug(LOG_JWT_PARSE_FAILED, e.getMessage());
            return Optional.empty();
        }
    }
}
