package com.chitchat.user.repository;

import com.chitchat.user.entity.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OtpRequest entity
 */
@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, Long> {

    /**
     * Find the latest non-expired OTP for a phone number
     */
    @Query("SELECT o FROM OtpRequest o WHERE o.phoneNumber = :phoneNumber " +
           "AND o.expiresAt > :now AND o.isVerified = false " +
           "ORDER BY o.createdAt DESC")
    Optional<OtpRequest> findLatestValidOtpByPhoneNumber(@Param("phoneNumber") String phoneNumber,
                                                        @Param("now") LocalDateTime now);

    /**
     * Find all OTP requests for a phone number ordered by creation date
     */
    List<OtpRequest> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    /**
     * Find OTP by phone number and OTP code for verification
     */
    @Query("SELECT o FROM OtpRequest o WHERE o.phoneNumber = :phoneNumber " +
           "AND o.otpCode = :otpCode AND o.expiresAt > :now AND o.isVerified = false")
    Optional<OtpRequest> findValidOtpForVerification(@Param("phoneNumber") String phoneNumber,
                                                    @Param("otpCode") String otpCode,
                                                    @Param("now") LocalDateTime now);

    /**
     * Count unverified OTPs for a phone number within time period
     */
    @Query("SELECT COUNT(o) FROM OtpRequest o WHERE o.phoneNumber = :phoneNumber " +
           "AND o.createdAt > :since AND o.isVerified = false")
    long countUnverifiedOtpsSince(@Param("phoneNumber") String phoneNumber,
                                 @Param("since") LocalDateTime since);

    /**
     * Find expired OTPs for cleanup
     */
    @Query("SELECT o FROM OtpRequest o WHERE o.expiresAt < :now")
    List<OtpRequest> findExpiredOtps(@Param("now") LocalDateTime now);

    /**
     * Delete expired OTPs (for cleanup)
     */
    @Query("DELETE FROM OtpRequest o WHERE o.expiresAt < :expiryTime")
    void deleteExpiredOtps(@Param("expiryTime") LocalDateTime expiryTime);
}