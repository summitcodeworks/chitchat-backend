package com.chitchat.user.service;

import com.chitchat.user.dto.*;

import java.util.List;

/**
 * Service interface for user management operations
 */
public interface UserService {

    /**
     * Firebase token authentication - handles both registration and login
     * If user exists, performs login; if not, creates new user
     */
    AuthResponse authenticateWithFirebase(FirebaseAuthRequest request);

    // Legacy methods - kept for backward compatibility
    @Deprecated
    AuthResponse registerUser(UserRegistrationRequest request);

    @Deprecated
    AuthResponse loginUser(UserLoginRequest request);
    
    UserResponse getUserProfile(Long userId);
    
    UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest request);
    
    List<UserResponse> syncContacts(Long userId, ContactsSyncRequest request);
    
    void blockUser(Long blockerId, Long blockedId);
    
    void unblockUser(Long blockerId, Long blockedId);
    
    List<UserResponse> getBlockedUsers(Long userId);
    
    void updateUserStatus(Long userId, boolean isOnline);
    
    UserResponse getUserByPhoneNumber(String phoneNumber);
    
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
}
