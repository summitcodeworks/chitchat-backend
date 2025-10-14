package com.chitchat.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event for sending status update to user via WebSocket
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
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
