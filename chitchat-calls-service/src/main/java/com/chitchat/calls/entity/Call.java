package com.chitchat.calls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Call entity for tracking call sessions
 */
@Entity
@Table(name = "calls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Call {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sessionId;
    
    @Column(nullable = false)
    private Long callerId;
    
    @Column(nullable = false)
    private Long calleeId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallType callType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;
    
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long duration; // in seconds
    
    private String callerSdp;
    private String calleeSdp;
    private String iceCandidates;
    
    private String rejectionReason;
    private String endReason;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CallType {
        VOICE, VIDEO
    }
    
    public enum CallStatus {
        INITIATED, RINGING, ANSWERED, REJECTED, ENDED, FAILED, MISSED
    }
}
