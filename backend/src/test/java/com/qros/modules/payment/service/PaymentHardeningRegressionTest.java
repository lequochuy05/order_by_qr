package com.qros.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.service.OrderDiscountService;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.gateway.PaymentGateway;
import com.qros.modules.payment.gateway.PaymentGatewayResolver;
import com.qros.modules.payment.mapper.PaymentMapper;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.promotion.service.VoucherService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.event.DomainEvents.PaymentSuccessEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class PaymentHardeningRegressionTest {

    @Test
    void zeroAmountOnlinePaymentSettlesDirectlyWithoutCallingPayosGateway() {
        PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
        OrderSettlementService orderSettlementService = mock(OrderSettlementService.class);
        PaymentCompletionService completionService = mock(PaymentCompletionService.class);
        UserLookupService userLookupService = mock(UserLookupService.class);
        SystemSettingsService settingsService = mock(SystemSettingsService.class);
        VoucherService voucherService = mock(VoucherService.class);
        PaymentGatewayResolver gatewayResolver = mock(PaymentGatewayResolver.class);
        PaymentGatewayCancellationService cancellationService = mock(PaymentGatewayCancellationService.class);

        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .onlinePaymentEnabled(true)
                        .paymentQrExpiresInMinutes(20)
                        .build());

        Order freeOrder = Order.builder()
                .id(55L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(50_000))
                .discountAmount(BigDecimal.valueOf(50_000))
                .finalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .voucherCode("FREE100")
                .businessDate(AppTime.today())
                .build();

        when(transactionRepository.findFirstByIdempotencyKey("idem-free")).thenReturn(Optional.empty());
        when(orderSettlementService.prepareForPayment(55L, "FREE100")).thenReturn(freeOrder);
        when(transactionRepository.findPendingOnlineTransactionsByOrderId(55L)).thenReturn(List.of());
        when(transactionRepository.saveAndFlush(any(PaymentTransaction.class))).thenAnswer(inv -> {
            PaymentTransaction tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(300L);
            }
            return tx;
        });
        when(completionService.completeSuccessfulTransaction(any(PaymentTransaction.class)))
                .thenAnswer(inv -> {
                    PaymentTransaction tx = inv.getArgument(0);
                    tx.setStatus(PaymentTransactionStatus.PAID);
                    tx.setPaidAt(AppTime.now());
                    return freeOrder;
                });

        PaymentPersistenceService persistenceService = new PaymentPersistenceService(
                transactionRepository,
                orderSettlementService,
                completionService,
                userLookupService,
                settingsService,
                voucherService);
        PaymentService paymentService = new PaymentService(
                gatewayResolver,
                new PaymentMapper(),
                persistenceService,
                settingsService,
                new SimpleMeterRegistry(),
                cancellationService);

        PaymentCreateResponse response = paymentService.createPayment(
                new PaymentCreateRequest(55L, PaymentMethod.PAYOS, "FREE100", "idem-free", null, null, false),
                null,
                "/T01/menu");

        assertThat(response.status()).isEqualTo(PaymentTransactionStatus.PAID);
        assertThat(response.amount()).isEqualByComparingTo("0");
        verify(completionService).completeSuccessfulTransaction(any(PaymentTransaction.class));
        verifyNoInteractions(gatewayResolver);
    }

    @Test
    void lateWebhookOnCancelledOrderMarksTransactionPaidWithManualRefundAuditWithoutThrowing() {
        PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
        OrderSettlementService orderSettlementService = mock(OrderSettlementService.class);
        OrderDiscountService orderDiscountService = mock(OrderDiscountService.class);
        VoucherService voucherService = mock(VoucherService.class);
        VoucherCheckoutService voucherCheckoutService = mock(VoucherCheckoutService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        PaymentCompletionService completionService = new PaymentCompletionService(
                transactionRepository,
                orderSettlementService,
                orderDiscountService,
                voucherService,
                voucherCheckoutService,
                eventPublisher,
                new SimpleMeterRegistry());

        Order cancelledOrder = Order.builder()
                .id(70L)
                .status(OrderStatus.CANCELLED)
                .paymentStatus(PaymentStatus.PENDING)
                .finalAmount(BigDecimal.valueOf(100_000))
                .build();
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(700L)
                .order(cancelledOrder)
                .amount(BigDecimal.valueOf(100_000))
                .paymentMethod(PaymentMethod.PAYOS)
                .status(PaymentTransactionStatus.CANCELLED)
                .voucherUsageReserved(true)
                .voucherCodeSnapshot("SAVE10")
                .build();

        when(orderSettlementService.loadForPayment(70L)).thenReturn(cancelledOrder);
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        completionService.completeSuccessfulTransaction(tx);

        assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.PAID);
        assertThat(tx.getFailureReason()).containsIgnoringCase("manual refund");
        verify(voucherService).releaseReservedUsageByCode("SAVE10");
        verify(orderSettlementService, never()).settleAfterPayment(any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(PaymentSuccessEvent.class));
    }

    @Test
    void partialWebhookAmountRejectsSettlementWithoutMarkingPaid() {
        PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
        OrderSettlementService orderSettlementService = mock(OrderSettlementService.class);
        PaymentCompletionService completionService = mock(PaymentCompletionService.class);
        UserLookupService userLookupService = mock(UserLookupService.class);
        SystemSettingsService settingsService = mock(SystemSettingsService.class);
        VoucherService voucherService = mock(VoucherService.class);

        PaymentPersistenceService persistenceService = new PaymentPersistenceService(
                transactionRepository,
                orderSettlementService,
                completionService,
                userLookupService,
                settingsService,
                voucherService);

        Order order = Order.builder()
                .id(80L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .finalAmount(BigDecimal.valueOf(100_000))
                .build();
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(800L)
                .gatewayOrderCode(123456789L)
                .order(order)
                .amount(BigDecimal.valueOf(100_000))
                .paymentMethod(PaymentMethod.PAYOS)
                .status(PaymentTransactionStatus.PENDING)
                .build();

        when(transactionRepository.findWithOrderByGatewayOrderCodeForUpdate(123456789L))
                .thenReturn(Optional.of(tx));
        when(orderSettlementService.loadForPayment(80L)).thenReturn(order);

        assertThatThrownBy(() -> persistenceService.confirmWebhook(
                        new PaymentWebhookResult(123456789L, BigDecimal.valueOf(50_000), "ref-partial", "{}")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_WEBHOOK_INVALID));

        assertThat(tx.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
        verify(completionService, never()).completeSuccessfulTransaction(any());
    }

    @Test
    void expiredOrphanedCreatingTransactionIsExpiredWhenGatewayLookupFails() {
        PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
        OrderSettlementService orderSettlementService = mock(OrderSettlementService.class);
        PaymentCompletionService completionService = mock(PaymentCompletionService.class);
        UserLookupService userLookupService = mock(UserLookupService.class);
        SystemSettingsService settingsService = mock(SystemSettingsService.class);
        VoucherService voucherService = mock(VoucherService.class);
        PaymentGatewayResolver gatewayResolver = mock(PaymentGatewayResolver.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentGatewayCancellationService cancellationService = mock(PaymentGatewayCancellationService.class);

        Order order = Order.builder()
                .id(90L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .finalAmount(BigDecimal.valueOf(100_000))
                .build();
        PaymentTransaction orphanTx = PaymentTransaction.builder()
                .id(900L)
                .gatewayOrderCode(987654321L)
                .order(order)
                .amount(BigDecimal.valueOf(100_000))
                .paymentMethod(PaymentMethod.PAYOS)
                .status(PaymentTransactionStatus.CREATING)
                .expiresAt(AppTime.now().minusMinutes(5))
                .voucherUsageReserved(true)
                .voucherCodeSnapshot("ORPHAN10")
                .build();

        when(transactionRepository.findWithOrderById(900L)).thenReturn(Optional.of(orphanTx));
        when(transactionRepository.findWithOrderByIdForUpdate(900L)).thenReturn(Optional.of(orphanTx));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gatewayResolver.resolve(PaymentMethod.PAYOS)).thenReturn(gateway);
        when(gateway.getPaymentStatus(orphanTx))
                .thenThrow(new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR, "PayOS 404 not found"));

        PaymentPersistenceService persistenceService = new PaymentPersistenceService(
                transactionRepository,
                orderSettlementService,
                completionService,
                userLookupService,
                settingsService,
                voucherService);
        PaymentService paymentService = new PaymentService(
                gatewayResolver,
                new PaymentMapper(),
                persistenceService,
                settingsService,
                new SimpleMeterRegistry(),
                cancellationService);

        var response = paymentService.syncPaymentStatus(900L);

        assertThat(response.status()).isEqualTo(PaymentTransactionStatus.EXPIRED);
        assertThat(orphanTx.getStatus()).isEqualTo(PaymentTransactionStatus.EXPIRED);
        verify(voucherService).releaseReservedUsageByCode("ORPHAN10");
    }
}
