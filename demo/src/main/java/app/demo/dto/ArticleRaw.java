package app.demo.dto;

import java.time.LocalDateTime;

public record ArticleRaw(
        String title,
        String content,
        String sourceName,
        String sourceUrl,
        LocalDateTime publishedDate
) {}
