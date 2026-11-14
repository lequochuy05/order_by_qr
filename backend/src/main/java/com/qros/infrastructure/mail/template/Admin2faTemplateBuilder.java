package com.qros.infrastructure.mail.template;

public final class Admin2faTemplateBuilder {

    private Admin2faTemplateBuilder() {}

    public static String build(String otp, String brandName, String copyright) {
        return """
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #1e293b; line-height: 1.6; max-width: 540px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background-color: #ffffff;">
              <div style="background: linear-gradient(135deg, #ea580c 0%%, #f97316 100%%); color: #ffffff; padding: 24px; text-align: center;">
                  <h1 style="margin: 0; font-size: 22px; font-weight: 800; letter-spacing: -0.5px;">%s</h1>
                  <p style="margin: 6px 0 0 0; font-size: 13px; opacity: 0.9;">Xác thực bảo mật tài khoản Quản trị</p>
              </div>
              <div style="padding: 32px 28px;">
                  <h2 style="color: #0f172a; font-size: 18px; margin-top: 0; font-weight: 700;">Mã xác thực đăng nhập (2FA)</h2>
                  <p style="color: #475569; font-size: 14px; margin-bottom: 24px;">Hệ thống nhận được yêu cầu đăng nhập tài khoản Quản trị viên. Vui lòng nhập mã bảo mật bên dưới để hoàn tất:</p>

                  <div style="text-align: center; margin: 28px 0; background: #fff7ed; border: 1px dashed #fdba74; border-radius: 10px; padding: 20px;">
                    <span style="font-family: 'SF Mono', Consolas, Monaco, monospace; font-size: 36px; font-weight: 900; letter-spacing: 10px; color: #ea580c; display: inline-block;">
                      %s
                    </span>
                    <p style="margin: 8px 0 0 0; font-size: 12px; color: #9a3412;">Mã này có hiệu lực trong vòng <strong>5 phút</strong></p>
                  </div>

                  <div style="border-left: 3px solid #f97316; padding-left: 12px; margin-top: 24px;">
                    <p style="color: #64748b; font-size: 12px; margin: 0;">Nếu bạn không thực hiện đăng nhập này, tài khoản của bạn có thể đang bị xâm nhập. Vui lòng đổi mật khẩu ngay lập tức.</p>
                  </div>
              </div>
              <div style="background: #f8fafc; color: #94a3b8; padding: 16px 20px; text-align: center; font-size: 11px; border-top: 1px solid #e2e8f0;">
                  <p style="margin: 0;">&copy; %s. Tất cả các quyền được bảo lưu.</p>
              </div>
            </div>
            """
                .formatted(brandName, otp, copyright);
    }
}
