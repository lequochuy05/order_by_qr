package com.qros.infrastructure.sms;

import org.springframework.lang.NonNull;

public interface SmsService {

    boolean isAvailable();

    void sendOtp(@NonNull String phone, @NonNull String otpCode);
}
