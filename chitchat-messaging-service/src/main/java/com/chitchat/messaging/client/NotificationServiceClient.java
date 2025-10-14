package com.chitchat.messaging.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for calling Notification Service APIs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationServiceClient {
    
    private final RestTemplate restTemplate;
    private static final String NOTIFICATION_SERVICE_URL = "http://localhost:9106";
    
    /**
     * Send notification when a new message is received
     */
    public void sendMessageNotification(Long recipientId, String senderName, String messageContent, Long senderId, String messageId) {
        sendMessageNotification(recipientId, senderName, messageContent, senderId, messageId, null);
    }
    
    /**
     * Send notification when a new message is received (with sender profile info)
     */
    public void sendMessageNotification(Long recipientId, String senderName, String messageContent, Long senderId, String messageId, String senderAvatarUrl) {
        try {
            String url = NOTIFICATION_SERVICE_URL + "/api/notifications/send";
            
            SendMessageNotificationDto dto = SendMessageNotificationDto.builder()
                    .userId(recipientId)
                    .title(senderName)
                    .body(truncateMessage(messageContent))
                    .type("MESSAGE")
                    .data(createMessageData(senderId, messageId, recipientId, senderName, senderAvatarUrl))
                    .imageUrl(senderAvatarUrl) // Add profile image to notification
                    .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer SYSTEM_TOKEN"); // System-level auth
            
            HttpEntity<SendMessageNotificationDto> request = new HttpEntity<>(dto, headers);
            
            log.info("Sending push notification to user {} for message from {} (avatar: {})", recipientId, senderId, senderAvatarUrl != null ? "yes" : "no");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Message notification sent successfully to user: {}", recipientId);
            } else {
                log.warn("Failed to send message notification. Status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error sending message notification to user: {}", recipientId, e);
            // Don't throw exception - notification failure shouldn't block message sending
        }
    }
    
    /**
     * Truncate message to fit in notification (max 100 chars)
     */
    private String truncateMessage(String message) {
        if (message == null) return "";
        return message.length() > 100 ? message.substring(0, 97) + "..." : message;
    }
    
    /**
     * Create custom data payload for notification
     * This matches the structure expected by the mobile app
     */
    private Map<String, Object> createMessageData(Long senderId, String messageId, Long recipientId, String senderName, String senderAvatarUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "NEW_MESSAGE"); // Mobile app checks for this
        data.put("senderId", String.valueOf(senderId)); // Mobile app expects string
        data.put("messageId", messageId);
        data.put("conversationId", String.valueOf(senderId)); // For mobile app navigation
        data.put("senderName", senderName != null ? senderName : "User"); // For MessagingStyle
        data.put("senderAvatarUrl", senderAvatarUrl != null ? senderAvatarUrl : ""); // For profile icon
        return data;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageNotificationDto {
        private Long userId;
        private String title;
        private String body;
        private String type;
        private Map<String, Object> data;
        private String imageUrl;  // Profile image for notification
    }
}

