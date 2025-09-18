package com.chitchat.status.service.impl;

import com.chitchat.status.document.Status;
import com.chitchat.status.dto.*;
import com.chitchat.status.repository.StatusRepository;
import com.chitchat.status.service.StatusService;
import com.chitchat.shared.exception.ChitChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of StatusService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {
    
    private final StatusRepository statusRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    @Transactional
    public StatusResponse createStatus(Long userId, CreateStatusRequest request) {
        log.info("Creating status for user: {}", userId);
        
        // Set expiration time (24 hours from now)
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        
        // Create status document
        Status status = Status.builder()
                .userId(userId)
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .type(request.getType())
                .privacy(request.getPrivacy())
                .expiresAt(expiresAt)
                .lastActivity(LocalDateTime.now())
                .build();
        
        status = statusRepository.save(status);
        
        // Publish status event to Kafka
        publishStatusEvent(status, "STATUS_CREATED");
        
        log.info("Status created successfully with ID: {}", status.getId());
        
        return mapToStatusResponse(status);
    }
    
    @Override
    public List<StatusResponse> getUserStatuses(Long userId) {
        List<Status> statuses = statusRepository.findByUserId(userId);
        return statuses.stream()
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StatusResponse> getActiveStatuses(Long userId) {
        List<Status> statuses = statusRepository.findActiveStatusesByUserId(userId, LocalDateTime.now());
        return statuses.stream()
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StatusResponse> getContactsStatuses(Long userId, List<Long> contactIds) {
        List<Status> statuses = statusRepository.findActiveStatusesByUserIds(contactIds, LocalDateTime.now());
        
        // Filter by privacy settings
        return statuses.stream()
                .filter(status -> isStatusVisibleToUser(status, userId))
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public StatusResponse viewStatus(String statusId, Long viewerId) {
        log.info("User {} viewing status: {}", viewerId, statusId);
        
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ChitChatException("Status not found", HttpStatus.NOT_FOUND, "STATUS_NOT_FOUND"));
        
        // Check if status is expired
        if (status.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ChitChatException("Status has expired", HttpStatus.GONE, "STATUS_EXPIRED");
        }
        
        // Check if user has already viewed this status
        boolean alreadyViewed = status.getViews().stream()
                .anyMatch(view -> view.getUserId().equals(viewerId));
        
        if (!alreadyViewed) {
            // Add view
            Status.StatusView view = Status.StatusView.builder()
                    .userId(viewerId)
                    .viewedAt(LocalDateTime.now())
                    .build();
            
            status.getViews().add(view);
            status.setLastActivity(LocalDateTime.now());
            status = statusRepository.save(status);
            
            // Publish view event to Kafka
            publishStatusEvent(status, "STATUS_VIEWED");
        }
        
        return mapToStatusResponse(status);
    }
    
    @Override
    @Transactional
    public StatusResponse reactToStatus(String statusId, Long userId, StatusReactionRequest request) {
        log.info("User {} reacting to status: {}", userId, statusId);
        
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ChitChatException("Status not found", HttpStatus.NOT_FOUND, "STATUS_NOT_FOUND"));
        
        // Check if status is expired
        if (status.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ChitChatException("Status has expired", HttpStatus.GONE, "STATUS_EXPIRED");
        }
        
        // Check if user has already reacted to this status
        boolean alreadyReacted = status.getReactions().stream()
                .anyMatch(reaction -> reaction.getUserId().equals(userId));
        
        if (alreadyReacted) {
            // Update existing reaction
            status.getReactions().stream()
                    .filter(reaction -> reaction.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(reaction -> {
                        reaction.setEmoji(request.getEmoji());
                        reaction.setReactedAt(LocalDateTime.now());
                    });
        } else {
            // Add new reaction
            Status.StatusReaction reaction = Status.StatusReaction.builder()
                    .userId(userId)
                    .emoji(request.getEmoji())
                    .reactedAt(LocalDateTime.now())
                    .build();
            
            status.getReactions().add(reaction);
        }
        
        status.setLastActivity(LocalDateTime.now());
        status = statusRepository.save(status);
        
        // Publish reaction event to Kafka
        publishStatusEvent(status, "STATUS_REACTED");
        
        return mapToStatusResponse(status);
    }
    
    @Override
    @Transactional
    public void deleteStatus(String statusId, Long userId) {
        log.info("Deleting status: {} by user: {}", statusId, userId);
        
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ChitChatException("Status not found", HttpStatus.NOT_FOUND, "STATUS_NOT_FOUND"));
        
        // Check if user is the owner
        if (!status.getUserId().equals(userId)) {
            throw new ChitChatException("Unauthorized to delete this status", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        statusRepository.delete(status);
        
        // Publish delete event to Kafka
        publishStatusEvent(status, "STATUS_DELETED");
        
        log.info("Status deleted successfully: {}", statusId);
    }
    
    @Override
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void deleteExpiredStatuses() {
        log.info("Cleaning up expired statuses");
        
        List<Status> expiredStatuses = statusRepository.findExpiredStatuses(LocalDateTime.now());
        
        for (Status status : expiredStatuses) {
            statusRepository.delete(status);
            log.info("Deleted expired status: {}", status.getId());
        }
        
        log.info("Cleaned up {} expired statuses", expiredStatuses.size());
    }
    
    @Override
    public StatusResponse getStatusById(String statusId, Long userId) {
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ChitChatException("Status not found", HttpStatus.NOT_FOUND, "STATUS_NOT_FOUND"));
        
        // Check if status is expired
        if (status.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ChitChatException("Status has expired", HttpStatus.GONE, "STATUS_EXPIRED");
        }
        
        // Check if user has access to this status
        if (!isStatusVisibleToUser(status, userId)) {
            throw new ChitChatException("Access denied to this status", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        
        return mapToStatusResponse(status);
    }
    
    @Override
    public List<StatusResponse> getStatusViews(String statusId, Long userId) {
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ChitChatException("Status not found", HttpStatus.NOT_FOUND, "STATUS_NOT_FOUND"));
        
        // Check if user is the owner
        if (!status.getUserId().equals(userId)) {
            throw new ChitChatException("Unauthorized to view status details", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        return status.getViews().stream()
                .map(view -> StatusResponse.StatusViewResponse.builder()
                        .userId(view.getUserId())
                        .viewedAt(view.getViewedAt())
                        .build())
                .map(viewResponse -> StatusResponse.builder()
                        .id(status.getId())
                        .userId(status.getUserId())
                        .content(status.getContent())
                        .mediaUrl(status.getMediaUrl())
                        .thumbnailUrl(status.getThumbnailUrl())
                        .type(status.getType())
                        .privacy(status.getPrivacy())
                        .expiresAt(status.getExpiresAt())
                        .views(List.of(viewResponse))
                        .reactions(status.getReactions().stream()
                                .map(reaction -> StatusResponse.StatusReactionResponse.builder()
                                        .userId(reaction.getUserId())
                                        .emoji(reaction.getEmoji())
                                        .reactedAt(reaction.getReactedAt())
                                        .build())
                                .collect(Collectors.toList()))
                        .lastActivity(status.getLastActivity())
                        .createdAt(status.getCreatedAt())
                        .updatedAt(status.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StatusResponse> getStatusReactions(String statusId, Long userId) {
        Status status = statusRepository.findById(statusId)
                .orElseThrow(() -> new ChitChatException("Status not found", HttpStatus.NOT_FOUND, "STATUS_NOT_FOUND"));
        
        // Check if user is the owner
        if (!status.getUserId().equals(userId)) {
            throw new ChitChatException("Unauthorized to view status details", HttpStatus.FORBIDDEN, "UNAUTHORIZED");
        }
        
        return status.getReactions().stream()
                .map(reaction -> StatusResponse.StatusReactionResponse.builder()
                        .userId(reaction.getUserId())
                        .emoji(reaction.getEmoji())
                        .reactedAt(reaction.getReactedAt())
                        .build())
                .map(reactionResponse -> StatusResponse.builder()
                        .id(status.getId())
                        .userId(status.getUserId())
                        .content(status.getContent())
                        .mediaUrl(status.getMediaUrl())
                        .thumbnailUrl(status.getThumbnailUrl())
                        .type(status.getType())
                        .privacy(status.getPrivacy())
                        .expiresAt(status.getExpiresAt())
                        .views(status.getViews().stream()
                                .map(view -> StatusResponse.StatusViewResponse.builder()
                                        .userId(view.getUserId())
                                        .viewedAt(view.getViewedAt())
                                        .build())
                                .collect(Collectors.toList()))
                        .reactions(List.of(reactionResponse))
                        .lastActivity(status.getLastActivity())
                        .createdAt(status.getCreatedAt())
                        .updatedAt(status.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    private boolean isStatusVisibleToUser(Status status, Long userId) {
        // User can always see their own statuses
        if (status.getUserId().equals(userId)) {
            return true;
        }
        
        // Check privacy settings
        switch (status.getPrivacy()) {
            case PUBLIC:
                return true;
            case CONTACTS_ONLY:
                // This would require checking if users are contacts
                // For now, we'll assume all users are contacts
                return true;
            case SELECTED_CONTACTS:
                // This would require checking if user is in selected contacts list
                // For now, we'll assume all users are selected
                return true;
            default:
                return false;
        }
    }
    
    private void publishStatusEvent(Status status, String eventType) {
        // Publish status event to Kafka for real-time updates
        kafkaTemplate.send("status-events", eventType, status);
    }
    
    private StatusResponse mapToStatusResponse(Status status) {
        List<StatusResponse.StatusViewResponse> viewResponses = status.getViews().stream()
                .map(view -> StatusResponse.StatusViewResponse.builder()
                        .userId(view.getUserId())
                        .viewedAt(view.getViewedAt())
                        .build())
                .collect(Collectors.toList());
        
        List<StatusResponse.StatusReactionResponse> reactionResponses = status.getReactions().stream()
                .map(reaction -> StatusResponse.StatusReactionResponse.builder()
                        .userId(reaction.getUserId())
                        .emoji(reaction.getEmoji())
                        .reactedAt(reaction.getReactedAt())
                        .build())
                .collect(Collectors.toList());
        
        return StatusResponse.builder()
                .id(status.getId())
                .userId(status.getUserId())
                .content(status.getContent())
                .mediaUrl(status.getMediaUrl())
                .thumbnailUrl(status.getThumbnailUrl())
                .type(status.getType())
                .privacy(status.getPrivacy())
                .expiresAt(status.getExpiresAt())
                .views(viewResponses)
                .reactions(reactionResponses)
                .lastActivity(status.getLastActivity())
                .createdAt(status.getCreatedAt())
                .updatedAt(status.getUpdatedAt())
                .build();
    }
}
