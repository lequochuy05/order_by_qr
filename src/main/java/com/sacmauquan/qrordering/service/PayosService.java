package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import org.springframework.lang.NonNull;
import vn.payos.model.webhooks.Webhook;

/**
 * PayosService - Manages PayOS payment gateway integration.
 */
public interface PayosService {
    PayosCreateResponse createPaymentLink(@NonNull PayosCreateRequest request);

    void cancelPaymentLink(@NonNull Long transactionId, String reason);

    PaymentTransaction syncPaymentStatus(@NonNull Long transactionId);

    /**
     * Process Webhook data from PayOS (Verify signature and update financial)
     */
    void processWebhook(Webhook webhook);
}
