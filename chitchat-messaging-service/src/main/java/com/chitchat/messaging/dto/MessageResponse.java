package com.chitchat.messaging.dto;

import com.chitchat.messaging.document.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for message response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private String id;
    private Long senderId;
    private Long recipientId;
    private String groupId;
    private String content;
    private Message.MessageType type;
    private Message.MessageStatus status;
    private String mediaUrl;
    private String thumbnailUrl;
    private Long replyToMessageId;
    private List<String> mentions;
    private LocalDateTime scheduledAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
