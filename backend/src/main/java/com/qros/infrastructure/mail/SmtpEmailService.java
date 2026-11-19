package com.qros.infrastructure.mail;

import com.qros.core.config.AppProperties;
import com.qros.infrastructure.mail.template.Admin2faTemplateBuilder;
import com.qros.infrastructure.mail.template.ResetPasswordTemplateBuilder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * SmtpEmailService - Manages sending emails via Brevo REST API or SMTP fallback.
 */
@Service
public class SmtpEmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SmtpEmailService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            AppProperties appProperties,
            ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
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
     * Dispatches HTML email via Brevo REST API if API key is present, otherwise falls back to SMTP.
     */
    private void sendHtmlEmail(@NonNull String to, @NonNull String subject, @NonNull String htmlContent) {
        String brevoKey =
                appProperties.getEmail() != null ? appProperties.getEmail().getBrevoApiKey() : null;
        if (brevoKey != null && !brevoKey.isBlank()) {
            sendViaBrevoApi(to, subject, htmlContent, brevoKey.trim());
            return;
        }
        sendViaSmtp(to, subject, htmlContent);
    }

    private void sendViaBrevoApi(String to, String subject, String htmlContent, String apiKey) {
        try {
            log.info("Sending email via Brevo REST API to: {} with subject: {}", to, subject);

            String senderEmail = mailProperties.getUsername();
            if (senderEmail == null || senderEmail.isBlank()) {
                senderEmail = "no-reply@qros.vn";
            }
            String senderName = appProperties.getEmail().getFromName();

            Map<String, Object> body = Map.of(
                    "sender",
                    Map.of("name", senderName, "email", senderEmail),
                    "to",
                    List.of(Map.of("email", to)),
                    "subject",
                    subject,
                    "htmlContent",
                    htmlContent);

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent successfully via Brevo API to: {}", to);
            } else {
                log.error("Brevo API responded with error status {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException("Brevo API error status: " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email via Brevo API to {}", to, e);
            throw new IllegalStateException("Failed to send email via Brevo API to " + to, e);
        }
    }

    private void sendViaSmtp(String to, String subject, String htmlContent) {
        try {
            log.info("Starting to send email via SMTP to: {} with subject: {}", to, subject);

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

            log.info("Email sent successfully via SMTP to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email via SMTP to {}", to, e);
            throw new IllegalStateException("Failed to send email to " + to, e);
        }
    }
}
