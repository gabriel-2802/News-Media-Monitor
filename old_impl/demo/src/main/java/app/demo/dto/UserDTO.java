package app.demo.dto;

import java.util.Set;

public record UserDTO(
        Long id,
        String username,
        String email,
        Set<String> roles,
        Set<String> subscribedTopics
) {}
