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
 * Entity to store OTP (One-Time Password) request and verification data
 * 
 * This entity provides complete audit trail for SMS-based authentication:
 * - OTP generation and delivery
 * - Verification attempts and results
 * - Security tracking (IP, user agent, headers)
 * - Twilio SMS delivery status
 * - Rate limiting data
 * 
 * Database Table: otp_requests
 * 
 * Security Features:
 * - OTP expires after 5 minutes
 * - Limited verification attempts (prevents brute force)
 * - Complete request/response logging for audit
 * - IP address and user agent tracking
 * - Twilio message SID for delivery confirmation
 * 
 * Lifecycle:
 * 1. User requests OTP -> Record created with generated OTP
 * 2. SMS sent via Twilio -> smsSent=true, twilioMessageSid set
 * 3. User submits OTP -> verificationAttempts incremented
 * 4. On success -> isVerified=true, verifiedAt set
 * 5. After 5 minutes -> OTP becomes invalid (expiresAt check)
 * 
 * Used for:
 * - User registration (new phone number verification)
 * - User login (existing user authentication)
 * - Phone number change verification
 * - Security auditing and compliance
 */
@Entity
@Table(name = "otp_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OtpRequest {

    /**
     * Unique identifier for this OTP request (auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Phone number for which OTP was requested
     * 
     * Format: E.164 international format (e.g., +14155552671)
     * Used to:
     * - Send OTP via SMS
     * - Look up OTP during verification
     * - Track OTP request rate per phone number
     */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * The generated OTP code
     * 
     * Typically 6-digit numeric code (e.g., "123456")
     * Randomly generated for each request
     * Stored in plain text (temporary, expires in 5 minutes)
     * 
     * Security Note: Not hashed because:
     * - Very short lifetime (5 minutes)
     * - Single use only
     * - Rate limited per phone number
     * - Verification attempts limited
     */
    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    /**
     * IP address from which the OTP request originated
     * 
     * Supports both IPv4 and IPv6 (max 45 chars)
     * Used for:
     * - Security auditing
     * - Fraud detection
     * - Rate limiting by IP
     * - Geographic analysis
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;

    /**
     * User agent string of the requesting client
     * 
     * Contains browser/app and OS information
     * Examples:
     * - "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)"
     * - "ChitChat-Android/1.0.5"
     * 
     * Used for:
     * - Device type analysis
     * - App version tracking
     * - Compatibility issues
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Complete HTTP request headers (JSON or text format)
     * 
     * Captured for complete audit trail.
     * Sensitive headers (Authorization, Cookie) are masked.
     * 
     * Used for:
     * - Security investigation
     * - Debugging issues
     * - Compliance auditing
     */
    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    /**
     * Request payload/body (JSON serialized)
     * 
     * Contains the SendOtpRequest data.
     * Typically just the phone number.
     * 
     * Used for:
     * - Audit trail
     * - Debugging
     * - Compliance
     */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /**
     * Response status (SUCCESS, ERROR, PENDING)
     * 
     * Indicates the outcome of the OTP request processing.
     * Set after SMS sending attempt.
     * 
     * Values:
     * - SUCCESS: OTP sent successfully
     * - ERROR: Failed to send OTP
     * - PENDING: Still processing
     */
    @Column(name = "response_status", length = 20)
    private String responseStatus;

    /**
     * Human-readable response message
     * 
     * Examples:
     * - "OTP sent successfully via SMS"
     * - "Failed to send OTP. Please try again."
     * 
     * Used for logging and user communication.
     */
    @Column(name = "response_message", length = 500)
    private String responseMessage;

    /**
     * Response payload (JSON serialized ApiResponse)
     * 
     * Complete API response sent to client.
     * Used for complete request/response logging.
     */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    /**
     * Flag indicating if SMS was successfully sent
     * 
     * True: SMS delivered to Twilio successfully
     * False: SMS sending failed
     * Null: SMS not attempted yet
     * 
     * Critical for determining OTP delivery success.
     */
    @Column(name = "sms_sent")
    private Boolean smsSent;

    /**
     * Error message if SMS sending failed
     * 
     * Contains exception message or Twilio error details.
     * Used for troubleshooting SMS delivery issues.
     * 
     * Common errors:
     * - Invalid phone number format
     * - Twilio account issues
     * - Network timeout
     */
    @Column(name = "sms_error_message", length = 1000)
    private String smsErrorMessage;

    /**
     * Twilio message SID (unique identifier)
     * 
     * Format: "SM" followed by 32 hex characters
     * Example: "SM9b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5"
     * 
     * Used to:
     * - Track SMS delivery status via Twilio API
     * - Verify SMS was sent
     * - Get delivery receipts
     * - Billing reconciliation
     */
    @Column(name = "twilio_message_sid", length = 100)
    private String twilioMessageSid;

    /**
     * Flag indicating if OTP was successfully verified
     * 
     * False initially, set to true after successful verification.
     * Used to prevent OTP reuse.
     * 
     * Verification flow:
     * 1. User submits OTP
     * 2. If correct, this is set to true
     * 3. verifiedAt timestamp is set
     * 4. OTP is cleared from Redis
     */
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    /**
     * Timestamp when OTP was successfully verified
     * 
     * Null if not yet verified.
     * Set to current time upon successful verification.
     * 
     * Used for:
     * - Calculating verification time
     * - Security auditing
     * - User behavior analysis
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Timestamp when OTP expires and becomes invalid
     * 
     * Default: createdAt + 5 minutes
     * After this time, OTP verification will fail.
     * 
     * Automatically set in @PrePersist onCreate() method.
     * 
     * Security: Short expiration reduces brute force window.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Number of times user attempted to verify this OTP
     * 
     * Incremented each time user submits OTP for verification.
     * Used for:
     * - Brute force protection (max 3 attempts)
     * - User experience metrics
     * - Security monitoring
     * 
     * If attempts > 3, user must request new OTP.
     */
    @Column(name = "verification_attempts")
    @Builder.Default
    private Integer verificationAttempts = 0;

    /**
     * Timestamp of the most recent verification attempt
     * 
     * Updated each time user tries to verify OTP.
     * Used for:
     * - Rate limiting verification attempts
     * - Detecting suspicious patterns
     * - User flow analysis
     */
    @Column(name = "last_verification_attempt")
    private LocalDateTime lastVerificationAttempt;

    /**
     * Timestamp when this OTP request was created
     * 
     * Automatically set by JPA auditing.
     * Used as baseline for expiration calculation.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this record was last updated
     * 
     * Automatically updated by JPA auditing on every change.
     * Tracks when verification attempts, status, etc. were modified.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback executed before entity is first persisted
     * 
     * Sets default values:
     * - createdAt if not already set
     * - expiresAt to createdAt + 5 minutes
     * 
     * This ensures OTP expiration is always properly configured.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            // OTP expires 5 minutes after creation
            expiresAt = createdAt.plusMinutes(5);
        }
    }

    /**
     * JPA lifecycle callback executed before entity is updated
     * 
     * Manually sets updatedAt timestamp.
     * Redundant with @LastModifiedDate but ensures timestamp is always set.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}