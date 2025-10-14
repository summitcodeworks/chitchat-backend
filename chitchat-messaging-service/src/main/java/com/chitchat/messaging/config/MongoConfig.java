package com.chitchat.messaging.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB Configuration for Optimized Performance
 * 
 * Configures:
 * - Connection pooling with larger pool sizes
 * - Read preference for better distribution
 * - Write concern for faster writes
 * - Timeout settings
 * - Auditing support
 */
@Slf4j
@Configuration
@EnableMongoAuditing
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return "chitchat";
    }

    @Override
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> 
                    builder.maxSize(100)  // Increased from 50 for better concurrency
                           .minSize(20)    // Increased from 10 to keep more connections ready
                           .maxWaitTime(1, TimeUnit.SECONDS)  // Reduced from 2 for faster failures
                           .maxConnectionIdleTime(3, TimeUnit.MINUTES)  // Reduced to recycle connections faster
                           .maxConnectionLifeTime(20, TimeUnit.MINUTES))  // Reduced to prevent stale connections
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(5, TimeUnit.SECONDS)  // Reduced from 10 for faster failures
                           .readTimeout(10, TimeUnit.SECONDS))   // Reduced from 20 for faster responses
                .applyToServerSettings(builder ->
                    builder.heartbeatFrequency(5, TimeUnit.SECONDS)  // Reduced from 10 for faster failover detection
                           .minHeartbeatFrequency(500, TimeUnit.MILLISECONDS))
                .readPreference(ReadPreference.primaryPreferred())  // Read from primary, fallback to secondary
                .writeConcern(WriteConcern.W1.withJournal(false))  // Fast writes, acknowledged by primary only
                .retryWrites(true)  // Enable automatic retry for write operations
                .retryReads(true)   // Enable automatic retry for read operations
                .build();
        
        log.info("MongoDB client configured with high-performance settings - Pool size: 100, Read preference: primaryPreferred");
        return MongoClients.create(settings);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}

