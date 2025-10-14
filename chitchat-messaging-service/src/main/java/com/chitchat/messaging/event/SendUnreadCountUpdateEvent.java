package com.chitchat.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event for sending unread count update via WebSocket
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class SendUnreadCountUpdateEvent extends WebSocketEvent {
    private Long userId;
    
    public SendUnreadCountUpdateEvent(Long userId) {
        super(userId, "SEND_UNREAD_COUNT_UPDATE");
        this.userId = userId;
    }
}
