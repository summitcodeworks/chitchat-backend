package com.chitchat.notification.service;

import com.chitchat.notification.entity.DeviceToken;
import com.chitchat.notification.entity.Notification;

import java.util.List;

/**
 * Service interface for Firebase Cloud Messaging operations
 */
public interface FirebaseNotificationService {
    
    boolean sendNotification(Notification notification, List<DeviceToken> deviceTokens);
    
    boolean sendNotificationToToken(String token, String title, String body, String data);
    
    boolean sendNotificationToTopic(String topic, String title, String body, String data);
}
