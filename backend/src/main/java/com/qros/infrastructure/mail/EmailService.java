package com.qros.infrastructure.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * EmailService - Manages sending emails in the system.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  @Value("${app.base-url}")
  private String appBaseUrl;

  /**
   * Sends a password reset email to the specified recipient.
   */
  @Async
  public void sendResetPasswordEmail(@NonNull String toEmail, @NonNull String token) {
    String baseUrl = Objects.requireNonNull(appBaseUrl, "app.base-url not configured");

    String resetLink = baseUrl + "/reset-password.html?token=" + token;
    String subject = "Reset password";
    String htmlContent = buildResetPasswordTemplate(resetLink);

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
          Objects.requireNonNull(fromEmail, "spring.mail.username not configured"),
          "Sắc Màu Quán",
          StandardCharsets.UTF_8.name()));
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);

      mailSender.send(message);

      log.info("Email sent successfully to: {}", to);
    } catch (Exception e) {
      log.error("Failed to send email to {}: {}", to, e.getMessage());
    }
  }

  /**
   * Builds the HTML template for the password reset email.
   */
  @NonNull
  private String buildResetPasswordTemplate(String resetLink) {
    String template = """
        <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; line-height: 1.6; max-width: 600px; margin: 0 auto; border: 1px solid #eee; border-radius: 10px; overflow: hidden;">
          <div style="background: #2c7be5; color: #fff; padding: 20px; text-align: center;">
              <h1 style="margin: 0; font-size: 24px;">Sắc Màu Quán</h1>
          </div>
          <div style="padding: 30px;">
              <h2 style="color: #2c7be5;">Hello,</h2>
              <p>We received a request to reset the password for your account.</p>
              <p>Please click the button below to reset your password (valid for 30 minutes):</p>
              <div style="text-align: center; margin: 40px 0;">
                <a href="%s"
                  style="background: #2c7be5; color: #ffffff; padding: 15px 30px;
                          text-decoration: none; border-radius: 8px; font-size: 16px; font-weight: bold; display: inline-block;">
                  RESET PASSWORD
                </a>
              </div>
              <p style="color: #666; font-size: 14px;">If you did not request this, you can safely ignore this email.</p>
          </div>
          <div style="background: #f9f9f9; color: #999; padding: 20px; text-align: center; font-size: 12px; border-top: 1px solid #eee;">
              <p>&copy; 2026 wuchuy</p>
          </div>
        </div>
        """
        .formatted(resetLink);
    return Objects.requireNonNull(template);
  }
}