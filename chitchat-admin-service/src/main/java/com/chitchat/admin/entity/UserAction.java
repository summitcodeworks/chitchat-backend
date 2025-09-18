package com.chitchat.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User action entity for audit logging
 */
@Entity
@Table(name = "user_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserAction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String action;
    
    @Column(nullable = false)
    private String resource;
    
    private String resourceId;
    private String details;
    private String ipAddress;
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum ActionStatus {
        SUCCESS, FAILED, PENDING
    }
}
