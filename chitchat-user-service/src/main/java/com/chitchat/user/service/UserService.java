package com.chitchat.user.service;

import com.chitchat.user.dto.*;

import java.util.List;

/**
 * Service interface for user management operations
 */
public interface UserService {

    /**
     * Send OTP to phone number for verification
     */
    SendOtpResponse sendOTP(SendOtpRequest request);

    /**
     * WhatsApp-style authentication - handles both registration and login
     * If user exists, performs login; if not, creates new user
     */
    AuthResponse authenticate(AuthenticationRequest request);

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
}
