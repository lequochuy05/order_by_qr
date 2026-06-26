package com.qros.modules.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.password-reset")
public class PasswordResetProperties {
    private boolean phoneEnabled = false;
    private boolean devLogOtp = false;
    private Sms sms = new Sms();

    @Getter
    @Setter
    public static class Sms {
        private String otpMessageTemplate = "Your QROS password reset OTP is %s. It expires in 5 minutes.";
        private Twilio twilio = new Twilio();
    }

    @Getter
    @Setter
    public static class Twilio {
        private boolean enabled = false;
        private String accountSid;
        private String authToken;
        private String fromPhone;
        private String apiBaseUrl = "https://api.twilio.com/2010-04-01";
        private int timeoutSeconds = 10;
    }
}
