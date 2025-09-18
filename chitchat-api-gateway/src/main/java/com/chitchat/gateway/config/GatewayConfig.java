package com.chitchat.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration for ChitChat microservices
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://chitchat-user-service"))
                
                // Messaging Service Routes
                .route("messaging-service", r -> r.path("/api/messages/**")
                        .uri("lb://chitchat-messaging-service"))
                
                // Media Service Routes
                .route("media-service", r -> r.path("/api/media/**")
                        .uri("lb://chitchat-media-service"))
                
                // Calls Service Routes
                .route("calls-service", r -> r.path("/api/calls/**")
                        .uri("lb://chitchat-calls-service"))
                
                // Notification Service Routes
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("lb://chitchat-notification-service"))
                
                // Status Service Routes
                .route("status-service", r -> r.path("/api/status/**")
                        .uri("lb://chitchat-status-service"))
                
                // Admin Service Routes
                .route("admin-service", r -> r.path("/api/admin/**")
                        .uri("lb://chitchat-admin-service"))
                
                // WebSocket Routes
                .route("websocket-messaging", r -> r.path("/ws/messages/**")
                        .uri("lb:ws://chitchat-messaging-service"))
                
                .route("websocket-calls", r -> r.path("/ws/calls/**")
                        .uri("lb:ws://chitchat-calls-service"))
                
                .route("websocket-status", r -> r.path("/ws/status/**")
                        .uri("lb:ws://chitchat-user-service"))
                
                .build();
    }
}
