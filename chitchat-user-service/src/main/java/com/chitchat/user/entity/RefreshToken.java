package com.chitchat.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a refresh token for user authentication
 * 
 * Refresh tokens are long-lived tokens used to obtain new access tokens
 * without requiring the user to re-authenticate. They provide a balance
 * between security and user experience.
 * 
 * Security Features:
 * - Longer expiration than access tokens (typically 30 days vs 1 hour)
 * - Can be revoked individually or in bulk per user
 * - Tracked in database for audit and security purposes
 * - One-time use: revoked after generating new access token
 * 
 * Flow:
 * 1. User authenticates -> receives access token + refresh token
 * 2. Access token expires -> client uses refresh token to get new access token
 * 3. Refresh token is rotated (old one revoked, new one issued)
 * 4. Process repeats until refresh token expires or is revoked
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token", columnList = "token"),
    @Index(name = "idx_refresh_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The actual refresh token string (UUID format)
     */
    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;
    
    /**
     * ID of the user this token belongs to
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Phone number of the user (denormalized for quick lookup)
     */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;
    
    /**
     * When the refresh token was issued
     */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
    
    /**
     * When the refresh token will expire
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Whether the token has been revoked (manually or after use)
     */
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;
    
    /**
     * When the token was last used to refresh an access token
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    /**
     * Device information (optional, for security tracking)
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
    
    /**
     * IP address when token was created (optional, for security)
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Checks if refresh token has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    /**
     * Checks if refresh token is valid (not revoked and not expired)
     */
    public boolean isValid() {
        return !this.isRevoked && !this.isExpired();
    }
}

