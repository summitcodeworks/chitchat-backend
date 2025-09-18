package com.chitchat.status;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ChitChat Status Service Application
 * Handles ephemeral status stories and updates
 */
@SpringBootApplication
@EnableScheduling
public class ChitChatStatusServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatStatusServiceApplication.class, args);
    }
}
