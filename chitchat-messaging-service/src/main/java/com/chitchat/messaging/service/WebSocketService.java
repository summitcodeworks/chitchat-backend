package com.chitchat.messaging.service;

import com.chitchat.messaging.dto.MessageResponse;

/**
 * Service interface for WebSocket operations
 * 
 * Provides methods for real-time message broadcasting via WebSocket.
 * Manages user connections and message delivery.
 */
public interface WebSocketService {
    
    /**
     * Send a message to a specific user (receiver) via WebSocket
     * 
     * @param receiverId ID of the user to receive the message
     * @param message Message to send
     */
    void sendMessageToUser(Long receiverId, MessageResponse message);
    
    /**
     * Send message status update to the sender
     * 
     * @param senderId ID of the sender to notify about status change
     * @param messageId ID of the message
     * @param status New status (DELIVERED, READ, etc.)
     */
    void sendStatusUpdateToUser(Long senderId, String messageId, String status);
    
    /**
     * Send typing indicator to a specific user (receiver)
     * 
     * @param receiverId ID of the user to receive the typing indicator
     * @param senderId ID of the user who is typing
     * @param senderName Name of the user who is typing
     * @param isTyping Whether the user is typing or stopped typing
     */
    void sendTypingIndicator(Long receiverId, Long senderId, String senderName, boolean isTyping);
    
    /**
     * Send conversation list update notification to a user
     * 
     * @param userId ID of the user to notify about conversation list update
     */
    void sendConversationUpdate(Long userId);
    
    /**
     * Send total unread count update to a user
     * 
     * @param userId ID of the user to notify about unread count change
     */
    void sendUnreadCountUpdate(Long userId);
    
    /**
     * Check if a user is connected via WebSocket
     * 
     * @param userId ID of the user to check
     * @return true if user is connected, false otherwise
     */
    boolean isUserConnected(Long userId);
    
    /**
     * Get the number of active WebSocket connections
     * 
     * @return Number of active connections
     */
    int getActiveConnectionsCount();
}
