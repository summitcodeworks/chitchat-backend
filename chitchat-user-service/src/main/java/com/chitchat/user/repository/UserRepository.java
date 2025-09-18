package com.chitchat.user.repository;

import com.chitchat.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    Optional<User> findByFirebaseUid(String firebaseUid);
    
    @Query("SELECT u FROM User u WHERE u.phoneNumber IN :phoneNumbers AND u.isActive = true")
    List<User> findByPhoneNumbersIn(@Param("phoneNumbers") List<String> phoneNumbers);
    
    @Query("SELECT u FROM User u WHERE u.isOnline = true")
    List<User> findOnlineUsers();
    
    @Query("SELECT u FROM User u WHERE u.name ILIKE %:name% AND u.isActive = true")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);
}
