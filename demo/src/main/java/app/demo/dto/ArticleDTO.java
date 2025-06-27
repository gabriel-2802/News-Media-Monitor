package app.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleDTO {
    private long id;
    private String title;
    private String content;
    private String source;
    private String url;
    private LocalDateTime published;
    private String summary;

    private String topic;
    private long clusterId;
}
