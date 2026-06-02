package com.bank.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 8) String password,
            @Email @NotBlank String email,
            UUID customerId) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record TokenResponse(String accessToken, long expiresInSec, String tokenType) {}
}
