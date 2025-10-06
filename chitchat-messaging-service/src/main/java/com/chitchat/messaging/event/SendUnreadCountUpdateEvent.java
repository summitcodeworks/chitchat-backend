package com.chitchat.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for sending unread count update via WebSocket
 */
@Data
@NoArgsConstructor
public class SendUnreadCountUpdateEvent extends WebSocketEvent {
    private Long userId;
    
    public SendUnreadCountUpdateEvent(Long userId) {
        super(userId, "SEND_UNREAD_COUNT_UPDATE");
        this.userId = userId;
    }
}
