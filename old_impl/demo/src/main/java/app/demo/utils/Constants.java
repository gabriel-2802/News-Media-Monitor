package app.demo.utils;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class Constants {
    public static final long JWT_EXPIRATION = 2592000000L; // 30 days in milliseconds
    private static final String secret = "mysecretkeymysecretkeymysecretkeymysecretkeymysecretkeymysecretmysecretkey";
    public static final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    public static final long ADMIN_REGISTER_CODE = 282828282L;
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";
    public static final String ADMIN_ENDPOINT = "/api/admin/**";
    public static final String USER_ENDPOINT = "/api/user/**";
}