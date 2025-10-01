package com.chitchat.user.entity;

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
 * User entity for ChitChat application - Core user profile information
 * 
 * This entity represents a registered user in the ChitChat messaging platform.
 * ChitChat uses phone number-based authentication (no passwords stored).
 * 
 * Database Table: users
 * 
 * Key Features:
 * - Phone number is the primary unique identifier (used for login)
 * - No password field - authentication via SMS OTP
 * - Firebase UID for push notifications and real-time features
 * - Online/offline status tracking
 * - Profile customization (name, avatar, about)
 * - Automatic timestamp management via JPA auditing
 * 
 * Privacy & Security:
 * - Phone numbers are unique and never shared with other users
 * - isActive flag allows soft deletion (GDPR compliance)
 * - lastSeen can be hidden based on privacy settings
 * - Device tokens are encrypted in transit
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    /**
     * Unique identifier for the user (auto-generated)
     * Primary key for all user-related operations
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User's phone number in E.164 format (e.g., +14155552671)
     * 
     * This is the primary authentication identifier:
     * - Must be unique across all users
     * - Used for SMS OTP delivery
     * - Used for contact discovery
     * - Never displayed to other users for privacy
     */
    @Column(unique = true, nullable = false)
    private String phoneNumber;
    
    /**
     * User's display name
     * 
     * Visible to all contacts and in group chats.
     * Can be updated by user at any time.
     * Typically first name + last name or nickname.
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * URL to user's profile picture/avatar
     * 
     * Points to image stored in media service or CDN.
     * Null/empty means using default avatar.
     * Visible to all contacts.
     */
    private String avatarUrl;
    
    /**
     * User's "about" or status message
     * 
     * Short bio or status message (e.g., "Hey there! I am using ChitChat")
     * Optional field, can be null.
     * Visible to all contacts.
     */
    private String about;
    
    /**
     * Timestamp of when user was last seen online
     * 
     * Updated when:
     * - User opens the app
     * - User sends a message
     * - User performs any activity
     * 
     * Can be hidden based on privacy settings.
     */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    /**
     * Flag indicating if user is currently online
     * 
     * True when:
     * - User has active WebSocket connection
     * - User is actively using the app
     * 
     * False when:
     * - User closes the app
     * - Connection times out
     * - User goes offline
     */
    @Column(name = "is_online")
    private Boolean isOnline;
    
    /**
     * Flag indicating if user account is active
     * 
     * False when:
     * - User deletes their account (soft delete)
     * - Admin suspends the account
     * - Account is deactivated for compliance reasons
     * 
     * Inactive users cannot:
     * - Log in
     * - Send/receive messages
     * - Be discovered by other users
     */
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * Firebase User ID for push notifications
     * 
     * Used for:
     * - Firebase Cloud Messaging (FCM) push notifications
     * - Firebase Authentication integration
     * - Real-time database updates
     * 
     * Generated during first login/registration.
     */
    @Column(name = "firebase_uid")
    private String firebaseUid;
    
    /**
     * FCM device token for push notifications
     * 
     * Used to send push notifications to user's device.
     * Updated each time user logs in from a new device.
     * Expires and needs to be refreshed periodically.
     * 
     * Format: FCM registration token from Firebase SDK
     */
    @Column(name = "device_token")
    private String deviceToken;
    
    /**
     * Timestamp when user account was created
     * 
     * Automatically set by JPA auditing on first save.
     * Never updated after initial creation.
     * Used for analytics and user tenure tracking.
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when user record was last updated
     * 
     * Automatically updated by JPA auditing on every save.
     * Tracks when profile changes occurred.
     * Used for cache invalidation.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
