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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import javax.annotation.PreDestroy;

/**
 * WebSocket handler for REAL-TIME message broadcasting ONLY
 * 
 * ==========================================================
 * IMPORTANT: WebSocket handles ONLY REAL-TIME operations
 * ==========================================================
 * 
 * ARCHITECTURE OVERVIEW:
 * =====================
 * 
 * WebSocket (THIS CLASS):
 * - Real-time message delivery to CURRENT active sessions
 * - Typing indicators (who is typing now)
 * - Online/offline status updates
 * - Message read receipts (instant feedback)
 * - Multi-device support (phone + web simultaneously)
 * 
 * REST API (Controller):
 * - Message HISTORY pagination: GET /api/messages/conversation/{userId}?page=0&size=50
 * - Conversation list: GET /api/messages/conversations
 * - Search messages: GET /api/messages/search?query=text
 * - Bulk operations: PUT /api/messages/conversation/{senderId}/mark-all-read
 * 
 * WHY THIS SEPARATION?
 * ===================
 * 1. WebSocket = Real-time push (server → client when event happens)
 * 2. REST API = Request-response (client asks → server responds)
 * 3. Pagination needs HTTP caching, ETag, conditional requests
 * 4. WebSocket should be lightweight and fast
 * 5. Standard practice: Real-time vs Historical data
 * 
 * MESSAGE FLOW EXAMPLE:
 * ====================
 * 
 * User A sends message to User B:
 * 
 * 1. User A (WebSocket) → Server: SEND_MESSAGE
 * 2. Server saves to MongoDB
 * 3. Server → User B (WebSocket): NEW_MESSAGE (if B is online)
 * 4. Server → Push Notification (if B is offline)
 * 
 * User B opens chat:
 * 
 * 1. User B (REST API) → Server: GET /api/messages/conversation/A?page=0&size=50
 * 2. Server returns paginated history
 * 3. User B connects WebSocket for new messages
 * 4. New messages arrive via WebSocket in real-time
 * 
 * MULTI-SESSION SUPPORT:
 * =====================
 * - User can have multiple devices connected (phone, web, tablet)
 * - Each device is a separate WebSocket session
 * - When message arrives, sent to ALL active sessions
 * - Example: Message appears on both phone and laptop simultaneously
 * 
 * Features:
 * - User session management (supports multiple sessions per user)
 * - Real-time message broadcasting to current active sessions
 * - Connection status tracking
 * - Performance metrics (connections, messages sent/failed)
 * 
 * Message Types Handled:
 * - NEW_MESSAGE: Real-time message delivery
 * - MESSAGE_STATUS: Read/delivered status updates
 * - TYPING: Typing indicator
 * - USER_STATUS: Online/offline status
 * - CONVERSATION_UPDATE: Conversation list changed notification
 * - UNREAD_COUNT: Total unread count update
 * 
 * Message Types NOT Handled (use REST API):
 * - GET_CONVERSATION_MESSAGES: Use REST API for pagination
 * - SEARCH_MESSAGES: Use REST API for search
 * - BULK_OPERATIONS: Use REST API for bulk updates
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler implements WebSocketHandler {
    
    private final ObjectMapper objectMapper;
    
    // Store active WebSocket sessions by user ID - supports multiple sessions per user
    private final Map<Long, ConcurrentHashMap<String, WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks - increased threads for better performance
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "ws-cleanup");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);  // Low priority for cleanup tasks
        return t;
    });
    
    // Executor for async WebSocket operations - optimized for high concurrency
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(50, r -> {
        Thread t = new Thread(r, "ws-send");
        t.setDaemon(true);
        return t;
    });
    
    // Executor for broadcast operations to multiple users
    private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(30, r -> {
        Thread t = new Thread(r, "ws-broadcast");
        t.setDaemon(true);
        return t;
    });
    
    // Session metrics
    private final java.util.concurrent.atomic.AtomicLong totalConnections = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong activeConnections = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong messagesSent = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong messagesFailed = new java.util.concurrent.atomic.AtomicLong(0);
    
    // Initialize cleanup scheduler
    {
        // Clean up stale sessions every 30 seconds
        cleanupExecutor.scheduleWithFixedDelay(this::cleanupStaleSessions, 30, 30, TimeUnit.SECONDS);
        
        // Log metrics every 60 seconds
        cleanupExecutor.scheduleWithFixedDelay(this::logMetrics, 60, 60, TimeUnit.SECONDS);
    }
    
    private MessagingService messagingService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        totalConnections.incrementAndGet();
        activeConnections.incrementAndGet();
        
        log.info("WebSocket connection established for session: {} - URI: {} - Total: {}, Active: {}", 
            session.getId(), session.getUri(), totalConnections.get(), activeConnections.get());
        
        // Validate session
        if (session == null || !session.isOpen()) {
            log.warn("Invalid session received in afterConnectionEstablished");
            activeConnections.decrementAndGet();
            return;
        }
        
        // Set connection properties for stability
        try {
            session.setTextMessageSizeLimit(16384); // 16KB limit (increased for better throughput)
            session.setBinaryMessageSizeLimit(16384);
        } catch (Exception e) {
            log.warn("Failed to set message size limits: {}", e.getMessage());
        }
        
        Long userId = extractUserIdFromSession(session);
        if (userId != null) {
            // Support multiple sessions per user (e.g., mobile + web)
            ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.computeIfAbsent(
                userId, k -> new ConcurrentHashMap<>()
            );
            
            // Add new session
            sessions.put(session.getId(), session);
            
            log.info("WebSocket connection established for user: {} - User sessions: {} - Total active users: {}", 
                userId, sessions.size(), userSessions.size());
            
            // Async connection confirmation to avoid blocking
            asyncExecutor.submit(() -> {
                try {
                    // Send connection confirmation
                    sendMessage(session, createConnectionMessage(userId));
                    
                    // Send ping to test connection
                    sendPing(session);
                    
                    // Broadcast user online status
                    broadcastUserStatus(userId, "ONLINE");
                    
                    // Send pending SENT messages to user
                    sendPendingMessagesToUser(userId);
                } catch (Exception e) {
                    log.error("Error sending connection confirmation for user {}: {}", userId, e.getMessage());
                }
            });
        } else {
            // Allow connection without userId initially
            log.info("WebSocket connection established without user ID. Session: {} - URI: {}", 
                session.getId(), session.getUri());
            
            asyncExecutor.submit(() -> {
                try {
                    Map<String, Object> authRequest = new HashMap<>();
                    authRequest.put("type", "AUTH_REQUEST");
                    authRequest.put("message", "Please provide userId or token for authentication");
                    authRequest.put("connectionStable", true);
                    sendMessage(session, objectMapper.writeValueAsString(authRequest));
                } catch (Exception e) {
                    log.error("Error sending auth request: {}", e.getMessage());
                }
            });
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
        // EOFException is a normal occurrence when clients disconnect abruptly
        // (browser closed, network lost, etc.) - handle it gracefully
        if (exception instanceof java.io.EOFException || 
            exception.getCause() instanceof java.io.EOFException ||
            exception.getMessage() != null && exception.getMessage().contains("EOF")) {
            
            log.info("WebSocket client disconnected abruptly (session: {}). This is expected behavior.", 
                    session.getId());
            log.debug("Disconnect reason: {}", exception.getMessage());
            
        } else if (exception instanceof java.io.IOException) {
            // Other IO exceptions are also often normal (network issues, timeouts)
            log.info("WebSocket connection closed due to IO error (session: {}): {}", 
                    session.getId(), exception.getMessage());
            
        } else {
            // Log actual errors that need attention
            log.error("WebSocket transport error for session: {} - Error: {}", 
                    session.getId(), exception.getMessage());
            log.debug("Full transport error details:", exception);
            
            // Try to send error message before closing
            try {
                if (session.isOpen()) {
                    sendMessage(session, createErrorMessage("Transport error occurred: " + exception.getMessage()));
                }
            } catch (Exception e) {
                log.debug("Failed to send error message before closing session: {}", e.getMessage());
            }
        }
        
        removeUserSession(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        activeConnections.decrementAndGet();
        log.info("WebSocket connection closed for session: {} with status: {} - Active: {}", 
            session.getId(), closeStatus, activeConnections.get());
        removeUserSession(session);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Send a message to a specific user (receiver) - optimized for multiple sessions
     */
    public void sendMessageToUser(Long receiverId, MessageResponse message) {
        ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(receiverId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("Receiver {} not connected via WebSocket", receiverId);
            return;
        }
        
        String payload = createMessagePayload("NEW_MESSAGE", message);
        
        // Send to all sessions of the user in parallel
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            futures.add(CompletableFuture.runAsync(() -> {
                if (session.isOpen()) {
                    try {
                        sendMessage(session, payload);
                        messagesSent.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Failed to send message to receiver {} session {}", receiverId, session.getId(), e);
                        messagesFailed.incrementAndGet();
                        removeUserSession(session);
                    }
                }
            }, asyncExecutor));
        }
        
        // Don't wait for completion - fire and forget for better performance
        log.debug("Message queued to {} sessions for receiver {}", futures.size(), receiverId);
    }
    
    /**
     * Send message status update to the sender - optimized for multiple sessions
     */
    public void sendStatusUpdateToUser(Long senderId, String messageId, String status) {
        ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(senderId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("Sender {} not connected via WebSocket", senderId);
            return;
        }
        
        String payload = createStatusPayload(messageId, status);
        
        // Send to all sessions in parallel
        for (WebSocketSession session : sessions.values()) {
            asyncExecutor.submit(() -> {
                if (session.isOpen()) {
                    try {
                        sendMessage(session, payload);
                        messagesSent.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Failed to send status update to sender {} session {}", senderId, session.getId(), e);
                        messagesFailed.incrementAndGet();
                        removeUserSession(session);
                    }
                }
            });
        }
        
        log.debug("Status update queued to {} sessions for sender {}", sessions.size(), senderId);
    }
    
    /**
     * Send typing indicator to a specific user - optimized for multiple sessions
     */
    public void sendTypingIndicator(Long receiverId, Long senderId, String senderName, boolean isTyping) {
        log.debug("Sending typing indicator from user {} to user {}: {}", senderId, receiverId, isTyping);
        
        ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(receiverId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("Receiver {} not connected via WebSocket", receiverId);
            return;
        }
        
        String payload = createTypingPayload(senderId, senderName, isTyping);
        
        // Send to all sessions in parallel
        for (WebSocketSession session : sessions.values()) {
            asyncExecutor.submit(() -> {
                if (session.isOpen()) {
                    try {
                        sendMessage(session, payload);
                        messagesSent.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Failed to send typing indicator to receiver {}", receiverId, e);
                        messagesFailed.incrementAndGet();
                        removeUserSession(session);
                    }
                }
            });
        }
        
        log.debug("Typing indicator queued to {} sessions for receiver {}", sessions.size(), receiverId);
    }
    
    /**
     * Get number of active WebSocket connections
     */
    public int getActiveConnectionsCount() {
        return (int) activeConnections.get();
    }
    
    /**
     * Get number of unique users connected
     */
    public int getActiveUsersCount() {
        return userSessions.size();
    }
    
    /**
     * Log WebSocket metrics
     */
    private void logMetrics() {
        long totalSessions = userSessions.values().stream()
            .mapToInt(ConcurrentHashMap::size)
            .sum();
        
        log.info("WebSocket Metrics - Users: {}, Sessions: {}, Total Connections: {}, Active: {}, Sent: {}, Failed: {}",
            userSessions.size(), totalSessions, totalConnections.get(), activeConnections.get(), 
            messagesSent.get(), messagesFailed.get());
    }
    
    /**
     * Broadcast conversation list update to a user - optimized for multiple sessions
     */
    public void sendConversationUpdate(Long userId) {
        ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("User {} not connected via WebSocket for conversation update", userId);
            return;
        }
        
        // Fetch data once and send to all sessions
        asyncExecutor.submit(() -> {
            try {
                List<ConversationResponse> conversations = messagingService.getConversationList(userId);
                String payload = createConversationListMessage(conversations);
                
                // Send to all sessions
                for (WebSocketSession session : sessions.values()) {
                    if (session.isOpen()) {
                        try {
                            sendMessage(session, payload);
                            messagesSent.incrementAndGet();
                        } catch (Exception e) {
                            log.error("Failed to send conversation update to user {} session {}", 
                                userId, session.getId(), e);
                            messagesFailed.incrementAndGet();
                            removeUserSession(session);
                        }
                    }
                }
                
                log.debug("Conversation list update sent to {} sessions for user {}", sessions.size(), userId);
            } catch (Exception e) {
                log.error("Failed to fetch conversation update for user {}", userId, e);
            }
        });
    }
    
    /**
     * Send total unread count to a user - optimized for multiple sessions
     */
    public void sendUnreadCountUpdate(Long userId) {
        ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("User {} not connected via WebSocket for unread count update", userId);
            return;
        }
        
        // Fetch data once and send to all sessions
        asyncExecutor.submit(() -> {
            try {
                long totalUnreadCount = messagingService.getTotalUnreadCount(userId);
                String payload = createUnreadCountMessage(totalUnreadCount);
                
                // Send to all sessions
                for (WebSocketSession session : sessions.values()) {
                    if (session.isOpen()) {
                        try {
                            sendMessage(session, payload);
                            messagesSent.incrementAndGet();
                        } catch (Exception e) {
                            log.error("Failed to send unread count to user {} session {}", 
                                userId, session.getId(), e);
                            messagesFailed.incrementAndGet();
                            removeUserSession(session);
                        }
                    }
                }
                
                log.debug("Unread count update sent to {} sessions for user {}: {}", 
                    sessions.size(), userId, totalUnreadCount);
            } catch (Exception e) {
                log.error("Failed to fetch unread count for user {}", userId, e);
            }
        });
    }
    
    /**
     * Send pending messages (SENT status) to user when they come online
     * 
     * When a user connects via WebSocket, this method retrieves all messages
     * that are in SENT status and sends them via WebSocket.
     * 
     * Messages in SENT status are those that:
     * - Were sent while user was offline
     * - Were not delivered yet
     * 
     * After sending, these messages are automatically marked as DELIVERED.
     * Messages that are already DELIVERED or READ are not sent again.
     */
    public void sendPendingMessagesToUser(Long userId) {
        asyncExecutor.submit(() -> {
            try {
                log.debug("Checking for pending messages for user: {}", userId);
                
                // Get all pending messages (SENT status only)
                List<MessageResponse> pendingMessages = messagingService.getPendingMessages(userId);
                
                if (pendingMessages.isEmpty()) {
                    log.debug("No pending messages for user: {}", userId);
                    return;
                }
                
                log.info("Sending {} pending messages to user: {}", pendingMessages.size(), userId);
                
                // Send each pending message via WebSocket
                ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(userId);
                if (sessions == null || sessions.isEmpty()) {
                    log.warn("User {} not connected anymore, cannot send pending messages", userId);
                    return;
                }
                
                for (MessageResponse message : pendingMessages) {
                    String payload = createMessagePayload("NEW_MESSAGE", message);
                    
                    // Send to all sessions of the user
                    for (WebSocketSession session : sessions.values()) {
                        if (session.isOpen()) {
                            try {
                                sendMessage(session, payload);
                                messagesSent.incrementAndGet();
                                log.debug("Sent pending message {} to user {} via WebSocket", 
                                    message.getId(), userId);
                            } catch (Exception e) {
                                log.error("Failed to send pending message {} to user {}", 
                                    message.getId(), userId, e);
                                messagesFailed.incrementAndGet();
                            }
                        }
                    }
                }
                
                log.info("Successfully sent {} pending messages to user: {}", pendingMessages.size(), userId);
                
            } catch (Exception e) {
                log.error("Error sending pending messages to user {}: {}", userId, e.getMessage(), e);
            }
        });
    }
    
    /**
     * Check if a user is connected via WebSocket (has at least one active session)
     */
    public boolean isUserConnected(Long userId) {
        ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        
        // Check if at least one session is open
        return sessions.values().stream().anyMatch(WebSocketSession::isOpen);
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
        for (Map.Entry<Long, ConcurrentHashMap<String, WebSocketSession>> entry : userSessions.entrySet()) {
            ConcurrentHashMap<String, WebSocketSession> sessions = entry.getValue();
            if (sessions.containsValue(session)) {
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
    
    /**
     * Handle incoming WebSocket messages
     * 
     * IMPORTANT: WebSocket is ONLY for REAL-TIME operations
     * ======================================================
     * 
     * WebSocket Handles (REAL-TIME):
     * - SEND_MESSAGE: Send new message in real-time
     * - TYPING: Show typing indicators
     * - USER_STATUS: Online/offline status
     * - PIN_MESSAGE: Pin/unpin messages
     * - AUTH: Authenticate WebSocket connection
     * - PING/PONG: Keep-alive
     * 
     * REST API Handles (PAGINATION & HISTORY):
     * - GET /api/messages/conversation/{userId}?page=0&size=50 - Load message history
     * - GET /api/messages/conversations - Get conversation list
     * - GET /api/messages/search?query=text - Search messages
     * 
     * WHY PAGINATION IS NOT IN WEBSOCKET:
     * ===================================
     * 1. WebSocket is for real-time push notifications only
     * 2. Pagination requires complex state management (page number, sorting, filtering)
     * 3. REST API is standard and optimized for pagination
     * 4. WebSocket should remain lightweight and fast
     * 5. Separation of concerns: Real-time (WebSocket) vs History (REST)
     * 
     * MESSAGE FLOW:
     * =============
     * 
     * When user opens chat:
     *   1. Load message history via REST API (paginated)
     *   2. Connect to WebSocket for real-time updates
     *   3. New messages arrive via WebSocket (real-time)
     *   4. Load more history via REST API (scroll up)
     * 
     * When user sends message:
     *   1. Send via WebSocket → SEND_MESSAGE
     *   2. Message saved to MongoDB
     *   3. Receiver gets it via WebSocket (if online)
     *   4. Message appears in history via REST API (if offline)
     */
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
                    // REAL-TIME: Send message to receiver's current active sessions
                    handleSendMessage(session, messageData);
                    break;
                    
                case "TYPING":
                    // REAL-TIME: Show typing indicator
                    handleTypingIndicator(session, messageData);
                    break;
                    
                case "USER_STATUS":
                    // REAL-TIME: Broadcast online/offline status
                    handleUserStatus(session, messageData);
                    break;
                    
                case "PIN_MESSAGE":
                    // REAL-TIME: Pin/unpin message notification
                    handlePinMessage(session, messageData);
                    break;
                    
                case "GET_CONVERSATIONS":
                    // REAL-TIME: Get conversation list (cached)
                    handleGetConversations(session, messageData);
                    break;
                    
                // ========================================
                // PAGINATION DISABLED IN WEBSOCKET
                // ========================================
                // GET_CONVERSATION_MESSAGES is commented out
                // Use REST API instead: GET /api/messages/conversation/{userId}?page=0&size=50
                //
                // case "GET_CONVERSATION_MESSAGES":
                //     handleGetConversationMessages(session, messageData);
                //     break;
                //
                // Reason: WebSocket should only handle real-time push notifications
                // Pagination is better handled by REST API with proper HTTP caching
                // ========================================
                    
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
                
                // Add session to user's session map (supports multiple sessions)
                ConcurrentHashMap<String, WebSocketSession> sessions = userSessions.computeIfAbsent(
                    userId, k -> new ConcurrentHashMap<>()
                );
                sessions.put(session.getId(), session);
                
                log.info("User {} authenticated via WebSocket. Total sessions: {}", userId, sessions.size());
                
                // Send authentication success message
                sendMessage(session, createAuthSuccessMessage(userId));
                
                // Broadcast user online status (only if this is the first session)
                if (sessions.size() == 1) {
                    broadcastUserStatus(userId, "ONLINE");
                }
                
                // Send pending SENT messages to user
                sendPendingMessagesToUser(userId);
                
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
            String sessionId = session != null ? session.getId() : "unknown";
            log.warn("Error sending ping to session {}: {}", sessionId, e.getMessage());
        }
    }
    
    /**
     * Handle SEND_MESSAGE WebSocket event
     * 
     * MESSAGE FLOW EXPLANATION:
     * ========================
     * 
     * 1. SENDER SENDS MESSAGE (via WebSocket)
     *    - Sender's current WebSocket session sends message
     *    - Message is saved to MongoDB database
     *    
     * 2. REAL-TIME DELIVERY (via WebSocket to receiver's CURRENT sessions)
     *    - If receiver is ONLINE: Message sent immediately to ALL active sessions
     *    - If receiver is OFFLINE: Message stored in DB, push notification sent
     *    
     * 3. MESSAGE PAGINATION (via REST API)
     *    - When user opens chat: GET /api/messages/conversation/{userId}?page=0&size=50
     *    - Loads message HISTORY from MongoDB (paginated)
     *    - WebSocket is ONLY for real-time NEW messages
     *    
     * WEBSOCKET vs REST API:
     * ======================
     * 
     * WebSocket Usage:
     * - Real-time message delivery to CURRENTLY CONNECTED sessions
     * - Typing indicators
     * - Online/offline status
     * - Message read receipts
     * 
     * REST API Usage:
     * - Loading conversation history (PAGINATION)
     * - GET /api/messages/conversation/{receiverId}?page=0&size=50
     * - Searching messages
     * - Getting conversation list
     * 
     * MULTI-SESSION SUPPORT:
     * =====================
     * - User can be connected on multiple devices (mobile + web + tablet)
     * - Message sent to ALL active sessions simultaneously
     * - Each session receives message independently
     * 
     * Example:
     * - User logs in on phone: Creates session-1
     * - User logs in on laptop: Creates session-2
     * - When message arrives: Both session-1 and session-2 receive it
     * 
     * @param session Current sender's WebSocket session
     * @param messageData Message payload from sender
     */
    private void handleSendMessage(WebSocketSession session, Map<String, Object> messageData) {
        try {
            // STEP 1: Authenticate sender from current WebSocket session
            Long senderId = getUserIdFromSession(session);
            if (senderId == null) {
                log.warn("Cannot send message: user not authenticated");
                sendMessage(session, createErrorMessage("User not authenticated"));
                return;
            }
            
            // STEP 2: Extract message data from WebSocket payload
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
            String replyToMessageId = (String) data.get("replyToMessageId");
            
            if (recipientIdObj == null || content == null) {
                log.warn("Missing recipientId or content in SEND_MESSAGE");
                sendMessage(session, createErrorMessage("Missing recipientId or content"));
                return;
            }
            
            Long recipientId = ((Number) recipientIdObj).longValue();
            
            // STEP 3: Save message to MongoDB database for persistence
            // This ensures:
            // - Message is stored for pagination (conversation history)
            // - Message can be delivered later if receiver is offline
            // - Message is available for search
            try {
                SendMessageRequest request = SendMessageRequest.builder()
                        .recipientId(recipientId)
                        .groupId(groupId)
                        .content(content)
                        .type(messageType != null ? Message.MessageType.valueOf(messageType) : Message.MessageType.TEXT)
                        .replyToMessageId(replyToMessageId)
                        .build();
                
                // MessagingService handles:
                // - Saving to MongoDB
                // - Sending push notification (if receiver offline)
                // - Publishing to Kafka events
                // - Cache management
                MessageResponse messageResponse = messagingService.sendMessage(senderId, request);
                String messageId = messageResponse.getId();
                
                log.info("Message saved to database with ID: {}", messageId);
                
                // STEP 4: Send message to receiver's CURRENT active WebSocket sessions
                // This is REAL-TIME delivery, NOT pagination
                // Receiver loads chat history via REST API when opening conversation
                ConcurrentHashMap<String, WebSocketSession> recipientSessions = userSessions.get(recipientId);
                
                if (recipientSessions != null && !recipientSessions.isEmpty()) {
                    // Receiver is ONLINE with one or more active sessions
                    String messagePayload = createWebSocketMessage(senderId, recipientId, content, 
                        messageType, messageId, replyToMessageId);
                    
                    int deliveredCount = 0;
                    // Send to ALL receiver's current active sessions
                    // Example: If receiver has phone + laptop online, both receive the message
                    for (Map.Entry<String, WebSocketSession> entry : recipientSessions.entrySet()) {
                        WebSocketSession recipientSession = entry.getValue();
                        if (recipientSession.isOpen()) {
                            // Async send to avoid blocking
                            asyncExecutor.submit(() -> {
                                try {
                                    sendMessage(recipientSession, messagePayload);
                                    messagesSent.incrementAndGet();
                                    log.debug("Message delivered to recipient {} session {} via WebSocket", 
                                        recipientId, entry.getKey());
                                } catch (Exception e) {
                                    log.error("Failed to send message to recipient {} session {}", 
                                        recipientId, entry.getKey(), e);
                                    messagesFailed.incrementAndGet();
                                    removeUserSession(recipientSession);
                                }
                            });
                            deliveredCount++;
                        }
                    }
                    
                    log.info("Message queued for real-time delivery to {} active sessions of recipient {}", 
                        deliveredCount, recipientId);
                } else {
                    // Receiver is OFFLINE
                    // - Message already saved to MongoDB (for pagination when user comes online)
                    // - Push notification will be sent by MessagingService
                    // - When receiver comes online and opens chat, they'll see message via REST API pagination
                    log.debug("Recipient {} not connected via WebSocket (offline), message stored in DB for pagination", 
                        recipientId);
                }
                
                // STEP 5: Send success confirmation to sender's current session
                try {
                    sendMessage(session, createSendMessageResponse(messageId, recipientId, 
                        "Message sent successfully"));
                } catch (Exception e) {
                    log.error("Failed to send success response to sender", e);
                }
                
                log.info("Message flow complete: Sender {} -> Receiver {} (ID: {})", senderId, recipientId, messageId);
                
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
    
    private String createWebSocketMessage(Long senderId, Long recipientId, String content, String messageType, String messageId, String replyToMessageId) {
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
            if (replyToMessageId != null && !replyToMessageId.isEmpty()) {
                data.put("replyToMessageId", replyToMessageId);
            }
            
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
            for (Map.Entry<Long, ConcurrentHashMap<String, WebSocketSession>> entry : userSessions.entrySet()) {
                Long recipientId = entry.getKey();
                ConcurrentHashMap<String, WebSocketSession> sessions = entry.getValue();
                
                // Don't send status to the user themselves
                if (!recipientId.equals(userId)) {
                    for (WebSocketSession session : sessions.values()) {
                        try {
                            sendMessage(session, statusMessage);
                            log.debug("User status broadcast sent to user: {}", recipientId);
                        } catch (Exception e) {
                            log.error("Failed to send status broadcast to user: {}", recipientId, e);
                            removeUserSession(session);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error broadcasting user status", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handlePinMessage(WebSocketSession session, Map<String, Object> messageData) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                log.warn("Cannot pin message: user not authenticated");
                sendMessage(session, createErrorMessage("User not authenticated"));
                return;
            }
            
            // Extract message data
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data == null) {
                log.warn("Missing data in PIN_MESSAGE");
                sendMessage(session, createErrorMessage("Missing message data"));
                return;
            }
            
            String messageId = (String) data.get("messageId");
            Boolean isPinnedObj = (Boolean) data.get("isPinned");
            
            if (messageId == null || isPinnedObj == null) {
                log.warn("Missing messageId or isPinned in PIN_MESSAGE");
                sendMessage(session, createErrorMessage("Missing messageId or isPinned"));
                return;
            }
            
            boolean isPinned = isPinnedObj;
            
            // Call messaging service to pin/unpin message
            MessageResponse response = messagingService.pinMessage(messageId, userId, isPinned);
            
            // Send confirmation to sender
            String pinResponseMessage = createPinMessageResponse(messageId, isPinned, "Message " + (isPinned ? "pinned" : "unpinned") + " successfully");
            sendMessage(session, pinResponseMessage);
            
            // Broadcast pin status to both users in the conversation
            broadcastPinMessage(response, userId, isPinned);
            
            log.info("Message {} {} by user {}", messageId, isPinned ? "pinned" : "unpinned", userId);
            
        } catch (Exception e) {
            log.error("Error handling pin message", e);
            try {
                sendMessage(session, createErrorMessage("Failed to pin message: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
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
    
    private void handleGetConversationMessages(WebSocketSession session, Map<String, Object> messageData) {
        try {
            Long userId = getUserIdFromSession(session);
            if (userId == null) {
                log.warn("Cannot get conversation messages: user not authenticated");
                sendMessage(session, createErrorMessage("User not authenticated"));
                return;
            }
            
            // Extract parameters from message data
            Object receiverIdObj = messageData.get("receiverId");
            Object pageNumberObj = messageData.getOrDefault("pageNumber", 0);
            Object pageSizeObj = messageData.getOrDefault("pageSize", 50);
            
            if (receiverIdObj == null) {
                sendMessage(session, createErrorMessage("receiverId is required"));
                return;
            }
            
            Long receiverId = ((Number) receiverIdObj).longValue();
            int pageNumber = ((Number) pageNumberObj).intValue();
            int pageSize = ((Number) pageSizeObj).intValue();
            
            // Create pageable with descending order by createdAt (newest first)
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize, 
                    org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id");
            
            // Get paginated messages
            org.springframework.data.domain.Page<MessageResponse> messages = 
                messagingService.getConversationMessages(userId, receiverId, pageable);
            
            // Send messages via WebSocket
            String conversationMessagesMessage = createConversationMessagesMessage(messages, receiverId);
            sendMessage(session, conversationMessagesMessage);
            
            log.info("Conversation messages sent to user {} for conversation with {} (page: {}, size: {})", 
                    userId, receiverId, pageNumber, pageSize);
            
        } catch (Exception e) {
            log.error("Error handling get conversation messages", e);
            try {
                sendMessage(session, createErrorMessage("Failed to get conversation messages: " + e.getMessage()));
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
            
            // Calculate total unread count from conversations with safe casting
            long totalUnreadCount = 0;
            try {
                totalUnreadCount = conversations.stream()
                        .mapToLong(conv -> {
                            if (conv instanceof ConversationResponse) {
                                return conv.getUnreadCount();
                            } else if (conv instanceof Map) {
                                Map<?, ?> convMap = (Map<?, ?>) conv;
                                Object unreadCount = convMap.get("unreadCount");
                                if (unreadCount instanceof Number) {
                                    return ((Number) unreadCount).longValue();
                                } else if (unreadCount instanceof String) {
                                    return Long.parseLong((String) unreadCount);
                                }
                            }
                            return 0L;
                        })
                        .sum();
            } catch (Exception e) {
                log.warn("Error calculating unread count from conversations: {}", e.getMessage());
                totalUnreadCount = 0;
            }
            
            response.put("totalUnreadCount", totalUnreadCount);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating conversation list message", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create conversation list\"}";
        }
    }
    
    private String createConversationMessagesMessage(org.springframework.data.domain.Page<MessageResponse> messages, Long receiverId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CONVERSATION_MESSAGES");
            response.put("receiverId", receiverId);
            response.put("messages", messages.getContent());
            response.put("timestamp", System.currentTimeMillis());
            
            // Pagination metadata
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("currentPage", messages.getNumber());
            pagination.put("pageSize", messages.getSize());
            pagination.put("totalPages", messages.getTotalPages());
            pagination.put("totalMessages", messages.getTotalElements());
            pagination.put("hasNext", messages.hasNext());
            pagination.put("hasPrevious", messages.hasPrevious());
            response.put("pagination", pagination);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating conversation messages message", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create conversation messages\"}";
        }
    }
    
    private String createPinMessageResponse(String messageId, boolean isPinned, String message) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "PIN_MESSAGE_RESPONSE");
            response.put("messageId", messageId);
            response.put("isPinned", isPinned);
            response.put("message", message);
            response.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error creating pin message response", e);
            return "{\"type\":\"ERROR\",\"message\":\"Failed to create pin response\"}";
        }
    }
    
    private void broadcastPinMessage(MessageResponse message, Long pinnedBy, boolean isPinned) {
        try {
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("type", "MESSAGE_PINNED");
            broadcast.put("messageId", message.getId());
            broadcast.put("isPinned", isPinned);
            broadcast.put("pinnedBy", pinnedBy);
            broadcast.put("senderId", message.getSenderId());
            broadcast.put("recipientId", message.getRecipientId());
            broadcast.put("timestamp", System.currentTimeMillis());
            
            String broadcastMessage = objectMapper.writeValueAsString(broadcast);
            
            // Send to both users in the conversation
            Long senderId = message.getSenderId();
            Long recipientId = message.getRecipientId();
            
            if (senderId != null && userSessions.containsKey(senderId)) {
                ConcurrentHashMap<String, WebSocketSession> senderSessions = userSessions.get(senderId);
                for (WebSocketSession session : senderSessions.values()) {
                    sendMessage(session, broadcastMessage);
                }
                log.debug("Pin message broadcast sent to sender: {}", senderId);
            }
            
            if (recipientId != null && userSessions.containsKey(recipientId)) {
                ConcurrentHashMap<String, WebSocketSession> recipientSessions = userSessions.get(recipientId);
                for (WebSocketSession session : recipientSessions.values()) {
                    sendMessage(session, broadcastMessage);
                }
                log.debug("Pin message broadcast sent to recipient: {}", recipientId);
            }
            
        } catch (Exception e) {
            log.error("Error broadcasting pin message", e);
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
        
        log.debug("Removing WebSocket session: {}", session.getId());
        
        // Find and remove the session from user's session map
        Long userId = null;
        String sessionId = session.getId();
        
        for (Map.Entry<Long, ConcurrentHashMap<String, WebSocketSession>> entry : userSessions.entrySet()) {
            ConcurrentHashMap<String, WebSocketSession> sessions = entry.getValue();
            if (sessions.containsKey(sessionId)) {
                sessions.remove(sessionId);
                userId = entry.getKey();
                final Long finalUserId = userId;
                
                // If user has no more sessions, remove user entry and broadcast offline
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                    asyncExecutor.submit(() -> {
                        try {
                            broadcastUserStatus(finalUserId, "OFFLINE");
                            log.info("User {} went offline (all sessions closed)", finalUserId);
                        } catch (Exception e) {
                            log.warn("Error broadcasting offline status for user {}: {}", finalUserId, e.getMessage());
                        }
                    });
                } else {
                    log.debug("User {} still has {} active session(s)", userId, sessions.size());
                }
                break;
            }
        }
        
        // Close session if still open
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.debug("Error closing session: {}", e.getMessage());
            }
        }
        
        if (userId != null) {
            log.debug("Removed session {} for user {}. Active users: {}", 
                sessionId, userId, userSessions.size());
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
     * Clean up stale WebSocket sessions - optimized for multiple sessions per user
     */
    private void cleanupStaleSessions() {
        try {
            int cleanedSessions = 0;
            int cleanedUsers = 0;
            
            Iterator<Map.Entry<Long, ConcurrentHashMap<String, WebSocketSession>>> userIterator = 
                userSessions.entrySet().iterator();
            
            while (userIterator.hasNext()) {
                Map.Entry<Long, ConcurrentHashMap<String, WebSocketSession>> userEntry = userIterator.next();
                ConcurrentHashMap<String, WebSocketSession> sessions = userEntry.getValue();
                
                // Remove stale sessions for this user
                Iterator<Map.Entry<String, WebSocketSession>> sessionIterator = sessions.entrySet().iterator();
                while (sessionIterator.hasNext()) {
                    Map.Entry<String, WebSocketSession> sessionEntry = sessionIterator.next();
                    WebSocketSession session = sessionEntry.getValue();
                    
                    if (session == null || !session.isOpen()) {
                        sessionIterator.remove();
                        cleanedSessions++;
                        log.debug("Cleaned up stale session: {}", sessionEntry.getKey());
                    }
                }
                
                // Remove user entry if no sessions remain
                if (sessions.isEmpty()) {
                    userIterator.remove();
                    cleanedUsers++;
                    log.debug("Removed user {} (no active sessions)", userEntry.getKey());
                }
            }
            
            if (cleanedSessions > 0 || cleanedUsers > 0) {
                log.info("Cleaned up {} stale sessions and {} users. Active users: {}, Total sessions: {}", 
                    cleanedSessions, cleanedUsers, userSessions.size(), 
                    userSessions.values().stream().mapToInt(ConcurrentHashMap::size).sum());
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
            log.info("Shutting down WebSocket handler executors");
            
            // Close all active sessions
            for (ConcurrentHashMap<String, WebSocketSession> sessions : userSessions.values()) {
                if (sessions != null) {
                    for (WebSocketSession session : sessions.values()) {
                        if (session != null && session.isOpen()) {
                            try {
                                session.close(CloseStatus.SERVER_ERROR);
                            } catch (IOException e) {
                                log.warn("Error closing session during shutdown: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
            
            userSessions.clear();
            
            // Shutdown async executor
            asyncExecutor.shutdown();
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            
            // Shutdown cleanup executor
            cleanupExecutor.shutdown();
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
            
            log.info("WebSocket handler shutdown complete");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncExecutor.shutdownNow();
            cleanupExecutor.shutdownNow();
        } catch (Exception e) {
            log.error("Error during WebSocket handler shutdown", e);
        }
    }
}
