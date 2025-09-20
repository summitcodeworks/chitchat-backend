package com.chitchat.user.controller;

import com.chitchat.shared.dto.ApiResponse;
import com.chitchat.user.dto.*;
import com.chitchat.user.service.UserService;
import com.chitchat.user.util.PasswordUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user management operations
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Authentication request received for phone: {}", request.getPhoneNumber());
        AuthResponse response = userService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    // Legacy endpoints - kept for backward compatibility
    @Deprecated
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Legacy user registration request received for phone: {}", request.getPhoneNumber());
        AuthResponse response = userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success(response, "User registered successfully"));
    }

    @Deprecated
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(@Valid @RequestBody UserLoginRequest request) {
        log.info("Legacy user login request received for phone: {}", request.getPhoneNumber());
        AuthResponse response = userService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        UserResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = extractUserIdFromToken(token);
        UserResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }
    
    @PostMapping("/contacts/sync")
    public ResponseEntity<ApiResponse<java.util.List<UserResponse>>> syncContacts(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ContactsSyncRequest request) {
        Long userId = extractUserIdFromToken(token);
        java.util.List<UserResponse> response = userService.syncContacts(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Contacts synced successfully"));
    }
    
    @PostMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        Long blockerId = extractUserIdFromToken(token);
        userService.blockUser(blockerId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User blocked successfully"));
    }
    
    @DeleteMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        Long blockerId = extractUserIdFromToken(token);
        userService.unblockUser(blockerId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User unblocked successfully"));
    }
    
    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<java.util.List<UserResponse>>> getBlockedUsers(
            @RequestHeader("Authorization") String token) {
        Long userId = extractUserIdFromToken(token);
        java.util.List<UserResponse> response = userService.getBlockedUsers(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @RequestHeader("Authorization") String token,
            @RequestParam boolean isOnline) {
        Long userId = extractUserIdFromToken(token);
        userService.updateUserStatus(userId, isOnline);
        return ResponseEntity.ok(ApiResponse.success(null, "Status updated successfully"));
    }
    
    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByPhoneNumber(@PathVariable String phoneNumber) {
        UserResponse response = userService.getUserByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/admin/password/verify")
    public ResponseEntity<ApiResponse<PasswordUtil.PasswordTestResult>> verifyPassword(
            @RequestHeader("Authorization") String token,
            @RequestBody PasswordVerificationRequest request) {
        log.info("Password verification request received for hash: {}",
                request.getHashedPassword().substring(0, 20) + "...");

        PasswordUtil.PasswordTestResult result;

        if (request.getPlainPassword() != null && !request.getPlainPassword().isEmpty()) {
            // Verify specific password
            boolean matches = PasswordUtil.verifyPassword(request.getPlainPassword(), request.getHashedPassword());
            result = new PasswordUtil.PasswordTestResult();
            result.setHashedPassword(request.getHashedPassword());
            result.setMatchFound(matches);
            result.setMatchedPassword(matches ? request.getPlainPassword() : null);
            result.setTotalTested(1);
        } else {
            // Test common passwords
            result = PasswordUtil.testCommonPasswords(request.getHashedPassword());
        }

        return ResponseEntity.ok(ApiResponse.success(result, "Password verification completed"));
    }

    @PostMapping("/admin/password/info")
    public ResponseEntity<ApiResponse<PasswordUtil.BCryptInfo>> getPasswordInfo(
            @RequestHeader("Authorization") String token,
            @RequestBody PasswordInfoRequest request) {
        log.info("Password info request received for hash: {}",
                request.getHashedPassword().substring(0, 20) + "...");

        if (!PasswordUtil.isBCryptHash(request.getHashedPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid BCrypt hash format"));
        }

        PasswordUtil.BCryptInfo info = PasswordUtil.getBCryptInfo(request.getHashedPassword());
        return ResponseEntity.ok(ApiResponse.success(info, "Password info retrieved"));
    }

    private Long extractUserIdFromToken(String token) {
        // Extract user ID from JWT token
        // This is a simplified implementation
        // In a real application, you would use a proper JWT service
        return 1L; // Placeholder
    }
}
