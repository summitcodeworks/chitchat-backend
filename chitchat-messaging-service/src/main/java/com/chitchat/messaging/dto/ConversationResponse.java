package com.chitchat.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for conversation list items
 * Contains conversation summary with latest message and user details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    
    /**
     * Other user's ID in the conversation
     */
    private Long userId;
    
    /**
     * Other user's name
     */
    private String userName;
    
    /**
     * Other user's avatar URL
     */
    private String userAvatar;
    
    /**
     * Other user's online status
     */
    private String userStatus;
    
    /**
     * Latest message ID
     */
    private String latestMessageId;
    
    /**
     * Latest message content
     */
    private String latestMessageContent;
    
    /**
     * Latest message type (TEXT, IMAGE, etc.)
     */
    private String latestMessageType;
    
    /**
     * Latest message sender ID
     */
    private Long latestMessageSenderId;
    
    /**
     * Latest message sender name
     */
    private String latestMessageSenderName;
    
    /**
     * Latest message timestamp
     */
    private LocalDateTime latestMessageTime;
    
    /**
     * Latest message status (SENT, DELIVERED, READ)
     */
    private String latestMessageStatus;
    
    /**
     * Number of unread messages in this conversation
     */
    private Integer unreadCount;
    
    /**
     * Whether the other user is currently typing
     */
    private Boolean isTyping;
    
    /**
     * Conversation type (INDIVIDUAL, GROUP)
     */
    private String conversationType;
    
    /**
     * Group ID if this is a group conversation
     */
    private String groupId;
    
    /**
     * Group name if this is a group conversation
     */
    private String groupName;
    
    /**
     * Group avatar if this is a group conversation
     */
    private String groupAvatar;
}
