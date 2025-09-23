package com.chitchat.gateway.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for notification service
 */
@Slf4j
@Component
public class NotificationWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        sessions.put(userId, session);
        log.info("Notification WebSocket connection established for user: {}", userId);
        
        // Send welcome message
        session.sendMessage(new TextMessage("{\"type\":\"connection\",\"status\":\"connected\",\"service\":\"notifications\"}"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String userId = getUserId(session);
        log.debug("Received notification message from user {}: {}", userId, message.getPayload());
        
        // Handle notification-related messages
        session.sendMessage(new TextMessage("{\"type\":\"ack\",\"message\":\"Notification received\"}"));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = getUserId(session);
        log.error("Notification WebSocket transport error for user {}: {}", userId, exception.getMessage());
        sessions.remove(userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = getUserId(session);
        sessions.remove(userId);
        log.info("Notification WebSocket connection closed for user: {}, status: {}", userId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private String getUserId(WebSocketSession session) {
        WebSocketAuthHandler.WebSocketPrincipal principal = 
            (WebSocketAuthHandler.WebSocketPrincipal) session.getPrincipal();
        return principal != null ? principal.getName() : "unknown";
    }

    public void sendNotificationToUser(String userId, String notification) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(notification));
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
            }
        }
    }
}
