package com.chitchat.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User action entity for comprehensive audit logging
 * 
 * This entity tracks all administrative actions performed on users and system resources.
 * It provides a complete audit trail for:
 * - Compliance and regulatory requirements
 * - Security monitoring
 * - User activity tracking
 * - Debugging and troubleshooting
 * 
 * Database Table: user_actions
 * 
 * Every significant action in the system is logged here with full context including:
 * who performed it, what was done, when it happened, and the result.
 */
@Entity
@Table(name = "user_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserAction {
    
    /**
     * Unique identifier for the action log entry (auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID of the user on whom the action was performed
     * Can be an admin ID for admin actions or regular user ID for user actions
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * Type of action performed
     * Examples: SUSPEND_USER, DELETE_USER, EXPORT_DATA, GENERATE_REPORT
     */
    @Column(nullable = false)
    private String action;
    
    /**
     * Type of resource the action was performed on
     * Examples: USER, MESSAGE, GROUP, ADMIN, COMPLIANCE
     */
    @Column(nullable = false)
    private String resource;
    
    /**
     * Specific identifier of the resource acted upon
     * Example: User ID, Message ID, Group ID
     */
    private String resourceId;
    
    /**
     * Additional contextual information about the action
     * Can include reason for action, parameters used, etc.
     */
    private String details;
    
    /**
     * IP address from which the action was initiated
     * Important for security auditing
     */
    private String ipAddress;
    
    /**
     * Browser/client user agent string
     * Helps identify the client application used
     */
    private String userAgent;
    
    /**
     * Current status of the action
     * Stored as string for readability in database
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;
    
    /**
     * Timestamp when the action was initiated
     * Automatically set by JPA auditing
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Enum representing the completion status of an action
     * 
     * SUCCESS: Action completed successfully
     * FAILED: Action failed due to error or validation
     * PENDING: Action is still in progress (for async operations)
     */
    public enum ActionStatus {
        SUCCESS, FAILED, PENDING
    }
}
