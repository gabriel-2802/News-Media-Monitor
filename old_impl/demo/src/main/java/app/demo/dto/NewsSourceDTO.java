package app.demo.dto;

public record NewsSourceDTO(
        Long id,
        String name,
        String baseUrl,
        String rssUrl
) {}
