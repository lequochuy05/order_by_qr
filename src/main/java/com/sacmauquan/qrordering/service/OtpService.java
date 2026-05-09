package com.sacmauquan.qrordering.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OtpService {
    public void sendOtp(String phone, String otp) {
        System.out.println("Gửi OTP " + otp + " đến số điện thoại " + phone);
    }
}