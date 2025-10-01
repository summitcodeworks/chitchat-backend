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
 * Notification entity for tracking push notification delivery
 * 
 * This entity stores all notifications sent through Firebase Cloud Messaging.
 * Provides complete audit trail for notification delivery including:
 * - What notification was sent
 * - To whom it was sent
 * - When it was sent
 * - Delivery status
 * - User interaction (read/unread)
 * 
 * Database Table: notifications
 * 
 * Use Cases:
 * - Track notification delivery success/failure
 * - Retry failed notifications
 * - Notification history for users
 * - Analytics on notification engagement
 * - Debugging notification issues
 * - Compliance and auditing
 * 
 * Notification Lifecycle:
 * 1. PENDING: Created but not yet sent
 * 2. SENT: Sent to FCM successfully
 * 3. DELIVERED: Delivered to user's device (FCM confirmation)
 * 4. READ: User opened/interacted with notification
 * 5. FAILED: Delivery failed (invalid token, FCM error, etc.)
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {
    
    /**
     * Unique identifier for the notification record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID of user receiving the notification
     * 
     * References User entity in user-service.
     * Used to look up device token for FCM delivery.
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * Notification title
     * 
     * Displayed as bold headline in notification.
     * Examples:
     * - "New message from John"
     * - "Incoming call from Alice"
     * - "You were added to Group Chat"
     */
    @Column(nullable = false)
    private String title;
    
    /**
     * Notification body/message
     * 
     * Main notification content displayed below title.
     * Examples:
     * - "Hey, are you there?"
     * - "Tap to answer"
     * - "Alice added you to 'Weekend Plans'"
     */
    @Column(nullable = false)
    private String body;
    
    /**
     * Type of notification
     * 
     * Determines:
     * - Notification icon
     * - Notification sound
     * - Action when tapped
     * - Priority level
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    /**
     * Current delivery status of the notification
     * 
     * Tracks the notification through its lifecycle.
     * Updated as FCM provides delivery confirmations.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    /**
     * Additional data as JSON string
     * 
     * Custom key-value pairs for app-specific handling.
     * Examples:
     * - {"chatId": "123", "senderId": "456"}
     * - {"callId": "789", "callType": "VIDEO"}
     * 
     * Used by app to navigate to correct screen when notification tapped.
     */
    private String data;
    
    /**
     * URL to image for rich notification
     * 
     * Optional image displayed in notification.
     * Examples:
     * - Sender's avatar
     * - Message image thumbnail
     * - Group icon
     */
    private String imageUrl;
    
    /**
     * Deep link URL when notification is tapped
     * 
     * Examples:
     * - "chitchat://chat/123"
     * - "chitchat://call/456"
     * - "chitchat://group/789"
     * 
     * App uses this to navigate to appropriate screen.
     */
    private String actionUrl;
    
    /**
     * Scheduled delivery time for future notifications
     * 
     * If set, notification is held until this time.
     * Null for immediate notifications (most common).
     */
    private LocalDateTime scheduledAt;
    
    /**
     * Timestamp when notification was sent to FCM
     * 
     * Set when status changes to SENT.
     * Used to calculate delivery latency.
     */
    private LocalDateTime sentAt;
    
    /**
     * Timestamp when user interacted with notification
     * 
     * Set when user:
     * - Taps on notification
     * - Opens app from notification
     * - Dismisses notification
     * 
     * Used for engagement metrics.
     */
    private LocalDateTime readAt;
    
    /**
     * Error message if notification delivery failed
     * 
     * Contains FCM error details.
     * Common errors:
     * - Invalid registration token
     * - Token not registered
     * - Message too large
     * - Rate limit exceeded
     */
    private String errorMessage;
    
    /**
     * Number of retry attempts for failed notifications
     * 
     * Incremented each time delivery is retried.
     * Max retries typically 3-5.
     * Prevents infinite retry loops.
     */
    private Integer retryCount;
    
    /**
     * Timestamp when notification record was created
     * 
     * Automatically set by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when notification record was last updated
     * 
     * Automatically updated by JPA auditing.
     * Changes when status updates or delivery confirmed.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Enum defining types of notifications
     * 
     * Each type has different:
     * - Icon
     * - Sound
     * - Priority
     * - Action
     * 
     * MESSAGE: New chat message received
     * CALL: Incoming voice/video call
     * STATUS_UPDATE: New status from contact
     * FRIEND_REQUEST: New contact request
     * GROUP_INVITE: Invited to group
     * SYSTEM: System announcements
     */
    public enum NotificationType {
        MESSAGE, CALL, STATUS_UPDATE, FRIEND_REQUEST, GROUP_INVITE, SYSTEM
    }
    
    /**
     * Enum defining notification delivery status
     * 
     * Lifecycle:
     * PENDING -> SENT -> DELIVERED -> READ
     * 
     * Or:
     * PENDING -> FAILED (if delivery fails)
     * 
     * PENDING: Created, not yet sent to FCM
     * SENT: Successfully sent to FCM
     * DELIVERED: FCM confirmed delivery to device
     * READ: User interacted with notification
     * FAILED: Delivery failed (retry or discard)
     */
    public enum NotificationStatus {
        PENDING, SENT, DELIVERED, READ, FAILED
    }
}
