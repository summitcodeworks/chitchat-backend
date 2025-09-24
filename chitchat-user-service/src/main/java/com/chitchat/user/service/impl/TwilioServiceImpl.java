package com.chitchat.user.service.impl;

import com.chitchat.user.service.TwilioService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Implementation of Twilio SMS service
 */
@Slf4j
@Service
public class TwilioServiceImpl implements TwilioService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio service initialized with account SID: {}", accountSid);
    }

    @Override
    public boolean sendOtpSms(String phoneNumber, String otp) {
        try {
            log.info("Sending OTP SMS to phone number: {}", phoneNumber);
            
            String messageBody = String.format("Your ChitChat verification code is: %s. This code will expire in 5 minutes.", otp);
            
            Message message = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                messageBody
            ).create();

            log.info("OTP SMS sent successfully. Message SID: {}", message.getSid());
            return true;

        } catch (Exception e) {
            log.error("Failed to send OTP SMS to phone number: {}", phoneNumber, e);
            return false;
        }
    }

    @Override
    public boolean sendWelcomeSms(String phoneNumber, String userName) {
        try {
            log.info("Sending welcome SMS to phone number: {}", phoneNumber);
            
            String messageBody = String.format("Welcome to ChitChat, %s! Your account has been created successfully. Start chatting with your friends!", userName);
            
            Message message = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                messageBody
            ).create();

            log.info("Welcome SMS sent successfully. Message SID: {}", message.getSid());
            return true;

        } catch (Exception e) {
            log.error("Failed to send welcome SMS to phone number: {}", phoneNumber, e);
            return false;
        }
    }

    @Override
    public boolean sendNotificationSms(String phoneNumber, String message) {
        try {
            log.info("Sending notification SMS to phone number: {}", phoneNumber);
            
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                message
            ).create();

            log.info("Notification SMS sent successfully. Message SID: {}", twilioMessage.getSid());
            return true;

        } catch (Exception e) {
            log.error("Failed to send notification SMS to phone number: {}", phoneNumber, e);
            return false;
        }
    }
}
