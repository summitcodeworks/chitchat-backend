package com.chitchat.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing dynamic application configuration and secrets
 * 
 * This entity provides centralized configuration management across all microservices.
 * Allows runtime configuration changes without redeploying services.
 * 
 * Database Table: application_config
 * 
 * Key Features:
 * - Centralized config storage (single source of truth)
 * - Service-specific configurations
 * - Encryption support for sensitive values
 * - Runtime updates without restart
 * - Configuration versioning via timestamps
 * - Descriptions for documentation
 * 
 * Use Cases:
 * - API keys and secrets (Twilio, Firebase, AWS)
 * - Feature flags (enable/disable features)
 * - Rate limits and thresholds
 * - JWT secret and expiration
 * - External service URLs
 * - Business logic parameters
 * 
 * Security:
 * - Sensitive values marked with isEncrypted flag
 * - Values can be encrypted at rest
 * - Access controlled via API
 * - Audit trail via timestamps
 * 
 * Example Configurations:
 * - Key: "jwt.secret", Value: "your-secret-key", Service: "user-service"
 * - Key: "twilio.account.sid", Value: "ACxxx", Service: "user-service", Encrypted: true
 * - Key: "otp.expiry.minutes", Value: "5", Service: "user-service"
 * - Key: "max.group.members", Value: "256", Service: "messaging-service"
 */
@Entity
@Table(name = "application_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationConfig {

    /**
     * Unique identifier for the configuration entry
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Configuration key (unique identifier)
     * 
     * Naming convention: dot-separated hierarchy
     * Examples:
     * - "jwt.secret"
     * - "twilio.account.sid"
     * - "firebase.credentials.path"
     * - "feature.group.calls.enabled"
     * 
     * Must be unique across all services.
     */
    @Column(name = "config_key", unique = true, nullable = false, length = 100)
    private String configKey;

    /**
     * Configuration value
     * 
     * Can be:
     * - Plain text for non-sensitive configs
     * - Encrypted string for sensitive configs
     * - JSON for complex configurations
     * 
     * TEXT column type supports large values.
     */
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    /**
     * Human-readable description of the configuration
     * 
     * Explains:
     * - What the config is for
     * - Valid values or format
     * - Impact of changing it
     * 
     * Example: "JWT token expiration time in seconds. Default: 86400 (24 hours)"
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Flag indicating if the value is encrypted
     * 
     * true: Value is encrypted at rest (API keys, secrets)
     * false: Value is plain text (URLs, numbers, flags)
     * 
     * Encrypted values are decrypted by ConfigurationService before use.
     */
    @Builder.Default
    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = false;

    /**
     * Service name this configuration belongs to
     * 
     * Groups configurations by microservice.
     * Examples: "user-service", "messaging-service", "all"
     * 
     * Services only load their own configs for security and performance.
     */
    @Column(name = "service", nullable = false, length = 50)
    private String service;

    /**
     * Timestamp when configuration was created
     * 
     * Automatically set by Hibernate on insert.
     * Never updated.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when configuration was last modified
     * 
     * Automatically updated by Hibernate on every change.
     * Used for cache invalidation and config versioning.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
