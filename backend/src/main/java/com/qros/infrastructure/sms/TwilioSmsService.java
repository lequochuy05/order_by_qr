package com.qros.infrastructure.sms;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.restclient.RestTemplateBuilder;
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

    private final SmsProperties properties;
    private final RestTemplate restTemplate;

    public TwilioSmsService(SmsProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        int timeoutSeconds = Math.max(1, properties.getTwilio().getTimeoutSeconds());
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public boolean isAvailable() {
        SmsProperties.Twilio twilio = properties.getTwilio();
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

        SmsProperties.Twilio twilio = properties.getTwilio();
        String url = twilio.getApiBaseUrl() + "/Accounts/" + twilio.getAccountSid() + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilio.getAccountSid(), twilio.getAuthToken());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phone);
        body.add("From", twilio.getFromPhone());
        body.add("Body", properties.getOtpMessageTemplate().formatted(otpCode));

        HttpStatusCode statusCode = restTemplate
                .postForEntity(url, new HttpEntity<>(body, headers), String.class)
                .getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            throw new IllegalStateException("Twilio SMS request failed with status " + statusCode);
        }

        log.info("Password reset OTP SMS sent to {}", com.qros.shared.security.PhoneMasker.mask(phone));
    }
}
