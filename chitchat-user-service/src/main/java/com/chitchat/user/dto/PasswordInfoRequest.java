package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for password info retrieval
 */
@Data
public class PasswordInfoRequest {

    @NotBlank(message = "Hashed password is required")
    private String hashedPassword;
}