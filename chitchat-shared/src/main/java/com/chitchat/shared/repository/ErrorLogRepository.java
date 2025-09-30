package com.chitchat.shared.repository;

import com.chitchat.shared.entity.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ErrorLog entity
 */
@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    /**
     * Find error log by trace ID
     */
    Optional<ErrorLog> findByTraceId(String traceId);

    /**
     * Find error logs by service name
     */
    List<ErrorLog> findByServiceNameOrderByCreatedAtDesc(String serviceName);

    /**
     * Find unresolved error logs
     */
    List<ErrorLog> findByResolvedFalseOrderByCreatedAtDesc();

    /**
     * Find error logs by user ID
     */
    List<ErrorLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find error logs by error code
     */
    List<ErrorLog> findByErrorCodeOrderByCreatedAtDesc(String errorCode);

    /**
     * Find error logs by HTTP status
     */
    List<ErrorLog> findByHttpStatusOrderByCreatedAtDesc(Integer httpStatus);

    /**
     * Find error logs within a time range
     */
    List<ErrorLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Find error logs by endpoint pattern
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.endpoint LIKE %:endpoint% ORDER BY e.createdAt DESC")
    List<ErrorLog> findByEndpointContaining(@Param("endpoint") String endpoint);

    /**
     * Get error count by service and time range
     */
    @Query("SELECT COUNT(e) FROM ErrorLog e WHERE e.serviceName = :serviceName AND e.createdAt BETWEEN :start AND :end")
    Long countByServiceAndTimeRange(@Param("serviceName") String serviceName, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get error count by error code and time range
     */
    @Query("SELECT COUNT(e) FROM ErrorLog e WHERE e.errorCode = :errorCode AND e.createdAt BETWEEN :start AND :end")
    Long countByErrorCodeAndTimeRange(@Param("errorCode") String errorCode, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find recent error logs with pagination
     */
    Page<ErrorLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find error logs by multiple criteria
     */
    @Query("SELECT e FROM ErrorLog e WHERE " +
           "(:serviceName IS NULL OR e.serviceName = :serviceName) AND " +
           "(:errorCode IS NULL OR e.errorCode = :errorCode) AND " +
           "(:httpStatus IS NULL OR e.httpStatus = :httpStatus) AND " +
           "(:resolved IS NULL OR e.resolved = :resolved) AND " +
           "(:userId IS NULL OR e.userId = :userId) AND " +
           "e.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY e.createdAt DESC")
    Page<ErrorLog> findByCriteria(@Param("serviceName") String serviceName,
                                  @Param("errorCode") String errorCode,
                                  @Param("httpStatus") Integer httpStatus,
                                  @Param("resolved") Boolean resolved,
                                  @Param("userId") Long userId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);

    /**
     * Get error statistics
     */
    @Query("SELECT e.errorCode, COUNT(e) FROM ErrorLog e WHERE e.createdAt BETWEEN :start AND :end GROUP BY e.errorCode ORDER BY COUNT(e) DESC")
    List<Object[]> getErrorStatsByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Get service error statistics
     */
    @Query("SELECT e.serviceName, COUNT(e) FROM ErrorLog e WHERE e.createdAt BETWEEN :start AND :end GROUP BY e.serviceName ORDER BY COUNT(e) DESC")
    List<Object[]> getServiceErrorStatsByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Delete old error logs (for cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}