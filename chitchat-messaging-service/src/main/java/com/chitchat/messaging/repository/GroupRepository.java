package com.chitchat.messaging.repository;

import com.chitchat.messaging.document.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Group document
 */
@Repository
public interface GroupRepository extends MongoRepository<Group, String> {
    
    @Query("{ 'members.userId': ?0 }")
    List<Group> findByUserId(Long userId);
    
    @Query("{ 'adminId': ?0 }")
    List<Group> findByAdminId(Long adminId);
    
    @Query("{ name: { $regex: ?0, $options: 'i' } }")
    List<Group> findByNameContainingIgnoreCase(String name);
    
    @Query("{ 'members.userId': ?0, 'members.role': ?1 }")
    List<Group> findByUserIdAndRole(Long userId, Group.GroupRole role);
    
    @Query("{ 'members.userId': ?0 }")
    Optional<Group> findGroupByUserId(Long userId);
    
    @Query("{ 'members.userId': ?0, 'members.role': { $in: ['ADMIN', 'MODERATOR'] } }")
    List<Group> findGroupsWhereUserIsAdminOrModerator(Long userId);
}
