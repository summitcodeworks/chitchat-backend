package com.chitchat.user.entity;

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
 * User entity for ChitChat application
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String phoneNumber;
    
    @Column(nullable = false)
    private String name;
    
    private String avatarUrl;
    
    private String about;
    
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    @Column(name = "is_online")
    private Boolean isOnline;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "firebase_uid")
    private String firebaseUid;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
