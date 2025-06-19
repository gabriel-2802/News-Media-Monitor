package app.demo.dto;

import lombok.Data;

import java.util.Set;

@Data
public class TopicDTO {
    private Long id;
    private String name;
    private Set<String> subscribers;
}
