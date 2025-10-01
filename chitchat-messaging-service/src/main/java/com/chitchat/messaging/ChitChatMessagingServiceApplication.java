package com.chitchat.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * ChitChat Messaging Service Application - Real-time Chat Microservice
 * 
 * This microservice handles all messaging-related operations for ChitChat:
 * 
 * Core Functionalities:
 * - One-on-one messaging between users
 * - Group chat creation and management
 * - Message delivery tracking (sent, delivered, read receipts)
 * - Media message handling (images, videos, documents)
 * - Message search and history
 * - Real-time message notifications via Kafka
 * - Message encryption (in transit)
 * - Typing indicators
 * - Message reactions and replies
 * 
 * Technology Stack:
 * - Spring Boot microservice framework
 * - MongoDB for message storage (document database)
 * - Kafka for real-time event streaming
 * - WebSocket for live message delivery
 * - REST API for message history and management
 * 
 * Why MongoDB?
 * - Flexible schema for different message types (text, media, location)
 * - High write throughput for real-time messaging
 * - Horizontal scalability for growing message volumes
 * - Document model fits message structure naturally
 * - Efficient querying with indexes on senderId, recipientId, groupId
 * 
 * Message Flow:
 * 1. User sends message via REST API
 * 2. Message saved to MongoDB
 * 3. Event published to Kafka topic
 * 4. Notification service picks up event
 * 5. Push notification sent to recipient
 * 6. Recipient receives message via WebSocket or on app open
 * 7. Delivery/read receipts tracked in message status
 * 
 * Key Annotations:
 * @EnableMongoAuditing - Automatic createdAt/updatedAt for MongoDB documents
 * @SpringBootApplication(exclude={...}) - Excludes JPA/SQL dependencies (uses MongoDB only)
 */
@SpringBootApplication(exclude = {
    // Exclude SQL/JPA dependencies since this service uses MongoDB
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@EnableMongoAuditing  // Enable automatic timestamp management for MongoDB documents
public class ChitChatMessagingServiceApplication {

    /**
     * Application entry point
     * 
     * Starts the Messaging Service microservice on configured port (default: 8082)
     * Registers with Eureka service discovery
     * Initializes MongoDB connections
     * Sets up Kafka producers/consumers
     * Configures WebSocket endpoints for real-time messaging
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ChitChatMessagingServiceApplication.class, args);
    }
}
