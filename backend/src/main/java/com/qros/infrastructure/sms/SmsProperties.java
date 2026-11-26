package com.qros.infrastructure.sms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.password-reset.sms")
public class SmsProperties {

    private String otpMessageTemplate = "Your QROS password reset OTP is %s. It expires in 5 minutes.";
    private Twilio twilio = new Twilio();

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
