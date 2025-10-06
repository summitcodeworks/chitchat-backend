package com.chitchat.messaging.websocket;

import com.chitchat.messaging.dto.ConversationResponse;
import com.chitchat.messaging.dto.MessageResponse;
import com.chitchat.messaging.dto.SendMessageRequest;
import com.chitchat.messaging.document.Message;
import com.chitchat.messaging.service.MessagingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

/**
 * WebSocket handler for real-time message broadcasting
 * 
 * Handles WebSocket connections for ChitChat messaging service.
 * Manages user sessions and broadcasts messages in real-time.
 * 
 * Features:
 * - User session management
 * - Message broadcasting to specific users
 * - Connection status tracking
 * - Error handling and logging
 * 
 * Message Types:
 * - NEW_MESSAGE: New message received
 * - MESSAGE_STATUS: Message delivery/read status update
 * - TYPING: Typing indicator
 * - USER_STATUS: User online/offline status
 * - CONVERSATION_LIST: Full conversation list data
 * - CONVERSATION_UPDATE: Conversation list update notification
 * - GET_CONVERSATIONS: Request for conversation list
 * - UNREAD_COUNT: Total unread message count
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler implements WebSocketHandler {
    
    private final ObjectMapper objectMapper;
    
    // Store active WebSocket sessions by user ID
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "websocket-cleanup");
        t.setDaemon(true);
        return t;
    });
    
    // Initialize cleanup scheduler
    {
        // Clean up stale sessions every 30 seconds
        cleanupExecutor.scheduleWithFixedDelay(this::cleanupStaleSessions, 30, 30, TimeUnit.SECONDS);
    }
    
    private MessagingService messagingService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established for session: {} - URI: {}", session.getId(), session.getUri());
        
        // Validate session
        if (session == null || !session.isOpen()) {
            log.warn("Invalid session received in afterConnectionEstablished");
            return;
        }
        
        // Set connection properties for stability
        try {
            session.setTextMessageSizeLimit(8192); // 8KB limit
            session.setBinaryMessageSizeLimit(8192); // 8KB limit
        } catch (Exception e) {
            log.warn("Failed to set message size limits: {}", e.getMessage());
        }
        
        Long userId = extractUserIdFromSession(session);
        if (userId != null) {
            // Check if user is already connected
            if (userSessions.containsKey(userId)) {
                log.info("User {} already connected, closing previous session", userId);
                WebSocketSession existingSession = userSessions.get(userId);
                try {
                    if (existingSession.isOpen()) {
                        existingSession.close(CloseStatus.NORMAL);
                    }
                } catch (Exception e) {
                    log.warn("Failed to close existing session for user {}: {}", userId, e.getMessage());
                }
            }
            
            userSessions.put(userId, session);
            log.info("WebSocket connection established and authenticated for user: {} - Active sessions: {}", userId, userSessions.size());
            
            try {
                // Send connection confirmation with stability info
                sendMessage(session, createConnectionMessage(userId));
                
                // Send ping to test connection stability
                sendPing(session);
                
                // Broadcast user online status to other users
                broadcastUserStatus(userId, "ONLINE");
            } catch (Exception e) {
                log.error("Error sending connection confirmation for user {}: {}", userId, e.getMessage());
            }
        } else {
            // Allow connection without userId initially - client can send userId later
            log.info("WebSocket connection established without user ID. Session: {} - URI: {}", session.getId(), session.getUri());
            
            try {
                // Send a message asking for userId or token
                Map<String, Object> authRequest = new HashMap<>();
                authRequest.put("type", "AUTH_REQUEST");
                authRequest.put("message", "Please provide userId or token for authentication");
                authRequest.put("connectionStable", true);
                sendMessage(session, objectMapper.writeValueAsString(authRequest));
            } catch (Exception e) {
                log.error("Error sending auth request: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            log.debug("Received WebSocket message: {}", payload);
            log.info("WebSocket session ID: {}, URI: {}", session.getId(), session.getUri());
            
            // Handle different message types (ping, typing indicators, etc.)
            handleIncomingMessage(session, payload);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session: {} - Error: {}", session.getId(), exception.getMessage());
        log.error("Full transport error details:", exception);
        
        // Try to send error message before closing
        try {
            if (session.isOpen()) {
                sendMessage(session, createErrorMessage("Transport error occurred: " + exception.getMessage()));
            }
        } catch (Exception e) {
            log.warn("Failed to send error message before closing session: {}", e.getMessage());
        }
        
        removeUserSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket connection closed for session: {} with status: {}", session.getId(), closeStatus);
        removeUserSession(session);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Send a message to a specific user (receiver)
     */
    public void sendMessageToUser(Long receiverId, MessageResponse message) {
        WebSocketSession session = userSessions.get(receiverId);
        if (session != null && session.isOpen()) {
            try {
                String payload = createMessagePayload("NEW_MESSAGE", message);
                sendMessage(session, payload);
                log.debug("Message sent to receiver {} via WebSocket", receiverId);
            } catch (Exception e) {
                log.error("Failed to send message to receiver {} via WebSocket", receiverId, e);
                removeUserSession(session);
            }
        } else {
            log.debug("Receiver {} not connected via WebSocket", receiverId);
        }
    }
    
    /**
     * Send message status update to the sender (when receiver reads/delivers message)
     */
    public void sendStatusUpdateToUser(Long senderId, String messageId, String status) {
        WebSocketSession session = userSessions.get(senderId);
        if (session != null && session.isOpen()) {
            try {
                String payload = createStatusPayload(messageId, status);
                sendMessage(session, payload);
                log.debug("Status update sent to sender {} via WebSocket", senderId);
            } catch (Exception e) {
                log.error("Failed to send status update to sender {} via WebSocket", senderId, e);
                removeUserSession(session);
            }
        } else {
            log.debug("Sender {} not connected via WebSocket", senderId);
        }
    }
    
    /**
     * Send typing indicator to a specific user (receiver)
     */
    public void sendTypingIndicator(Long receiverId, Long senderId, String senderName, boolean isTyping) {
        log.info("Attempting to send typing indicator from user {} to user {}: {}", senderId, receiverId, isTyping);
        log.info("Current active sessions: {}", userSessions.keySet());
        
        WebSocketSession session = userSessions.get(receiverId);
        if (session != null && session.isOpen()) {
            try {
                String payload = createTypingPayload(senderId, senderName, isTyping);
                log.info("Sending typing payload to user {}: {}", receiverId, payload);
                sendMessage(session, payload);
                log.info("Typing indicator sent to receiver {} from sender {} via WebSocket", receiverId, senderId);
            } catch (Exception e) {
                log.error("Failed to send typing indicator to receiver {} via WebSocket", receiverId, e);
                removeUserSession(session);
            }
        } else {
            log.warn("Receiver {} not connected via WebSocket. Session: {}, isOpen: {}", 
                receiverId, session, session != null ? session.isOpen() : "N/A");
        }
    }
    
    /**
     * Get number of active WebSocket connections
     */
    public int getActiveConnectionsCount() {
        return userSessions.size();
    }
    
    /**
     * Broadcast conversation list update to a user
     * Called when new messages are received to update conversation list
     */
    public void sendConversationUpdate(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                // Get updated conversation list
                List<ConversationResponse> conversations = messagingService.getConversationList(userId);
                String payload = createConversationListMessage(conversations);
                sendMessage(session, payload);
                log.debug("Conversation list update sent to user {} via WebSocket", userId);
            } catch (Exception e) {
                log.error("Failed to send conversation update to user {} via WebSocket", userId, e);
                removeUserSession(session);
            }
        } else {
            log.debug("User {} not connected via WebSocket for conversation update", userId);
        }
    }
    
    /**
     * Send total unread count to a user
     * Called when unread count changes (new messages received)
     */
    public void sendUnreadCountUpdate(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                // Get total unread count
                long totalUnreadCount = messagingService.getTotalUnreadCount(userId);
                String payload = createUnreadCountMessage(totalUnreadCount);
                sendMessage(session, payload);
                log.debug("Unread count update sent to user {} via WebSocket: {}", userId, totalUnreadCount);
            } catch (Exception e) {
                log.error("Failed to send unread count update to user {} via WebSocket", userId, e);
                removeUserSession(session);
            }
        } else {
            log.debug("User {} not connected via WebSocket for unread count update", userId);
        }
    }
    
    /**
     * Check if a user is connected via WebSocket
     */
    public boolean isUserConnected(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * Set MessagingService to break circular dependency
     * Called by WebSocketServiceImpl after both beans are created
     */
    public void setMessagingService(@Lazy MessagingService messagingService) {
        this.messagingService = messagingService;
    }
    
    private Long extractUserIdFromSession(WebSocketSession session) {
        return getUserIdFromSession(session);
    }
    
    private Long getUserIdFromSession(WebSocketSession session) {
        // Try to get user ID from query parameters
        String query = session.getUri().getQuery();
        if (query != null) {
            // Parse query parameters
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("userId=")) {
                    try {
                        return Long.parseLong(param.substring(7));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid user ID in WebSocket query parameter: {}", param);
                    }
                }
                
                // Extract user ID from JWT token
                if (param.startsWith("token=")) {
                    String token = param.substring(6);
                    try {
                        Long userId = extractUserIdFromToken(token);
                        if (userId != null) {
                            log.info("Extracted user ID {} from JWT token", userId);
                            return userId;
                        }
                    } catch (Exception e) {
                        log.warn("Error extracting user ID from token: {}", e.getMessage());
                    }
                }
            }
        }
        
        // Try to get user ID from headers
        Map<String, Object> attributes = session.getAttributes();
        Object userIdObj = attributes.get("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        
        // Try to find user ID from userSessions map
        for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    private Long extractUserIdFromToken(String token) {
        try {
            // Decode JWT token without verification (for WebSocket connection)
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                log.warn("Invalid JWT token format");
                return null;
            }
            
            // Decode the payload (second part)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
            
            // Parse JSON payload
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            
            // Extract userId
            Object userIdObj = payloadMap.get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            }
            
            log.warn("No userId found in JWT token payload");
            return null;
            
        } catch (Exception e) {
            log.error("Error decoding JWT token: {}", e.getMessage());
            return null;
        }
    }
    
    private void handleIncomingMessage(WebSocketSession session, String payload) {
        try {
            // Parse incoming message and handle different types
            log.debug("Handling incoming WebSocket message: {}", payload);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String messageType = (String) messageData.get("type");
            
            switch (messageType) {
                case "AUTH":
                    handleAuthentication(session, messageData);
                    break;
                case "PING":
                case "ping":
                    handlePing(session);
                    break;
                case "SEND_MESSAGE":
                    handleSendMessage(session, messageData);
                    break;
                case "TYPING":
                    handleTypingIndicator(session, messageData);
                    break;
                case "USER_STATUS":
                    handleUserStatus(session, messageData);
                    break;
                case "GET_CONVERSATIONS":
                    handleGetConversations(session, messageData);
                    break;
                default:
                    log.debug("Unknown message type: {}", messageType);
            }
            
        } catch (Exception e) {
            log.error("Error handling incoming WebSocket message", e);
        }
    }
    
    private void handleAuthentication(WebSocketSession session, Map<String, Object> messageData) {
        try {
            Object userIdObj = messageData.get("userId");
            if (userIdObj instanceof Number) {
                Long userId = ((Number) userIdObj).longValue();
                
                // Check if user is already connected
                if (userSessions.containsKey(userId)) {
                    // Close existing connection
                    WebSocketSession existingSession = userSessions.get(userId);
                    try {
                        existingSession.close(CloseStatus.NORMAL);
                    } catch (Exception e) {
                        log.warn("Error closing existing session for user: {}", userId, e);
                    }
                }
                
                // Add new session
                userSessions.put(userId, session);
                log.info("User {} authenticated via WebSocket", userId);
                
                // Send authentication success message
                sendMessage(session, createAuthSuccessMessage(userId));
                
                // Broadcast user online status to other users
                broadcastUserStatus(userId, "ONLINE");
                
            } else {
                log.warn("Invalid userId in authentication message");
                sendMessage(session, createErrorMessage("Invalid userId format"));
            }
        } catch (Exception e) {
            log.error("Error handling authentication", e);
            try {
                sendMessage(session, createErrorMessage("Authentication failed"));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }
    
    private void handlePing(WebSocketSession session) {
        try {
            Map<String, Object> pongResponse = new HashMap<>();
            pongResponse.put("type", "PONG");
            pongResponse.put("timestamp", System.currentTimeMillis());
            pongResponse.put("connectionStable", true);
            sendMessage(session, objectMapper.writeValueAsString(pongResponse));
        } catch (Exception e) {
            log.error("Error sending pong response", e);
        }
    }
    
    /**
     * Send ping to test connection stability
     */
    private void sendPing(WebSocketSession session) {
        try {
            if (session != null && session.isOpen()) {
                Map<String, Object> pingMessage = new HashMap<>();
                pingMessage.put("type", "PING");
                pingMessage.put("timestamp", System.currentTimeMillis());
                pingMessage.put("connectionStable", true);
                sendMessage(session, objectMapper.writeValueAsString(pingMessage));
                log.debug("Sent ping to session: {}", session.getId());
            }
        } catch (Exception e) {
            log.warn("Error sending ping to session {}: {}", session.getId(), e.getMessage());
        }
    }
    
    private void handleSendMessage(WebSocketSession session, Map<String, Object> messageData) {
        try {
            // Extract sender ID from session
            Long senderId = getUserIdFromSession(session);
            if (senderId == null) {
                log.warn("Cannot send message: user not authenticated");
                sendMessage(session, createErrorMessage("User not authenticated"));
                return;
            }
            
            // Extract message data
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data == null) {
                log.warn("No data in SEND_MESSAGE");
                sendMessage(session, createErrorMessage("No message data provided"));
                return;
            }
            
            Object recipientIdObj = data.get("recipientId");
            String content = (String) data.get("content");
            String messageType = (String) data.get("type");
            String groupId = (String) data.get("groupId");
            
            if (recipientIdObj == null || content == null) {
                log.warn("Missing recipientId or content in SEND_MESSAGE");
                sendMessage(session, createErrorMessage("Missing recipientId or content"));
                return;
            }
            
            Long recipientId = ((Number) recipientIdObj).longValue();
            
            // Create and save message to database using the messaging service
            try {
                SendMessageRequest request = SendMessageRequest.builder()
                        .recipientId(recipientId)
                        .groupId(groupId)
                        .content(content)
                        .type(messageType != null ? Message.MessageType.valueOf(messageType) : Message.MessageType.TEXT)
                        .build();
                
                MessageResponse messageResponse = messagingService.sendMessage(senderId, request);
                String messageId = messageResponse.getId();
                
                log.info("Message saved to database with ID: {}", messageId);
                
                // Forward message to recipient via WebSocket with database ID
                WebSocketSession recipientSession = userSessions.get(recipientId);
                if (recipientSession != null && recipientSession.isOpen()) {
                    try {
                        String messagePayload = createWebSocketMessage(senderId, recipientId, content, messageType, messageId);
                        sendMessage(recipientSession, messagePayload);
                        log.debug("Message forwarded to user {} via WebSocket with database ID: {}", recipientId, messageId);
                    } catch (Exception e) {
                        log.error("Failed to forward message to user {} via WebSocket", recipientId, e);
                        removeUserSession(recipientSession);
                    }
                } else {
                    log.debug("Recipient {} not connected via WebSocket", recipientId);
                }
                
                // Send success response to sender with database ID
                try {
                    sendMessage(session, createSendMessageResponse(messageId, recipientId, "Message sent successfully"));
                } catch (Exception e) {
                    log.error("Failed to send success response to sender", e);
                }
                
                log.info("Message sent from user {} to user {} via WebSocket and saved to database with ID: {}", senderId, recipientId, messageId);
                
            } catch (Exception e) {
                log.error("Failed to save message to database", e);
                sendMessage(session, createErrorMessage("Failed to save message: " + e.getMessage()));
                return;
            }
            
        } catch (Exception e) {
            log.error("Error handling SEND_MESSAGE", e);
            try {
                sendMessage(session, createErrorMessage("Failed to send message: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }
    
    private String createWebSocketMessage(Long senderId, Long recipientId, String content, String messageType, String messageId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NEW_MESSAGE");
            
            Map<String, Object> data = new HashMap<>();
            data.put("messageId", messageId);
            data.put("senderId", senderId);
            data.put("receiverId", recipientId);
            data.put("content", content);
            data.put("type", messageType != null ? messageType : "TEXT");
            data.put("timestamp", System.currentTimeMillis());
            data.put("status", "SENT");
            
            message.put("data", data);
            
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error creating WebSocket message", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create message\"}";
        }
    }
    
    private String createSendMessageResponse(String messageId, Long recipientId, String status) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "SEND_MESSAGE_RESPONSE");
            response.put("messageId", messageId);
            response.put("recipientId", recipientId);
            response.put("status", status);
            response.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating send message response", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create response\"}";
        }
    }
    
    private void handleTypingIndicator(WebSocketSession session, Map<String, Object> messageData) {
        try {
            Long senderId = getUserIdFromSession(session);
            if (senderId == null) {
                log.warn("Cannot send typing indicator: user not authenticated");
                sendMessage(session, createErrorMessage("User not authenticated"));
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data == null) {
                log.warn("No data in TYPING message");
                sendMessage(session, createErrorMessage("No typing data provided"));
                return;
            }
            
            Object recipientIdObj = data.get("recipientId");
            Object isTypingObj = data.get("isTyping");
            String senderName = (String) data.get("senderName");
            
            if (recipientIdObj == null || isTypingObj == null) {
                log.warn("Missing recipientId or isTyping in TYPING message");
                sendMessage(session, createErrorMessage("Missing recipientId or isTyping"));
                return;
            }
            
            Long recipientId = ((Number) recipientIdObj).longValue();
            boolean isTyping = (Boolean) isTypingObj;
            
            // Forward typing indicator to recipient
            sendTypingIndicator(recipientId, senderId, senderName != null ? senderName : "User " + senderId, isTyping);
            
            // Send confirmation to sender
            try {
                sendMessage(session, createTypingResponse(recipientId, isTyping, "Typing indicator sent"));
            } catch (Exception e) {
                log.error("Failed to send typing confirmation to sender", e);
            }
            
            log.info("Typing indicator forwarded from user {} to user {}: {}", senderId, recipientId, isTyping);
            
        } catch (Exception e) {
            log.error("Error handling typing indicator", e);
            try {
                sendMessage(session, createErrorMessage("Failed to send typing indicator: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }
    
    private void handleUserStatus(WebSocketSession session, Map<String, Object> messageData) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                log.warn("Cannot update user status: user not authenticated");
                sendMessage(session, createErrorMessage("User not authenticated"));
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data == null) {
                log.warn("No data in USER_STATUS message");
                sendMessage(session, createErrorMessage("No status data provided"));
                return;
            }
            
            String status = (String) data.get("status");
            if (status == null) {
                log.warn("Missing status in USER_STATUS message");
                sendMessage(session, createErrorMessage("Missing status"));
                return;
            }
            
            // Broadcast user status to all connected users
            broadcastUserStatus(userId, status);
            
            // Send confirmation to sender
            try {
                sendMessage(session, createUserStatusResponse(userId, status, "Status updated"));
            } catch (Exception e) {
                log.error("Failed to send status confirmation to sender", e);
            }
            
            log.info("User {} status updated to: {}", userId, status);
            
        } catch (Exception e) {
            log.error("Error handling user status", e);
            try {
                sendMessage(session, createErrorMessage("Failed to update status: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }
    
    private void broadcastUserStatus(Long userId, String status) {
        try {
            String statusMessage = createUserStatusBroadcast(userId, status);
            
            // Send to all connected users
            for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
                Long recipientId = entry.getKey();
                WebSocketSession session = entry.getValue();
                
                // Don't send status to the user themselves
                if (!recipientId.equals(userId)) {
                    try {
                        sendMessage(session, statusMessage);
                        log.debug("User status broadcast sent to user: {}", recipientId);
                    } catch (Exception e) {
                        log.error("Failed to send status broadcast to user: {}", recipientId, e);
                        removeUserSession(session);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting user status", e);
        }
    }
    
    private void handleGetConversations(WebSocketSession session, Map<String, Object> messageData) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                log.warn("Cannot get conversations: user not authenticated");
                sendMessage(session, createErrorMessage("User not authenticated"));
                return;
            }
            
            // Get conversation list from messaging service
            List<ConversationResponse> conversations = messagingService.getConversationList(userId);
            
            // Send conversation list via WebSocket
            String conversationListMessage = createConversationListMessage(conversations);
            sendMessage(session, conversationListMessage);
            
            log.info("Conversation list sent to user {} via WebSocket", userId);
            
        } catch (Exception e) {
            log.error("Error handling get conversations", e);
            try {
                sendMessage(session, createErrorMessage("Failed to get conversations: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }
    
    private String createTypingResponse(Long recipientId, boolean isTyping, String message) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "TYPING_RESPONSE");
            response.put("recipientId", recipientId);
            response.put("isTyping", isTyping);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating typing response", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create typing response\"}";
        }
    }
    
    private String createUserStatusResponse(Long userId, String status, String message) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "USER_STATUS_RESPONSE");
            response.put("userId", userId);
            response.put("status", status);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating user status response", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create status response\"}";
        }
    }
    
    private String createUserStatusBroadcast(Long userId, String status) {
        try {
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("type", "USER_STATUS_BROADCAST");
            broadcast.put("userId", userId);
            broadcast.put("status", status);
            broadcast.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(broadcast);
        } catch (Exception e) {
            log.error("Error creating user status broadcast", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create status broadcast\"}";
        }
    }
    
    private String createConversationUpdateMessage() {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "CONVERSATION_UPDATE");
            update.put("message", "Conversation list has been updated");
            update.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(update);
        } catch (Exception e) {
            log.error("Error creating conversation update message", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create conversation update\"}";
        }
    }
    
    private String createConversationListMessage(List<ConversationResponse> conversations) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CONVERSATION_LIST");
            response.put("conversations", conversations);
            response.put("timestamp", System.currentTimeMillis());
            response.put("count", conversations.size());
            
            // Calculate total unread count from conversations
            long totalUnreadCount = conversations.stream()
                    .mapToLong(conv -> conv.getUnreadCount())
                    .sum();
            response.put("totalUnreadCount", totalUnreadCount);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating conversation list message", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create conversation list\"}";
        }
    }
    
    private String createUnreadCountMessage(long totalUnreadCount) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "UNREAD_COUNT");
            response.put("totalUnreadCount", totalUnreadCount);
            response.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating unread count message", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create unread count\"}";
        }
    }
    
    private String createErrorMessage(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "ERROR");
        errorResponse.put("message", message);
        try {
            return objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            return "{\"type\":\"ERROR\",\"message\":\"Internal error\"}";
        }
    }
    
    private void removeUserSession(WebSocketSession session) {
        if (session == null) {
            log.warn("Attempted to remove null session");
            return;
        }
        
        log.info("Removing WebSocket session: {} - URI: {}", session.getId(), session.getUri());
        
        // Find the user ID for this session before removing
        Long userId = null;
        WebSocketSession sessionToRemove = null;
        
        // Use iterator to safely remove while iterating
        var iterator = userSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().equals(session)) {
                userId = entry.getKey();
                sessionToRemove = entry.getValue();
                iterator.remove(); // Safe removal
                break;
            }
        }
        
        log.info("Active sessions after removal: {}", userSessions.size());
        
        // Close session if still open
        if (sessionToRemove != null && sessionToRemove.isOpen()) {
            try {
                sessionToRemove.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.warn("Error closing session: {}", e.getMessage());
            }
        }
        
        // Broadcast offline status if user was connected
        if (userId != null) {
            try {
                broadcastUserStatus(userId, "OFFLINE");
                log.info("User {} went offline", userId);
            } catch (Exception e) {
                log.warn("Error broadcasting offline status for user {}: {}", userId, e.getMessage());
            }
        }
        
        // Force garbage collection hint for large objects
        if (userSessions.size() == 0) {
            System.gc(); // Only call when no sessions remain
        }
    }
    
    private void sendMessage(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }
    
    private String createConnectionMessage(Long userId) {
        try {
            Map<String, Object> connectionMessage = new HashMap<>();
            connectionMessage.put("type", "CONNECTION");
            connectionMessage.put("userId", userId);
            connectionMessage.put("status", "connected");
            connectionMessage.put("timestamp", System.currentTimeMillis());
            connectionMessage.put("message", "WebSocket connection established");
            connectionMessage.put("activeConnections", userSessions.size());
            connectionMessage.put("connectionStable", true);
            connectionMessage.put("serverTime", System.currentTimeMillis());
            connectionMessage.put("heartbeatInterval", 30000); // 30 seconds
            
            return objectMapper.writeValueAsString(connectionMessage);
        } catch (Exception e) {
            log.error("Error creating connection message", e);
            return String.format("{\"type\":\"CONNECTION\",\"status\":\"connected\",\"userId\":%d,\"connectionStable\":true}", userId);
        }
    }
    
    private String createAuthSuccessMessage(Long userId) {
        try {
            Map<String, Object> authMessage = new HashMap<>();
            authMessage.put("type", "AUTH_SUCCESS");
            authMessage.put("userId", userId);
            authMessage.put("status", "authenticated");
            authMessage.put("timestamp", System.currentTimeMillis());
            authMessage.put("message", "Authentication successful");
            authMessage.put("activeConnections", userSessions.size());
            authMessage.put("connectionStable", true);
            authMessage.put("serverTime", System.currentTimeMillis());
            authMessage.put("heartbeatInterval", 30000); // 30 seconds
            
            return objectMapper.writeValueAsString(authMessage);
        } catch (Exception e) {
            log.error("Error creating auth success message", e);
            return String.format("{\"type\":\"AUTH_SUCCESS\",\"status\":\"authenticated\",\"userId\":%d,\"connectionStable\":true}", userId);
        }
    }
    
    private String createMessagePayload(String type, MessageResponse message) {
        try {
            return String.format("{\"type\":\"%s\",\"data\":%s}", type, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Error creating message payload", e);
            return "{\"type\":\"" + type + "\",\"error\":\"Failed to serialize message\"}";
        }
    }
    
    private String createStatusPayload(String messageId, String status) {
        return String.format("{\"type\":\"MESSAGE_STATUS\",\"messageId\":\"%s\",\"status\":\"%s\"}", messageId, status);
    }
    
    private String createTypingPayload(Long senderId, String senderName, boolean isTyping) {
        return String.format("{\"type\":\"TYPING\",\"senderId\":%d,\"senderName\":\"%s\",\"isTyping\":%b}", 
                senderId, senderName, isTyping);
    }
    
    /**
     * Clean up stale WebSocket sessions
     */
    private void cleanupStaleSessions() {
        try {
            Iterator<Map.Entry<Long, WebSocketSession>> iterator = userSessions.entrySet().iterator();
            int cleanedCount = 0;
            
            while (iterator.hasNext()) {
                Map.Entry<Long, WebSocketSession> entry = iterator.next();
                WebSocketSession session = entry.getValue();
                
                // Remove closed or invalid sessions
                if (session == null || !session.isOpen()) {
                    iterator.remove();
                    cleanedCount++;
                    
                    if (session != null) {
                        log.debug("Cleaned up stale session: {}", session.getId());
                    }
                }
            }
            
            if (cleanedCount > 0) {
                log.info("Cleaned up {} stale WebSocket sessions. Active sessions: {}", cleanedCount, userSessions.size());
            }
            
        } catch (Exception e) {
            log.error("Error during WebSocket session cleanup", e);
        }
    }
    
    /**
     * Shutdown cleanup executor
     */
    @PreDestroy
    public void shutdown() {
        try {
            log.info("Shutting down WebSocket handler cleanup executor");
            
            // Close all active sessions
            for (WebSocketSession session : userSessions.values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.SERVER_ERROR);
                    } catch (IOException e) {
                        log.warn("Error closing session during shutdown: {}", e.getMessage());
                    }
                }
            }
            
            userSessions.clear();
            
            // Shutdown executor
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
            
            log.info("WebSocket handler shutdown complete");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupExecutor.shutdownNow();
        } catch (Exception e) {
            log.error("Error during WebSocket handler shutdown", e);
        }
    }
}
