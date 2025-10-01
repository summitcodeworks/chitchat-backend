package com.chitchat.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for admin service
 * 
 * This configuration class provides security-related beans for the admin service.
 * Currently focuses on password encoding for secure storage and validation of admin credentials.
 * 
 * Key Features:
 * - BCrypt password encoding for admin user passwords
 * - Uses strong hashing algorithm for password security
 * - Provides password encoder bean for dependency injection across the admin service
 */
@Configuration
public class SecurityConfig {

    /**
     * Creates and configures the password encoder bean for admin service
     * 
     * Uses BCryptPasswordEncoder which:
     * - Applies BCrypt strong hashing algorithm
     * - Includes salt for each password hash
     * - Makes password cracking computationally expensive
     * - Recommended by Spring Security for production use
     * 
     * @return PasswordEncoder instance configured with BCrypt algorithm
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
