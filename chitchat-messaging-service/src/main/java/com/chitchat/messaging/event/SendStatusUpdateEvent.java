package com.chitchat.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for sending status update to user via WebSocket
 */
@Data
@NoArgsConstructor
public class SendStatusUpdateEvent extends WebSocketEvent {
    private Long senderId;
    private String messageId;
    private String status;
    
    public SendStatusUpdateEvent(Long senderId, String messageId, String status) {
        super(null, "SEND_STATUS_UPDATE");
        this.senderId = senderId;
        this.messageId = messageId;
        this.status = status;
    }
}
