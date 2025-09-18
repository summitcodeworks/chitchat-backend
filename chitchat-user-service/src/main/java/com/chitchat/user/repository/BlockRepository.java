package com.chitchat.user.repository;

import com.chitchat.user.entity.Block;
import com.chitchat.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Block entity
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);
    
    @Query("SELECT b FROM Block b WHERE b.blocker.id = :blockerId AND b.blocked.id = :blockedId")
    Optional<Block> findByBlockerIdAndBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);
    
    @Query("SELECT b.blocked FROM Block b WHERE b.blocker.id = :blockerId")
    List<User> findBlockedUsersByBlockerId(@Param("blockerId") Long blockerId);
    
    @Query("SELECT b.blocker FROM Block b WHERE b.blocked.id = :blockedId")
    List<User> findBlockersByBlockedId(@Param("blockedId") Long blockedId);
    
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
}
