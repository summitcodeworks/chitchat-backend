package com.chitchat.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for sending conversation update via WebSocket
 */
@Data
@NoArgsConstructor
public class SendConversationUpdateEvent extends WebSocketEvent {
    private Long userId;
    
    public SendConversationUpdateEvent(Long userId) {
        super(userId, "SEND_CONVERSATION_UPDATE");
        this.userId = userId;
    }
}
