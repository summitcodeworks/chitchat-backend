package com.chitchat.notification.dto;

import com.chitchat.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private Long id;
    private Long userId;
    private String title;
    private String body;
    private Notification.NotificationType type;
    private Notification.NotificationStatus status;
    private String data;
    private String imageUrl;
    private String actionUrl;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
