package com.chitchat.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for send OTP response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpResponse {

    private String phoneNumber;
    private String message;
    private Boolean otpSent;

    // For development/testing - indicates the test OTP
    private String testOtp;
}