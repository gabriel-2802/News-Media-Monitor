package app.demo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class SearchRequestDTO {
    private String keyword;
    private String topicName;
    private String sourceName;
}
