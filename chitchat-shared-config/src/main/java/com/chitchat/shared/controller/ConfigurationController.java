package com.chitchat.shared.controller;

import com.chitchat.shared.dto.ApiResponse;
import com.chitchat.shared.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing application configuration
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @GetMapping("/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfigValue(@PathVariable String configKey) {
        log.info("Getting configuration value for key: {}", configKey);
        
        String value = configurationService.getConfigValue(configKey);
        Map<String, String> result = new HashMap<>();
        result.put("key", configKey);
        result.put("value", value);
        
        return ResponseEntity.ok(ApiResponse.success(result, "Configuration retrieved successfully"));
    }

    @GetMapping("/service/{service}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfigurationsByService(@PathVariable String service) {
        log.info("Getting configurations for service: {}", service);
        
        Map<String, String> configurations = configurationService.getConfigurationsByService(service);
        
        return ResponseEntity.ok(ApiResponse.success(configurations, "Service configurations retrieved successfully"));
    }

    @GetMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfigurationsByServices(@RequestParam List<String> services) {
        log.info("Getting configurations for services: {}", services);
        
        Map<String, String> configurations = configurationService.getConfigurationsByServices(services);
        
        return ResponseEntity.ok(ApiResponse.success(configurations, "Multi-service configurations retrieved successfully"));
    }

    @GetMapping("/pattern/{pattern}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfigurationsByPattern(@PathVariable String pattern) {
        log.info("Getting configurations for pattern: {}", pattern);
        
        Map<String, String> configurations = configurationService.getConfigurationsByKeyPattern(pattern);
        
        return ResponseEntity.ok(ApiResponse.success(configurations, "Pattern-matched configurations retrieved successfully"));
    }

    @PostMapping("/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setConfigValue(
            @PathVariable String configKey,
            @RequestParam String configValue,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "false") Boolean isEncrypted,
            @RequestParam(required = false, defaultValue = "SHARED") String service) {
        
        log.info("Setting configuration value for key: {}", configKey);
        
        configurationService.setConfigValue(configKey, configValue, description, isEncrypted, service);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Configuration updated successfully"));
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteConfigValue(@PathVariable String configKey) {
        log.info("Deleting configuration for key: {}", configKey);
        
        configurationService.deleteConfig(configKey);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Configuration deleted successfully"));
    }

    @PostMapping("/refresh-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        log.info("Refreshing configuration cache");
        
        configurationService.refreshCache();
        
        return ResponseEntity.ok(ApiResponse.success(null, "Configuration cache refreshed successfully"));
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkConfigurationHealth() {
        log.info("Checking configuration health");
        
        Map<String, Object> health = new HashMap<>();
        
        // Check critical configurations
        String jwtSecret = configurationService.getJwtSecret();
        String twilioAccountSid = configurationService.getTwilioAccountSid();
        String twilioAuthToken = configurationService.getTwilioAuthToken();
        String twilioPhoneNumber = configurationService.getTwilioPhoneNumber();
        
        health.put("jwtSecret", jwtSecret != null ? "✓ Configured" : "✗ Missing");
        health.put("twilioAccountSid", twilioAccountSid != null ? "✓ Configured" : "✗ Missing");
        health.put("twilioAuthToken", twilioAuthToken != null ? "✓ Configured" : "✗ Missing");
        health.put("twilioPhoneNumber", twilioPhoneNumber != null ? "✓ Configured" : "✗ Missing");
        
        boolean allConfigured = jwtSecret != null && twilioAccountSid != null && 
                               twilioAuthToken != null && twilioPhoneNumber != null;
        
        health.put("overallStatus", allConfigured ? "HEALTHY" : "UNHEALTHY");
        health.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(ApiResponse.success(health, "Configuration health check completed"));
    }
}
