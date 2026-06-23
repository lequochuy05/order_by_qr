package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.qros.modules.payment.service.PaymentPersistenceService;
import com.qros.modules.payment.service.PaymentService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    UserRepository userRepository;

    @Mock
    SystemSettingsService settingsService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentPersistenceService persistenceService = new PaymentPersistenceService(
                transactionRepository,
                orderSettlementService,
                paymentCompletionService,
                userRepository,
                settingsService);
        paymentService = new PaymentService(
                gatewayResolver, new PaymentMapper(), persistenceService, settingsService, new SimpleMeterRegistry());
        lenient()
                .when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .cashPaymentEnabled(true)
                        .onlinePaymentEnabled(true)
                        .paymentQrExpiresInMinutes(20)
                        .build());
    }

    @Test
    void createPaymentLinkUsesOrderFinalAmountInsteadOfClientAmount() {
        Order order = payableOrder();
        PaymentCreateRequest request = new PaymentCreateRequest(1L, PaymentMethod.PAYOS, null, "idem-1");

        when(orderSettlementService.prepareForPayment(1L, null)).thenReturn(order);
        when(gatewayResolver.resolve(PaymentMethod.PAYOS)).thenReturn(gateway);
        when(transactionRepository.findFirstByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(transactionRepository.findFirstByOrderIdAndPaymentMethodAndStatusInOrderByCreatedAtDesc(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(PaymentMethod.PAYOS),
                        any()))
                .thenReturn(Optional.empty());
        AtomicReference<PaymentTransaction> transactionReference = new AtomicReference<>();
        AtomicReference<PaymentTransactionStatus> initialStatus = new AtomicReference<>();
        when(transactionRepository.saveAndFlush(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(99L);
            }
            initialStatus.set(transaction.getStatus());
            transactionReference.set(transaction);
            return transaction;
        });
        when(transactionRepository.findWithOrderByIdForUpdate(99L))
                .thenAnswer(invocation -> Optional.of(transactionReference.get()));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(gateway.createPaymentLink(any(PaymentTransaction.class)))
                .thenReturn(new PaymentGatewayCreateResult("checkout", "qr", "payos-ref", "{}"));

        PaymentCreateResponse response = paymentService.createPayment(request, null);

        PaymentTransaction created = transactionReference.get();
        assertThat(created.getAmount()).isEqualByComparingTo("125000");
        assertThat(created.getPaymentMethod()).isEqualTo(PaymentMethod.PAYOS);
        assertThat(initialStatus.get()).isEqualTo(PaymentTransactionStatus.CREATING);
        assertThat(created.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
        assertThat(created.getBusinessDate()).isEqualTo(AppTime.today());

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

        when(transactionRepository.findFirstByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        PaymentCreateResponse response =
                paymentService.createPayment(new PaymentCreateRequest(1L, PaymentMethod.PAYOS, null, " idem-1 "), null);

        assertThat(response.transactionId()).isEqualTo(88L);
        assertThat(response.idempotencyKey()).isEqualTo("idem-1");
        verify(transactionRepository, never()).saveAndFlush(any(PaymentTransaction.class));
    }

    @Test
    void createPaymentSettlesCashImmediatelyWithUnifiedResponse() {
        Order order = payableOrder();
        User cashier = User.builder()
                .id(7L)
                .email("staff@example.com")
                .fullName("Staff")
                .password("pw")
                .build();
        PaymentTransaction pendingOnline = PaymentTransaction.builder()
                .id(77L)
                .order(order)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.PAYOS)
                .build();

        when(transactionRepository.findFirstByIdempotencyKey("cash-idem")).thenReturn(Optional.empty());
        when(orderSettlementService.prepareForPayment(1L, null)).thenReturn(order);
        when(userRepository.findById(7L)).thenReturn(Optional.of(cashier));
        when(transactionRepository.saveAndFlush(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(100L);
            }
            return transaction;
        });
        when(paymentCompletionService.completeSuccessfulTransaction(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> {
                    PaymentTransaction transaction = invocation.getArgument(0);
                    transaction.setStatus(PaymentTransactionStatus.PAID);
                    transaction.setPaidAt(AppTime.now());
                    return order;
                });
        when(transactionRepository.findPendingOnlineTransactionsByOrderId(1L)).thenReturn(List.of(pendingOnline));

        PaymentCreateResponse response =
                paymentService.createPayment(new PaymentCreateRequest(1L, PaymentMethod.CASH, null, "cash-idem"), 7L);

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository).saveAndFlush(transactionCaptor.capture());

        PaymentTransaction cashTransaction = transactionCaptor.getValue();
        assertThat(cashTransaction.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(cashTransaction.getCreatedBy()).isSameAs(cashier);
        assertThat(cashTransaction.getIdempotencyKey()).isEqualTo("cash-idem");

        assertThat(response.transactionId()).isEqualTo(100L);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.status()).isEqualTo(PaymentTransactionStatus.PAID);
        assertThat(response.checkoutUrl()).isNull();
        assertThat(response.qrCode()).isNull();
        assertThat(response.amount()).isEqualByComparingTo("125000");
        assertThat(pendingOnline.getStatus()).isEqualTo(PaymentTransactionStatus.CANCELLED);
    }

    @Test
    void createPaymentReusesDefaultCashIdempotencyKeyBeforeOrderValidation() {
        Order order = payableOrder();
        PaymentTransaction existing = PaymentTransaction.builder()
                .id(101L)
                .order(order)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PAID)
                .paymentMethod(PaymentMethod.CASH)
                .idempotencyKey("cash:order:1")
                .build();

        when(transactionRepository.findFirstByIdempotencyKey("cash:order:1")).thenReturn(Optional.of(existing));

        PaymentCreateResponse response =
                paymentService.createPayment(new PaymentCreateRequest(1L, PaymentMethod.CASH, null, null), 7L);

        assertThat(response.transactionId()).isEqualTo(101L);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.status()).isEqualTo(PaymentTransactionStatus.PAID);
        verify(orderSettlementService, never()).prepareForPayment(any(), any());
        verify(transactionRepository, never()).saveAndFlush(any(PaymentTransaction.class));
    }

    @Test
    void confirmPaymentFromWebhookLocksOrderBeforeTransaction() {
        Order orderReference = Order.builder().id(1L).build();
        Order lockedOrder = payableOrder();
        PaymentTransaction lockedTransaction = PaymentTransaction.builder()
                .id(99L)
                .order(orderReference)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PENDING)
                .paymentMethod(PaymentMethod.PAYOS)
                .build();

        when(transactionRepository.findWithOrderByIdForUpdate(99L)).thenReturn(Optional.of(lockedTransaction));
        when(orderSettlementService.loadForPayment(1L)).thenReturn(lockedOrder);

        paymentService.confirmPaymentFromWebhook(
                new PaymentWebhookResult(99L, BigDecimal.valueOf(125_000), "payos-ref", "{}"));

        var ordered = inOrder(transactionRepository, orderSettlementService, paymentCompletionService);
        ordered.verify(transactionRepository).findWithOrderByIdForUpdate(99L);
        ordered.verify(orderSettlementService).loadForPayment(1L);
        ordered.verify(paymentCompletionService).completeSuccessfulTransaction(lockedTransaction);

        assertThat(lockedTransaction.getOrder()).isSameAs(lockedOrder);
        assertThat(lockedTransaction.getExternalReference()).isEqualTo("payos-ref");
        assertThat(lockedTransaction.getProviderPayload()).isEqualTo("{}");
    }

    @Test
    void confirmPaymentFromWebhookIsIdempotentWhenAlreadyPaid() {
        Order orderReference = Order.builder().id(1L).build();
        PaymentTransaction existing = PaymentTransaction.builder()
                .id(99L)
                .order(orderReference)
                .amount(BigDecimal.valueOf(125_000))
                .status(PaymentTransactionStatus.PAID)
                .paymentMethod(PaymentMethod.PAYOS)
                .build();

        when(transactionRepository.findWithOrderByIdForUpdate(99L)).thenReturn(Optional.of(existing));
        when(orderSettlementService.loadForPayment(1L)).thenReturn(payableOrder());

        paymentService.confirmPaymentFromWebhook(
                new PaymentWebhookResult(99L, BigDecimal.valueOf(125_000), "payos-ref", "{}"));

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
