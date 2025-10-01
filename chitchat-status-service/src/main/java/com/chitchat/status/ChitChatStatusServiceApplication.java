package com.chitchat.status;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ChitChat Status Service Application - Ephemeral Stories Microservice
 * 
 * This microservice handles WhatsApp/Instagram-style status stories:
 * 
 * Core Functionalities:
 * - Status story creation and upload
 * - Ephemeral content (auto-delete after 24 hours)
 * - Status viewing and view tracking
 * - Status privacy controls (all contacts, selected, hidden)
 * - Status reactions
 * - Status reply messages
 * - Media status (photos, videos, text)
 * - Status muting per contact
 * 
 * Status Types:
 * - TEXT: Text-only status with background color
 * - IMAGE: Photo status with optional caption
 * - VIDEO: Video status with optional caption
 * - LINK: Shared link with preview
 * 
 * Technology Stack:
 * - Spring Boot microservice framework
 * - MongoDB for status storage (document database)
 * - Scheduled tasks for auto-deletion (24-hour lifecycle)
 * - Media service integration for photo/video uploads
 * - User service integration for privacy lists
 * 
 * Why MongoDB?
 * - Flexible schema for different status types
 * - TTL indexes for automatic 24-hour deletion
 * - High write throughput for frequent status updates
 * - Efficient querying for status feeds
 * 
 * Auto-Deletion Mechanism:
 * - MongoDB TTL (Time-To-Live) index on createdAt field
 * - Documents automatically deleted 24 hours after creation
 * - Scheduled cleanup task runs periodically as backup
 * - Media files also deleted when status expires
 * 
 * Status Lifecycle:
 * 1. User creates status (photo/video/text)
 * 2. Status saved to MongoDB with 24-hour TTL
 * 3. Media files uploaded to media-service/CDN
 * 4. Status visible to contacts based on privacy settings
 * 5. View tracking updates when contacts view status
 * 6. After 24 hours, MongoDB auto-deletes document
 * 7. Scheduled task cleans up orphaned media files
 * 
 * Privacy Levels:
 * - MY_CONTACTS: Visible to all contacts (default)
 * - MY_CONTACTS_EXCEPT: Visible to contacts except blocked list
 * - ONLY_SHARE_WITH: Visible only to selected contacts
 * - NONE: Status feature disabled for this user
 * 
 * Key Annotations:
 * @EnableScheduling - Enables scheduled tasks for cleanup and maintenance
 * @SpringBootApplication(exclude={...}) - Excludes JPA/SQL (uses MongoDB only)
 */
@SpringBootApplication(exclude = {
    // Exclude SQL/JPA dependencies since this service uses MongoDB
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@EnableScheduling  // Enable scheduled tasks for status cleanup and expiration
public class ChitChatStatusServiceApplication {

    /**
     * Application entry point
     * 
     * Starts the Status Service microservice on configured port (default: 8085)
     * Registers with Eureka service discovery
     * Initializes MongoDB with TTL indexes
     * Configures scheduled tasks for cleanup
     * Sets up REST endpoints for status management
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ChitChatStatusServiceApplication.class, args);
    }
}
