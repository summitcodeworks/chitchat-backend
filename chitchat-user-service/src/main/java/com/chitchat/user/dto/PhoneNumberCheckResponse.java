package com.chitchat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for phone number existence check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumberCheckResponse {
    private String phoneNumber;
    private boolean exists;
    private UserResponse user; // Only populated if user exists
    private String message;
}
