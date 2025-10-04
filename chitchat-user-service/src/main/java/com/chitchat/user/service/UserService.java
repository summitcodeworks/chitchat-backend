package com.chitchat.user.service;

import com.chitchat.user.dto.*;

import java.util.List;

/**
 * Service interface for user management operations
 * 
 * This service handles all user-related business logic including:
 * - User authentication (SMS/OTP and Firebase)
 * - Profile management
 * - Contact synchronization
 * - User blocking/unblocking
 * - Online status tracking
 * - Device token management
 * - Phone number verification
 * 
 * Authentication Methods:
 * 1. SMS/OTP: Primary method (authenticateWithPhoneNumber)
 * 2. Firebase: Alternative method (authenticateWithFirebase)
 * 3. Legacy: Deprecated methods kept for backward compatibility
 */
public interface UserService {

    /**
     * Authenticates user with Firebase token
     * 
     * Handles both registration and login in a single operation:
     * - If user exists: Performs login and returns existing user
     * - If user doesn't exist: Creates new user and returns it
     * 
     * Firebase token is verified with Firebase Admin SDK before processing.
     * 
     * @param request Contains Firebase ID token and user info
     * @return AuthResponse with JWT token and user details
     * @throws ChitChatException if Firebase token is invalid
     */
    AuthResponse authenticateWithFirebase(FirebaseAuthRequest request);

    /**
     * Legacy user registration method
     * 
     * @deprecated Use authenticateWithPhoneNumber() instead after OTP verification
     * This method is kept for backward compatibility only.
     */
    @Deprecated
    AuthResponse registerUser(UserRegistrationRequest request);

    /**
     * Legacy user login method
     * 
     * @deprecated Use authenticateWithPhoneNumber() instead after OTP verification
     * This method is kept for backward compatibility only.
     */
    @Deprecated
    AuthResponse loginUser(UserLoginRequest request);
    
    /**
     * Retrieves user's own profile information
     * 
     * @param userId ID of the user
     * @return UserResponse with profile details
     * @throws ChitChatException if user not found
     */
    UserResponse getUserProfile(Long userId);
    
    /**
     * Updates user profile information
     * 
     * Allows updating:
     * - Name
     * - Avatar URL
     * - About/status message
     * 
     * @param userId ID of the user
     * @param request Updated profile information
     * @return UserResponse with updated profile
     * @throws ChitChatException if user not found
     */
    UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest request);
    
    /**
     * Synchronizes user's contacts with ChitChat users
     * 
     * Checks which phone numbers from user's contact list are registered on ChitChat.
     * Returns list of registered users for easy chat initiation.
     * 
     * @param userId ID of the user syncing contacts
     * @param request Contains list of phone numbers from device contacts
     * @return List of UserResponse for contacts that are registered
     */
    List<UserResponse> syncContacts(Long userId, ContactsSyncRequest request);
    
    /**
     * Blocks a user
     * 
     * When blocked:
     * - Blocked user cannot send messages
     * - Blocked user cannot see online status
     * - Blocked user cannot make calls
     * - Existing conversation remains visible
     * 
     * @param blockerId ID of user doing the blocking
     * @param blockedId ID of user being blocked
     * @throws ChitChatException if users not found or already blocked
     */
    void blockUser(Long blockerId, Long blockedId);
    
    /**
     * Unblocks a previously blocked user
     * 
     * Restores normal interaction capabilities.
     * 
     * @param blockerId ID of user doing the unblocking
     * @param blockedId ID of user being unblocked
     * @throws ChitChatException if users not found or not blocked
     */
    void unblockUser(Long blockerId, Long blockedId);
    
    /**
     * Gets list of users blocked by this user
     * 
     * @param userId ID of the user
     * @return List of UserResponse for blocked users
     */
    List<UserResponse> getBlockedUsers(Long userId);
    
    /**
     * Updates user's online/offline status
     * 
     * Called when:
     * - User opens app (isOnline = true)
     * - User closes app (isOnline = false)
     * - WebSocket connection established/terminated
     * 
     * Also updates lastSeen timestamp.
     * 
     * @param userId ID of the user
     * @param isOnline true if online, false if offline
     */
    void updateUserStatus(Long userId, boolean isOnline);
    
    /**
     * Finds user by phone number
     * 
     * @param phoneNumber Phone number in E.164 format
     * @return UserResponse with user details
     * @throws ChitChatException if user not found
     */
    UserResponse getUserByPhoneNumber(String phoneNumber);
    
    /**
     * Finds user by ID
     * 
     * @param userId ID of the user
     * @return UserResponse with user details
     * @throws ChitChatException if user not found
     */
    UserResponse getUserById(Long userId);
    
    /**
     * Check if a phone number exists in the system for creating new chats
     * @param phoneNumber The phone number to check
     * @return PhoneNumberCheckResponse with existence status and user details if found
     */
    PhoneNumberCheckResponse checkPhoneNumberExists(String phoneNumber);
    
    /**
     * Check if multiple phone numbers exist in the system
     * @param request The batch phone number check request
     * @return BatchPhoneNumberCheckResponse with existence status for each phone number
     */
    BatchPhoneNumberCheckResponse checkMultiplePhoneNumbersExist(BatchPhoneNumberCheckRequest request);
    
    /**
     * Authenticate user with phone number (login or register)
     * @param phoneNumber The phone number
     * @return AuthResponse with JWT token and user details
     */
    AuthResponse authenticateWithPhoneNumber(String phoneNumber);
    
    /**
     * Update device token for push notifications
     * @param userId The user ID
     * @param request The device token update request
     * @return UserResponse with updated user details
     */
    UserResponse updateDeviceToken(Long userId, DeviceTokenUpdateRequest request);
    
    /**
     * Find user by phone number (internal use)
     * @param phoneNumber The phone number
     * @return Optional containing User entity if found
     */
    java.util.Optional<com.chitchat.user.entity.User> findUserByPhoneNumber(String phoneNumber);
    
    /**
     * Refresh access token using refresh token
     * 
     * Validates the refresh token and issues a new access token.
     * The old refresh token is revoked and a new one is issued (token rotation).
     * 
     * @param request Contains the refresh token
     * @return AuthResponse with new access token and refresh token
     * @throws ChitChatException if refresh token is invalid or expired
     */
    AuthResponse refreshAccessToken(RefreshTokenRequest request);
}
