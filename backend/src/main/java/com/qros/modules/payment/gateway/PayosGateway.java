package com.qros.modules.payment.gateway;

import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.shared.enums.PaymentMethod;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PayosGateway implements PaymentGateway {

    private final PayOS payOS;

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.PAYOS;
    }

    @Override
    public PaymentGatewayCreateResult createPaymentLink(PaymentTransaction transaction) {
        try {
            String returnUrl = frontendUrl + "/admin/table-manager";
            long expiredAt = transaction.getExpiresAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toEpochSecond();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(transaction.getId())
                    .amount(transaction.getAmount().longValue())
                    .description("Payment for Order #" + transaction.getOrder().getId())
                    .returnUrl(returnUrl)
                    .cancelUrl(returnUrl)
                    .expiredAt(expiredAt)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);

            return new PaymentGatewayCreateResult(
                    response.getCheckoutUrl(),
                    response.getQrCode(),
                    response.getPaymentLinkId(),
                    providerPayload("PAYOS", "CREATE_LINK", response.getPaymentLinkId()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "PayOS Gateway error: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelPayment(PaymentTransaction transaction, String reason) {
        try {
            payOS.paymentRequests().cancel(transaction.getId(), reason);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCELLATION_FAILED,
                    "PayOS Cancellation error: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayStatusResult getPaymentStatus(PaymentTransaction transaction) {
        try {
            PaymentLink data = payOS.paymentRequests().get(transaction.getId());
            String remoteStatus = data.getStatus().toString();

            if ("PAID".equalsIgnoreCase(remoteStatus)) {
                return new PaymentGatewayStatusResult(
                        PaymentTransactionStatus.PAID,
                        transaction.getExternalReference(),
                        providerPayload("PAYOS", "SYNC_PAID", transaction.getExternalReference()),
                        null);
            }

            if ("CANCELLED".equalsIgnoreCase(remoteStatus)) {
                return new PaymentGatewayStatusResult(
                        PaymentTransactionStatus.CANCELLED,
                        transaction.getExternalReference(),
                        providerPayload("PAYOS", "SYNC_CANCELLED", transaction.getExternalReference()),
                        "CANCELLED");
            }

            if ("EXPIRED".equalsIgnoreCase(remoteStatus)) {
                return new PaymentGatewayStatusResult(
                        PaymentTransactionStatus.EXPIRED,
                        transaction.getExternalReference(),
                        providerPayload("PAYOS", "SYNC_EXPIRED", transaction.getExternalReference()),
                        "EXPIRED");
            }

            return new PaymentGatewayStatusResult(
                    PaymentTransactionStatus.PENDING,
                    transaction.getExternalReference(),
                    providerPayload("PAYOS", "SYNC_PENDING", transaction.getExternalReference()),
                    null);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "PayOS status sync failed: " + e.getMessage(), e);
        }
    }

    public PaymentWebhookResult verifyWebhook(Webhook webhook) {
        try {
            if ("00".equals(webhook.getCode())
                    && webhook.getData() != null
                    && webhook.getData().getOrderCode() != null
                    && webhook.getData().getOrderCode() == 0) {
                return null;
            }

            WebhookData data = payOS.webhooks().verify(webhook);

            return new PaymentWebhookResult(
                    data.getOrderCode(),
                    BigDecimal.valueOf(data.getAmount()),
                    data.getPaymentLinkId(),
                    providerPayload("PAYOS", "WEBHOOK_PAID", data.getPaymentLinkId()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID,
                    "Invalid PayOS webhook signature", e);
        }
    }

    private String providerPayload(String provider, String event, String reference) {
        String safeReference = reference != null ? reference.replace("\"", "") : "";
        return "{\"provider\":\"" + provider + "\",\"event\":\"" + event + "\",\"reference\":\"" + safeReference
                + "\"}";
    }
}