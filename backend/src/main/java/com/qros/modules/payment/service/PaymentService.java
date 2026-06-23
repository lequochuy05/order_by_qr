package com.qros.modules.payment.service;

import com.qros.modules.payment.dto.internal.OnlinePaymentPreparation;
import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentGatewayStatusResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.dto.response.PaymentTransactionResponse;
import com.qros.modules.payment.gateway.PaymentGateway;
import com.qros.modules.payment.gateway.PaymentGatewayResolver;
import com.qros.modules.payment.mapper.PaymentMapper;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGatewayResolver gatewayResolver;
    private final PaymentMapper paymentMapper;
    private final PaymentPersistenceService persistenceService;
    private final SystemSettingsService settingsService;
    private final MeterRegistry meterRegistry;

    public PaymentCreateResponse createPayment(@NonNull PaymentCreateRequest request, Long userId) {
        validatePaymentMethodEnabled(request.paymentMethod());

        if (request.paymentMethod() == PaymentMethod.CASH) {
            try {
                return paymentMapper.toCreateResponse(persistenceService.settleCash(request, userId));
            } catch (DataIntegrityViolationException exception) {
                return paymentMapper.toCreateResponse(persistenceService.recoverIdempotency(request));
            }
        }

        OnlinePaymentPreparation preparation;
        try {
            preparation = persistenceService.prepareOnline(request);
        } catch (DataIntegrityViolationException exception) {
            return paymentMapper.toCreateResponse(persistenceService.recoverIdempotency(request));
        }

        if (!preparation.gatewayCallRequired()) {
            return paymentMapper.toCreateResponse(preparation.transaction());
        }

        PaymentTransaction transaction = preparation.transaction();
        PaymentGateway gateway = gatewayResolver.resolve(request.paymentMethod());
        try {
            PaymentGatewayCreateResult result =
                    recordGatewayCall("create", request.paymentMethod(), () -> gateway.createPaymentLink(transaction));
            return paymentMapper.toCreateResponse(
                    persistenceService.completeOnlineCreation(transaction.getId(), result));
        } catch (RuntimeException exception) {
            persistenceService.markOnlineCreationFailed(transaction.getId(), exception.getMessage());
            throw exception;
        }
    }

    public void cancelPaymentLink(@NonNull Long transactionId, String reason) {
        PaymentTransaction transaction = persistenceService.loadForGatewayOperation(transactionId);
        if (!isActiveOnline(transaction)) {
            throw new BusinessException(
                    ErrorCode.PAYMENT_TRANSACTION_INVALID_STATE, "Only active online transactions can be cancelled");
        }

        String normalizedReason = normalizeCancelReason(reason);
        PaymentGateway gateway = gatewayResolver.resolve(transaction.getPaymentMethod());
        recordGatewayCall("cancel", transaction.getPaymentMethod(), () -> {
            gateway.cancelPayment(transaction, normalizedReason);
            return null;
        });
        persistenceService.markCancelled(transactionId, normalizedReason);
    }

    public PaymentTransactionResponse syncPaymentStatus(@NonNull Long transactionId) {
        PaymentTransaction transaction = persistenceService.loadForGatewayOperation(transactionId);
        if (!isActiveOnline(transaction)) {
            return paymentMapper.toTransactionResponse(transaction);
        }

        PaymentGateway gateway = gatewayResolver.resolve(transaction.getPaymentMethod());
        PaymentGatewayStatusResult result = recordGatewayCall(
                "status", transaction.getPaymentMethod(), () -> gateway.getPaymentStatus(transaction));
        return paymentMapper.toTransactionResponse(persistenceService.applyGatewayStatus(transactionId, result));
    }

    public void confirmPaymentFromWebhook(PaymentWebhookResult webhookResult) {
        persistenceService.confirmWebhook(webhookResult);
    }

    private void validatePaymentMethodEnabled(PaymentMethod paymentMethod) {
        SystemSettings settings = settingsService.getSettingsEntity();
        boolean enabled = paymentMethod == PaymentMethod.CASH
                ? Boolean.TRUE.equals(settings.getCashPaymentEnabled())
                : Boolean.TRUE.equals(settings.getOnlinePaymentEnabled());

        if (!enabled) {
            throw new BusinessException(
                    ErrorCode.FEATURE_DISABLED,
                    paymentMethod == PaymentMethod.CASH ? "Cash payment is disabled" : "Online payment is disabled");
        }
    }

    private boolean isActiveOnline(PaymentTransaction transaction) {
        return transaction.getStatus() == PaymentTransactionStatus.CREATING
                || transaction.getStatus() == PaymentTransactionStatus.PENDING;
    }

    private String normalizeCancelReason(String reason) {
        return reason == null || reason.isBlank() ? "Customer changed payment method" : reason.trim();
    }

    private <T> T recordGatewayCall(String operation, PaymentMethod method, java.util.function.Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return action.get();
        } finally {
            sample.stop(Timer.builder("payment.gateway.latency")
                    .tag("operation", operation)
                    .tag("method", Objects.toString(method, "unknown"))
                    .register(meterRegistry));
        }
    }
}
