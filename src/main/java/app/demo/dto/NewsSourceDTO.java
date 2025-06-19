package app.demo.dto;

import lombok.Data;

@Data
public class NewsSourceDTO {
    private Long id;
    private String name;
    private String baseUrl;
    private String rssUrl;

}
