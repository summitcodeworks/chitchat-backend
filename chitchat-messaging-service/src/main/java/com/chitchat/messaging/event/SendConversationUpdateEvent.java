package com.chitchat.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event for sending conversation update via WebSocket
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class SendConversationUpdateEvent extends WebSocketEvent {
    private Long userId;
    
    public SendConversationUpdateEvent(Long userId) {
        super(userId, "SEND_CONVERSATION_UPDATE");
        this.userId = userId;
    }
}
