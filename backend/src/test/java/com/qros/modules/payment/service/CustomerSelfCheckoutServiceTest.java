package com.qros.modules.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerSelfCheckoutServiceTest {

    @Mock
    private SystemSettingsService settingsService;

    @Mock
    private OrderSettlementService orderSettlementService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentPersistenceService persistenceService;

    private CustomerSelfCheckoutService selfCheckoutService;

    @BeforeEach
    void setUp() {
        selfCheckoutService = new CustomerSelfCheckoutService(
                settingsService, orderSettlementService, paymentService, persistenceService);
    }

    @Test
    void createSelfCheckoutThrowsWhenToggleDisabled() {
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .customerSelfCheckoutEnabled(false)
                        .onlinePaymentEnabled(true)
                        .build());

        CustomerSelfCheckoutCreateRequest request = new CustomerSelfCheckoutCreateRequest("tok-1", null, "idem-1");

        assertThatThrownBy(() -> selfCheckoutService.createSelfCheckout("T01", request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FEATURE_DISABLED));

        verifyNoInteractions(orderSettlementService, paymentService);
    }

    @Test
    void createSelfCheckoutDelegatesPayosPaymentWhenEnabled() {
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .customerSelfCheckoutEnabled(true)
                        .onlinePaymentEnabled(true)
                        .allowSettlementWithUnfinishedItems(true)
                        .build());

        Order order = Order.builder().id(55L).build();
        when(orderSettlementService.resolveSessionOrderForSelfCheckout("T01", "tok-1"))
                .thenReturn(order);

        PaymentCreateResponse expected = new PaymentCreateResponse(
                99L,
                55L,
                PaymentMethod.PAYOS,
                PaymentTransactionStatus.PENDING,
                "https://pay.os/99",
                "vietqr-data",
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(20),
                BigDecimal.valueOf(90_000),
                BigDecimal.valueOf(100_000),
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(90_000),
                "SAVE10",
                "idem-1",
                "ref-99");
        when(paymentService.createPayment(any(PaymentCreateRequest.class), isNull()))
                .thenReturn(expected);

        PaymentCreateResponse result = selfCheckoutService.createSelfCheckout(
                "T01", new CustomerSelfCheckoutCreateRequest("tok-1", "SAVE10", "idem-1"));

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<PaymentCreateRequest> captor = ArgumentCaptor.forClass(PaymentCreateRequest.class);
        verify(paymentService).createPayment(captor.capture(), isNull());

        PaymentCreateRequest sent = captor.getValue();
        assertThat(sent.orderId()).isEqualTo(55L);
        assertThat(sent.paymentMethod()).isEqualTo(PaymentMethod.PAYOS);
        assertThat(sent.voucherCode()).isEqualTo("SAVE10");
        assertThat(sent.idempotencyKey()).isEqualTo("idem-1");
        assertThat(sent.confirmUnfinishedItems()).isFalse();
    }

    @Test
    void cancelSelfCheckoutRejectsTransactionFromDifferentOrder() {
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .customerSelfCheckoutEnabled(true)
                        .onlinePaymentEnabled(true)
                        .build());
        when(orderSettlementService.requireActiveSessionOrder("T01", "tok-1"))
                .thenReturn(Order.builder().id(55L).build());
        when(persistenceService.loadForGatewayOperation(77L))
                .thenReturn(PaymentTransaction.builder()
                        .id(77L)
                        .order(Order.builder().id(88L).build())
                        .build());

        CustomerSelfCheckoutCancelRequest request = new CustomerSelfCheckoutCancelRequest("tok-1", "Change voucher");

        assertThatThrownBy(() -> selfCheckoutService.cancelSelfCheckout("T01", 77L, request))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        verifyNoInteractions(paymentService);
    }

    @Test
    void cancelSelfCheckoutCancelsMatchingOrderTransaction() {
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .customerSelfCheckoutEnabled(true)
                        .onlinePaymentEnabled(true)
                        .build());
        when(orderSettlementService.requireActiveSessionOrder("T01", "tok-1"))
                .thenReturn(Order.builder().id(55L).build());
        when(persistenceService.loadForGatewayOperation(77L))
                .thenReturn(PaymentTransaction.builder()
                        .id(77L)
                        .order(Order.builder().id(55L).build())
                        .build());

        selfCheckoutService.cancelSelfCheckout(
                "T01", 77L, new CustomerSelfCheckoutCancelRequest("tok-1", "Change voucher"));

        verify(paymentService).cancelPaymentLink(eq(77L), eq("Change voucher"));
    }

    @Test
    void syncSelfCheckoutStatusReturnsPaidTransactionForMatchingTableAfterSessionClose() {
        Order order = Order.builder().id(55L).build();
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(77L)
                .order(order)
                .status(PaymentTransactionStatus.PAID)
                .build();
        PaymentTransactionResponse response = new PaymentTransactionResponse(
                77L,
                55L,
                BigDecimal.valueOf(100_000),
                PaymentMethod.PAYOS,
                PaymentTransactionStatus.PAID,
                null,
                null,
                "ref-77",
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(20),
                LocalDateTime.now(),
                null,
                null);

        when(persistenceService.loadForGatewayOperation(77L)).thenReturn(tx);
        when(orderSettlementService.belongsToTableCode(order, "T01")).thenReturn(true);
        when(paymentService.syncPaymentStatus(77L)).thenReturn(response);

        PaymentTransactionResponse result = selfCheckoutService.syncSelfCheckoutStatus("T01", 77L, "revoked-token");

        assertThat(result.status()).isEqualTo(PaymentTransactionStatus.PAID);
        verifyNoInteractions(settingsService);
    }
}
