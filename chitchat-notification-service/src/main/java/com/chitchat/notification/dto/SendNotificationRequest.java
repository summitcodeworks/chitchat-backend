package com.chitchat.notification.dto;

import com.chitchat.notification.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for sending notification request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    @NotNull(message = "Notification type is required")
    private Notification.NotificationType type;
    
    private String imageUrl;
    private String actionUrl;
    private Map<String, Object> data;
    private LocalDateTime scheduledAt;
}
