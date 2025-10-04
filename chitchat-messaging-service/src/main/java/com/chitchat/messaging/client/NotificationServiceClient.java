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
        try {
            String url = NOTIFICATION_SERVICE_URL + "/api/notifications/send";
            
            SendMessageNotificationDto dto = SendMessageNotificationDto.builder()
                    .userId(recipientId)
                    .title(senderName)
                    .body(truncateMessage(messageContent))
                    .type("MESSAGE")
                    .data(createMessageData(senderId, messageId))
                    .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer SYSTEM_TOKEN"); // System-level auth
            
            HttpEntity<SendMessageNotificationDto> request = new HttpEntity<>(dto, headers);
            
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
     */
    private Map<String, Object> createMessageData(Long senderId, String messageId) {
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", senderId);
        data.put("messageId", messageId);
        data.put("screen", "chat"); // Navigate to chat screen
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
    }
}

