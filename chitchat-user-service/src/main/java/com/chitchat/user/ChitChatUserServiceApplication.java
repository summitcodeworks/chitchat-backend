package com.chitchat.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ChitChat User Service Application
 * Handles user registration, authentication, profile management, and contacts
 */
@SpringBootApplication
@EnableJpaAuditing
public class ChitChatUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatUserServiceApplication.class, args);
    }
}
