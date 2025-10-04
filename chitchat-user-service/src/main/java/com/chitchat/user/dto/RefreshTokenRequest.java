package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for refreshing access token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    
    /**
     * The refresh token to use for obtaining a new access token
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

