package com.qros.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.payment.dto.internal.PaymentGatewayCreateResult;
import com.qros.modules.payment.dto.internal.PaymentWebhookResult;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.dto.response.PaymentCreateResponse;
import com.qros.modules.payment.gateway.PaymentGateway;
import com.qros.modules.payment.gateway.PaymentGatewayResolver;
import com.qros.modules.payment.mapper.PaymentMapper;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.payment.service.PaymentCompletionService;
import com.qros.modules.payment.service.PaymentService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayosServiceImplTest {

    @Mock
    OrderSettlementService orderSettlementService;

    @Mock
    PaymentTransactionRepository transactionRepository;

    @Mock
    PaymentGatewayResolver gatewayResolver;

    @Mock
    PaymentGateway gateway;

    @Mock
    PaymentCompletionService paymentCompletionService;

    @Mock
    TransactionSideEffectService sideEffects;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                transactionRepository,
                gatewayResolver,
                new PaymentMapper(),
                paymentCompletionService,
                orderSettlementService,
                sideEffects);
    }

    @Test
    void createPaymentLinkUsesOrderFinalAmountInsteadOfClientAmount() {
        Order order = payableOrder();
        PaymentCreateRequest request = new PaymentCreateRequest(
                1L,
                PaymentMethod.PAYOS,
                null,
                "idem-1");

        when(orderSettlementService.prepareForOnlinePayment(1L, null)).thenReturn(order);
        when(gatewayResolver.resolve(PaymentMethod.PAYOS)).thenReturn(gateway);
        when(transactionRepository.findFirstByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(transactionRepository.findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                1L,
                PaymentMethod.PAYOS,
                PaymentTransactionStatus.PENDING)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(99L);
            }
            return transaction;
        });
        when(gateway.createPaymentLink(any(PaymentTransaction.class)))
                .thenReturn(new PaymentGatewayCreateResult("checkout", "qr", "payos-ref", "{}"));

        PaymentCreateResponse response = paymentService.createPaymentLink(request);

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository, org.mockito.Mockito.atLeastOnce()).save(transactionCaptor.capture());

        PaymentTransaction firstSaved = transactionCaptor.getAllValues().get(0);
        assertThat(firstSaved.getAmount()).isEqualByComparingTo("125000");
        assertThat(firstSaved.getPaymentMethod()).isEqualTo(PaymentMethod.PAYOS);
        assertThat(firstSaved.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
        assertThat(firstSaved.getBusinessDate()).isEqualTo(AppTime.today());

        assertThat(response.transactionId()).isEqualTo(99L);
        assertThat(response.amount()).isEqualByComparingTo("125000");
        assertThat(response.externalReference()).isEqualTo("payos-ref");
    }

    @Test
    void createPaymentLinkReusesMatchingIdempotentTransaction() {
        Order order = payableOrder();
        PaymentTransaction existing = PaymentTransaction.builder()
                .id(88L)
                .order(order)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.PAYOS)
                .checkoutUrl("checkout")
                .qrCode("qr")
                .idempotencyKey("idem-1")
                .build();

        when(orderSettlementService.prepareForOnlinePayment(1L, null)).thenReturn(order);
        when(gatewayResolver.resolve(PaymentMethod.PAYOS)).thenReturn(gateway);
        when(transactionRepository.findFirstByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        PaymentCreateResponse response = paymentService.createPaymentLink(new PaymentCreateRequest(
                1L,
                PaymentMethod.PAYOS,
                null,
                " idem-1 "));

        assertThat(response.transactionId()).isEqualTo(88L);
        assertThat(response.idempotencyKey()).isEqualTo("idem-1");
        verify(transactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void confirmPaymentFromWebhookLocksOrderBeforeTransaction() {
        Order orderReference = Order.builder()
                .id(1L)
                .build();
        Order lockedOrder = payableOrder();
        PaymentTransaction existing = PaymentTransaction.builder()
                .id(99L)
                .order(orderReference)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.PAYOS)
                .build();
        PaymentTransaction lockedTransaction = PaymentTransaction.builder()
                .id(99L)
                .order(orderReference)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.PAYOS)
                .build();

        when(transactionRepository.findWithOrderById(99L)).thenReturn(Optional.of(existing));
        when(orderSettlementService.loadForPayment(1L)).thenReturn(lockedOrder);
        when(transactionRepository.findWithOrderByIdForUpdate(99L)).thenReturn(Optional.of(lockedTransaction));

        paymentService.confirmPaymentFromWebhook(new PaymentWebhookResult(
                99L,
                BigDecimal.valueOf(125_000),
                "payos-ref",
                "{}"));

        var ordered = inOrder(transactionRepository, orderSettlementService, paymentCompletionService);
        ordered.verify(transactionRepository).findWithOrderById(99L);
        ordered.verify(orderSettlementService).loadForPayment(1L);
        ordered.verify(transactionRepository).findWithOrderByIdForUpdate(99L);
        ordered.verify(paymentCompletionService).completeSuccessfulTransaction(lockedTransaction);

        assertThat(lockedTransaction.getOrder()).isSameAs(lockedOrder);
        assertThat(lockedTransaction.getExternalReference()).isEqualTo("payos-ref");
        assertThat(lockedTransaction.getProviderPayload()).isEqualTo("{}");
    }

    @Test
    void confirmPaymentFromWebhookIsIdempotentWhenAlreadyPaid() {
        Order orderReference = Order.builder()
                .id(1L)
                .build();
        PaymentTransaction existing = PaymentTransaction.builder()
                .id(99L)
                .order(orderReference)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PAID)
                .paymentMethod(PaymentMethod.PAYOS)
                .build();

        when(transactionRepository.findWithOrderById(99L)).thenReturn(Optional.of(existing));
        when(orderSettlementService.loadForPayment(1L)).thenReturn(payableOrder());
        when(transactionRepository.findWithOrderByIdForUpdate(99L)).thenReturn(Optional.of(existing));

        paymentService.confirmPaymentFromWebhook(new PaymentWebhookResult(
                99L,
                BigDecimal.valueOf(125_000),
                "payos-ref",
                "{}"));

        verify(paymentCompletionService, never()).completeSuccessfulTransaction(any(PaymentTransaction.class));
    }

    private Order payableOrder() {
        return Order.builder()
                .id(1L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(125_000))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.valueOf(125_000))
                .paidAmount(BigDecimal.ZERO)
                .businessDate(AppTime.today())
                .build();
    }
}
