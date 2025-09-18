package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String deviceInfo;
}
