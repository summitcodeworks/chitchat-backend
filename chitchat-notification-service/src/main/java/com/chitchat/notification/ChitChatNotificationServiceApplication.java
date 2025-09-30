package com.chitchat.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ChitChat Notification Service Application
 * Handles push notifications and in-app alerts
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class ChitChatNotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatNotificationServiceApplication.class, args);
    }
}
