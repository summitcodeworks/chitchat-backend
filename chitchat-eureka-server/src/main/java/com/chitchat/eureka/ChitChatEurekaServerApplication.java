package com.chitchat.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ChitChat Eureka Server Application
 * Service discovery server for all ChitChat microservices
 */
@SpringBootApplication
@EnableEurekaServer
public class ChitChatEurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatEurekaServerApplication.class, args);
    }
}
