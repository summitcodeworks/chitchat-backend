package com.chitchat.notification.controller;

import com.chitchat.notification.dto.*;
import com.chitchat.notification.service.NotificationService;
import com.chitchat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for notification operations
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PostMapping("/device-token")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        Long userId = extractUserIdFromToken(token);
        notificationService.registerDeviceToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Device token registered successfully"));
    }
    
    @PostMapping("/register-device")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        // For device registration, we can work without user authentication
        // The device will be associated with a user later when they log in
        Long userId = token != null ? extractUserIdFromToken(token) : null;
        notificationService.registerDeviceToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Device registered successfully"));
    }
    
    @DeleteMapping("/device-token/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(
            @RequestHeader("Authorization") String token,
            @PathVariable String deviceId) {
        Long userId = extractUserIdFromToken(token);
        notificationService.unregisterDeviceToken(userId, deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device token unregistered successfully"));
    }
    
    @DeleteMapping("/unregister-device/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> unregisterDevice(
            @RequestHeader("Authorization") String token,
            @PathVariable String deviceId) {
        Long userId = extractUserIdFromToken(token);
        notificationService.unregisterDeviceToken(userId, deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device unregistered successfully"));
    }
    
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SendNotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification sent successfully"));
    }
    
    @PostMapping("/send-by-phone")
    public ResponseEntity<ApiResponse<Void>> sendNotificationByPhone(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody SendNotificationByPhoneRequest request) {
        log.info("Sending notification to phone number: {}", request.getPhoneNumber());
        notificationService.sendNotificationByPhone(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification sent successfully to phone number"));
    }
    
    @PostMapping("/send-bulk")
    public ResponseEntity<ApiResponse<Void>> sendBulkNotification(
            @RequestHeader("Authorization") String token,
            @RequestBody List<Long> userIds,
            @Valid @RequestBody SendNotificationRequest request) {
        notificationService.sendBulkNotification(userIds, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Bulk notification sent successfully"));
    }
    
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUserNotifications(
            @RequestHeader("Authorization") String token,
            Pageable pageable) {
        Long userId = extractUserIdFromToken(token);
        Page<NotificationResponse> response = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        List<NotificationResponse> response = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long notificationId) {
        Long userId = extractUserIdFromToken(token);
        notificationService.markNotificationAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }
    
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        notificationService.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
    
    private Long extractUserIdFromToken(String token) {
        // Extract user ID from JWT token
        // This is a simplified implementation
        // In a real application, you would use a proper JWT service
        return 1L; // Placeholder
    }
}
