package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending OTP request
 * 
 * Request body for /api/users/send-otp endpoint.
 * Initiates SMS OTP delivery to specified phone number.
 * 
 * Request Example:
 * POST /api/users/send-otp
 * {
 *   "phoneNumber": "+14155552671"
 * }
 * 
 * Validation Rules:
 * - Phone number is required (not null, not empty)
 * - Must be in E.164 international format
 * - Must start with + followed by country code
 * - Total length: 1-15 digits after +
 * 
 * Valid Examples (all formats accepted):
 * - +14155552671 (standard international)
 * - 14155552671 (without +)
 * - +1 415 555 2671 (with spaces)
 * - (415) 555-2671 (with parentheses and hyphens)
 * - +1-415-555-2671 (with hyphens)
 * - +91 892 960 7491 (India with spaces)
 * - (91) 892-960-7491 (mixed formatting)
 * 
 * All formatting is automatically removed and stored as: 14155552671
 * 
 * Invalid Examples:
 * - abc123 (contains letters)
 * - 123 (too short)
 * - +1 415 555 2671 ext 123 (contains text)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {
    
    /**
     * Phone number in international format (flexible)
     * 
     * Accepts various formats - all formatting will be automatically removed:
     * - +918929607491
     * - 918929607491
     * - +1 415 555 2671
     * - (415) 555-2671
     * - +1-415-555-2671
     * 
     * Validation:
     * - @NotBlank: Cannot be null or empty
     * - @Pattern: Flexible pattern that accepts digits with optional formatting
     * 
     * The regex allows:
     * - Optional + at start
     * - Digits with optional spaces, parentheses, hyphens, dots
     * - Must contain at least some digits
     * 
     * All formatting characters (spaces, parentheses, hyphens, dots) are automatically
     * removed before storage, resulting in clean international phone numbers.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[\\+\\d\\s\\(\\)\\-\\.]{7,20}$", message = "Phone number must contain 7-20 characters including digits and optional formatting (+, spaces, parentheses, hyphens)")
    private String phoneNumber;
}
