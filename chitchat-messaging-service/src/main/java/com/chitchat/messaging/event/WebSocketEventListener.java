package com.chitchat.messaging.event;

import com.chitchat.messaging.websocket.MessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for WebSocket events
 * Handles WebSocket operations triggered by messaging service events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    
    private final MessageWebSocketHandler messageWebSocketHandler;
    
    @EventListener
    public void handleSendMessageEvent(SendMessageEvent event) {
        log.debug("Handling SendMessageEvent for user: {}", event.getReceiverId());
        messageWebSocketHandler.sendMessageToUser(event.getReceiverId(), event.getMessage());
    }
    
    @EventListener
    public void handleSendStatusUpdateEvent(SendStatusUpdateEvent event) {
        log.debug("Handling SendStatusUpdateEvent for user: {}", event.getSenderId());
        messageWebSocketHandler.sendStatusUpdateToUser(event.getSenderId(), event.getMessageId(), event.getStatus());
    }
    
    @EventListener
    public void handleSendTypingIndicatorEvent(SendTypingIndicatorEvent event) {
        log.debug("Handling SendTypingIndicatorEvent for user: {}", event.getReceiverId());
        messageWebSocketHandler.sendTypingIndicator(event.getReceiverId(), event.getSenderId(), 
                event.getSenderName(), event.isTyping());
    }
    
    @EventListener
    public void handleSendConversationUpdateEvent(SendConversationUpdateEvent event) {
        log.debug("Handling SendConversationUpdateEvent for user: {}", event.getUserId());
        messageWebSocketHandler.sendConversationUpdate(event.getUserId());
    }
    
    @EventListener
    public void handleSendUnreadCountUpdateEvent(SendUnreadCountUpdateEvent event) {
        log.debug("Handling SendUnreadCountUpdateEvent for user: {}", event.getUserId());
        messageWebSocketHandler.sendUnreadCountUpdate(event.getUserId());
    }
}
