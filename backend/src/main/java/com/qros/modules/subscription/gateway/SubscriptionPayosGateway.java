package com.qros.modules.subscription.gateway;

import com.qros.modules.subscription.model.SubscriptionInvoice;
import com.qros.modules.subscription.model.SubscriptionInvoiceStatus;
import com.qros.shared.config.AppProperties;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionPayosGateway {

    private final PayOS payOS;
    private final AppProperties appProperties;

    public record GatewayLinkResult(String checkoutUrl, String qrCode, String paymentLinkId, String providerPayload) {}

    public record GatewayStatusResult(
            SubscriptionInvoiceStatus status, String externalReference, String providerPayload) {}

    public GatewayLinkResult createPaymentLink(SubscriptionInvoice invoice, String returnPath) {
        try {
            String baseUrl = normalizeBaseUrl(appProperties.getFrontend().getBaseUrl());
            String returnUrl = baseUrl + resolveReturnPath(returnPath);
            long expiredAt = invoice.getExpiresAt().atZone(AppTime.ZONE).toEpochSecond();
            String description = "QROS PRO " + invoice.getBillingCycle().name();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(invoice.getGatewayOrderCode())
                    .amount(invoice.getAmount().longValue())
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(returnUrl)
                    .expiredAt(expiredAt)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            return new GatewayLinkResult(
                    response.getCheckoutUrl(),
                    response.getQrCode(),
                    response.getPaymentLinkId(),
                    providerPayload("CREATE_LINK", response.getPaymentLinkId()));
        } catch (Exception e) {
            log.error(
                    "PayOS createPaymentLink failed for subscription invoice {}: {}",
                    invoice.getId(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    ErrorCode.PAYMENT_GATEWAY_ERROR, "Unable to create PayOS subscription payment link", e);
        }
    }

    public GatewayStatusResult getPaymentStatus(SubscriptionInvoice invoice) {
        try {
            PaymentLink data = payOS.paymentRequests().get(invoice.getGatewayOrderCode());
            String remoteStatus = data.getStatus() != null ? data.getStatus().toString() : "";
            String reference = data.getId();

            if ("PAID".equalsIgnoreCase(remoteStatus)) {
                return new GatewayStatusResult(
                        SubscriptionInvoiceStatus.PAID, reference, providerPayload("SYNC_PAID", reference));
            }
            if ("CANCELLED".equalsIgnoreCase(remoteStatus)) {
                return new GatewayStatusResult(
                        SubscriptionInvoiceStatus.CANCELLED, reference, providerPayload("SYNC_CANCELLED", reference));
            }
            if ("EXPIRED".equalsIgnoreCase(remoteStatus)) {
                return new GatewayStatusResult(
                        SubscriptionInvoiceStatus.EXPIRED, reference, providerPayload("SYNC_EXPIRED", reference));
            }
            return new GatewayStatusResult(
                    SubscriptionInvoiceStatus.PENDING, reference, providerPayload("SYNC_PENDING", reference));
        } catch (Exception e) {
            log.error(
                    "PayOS getPaymentStatus failed for subscription invoice {}: {}",
                    invoice.getId(),
                    e.getMessage(),
                    e);
            throw new BusinessException(
                    ErrorCode.PAYMENT_GATEWAY_ERROR, "Unable to sync PayOS subscription payment status", e);
        }
    }

    public void cancelPayment(SubscriptionInvoice invoice, String reason) {
        try {
            payOS.paymentRequests().cancel(invoice.getGatewayOrderCode(), reason);
        } catch (Exception e) {
            log.warn("PayOS cancelPayment failed for subscription invoice {}: {}", invoice.getId(), e.getMessage());
        }
    }

    private String providerPayload(String event, String reference) {
        String safeRef = reference != null ? reference.replace("\"", "") : "";
        return "{\"provider\":\"PAYOS\",\"scope\":\"SUBSCRIPTION\",\"event\":\"" + event + "\",\"reference\":\""
                + safeRef + "\"}";
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

    private String resolveReturnPath(String requestedPath) {
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
        return "/admin/billing";
    }
}
