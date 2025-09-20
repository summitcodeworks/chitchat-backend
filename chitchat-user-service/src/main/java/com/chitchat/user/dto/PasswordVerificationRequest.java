package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for password verification
 */
@Data
public class PasswordVerificationRequest {

    @NotBlank(message = "Hashed password is required")
    private String hashedPassword;

    private String plainPassword; // Optional: if provided, verify this specific password
}