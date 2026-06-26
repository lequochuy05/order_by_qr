package com.qros.infrastructure.sms;

import com.qros.modules.auth.config.PasswordResetProperties;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TwilioSmsService implements SmsService {

    private final PasswordResetProperties properties;
    private final RestTemplate restTemplate;

    public TwilioSmsService(PasswordResetProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        int timeoutSeconds = Math.max(1, properties.getSms().getTwilio().getTimeoutSeconds());
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public boolean isAvailable() {
        PasswordResetProperties.Twilio twilio = properties.getSms().getTwilio();
        return twilio.isEnabled()
                && StringUtils.hasText(twilio.getAccountSid())
                && StringUtils.hasText(twilio.getAuthToken())
                && StringUtils.hasText(twilio.getFromPhone());
    }

    @Override
    public void sendOtp(@NonNull String phone, @NonNull String otpCode) {
        if (!isAvailable()) {
            throw new IllegalStateException("Twilio SMS delivery is not configured");
        }

        PasswordResetProperties.Sms sms = properties.getSms();
        PasswordResetProperties.Twilio twilio = sms.getTwilio();
        String url = twilio.getApiBaseUrl() + "/Accounts/" + twilio.getAccountSid() + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilio.getAccountSid(), twilio.getAuthToken());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phone);
        body.add("From", twilio.getFromPhone());
        body.add("Body", sms.getOtpMessageTemplate().formatted(otpCode));

        HttpStatusCode statusCode = restTemplate
                .postForEntity(url, new HttpEntity<>(body, headers), String.class)
                .getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            throw new IllegalStateException("Twilio SMS request failed with status " + statusCode);
        }

        log.info("Password reset OTP SMS sent to {}", maskPhone(phone));
    }

    private String maskPhone(String phone) {
        String normalized = Objects.toString(phone, "");
        if (normalized.length() < 4) {
            return "****";
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }
}
