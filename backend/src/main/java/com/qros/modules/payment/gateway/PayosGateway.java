package com.qros.modules.payment.gateway;

import com.qros.core.config.AppProperties;
import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@Component
@RequiredArgsConstructor
public class PayosGateway implements PaymentGateway {

    private final PayOS payOS;
    private final AppProperties appProperties;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.PAYOS;
    }

    @Override
    public PaymentGatewayCreateResult createPaymentLink(PaymentTransaction transaction) {
        try {
            String returnUrl = appProperties.getFrontend().getBaseUrl() + "/admin/tables";
            Long orderCode = gatewayOrderCode(transaction);
            long expiredAt = transaction
                    .getExpiresAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toEpochSecond();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
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
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR, "PayOS Gateway error: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelPayment(PaymentTransaction transaction, String reason) {
        try {
            payOS.paymentRequests().cancel(gatewayOrderCode(transaction), reason);
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_CANCELLATION_FAILED, "PayOS Cancellation error: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentGatewayStatusResult getPaymentStatus(PaymentTransaction transaction) {
        try {
            PaymentLink data = payOS.paymentRequests().get(gatewayOrderCode(transaction));
            String remoteStatus = data.getStatus().toString();
            String externalReference = data.getId();

            if ("PAID".equalsIgnoreCase(remoteStatus)) {
                return new PaymentGatewayStatusResult(
                        PaymentTransactionStatus.PAID,
                        externalReference,
                        providerPayload("PAYOS", "SYNC_PAID", externalReference),
                        null);
            }

            if ("CANCELLED".equalsIgnoreCase(remoteStatus)) {
                return new PaymentGatewayStatusResult(
                        PaymentTransactionStatus.CANCELLED,
                        externalReference,
                        providerPayload("PAYOS", "SYNC_CANCELLED", externalReference),
                        "CANCELLED");
            }

            if ("EXPIRED".equalsIgnoreCase(remoteStatus)) {
                return new PaymentGatewayStatusResult(
                        PaymentTransactionStatus.EXPIRED,
                        externalReference,
                        providerPayload("PAYOS", "SYNC_EXPIRED", externalReference),
                        "EXPIRED");
            }

            return new PaymentGatewayStatusResult(
                    PaymentTransactionStatus.PENDING,
                    externalReference,
                    providerPayload("PAYOS", "SYNC_PENDING", externalReference),
                    null);
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_GATEWAY_ERROR, "PayOS status sync failed: " + e.getMessage(), e);
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
            throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID, "Invalid PayOS webhook signature", e);
        }
    }

    private String providerPayload(String provider, String event, String reference) {
        String safeReference = reference != null ? reference.replace("\"", "") : "";
        return "{\"provider\":\"" + provider + "\",\"event\":\"" + event + "\",\"reference\":\"" + safeReference
                + "\"}";
    }

    private Long gatewayOrderCode(PaymentTransaction transaction) {
        return transaction.getGatewayOrderCode() != null ? transaction.getGatewayOrderCode() : transaction.getId();
    }
}
