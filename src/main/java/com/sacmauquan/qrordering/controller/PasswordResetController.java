package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.PasswordResetRequest;
import com.sacmauquan.qrordering.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

/**
 * PasswordResetController - Xử lý quy trình khôi phục mật khẩu.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService resetService;

    /**
     * Yêu cầu khôi phục mật khẩu qua Email.
     * Luôn trả về thông báo chung để ngăn chặn kẻ xấu dò tìm email có trong hệ
     * thống.
     */
    @PostMapping("/forgot-password-email")
    public ApiResponse<Void> forgotPassword(@RequestParam @NonNull String email) {
        try {
            resetService.createPasswordResetToken(email);
        } catch (Exception e) {
            log.warn("Yêu cầu quên mật khẩu cho email không tồn tại hoặc lỗi: {}", email);
        }
        return ApiResponse.success(
                "Nếu email của bạn tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.", null);
    }

    /**
     * Xác nhận đặt lại mật khẩu bằng Token.
     * Mật khẩu mới được truyền trong Body để đảm bảo an toàn tuyệt đối.
     */
    @PostMapping("/reset-password-email")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody @NonNull PasswordResetRequest req) {
        resetService.resetPassword(req.getToken(), req.getNewPassword());
        return ApiResponse.success("Đặt lại mật khẩu thành công.", null);
    }

    /**
     * Yêu cầu mã OTP khôi phục mật khẩu qua Số điện thoại.
     */
    @PostMapping("/forgot-password-phone")
    public ApiResponse<Void> forgotPasswordPhone(@RequestParam @NonNull String phone) {
        try {
            resetService.createOtpAndSendOtp(phone);
        } catch (Exception e) {
            log.warn("Yêu cầu OTP cho số điện thoại không tồn tại hoặc lỗi: {}", phone);
        }
        return ApiResponse.success("Nếu số điện thoại tồn tại, mã OTP đã được gửi đi.", null);
    }

    /**
     * Xác nhận đặt lại mật khẩu bằng mã OTP.
     */
    @PostMapping("/reset-password-phone")
    public ApiResponse<Void> resetPasswordPhone(@Valid @RequestBody @NonNull PasswordResetRequest req) {
        resetService.resetPasswordWithOtp(req.getPhone(), req.getOtp(), req.getNewPassword());
        return ApiResponse.success("Đặt lại mật khẩu thành công.", null);
    }
}
