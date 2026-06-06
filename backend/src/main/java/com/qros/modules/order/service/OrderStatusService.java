package com.qros.modules.order.service;

import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.modules.promotion.dto.DiscountResult;
import com.qros.modules.promotion.service.DiscountService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.user.model.User;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.util.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiningTableRepository tableRepository;
    private final UserRepository userRepository;
    private final DiscountService discountService;
    private final OrderStateFactory orderStateFactory;
    private final NotificationService notificationService;
    private final OrderPricingService orderPricingService;
    private final OrderMapper orderMapper;
    private final MeterRegistry meterRegistry;

    private Counter ordersCompletedCounter;

    @PostConstruct
    public void initCounters() {
        ordersCompletedCounter = Counter.builder("orders.completed")
                .description("Total number of orders marked as COMPLETED")
                .register(meterRegistry);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tables", allEntries = true),
            @CacheEvict(value = "order_by_id", key = "#id"),
            @CacheEvict(value = "order_stats", allEntries = true)
    })
    public OrderResponse updateStatus(@NonNull Long id, @NonNull String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        Order.OrderStatus targetStatus;
        try {
            targetStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status: " + status);
        }

        try {
            orderStateFactory.validateTransition(order.getStatus(), targetStatus);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OrderState state = orderStateFactory.getState(targetStatus);
        state.handleRequest(order);

        Order saved = orderRepository.save(Objects.requireNonNull(order));
        recalcTableStatus(order);
        notificationService.notifyOrderChange();
        return orderMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void cancelOrderItem(@NonNull Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        if (item.isPrepared()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel items that are already prepared");
        }

        Order order = item.getOrder();
        order.getOrderItems().remove(item);
        orderItemRepository.delete(item);

        if (order.getOrderItems().isEmpty()) {
            order.setStatus(Order.OrderStatus.CANCELLED);
        } else {
            orderPricingService.recalculateOrderTotals(order);
        }

        recalcTableStatus(order);
        orderRepository.save(order);
        notificationService.notifyOrderChange();
    }

    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        try {
            OrderItem.OrderItemStatus status = OrderItem.OrderItemStatus.valueOf(newStatus.toUpperCase());
            item.setStatus(status);
            item.setPrepared(status == OrderItem.OrderItemStatus.FINISHED);
            orderItemRepository.save(item);

            Order order = item.getOrder();
            tryAutoPromoteOrder(order);
            recalcTableStatus(order);
            notificationService.notifyOrderChange();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item status code: " + newStatus);
        }
    }

    @Transactional
    public void markItemPrepared(@NonNull Long itemId) {
        updateItemStatus(itemId, "FINISHED");
    }

    @Transactional
    public OrderResponse updateOrderItem(@NonNull Long itemId, int quantity, String notes) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        if (item.isPrepared()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot update items that have already been prepared");
        }

        item.setQuantity(quantity);
        item.setNotes(notes);
        orderItemRepository.save(item);

        Order order = item.getOrder();
        orderPricingService.recalculateOrderTotals(order);
        orderRepository.save(Objects.requireNonNull(order));

        notificationService.notifyOrderChange();
        return orderMapper.toResponse(order);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
                    "stats_dish_trend", "stats_dashboard" }, allEntries = true),
            @CacheEvict(value = "order_by_id", key = "#id"),
            @CacheEvict(value = "order_stats", allEntries = true)
    })
    public String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is already settled");
        }

        if (order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order must be in AWAITING_PAYMENT status before payment. Current: " + order.getStatus());
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processing user not found"));

        if (voucherCode != null && !voucherCode.isBlank()) {
            DiscountResult result = discountService.applyVoucher(voucherCode, order.getOriginalTotal());
            order.setVoucherCode(voucherCode);
            order.setDiscountVoucher(result.discountValue());
            order.setTotalAmount(result.finalTotal());
        }

        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentMethod(Order.PaymentMethod.CASH);
        order.setPaidBy(currentUser);
        order.setPaymentTime(AppTime.now());

        orderRepository.save(Objects.requireNonNull(order));
        recalcTableStatus(order);
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        ordersCompletedCounter.increment();
        log.info("Order #{} settled via CASH by Staff Member: {}", id, currentUser.getFullName());

        return "Order settled successfully";
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
                    "stats_dish_trend", "stats_dashboard" }, allEntries = true),
            @CacheEvict(value = "order_by_id", key = "#id"),
            @CacheEvict(value = "order_stats", allEntries = true)
    })
    public OrderResponse confirmPaid(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentTime(AppTime.now());

        Order saved = orderRepository.save(order);
        recalcTableStatus(order);
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        ordersCompletedCounter.increment();
        return orderMapper.toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
                    "stats_dish_trend", "stats_dashboard" }, allEntries = true),
            @CacheEvict(value = "order_by_id", key = "#id"),
            @CacheEvict(value = "order_stats", allEntries = true)
    })
    public OrderResponse cancelOrder(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        recalcTableStatus(order);
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        return orderMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void deleteOrder(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        DiningTable table = order.getTable();
        if (table != null) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        orderRepository.delete(Objects.requireNonNull(order));
        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
    }

    @Transactional
    public void tryAutoPromoteOrder(Order order) {
        if (order.getStatus() != Order.OrderStatus.PENDING
                && order.getStatus() != Order.OrderStatus.SERVING
                && order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT) {
            return;
        }

        if (order.getOrderItems().isEmpty()) {
            return;
        }

        boolean allDone = order.getOrderItems().stream()
                .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.FINISHED
                        || item.getStatus() == OrderItem.OrderItemStatus.CANCELLED);

        boolean anyCookingOrFinished = order.getOrderItems().stream()
                .anyMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.COOKING
                        || item.getStatus() == OrderItem.OrderItemStatus.FINISHED);

        if (allDone && order.getStatus() != Order.OrderStatus.AWAITING_PAYMENT) {
            order.setStatus(Order.OrderStatus.AWAITING_PAYMENT);
            orderRepository.save(order);
            log.info("Order #{} auto-promoted to AWAITING_PAYMENT (all items done)", order.getId());
        } else if (!allDone && order.getStatus() == Order.OrderStatus.AWAITING_PAYMENT) {
            order.setStatus(Order.OrderStatus.SERVING);
            orderRepository.save(order);
            log.info("Order #{} auto-demoted to SERVING (items reverted to incomplete)", order.getId());
        } else if (anyCookingOrFinished && order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.SERVING);
            orderRepository.save(order);
            log.info("Order #{} auto-promoted to SERVING (kitchen started prep)", order.getId());
        }
    }

    public void recalcTableStatus(Order order) {
        DiningTable table = order.getTable();
        if (table == null) {
            return;
        }

        if (order.getOrderItems().isEmpty() || order.getStatus() == Order.OrderStatus.CANCELLED
                || order.getStatus() == Order.OrderStatus.COMPLETED) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
        } else if (order.getStatus() == Order.OrderStatus.AWAITING_PAYMENT) {
            table.setStatus(DiningTable.TableStatus.WAITING_FOR_PAYMENT);
        } else {
            boolean allDone = order.getOrderItems().stream()
                    .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.FINISHED
                            || item.getStatus() == OrderItem.OrderItemStatus.CANCELLED);
            table.setStatus(allDone ? DiningTable.TableStatus.WAITING_FOR_PAYMENT : DiningTable.TableStatus.OCCUPIED);
        }
        tableRepository.save(table);
        notificationService.notifyTableChange();
    }
}
