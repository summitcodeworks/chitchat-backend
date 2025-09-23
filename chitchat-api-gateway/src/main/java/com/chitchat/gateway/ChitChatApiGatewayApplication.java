package com.chitchat.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ChitChat API Gateway Application
 * Central entry point for all client requests with routing, load balancing, and security
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class
})
@EnableFeignClients
public class ChitChatApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChitChatApiGatewayApplication.class, args);
    }
}
