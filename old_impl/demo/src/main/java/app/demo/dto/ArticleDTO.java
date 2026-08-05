package app.demo.dto;

import java.time.LocalDateTime;

public record ArticleDTO(
        long id,
        String title,
        String content,
        String source,
        String url,
        LocalDateTime published,
        String summary,
        String topic,
        long clusterId,
        boolean notified
) {}
