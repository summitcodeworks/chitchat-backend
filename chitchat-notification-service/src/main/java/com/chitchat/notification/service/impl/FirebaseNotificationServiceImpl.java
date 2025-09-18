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
        this.firebaseMessaging = FirebaseMessaging.getInstance();
    }
    
    @Override
    public boolean sendNotification(Notification notification, List<DeviceToken> deviceTokens) {
        try {
            log.info("Sending notification to {} devices for user: {}", deviceTokens.size(), notification.getUserId());
            
            // Create notification payload
            com.google.firebase.messaging.Notification.Builder notificationBuilder = com.google.firebase.messaging.Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody());
            
            if (notification.getImageUrl() != null) {
                notificationBuilder.setImage(notification.getImageUrl());
            }
            
            // Create message builder
            Message.Builder messageBuilder = Message.builder()
                    .setNotification(notificationBuilder.build());
            
            // Add data payload
            if (notification.getData() != null) {
                messageBuilder.putData("type", notification.getType().toString());
                messageBuilder.putData("actionUrl", notification.getActionUrl() != null ? notification.getActionUrl() : "");
                messageBuilder.putData("notificationId", notification.getId().toString());
            }
            
            // Send to each device token
            int successCount = 0;
            for (DeviceToken deviceToken : deviceTokens) {
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
                        // This would need to be saved to database
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
            log.info("Sending notification to token: {}", token);
            
            com.google.firebase.messaging.Notification firebaseNotification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(firebaseNotification)
                    .putData("data", data)
                    .build();
            
            String response = firebaseMessaging.send(message);
            log.info("Notification sent successfully, response: {}", response);
            
            return true;
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: {}", token, e);
            return false;
        }
    }
    
    @Override
    public boolean sendNotificationToTopic(String topic, String title, String body, String data) {
        try {
            log.info("Sending notification to topic: {}", topic);
            
            com.google.firebase.messaging.Notification firebaseNotification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();
            
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(firebaseNotification)
                    .putData("data", data)
                    .build();
            
            String response = firebaseMessaging.send(message);
            log.info("Notification sent successfully to topic: {}, response: {}", topic, response);
            
            return true;
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to topic: {}", topic, e);
            return false;
        }
    }
}
