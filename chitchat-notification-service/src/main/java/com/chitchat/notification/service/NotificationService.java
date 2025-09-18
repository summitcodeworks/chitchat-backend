package com.chitchat.notification.service;

import com.chitchat.notification.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for notification operations
 */
public interface NotificationService {
    
    void registerDeviceToken(Long userId, RegisterDeviceTokenRequest request);
    
    void unregisterDeviceToken(Long userId, String deviceId);
    
    void sendNotification(SendNotificationRequest request);
    
    void sendBulkNotification(List<Long> userIds, SendNotificationRequest request);
    
    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);
    
    List<NotificationResponse> getUnreadNotifications(Long userId);
    
    void markNotificationAsRead(Long notificationId, Long userId);
    
    void markAllNotificationsAsRead(Long userId);
    
    void processPendingNotifications();
    
    void retryFailedNotifications();
    
    void cleanupOldNotifications();
}
