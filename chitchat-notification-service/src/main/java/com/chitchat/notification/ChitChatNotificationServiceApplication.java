package com.chitchat.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ChitChat Notification Service Application - Push Notification Microservice
 * 
 * This microservice handles all notification delivery for ChitChat:
 * 
 * Core Functionalities:
 * - Push notifications via Firebase Cloud Messaging (FCM)
 * - In-app notification management
 * - Device token registration and management
 * - Notification templates and localization
 * - Notification delivery tracking
 * - Batch notification sending
 * - Silent notifications for data sync
 * - Priority notification handling
 * 
 * Notification Types:
 * - New message notifications
 * - Call notifications (incoming call)
 * - Group activity notifications
 * - Status update notifications
 * - System announcements
 * - OTP delivery notifications
 * 
 * Technology Stack:
 * - Spring Boot microservice framework
 * - Firebase Cloud Messaging (FCM) for push delivery
 * - PostgreSQL for notification history
 * - Feign clients for inter-service communication
 * - Async processing for high throughput
 * 
 * Notification Flow:
 * 1. Service (messaging, calls, etc.) calls notification API
 * 2. Notification service looks up user's device token
 * 3. Notification formatted according to template
 * 4. FCM API called to deliver notification
 * 5. Delivery status tracked in database
 * 6. Retry logic for failed deliveries
 * 
 * Firebase Cloud Messaging (FCM):
 * - Google's cross-platform notification solution
 * - Supports Android, iOS, and web push
 * - Reliable delivery with automatic retry
 * - Message priority levels (high/normal)
 * - Topic-based and device-specific messaging
 * 
 * Key Annotations:
 * @EnableFeignClients - Enable REST clients for calling user-service
 * @EnableAsync - Enable async processing for notification sending
 */
@SpringBootApplication
@EnableFeignClients  // Enable Feign clients (e.g., UserServiceClient to get device tokens)
@EnableAsync  // Enable async methods for non-blocking notification delivery
public class ChitChatNotificationServiceApplication {

    /**
     * Application entry point
     * 
     * Starts the Notification Service microservice on configured port (default: 8084)
     * Registers with Eureka service discovery
     * Initializes Firebase Admin SDK for FCM
     * Sets up async executor for notification processing
     * Configures Feign clients for inter-service calls
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ChitChatNotificationServiceApplication.class, args);
    }
}
