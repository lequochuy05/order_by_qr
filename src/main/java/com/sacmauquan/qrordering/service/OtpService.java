package com.sacmauquan.qrordering.service;

import org.springframework.stereotype.Service;

@Service
public class OtpService {
    public void sendOtp(String phone, String otp) {
        System.out.println("Gửi OTP " + otp + " đến số điện thoại " + phone);
    }
}