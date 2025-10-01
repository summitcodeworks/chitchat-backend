package com.chitchat.admin.controller;

import com.chitchat.admin.dto.*;
import com.chitchat.admin.entity.UserAction;
import com.chitchat.admin.service.AdminService;
import com.chitchat.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for admin operations
 * 
 * This controller provides HTTP endpoints for administrative functions including:
 * - Admin authentication and profile management
 * - System analytics and metrics
 * - User management and moderation
 * - Compliance reporting and data export
 * - Audit trail access
 * 
 * All endpoints except /login require Authorization header with valid admin token.
 * 
 * Base path: /api/admin
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * Admin login endpoint
     * 
     * Authenticates an admin user using username and password.
     * Returns admin profile and authentication token on success.
     * 
     * @param request Login credentials (username and password)
     * @return ApiResponse containing admin details and auth token
     * @throws ChitChatException if credentials invalid or account inactive
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        log.info("Admin login request received for username: {}", request.getUsername());
        AdminResponse response = adminService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
    
    /**
     * Get current admin's profile
     * 
     * Returns profile information for the authenticated admin user.
     * Admin ID is extracted from the JWT token.
     * 
     * @param token JWT authentication token (Bearer token)
     * @return ApiResponse containing admin profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AdminResponse>> getAdminProfile(@RequestHeader("Authorization") String token) {
        Long adminId = extractAdminIdFromToken(token);
        log.info("Fetching profile for admin ID: {}", adminId);
        AdminResponse response = adminService.getAdminProfile(adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get all admin users
     * 
     * Returns a list of all administrators in the system.
     * Typically restricted to SUPER_ADMIN role only.
     * 
     * @param token JWT authentication token
     * @return ApiResponse containing list of all admins
     */
    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<List<AdminResponse>>> getAllAdmins(@RequestHeader("Authorization") String token) {
        log.info("Fetching all admin users");
        List<AdminResponse> response = adminService.getAllAdmins();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Create a new admin user
     * 
     * Creates a new admin account with specified role and permissions.
     * Username and email must be unique.
     * Default password is set (should be changed on first login).
     * 
     * @param token JWT authentication token
     * @param adminRequest Details for the new admin user
     * @return ApiResponse containing created admin details
     * @throws ChitChatException if username or email already exists
     */
    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<AdminResponse>> createAdmin(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AdminResponse adminRequest) {
        log.info("Creating new admin user: {}", adminRequest.getUsername());
        AdminResponse response = adminService.createAdmin(adminRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin created successfully"));
    }
    
    /**
     * Update an existing admin user
     * 
     * Updates admin information including email, role, and active status.
     * Username cannot be changed for security/audit reasons.
     * 
     * @param token JWT authentication token
     * @param adminId ID of admin to update
     * @param adminRequest Updated admin information
     * @return ApiResponse containing updated admin details
     * @throws ChitChatException if admin not found
     */
    @PutMapping("/admins/{adminId}")
    public ResponseEntity<ApiResponse<AdminResponse>> updateAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable Long adminId,
            @Valid @RequestBody AdminResponse adminRequest) {
        log.info("Updating admin user ID: {}", adminId);
        AdminResponse response = adminService.updateAdmin(adminId, adminRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin updated successfully"));
    }
    
    /**
     * Delete an admin user
     * 
     * Permanently removes an admin account from the system.
     * Use with caution - this action cannot be undone.
     * Should be restricted to SUPER_ADMIN role.
     * 
     * @param token JWT authentication token
     * @param adminId ID of admin to delete
     * @return ApiResponse with success confirmation
     * @throws ChitChatException if admin not found
     */
    @DeleteMapping("/admins/{adminId}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable Long adminId) {
        log.info("Deleting admin user ID: {}", adminId);
        adminService.deleteAdmin(adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Admin deleted successfully"));
    }
    
    /**
     * Get system analytics and metrics
     * 
     * Generates comprehensive analytics for the specified time period including:
     * - User statistics (total, active, new)
     * - Message counts and trends
     * - Call statistics
     * - Status update metrics
     * - Daily breakdowns
     * 
     * @param token JWT authentication token
     * @param startDate Beginning of analytics period (ISO date-time format)
     * @param endDate End of analytics period (ISO date-time format)
     * @return ApiResponse containing analytics data
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Generating analytics from {} to {}", startDate, endDate);
        AnalyticsResponse response = adminService.getAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Perform management action on a user
     * 
     * Executes administrative actions on user accounts such as:
     * - SUSPEND: Temporarily disable user account
     * - ACTIVATE: Re-enable a suspended account
     * - DELETE: Permanently remove user account
     * - WARN: Issue a warning to the user
     * 
     * All actions are logged in the audit trail with timestamp and reason.
     * 
     * @param token JWT authentication token
     * @param request Contains user ID, action type, and reason
     * @return ApiResponse with success confirmation
     */
    @PostMapping("/users/manage")
    public ResponseEntity<ApiResponse<Void>> manageUser(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserManagementRequest request) {
        log.info("Managing user {} with action: {}", request.getUserId(), request.getAction());
        adminService.manageUser(request);
        return ResponseEntity.ok(ApiResponse.success(null, "User management action completed"));
    }
    
    /**
     * Get actions performed on a specific user
     * 
     * Returns paginated list of all administrative actions performed on a user.
     * Useful for tracking user history and audit purposes.
     * 
     * @param token JWT authentication token
     * @param userId ID of the user to get actions for
     * @param pageable Pagination parameters (page, size, sort)
     * @return ApiResponse containing paginated user actions
     */
    @GetMapping("/users/{userId}/actions")
    public ResponseEntity<ApiResponse<Page<UserAction>>> getUserActions(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            Pageable pageable) {
        log.info("Fetching actions for user ID: {}", userId);
        Page<UserAction> response = adminService.getUserActions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Get recent administrative actions
     * 
     * Returns the most recent administrative actions across the entire system.
     * Includes actions from the last 7 days.
     * Useful for monitoring recent admin activity.
     * 
     * @param token JWT authentication token
     * @param limit Maximum number of actions to return (default: 50)
     * @return ApiResponse containing list of recent actions
     */
    @GetMapping("/actions/recent")
    public ResponseEntity<ApiResponse<List<UserAction>>> getRecentActions(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("Fetching {} recent actions", limit);
        List<UserAction> response = adminService.getRecentActions(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Export all user data
     * 
     * Initiates export of all data for a specific user including:
     * - User profile information
     * - All messages sent and received
     * - Media files
     * - Call logs
     * - Group memberships
     * 
     * Used for GDPR data portability requests.
     * Export is processed asynchronously and user is notified when complete.
     * 
     * @param token JWT authentication token
     * @param userId ID of user to export data for
     * @return ApiResponse with initiation confirmation
     */
    @PostMapping("/users/{userId}/export")
    public ResponseEntity<ApiResponse<Void>> exportUserData(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        log.info("Initiating user data export for user ID: {}", userId);
        adminService.exportUserData(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User data export initiated"));
    }
    
    /**
     * Export chat logs for a user
     * 
     * Exports all chat messages for a specific user within the given time range.
     * Used for:
     * - Legal compliance requests
     * - Investigation purposes
     * - User data requests
     * 
     * @param token JWT authentication token
     * @param userId ID of user to export chat logs for
     * @param startDate Beginning of export period (ISO date-time format)
     * @param endDate End of export period (ISO date-time format)
     * @return ApiResponse with initiation confirmation
     */
    @PostMapping("/users/{userId}/chat-logs/export")
    public ResponseEntity<ApiResponse<Void>> exportChatLogs(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Initiating chat logs export for user ID: {} from {} to {}", userId, startDate, endDate);
        adminService.exportChatLogs(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(null, "Chat logs export initiated"));
    }
    
    /**
     * Generate compliance report
     * 
     * Generates a comprehensive compliance report for regulatory requirements including:
     * - User activity summaries
     * - Data retention compliance
     * - Security incident reports
     * - Administrative actions taken
     * - System health metrics
     * 
     * Used for legal and regulatory compliance (GDPR, etc.)
     * 
     * @param token JWT authentication token
     * @param startDate Beginning of reporting period (ISO date-time format)
     * @param endDate End of reporting period (ISO date-time format)
     * @return ApiResponse with initiation confirmation
     */
    @PostMapping("/compliance/report")
    public ResponseEntity<ApiResponse<Void>> generateComplianceReport(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Initiating compliance report generation from {} to {}", startDate, endDate);
        adminService.generateComplianceReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(null, "Compliance report generation initiated"));
    }
    
    /**
     * Extracts admin ID from JWT token
     * 
     * This is a placeholder implementation.
     * In production, this should:
     * 1. Parse the JWT token
     * 2. Validate token signature
     * 3. Extract admin ID from claims
     * 4. Verify token hasn't expired
     * 
     * @param token JWT token string
     * @return Admin user ID extracted from token
     */
    private Long extractAdminIdFromToken(String token) {
        // TODO: Implement proper JWT parsing and validation
        // Extract admin ID from JWT token
        // This is a simplified implementation
        // In a real application, you would use a proper JWT service
        return 1L; // Placeholder
    }
}
