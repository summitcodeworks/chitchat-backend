package com.chitchat.messaging.dto;

import com.chitchat.messaging.document.Message;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for sending message request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    private Long recipientId;
    private Long receiverId; // Alternative field name for backward compatibility
    private String groupId;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    // Optional - defaults to TEXT if not provided
    @Builder.Default
    private Message.MessageType type = Message.MessageType.TEXT;
    
    private String mediaUrl;
    private String thumbnailUrl;
    private String replyToMessageId;
    private List<String> mentions;
    private LocalDateTime scheduledAt;
}
