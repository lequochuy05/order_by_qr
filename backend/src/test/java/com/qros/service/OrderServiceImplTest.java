package com.qros.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderPaymentService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderStatusService;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.payment.service.PaymentCompletionService;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    VoucherCheckoutService voucherCheckoutService;

    @Mock
    PaymentTransactionRepository transactionRepository;

    @Mock
    PaymentCompletionService paymentCompletionService;

    @Mock
    OrderPricingService orderPricingService;

    @Mock
    OrderStatusService orderStatusService;

    private OrderPaymentService orderPaymentService;

    @BeforeEach
    void setUp() {
        orderPaymentService = new OrderPaymentService(
                orderRepository,
                userRepository,
                voucherCheckoutService,
                transactionRepository,
                orderPricingService,
                paymentCompletionService,
                orderStatusService,
                new OrderMapper());
    }

    @Test
    void payOrderCreatesCashPaymentTransaction() {
        User cashier = User.builder().id(7L).fullName("Cashier").build();
        Order order = Order.builder()
                .id(50L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(90_000))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.valueOf(90_000))
                .paidAmount(BigDecimal.ZERO)
                .businessDate(AppTime.today())
                .build();

        when(orderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(order));
        when(userRepository.findById(7L)).thenReturn(Optional.of(cashier));
        when(transactionRepository.findFirstByIdempotencyKey("cash:order:50")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String result = orderPaymentService.payOrder(50L, 7L, null);

        ArgumentCaptor<PaymentTransaction> transactionCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        PaymentTransaction transaction = transactionCaptor.getValue();
        assertThat(result).isEqualTo("Order settled successfully");
        assertThat(transaction.getOrder()).isEqualTo(order);
        assertThat(transaction.getAmount()).isEqualByComparingTo("90000");
        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
        assertThat(transaction.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(transaction.getCreatedBy()).isEqualTo(cashier);
        assertThat(transaction.getIdempotencyKey()).isEqualTo("cash:order:50");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentCompletionService).completeSuccessfulTransaction(transaction, null);
    }

    @Test
    void payOrderAutoPromotesFinishedPendingOrderBeforePayment() {
        User cashier = User.builder().id(8L).fullName("Cashier").build();
        Order order = Order.builder()
                .id(52L)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(120_000))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.valueOf(120_000))
                .paidAmount(BigDecimal.ZERO)
                .businessDate(AppTime.today())
                .build();

        when(orderRepository.findByIdForUpdate(52L)).thenReturn(Optional.of(order));
        when(userRepository.findById(8L)).thenReturn(Optional.of(cashier));
        doAnswer(invocation -> {
                    order.setStatus(OrderStatus.AWAITING_PAYMENT);
                    return null;
                })
                .when(orderStatusService)
                .tryAutoPromoteOrder(order);
        when(transactionRepository.findFirstByIdempotencyKey("cash:order:52")).thenReturn(Optional.empty());
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String result = orderPaymentService.payOrder(52L, 8L, null);

        assertThat(result).isEqualTo("Order settled successfully");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(orderStatusService).tryAutoPromoteOrder(order);
        verify(paymentCompletionService).completeSuccessfulTransaction(any(PaymentTransaction.class), isNull());
    }

    @Test
    void payOrderRejectsOrdersThatAreNotAwaitingPayment() {
        Order order = Order.builder()
                .id(51L)
                .status(OrderStatus.SERVING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderPaymentService.payOrder(51L, 7L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_PAYMENT_INVALID);
        verify(transactionRepository, never()).save(any(PaymentTransaction.class));
        verify(paymentCompletionService, never())
                .completeSuccessfulTransaction(any(PaymentTransaction.class), isNull());
    }
}
