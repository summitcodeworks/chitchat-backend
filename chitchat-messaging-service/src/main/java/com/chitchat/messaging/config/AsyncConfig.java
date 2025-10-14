package com.chitchat.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration for Non-blocking Operations
 * 
 * Enables asynchronous execution for:
 * - WebSocket message broadcasts
 * - Push notifications
 * - Kafka event publishing
 * - Database operations
 * 
 * Optimized for high concurrency and multiple users
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "websocketExecutor")
    public Executor websocketExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);  // Increased from 10 to handle more concurrent websocket operations
        executor.setMaxPoolSize(100);  // Increased from 20 to scale better
        executor.setQueueCapacity(500);  // Increased from 100 for more buffering
        executor.setThreadNamePrefix("ws-async-");
        executor.setKeepAliveSeconds(60);  // Keep idle threads alive for 60 seconds
        executor.setAllowCoreThreadTimeOut(true);  // Allow core threads to timeout when idle
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());  // Fallback to caller thread instead of dropping
        executor.initialize();
        log.info("WebSocket executor initialized with core: 50, max: 100, queue: 500");
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);  // Increased from 5 for more concurrent notifications
        executor.setMaxPoolSize(50);   // Increased from 10 to handle notification bursts
        executor.setQueueCapacity(200);  // Increased from 50 for better buffering
        executor.setThreadNamePrefix("notif-async-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Notification executor initialized with core: 20, max: 50, queue: 200");
        return executor;
    }

    @Bean(name = "dbExecutor")
    public Executor dbExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(30);  // Dedicated pool for async database operations
        executor.setMaxPoolSize(60);
        executor.setQueueCapacity(300);
        executor.setThreadNamePrefix("db-async-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Database executor initialized with core: 30, max: 60, queue: 300");
        return executor;
    }

    @Bean(name = "kafkaExecutor")
    public Executor kafkaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(15);  // Dedicated pool for Kafka publishing
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(150);
        executor.setThreadNamePrefix("kafka-async-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Kafka executor initialized with core: 15, max: 30, queue: 150");
        return executor;
    }
}

