package com.chitchat.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Block entity for managing user blocking functionality
 * 
 * This entity represents a blocking relationship between two users.
 * When User A blocks User B:
 * - B cannot send messages to A
 * - B cannot see A's online status or "last seen"
 * - B cannot call A
 * - B cannot see A's status updates
 * - B doesn't know they've been blocked (privacy)
 * 
 * Database Table: blocks
 * 
 * The block relationship is unidirectional:
 * - blocker -> blocked (one way)
 * - If both users block each other, two separate records exist
 * 
 * Indexes Recommended:
 * - Index on (blocker_id, blocked_id) for quick lookup
 * - Unique constraint on (blocker_id, blocked_id) to prevent duplicates
 * 
 * Privacy Design:
 * - Blocked user is never notified
 * - Messages appear to send but aren't delivered
 * - Calls appear to ring but don't go through
 * - This prevents harassment escalation
 */
@Entity
@Table(name = "blocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Block {
    
    /**
     * Unique identifier for the block relationship
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who initiated the block
     * 
     * Lazy loading to avoid fetching full user object unless needed.
     * This is the user who wants to block communication.
     * 
     * Example: If Alice blocks Bob, Alice is the blocker
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;
    
    /**
     * User who is being blocked
     * 
     * Lazy loading for performance.
     * This user's messages, calls, and status updates are hidden from blocker.
     * 
     * Example: If Alice blocks Bob, Bob is the blocked user
     * 
     * Important: Bob doesn't know he's been blocked
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;
    
    /**
     * Timestamp when the block was created
     * 
     * Automatically set by JPA auditing.
     * Used for:
     * - Tracking when block occurred
     * - Analytics on blocking behavior
     * - Compliance and moderation
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
