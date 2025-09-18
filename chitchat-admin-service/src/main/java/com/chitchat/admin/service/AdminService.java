package com.chitchat.admin.service;

import com.chitchat.admin.dto.*;
import com.chitchat.admin.entity.UserAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for admin operations
 */
public interface AdminService {
    
    AdminResponse login(AdminLoginRequest request);
    
    AdminResponse getAdminProfile(Long adminId);
    
    List<AdminResponse> getAllAdmins();
    
    AdminResponse createAdmin(AdminResponse adminRequest);
    
    AdminResponse updateAdmin(Long adminId, AdminResponse adminRequest);
    
    void deleteAdmin(Long adminId);
    
    AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate);
    
    void manageUser(UserManagementRequest request);
    
    Page<UserAction> getUserActions(Long userId, Pageable pageable);
    
    List<UserAction> getRecentActions(int limit);
    
    void exportUserData(Long userId);
    
    void exportChatLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    void generateComplianceReport(LocalDateTime startDate, LocalDateTime endDate);
}
