package com.qros.modules.order.service;

import com.qros.modules.inventory.service.InventoryReservationService;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderComboRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.infrastructure.OrderCacheInvalidationService;
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
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.repository.DiningTableRepository;
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

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT);
    private static final List<String> ACTIVE_ORDER_STATUS_NAMES =
            ACTIVE_ORDER_STATUSES.stream().map(Enum::name).toList();

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository tableRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderPricingService orderPricingService;
    private final OrderStatusService orderStatusService;
    private final OrderTableSyncService orderTableSyncService;
    private final OrderMapper orderMapper;
    private final MeterRegistry meterRegistry;
    private final OrderCacheInvalidationService orderCacheInvalidationService;
    private final InventoryReservationService inventoryReservationService;
    private final OrderValidator orderValidator;
    private final TableSessionService tableSessionService;
    private final PaymentTransactionRepository paymentTransactionRepository;
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

        TableSession session =
                tableSessionService.requireOpenSessionForOrdering(request.tableCode(), request.sessionToken());

        DiningTable table = Optional.ofNullable(session.getTable())
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID, "Secure Table Code invalid"));

        return createOrder(table, session, request.items(), request.combos(), BatchSource.QR);
    }

    @Transactional
    public OrderResponse createStaffOrder(@NonNull StaffCreateOrderRequest request) {
        validateOrderContent(request.items(), request.combos());

        DiningTable table = tableRepository
                .findByIdForUpdate(request.tableId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND, "Table ID invalid"));

        return createOrder(table, null, request.items(), request.combos(), BatchSource.STAFF);
    }

    private OrderResponse createOrder(
            DiningTable table,
            TableSession session,
            List<OrderItemRequest> items,
            List<OrderComboRequest> combos,
            BatchSource source) {
        Order order = getOrCreateActiveOrder(table, session);
        ensureNoPaymentInProgress(order);
        orderValidator.validateTableAcceptsOrders(table);

        OrderBatch batch =
                OrderBatch.builder().submittedAt(AppTime.now()).source(source).build();

        order.addBatch(batch);

        Map<OrderItem, Integer> quantitiesToReserve = new LinkedHashMap<>();

        buildOrderItems(items, order, batch, quantitiesToReserve);
        buildOrderCombos(combos, order, batch, quantitiesToReserve);

        inventoryReservationService.reserveForOrderItems(quantitiesToReserve);

        orderPricingService.recalculateOrderTotals(order);
        applyAutoConfirmation(order, source);

        Order saved = orderRepository.save(order);

        orderStatusService.tryAutoPromoteOrder(saved);
        orderTableSyncService.recalcTableStatus(saved);

        orderCacheInvalidationService.evictAfterOrderMutation(saved);

        ordersCreatedCounter.increment();

        eventPublisher.publishEvent(new OrderChangeEvent());
        log.info("Order processed for table {}: ID #{}", table.getTableNumber(), saved.getId());

        return orderMapper.toResponse(saved);
    }

    private void applyAutoConfirmation(Order order, BatchSource source) {
        if (source == BatchSource.QR
                && order.getStatus() == OrderStatus.PENDING
                && Boolean.TRUE.equals(settingsService.getSettingsEntity().getAutoConfirmOrders())) {
            order.setStatus(OrderStatus.SERVING);
        }
    }

    private Order getOrCreateActiveOrder(DiningTable table, TableSession session) {
        if (session != null) {
            List<ActiveOrderLockProjection> activeSessionOrders =
                    orderRepository.findActiveOrderLocksByTableSessionIdForUpdate(
                            session.getId(), ACTIVE_ORDER_STATUS_NAMES);

            if (!activeSessionOrders.isEmpty()) {
                if (activeSessionOrders.size() > 1) {
                    log.warn(
                            "Table session {} has {} active orders. Using the latest one.",
                            session.getId(),
                            activeSessionOrders.size());
                }

                return loadActiveOrderDetail(activeSessionOrders.get(0).getId());
            }
        }

        List<ActiveOrderLockProjection> activeOrders =
                orderRepository.findActiveOrderLocksByTableIdForUpdate(table.getId(), ACTIVE_ORDER_STATUS_NAMES);

        if (!activeOrders.isEmpty()) {
            if (activeOrders.size() > 1) {
                log.warn("Table {} has {} active orders. Using the latest one.", table.getId(), activeOrders.size());
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

        boolean hasPendingPayment = paymentTransactionRepository.existsByOrderIdAndStatusIn(
                order.getId(), List.of(PaymentTransactionStatus.CREATING, PaymentTransactionStatus.PENDING));

        if (hasPendingPayment) {
            throw new BusinessException(
                    ErrorCode.ORDER_PAYMENT_IN_PROGRESS,
                    "The table is currently being paid. Please contact a staff member.");
        }
    }

    private void validateOrderContent(List<OrderItemRequest> items, List<OrderComboRequest> combos) {
        boolean hasItems = items != null && !items.isEmpty();
        boolean hasCombos = combos != null && !combos.isEmpty();

        if (!hasItems && !hasCombos) {
            throw new BusinessException(ErrorCode.ORDER_CONTENT_EMPTY);
        }

        if (hasCombos
                && combos.stream()
                        .anyMatch(combo -> combo == null || combo.quantity() == null || combo.quantity() < 1)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Combo quantity must be at least 1");
        }
    }

    private void buildOrderItems(
            List<OrderItemRequest> items, Order order, OrderBatch batch, Map<OrderItem, Integer> quantitiesToReserve) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Set<Long> menuItemIds = items.stream().map(OrderItemRequest::menuItemId).collect(Collectors.toSet());

        Map<Long, MenuItem> menuItemsMap = menuItemRepository.findAllByIdIn(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));
        Map<Long, ItemOptionValue> selectedValuesById = loadSelectedOptionValues(items);

        for (OrderItemRequest itemRequest : items) {
            MenuItem menuItem = Optional.ofNullable(menuItemsMap.get(itemRequest.menuItemId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));
            orderValidator.validateMenuItemOrderable(menuItem);

            List<Long> selectedOptionValueIds =
                    itemRequest.selectedOptionValueIds() == null ? List.of() : itemRequest.selectedOptionValueIds();

            validateSelectedOptions(menuItem, selectedOptionValueIds);

            List<ItemOptionValue> selectedValues = resolveSelectedValues(selectedOptionValueIds, selectedValuesById);
            validateOptionValuesExist(selectedOptionValueIds, selectedValues);

            BigDecimal extraPrice = selectedValues.stream()
                    .map(ItemOptionValue::getExtraPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal unitPrice = menuItem.getPrice().add(extraPrice);

            Optional<OrderItem> existing = order.getOrderItems().stream()
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
        Set<Long> selectedOptionValueIds = items.stream()
                .filter(Objects::nonNull)
                .flatMap(item -> item.selectedOptionValueIds() == null
                        ? java.util.stream.Stream.<Long>empty()
                        : item.selectedOptionValueIds().stream())
                .collect(Collectors.toSet());

        if (selectedOptionValueIds.isEmpty()) {
            return Map.of();
        }

        return itemOptionValueRepository.findAllByIdIn(selectedOptionValueIds).stream()
                .collect(Collectors.toMap(ItemOptionValue::getId, value -> value));
    }

    private List<ItemOptionValue> resolveSelectedValues(
            List<Long> selectedOptionValueIds, Map<Long, ItemOptionValue> selectedValuesById) {
        if (selectedOptionValueIds == null || selectedOptionValueIds.isEmpty()) {
            return List.of();
        }

        return selectedOptionValueIds.stream()
                .map(selectedValuesById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private void buildOrderCombos(
            List<OrderComboRequest> combos,
            Order order,
            OrderBatch batch,
            Map<OrderItem, Integer> quantitiesToReserve) {
        if (combos == null || combos.isEmpty()) {
            return;
        }

        List<Long> comboIds = combos.stream().map(OrderComboRequest::comboId).collect(Collectors.toList());

        java.util.Map<Long, Combo> combosMap = comboRepository.findAllByIdInWithItems(comboIds).stream()
                .collect(Collectors.toMap(Combo::getId, c -> c));

        for (OrderComboRequest comboRequest : combos) {
            Combo combo = Optional.ofNullable(combosMap.get(comboRequest.comboId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMBO_NOT_FOUND));
            orderValidator.validateComboOrderable(combo);

            Optional<OrderItem> existing = order.getOrderItems().stream()
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

    private void mergeExistingItem(
            OrderItem existingItem, Integer additionalQuantity, Map<OrderItem, Integer> quantitiesToReserve) {

        quantitiesToReserve.merge(existingItem, additionalQuantity, Integer::sum);

        existingItem.setQuantity(existingItem.getQuantity() + additionalQuantity);
        orderPricingService.recalculateLineTotal(existingItem);
    }

    private void validateSelectedOptions(MenuItem menuItem, List<Long> selectedValueIds) {
        Set<Long> selectedIds = new HashSet<>(selectedValueIds);

        validateRequiredOptions(menuItem, selectedIds);
        validateMaxSelections(menuItem, selectedIds);
        validateOptionValuesBelongToMenuItem(menuItem, selectedIds);
    }

    private void validateRequiredOptions(MenuItem menuItem, Set<Long> selectedIds) {
        menuItem.getItemOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getRequired()))
                .forEach(option -> {
                    boolean selected =
                            option.getOptionValues().stream().anyMatch(value -> selectedIds.contains(value.getId()));

                    if (!selected) {
                        throw new BusinessException(
                                ErrorCode.INVALID_REQUEST, "Required option selection missing: " + option.getName());
                    }
                });
    }

    private void validateMaxSelections(MenuItem menuItem, Set<Long> selectedIds) {
        menuItem.getItemOptions().forEach(option -> {
            int maxSelection = option.getMaxSelection() == null ? 1 : option.getMaxSelection();
            long selectedCount = option.getOptionValues().stream()
                    .filter(value -> selectedIds.contains(value.getId()))
                    .count();

            if (selectedCount > maxSelection) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST, "Too many option selections for: " + option.getName());
            }
        });
    }

    private void validateOptionValuesBelongToMenuItem(MenuItem menuItem, Set<Long> selectedIds) {
        if (selectedIds.isEmpty()) {
            return;
        }

        Set<Long> validValueIds = menuItem.getItemOptions().stream()
                .flatMap(option -> option.getOptionValues().stream())
                .map(ItemOptionValue::getId)
                .collect(Collectors.toSet());

        boolean allBelongToMenuItem = validValueIds.containsAll(selectedIds);

        if (!allBelongToMenuItem) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST, "Selected option contains invalid value for this menu item.");
        }
    }

    private void validateOptionValuesExist(List<Long> requestedIds, List<ItemOptionValue> selectedValues) {
        if (requestedIds.isEmpty()) {
            return;
        }

        Set<Long> foundIds = selectedValues.stream().map(ItemOptionValue::getId).collect(Collectors.toSet());

        if (!foundIds.containsAll(requestedIds)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Selected option value does not exist.");
        }
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
