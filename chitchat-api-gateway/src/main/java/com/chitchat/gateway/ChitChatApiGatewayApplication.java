package com.chitchat.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChitChat API Gateway Application
 * Central entry point for all client requests with routing, load balancing, and security
 */
@SpringBootApplication
public class ChitChatApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatApiGatewayApplication.class, args);
    }
}
