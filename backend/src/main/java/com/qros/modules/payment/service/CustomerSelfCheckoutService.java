package com.qros.modules.payment.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.payment.dto.request.CustomerSelfCheckoutCancelRequest;
import com.qros.modules.payment.dto.request.CustomerSelfCheckoutCreateRequest;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentTransactionResponse;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerSelfCheckoutService {

    private final SystemSettingsService settingsService;
    private final OrderSettlementService orderSettlementService;
    private final PaymentService paymentService;
    private final PaymentPersistenceService persistenceService;

    public PaymentCreateResponse createSelfCheckout(
            @NonNull String tableCode, @NonNull CustomerSelfCheckoutCreateRequest request) {
        requireSelfCheckoutEnabled();
        Order order = orderSettlementService.resolveSessionOrderForSelfCheckout(tableCode, request.sessionToken());

        PaymentCreateRequest paymentRequest = new PaymentCreateRequest(
                order.getId(), PaymentMethod.PAYOS, request.voucherCode(), request.idempotencyKey(), null, null, false);

        return paymentService.createPayment(paymentRequest, null);
    }

    public void cancelSelfCheckout(
            @NonNull String tableCode,
            @NonNull Long transactionId,
            @NonNull CustomerSelfCheckoutCancelRequest request) {
        requireSelfCheckoutEnabled();
        Order order = orderSettlementService.requireActiveSessionOrder(tableCode, request.sessionToken());
        PaymentTransaction tx = persistenceService.loadForGatewayOperation(transactionId);

        if (tx.getOrder() == null || !Objects.equals(tx.getOrder().getId(), order.getId())) {
            throw new BusinessException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND);
        }

        paymentService.cancelPaymentLink(transactionId, request.reason());
    }

    public PaymentTransactionResponse syncSelfCheckoutStatus(
            @NonNull String tableCode, @NonNull Long transactionId, @NonNull String sessionToken) {
        PaymentTransaction tx = persistenceService.loadForGatewayOperation(transactionId);

        if (tx.getStatus() == PaymentTransactionStatus.PAID
                && orderSettlementService.belongsToTableCode(tx.getOrder(), tableCode)) {
            return paymentService.syncPaymentStatus(transactionId);
        }

        requireSelfCheckoutEnabled();
        Order order = orderSettlementService.requireActiveSessionOrder(tableCode, sessionToken);

        if (tx.getOrder() == null || !Objects.equals(tx.getOrder().getId(), order.getId())) {
            throw new BusinessException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND);
        }

        return paymentService.syncPaymentStatus(transactionId);
    }

    private SystemSettings requireSelfCheckoutEnabled() {
        SystemSettings settings = settingsService.getSettingsEntity();
        if (!Boolean.TRUE.equals(settings.getCustomerSelfCheckoutEnabled())) {
            throw new BusinessException(ErrorCode.FEATURE_DISABLED, "Customer self-checkout is currently disabled");
        }
        if (!Boolean.TRUE.equals(settings.getOnlinePaymentEnabled())) {
            throw new BusinessException(ErrorCode.FEATURE_DISABLED, "Online payment is disabled");
        }
        return settings;
    }
}
