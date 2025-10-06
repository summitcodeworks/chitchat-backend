package com.chitchat.messaging.event;

import com.chitchat.messaging.dto.MessageResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for sending message to user via WebSocket
 */
@Data
@NoArgsConstructor
public class SendMessageEvent extends WebSocketEvent {
    private Long receiverId;
    private MessageResponse message;
    
    public SendMessageEvent(Long receiverId, MessageResponse message) {
        super(null, "SEND_MESSAGE");
        this.receiverId = receiverId;
        this.message = message;
    }
}
