package data.provider.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenValidator {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String ERR_KEY_PATH_NULL = "jwt.public-key-path must not be null";
    private static final String LOG_JWT_PARSE_FAILED = "JWT parsing failed: {}";

    private final String publicKeyPath;
    private PublicKey signingKey;

    public JwtTokenValidator(@Value("${jwt.public-key-path}") final String publicKeyPath) {
        this.publicKeyPath = Objects.requireNonNull(publicKeyPath, ERR_KEY_PATH_NULL);
    }

    @PostConstruct
    void init() throws Exception {
        final String pem = Files.readString(Path.of(publicKeyPath));
        final String base64 = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        final byte[] der = Base64.getDecoder().decode(base64);
        this.signingKey = KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(der));
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
