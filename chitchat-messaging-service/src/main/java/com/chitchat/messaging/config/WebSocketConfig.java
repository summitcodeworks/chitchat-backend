package com.chitchat.messaging.config;

import com.chitchat.messaging.websocket.MessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time messaging
 * 
 * Enables WebSocket support for ChitChat messaging service.
 * Provides real-time message delivery to connected clients.
 * 
 * WebSocket Endpoints:
 * - /ws/messages - Main message broadcasting endpoint
 * - /ws/status - Message status updates (delivered, read)
 * 
 * Security:
 * - CORS enabled for all origins (development only)
 * - Authentication handled via query parameters or headers
 * 
 * Features:
 * - Real-time message broadcasting
 * - Message status updates
 * - Connection management
 * - Error handling
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final MessageWebSocketHandler messageWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Main WebSocket endpoint for message broadcasting
        registry.addHandler(messageWebSocketHandler, "/ws/messages")
                .setAllowedOrigins("*") // Allow all origins for development
                .withSockJS(); // Enable SockJS fallback for older browsers
        
        // WebSocket endpoint for message status updates
        registry.addHandler(messageWebSocketHandler, "/ws/status")
                .setAllowedOrigins("*")
                .withSockJS();
        
        // Alternative endpoint without SockJS for native WebSocket clients
        registry.addHandler(messageWebSocketHandler, "/ws/messages-native")
                .setAllowedOrigins("*");
    }
}
