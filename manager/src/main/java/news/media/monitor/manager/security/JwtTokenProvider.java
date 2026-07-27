package news.media.monitor.manager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String RSA_ALGORITHM          = "RSA";
    private static final String CLAIMS_KEY_ROLES       = "roles";
    private static final String PROP_JWT_PRIVATE_KEY   = "${jwt.private-key-path}";
    private static final String PROP_JWT_PUBLIC_KEY    = "${jwt.public-key-path}";
    private static final String PROP_JWT_EXPIRATION    = "${jwt.expiration}";
    private static final String ERR_KEY_PATH_NULL      = "jwt key path must not be null";
    private static final String LOG_JWT_PARSE_FAILED   = "JWT parsing failed: {}";

    private final String      privateKeyPath;
    private final String      publicKeyPath;
    private final long        jwtExpirationMs;
    private       PrivateKey  privateKey;
    private       PublicKey   publicKey;

    public JwtTokenProvider(
            @Value(PROP_JWT_PRIVATE_KEY) String privateKeyPath,
            @Value(PROP_JWT_PUBLIC_KEY)  String publicKeyPath,
            @Value(PROP_JWT_EXPIRATION)  long   jwtExpirationMs) {
        this.privateKeyPath  = Objects.requireNonNull(privateKeyPath, ERR_KEY_PATH_NULL);
        this.publicKeyPath   = Objects.requireNonNull(publicKeyPath, ERR_KEY_PATH_NULL);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @PostConstruct
    void init() throws Exception {
        this.privateKey = loadPrivateKey(privateKeyPath);
        this.publicKey  = loadPublicKey(publicKeyPath);
    }

    private static PrivateKey loadPrivateKey(String path) throws Exception {
        byte[] der = pemToDer(Files.readString(Path.of(path)));
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static PublicKey loadPublicKey(String path) throws Exception {
        byte[] der = pemToDer(Files.readString(Path.of(path)));
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(new X509EncodedKeySpec(der));
    }

    private static byte[] pemToDer(String pem) {
        String base64 = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
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

        return builder.signWith(privateKey, Jwts.SIG.RS256).compact();
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
                    .verifyWith(publicKey)
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