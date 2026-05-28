package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import org.springframework.lang.NonNull;
import vn.payos.model.webhooks.Webhook;

/**
 * PayosService - Interface for managing PayOS payment gateway integration.
 * Handles payment link creation, cancellation, status synchronization, and webhook processing.
 */
public interface PayosService {
    
    /**
     * Generates a unique payment link for an order via PayOS.
     * 
     * @param request Payment creation details
     * @return Response containing the checkout URL and QR code
     */
    PayosCreateResponse createPaymentLink(@NonNull PayosCreateRequest request);

    /**
     * Cancels an existing pending payment link on PayOS.
     * 
     * @param transactionId System transaction identifier
     * @param reason Reason for cancellation
     */
    void cancelPaymentLink(@NonNull Long transactionId, String reason);

    /**
     * Synchronizes the local transaction status with the remote PayOS status.
     * 
     * @param transactionId System transaction identifier
     * @return Updated PaymentTransaction entity
     */
    PaymentTransaction syncPaymentStatus(@NonNull Long transactionId);

    /**
     * Processes incoming webhook notifications from PayOS to confirm payment success.
     * Includes signature verification and financial record updates.
     * 
     * @param webhook The raw webhook data from PayOS
     */
    void processWebhook(Webhook webhook);
}
