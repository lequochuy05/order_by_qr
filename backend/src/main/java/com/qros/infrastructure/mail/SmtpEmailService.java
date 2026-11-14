package com.qros.infrastructure.mail;

import com.qros.core.config.AppProperties;
import com.qros.infrastructure.mail.template.Admin2faTemplateBuilder;
import com.qros.infrastructure.mail.template.ResetPasswordTemplateBuilder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SmtpEmailService - Manages sending emails in the system.
 */
@Service
public class SmtpEmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final AppProperties appProperties;

    public SmtpEmailService(JavaMailSender mailSender, MailProperties mailProperties, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.appProperties = appProperties;
    }

    /**
     * Sends a password reset email to the specified recipient.
     */
    @Async
    public void sendResetPasswordEmail(@NonNull String toEmail, @NonNull String token) {
        String baseUrl = Objects.requireNonNull(
                appProperties.getFrontend().getBaseUrl(), "app.frontend.base-url not configured");

        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Reset password";
        AppProperties.Email email = appProperties.getEmail();
        String htmlContent = ResetPasswordTemplateBuilder.build(resetLink, email.getBrandName(), email.getCopyright());

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Sends a 2FA OTP verification email to an administrator.
     */
    @Async
    public void sendAdmin2faOtpEmail(@NonNull String toEmail, @NonNull String otp) {
        AppProperties.Email email = appProperties.getEmail();
        String subject = "[" + email.getBrandName() + "] Mã xác thực đăng nhập 2FA";
        String htmlContent = Admin2faTemplateBuilder.build(otp, email.getBrandName(), email.getCopyright());

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    /**
     * Sends an HTML email to the specified recipient.
     */
    private void sendHtmlEmail(@NonNull String to, @NonNull String subject, @NonNull String htmlContent) {
        try {
            log.info("Starting to send email to: {} with subject: {}", to, subject);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(
                    Objects.requireNonNull(mailProperties.getUsername(), "spring.mail.username not configured"),
                    appProperties.getEmail().getFromName(),
                    StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new IllegalStateException("Failed to send email to " + to, e);
        }
    }
}
