package com.chitchat.user.service;

/**
 * Service interface for Twilio SMS operations
 */
public interface TwilioService {
    
    /**
     * Send OTP SMS to the given phone number
     * @param phoneNumber The phone number to send OTP to
     * @param otp The OTP code to send
     * @return true if SMS sent successfully, false otherwise
     */
    boolean sendOtpSms(String phoneNumber, String otp);
    
    /**
     * Send welcome SMS to new user
     * @param phoneNumber The phone number
     * @param userName The user's name
     * @return true if SMS sent successfully, false otherwise
     */
    boolean sendWelcomeSms(String phoneNumber, String userName);
    
    /**
     * Send notification SMS
     * @param phoneNumber The phone number
     * @param message The message to send
     * @return true if SMS sent successfully, false otherwise
     */
    boolean sendNotificationSms(String phoneNumber, String message);
    
    /**
     * Send OTP via WhatsApp
     * @param phoneNumber The phone number to send OTP to
     * @param otp The OTP code to send
     * @return true if WhatsApp message sent successfully, false otherwise
     */
    boolean sendOtpWhatsApp(String phoneNumber, String otp);
}
