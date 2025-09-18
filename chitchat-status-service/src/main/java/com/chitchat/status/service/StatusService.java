package com.chitchat.status.service;

import com.chitchat.status.dto.*;

import java.util.List;

/**
 * Service interface for status operations
 */
public interface StatusService {
    
    StatusResponse createStatus(Long userId, CreateStatusRequest request);
    
    List<StatusResponse> getUserStatuses(Long userId);
    
    List<StatusResponse> getActiveStatuses(Long userId);
    
    List<StatusResponse> getContactsStatuses(Long userId, List<Long> contactIds);
    
    StatusResponse viewStatus(String statusId, Long viewerId);
    
    StatusResponse reactToStatus(String statusId, Long userId, StatusReactionRequest request);
    
    void deleteStatus(String statusId, Long userId);
    
    void deleteExpiredStatuses();
    
    StatusResponse getStatusById(String statusId, Long userId);
    
    List<StatusResponse> getStatusViews(String statusId, Long userId);
    
    List<StatusResponse> getStatusReactions(String statusId, Long userId);
}
