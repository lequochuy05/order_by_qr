package com.qros.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.order.listener.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.promotion.service.PromotionService;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.service.TableSessionService;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AuditHardeningFinancialTest {

    @Test
    void orderPricingCalculatesServiceChargeThenTaxAndPersistsSnapshots() {
        SystemSettingsService settingsService = mock(SystemSettingsService.class);
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .serviceChargePercent(new BigDecimal("5.00"))
                        .taxPercent(new BigDecimal("10.00"))
                        .build());

        OrderPricingService pricingService = new OrderPricingService(
                mock(MenuCatalogLookupService.class),
                mock(VoucherCheckoutService.class),
                mock(OrderValidator.class),
                mock(TableSessionService.class),
                mock(PromotionService.class),
                settingsService);

        Order order = Order.builder().id(1L).build();
        // subtotal = 100,000, discount = 20,000 => net = 80,000
        // serviceCharge (5%) = 4,000 => taxable = 84,000
        // tax (10%) = 8,400 => final = 92,400
        pricingService.setOrderMoney(order, BigDecimal.valueOf(100_000), BigDecimal.valueOf(20_000));

        assertThat(order.getSubtotalAmount()).isEqualByComparingTo("100000");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("20000");
        assertThat(order.getServiceChargePercentSnapshot()).isEqualByComparingTo("5.00");
        assertThat(order.getServiceChargeAmount()).isEqualByComparingTo("4000");
        assertThat(order.getTaxPercentSnapshot()).isEqualByComparingTo("10.00");
        assertThat(order.getTaxAmount()).isEqualByComparingTo("8400");
        assertThat(order.getFinalAmount()).isEqualByComparingTo("92400");
    }

    @Test
    void earlySettlementWithUnfinishedItemsMarksOrderPaidWhileKeepingKitchenActive() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderStateFactory orderStateFactory = mock(OrderStateFactory.class);
        OrderAuditService orderAuditService = mock(OrderAuditService.class);
        OrderTableSyncService orderTableSyncService = mock(OrderTableSyncService.class);
        OrderCacheInvalidationService cacheInvalidationService = mock(OrderCacheInvalidationService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        VoucherCheckoutService voucherCheckoutService = mock(VoucherCheckoutService.class);
        TableSessionService tableSessionService = mock(TableSessionService.class);
        SystemSettingsService settingsService = mock(SystemSettingsService.class);

        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .allowSettlementWithUnfinishedItems(true)
                        .serviceChargePercent(BigDecimal.ZERO)
                        .taxPercent(BigDecimal.ZERO)
                        .build());

        OrderPricingService pricingService = new OrderPricingService(
                mock(MenuCatalogLookupService.class),
                voucherCheckoutService,
                mock(OrderValidator.class),
                tableSessionService,
                mock(PromotionService.class),
                settingsService);

        OrderSettlementService settlementService = new OrderSettlementService(
                orderRepository,
                orderStateFactory,
                orderAuditService,
                orderTableSyncService,
                cacheInvalidationService,
                eventPublisher,
                pricingService,
                voucherCheckoutService,
                tableSessionService,
                settingsService);

        OrderItem cookingItem = OrderItem.builder()
                .id(11L)
                .status(OrderItemStatus.COOKING)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(80_000))
                .lineTotal(BigDecimal.valueOf(80_000))
                .build();
        TableSession session = TableSession.builder().id(50L).build();
        Order order = Order.builder()
                .id(10L)
                .orderType(OrderType.DINE_IN)
                .status(OrderStatus.SERVING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(80_000))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.valueOf(80_000))
                .paidAmount(BigDecimal.ZERO)
                .tableSession(session)
                .businessDate(AppTime.today())
                .orderItems(new LinkedHashSet<>(Set.of(cookingItem)))
                .build();

        when(orderRepository.findPaymentByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheInvalidationService.resolveTableCodes(order)).thenReturn(List.of("BAN_01"));

        Order prepared = settlementService.prepareForPayment(10L, null, true);
        assertThat(prepared.getFinalAmount()).isEqualByComparingTo("80000");

        settlementService.settleAfterPayment(order, PaymentMethod.CASH, null, BigDecimal.valueOf(80_000), "cash");

        assertThat(order.isPaid()).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SERVING);
        assertThat(order.canAcceptNewItems()).isFalse();
        verify(tableSessionService, never()).closeSession(any(), any(), any());
    }

    @Test
    void cancellingLastOrderItemClosesSessionAndBlocksWhenPendingOnlinePaymentExists() {
        OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserLookupService userLookupService = mock(UserLookupService.class);
        OrderInventoryReservationPort reservationPort = mock(OrderInventoryReservationPort.class);
        OrderPricingService pricingService = mock(OrderPricingService.class);
        OrderAuditService auditService = mock(OrderAuditService.class);
        OrderStatusService statusService = mock(OrderStatusService.class);
        OrderTableSyncService tableSyncService = mock(OrderTableSyncService.class);
        OrderCacheInvalidationService cacheInvalidationService = mock(OrderCacheInvalidationService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderPaymentPort paymentPort = mock(OrderPaymentPort.class);
        TableSessionService tableSessionService = mock(TableSessionService.class);

        OrderItemWorkflowService workflowService = new OrderItemWorkflowService(
                orderItemRepository,
                orderRepository,
                userLookupService,
                reservationPort,
                pricingService,
                auditService,
                statusService,
                tableSyncService,
                cacheInvalidationService,
                eventPublisher,
                orderMapper,
                paymentPort,
                tableSessionService);

        DiningTable table = DiningTable.builder().id(1L).tableCode("BAN_01").build();
        TableSession session = TableSession.builder().id(99L).table(table).build();
        Order order = Order.builder()
                .id(10L)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .table(table)
                .tableSession(session)
                .build();
        OrderItem item = OrderItem.builder()
                .id(20L)
                .order(order)
                .status(OrderItemStatus.PENDING)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(50_000))
                .lineTotal(BigDecimal.valueOf(50_000))
                .build();
        order.setOrderItems(new LinkedHashSet<>(Set.of(item)));

        when(orderItemRepository.findDetailByIdForUpdate(20L)).thenReturn(Optional.of(item));
        when(orderRepository.findWorkflowByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(paymentPort.hasPendingPaymentForOrder(10L)).thenReturn(true);

        assertThatThrownBy(() -> workflowService.cancelOrderItem(20L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_PAYMENT_IN_PROGRESS));

        when(paymentPort.hasPendingPaymentForOrder(10L)).thenReturn(false);
        when(cacheInvalidationService.resolveTableCodes(order)).thenReturn(List.of("BAN_01"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        workflowService.cancelOrderItem(20L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(tableSessionService).closeSession(eq(99L), eq(TableSessionStatus.CANCELLED), any());
    }

    @Test
    void optionSupportRejectsDuplicateAndNullOptionValueIds() {
        MenuItem menuItem = MenuItem.builder().id(1L).name("Pho").build();

        assertThatThrownBy(() -> OrderOptionSupport.validateSelectedOptions(menuItem, List.of(1L, 1L)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

        java.util.List<Long> withNull = new java.util.ArrayList<>();
        withNull.add(1L);
        withNull.add(null);
        assertThatThrownBy(() -> OrderOptionSupport.validateSelectedOptions(menuItem, withNull))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
