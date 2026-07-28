package news.media.monitor.manager.dto.responses;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static AuthResponse of(String token, long expiresIn, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}