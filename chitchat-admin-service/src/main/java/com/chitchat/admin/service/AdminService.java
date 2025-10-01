package com.chitchat.admin.service;

import com.chitchat.admin.dto.*;
import com.chitchat.admin.entity.UserAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for admin operations
 * 
 * This service provides administrative functionality for the ChitChat application including:
 * - Admin user management (CRUD operations)
 * - User authentication and authorization
 * - Analytics and reporting
 * - User management and moderation
 * - Compliance and data export
 * - Audit logging
 * 
 * All methods may throw ChitChatException for business logic violations or errors.
 */
public interface AdminService {
    
    /**
     * Authenticates an admin user with username and password
     * 
     * Validates credentials, checks if account is active, and updates last login timestamp.
     * 
     * @param request Contains username and password for authentication
     * @return AdminResponse with admin details and authentication token
     * @throws ChitChatException if credentials are invalid or account is inactive
     */
    AdminResponse login(AdminLoginRequest request);
    
    /**
     * Retrieves the profile information for a specific admin user
     * 
     * @param adminId Unique identifier of the admin user
     * @return AdminResponse containing admin profile details
     * @throws ChitChatException if admin not found
     */
    AdminResponse getAdminProfile(Long adminId);
    
    /**
     * Retrieves all admin users in the system
     * 
     * @return List of all AdminResponse objects
     */
    List<AdminResponse> getAllAdmins();
    
    /**
     * Creates a new admin user account
     * 
     * Validates uniqueness of username and email before creation.
     * Password is encrypted using BCrypt before storage.
     * 
     * @param adminRequest Contains details for the new admin user
     * @return AdminResponse with created admin details
     * @throws ChitChatException if username or email already exists
     */
    AdminResponse createAdmin(AdminResponse adminRequest);
    
    /**
     * Updates an existing admin user's information
     * 
     * Allows updating email, role, and active status.
     * Username cannot be changed for security reasons.
     * 
     * @param adminId ID of the admin to update
     * @param adminRequest Updated admin information
     * @return AdminResponse with updated admin details
     * @throws ChitChatException if admin not found
     */
    AdminResponse updateAdmin(Long adminId, AdminResponse adminRequest);
    
    /**
     * Deletes an admin user from the system
     * 
     * Permanently removes the admin account. Use with caution.
     * 
     * @param adminId ID of the admin to delete
     * @throws ChitChatException if admin not found
     */
    void deleteAdmin(Long adminId);
    
    /**
     * Generates system analytics for a specified time period
     * 
     * Aggregates data from multiple services including:
     * - Total and active user counts
     * - Message statistics
     * - Call statistics
     * - Status update statistics
     * - Daily breakdowns
     * 
     * @param startDate Beginning of the analytics period
     * @param endDate End of the analytics period
     * @return AnalyticsResponse containing comprehensive metrics
     */
    AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Performs a management action on a user account
     * 
     * Supported actions include:
     * - Suspend user
     * - Activate user
     * - Delete user
     * - Warn user
     * 
     * All actions are logged in the audit trail.
     * 
     * @param request Contains user ID, action type, and reason
     */
    void manageUser(UserManagementRequest request);
    
    /**
     * Retrieves paginated list of actions performed on a specific user
     * 
     * @param userId ID of the user to get actions for
     * @param pageable Pagination parameters
     * @return Page of UserAction objects
     */
    Page<UserAction> getUserActions(Long userId, Pageable pageable);
    
    /**
     * Retrieves recent administrative actions across the system
     * 
     * Returns most recent actions from the last 7 days.
     * 
     * @param limit Maximum number of actions to return
     * @return List of recent UserAction objects
     */
    List<UserAction> getRecentActions(int limit);
    
    /**
     * Exports all data for a specific user
     * 
     * Generates a comprehensive export including:
     * - User profile
     * - Messages
     * - Media files
     * - Call logs
     * - Groups
     * 
     * Used for GDPR compliance and data portability.
     * 
     * @param userId ID of the user to export data for
     */
    void exportUserData(Long userId);
    
    /**
     * Exports chat logs for a specific user within a time range
     * 
     * Used for legal compliance, investigations, or user requests.
     * 
     * @param userId ID of the user to export chat logs for
     * @param startDate Beginning of the export period
     * @param endDate End of the export period
     */
    void exportChatLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Generates a compliance report for regulatory requirements
     * 
     * Creates a comprehensive report including:
     * - User activities
     * - Data retention compliance
     * - Security incidents
     * - Admin actions
     * 
     * @param startDate Beginning of the reporting period
     * @param endDate End of the reporting period
     */
    void generateComplianceReport(LocalDateTime startDate, LocalDateTime endDate);
}
