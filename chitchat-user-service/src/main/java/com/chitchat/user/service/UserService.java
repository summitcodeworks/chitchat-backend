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
}
