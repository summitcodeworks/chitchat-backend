package com.chitchat.messaging.dto;

import com.chitchat.messaging.document.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String groupId;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    @NotNull(message = "Message type is required")
    private Message.MessageType type;
    
    private String mediaUrl;
    private String thumbnailUrl;
    private Long replyToMessageId;
    private List<String> mentions;
    private LocalDateTime scheduledAt;
}
