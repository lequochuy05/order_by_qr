package com.qros.modules.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OtpService - Handles the generation and delivery of One-Time Passwords via
 * SMS.
 * Note: Current implementation is a placeholder for external SMS gateway
 * integration.
 */
@Service
@Slf4j
public class OtpService {

    /**
     * Sends an OTP message to a specified phone number.
     * 
     * @param phone The recipient phone number
     * @param otp   The generated OTP content or message
     */
    public void sendOtp(String phone, String otp) {
        log.info("[SMS Mock] Dispatching OTP to {}: Content: {}", phone, otp);
        // Integrate with Twilio, SpeedSMS, or other providers here
    }
}