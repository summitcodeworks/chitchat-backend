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
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminResponse response = adminService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AdminResponse>> getAdminProfile(@RequestHeader("Authorization") String token) {
        Long adminId = extractAdminIdFromToken(token);
        AdminResponse response = adminService.getAdminProfile(adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/admins")
    public ResponseEntity<ApiResponse<List<AdminResponse>>> getAllAdmins(@RequestHeader("Authorization") String token) {
        List<AdminResponse> response = adminService.getAllAdmins();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<AdminResponse>> createAdmin(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AdminResponse adminRequest) {
        AdminResponse response = adminService.createAdmin(adminRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin created successfully"));
    }
    
    @PutMapping("/admins/{adminId}")
    public ResponseEntity<ApiResponse<AdminResponse>> updateAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable Long adminId,
            @Valid @RequestBody AdminResponse adminRequest) {
        AdminResponse response = adminService.updateAdmin(adminId, adminRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin updated successfully"));
    }
    
    @DeleteMapping("/admins/{adminId}")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(
            @RequestHeader("Authorization") String token,
            @PathVariable Long adminId) {
        adminService.deleteAdmin(adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Admin deleted successfully"));
    }
    
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        AnalyticsResponse response = adminService.getAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/users/manage")
    public ResponseEntity<ApiResponse<Void>> manageUser(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserManagementRequest request) {
        adminService.manageUser(request);
        return ResponseEntity.ok(ApiResponse.success(null, "User management action completed"));
    }
    
    @GetMapping("/users/{userId}/actions")
    public ResponseEntity<ApiResponse<Page<UserAction>>> getUserActions(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            Pageable pageable) {
        Page<UserAction> response = adminService.getUserActions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/actions/recent")
    public ResponseEntity<ApiResponse<List<UserAction>>> getRecentActions(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "50") int limit) {
        List<UserAction> response = adminService.getRecentActions(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/users/{userId}/export")
    public ResponseEntity<ApiResponse<Void>> exportUserData(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        adminService.exportUserData(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User data export initiated"));
    }
    
    @PostMapping("/users/{userId}/chat-logs/export")
    public ResponseEntity<ApiResponse<Void>> exportChatLogs(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        adminService.exportChatLogs(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(null, "Chat logs export initiated"));
    }
    
    @PostMapping("/compliance/report")
    public ResponseEntity<ApiResponse<Void>> generateComplianceReport(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        adminService.generateComplianceReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(null, "Compliance report generation initiated"));
    }
    
    private Long extractAdminIdFromToken(String token) {
        // Extract admin ID from JWT token
        // This is a simplified implementation
        // In a real application, you would use a proper JWT service
        return 1L; // Placeholder
    }
}
