package com.chitchat.notification.entity;

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
 * Device token entity for push notifications
 */
@Entity
@Table(name = "device_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DeviceToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String token;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;
    
    @Column(nullable = false)
    private String deviceId;
    
    private String appVersion;
    private String osVersion;
    private String deviceModel;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum DeviceType {
        ANDROID, IOS, WEB
    }
}
