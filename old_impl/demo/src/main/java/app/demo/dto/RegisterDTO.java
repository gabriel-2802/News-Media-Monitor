package app.demo.dto;

public record RegisterDTO(
        String username,
        String email,
        String password,
        Long adminRegisterCode
) {}
