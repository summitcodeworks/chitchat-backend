package com.chitchat.shared.service;

import java.util.List;
import java.util.Map;

/**
 * Service for managing application configuration and secrets
 */
public interface ConfigurationService {

    /**
     * Get configuration value by key
     */
    String getConfigValue(String configKey);

    /**
     * Get configuration value by key with default value
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * Get configuration value as Integer
     */
    Integer getConfigValueAsInt(String configKey);

    /**
     * Get configuration value as Integer with default value
     */
    Integer getConfigValueAsInt(String configKey, Integer defaultValue);

    /**
     * Get configuration value as Long
     */
    Long getConfigValueAsLong(String configKey);

    /**
     * Get configuration value as Long with default value
     */
    Long getConfigValueAsLong(String configKey, Long defaultValue);

    /**
     * Get configuration value as Boolean
     */
    Boolean getConfigValueAsBoolean(String configKey);

    /**
     * Get configuration value as Boolean with default value
     */
    Boolean getConfigValueAsBoolean(String configKey, Boolean defaultValue);

    /**
     * Get all configurations for a service
     */
    Map<String, String> getConfigurationsByService(String service);

    /**
     * Get all configurations for multiple services
     */
    Map<String, String> getConfigurationsByServices(List<String> services);

    /**
     * Get configurations by key pattern
     */
    Map<String, String> getConfigurationsByKeyPattern(String keyPattern);

    /**
     * Set configuration value
     */
    void setConfigValue(String configKey, String configValue);

    /**
     * Set configuration value with description and encryption flag
     */
    void setConfigValue(String configKey, String configValue, String description, Boolean isEncrypted, String service);

    /**
     * Check if configuration exists
     */
    boolean hasConfig(String configKey);

    /**
     * Delete configuration
     */
    void deleteConfig(String configKey);

    /**
     * Refresh cache (if caching is enabled)
     */
    void refreshCache();

    /**
     * Get JWT secret
     */
    String getJwtSecret();

    /**
     * Get JWT expiration time
     */
    Long getJwtExpiration();

    /**
     * Get Twilio Account SID
     */
    String getTwilioAccountSid();

    /**
     * Get Twilio Auth Token
     */
    String getTwilioAuthToken();

    /**
     * Get Twilio Phone Number
     */
    String getTwilioPhoneNumber();

    /**
     * Get Firebase Project ID
     */
    String getFirebaseProjectId();

    /**
     * Get Firebase Web API Key
     */
    String getFirebaseWebApiKey();

    /**
     * Get Firebase Auth Domain
     */
    String getFirebaseAuthDomain();

    /**
     * Get Firebase Storage Bucket
     */
    String getFirebaseStorageBucket();

    /**
     * Get Database Password
     */
    String getDatabasePassword();
}
