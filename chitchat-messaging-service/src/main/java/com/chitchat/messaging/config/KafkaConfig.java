package com.chitchat.messaging.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for messaging service
 * 
 * Configures Apache Kafka producer for real-time message event streaming.
 * 
 * Purpose:
 * When a message is sent:
 * 1. Message saved to MongoDB
 * 2. Event published to Kafka topic
 * 3. Notification service consumes event
 * 4. Push notification sent to recipient
 * 5. Other services can subscribe for analytics, etc.
 * 
 * Why Kafka?
 * - Decouples message storage from notification delivery
 * - High throughput for real-time messaging
 * - Reliable event delivery (at-least-once guarantee)
 * - Scalable (handles millions of messages)
 * - Multiple consumers can process same events
 * - Event replay capability
 * 
 * Configuration:
 * - Key: String (typically message ID or user ID)
 * - Value: JSON object (message details)
 * - Serialization: JSON for flexibility
 * - Bootstrap servers: Kafka cluster connection
 * 
 * Topics Used:
 * - message-events: New message notifications
 * - message-status-events: Delivery/read receipts
 * - typing-events: Typing indicators (optional)
 */
@Configuration
public class KafkaConfig {

    /**
     * Kafka cluster bootstrap servers
     * 
     * Format: "host1:port1,host2:port2,..."
     * Example: "localhost:9092" or "kafka-1:9092,kafka-2:9092"
     * 
     * Configured in application.yml
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates Kafka producer factory with JSON serialization
     * 
     * Producer Configuration:
     * - BOOTSTRAP_SERVERS: Kafka cluster connection
     * - KEY_SERIALIZER: String keys (message/user IDs)
     * - VALUE_SERIALIZER: JSON for complex objects
     * - ADD_TYPE_INFO_HEADERS: false (no Java type info in headers)
     * 
     * The producer is thread-safe and can be shared across the application.
     * 
     * @return ProducerFactory configured for String keys and JSON values
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Kafka broker addresses
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serialize keys as strings (message IDs, user IDs)
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Serialize values as JSON (message objects, events)
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Don't add Java type information to headers
        // Makes it easier for non-Java consumers to process events
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates Kafka template for sending messages to topics
     * 
     * KafkaTemplate is the main Spring abstraction for Kafka operations.
     * Provides:
     * - Synchronous and asynchronous send methods
     * - Automatic serialization
     * - Error handling and callbacks
     * - Transaction support
     * 
     * Usage:
     * kafkaTemplate.send("message-events", messageId, messageEvent);
     * 
     * @return KafkaTemplate configured with the producer factory
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
