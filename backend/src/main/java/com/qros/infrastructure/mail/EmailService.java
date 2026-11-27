package com.qros.infrastructure.mail;

import org.springframework.lang.NonNull;

/**
 * Abstraction for sending transactional application emails.
 */
public interface EmailService {

    void sendResetPasswordEmail(@NonNull String toEmail, @NonNull String token);

    void sendAdmin2faOtpEmail(@NonNull String toEmail, @NonNull String otp);
}
