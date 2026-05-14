package com.sacmauquan.qrordering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.model.Order;
import com.sacmauquan.qrordering.model.PaymentTransaction;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import com.sacmauquan.qrordering.repository.OrderRepository;
import com.sacmauquan.qrordering.repository.PaymentTransactionRepository;
import com.sacmauquan.qrordering.repository.UserRepository;
import com.sacmauquan.qrordering.service.impl.PayosServiceImpl;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayosServiceImplTest {
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    PayOS payOS;
    @Mock
    OrderRepository orderRepository;
    @Mock
    PaymentTransactionRepository transactionRepository;
    @Mock
    DiningTableRepository tableRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    TransactionSideEffectService sideEffects;
    @Mock
    DiscountService discountService;
    @Mock
    UserRepository userRepository;

    PayosServiceImpl payosService;

    @BeforeEach
    void setUp() {
        payosService = new PayosServiceImpl(payOS, orderRepository, transactionRepository, tableRepository,
                notificationService, sideEffects, discountService, userRepository);
        ReflectionTestUtils.setField(payosService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void createPaymentLinkUsesOrderTotalInsteadOfClientAmount() throws Exception {
        Order order = Order.builder()
                .id(1L)
                .totalAmount(BigDecimal.valueOf(125000))
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .build();
        PayosCreateRequest request = new PayosCreateRequest();
        request.setOrderId(1L);
        CreatePaymentLinkResponse response = mock(CreatePaymentLinkResponse.class);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(transactionRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(1L,
                PaymentTransaction.TransactionStatus.PENDING)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setId(99L);
            return tx;
        });
        when(payOS.paymentRequests().create(any())).thenReturn(response);
        when(response.getCheckoutUrl()).thenReturn("checkout");
        when(response.getQrCode()).thenReturn("qr");
        when(response.getPaymentLinkId()).thenReturn("payos-ref");

        payosService.createPaymentLink(request);

        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository, org.mockito.Mockito.atLeastOnce()).save(txCaptor.capture());
        assertThat(txCaptor.getAllValues().get(0).getAmount()).isEqualByComparingTo("125000");
    }

    @Test
    void processWebhookRejectsAmountMismatch() throws Exception {
        Webhook webhook = mock(Webhook.class);
        WebhookData webhookData = mock(WebhookData.class);
        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(10L)
                .amount(BigDecimal.valueOf(100000))
                .status(PaymentTransaction.TransactionStatus.PENDING)
                .order(Order.builder().id(1L).totalAmount(BigDecimal.valueOf(100000)).build())
                .build();

        when(payOS.webhooks().verify(webhook)).thenReturn(webhookData);
        when(webhookData.getOrderCode()).thenReturn(10L);
        when(webhookData.getAmount()).thenReturn(90000L);
        when(transactionRepository.findWithOrderById(10L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> payosService.processWebhook(webhook))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Webhook amount does not match transaction");
    }

    @Test
    void processWebhookCompletesOrderWhenPaidAmountCoversBill() throws Exception {
        Webhook webhook = mock(Webhook.class);
        WebhookData webhookData = mock(WebhookData.class);
        DiningTable table = DiningTable.builder().id(2L).status(DiningTable.TableStatus.OCCUPIED).build();
        Order order = Order.builder()
                .id(1L)
                .table(table)
                .totalAmount(BigDecimal.valueOf(100000))
                .paymentStatus(Order.PaymentStatus.PENDING)
                .status(Order.OrderStatus.PENDING)
                .build();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(10L)
                .amount(BigDecimal.valueOf(100000))
                .status(PaymentTransaction.TransactionStatus.PENDING)
                .order(order)
                .build();

        when(payOS.webhooks().verify(webhook)).thenReturn(webhookData);
        when(webhookData.getOrderCode()).thenReturn(10L);
        when(webhookData.getAmount()).thenReturn(100000L);
        when(webhookData.getPaymentLinkId()).thenReturn("payos-ref");
        when(transactionRepository.findWithOrderById(10L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.sumPaidAmountByOrderId(1L)).thenReturn(BigDecimal.valueOf(100000));

        payosService.processWebhook(webhook);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PAID);
        assertThat(table.getStatus()).isEqualTo(DiningTable.TableStatus.AVAILABLE);
    }
}
