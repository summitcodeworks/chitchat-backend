package com.chitchat.user.service.impl;

import com.chitchat.shared.exception.ChitChatException;
import com.chitchat.user.dto.*;
import com.chitchat.user.entity.Block;
import com.chitchat.user.entity.User;
import com.chitchat.user.repository.BlockRepository;
import com.chitchat.user.repository.UserRepository;
import com.chitchat.user.service.UserService;
import com.chitchat.user.service.JwtService;
import com.chitchat.user.service.FirebaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of UserService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final JwtService jwtService;
    private final FirebaseService firebaseService;
    
    @Override
    @Transactional
    public AuthResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering user with phone number: {}", request.getPhoneNumber());
        
        // Check if user already exists
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new ChitChatException("User already exists", HttpStatus.CONFLICT, "USER_EXISTS");
        }
        
        // Verify OTP with Firebase
        String firebaseUid = firebaseService.verifyPhoneNumber(request.getPhoneNumber());
        
        // Create new user
        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .name(request.getName())
                .isActive(true)
                .isOnline(false)
                .firebaseUid(firebaseUid)
                .build();
        
        user = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtService.generateToken(user);
        
        log.info("User registered successfully with ID: {}", user.getId());
        
        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .message("User registered successfully")
                .build();
    }
    
    @Override
    @Transactional
    public AuthResponse loginUser(UserLoginRequest request) {
        log.info("User login attempt for phone number: {}", request.getPhoneNumber());
        
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ChitChatException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        // Verify OTP with Firebase
        firebaseService.verifyOTP(request.getPhoneNumber(), request.getOtp());
        
        // Update last seen and online status
        user.setLastSeen(LocalDateTime.now());
        user.setIsOnline(true);
        userRepository.save(user);
        
        // Generate JWT token
        String token = jwtService.generateToken(user);
        
        log.info("User logged in successfully with ID: {}", user.getId());
        
        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .message("Login successful")
                .build();
    }
    
    @Override
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChitChatException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        return mapToUserResponse(user);
    }
    
    @Override
    @Transactional
    public UserResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChitChatException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        user.setName(request.getName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setAbout(request.getAbout());
        
        user = userRepository.save(user);
        
        log.info("User profile updated for ID: {}", userId);
        
        return mapToUserResponse(user);
    }
    
    @Override
    public List<UserResponse> syncContacts(Long userId, ContactsSyncRequest request) {
        log.info("Syncing contacts for user ID: {}", userId);
        
        List<User> registeredUsers = userRepository.findByPhoneNumbersIn(request.getPhoneNumbers());
        
        return registeredUsers.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ChitChatException("Blocker not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new ChitChatException("User to block not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            throw new ChitChatException("User already blocked", HttpStatus.CONFLICT, "USER_ALREADY_BLOCKED");
        }
        
        Block block = Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
        
        blockRepository.save(block);
        
        log.info("User {} blocked user {}", blockerId, blockedId);
    }
    
    @Override
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new ChitChatException("User not blocked", HttpStatus.NOT_FOUND, "USER_NOT_BLOCKED"));
        
        blockRepository.delete(block);
        
        log.info("User {} unblocked user {}", blockerId, blockedId);
    }
    
    @Override
    public List<UserResponse> getBlockedUsers(Long userId) {
        List<User> blockedUsers = blockRepository.findBlockedUsersByBlockerId(userId);
        
        return blockedUsers.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void updateUserStatus(Long userId, boolean isOnline) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChitChatException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        user.setIsOnline(isOnline);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User status updated for ID: {} - Online: {}", userId, isOnline);
    }
    
    @Override
    public UserResponse getUserByPhoneNumber(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ChitChatException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        return mapToUserResponse(user);
    }
    
    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ChitChatException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        
        return mapToUserResponse(user);
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .about(user.getAbout())
                .lastSeen(user.getLastSeen())
                .isOnline(user.getIsOnline())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
