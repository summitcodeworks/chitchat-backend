package com.chitchat.notification.service.impl;

import com.chitchat.notification.entity.DeviceToken;
import com.chitchat.notification.entity.Notification;
import com.chitchat.notification.service.FirebaseNotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implementation of FirebaseNotificationService
 */
@Slf4j
@Service
public class FirebaseNotificationServiceImpl implements FirebaseNotificationService {
    
    private final FirebaseMessaging firebaseMessaging;

    public FirebaseNotificationServiceImpl() {
        FirebaseMessaging messaging;
        try {
            messaging = FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.warn("Firebase not initialized, using mock implementation");
            messaging = null;
        }
        this.firebaseMessaging = messaging;
    }
    
    @Override
    public boolean sendNotification(Notification notification, List<DeviceToken> deviceTokens) {
        try {
            if (deviceTokens == null || deviceTokens.isEmpty()) {
                log.warn("No device tokens provided for notification");
                return false;
            }

            log.info("Sending notification to {} devices for user: {}", deviceTokens.size(), notification.getUserId());

            if (firebaseMessaging == null) {
                log.warn("Firebase not available, simulating notification sending");
                return true;
            }

            // Create notification payload
            com.google.firebase.messaging.Notification.Builder notificationBuilder = com.google.firebase.messaging.Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody());

            if (notification.getImageUrl() != null && !notification.getImageUrl().trim().isEmpty()) {
                notificationBuilder.setImage(notification.getImageUrl());
            }

            // Create message builder
            Message.Builder messageBuilder = Message.builder()
                    .setNotification(notificationBuilder.build());

            // Add basic data payload
            messageBuilder.putData("type", notification.getType().toString());
            messageBuilder.putData("notificationId", notification.getId().toString());

            // Parse and add custom data fields individually (required for mobile app)
            if (notification.getData() != null && !notification.getData().trim().isEmpty()) {
                try {
                    // Parse JSON string to extract individual fields
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> dataMap = mapper.readValue(notification.getData(), Map.class);
                    
                    // Add each data field individually to FCM message
                    for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                        messageBuilder.putData(key, value);
                        log.debug("Added data field: {} = {}", key, value);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse notification data JSON: {}", notification.getData(), e);
                    // Fallback: add as single field
                    messageBuilder.putData("customData", notification.getData());
                }
            }

            // Send to each device token
            int successCount = 0;
            for (DeviceToken deviceToken : deviceTokens) {
                if (deviceToken.getToken() == null || deviceToken.getToken().trim().isEmpty()) {
                    log.warn("Skipping empty token for device: {}", deviceToken.getDeviceId());
                    continue;
                }

                try {
                    Message message = messageBuilder.setToken(deviceToken.getToken()).build();
                    String response = firebaseMessaging.send(message);
                    log.info("Notification sent successfully to device: {}, response: {}", deviceToken.getDeviceId(), response);
                    successCount++;
                } catch (FirebaseMessagingException e) {
                    log.error("Failed to send notification to device: {}", deviceToken.getDeviceId(), e);

                    // Handle invalid tokens
                    if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT ||
                        e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                        // Mark token as inactive
                        deviceToken.setIsActive(false);
                        log.info("Marked device token as inactive for device: {}", deviceToken.getDeviceId());
                    }
                }
            }

            boolean success = successCount > 0;
            log.info("Notification sending completed. Success: {}/{}", successCount, deviceTokens.size());

            return success;

        } catch (Exception e) {
            log.error("Failed to send notification", e);
            return false;
        }
    }
    
    @Override
    public boolean sendNotificationToToken(String token, String title, String body, String data) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Cannot send notification: token is null or empty");
                return false;
            }

            if (title == null || title.trim().isEmpty()) {
                log.warn("Cannot send notification: title is null or empty");
                return false;
            }

            log.info("Sending notification to token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");

            if (firebaseMessaging == null) {
                log.warn("Firebase not available, simulating notification sending");
                return true;
            }

            com.google.firebase.messaging.Notification firebaseNotification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body != null ? body : "")
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(firebaseNotification);

            if (data != null && !data.trim().isEmpty()) {
                messageBuilder.putData("customData", data);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            log.info("Notification sent successfully, response: {}", response);

            return true;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: {}", token, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending notification to token", e);
            return false;
        }
    }
    
    @Override
    public boolean sendNotificationToTopic(String topic, String title, String body, String data) {
        try {
            if (topic == null || topic.trim().isEmpty()) {
                log.warn("Cannot send notification: topic is null or empty");
                return false;
            }

            if (title == null || title.trim().isEmpty()) {
                log.warn("Cannot send notification: title is null or empty");
                return false;
            }

            log.info("Sending notification to topic: {}", topic);

            if (firebaseMessaging == null) {
                log.warn("Firebase not available, simulating notification sending to topic");
                return true;
            }

            com.google.firebase.messaging.Notification firebaseNotification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body != null ? body : "")
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setTopic(topic)
                    .setNotification(firebaseNotification);

            if (data != null && !data.trim().isEmpty()) {
                messageBuilder.putData("customData", data);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            log.info("Notification sent successfully to topic: {}, response: {}", topic, response);

            return true;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to topic: {}", topic, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending notification to topic", e);
            return false;
        }
    }
}
