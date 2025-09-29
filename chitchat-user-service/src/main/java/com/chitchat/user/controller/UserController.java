package com.chitchat.user.controller;

import com.chitchat.shared.dto.ApiResponse;
import com.chitchat.user.dto.*;
import com.chitchat.user.entity.OtpRequest;
import com.chitchat.user.service.UserService;
import com.chitchat.user.service.TwilioService;
import com.chitchat.user.service.OtpService;
import com.chitchat.user.service.OtpRequestService;
import com.chitchat.user.service.JwtService;
import com.chitchat.user.util.PasswordUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Enumeration;

/**
 * REST controller for user management operations
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TwilioService twilioService;
    private final OtpService otpService;
    private final OtpRequestService otpRequestService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    // SMS-based Authentication Endpoints
    
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request,
                                                     HttpServletRequest httpRequest) {
        log.info("Send OTP request received for phone: {}", request.getPhoneNumber());

        // Check rate limiting
        if (otpRequestService.hasExceededRequestLimit(request.getPhoneNumber(), 5, 15)) {
            log.warn("Rate limit exceeded for phone: {}", request.getPhoneNumber());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Too many OTP requests. Please try again later."));
        }

        // Extract request context
        String requestIp = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String requestHeaders = extractHeaders(httpRequest);
        String requestPayload = serializeToJson(request);

        log.info("OTP request context - IP: {}, User-Agent: {}", requestIp, userAgent);

        // Generate OTP
        String otp = otpService.generateOtp(request.getPhoneNumber());
        log.info("Generated OTP for phone: {}", request.getPhoneNumber());

        // Create OTP request record
        OtpRequest otpRequestRecord = OtpRequest.builder()
            .phoneNumber(request.getPhoneNumber())
            .otpCode(otp)
            .requestIp(requestIp)
            .userAgent(userAgent)
            .requestHeaders(requestHeaders)
            .requestPayload(requestPayload)
            .expiresAt(LocalDateTime.now().plusMinutes(5))
            .build();

        // Save the OTP request to database
        otpRequestRecord = otpRequestService.saveOtpRequest(otpRequestRecord);
        log.info("Saved OTP request to database with ID: {}", otpRequestRecord.getId());

        // Send OTP via SMS
        boolean smsSent = false;
        String smsErrorMessage = null;
        String twilioMessageSid = null;

        try {
            smsSent = twilioService.sendOtpSms(request.getPhoneNumber(), otp);
            if (smsSent) {
                log.info("SMS sent successfully for OTP request ID: {}", otpRequestRecord.getId());
            }
        } catch (Exception e) {
            smsErrorMessage = e.getMessage();
            log.error("Failed to send SMS for OTP request ID: {}", otpRequestRecord.getId(), e);
        }

        // Update SMS result in database
        otpRequestRecord = otpRequestService.updateSmsResult(
            otpRequestRecord.getId(), smsSent, smsErrorMessage, twilioMessageSid);

        // Prepare response
        ApiResponse<Void> response;
        ResponseEntity<ApiResponse<Void>> responseEntity;

        if (smsSent) {
            response = ApiResponse.success(null, "OTP sent successfully");
            responseEntity = ResponseEntity.ok(response);

            // Update success response in database
            otpRequestRecord.setResponseStatus("SUCCESS");
            otpRequestRecord.setResponseMessage("OTP sent successfully");
        } else {
            response = ApiResponse.error("Failed to send OTP. Please try again.");
            responseEntity = ResponseEntity.internalServerError().body(response);

            // Update error response in database
            otpRequestRecord.setResponseStatus("ERROR");
            otpRequestRecord.setResponseMessage("Failed to send OTP. Please try again.");
        }

        // Save final response data
        otpRequestRecord.setResponsePayload(serializeToJson(response));
        otpRequestService.saveOtpRequest(otpRequestRecord);

        log.info("Completed OTP request processing for ID: {} - SMS sent: {}",
                otpRequestRecord.getId(), smsSent);

        return responseEntity;
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify OTP request received for phone: {}", request.getPhoneNumber());

        // Find OTP in database first
        var otpRecord = otpRequestService.findOtpForVerification(request.getPhoneNumber(), request.getOtp());

        if (otpRecord.isEmpty()) {
            log.warn("Invalid or expired OTP for phone: {}", request.getPhoneNumber());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid or expired OTP"));
        }

        OtpRequest otpRequest = otpRecord.get();

        // Increment verification attempts
        otpRequest = otpRequestService.incrementVerificationAttempts(otpRequest.getId());

        // Check if too many verification attempts
        if (otpRequest.getVerificationAttempts() > 3) {
            log.warn("Too many verification attempts for OTP ID: {}", otpRequest.getId());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Too many verification attempts. Please request a new OTP."));
        }

        // Verify OTP using both database and Redis (double verification)
        boolean isValidOtp = otpService.verifyOtp(request.getPhoneNumber(), request.getOtp());

        if (!isValidOtp) {
            log.warn("OTP verification failed for phone: {}", request.getPhoneNumber());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid or expired OTP"));
        }

        // Mark OTP as verified in database
        otpRequestService.markAsVerified(otpRequest.getId());
        log.info("OTP verified successfully for phone: {}", request.getPhoneNumber());

        // Authenticate user (login or register)
        AuthResponse response = userService.authenticateWithPhoneNumber(request.getPhoneNumber());

        log.info("User authentication completed for phone: {}", request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateWithFirebase(@Valid @RequestBody FirebaseAuthRequest request) {
        log.info("Firebase authentication request received");
        AuthResponse response = userService.authenticateWithFirebase(request);
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
    
    @PutMapping("/device-token")
    public ResponseEntity<ApiResponse<UserResponse>> updateDeviceToken(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody DeviceTokenUpdateRequest request) {
        Long userId = extractUserIdFromToken(token);
        UserResponse response = userService.updateDeviceToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Device token updated successfully"));
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
    
    @GetMapping("/check-phone/{phoneNumber}")
    public ResponseEntity<ApiResponse<PhoneNumberCheckResponse>> checkPhoneNumberExists(@PathVariable String phoneNumber) {
        log.info("Phone number existence check request for: {}", phoneNumber);
        PhoneNumberCheckResponse response = userService.checkPhoneNumberExists(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
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

    // Temporary debug endpoint for getting OTP when Twilio is not working
    @GetMapping("/debug/get-otp/{phoneNumber}")
    public ResponseEntity<ApiResponse<String>> getOtpForTesting(@PathVariable String phoneNumber) {
        log.info("Debug OTP retrieval request for phone: {}", phoneNumber);

        // First try to get from database
        var otpRecord = otpRequestService.findLatestValidOtp(phoneNumber);
        if (otpRecord.isPresent()) {
            String otp = otpRecord.get().getOtpCode();
            log.info("Retrieved OTP from database for phone: {}", phoneNumber);
            return ResponseEntity.ok(ApiResponse.success(otp, "OTP retrieved from database for testing purposes"));
        }

        // Fallback to Redis
        String otp = otpService.getOtpForTesting(phoneNumber);
        if (otp != null) {
            log.info("Retrieved OTP from Redis for phone: {}", phoneNumber);
            return ResponseEntity.ok(ApiResponse.success(otp, "OTP retrieved from Redis for testing purposes"));
        }

        log.warn("No OTP found in database or Redis for phone: {}", phoneNumber);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("No OTP found for this phone number. Please request OTP first."));
    }

    // Debug endpoint to get OTP request history
    @GetMapping("/debug/otp-history/{phoneNumber}")
    public ResponseEntity<ApiResponse<java.util.List<OtpRequest>>> getOtpHistory(@PathVariable String phoneNumber) {
        log.info("OTP history request for phone: {}", phoneNumber);

        java.util.List<OtpRequest> history = otpRequestService.getOtpHistory(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success(history, "OTP history retrieved successfully"));
    }

    private Long extractUserIdFromToken(String token) {
        // Extract user ID from JWT token using JwtService
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtService.extractUserId(token);
    }

    // Utility methods for request context extraction

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, get the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private String extractHeaders(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);

            // Mask sensitive headers
            if (headerName.toLowerCase().contains("authorization") ||
                headerName.toLowerCase().contains("cookie") ||
                headerName.toLowerCase().contains("token")) {
                headerValue = "***MASKED***";
            }

            headers.append(headerName).append(": ").append(headerValue).append("\n");
        }

        return headers.toString();
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return "Unable to serialize: " + e.getMessage();
        }
    }
}
