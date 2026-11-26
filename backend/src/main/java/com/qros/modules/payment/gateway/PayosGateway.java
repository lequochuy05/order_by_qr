package com.qros.modules.payment.gateway;

import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.shared.config.AppProperties;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@Slf4j
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
        return createPaymentLink(transaction, null);
    }

    @Override
    public PaymentGatewayCreateResult createPaymentLink(PaymentTransaction transaction, String returnPath) {
        try {
            String baseUrl = normalizeBaseUrl(appProperties.getFrontend().getBaseUrl());
            String returnUrl = baseUrl + resolveReturnPath(returnPath, transaction);
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
            log.error("PayOS createPaymentLink failed for transaction {}: {}", transaction.getId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR, "Unable to create PayOS payment link", e);
        }
    }

    @Override
    public void cancelPayment(PaymentTransaction transaction, String reason) {
        try {
            payOS.paymentRequests().cancel(gatewayOrderCode(transaction), reason);
        } catch (Exception e) {
            log.error("PayOS cancelPayment failed for transaction {}: {}", transaction.getId(), e.getMessage(), e);
            throw new BusinessException(
                    ErrorCode.PAYMENT_CANCELLATION_FAILED, "Unable to cancel PayOS payment link", e);
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
            log.error("PayOS getPaymentStatus failed for transaction {}: {}", transaction.getId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR, "Unable to sync PayOS payment status", e);
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

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String resolveReturnPath(String requestedPath, PaymentTransaction transaction) {
        if (requestedPath != null && !requestedPath.isBlank()) {
            String trimmed = requestedPath.trim();
            if (trimmed.startsWith("/")
                    && !trimmed.startsWith("//")
                    && !trimmed.contains("..")
                    && !trimmed.contains("\r")
                    && !trimmed.contains("\n")) {
                return trimmed;
            }
        }
        if (transaction != null
                && transaction.getOrder() != null
                && transaction.getOrder().isTakeaway()) {
            return "/admin/takeaway";
        }
        return "/admin/tables";
    }
}
