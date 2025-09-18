package com.chitchat.messaging.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Message document for MongoDB
 */
@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    private String id;
    
    private Long senderId;
    private Long recipientId;
    private String groupId;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private String mediaUrl;
    private String thumbnailUrl;
    private Long replyToMessageId;
    private List<String> mentions;
    private LocalDateTime scheduledAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum MessageType {
        TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION, CONTACT, STICKER
    }
    
    public enum MessageStatus {
        SENT, DELIVERED, READ, FAILED
    }
}
