package com.chitchat.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for device token update request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenUpdateRequest {
    
    @NotBlank(message = "Device token is required")
    private String deviceToken;
}
