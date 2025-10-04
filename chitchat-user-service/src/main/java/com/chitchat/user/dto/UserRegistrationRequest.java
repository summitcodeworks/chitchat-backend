package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration request
 * Accepts phone numbers in various formats with automatic formatting removal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[\\+\\d\\s\\(\\)\\-\\.]{7,20}$", message = "Phone number must contain 7-20 characters including digits and optional formatting")
    private String phoneNumber;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String deviceInfo;
}
