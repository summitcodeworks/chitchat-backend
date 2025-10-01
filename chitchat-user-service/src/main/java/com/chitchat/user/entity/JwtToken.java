package com.chitchat.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JWT Token entity for token management and revocation
 * 
 * This entity stores issued JWT tokens in the database for:
 * - Token blacklisting/revocation
 * - Session management
 * - Security auditing
 * - User login tracking
 * - Force logout functionality
 * 
 * Database Table: jwt_tokens
 * 
 * Use Cases:
 * - Revoke specific tokens when user logs out
 * - Revoke all tokens when password changes
 * - Track active sessions per user
 * - Implement "logout from all devices"
 * - Security incident response (revoke compromised tokens)
 * 
 * Note: JWT tokens are stateless by design, but storing them allows:
 * - Explicit revocation before natural expiration
 * - Session management across devices
 * - Enhanced security controls
 */
@Entity
@Table(name = "jwt_tokens")
public class JwtToken {
    
    /**
     * Unique identifier for the token record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The actual JWT token string
     * 
     * Unique constraint prevents duplicate token storage.
     * Length 1000 accommodates full JWT with claims.
     * 
     * Format: header.payload.signature (base64 encoded)
     */
    @Column(name = "token", unique = true, nullable = false, length = 1000)
    private String token;
    
    /**
     * ID of user who owns this token
     * 
     * Used for:
     * - Finding all tokens for a user
     * - Revoking all user's tokens
     * - Session count per user
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Phone number of token owner
     * 
     * Stored for quick lookup and auditing.
     * Matches the 'subject' claim in the JWT.
     */
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    
    /**
     * When the token was issued
     * 
     * Matches the 'iat' (issued at) claim in JWT.
     * Used for session duration tracking.
     */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
    
    /**
     * When the token expires
     * 
     * Matches the 'exp' (expiration) claim in JWT.
     * Default: 24 hours from issuedAt.
     * Used for cleanup of expired tokens.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Whether this token has been explicitly revoked
     * 
     * true: Token is blacklisted (logout, force logout, security)
     * false: Token is still valid (if not expired)
     * 
     * Revoked tokens cannot be used even if not expired.
     */
    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;
    
    /**
     * Timestamp when this record was created
     * 
     * Used for auditing and cleanup.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this record was last updated
     * 
     * Updated when token is revoked or modified.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Default constructor
     * 
     * Initializes:
     * - createdAt to current time
     * - isRevoked to false
     */
    public JwtToken() {
        this.createdAt = LocalDateTime.now();
        this.isRevoked = false;
    }
    
    /**
     * Constructor with all required fields
     * 
     * @param token The JWT token string
     * @param userId Owner's user ID
     * @param phoneNumber Owner's phone number
     * @param issuedAt When token was issued
     * @param expiresAt When token expires
     */
    public JwtToken(String token, Long userId, String phoneNumber, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this();
        this.token = token;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Boolean getIsRevoked() {
        return isRevoked;
    }
    
    public void setIsRevoked(Boolean isRevoked) {
        this.isRevoked = isRevoked;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * JPA lifecycle callback executed before entity update
     * 
     * Automatically sets updatedAt timestamp.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Checks if token has expired based on current time
     * 
     * @return true if current time is after expiration time
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    /**
     * Checks if token is currently valid for authentication
     * 
     * Token is valid if:
     * - Not explicitly revoked AND
     * - Not expired
     * 
     * @return true if token can be used for authentication
     */
    public boolean isValid() {
        return !this.isRevoked && !this.isExpired();
    }
}
