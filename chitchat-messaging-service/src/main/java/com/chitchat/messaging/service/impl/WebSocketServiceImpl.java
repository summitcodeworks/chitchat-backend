package com.chitchat.messaging.service.impl;

import com.chitchat.messaging.dto.MessageResponse;
import com.chitchat.messaging.service.MessagingService;
import com.chitchat.messaging.service.WebSocketService;
import com.chitchat.messaging.websocket.MessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Implementation of WebSocketService
 * 
 * Handles WebSocket operations for real-time message delivery.
 * Delegates to MessageWebSocketHandler for actual WebSocket operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final MessageWebSocketHandler messageWebSocketHandler;
    
    /**
     * Initialize the circular dependency after both beans are created
     */
    public void initializeMessagingService(@Lazy MessagingService messagingService) {
        messageWebSocketHandler.setMessagingService(messagingService);
    }
    
    @Override
    public void sendMessageToUser(Long receiverId, MessageResponse message) {
        log.debug("Sending message to receiver {} via WebSocket", receiverId);
        messageWebSocketHandler.sendMessageToUser(receiverId, message);
    }
    
    @Override
    public void sendStatusUpdateToUser(Long senderId, String messageId, String status) {
        log.debug("Sending status update to sender {} via WebSocket", senderId);
        messageWebSocketHandler.sendStatusUpdateToUser(senderId, messageId, status);
    }
    
    @Override
    public void sendTypingIndicator(Long receiverId, Long senderId, String senderName, boolean isTyping) {
        log.debug("Sending typing indicator to receiver {} from sender {} via WebSocket", receiverId, senderId);
        messageWebSocketHandler.sendTypingIndicator(receiverId, senderId, senderName, isTyping);
    }

    @Override
    public void sendConversationUpdate(Long userId) {
        log.debug("Sending conversation update to user {} via WebSocket", userId);
        messageWebSocketHandler.sendConversationUpdate(userId);
    }

    @Override
    public void sendUnreadCountUpdate(Long userId) {
        log.debug("Sending unread count update to user {} via WebSocket", userId);
        messageWebSocketHandler.sendUnreadCountUpdate(userId);
    }

    @Override
    public boolean isUserConnected(Long userId) {
        return messageWebSocketHandler.isUserConnected(userId);
    }

    @Override
    public int getActiveConnectionsCount() {
        return messageWebSocketHandler.getActiveConnectionsCount();
    }
}
