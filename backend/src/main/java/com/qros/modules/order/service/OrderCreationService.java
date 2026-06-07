package com.qros.modules.order.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderBatch;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.OrderItemOption;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.util.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository tableRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;
    private final NotificationService notificationService;
    private final OrderPricingService orderPricingService;
    private final OrderStatusService orderStatusService;
    private final OrderMapper orderMapper;
    private final MeterRegistry meterRegistry;

    private Counter ordersCreatedCounter;

    @PostConstruct
    public void initCounters() {
        ordersCreatedCounter = Counter.builder("orders.created")
                .description("Total number of orders created (including merged sessions)")
                .register(meterRegistry);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tables", allEntries = true),
            @CacheEvict(value = "order_by_id", allEntries = true),
            @CacheEvict(value = "order_stats", allEntries = true)
    })
    public OrderResponse createOrder(@NonNull OrderRequest request) {
        validateOrderItems(request);
        DiningTable table = resolveTable(request);

        Order order = orderRepository.findFirstByTableIdAndStatusInForUpdate(table.getId(),
                List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING, Order.OrderStatus.AWAITING_PAYMENT))
                .orElse(null);

        if (order == null) {
            order = Order.builder()
                    .table(table)
                    .status(Order.OrderStatus.PENDING)
                    .paymentStatus(Order.PaymentStatus.PENDING)
                    .orderType(Order.OrderType.DINE_IN)
                    .subtotalAmount(BigDecimal.ZERO)
                    .finalAmount(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .businessDate(AppTime.today())
                    .build();
        }

        OrderBatch batch = OrderBatch.builder()
                .order(order)
                .submittedAt(AppTime.now())
                .source(resolveBatchSource(request))
                .build();
        order.getOrderBatches().add(batch);

        buildOrderItems(request, order, batch);
        buildOrderCombos(request, order, batch);
        orderPricingService.recalculateOrderTotals(order);

        Order saved = orderRepository.save(Objects.requireNonNull(order));
        orderStatusService.tryAutoPromoteOrder(saved);

        table.setStatus(DiningTable.TableStatus.OCCUPIED);
        tableRepository.save(table);

        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        ordersCreatedCounter.increment();
        log.info("Order processed for Table {}: ID #{}", table.getTableNumber(), saved.getId());
        return orderMapper.toResponse(saved);
    }

    private void validateOrderItems(OrderRequest request) {
        if ((request.getItems() == null || request.getItems().isEmpty())
                && (request.getCombos() == null || request.getCombos().isEmpty())) {
            throw new BusinessException(ErrorCode.ORDER_CONTENT_EMPTY);
        }
    }

    private DiningTable resolveTable(OrderRequest request) {
        if (request.getTableCode() != null && !request.getTableCode().isBlank()) {
            return tableRepository.findByTableCode(request.getTableCode())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_CODE_INVALID,
                            "Không tìm thấy thông tin bàn. Mã QR này có thể đã được tạo lại hoặc không còn hiệu lực."));
        } else if (request.getTableId() != null) {
            return tableRepository.findById(Objects.requireNonNull(request.getTableId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND,
                            "Không tìm thấy thông tin bàn."));
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "Vui lòng quét mã QR trên bàn để đặt món.");
    }

    private OrderBatch.BatchSource resolveBatchSource(OrderRequest request) {
        return request.getTableCode() != null && !request.getTableCode().isBlank()
                ? OrderBatch.BatchSource.QR
                : OrderBatch.BatchSource.STAFF;
    }

    private void buildOrderItems(OrderRequest request, Order order, OrderBatch batch) {
        if (request.getItems() == null) {
            return;
        }

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(Objects.requireNonNull(itemRequest.getMenuItemId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));

            validateRequiredOptions(menuItem, itemRequest.getSelectedOptionValueIds());

            List<ItemOptionValue> selectedValues = itemRequest.getSelectedOptionValueIds() != null
                    ? itemOptionValueRepository.findAllById(
                            Objects.requireNonNull(itemRequest.getSelectedOptionValueIds()))
                    : List.of();

            BigDecimal extraPrice = selectedValues.stream()
                    .map(ItemOptionValue::getExtraPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitPrice = menuItem.getPrice().add(extraPrice);

            Optional<OrderItem> existing = order.getOrderItems().stream()
                    .filter(item -> item.getMenuItem() != null && item.getMenuItem().getId().equals(menuItem.getId()))
                    .filter(item -> Objects.equals(item.getNotes(), itemRequest.getNotes()))
                    .filter(item -> !item.isPrepared())
                    .filter(item -> checkOptionsMatch(item.getOrderItemOptions(),
                            itemRequest.getSelectedOptionValueIds()))
                    .findFirst();

            if (existing.isPresent()) {
                OrderItem existingItem = existing.get();
                if (existingItem.getItemNameSnapshot() == null) {
                    existingItem.setItemNameSnapshot(menuItem.getName());
                }
                if (existingItem.getItemType() == null) {
                    existingItem.setItemType(OrderItem.OrderItemType.MENU_ITEM);
                }
                if (existingItem.getBatch() == null) {
                    existingItem.setBatch(batch);
                }
                existingItem.setQuantity(existingItem.getQuantity() + itemRequest.getQuantity());
                orderPricingService.recalculateLineTotal(existingItem);
            } else {
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .batch(batch)
                        .menuItem(menuItem)
                        .unitPrice(unitPrice)
                        .itemNameSnapshot(menuItem.getName())
                        .itemType(OrderItem.OrderItemType.MENU_ITEM)
                        .quantity(itemRequest.getQuantity())
                        .lineTotal(orderPricingService.calculateLineTotal(unitPrice, itemRequest.getQuantity()))
                        .notes(itemRequest.getNotes())
                        .status(OrderItem.OrderItemStatus.PENDING)
                        .build();

                selectedValues.forEach(value -> orderItem.getOrderItemOptions().add(OrderItemOption.builder()
                        .orderItem(orderItem)
                        .optionName(value.getItemOption().getName())
                        .optionValueName(value.getName())
                        .extraPrice(value.getExtraPrice())
                        .itemOptionValue(value)
                        .build()));

                order.getOrderItems().add(orderItem);
            }
        }
    }

    private void buildOrderCombos(OrderRequest request, Order order, OrderBatch batch) {
        if (request.getCombos() == null) {
            return;
        }

        for (OrderRequest.OrderComboRequest comboRequest : request.getCombos()) {
            Combo combo = comboRepository.findById(Objects.requireNonNull(comboRequest.getComboId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMBO_NOT_FOUND));

            Optional<OrderItem> existing = order.getOrderItems().stream()
                    .filter(item -> item.getCombo() != null && item.getCombo().getId().equals(combo.getId()))
                    .filter(item -> Objects.equals(item.getNotes(), comboRequest.getNotes()))
                    .filter(item -> !item.isPrepared())
                    .findFirst();

            if (existing.isPresent()) {
                OrderItem existingItem = existing.get();
                if (existingItem.getItemNameSnapshot() == null) {
                    existingItem.setItemNameSnapshot(combo.getName());
                }
                if (existingItem.getItemType() == null) {
                    existingItem.setItemType(OrderItem.OrderItemType.COMBO);
                }
                if (existingItem.getBatch() == null) {
                    existingItem.setBatch(batch);
                }
                existingItem.setQuantity(existingItem.getQuantity() + comboRequest.getQuantity());
                orderPricingService.recalculateLineTotal(existingItem);
            } else {
                order.getOrderItems().add(OrderItem.builder()
                        .order(order)
                        .batch(batch)
                        .combo(combo)
                        .unitPrice(combo.getPrice())
                        .itemNameSnapshot(combo.getName())
                        .itemType(OrderItem.OrderItemType.COMBO)
                        .quantity(comboRequest.getQuantity())
                        .lineTotal(orderPricingService.calculateLineTotal(combo.getPrice(), comboRequest.getQuantity()))
                        .notes(comboRequest.getNotes())
                        .status(OrderItem.OrderItemStatus.PENDING)
                        .build());
            }
        }
    }

    private void validateRequiredOptions(MenuItem menuItem, List<Long> selectedValueIds) {
        Set<Long> selectedIds = selectedValueIds != null ? new HashSet<>(selectedValueIds) : Set.of();
        menuItem.getItemOptions().stream()
                .filter(ItemOption::isRequired)
                .forEach(option -> {
                    boolean selected = option.getOptionValues().stream()
                            .anyMatch(value -> selectedIds.contains(value.getId()));
                    if (!selected) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST,
                                "Required option selection missing: " + option.getName());
                    }
                });
    }

    private boolean checkOptionsMatch(Collection<OrderItemOption> existing, List<Long> incomingIds) {
        Set<Long> existingIds = existing.stream()
                .map(option -> option.getItemOptionValue().getId())
                .collect(Collectors.toSet());
        Set<Long> incoming = incomingIds != null ? new HashSet<>(incomingIds) : Set.of();
        return existingIds.equals(incoming);
    }
}
