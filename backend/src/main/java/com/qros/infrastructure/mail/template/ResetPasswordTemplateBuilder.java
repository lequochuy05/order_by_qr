package com.qros.infrastructure.mail.template;

import org.springframework.lang.NonNull;

public final class ResetPasswordTemplateBuilder {

    private ResetPasswordTemplateBuilder() {}

    @NonNull public static String build(String resetLink) {
        return """
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
    }
}
