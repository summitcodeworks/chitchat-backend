package com.chitchat.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ChitChat User Service Application - Core Microservice for User Management
 * 
 * This is the central microservice responsible for all user-related operations:
 * 
 * Core Functionalities:
 * - User Registration and Authentication (SMS/OTP-based)
 * - User Profile Management (name, avatar, about, status)
 * - JWT Token Generation and Validation
 * - Contact Synchronization
 * - User Search and Discovery
 * - Online/Offline Status Management
 * - Phone Number Verification
 * - Device Token Management for Push Notifications
 * 
 * Authentication Flow:
 * 1. User requests OTP via phone number
 * 2. OTP sent via Twilio SMS
 * 3. User submits OTP for verification
 * 4. Upon successful verification, JWT token is issued
 * 5. JWT token used for all subsequent API calls
 * 
 * Technology Stack:
 * - Spring Boot for microservice framework
 * - JPA/Hibernate for database access (PostgreSQL)
 * - Twilio for SMS/OTP delivery
 * - JWT for stateless authentication
 * - Feign clients for inter-service communication
 * - Redis (via shared components) for caching
 * 
 * Key Annotations:
 * @EnableJpaAuditing - Automatic createdAt/updatedAt timestamp management
 * @EnableFeignClients - Enable REST clients for calling other microservices
 * @ComponentScan - Includes shared components from chitchat-shared package
 * @EnableJpaRepositories - Scans both user and shared repositories
 * @EntityScan - Scans both user and shared entities
 */
@SpringBootApplication
@EnableJpaAuditing  // Enables automatic timestamp auditing for entities
@EnableFeignClients  // Enables Feign REST clients (e.g., NotificationServiceClient)
@ComponentScan(basePackages = {"com.chitchat.user", "com.chitchat.shared"})  // Scan user and shared packages
@EnableJpaRepositories(basePackages = {"com.chitchat.user.repository", "com.chitchat.shared.repository"})  // Include shared repos
@EntityScan(basePackages = {"com.chitchat.user.entity", "com.chitchat.shared.entity"})  // Include shared entities like ErrorLog
public class ChitChatUserServiceApplication {

    /**
     * Application entry point
     * 
     * Starts the User Service microservice on configured port (default: 8081)
     * Registers with Eureka service discovery
     * Initializes database connections and JPA repositories
     * Sets up REST endpoints for user management
     * 
     * @param args Command line arguments (typically none for Spring Boot)
     */
    public static void main(String[] args) {
        SpringApplication.run(ChitChatUserServiceApplication.class, args);
    }
}
