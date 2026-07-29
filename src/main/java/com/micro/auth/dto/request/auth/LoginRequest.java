package com.micro.auth.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request payload")
public record LoginRequest(
        @Email
        @Schema(name = "email", example = "user@example.com")
        String email,

        @NotBlank
        @Schema(name = "password", example = "Password123")
        String password
) {
}
