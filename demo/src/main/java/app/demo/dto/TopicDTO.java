package app.demo.dto;

import java.util.Set;

public record TopicDTO(
        Long id,
        String name,
        Set<String> subscribers
) {}
