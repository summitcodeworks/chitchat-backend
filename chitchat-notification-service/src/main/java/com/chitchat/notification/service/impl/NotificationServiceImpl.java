package com.chitchat.notification.service.impl;

import com.chitchat.notification.client.UserServiceClient;
import com.chitchat.notification.dto.*;
import com.chitchat.notification.entity.DeviceToken;
import com.chitchat.notification.entity.Notification;
import com.chitchat.notification.repository.DeviceTokenRepository;
import com.chitchat.notification.repository.NotificationRepository;
import com.chitchat.notification.service.NotificationService;
import com.chitchat.notification.service.FirebaseNotificationService;
import com.chitchat.shared.dto.ApiResponse;
import com.chitchat.shared.exception.ChitChatException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final ObjectMapper objectMapper;
    private final UserServiceClient userServiceClient;
    
    @Override
    @Transactional
    public void registerDeviceToken(Long userId, RegisterDeviceTokenRequest request) {
        log.info("Registering device token for user: {}", userId);
        
        // Check if device token already exists by deviceId (regardless of userId)
        DeviceToken existingToken = deviceTokenRepository.findByDeviceId(request.getDeviceId()).orElse(null);
        
        if (existingToken != null) {
            // Update existing token
            existingToken.setToken(request.getToken());
            existingToken.setDeviceType(request.getDeviceType());
            existingToken.setAppVersion(request.getAppVersion());
            existingToken.setOsVersion(request.getOsVersion());
            existingToken.setDeviceModel(request.getDeviceModel());
            existingToken.setIsActive(true);
            // Update userId if provided
            if (userId != null) {
                existingToken.setUserId(userId);
            }
            deviceTokenRepository.save(existingToken);
        } else {
            // Create new token
            DeviceToken deviceToken = DeviceToken.builder()
                    .userId(userId)
                    .token(request.getToken())
                    .deviceType(request.getDeviceType())
                    .deviceId(request.getDeviceId())
                    .appVersion(request.getAppVersion())
                    .osVersion(request.getOsVersion())
                    .deviceModel(request.getDeviceModel())
                    .isActive(true)
                    .build();
            
            deviceTokenRepository.save(deviceToken);
        }
        
        log.info("Device token registered successfully for user: {}", userId);
    }
    
    @Override
    @Transactional
    public void unregisterDeviceToken(Long userId, String deviceId) {
        log.info("Unregistering device token for user: {} and device: {}", userId, deviceId);
        
        DeviceToken deviceToken = deviceTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new ChitChatException("Device token not found", HttpStatus.NOT_FOUND, "DEVICE_TOKEN_NOT_FOUND"));
        
        deviceToken.setIsActive(false);
        deviceTokenRepository.save(deviceToken);
        
        log.info("Device token unregistered successfully");
    }
    
    @Override
    @Transactional
    public void sendNotification(SendNotificationRequest request) {
        log.info("Sending notification to user: {}", request.getUserId());
        
        // Create notification record
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .body(request.getBody())
                .type(request.getType())
                .status(Notification.NotificationStatus.PENDING)
                .imageUrl(request.getImageUrl())
                .actionUrl(request.getActionUrl())
                .scheduledAt(request.getScheduledAt() != null ? request.getScheduledAt() : LocalDateTime.now())
                .retryCount(0)
                .build();
        
        // Convert data to JSON string
        if (request.getData() != null) {
            try {
                notification.setData(objectMapper.writeValueAsString(request.getData()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize notification data", e);
            }
        }
        
        notification = notificationRepository.save(notification);
        
        // Send notification asynchronously
        sendNotificationAsync(notification);
    }
    
    @Override
    @Transactional
    public void sendNotificationByPhone(SendNotificationByPhoneRequest request) {
        log.info("Sending notification to phone number: {}", request.getPhoneNumber());
        
        try {
            // Get user by phone number from user service
            ApiResponse<UserServiceClient.UserDto> response = userServiceClient.getUserByPhoneNumber(request.getPhoneNumber());
            
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new ChitChatException("User not found with phone number: " + request.getPhoneNumber(), 
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
            }
            
            UserServiceClient.UserDto user = response.getData();
            log.info("Found user with ID: {} for phone number: {}", user.getId(), request.getPhoneNumber());
            
            // Create and send notification using userId
            SendNotificationRequest notificationRequest = SendNotificationRequest.builder()
                    .userId(user.getId())
                    .title(request.getTitle())
                    .body(request.getBody())
                    .type(request.getType())
                    .imageUrl(request.getImageUrl())
                    .actionUrl(request.getActionUrl())
                    .data(request.getData())
                    .scheduledAt(request.getScheduledAt())
                    .build();
            
            sendNotification(notificationRequest);
            
        } catch (Exception e) {
            log.error("Failed to send notification by phone number: {}", request.getPhoneNumber(), e);
            throw new ChitChatException("Failed to send notification: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, "NOTIFICATION_SEND_FAILED");
        }
    }
    
    @Override
    @Transactional
    public void sendBulkNotification(List<Long> userIds, SendNotificationRequest request) {
        log.info("Sending bulk notification to {} users", userIds.size());
        
        for (Long userId : userIds) {
            SendNotificationRequest userRequest = SendNotificationRequest.builder()
                    .userId(userId)
                    .title(request.getTitle())
                    .body(request.getBody())
                    .type(request.getType())
                    .imageUrl(request.getImageUrl())
                    .actionUrl(request.getActionUrl())
                    .data(request.getData())
                    .scheduledAt(request.getScheduledAt())
                    .build();
            
            sendNotification(userRequest);
        }
    }
    
    @Override
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        return notifications.map(this::mapToNotificationResponse);
    }
    
    @Override
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findUnreadNotificationsByUserId(userId);
        return notifications.stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ChitChatException("Notification not found", HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new ChitChatException("Unauthorized to mark this notification as read", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        notification.setReadAt(LocalDateTime.now());
        notification.setStatus(Notification.NotificationStatus.READ);
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadNotificationsByUserId(userId);
        
        for (Notification notification : unreadNotifications) {
            notification.setReadAt(LocalDateTime.now());
            notification.setStatus(Notification.NotificationStatus.READ);
        }
        
        notificationRepository.saveAll(unreadNotifications);
    }
    
    @Override
    @Async
    public void processPendingNotifications() {
        log.info("Processing pending notifications");
        
        List<Notification> pendingNotifications = notificationRepository.findScheduledNotificationsToSend(LocalDateTime.now());
        
        for (Notification notification : pendingNotifications) {
            sendNotificationAsync(notification);
        }
    }
    
    @Override
    @Async
    public void retryFailedNotifications() {
        log.info("Retrying failed notifications");
        
        List<Notification> failedNotifications = notificationRepository.findFailedNotificationsToRetry();
        
        for (Notification notification : failedNotifications) {
            sendNotificationAsync(notification);
        }
    }
    
    @Override
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Cleaning up old notifications");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Notification> oldNotifications = notificationRepository.findOldNotificationsToCleanup(cutoffDate);
        
        notificationRepository.deleteAll(oldNotifications);
        
        log.info("Cleaned up {} old notifications", oldNotifications.size());
    }
    
    @Async
    private void sendNotificationAsync(Notification notification) {
        try {
            // Get user's active device tokens
            List<DeviceToken> deviceTokens = deviceTokenRepository.findActiveTokensByUserId(notification.getUserId());
            
            if (deviceTokens.isEmpty()) {
                log.warn("No active device tokens found for user: {}", notification.getUserId());
                notification.setStatus(Notification.NotificationStatus.FAILED);
                notification.setErrorMessage("No active device tokens");
                notificationRepository.save(notification);
                return;
            }
            
            // Send notification via Firebase
            boolean success = firebaseNotificationService.sendNotification(notification, deviceTokens);
            
            if (success) {
                notification.setStatus(Notification.NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } else {
                notification.setStatus(Notification.NotificationStatus.FAILED);
                notification.setErrorMessage("Failed to send via Firebase");
                notification.setRetryCount(notification.getRetryCount() + 1);
            }
            
            notificationRepository.save(notification);
            
        } catch (Exception e) {
            log.error("Failed to send notification: {}", notification.getId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        }
    }
    
    private NotificationResponse mapToNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .status(notification.getStatus())
                .data(notification.getData())
                .imageUrl(notification.getImageUrl())
                .actionUrl(notification.getActionUrl())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
