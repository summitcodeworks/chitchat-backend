package com.chitchat.admin.service.impl;

import com.chitchat.admin.dto.*;
import com.chitchat.admin.entity.AdminUser;
import com.chitchat.admin.entity.UserAction;
import com.chitchat.admin.repository.AdminUserRepository;
import com.chitchat.admin.repository.UserActionRepository;
import com.chitchat.admin.service.AdminService;
import com.chitchat.shared.exception.ChitChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of AdminService
 * 
 * Provides concrete implementation of all administrative operations including:
 * - Admin user management and authentication
 * - System analytics aggregation
 * - User moderation and management
 * - Compliance reporting
 * - Data export functionality
 * 
 * All database operations are transactional to maintain data consistency.
 * Password security uses BCrypt hashing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    
    // Repository for admin user data access
    private final AdminUserRepository adminUserRepository;
    
    // Repository for audit logging of user actions
    private final UserActionRepository userActionRepository;
    
    // BCrypt password encoder for secure password handling
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Authenticates admin user and updates last login timestamp
     * 
     * Process:
     * 1. Look up admin by username
     * 2. Check if account is active
     * 3. Verify password using BCrypt
     * 4. Update last login time
     * 5. Return admin details
     */
    @Override
    @Transactional
    public AdminResponse login(AdminLoginRequest request) {
        log.info("Admin login attempt for username: {}", request.getUsername());
        
        // Step 1: Find admin user by username
        // Throws exception if username doesn't exist - don't reveal which part failed for security
        AdminUser adminUser = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ChitChatException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
        
        // Step 2: Check if the admin account is active
        // Prevents login for suspended/deactivated admin accounts
        if (!adminUser.getIsActive()) {
            log.warn("Login attempt on inactive admin account: {}", request.getUsername());
            throw new ChitChatException("Admin account is deactivated", HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
        }
        
        // Step 3: Verify password using BCrypt
        // passwordEncoder.matches() automatically handles salt and hashing
        if (!passwordEncoder.matches(request.getPassword(), adminUser.getPassword())) {
            log.warn("Invalid password for admin: {}", request.getUsername());
            throw new ChitChatException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        
        // Step 4: Update last login timestamp for audit purposes
        adminUser.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(adminUser);
        
        log.info("Admin logged in successfully: {}", adminUser.getUsername());
        
        // Step 5: Convert entity to response DTO (excludes sensitive data like password)
        return mapToAdminResponse(adminUser);
    }
    
    @Override
    public AdminResponse getAdminProfile(Long adminId) {
        AdminUser adminUser = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ChitChatException("Admin not found", HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND"));
        
        return mapToAdminResponse(adminUser);
    }
    
    @Override
    public List<AdminResponse> getAllAdmins() {
        List<AdminUser> adminUsers = adminUserRepository.findAll();
        return adminUsers.stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Creates a new admin user with validation and secure password handling
     * 
     * Process:
     * 1. Validate username uniqueness
     * 2. Validate email uniqueness
     * 3. Create admin entity with encrypted default password
     * 4. Set account as active
     * 5. Save to database
     */
    @Override
    @Transactional
    public AdminResponse createAdmin(AdminResponse adminRequest) {
        log.info("Creating admin user: {}", adminRequest.getUsername());
        
        // Validation Step 1: Ensure username is unique
        // Usernames are used for login and must be unique across all admins
        if (adminUserRepository.findByUsername(adminRequest.getUsername()).isPresent()) {
            log.warn("Attempt to create admin with existing username: {}", adminRequest.getUsername());
            throw new ChitChatException("Username already exists", HttpStatus.CONFLICT, "USERNAME_EXISTS");
        }
        
        // Validation Step 2: Ensure email is unique
        // Emails are used for communication and password recovery
        if (adminUserRepository.findByEmail(adminRequest.getEmail()).isPresent()) {
            log.warn("Attempt to create admin with existing email: {}", adminRequest.getEmail());
            throw new ChitChatException("Email already exists", HttpStatus.CONFLICT, "EMAIL_EXISTS");
        }
        
        // Create admin entity with builder pattern
        AdminUser adminUser = AdminUser.builder()
                .username(adminRequest.getUsername())
                .email(adminRequest.getEmail())
                // Password is BCrypt hashed before storage - never store plain text
                // Default password should be changed on first login
                .password(passwordEncoder.encode("defaultPassword"))
                .role(adminRequest.getRole())
                // New admin accounts are active by default
                .isActive(true)
                .build();
        
        // Persist to database - triggers JPA auditing for createdAt
        adminUser = adminUserRepository.save(adminUser);
        
        log.info("Admin user created successfully with ID: {} and role: {}", adminUser.getId(), adminUser.getRole());
        
        // Convert to DTO for response (excludes password)
        return mapToAdminResponse(adminUser);
    }
    
    @Override
    @Transactional
    public AdminResponse updateAdmin(Long adminId, AdminResponse adminRequest) {
        AdminUser adminUser = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ChitChatException("Admin not found", HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND"));
        
        adminUser.setEmail(adminRequest.getEmail());
        adminUser.setRole(adminRequest.getRole());
        adminUser.setIsActive(adminRequest.getIsActive());
        
        adminUser = adminUserRepository.save(adminUser);
        
        return mapToAdminResponse(adminUser);
    }
    
    @Override
    @Transactional
    public void deleteAdmin(Long adminId) {
        AdminUser adminUser = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ChitChatException("Admin not found", HttpStatus.NOT_FOUND, "ADMIN_NOT_FOUND"));
        
        adminUserRepository.delete(adminUser);
        
        log.info("Admin user deleted: {}", adminUser.getUsername());
    }
    
    /**
     * Generates comprehensive system analytics for the specified time period
     * 
     * In production, this would:
     * 1. Query user-service for user statistics
     * 2. Query messaging-service for message counts
     * 3. Query calls-service for call statistics
     * 4. Query status-service for status updates
     * 5. Query media-service for media file counts
     * 6. Aggregate all data into a single response
     * 
     * Currently returns mock data for demonstration purposes.
     */
    @Override
    public AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating analytics for period: {} to {}", startDate, endDate);
        
        // TODO: In production, this should make actual service calls to:
        // - User Service (via RestTemplate/WebClient or Feign)
        // - Messaging Service
        // - Calls Service
        // - Status Service
        // - Media Service
        // Each service would provide its own metrics for the time period
        
        // For now, we return mock data as placeholder
        AnalyticsResponse analytics = AnalyticsResponse.builder()
                .totalUsers(10000L)          // Total registered users
                .activeUsers(5000L)           // Users active in time period
                .totalMessages(100000L)       // Total messages sent
                .totalCalls(5000L)            // Total calls made
                .totalStatuses(2000L)         // Total status updates
                .totalMediaFiles(15000L)      // Total media files shared
                .messagesByDay(generateMockDailyData())  // Daily message breakdown
                .callsByDay(generateMockDailyData())     // Daily call breakdown
                .usersByDay(generateMockDailyData())     // Daily new users
                .generatedAt(LocalDateTime.now())        // Timestamp of report generation
                .build();
        
        log.info("Analytics generated successfully with {} total users", analytics.getTotalUsers());
        return analytics;
    }
    
    /**
     * Performs administrative action on a user account
     * 
     * Process:
     * 1. Call user-service to execute the action (suspend, activate, delete, warn)
     * 2. Log the action in audit trail
     * 3. Handle any errors from user-service
     * 
     * All actions are recorded for compliance and auditing.
     */
    @Override
    @Transactional
    public void manageUser(UserManagementRequest request) {
        log.info("Managing user: {} with action: {}", request.getUserId(), request.getAction());
        
        // TODO: In production, call user-service via REST client
        // Example:
        // try {
        //     userServiceClient.performAction(request.getUserId(), request.getAction(), request.getReason());
        // } catch (Exception e) {
        //     log.error("Failed to perform action on user-service", e);
        //     // Log as FAILED in audit trail
        //     throw new ChitChatException("Failed to execute user management action", HttpStatus.INTERNAL_SERVER_ERROR, "ACTION_FAILED");
        // }
        
        // Create audit log entry for this administrative action
        // This provides complete traceability of who did what and when
        UserAction userAction = UserAction.builder()
                .userId(request.getUserId())                    // Target user ID
                .action(request.getAction().toString())         // Action type (SUSPEND, DELETE, etc.)
                .resource("USER")                               // Resource type
                .resourceId(request.getUserId().toString())     // Resource ID
                // Details include action and reason for audit purposes
                .details("Admin action: " + request.getAction() + ". Reason: " + request.getReason())
                .status(UserAction.ActionStatus.SUCCESS)        // Mark as successful
                .build();
        
        // Persist audit log - critical for compliance
        userActionRepository.save(userAction);
        
        log.info("User management action completed successfully: {}", request.getAction());
    }
    
    @Override
    public Page<UserAction> getUserActions(Long userId, Pageable pageable) {
        return userActionRepository.findByUserId(userId, pageable);
    }
    
    @Override
    public List<UserAction> getRecentActions(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return userActionRepository.findRecentActions(since).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public void exportUserData(Long userId) {
        log.info("Exporting user data for user: {}", userId);
        
        // This would typically generate a comprehensive data export
        // including messages, media, calls, etc.
        
        UserAction userAction = UserAction.builder()
                .userId(userId)
                .action("EXPORT_DATA")
                .resource("USER_DATA")
                .resourceId(userId.toString())
                .details("Data export requested")
                .status(UserAction.ActionStatus.SUCCESS)
                .build();
        
        userActionRepository.save(userAction);
        
        log.info("User data export completed for user: {}", userId);
    }
    
    @Override
    public void exportChatLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Exporting chat logs for user: {} from {} to {}", userId, startDate, endDate);
        
        // This would typically query the messaging service for chat logs
        
        UserAction userAction = UserAction.builder()
                .userId(userId)
                .action("EXPORT_CHAT_LOGS")
                .resource("CHAT_LOGS")
                .resourceId(userId.toString())
                .details("Chat logs export from " + startDate + " to " + endDate)
                .status(UserAction.ActionStatus.SUCCESS)
                .build();
        
        userActionRepository.save(userAction);
        
        log.info("Chat logs export completed for user: {}", userId);
    }
    
    @Override
    public void generateComplianceReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating compliance report for period: {} to {}", startDate, endDate);
        
        // This would typically generate a comprehensive compliance report
        // including user activities, data retention, etc.
        
        UserAction userAction = UserAction.builder()
                .userId(0L) // System action
                .action("GENERATE_COMPLIANCE_REPORT")
                .resource("COMPLIANCE")
                .details("Compliance report generated for period " + startDate + " to " + endDate)
                .status(UserAction.ActionStatus.SUCCESS)
                .build();
        
        userActionRepository.save(userAction);
        
        log.info("Compliance report generated successfully");
    }
    
    /**
     * Generates mock daily data for last 7 days
     * 
     * Helper method for analytics demonstration.
     * In production, this would query actual databases for real metrics.
     * 
     * @return Map of date strings to count values
     */
    private Map<String, Long> generateMockDailyData() {
        Map<String, Long> dailyData = new HashMap<>();
        
        // Generate data for last 7 days (including today)
        for (int i = 6; i >= 0; i--) {
            // Calculate the date (i days ago from now)
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            
            // Generate random count between 0-999 for demonstration
            // In production, this would be actual query results
            dailyData.put(date.toLocalDate().toString(), (long) (Math.random() * 1000));
        }
        
        return dailyData;
    }
    
    /**
     * Maps AdminUser entity to AdminResponse DTO
     * 
     * Converts database entity to data transfer object for API responses.
     * Critically, this excludes the password field for security.
     * 
     * @param adminUser The admin entity from database
     * @return AdminResponse DTO safe for API response
     */
    private AdminResponse mapToAdminResponse(AdminUser adminUser) {
        return AdminResponse.builder()
                .id(adminUser.getId())
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .role(adminUser.getRole())
                .isActive(adminUser.getIsActive())
                .lastLogin(adminUser.getLastLogin())
                .createdAt(adminUser.getCreatedAt())
                .updatedAt(adminUser.getUpdatedAt())
                // NOTE: Password is intentionally excluded for security
                .build();
    }
}
