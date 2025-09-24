package com.chitchat.shared.service.impl;

import com.chitchat.shared.entity.ApplicationConfig;
import com.chitchat.shared.repository.ApplicationConfigRepository;
import com.chitchat.shared.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of ConfigurationService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationServiceImpl implements ConfigurationService {

    private final ApplicationConfigRepository configRepository;

    @Override
    @Cacheable(value = "config", key = "#configKey")
    public String getConfigValue(String configKey) {
        Optional<ApplicationConfig> config = configRepository.findByConfigKey(configKey);
        if (config.isPresent()) {
            log.debug("Retrieved config value for key: {}", configKey);
            return config.get().getConfigValue();
        }
        log.warn("Configuration not found for key: {}", configKey);
        return null;
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public Integer getConfigValueAsInt(String configKey) {
        String value = getConfigValue(configKey);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.error("Failed to parse config value as Integer for key: {}, value: {}", configKey, value);
            }
        }
        return null;
    }

    @Override
    public Integer getConfigValueAsInt(String configKey, Integer defaultValue) {
        Integer value = getConfigValueAsInt(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public Long getConfigValueAsLong(String configKey) {
        String value = getConfigValue(configKey);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.error("Failed to parse config value as Long for key: {}, value: {}", configKey, value);
            }
        }
        return null;
    }

    @Override
    public Long getConfigValueAsLong(String configKey, Long defaultValue) {
        Long value = getConfigValueAsLong(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public Boolean getConfigValueAsBoolean(String configKey) {
        String value = getConfigValue(configKey);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    @Override
    public Boolean getConfigValueAsBoolean(String configKey, Boolean defaultValue) {
        Boolean value = getConfigValueAsBoolean(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    @Cacheable(value = "configByService", key = "#service")
    public Map<String, String> getConfigurationsByService(String service) {
        List<ApplicationConfig> configs = configRepository.findByService(service);
        Map<String, String> result = new HashMap<>();
        for (ApplicationConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        log.debug("Retrieved {} configurations for service: {}", result.size(), service);
        return result;
    }

    @Override
    @Cacheable(value = "configByServices", key = "#services")
    public Map<String, String> getConfigurationsByServices(List<String> services) {
        List<ApplicationConfig> configs = configRepository.findByServiceIn(services);
        Map<String, String> result = new HashMap<>();
        for (ApplicationConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        log.debug("Retrieved {} configurations for services: {}", result.size(), services);
        return result;
    }

    @Override
    @Cacheable(value = "configByPattern", key = "#keyPattern")
    public Map<String, String> getConfigurationsByKeyPattern(String keyPattern) {
        List<ApplicationConfig> configs = configRepository.findByConfigKeyContaining(keyPattern);
        Map<String, String> result = new HashMap<>();
        for (ApplicationConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        log.debug("Retrieved {} configurations for pattern: {}", result.size(), keyPattern);
        return result;
    }

    @Override
    @Transactional
    public void setConfigValue(String configKey, String configValue) {
        setConfigValue(configKey, configValue, null, false, "SHARED");
    }

    @Override
    @Transactional
    public void setConfigValue(String configKey, String configValue, String description, Boolean isEncrypted, String service) {
        Optional<ApplicationConfig> existingConfig = configRepository.findByConfigKey(configKey);
        
        if (existingConfig.isPresent()) {
            ApplicationConfig config = existingConfig.get();
            config.setConfigValue(configValue);
            config.setDescription(description);
            config.setIsEncrypted(isEncrypted);
            config.setService(service);
            configRepository.save(config);
            log.info("Updated configuration: {}", configKey);
        } else {
            ApplicationConfig config = ApplicationConfig.builder()
                    .configKey(configKey)
                    .configValue(configValue)
                    .description(description)
                    .isEncrypted(isEncrypted)
                    .service(service)
                    .build();
            configRepository.save(config);
            log.info("Created new configuration: {}", configKey);
        }
        
        // Clear cache for this key
        refreshCache();
    }

    @Override
    public boolean hasConfig(String configKey) {
        return configRepository.existsByConfigKey(configKey);
    }

    @Override
    @Transactional
    public void deleteConfig(String configKey) {
        configRepository.deleteByConfigKey(configKey);
        log.info("Deleted configuration: {}", configKey);
        refreshCache();
    }

    @Override
    public void refreshCache() {
        // This would clear the cache if using a cache manager
        // For now, we rely on @Cacheable annotations with TTL
        log.debug("Cache refresh requested");
    }

    // Convenience methods for common configurations
    @Override
    public String getJwtSecret() {
        return getConfigValue("jwt.secret");
    }

    @Override
    public Long getJwtExpiration() {
        return getConfigValueAsLong("jwt.expiration", 3600L);
    }

    @Override
    public String getTwilioAccountSid() {
        return getConfigValue("twilio.account.sid");
    }

    @Override
    public String getTwilioAuthToken() {
        return getConfigValue("twilio.auth.token");
    }

    @Override
    public String getTwilioPhoneNumber() {
        return getConfigValue("twilio.phone.number");
    }

    @Override
    public String getFirebaseProjectId() {
        return getConfigValue("firebase.project.id");
    }

    @Override
    public String getFirebaseWebApiKey() {
        return getConfigValue("firebase.web.api.key");
    }

    @Override
    public String getFirebaseAuthDomain() {
        return getConfigValue("firebase.auth.domain");
    }

    @Override
    public String getFirebaseStorageBucket() {
        return getConfigValue("firebase.storage.bucket");
    }

    @Override
    public String getDatabasePassword() {
        return getConfigValue("database.password");
    }
}
