package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for legacy authentication request (deprecated - use FirebaseAuthRequest instead)
 * @deprecated Use FirebaseAuthRequest for new implementations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class AuthenticationRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "OTP is required")
    private String otp;

    // Optional fields for first-time registration (if user doesn't exist)
    private String name;
    private String deviceInfo;
}