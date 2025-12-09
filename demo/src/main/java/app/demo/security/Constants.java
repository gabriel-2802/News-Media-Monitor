package app.demo.security;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class Constants {
    public static final long JWT_EXPIRATION = 2592000000L; // 30 days in milliseconds
    private static final String secret = "mysecretkeymysecretkeymysecretkeymysecretkeymysecretkeymysecretmysecretkey";
    public static final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    public static final long ADMIN_REGISTER_CODE = 282828282L;
    public static final String HF_TOKEN = "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";
}