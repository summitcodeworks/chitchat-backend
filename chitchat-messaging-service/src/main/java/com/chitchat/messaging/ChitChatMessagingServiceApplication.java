package com.chitchat.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ChitChat Messaging Service Application
 * Handles real-time messaging, group chats, and message delivery
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
public class ChitChatMessagingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatMessagingServiceApplication.class, args);
    }
}
