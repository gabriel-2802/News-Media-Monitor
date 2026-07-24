package news.media.monitor.manager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final int    MIN_SECRET_LENGTH      = 32;
    private static final String CLAIMS_KEY_ROLES       = "roles";
    private static final String PROP_JWT_SECRET        = "${jwt.secret}";
    private static final String PROP_JWT_EXPIRATION    = "${jwt.expiration}";
    private static final String ERR_SECRET_NULL        = "jwt.secret must not be null";
    private static final String ERR_SECRET_TOO_SHORT   = "jwt.secret must be at least 32 characters for HMAC-SHA256.";
    private static final String LOG_JWT_PARSE_FAILED   = "JWT parsing failed: {}";

    private final String     jwtSecret;
    private final long       jwtExpirationMs;
    private       SecretKey  signingKey;

    public JwtTokenProvider(
            @Value(PROP_JWT_SECRET)     String jwtSecret,
            @Value(PROP_JWT_EXPIRATION) long   jwtExpirationMs) {
        this.jwtSecret       = Objects.requireNonNull(jwtSecret, ERR_SECRET_NULL);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(ERR_SECRET_TOO_SHORT);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null);
    }

    public String generateToken(UserDetails userDetails, Long userId) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return generateToken(userDetails.getUsername(), roles, userId);
    }

    public String generateToken(String subject, List<String> roles) {
        return generateToken(subject, roles, null);
    }

    private String generateToken(String subject, List<String> roles, Long userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        var builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIMS_KEY_ROLES, roles)
                .issuedAt(now)
                .expiration(expiry);

        if (userId != null) {
            builder.claim("userId", userId);
        }

        return builder.signWith(signingKey).compact();
    }

    public boolean validateToken(String token) {
        return parseClaims(token).isPresent();
    }

    public Optional<Claims> getClaims(String token) {
        return parseClaims(token);
    }

    public Optional<String> getSubject(String token) {
        return parseClaims(token).map(Claims::getSubject);
    }

    private Optional<Claims> parseClaims(String token) {
        if (Objects.isNull(token) || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims payload = Jwts.parser()
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