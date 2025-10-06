package com.chitchat.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;

/**
 * ChitChat API Gateway Application
 * Central entry point for all client requests with routing, load balancing, and security
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
})
@EntityScan(basePackages = {"com.chitchat.shared.entity"})
@EnableJpaRepositories(basePackages = {"com.chitchat.shared.repository"})
@ComponentScan(basePackages = {"com.chitchat.gateway", "com.chitchat.shared"})
public class ChitChatApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatApiGatewayApplication.class, args);
    }
}
