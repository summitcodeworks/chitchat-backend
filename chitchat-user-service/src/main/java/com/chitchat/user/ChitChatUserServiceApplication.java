package com.chitchat.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * ChitChat User Service Application
 * Handles user registration, authentication, profile management, and contacts
 */
@SpringBootApplication
@EnableJpaAuditing
@ComponentScan(basePackages = {"com.chitchat.user", "com.chitchat.shared"})
@EnableJpaRepositories(basePackages = {"com.chitchat.user.repository", "com.chitchat.shared.repository"})
@EntityScan(basePackages = {"com.chitchat.user.entity", "com.chitchat.shared.entity"})
public class ChitChatUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatUserServiceApplication.class, args);
    }
}
