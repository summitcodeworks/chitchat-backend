package com.chitchat.gateway.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for messaging service
 */
@Slf4j
@Component
public class MessagingWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        sessions.put(userId, session);
        log.info("Messaging WebSocket connection established for user: {}", userId);
        
        // Send welcome message
        session.sendMessage(new TextMessage("{\"type\":\"connection\",\"status\":\"connected\",\"userId\":\"" + userId + "\"}"));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String userId = getUserId(session);
        log.debug("Received message from user {}: {}", userId, message.getPayload());
        
        // Forward message to messaging service
        // This would typically involve routing to the appropriate microservice
        session.sendMessage(new TextMessage("{\"type\":\"ack\",\"message\":\"Message received\"}"));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = getUserId(session);
        log.error("WebSocket transport error for user {}: {}", userId, exception.getMessage());
        sessions.remove(userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = getUserId(session);
        sessions.remove(userId);
        log.info("Messaging WebSocket connection closed for user: {}, status: {}", userId, closeStatus);
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

    public void sendMessageToUser(String userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Failed to send message to user {}: {}", userId, e.getMessage());
            }
        }
    }
}
