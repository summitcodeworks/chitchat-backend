package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Firebase token authentication request
 * Frontend sends Firebase ID token after successful authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseAuthRequest {

    @NotBlank(message = "Firebase ID token is required")
    private String idToken;

    // Optional fields for first-time registration (if user doesn't exist)
    private String name;
    private String deviceInfo;
}
