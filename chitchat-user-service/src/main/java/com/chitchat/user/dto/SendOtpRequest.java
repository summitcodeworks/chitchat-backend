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
 * Valid Examples:
 * - +14155552671 (US)
 * - +919876543210 (India)
 * - +447911123456 (UK)
 * 
 * Invalid Examples:
 * - 4155552671 (missing +)
 * - +1 415 555 2671 (contains spaces)
 * - (415) 555-2671 (contains formatting)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {
    
    /**
     * Phone number in E.164 international format
     * 
     * Format: +[country code][number]
     * E.164 Standard ensures globally unique phone numbers
     * 
     * Validation:
     * - @NotBlank: Cannot be null or empty
     * - @Pattern: Must match E.164 format regex
     * 
     * The regex ^\\+[1-9]\\d{1,14}$ breaks down as:
     * - ^ - Start of string
     * - \\+ - Literal + sign
     * - [1-9] - Country code first digit (1-9, never 0)
     * - \\d{1,14} - Remaining 1-14 digits
     * - $ - End of string
     * 
     * This ensures valid international phone numbers only.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in international format (e.g., +1234567890)")
    private String phoneNumber;
}
