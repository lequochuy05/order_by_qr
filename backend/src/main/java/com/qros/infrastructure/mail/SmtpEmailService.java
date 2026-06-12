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

import com.qros.infrastructure.mail.template.ResetPasswordTemplateBuilder;

/**
 * SmtpEmailService - Manages sending emails in the system.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService {
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
    String htmlContent = ResetPasswordTemplateBuilder.build(resetLink);

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
}