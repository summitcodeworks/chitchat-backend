package com.chitchat.calls.service;

import com.chitchat.calls.entity.Call;

/**
 * Service interface for call notifications
 */
public interface NotificationService {
    
    void sendCallNotification(Long calleeId, Long callerId, Call.CallType callType, String sessionId);
    
    void sendCallEndNotification(Long userId, String sessionId, String reason);
}
