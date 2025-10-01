package com.chitchat.user.repository;

import com.chitchat.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity database operations
 * 
 * Provides data access methods for user management.
 * Extends JpaRepository for CRUD operations plus custom queries.
 * 
 * Standard Methods (from JpaRepository):
 * - save(), findById(), findAll(), delete(), etc.
 * 
 * Custom Query Methods:
 * - Phone number lookup (primary authentication)
 * - Firebase UID lookup (alternative authentication)
 * - Batch phone number checks (contact sync)
 * - Online user queries
 * - User search by name
 * 
 * Database: PostgreSQL
 * Table: users
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Finds user by phone number (primary lookup method)
     * 
     * Phone number is unique and used for:
     * - SMS/OTP authentication
     * - Contact discovery
     * - User identification
     * 
     * @param phoneNumber Phone number in E.164 format (e.g., +14155552671)
     * @return Optional containing User if found, empty otherwise
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    /**
     * Finds user by Firebase UID
     * 
     * Used for Firebase Authentication integration.
     * Firebase UID is unique identifier from Firebase system.
     * 
     * @param firebaseUid Firebase user identifier
     * @return Optional containing User if found, empty otherwise
     */
    Optional<User> findByFirebaseUid(String firebaseUid);
    
    /**
     * Finds multiple users by phone numbers (batch operation)
     * 
     * Used for contact synchronization:
     * - App sends list of phone numbers from device contacts
     * - Server returns which ones are registered ChitChat users
     * - Only returns active users (excludes deleted/suspended)
     * 
     * Optimized with IN clause for efficient batch lookup.
     * 
     * @param phoneNumbers List of phone numbers to check
     * @return List of User entities for registered phone numbers
     */
    @Query("SELECT u FROM User u WHERE u.phoneNumber IN :phoneNumbers AND u.isActive = true")
    List<User> findByPhoneNumbersIn(@Param("phoneNumbers") List<String> phoneNumbers);
    
    /**
     * Finds all currently online users
     * 
     * Used for:
     * - Admin dashboard metrics
     * - Real-time user count
     * - Analytics
     * 
     * Note: This query can be expensive with many users.
     * Consider caching or pagination for production.
     * 
     * @return List of all users with isOnline=true
     */
    @Query("SELECT u FROM User u WHERE u.isOnline = true")
    List<User> findOnlineUsers();
    
    /**
     * Searches users by name (case-insensitive)
     * 
     * Used for user search functionality.
     * ILIKE performs case-insensitive pattern matching.
     * Only returns active users for privacy.
     * 
     * Example: Searching "john" matches "John", "Johnny", "Johnson"
     * 
     * @param name Search term (partial match supported)
     * @return List of matching active users
     */
    @Query("SELECT u FROM User u WHERE u.name ILIKE %:name% AND u.isActive = true")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);
}
