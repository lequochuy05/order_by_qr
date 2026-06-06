package com.qros.test;

import com.qros.modules.menu.service.CategoryService;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.menu.service.ComboService;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.order.service.impl.OrderServiceImpl;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.modules.payment.service.PayosService;
import com.qros.modules.payment.service.impl.PayosServiceImpl;
import com.qros.modules.promotion.service.DiscountService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.recomendation.service.RecommendationService;
import com.qros.modules.user.service.UserService;
import com.qros.modules.table.service.DiningTableService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.OrderItemOption;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.user.model.User;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.payment.dto.PayosCreateRequest;
import com.qros.modules.payment.dto.PayosCreateResponse;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.entity.BaseEntity;
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

import com.qros.modules.payment.dto.PayosCreateRequest;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.order.model.Order;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.user.repository.UserRepository;
import com.qros.modules.payment.service.impl.PayosServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

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

    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    PayosServiceImpl payosService;

    @BeforeEach
    void setUp() {
        payosService = new PayosServiceImpl(payOS, orderRepository, transactionRepository, tableRepository,
                notificationService, sideEffects, discountService, userRepository, meterRegistry);
        ReflectionTestUtils.setField(payosService, "frontendUrl", "http://localhost:5173");
        payosService.initCounters();
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
