package com.qros.modules.order.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderComboRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.listener.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderBatch;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.OrderItemOption;
import com.qros.modules.order.model.enums.BatchSource;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderItemType;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.repository.projection.ActiveOrderLockProjection;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.event.DomainEvents.*;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = OrderStatus.ACTIVE_STATUSES;
    private static final List<String> ACTIVE_ORDER_STATUS_NAMES = OrderStatus.ACTIVE_STATUS_NAMES;

    private final OrderRepository orderRepository;
    private final MenuCatalogLookupService menuCatalogLookupService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderPricingService orderPricingService;
    private final OrderStatusService orderStatusService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderMapper orderMapper;
    private final MeterRegistry meterRegistry;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final OrderInventoryReservationPort inventoryReservationService;
    private final OrderValidator orderValidator;
    private final TableSessionService tableSessionService;
    private final OrderPaymentPort orderPaymentPort;
    private final SystemSettingsService settingsService;

    private Counter ordersCreatedCounter;

    @PostConstruct
    public void initCounters() {
        ordersCreatedCounter = Counter.builder("orders.created")
                .description("Total number of orders created including merged sessions")
                .register(meterRegistry);
    }

    @Transactional
    public OrderResponse createCustomerOrder(@NonNull CustomerCreateOrderRequest request) {
        validateOrderContent(request.items(), request.combos());
        orderValidator.validateCustomerOperatingHours();

        TableSession session =
                tableSessionService.requireOpenSessionForOrdering(request.tableCode(), request.sessionToken());

        DiningTable table = Optional.ofNullable(session.getTable())
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID, "Secure Table Code invalid"));

        return createOrder(table, session, request.items(), request.combos(), BatchSource.QR, null);
    }

    @Transactional
    public OrderResponse createStaffOrder(@NonNull StaffCreateOrderRequest request) {
        validateOrderContent(request.items(), request.combos());

        if (request.orderType() == OrderType.TAKEAWAY) {
            return createTakeawayOrder(request);
        }

        TableSession session = tableSessionService.getOrCreateStaffSession(request.tableId());
        DiningTable table = session.getTable();

        return createOrder(table, session, request.items(), request.combos(), BatchSource.STAFF, request.voucherCode());
    }

    private OrderResponse createTakeawayOrder(@NonNull StaffCreateOrderRequest request) {
        orderValidator.validateSystemAcceptsOrders();

        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .orderType(OrderType.TAKEAWAY)
                .subtotalAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .businessDate(AppTime.today())
                .build();

        Order saved = populateAndPersistOrderBatch(
                order, request.items(), request.combos(), BatchSource.STAFF, request.voucherCode(), true, null);
        log.info("Takeaway order processed: ID #{}", saved.getId());
        return orderMapper.toResponse(saved);
    }

    private OrderResponse createOrder(
            DiningTable table,
            TableSession session,
            List<OrderItemRequest> items,
            List<OrderComboRequest> combos,
            BatchSource source,
            String voucherCode) {
        Order order = getOrCreateActiveOrder(table, session);
        ensureNoPaymentInProgress(order);
        orderValidator.validateTableAcceptsOrders(table);

        Order saved = populateAndPersistOrderBatch(order, items, combos, source, voucherCode, true, savedOrder -> {
            orderStatusService.tryAutoPromoteOrder(savedOrder);
            orderTableSyncService.recalcTableStatus(savedOrder);
        });
        log.info("Order processed for table {}: ID #{}", table.getTableNumber(), saved.getId());
        return orderMapper.toResponse(saved);
    }

    private Order populateAndPersistOrderBatch(
            Order order,
            List<OrderItemRequest> items,
            List<OrderComboRequest> combos,
            BatchSource source,
            String voucherCode,
            boolean autoConfirmIfEnabled,
            Consumer<Order> postReservationHook) {
        OrderBatch batch =
                OrderBatch.builder().submittedAt(AppTime.now()).source(source).build();

        Map<OrderItem, Integer> quantitiesToReserve = new LinkedHashMap<>();
        buildOrderItems(items, order, batch, quantitiesToReserve);
        buildOrderCombos(combos, order, batch, quantitiesToReserve, source == BatchSource.QR);

        attachBatchIfNotEmpty(order, batch);
        cleanupEmptyBatches(order);

        orderPricingService.recalculateOrderTotals(order);
        if (voucherCode != null && !voucherCode.isBlank()) {
            orderPricingService.applyVoucher(order, voucherCode);
        }
        if (autoConfirmIfEnabled
                && order.getStatus() == OrderStatus.PENDING
                && Boolean.TRUE.equals(settingsService.getSettingsEntity().getAutoConfirmOrders())) {
            order.setStatus(OrderStatus.SERVING);
        }

        Order saved = orderRepository.save(order);
        orderRepository.flush();
        inventoryReservationService.reserveForOrderItems(quantitiesToReserve);

        if (postReservationHook != null) {
            postReservationHook.accept(saved);
        }

        orderCacheInvalidationService.evictAfterOrderMutation(saved);
        ordersCreatedCounter.increment();
        eventPublisher.publishEvent(new OrderChangeEvent());
        return saved;
    }

    private Order getOrCreateActiveOrder(DiningTable table, TableSession session) {
        if (session != null) {
            List<ActiveOrderLockProjection> activeSessionOrders =
                    orderRepository.findActiveOrderLocksByTableSessionIdForUpdate(
                            session.getId(), ACTIVE_ORDER_STATUS_NAMES);

            if (!activeSessionOrders.isEmpty()) {
                if (activeSessionOrders.size() > 1) {
                    throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
                }

                return loadActiveOrderDetail(activeSessionOrders.get(0).getId());
            }
        }

        List<ActiveOrderLockProjection> activeOrders =
                orderRepository.findActiveOrderLocksByTableIdForUpdate(table.getId(), ACTIVE_ORDER_STATUS_NAMES);

        if (!activeOrders.isEmpty()) {
            if (activeOrders.size() > 1) {
                throw new BusinessException(ErrorCode.TABLE_MERGE_HAS_MULTIPLE_ACTIVE_ORDERS);
            }

            ActiveOrderLockProjection activeOrderLock = activeOrders.get(0);
            Order activeOrder = loadActiveOrderDetail(activeOrderLock.getId());
            if (session != null && activeOrderLock.getTableSessionId() == null) {
                activeOrder.setTableSession(session);
            }

            return activeOrder;
        }

        return Order.builder()
                .table(table)
                .tableSession(session)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .orderType(OrderType.DINE_IN)
                .subtotalAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .businessDate(AppTime.today())
                .build();
    }

    private Order loadActiveOrderDetail(Long orderId) {
        return orderRepository
                .findDetailById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void ensureNoPaymentInProgress(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        if (order.isPaid()) {
            throw new BusinessException(
                    ErrorCode.ORDER_ALREADY_PAID, "This order is already paid and cannot accept additional items.");
        }

        boolean hasPendingPayment = orderPaymentPort.hasPendingPaymentForOrder(order.getId());

        if (hasPendingPayment) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_IN_PROGRESS,
                    "The table is currently being paid. Please contact a staff member.");
        }
    }

    private void validateOrderContent(List<OrderItemRequest> items, List<OrderComboRequest> combos) {
        OrderOptionSupport.validateOrderContent(items, combos);
    }

    private void buildOrderItems(
            List<OrderItemRequest> items, Order order, OrderBatch batch, Map<OrderItem, Integer> quantitiesToReserve) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Set<Long> menuItemIds = items.stream().map(OrderItemRequest::menuItemId).collect(Collectors.toSet());

        Map<Long, MenuItem> menuItemsMap = menuCatalogLookupService.findMenuItemsByIds(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));
        Map<Long, ItemOptionValue> selectedValuesById = loadSelectedOptionValues(items);

        for (OrderItemRequest itemRequest : items) {
            MenuItem menuItem = Optional.ofNullable(menuItemsMap.get(itemRequest.menuItemId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));
            orderValidator.validateMenuItemOrderable(menuItem);

            List<Long> selectedOptionValueIds =
                    itemRequest.selectedOptionValueIds() == null ? List.of() : itemRequest.selectedOptionValueIds();

            OrderOptionSupport.validateSelectedOptions(menuItem, selectedOptionValueIds);

            List<ItemOptionValue> selectedValues =
                    OrderOptionSupport.resolveSelectedValues(selectedOptionValueIds, selectedValuesById);
            OrderOptionSupport.validateOptionValuesExist(selectedOptionValueIds, selectedValues);

            BigDecimal extraPrice = OrderOptionSupport.calculateOptionsPrice(selectedValues);

            BigDecimal unitPrice = menuItem.getPrice().add(extraPrice);

            Optional<OrderItem> existing = order.getOrderItems().stream()
                    .filter(item -> Objects.equals(item.getBatch(), batch))
                    .filter(OrderItem::canBeMerged)
                    .filter(item -> item.getMenuItem() != null)
                    .filter(item -> Objects.equals(item.getMenuItem().getId(), menuItem.getId()))
                    .filter(item ->
                            Objects.equals(normalizeNotes(item.getNotes()), normalizeNotes(itemRequest.notes())))
                    .filter(item -> checkOptionsMatch(item.getOrderItemOptions(), selectedOptionValueIds))
                    .findFirst();

            if (existing.isPresent()) {
                mergeExistingItem(existing.get(), itemRequest.quantity(), quantitiesToReserve);
                continue;
            }

            OrderItem orderItem = OrderItem.builder()
                    .batch(batch)
                    .menuItem(menuItem)
                    .unitPrice(unitPrice)
                    .itemNameSnapshot(menuItem.getName())
                    .itemType(OrderItemType.MENU_ITEM)
                    .quantity(itemRequest.quantity())
                    .lineTotal(orderPricingService.calculateLineTotal(unitPrice, itemRequest.quantity()))
                    .notes(normalizeNotes(itemRequest.notes()))
                    .status(OrderItemStatus.PENDING)
                    .build();

            selectedValues.forEach(value -> orderItem.addOption(OrderItemOption.builder()
                    .optionName(value.getItemOption().getName())
                    .optionValueName(value.getName())
                    .extraPrice(value.getExtraPrice())
                    .itemOptionValue(value)
                    .build()));

            order.addItem(orderItem);
            quantitiesToReserve.put(orderItem, itemRequest.quantity());
        }
    }

    private Map<Long, ItemOptionValue> loadSelectedOptionValues(List<OrderItemRequest> items) {
        Set<Long> selectedOptionValueIds = OrderOptionSupport.extractSelectedOptionValueIds(items);
        if (selectedOptionValueIds.isEmpty()) {
            return Map.of();
        }

        return menuCatalogLookupService.findItemOptionValuesByIds(selectedOptionValueIds).stream()
                .collect(Collectors.toMap(ItemOptionValue::getId, value -> value));
    }

    private void buildOrderCombos(
            List<OrderComboRequest> combos,
            Order order,
            OrderBatch batch,
            Map<OrderItem, Integer> quantitiesToReserve,
            boolean isCustomerOrder) {
        if (combos == null || combos.isEmpty()) {
            return;
        }

        List<Long> comboIds = combos.stream().map(OrderComboRequest::comboId).collect(Collectors.toList());

        java.util.Map<Long, Combo> combosMap = menuCatalogLookupService.findCombosByIdsWithItems(comboIds).stream()
                .collect(Collectors.toMap(Combo::getId, c -> c));

        for (OrderComboRequest comboRequest : combos) {
            Combo combo = Optional.ofNullable(combosMap.get(comboRequest.comboId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMBO_NOT_FOUND));
            if (isCustomerOrder) {
                orderValidator.validateComboOrderable(combo, true);
            } else {
                orderValidator.validateComboOrderable(combo);
            }

            Optional<OrderItem> existing = order.getOrderItems().stream()
                    .filter(item -> Objects.equals(item.getBatch(), batch))
                    .filter(OrderItem::canBeMerged)
                    .filter(item -> item.getCombo() != null)
                    .filter(item -> Objects.equals(item.getCombo().getId(), combo.getId()))
                    .filter(item ->
                            Objects.equals(normalizeNotes(item.getNotes()), normalizeNotes(comboRequest.notes())))
                    .findFirst();

            if (existing.isPresent()) {
                mergeExistingItem(existing.get(), comboRequest.quantity(), quantitiesToReserve);
                continue;
            }

            OrderItem orderItem = OrderItem.builder()
                    .batch(batch)
                    .combo(combo)
                    .unitPrice(combo.getPrice())
                    .itemNameSnapshot(combo.getName())
                    .itemType(OrderItemType.COMBO)
                    .quantity(comboRequest.quantity())
                    .lineTotal(orderPricingService.calculateLineTotal(combo.getPrice(), comboRequest.quantity()))
                    .notes(normalizeNotes(comboRequest.notes()))
                    .status(OrderItemStatus.PENDING)
                    .build();

            order.addItem(orderItem);
            quantitiesToReserve.put(orderItem, comboRequest.quantity());
        }
    }

    private void attachBatchIfNotEmpty(Order order, OrderBatch batch) {
        boolean hasItemsInBatch =
                order.getOrderItems().stream().anyMatch(item -> Objects.equals(item.getBatch(), batch));
        if (hasItemsInBatch) {
            order.addBatch(batch);
        }
    }

    private void cleanupEmptyBatches(Order order) {
        if (order.getOrderBatches() == null || order.getOrderBatches().isEmpty()) {
            return;
        }
        Set<OrderBatch> batchesWithItems = order.getOrderItems().stream()
                .map(OrderItem::getBatch)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        order.getOrderBatches().removeIf(b -> !batchesWithItems.contains(b));
    }

    private void mergeExistingItem(
            OrderItem existingItem, Integer additionalQuantity, Map<OrderItem, Integer> quantitiesToReserve) {
        final int mergedQuantity;
        try {
            mergedQuantity = Math.addExact(existingItem.getQuantity(), additionalQuantity);
            if (mergedQuantity > 1000) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Order item quantity exceeds maximum limit");
            }
            quantitiesToReserve.merge(existingItem, additionalQuantity, Math::addExact);
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Order item quantity exceeds maximum limit");
        }

        existingItem.setQuantity(mergedQuantity);
        orderPricingService.recalculateLineTotal(existingItem);
    }

    private boolean checkOptionsMatch(Collection<OrderItemOption> existingOptions, List<Long> incomingIds) {
        Set<Long> existingIds = existingOptions.stream()
                .map(OrderItemOption::getItemOptionValue)
                .filter(Objects::nonNull)
                .map(ItemOptionValue::getId)
                .collect(Collectors.toSet());

        Set<Long> incoming = incomingIds == null ? Set.of() : new HashSet<>(incomingIds);

        return existingIds.equals(incoming);
    }

    private String normalizeNotes(String notes) {
        return notes == null ? "" : notes.trim();
    }
}
