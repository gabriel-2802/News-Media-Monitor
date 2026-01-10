package app.demo.dto;

public record AuthDTO(
        String accessToken,
        String tokenType
) {
    public AuthDTO(String accessToken) {
        this(accessToken, "Bearer ");
    }
}
