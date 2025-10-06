package com.chitchat.messaging.config;

import com.chitchat.messaging.service.MessagingService;
import com.chitchat.messaging.service.impl.WebSocketServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Configuration to handle circular dependency between MessagingService and WebSocketService
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CircularDependencyConfig {
    
    private final WebSocketServiceImpl webSocketService;
    private final MessagingService messagingService;
    
    @PostConstruct
    public void initializeCircularDependency() {
        log.info("Initializing circular dependency between MessagingService and WebSocketService");
        webSocketService.initializeMessagingService(messagingService);
        log.info("Circular dependency initialization completed");
    }
}
