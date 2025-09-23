package com.chitchat.gateway.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for API Gateway
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketAuthHandler webSocketAuthHandler;

    public WebSocketConfig(WebSocketAuthHandler webSocketAuthHandler) {
        this.webSocketAuthHandler = webSocketAuthHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register WebSocket handlers for different services
        registry.addHandler(new MessagingWebSocketHandler(), "/ws/messages")
                .setHandshakeHandler(webSocketAuthHandler)
                .setAllowedOrigins("*");

        registry.addHandler(new NotificationWebSocketHandler(), "/ws/notifications")
                .setHandshakeHandler(webSocketAuthHandler)
                .setAllowedOrigins("*");

        registry.addHandler(new CallsWebSocketHandler(), "/ws/calls")
                .setHandshakeHandler(webSocketAuthHandler)
                .setAllowedOrigins("*");
    }
}
