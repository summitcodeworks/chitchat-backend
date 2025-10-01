package com.chitchat.admin.entity;

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
 * Admin user entity for administrative access to ChitChat system
 * 
 * This entity represents administrators who have special privileges to:
 * - Manage regular users
 * - View analytics and system metrics
 * - Generate compliance reports
 * - Export user data
 * - Perform moderation actions
 * 
 * Database Table: admin_users
 * 
 * The entity uses JPA auditing for automatic tracking of creation and modification timestamps.
 * Passwords are stored using BCrypt hashing for security.
 */
@Entity
@Table(name = "admin_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AdminUser {
    
    /**
     * Unique identifier for the admin user (auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique username for admin login
     * Must be unique across all admin users
     */
    @Column(unique = true, nullable = false)
    private String username;
    
    /**
     * Email address of the admin user
     * Used for communication and password recovery
     */
    @Column(nullable = false)
    private String email;
    
    /**
     * Encrypted password (BCrypt hashed)
     * Never store plain text passwords - always use password encoder
     */
    @Column(nullable = false)
    private String password;
    
    /**
     * Role defining the admin's permissions level
     * Stored as string in database for readability
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role;
    
    /**
     * Flag indicating if the admin account is active
     * Inactive accounts cannot log in
     */
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * Timestamp of the last successful login
     * Used for security auditing and session management
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    /**
     * Timestamp when the admin account was created
     * Automatically set by JPA auditing, cannot be modified
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the admin account was last modified
     * Automatically updated by JPA auditing on any change
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Enum defining different levels of admin access
     * 
     * SUPER_ADMIN: Full system access, can manage other admins
     * ADMIN: Can manage users and view analytics
     * MODERATOR: Can moderate content and users
     * ANALYST: Read-only access to analytics and reports
     */
    public enum AdminRole {
        SUPER_ADMIN, ADMIN, MODERATOR, ANALYST
    }
}
