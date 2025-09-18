package com.chitchat.admin.repository;

import com.chitchat.admin.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AdminUser entity
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    
    Optional<AdminUser> findByUsername(String username);
    
    Optional<AdminUser> findByEmail(String email);
    
    List<AdminUser> findByRole(AdminUser.AdminRole role);
    
    List<AdminUser> findByIsActive(Boolean isActive);
    
    @Query("SELECT au FROM AdminUser au WHERE au.role IN :roles AND au.isActive = true")
    List<AdminUser> findByRolesAndActive(@Param("roles") List<AdminUser.AdminRole> roles);
    
    @Query("SELECT au FROM AdminUser au WHERE au.lastLogin < :before")
    List<AdminUser> findInactiveAdmins(@Param("before") java.time.LocalDateTime before);
}
