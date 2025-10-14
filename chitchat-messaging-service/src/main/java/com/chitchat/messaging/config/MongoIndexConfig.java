package com.chitchat.messaging.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * MongoDB Index Configuration for Performance Optimization
 * 
 * Creates compound indexes to optimize query performance for:
 * - Conversation message retrieval
 * - Unread message counts
 * - Message status updates
 * - Group message queries
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        log.info("Creating MongoDB indexes for messages collection...");

        try {
            IndexOperations indexOps = mongoTemplate.indexOps("messages");

            // Index 1: Compound index for conversation queries (senderId, recipientId, createdAt)
            // Optimizes: findConversationMessages query
            indexOps.ensureIndex(new Index()
                    .on("senderId", Sort.Direction.ASC)
                    .on("recipientId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("idx_sender_recipient_created"));

            // Index 2: Compound index for reverse conversation queries (recipientId, senderId, createdAt)
            // Optimizes: findConversationMessages query (bi-directional)
            indexOps.ensureIndex(new Index()
                    .on("recipientId", Sort.Direction.ASC)
                    .on("senderId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("idx_recipient_sender_created"));

            // Index 3: Compound index for unread message counts (recipientId, status)
            // Optimizes: countTotalUnreadMessages and findUnreadCountsBySender
            indexOps.ensureIndex(new Index()
                    .on("recipientId", Sort.Direction.ASC)
                    .on("status", Sort.Direction.ASC)
                    .named("idx_recipient_status"));

            // Index 4: Compound index for group messages (groupId, createdAt)
            // Optimizes: findGroupMessages query
            indexOps.ensureIndex(new Index()
                    .on("groupId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("idx_group_created"));

            // Index 5: Single index on senderId for sender's messages
            // Optimizes: findUnreadMessagesForSender
            indexOps.ensureIndex(new Index()
                    .on("senderId", Sort.Direction.ASC)
                    .named("idx_sender"));

            // Index 6: Single index on createdAt for time-based queries
            // Optimizes: findUndeliveredMessages and time range queries
            indexOps.ensureIndex(new Index()
                    .on("createdAt", Sort.Direction.DESC)
                    .named("idx_created"));

            log.info("MongoDB indexes created successfully for messages collection");

        } catch (Exception e) {
            log.error("Failed to create MongoDB indexes", e);
        }
    }
}

