package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import org.springframework.lang.NonNull;
import vn.payos.model.webhooks.Webhook;

/**
 * PayosService - Quản lý tích hợp cổng thanh toán PayOS.
 */
public interface PayosService {
    PayosCreateResponse createPaymentLink(@NonNull PayosCreateRequest request);
    
    void cancelPaymentLink(@NonNull Long transactionId, String reason);
    
    PaymentTransaction syncPaymentStatus(@NonNull Long transactionId);

    /**
     * Xử lý dữ liệu Webhook từ PayOS (Xác thực chữ ký và cập nhật tài chính)
     */
    void processWebhook(Webhook webhook);
}
