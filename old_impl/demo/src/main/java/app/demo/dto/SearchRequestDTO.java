package app.demo.dto;

public record SearchRequestDTO(
        String keyword,
        String topicName,
        String sourceName
) {}
