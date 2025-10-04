package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for legacy user login request (deprecated - use FirebaseAuthRequest instead)
 * Accepts phone numbers in various formats with automatic formatting removal
 * @deprecated Use FirebaseAuthRequest for new implementations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class UserLoginRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[\\+\\d\\s\\(\\)\\-\\.]{7,20}$", message = "Phone number must contain 7-20 characters including digits and optional formatting")
    private String phoneNumber;
    
    @NotBlank(message = "OTP is required")
    private String otp;
}
