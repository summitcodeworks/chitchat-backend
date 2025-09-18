package com.chitchat.calls.service.impl;

import com.chitchat.calls.entity.Call;
import com.chitchat.calls.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of NotificationService for call notifications
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public NotificationServiceImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public void sendCallNotification(Long calleeId, Long callerId, Call.CallType callType, String sessionId) {
        try {
            log.info("Sending call notification to user: {} from user: {}", calleeId, callerId);
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "CALL_INCOMING");
            notification.put("calleeId", calleeId);
            notification.put("callerId", callerId);
            notification.put("callType", callType.toString());
            notification.put("sessionId", sessionId);
            notification.put("timestamp", System.currentTimeMillis());
            
            // Send to Kafka for push notification service
            kafkaTemplate.send("call-notifications", calleeId.toString(), notification);
            
            log.info("Call notification sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send call notification", e);
        }
    }
    
    @Override
    public void sendCallEndNotification(Long userId, String sessionId, String reason) {
        try {
            log.info("Sending call end notification to user: {}", userId);
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "CALL_ENDED");
            notification.put("userId", userId);
            notification.put("sessionId", sessionId);
            notification.put("reason", reason);
            notification.put("timestamp", System.currentTimeMillis());
            
            // Send to Kafka for push notification service
            kafkaTemplate.send("call-notifications", userId.toString(), notification);
            
            log.info("Call end notification sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send call end notification", e);
        }
    }
}
