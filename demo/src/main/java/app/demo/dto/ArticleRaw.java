package app.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleRaw {
    private String title;
    private String content;
    private String sourceName;
    private String sourceUrl;
    private LocalDateTime publishedDate;
}
