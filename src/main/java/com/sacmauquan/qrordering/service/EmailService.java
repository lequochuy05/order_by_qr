package com.sacmauquan.qrordering.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String appBaseUrl;

    public void sendResetPasswordEmail(String toEmail, String token) {
        String resetLink = appBaseUrl + "/reset-password.html?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, "Sắc màu quán", "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu của bạn");

            // Nội dung HTML
            String htmlContent = """
                <div style="font-family: Arial, sans-serif; color: #333; line-height: 1.6;">
                  <h2 style="color: #2c7be5;">Xin chào,</h2>
                  <p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản của mình.</p>
                  <p>Hãy nhấn vào nút bên dưới để đặt lại mật khẩu:</p>
                  <p style="text-align: center; margin: 30px 0;">
                    <a href="%s" 
                       style="background: #2c7be5; color: #fff; padding: 12px 24px; 
                              text-decoration: none; border-radius: 6px; font-size: 16px;">
                      Đặt lại mật khẩu
                    </a>
                  </p>
                  <hr>
                  <p style="font-size: 13px; color: #999;">
                    Link sẽ hết hạn sau 30 phút.<br>
                    Nếu bạn không yêu cầu, hãy bỏ qua email này.
                  </p>
                </div>
                """.formatted(resetLink, resetLink, resetLink);

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }
}

