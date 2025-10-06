package com.chitchat.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for sending typing indicator via WebSocket
 */
@Data
@NoArgsConstructor
public class SendTypingIndicatorEvent extends WebSocketEvent {
    private Long receiverId;
    private Long senderId;
    private String senderName;
    private boolean isTyping;
    
    public SendTypingIndicatorEvent(Long receiverId, Long senderId, String senderName, boolean isTyping) {
        super(null, "SEND_TYPING_INDICATOR");
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.isTyping = isTyping;
    }
}
