package com.chitchat.notification.dto;

import com.chitchat.notification.entity.DeviceToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for registering device token request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {
    
    @NotBlank(message = "Device token is required")
    private String token;
    
    @NotNull(message = "Device type is required")
    private DeviceToken.DeviceType deviceType;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    private String appVersion;
    private String osVersion;
    private String deviceModel;
}
