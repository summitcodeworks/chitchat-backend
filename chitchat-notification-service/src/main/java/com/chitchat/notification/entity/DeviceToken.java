package com.chitchat.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Device token entity for Firebase Cloud Messaging (FCM) push notifications
 * 
 * This entity stores FCM registration tokens for user devices.
 * Each user can have multiple devices (phone, tablet, web browser).
 * 
 * Database Table: device_tokens
 * 
 * FCM Token Lifecycle:
 * 1. User logs in on device
 * 2. App requests FCM registration token from Firebase SDK
 * 3. Token sent to server and stored in this entity
 * 4. Server uses token to send push notifications
 * 5. Token may expire or change (app updates token)
 * 6. Inactive tokens are periodically cleaned up
 * 
 * Key Features:
 * - Multi-device support (user can have several active devices)
 * - Device type tracking (Android, iOS, Web)
 * - Token validation and refresh
 * - Inactive token cleanup
 * - Device metadata for analytics
 * 
 * Security:
 * - Tokens are device-specific and user-specific
 * - Tokens should be refreshed periodically
 * - Invalid tokens should be removed
 * - Never share tokens between users
 */
@Entity
@Table(name = "device_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DeviceToken {
    
    /**
     * Unique identifier for this device token record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID of user who owns this device
     * 
     * One user can have multiple devices (phone, tablet, web).
     * Used to find all tokens when sending notification to user.
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * FCM registration token
     * 
     * Format: Long string provided by Firebase SDK
     * Example: "dGhpcyBpcyBhIGZha2UgdG9rZW4..."
     * 
     * This is the actual token FCM uses to deliver notifications.
     * 
     * Token Characteristics:
     * - Unique per device
     * - Can expire or change
     * - Should be refreshed on app launch
     * - Becomes invalid when app is uninstalled
     */
    @Column(nullable = false)
    private String token;
    
    /**
     * Type of device
     * 
     * Used for:
     * - Platform-specific notification formatting
     * - Analytics (which platforms users use)
     * - Troubleshooting device-specific issues
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;
    
    /**
     * Unique identifier for the physical device
     * 
     * Platform-specific device ID:
     * - Android: Android ID or GUID
     * - iOS: Identifier for Vendor (IDFV)
     * - Web: Browser fingerprint or UUID
     * 
     * Used to:
     * - Prevent duplicate token registrations
     * - Track devices per user
     * - Implement "logout from all devices"
     */
    @Column(nullable = false)
    private String deviceId;
    
    /**
     * Version of the ChitChat app
     * 
     * Examples: "1.0.5", "2.1.0"
     * Used for:
     * - Feature compatibility checking
     * - Analytics on app version distribution
     * - Sending version-specific notifications
     */
    private String appVersion;
    
    /**
     * Operating system version
     * 
     * Examples: "Android 12", "iOS 15.4", "Windows 11"
     * Used for:
     * - Troubleshooting OS-specific issues
     * - Analytics
     * - Compatibility checks
     */
    private String osVersion;
    
    /**
     * Device model
     * 
     * Examples: "iPhone 13 Pro", "Samsung Galaxy S21", "Chrome Browser"
     * Used for analytics and troubleshooting.
     */
    private String deviceModel;
    
    /**
     * Flag indicating if this token is currently active
     * 
     * false when:
     * - App uninstalled
     * - User logged out from device
     * - Token became invalid
     * - User deleted device from account
     * 
     * Inactive tokens are not used for notifications.
     */
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * Timestamp when token was registered
     * 
     * Automatically set by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when token was last updated
     * 
     * Automatically updated by JPA auditing.
     * Updated when token is refreshed or status changed.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Enum defining supported device platforms
     * 
     * ANDROID: Android mobile devices
     * IOS: iPhone and iPad devices
     * WEB: Web browsers (Chrome, Safari, Firefox)
     * 
     * Each platform has different FCM token formats and behaviors.
     */
    public enum DeviceType {
        ANDROID, IOS, WEB
    }
}
