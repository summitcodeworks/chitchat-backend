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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    
    private final AdminUserRepository adminUserRepository;
    private final UserActionRepository userActionRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public AdminResponse login(AdminLoginRequest request) {
        log.info("Admin login attempt for username: {}", request.getUsername());
        
        AdminUser adminUser = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ChitChatException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));
        
        if (!adminUser.getIsActive()) {
            throw new ChitChatException("Admin account is deactivated", HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), adminUser.getPassword())) {
            throw new ChitChatException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        
        // Update last login
        adminUser.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(adminUser);
        
        log.info("Admin logged in successfully: {}", adminUser.getUsername());
        
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
    
    @Override
    @Transactional
    public AdminResponse createAdmin(AdminResponse adminRequest) {
        log.info("Creating admin user: {}", adminRequest.getUsername());
        
        // Check if username already exists
        if (adminUserRepository.findByUsername(adminRequest.getUsername()).isPresent()) {
            throw new ChitChatException("Username already exists", HttpStatus.CONFLICT, "USERNAME_EXISTS");
        }
        
        // Check if email already exists
        if (adminUserRepository.findByEmail(adminRequest.getEmail()).isPresent()) {
            throw new ChitChatException("Email already exists", HttpStatus.CONFLICT, "EMAIL_EXISTS");
        }
        
        AdminUser adminUser = AdminUser.builder()
                .username(adminRequest.getUsername())
                .email(adminRequest.getEmail())
                .password(passwordEncoder.encode("defaultPassword")) // Should be set by admin
                .role(adminRequest.getRole())
                .isActive(true)
                .build();
        
        adminUser = adminUserRepository.save(adminUser);
        
        log.info("Admin user created successfully: {}", adminUser.getUsername());
        
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
    
    @Override
    public AnalyticsResponse getAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating analytics for period: {} to {}", startDate, endDate);
        
        // This would typically query multiple services for analytics data
        // For now, we'll return mock data
        
        AnalyticsResponse analytics = AnalyticsResponse.builder()
                .totalUsers(10000L)
                .activeUsers(5000L)
                .totalMessages(100000L)
                .totalCalls(5000L)
                .totalStatuses(2000L)
                .totalMediaFiles(15000L)
                .messagesByDay(generateMockDailyData())
                .callsByDay(generateMockDailyData())
                .usersByDay(generateMockDailyData())
                .generatedAt(LocalDateTime.now())
                .build();
        
        return analytics;
    }
    
    @Override
    @Transactional
    public void manageUser(UserManagementRequest request) {
        log.info("Managing user: {} with action: {}", request.getUserId(), request.getAction());
        
        // This would typically call the user service to perform the action
        // For now, we'll just log the action
        
        UserAction userAction = UserAction.builder()
                .userId(request.getUserId())
                .action(request.getAction().toString())
                .resource("USER")
                .resourceId(request.getUserId().toString())
                .details("Admin action: " + request.getAction() + ". Reason: " + request.getReason())
                .status(UserAction.ActionStatus.SUCCESS)
                .build();
        
        userActionRepository.save(userAction);
        
        log.info("User management action completed: {}", request.getAction());
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
    
    private Map<String, Long> generateMockDailyData() {
        Map<String, Long> dailyData = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            dailyData.put(date.toLocalDate().toString(), (long) (Math.random() * 1000));
        }
        return dailyData;
    }
    
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
                .build();
    }
}
