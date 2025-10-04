package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for verifying OTP request
 * 
 * Accepts phone numbers in various formats:
 * - +918929607491
 * - 918929607491
 * - +1 415 555 2671
 * - (415) 555-2671
 * 
 * All formatting (spaces, parentheses, hyphens, dots) is automatically removed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[\\+\\d\\s\\(\\)\\-\\.]{7,20}$", message = "Phone number must contain 7-20 characters including digits and optional formatting")
    private String phoneNumber;
    
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;
}
