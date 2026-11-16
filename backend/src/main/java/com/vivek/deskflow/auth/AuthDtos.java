package com.vivek.deskflow.auth;

import com.vivek.deskflow.user.UserDtos.UserResponse;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterOrgRequest(
            @NotBlank(message = "Company name is required") @Size(max = 120) String companyName,
            @NotBlank(message = "Your name is required") @Size(max = 120) String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password) { }

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) { }

    public record AuthResponse(String token, UserResponse user, String companyName) { }
}
