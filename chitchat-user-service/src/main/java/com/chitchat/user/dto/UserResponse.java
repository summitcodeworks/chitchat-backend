package com.chitchat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user profile information in API responses
 * 
 * This DTO represents user data returned by various endpoints:
 * - GET /api/users/profile (own profile)
 * - GET /api/users/{userId} (other user's public profile)
 * - POST /api/users/contacts/sync (contact list)
 * - POST /api/users/verify-otp (after authentication)
 * 
 * Privacy Considerations:
 * - Phone number only returned for own profile (not shared with others)
 * - lastSeen can be hidden based on privacy settings
 * - isOnline can be hidden based on privacy settings
 * - Sensitive fields (deviceToken, firebaseUid, password) NEVER included
 * 
 * Differs from User Entity:
 * - No security fields (tokens, Firebase UID)
 * - No isActive flag (inactive users filtered out)
 * - Privacy-filtered based on context
 * - Safe for client consumption
 * 
 * Response Examples:
 * 
 * Own Profile:
 * {
 *   "id": 123,
 *   "phoneNumber": "+14155552671",
 *   "name": "John Doe",
 *   "avatarUrl": "https://cdn.chitchat.com/avatars/123.jpg",
 *   "about": "Hey there!",
 *   "lastSeen": "2024-01-15T10:30:00",
 *   "isOnline": true,
 *   "createdAt": "2024-01-01T12:00:00"
 * }
 * 
 * Other User's Profile (phoneNumber hidden):
 * {
 *   "id": 456,
 *   "phoneNumber": null,
 *   "name": "Alice",
 *   ...
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    /**
     * User's unique identifier
     * Used for API calls and references
     */
    private Long id;
    
    /**
     * User's phone number (E.164 format)
     * 
     * Only included when:
     * - User requesting own profile
     * - Admin/system calls
     * 
     * NOT included when viewing other users (privacy)
     */
    private String phoneNumber;
    
    /**
     * User's display name
     * 
     * Publicly visible to all contacts.
     * Examples: "John Doe", "Alice", "Bob Smith"
     */
    private String name;
    
    /**
     * URL to user's avatar/profile picture
     * 
     * Points to image in media service or CDN.
     * Null if user hasn't set custom avatar.
     */
    private String avatarUrl;
    
    /**
     * User's "about" status message
     * 
     * Bio or status text.
     * Examples: "Hey there! I am using ChitChat"
     * Optional, can be null.
     */
    private String about;
    
    /**
     * Timestamp when user was last seen online
     * 
     * Can be null if:
     * - Privacy settings hide it
     * - User hasn't been online yet
     * - Viewing own profile
     * 
     * Used for "last seen today at 2:30 PM" messages.
     */
    private LocalDateTime lastSeen;
    
    /**
     * Whether user is currently online
     * 
     * true: User is actively using the app
     * false: User is offline
     * null: Privacy settings hide online status
     * 
     * Used for real-time status indicators.
     */
    private Boolean isOnline;
    
    /**
     * When user account was created
     * 
     * Used for displaying account age.
     * Example: "Member since January 2024"
     */
    private LocalDateTime createdAt;
}
