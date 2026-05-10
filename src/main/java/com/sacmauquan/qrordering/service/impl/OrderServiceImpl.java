package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;
import com.sacmauquan.qrordering.service.DiscountService;
import com.sacmauquan.qrordering.service.NotificationService;
import com.sacmauquan.qrordering.service.OrderService;
import com.sacmauquan.qrordering.state.OrderState;
import com.sacmauquan.qrordering.state.OrderStateFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OrderServiceImpl - Quản lý quy trình đặt món và thanh toán.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository tableRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;
    private final DiscountService discountService;
    private final OrderStateFactory orderStateFactory;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get all orders
     */
    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithDetails().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get order history
     */
    @Override
    public Page<OrderResponse> getOrderHistory(String status, LocalDateTime startDate,
            LocalDateTime endDate, String search, @NonNull Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), Order.OrderStatus.valueOf(status.toUpperCase())));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (search != null && !search.isBlank()) {
                Join<Order, DiningTable> tableJoin = root.join("table", JoinType.LEFT);
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate idPred = cb.like(cb.lower(root.get("id").as(String.class)), pattern);
                Predicate tablePred = cb.like(cb.lower(tableJoin.get("tableNumber")), pattern);
                predicates.add(cb.or(idPred, tablePred));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec, pageable).map(this::convertToResponse);
    }

    /**
     * Get order stats
     */
    @Override
    public Map<String, Object> getOrderStats(String status, LocalDateTime startDate, LocalDateTime endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Order> root = cq.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(root.get("status"), Order.OrderStatus.valueOf(status.toUpperCase())));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        cq.multiselect(
                cb.count(root),
                cb.coalesce(cb.sum(root.get("totalAmount")), BigDecimal.ZERO))
                .where(cb.and(predicates.toArray(new Predicate[0])));

        Object[] result = entityManager.createQuery(cq).getSingleResult();

        return Map.of(
                "totalOrders", result[0],
                "totalRevenue", result[1]);
    }

    /**
     * Update order status
     */
    @Override
    @Transactional
    public OrderResponse updateStatus(@NonNull Long id, @NonNull String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found order"));

        OrderState state = orderStateFactory.getState(status.toUpperCase());
        state.handleRequest(order);

        Order saved = orderRepository.save(Objects.requireNonNull(order));

        // Handle table status when cancelled
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            DiningTable table = order.getTable();
            if (table != null) {
                table.setStatus(DiningTable.TableStatus.AVAILABLE);
                tableRepository.save(table);
            }
        } else {
            recalcTableStatus(order);
        }

        notificationService.notifyOrderChange();
        return convertToResponse(saved);
    }

    /**
     * Create order
     */
    @Override
    @Transactional
    public OrderResponse createOrder(@NonNull OrderRequest req) {
        if ((req.getItems() == null || req.getItems().isEmpty()) &&
                (req.getCombos() == null || req.getCombos().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must have at least one item or combo");
        }

        DiningTable table = resolveTable(req);

        // Prevent duplicate orders
        Order order = orderRepository.findFirstByTableIdAndStatusForUpdate(table.getId(), Order.OrderStatus.PENDING)
                .orElse(null);

        if (order == null) {
            order = Order.builder()
                    .table(table)
                    .status(Order.OrderStatus.PENDING)
                    .paymentStatus(Order.PaymentStatus.PENDING)
                    .orderType(Order.OrderType.DINE_IN)
                    .originalTotal(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .discountVoucher(BigDecimal.ZERO)
                    .build();
        }

        processItems(req, order);
        processCombos(req, order);

        recalculateOrderTotals(order);

        Order saved = orderRepository.save(Objects.requireNonNull(order));

        // Update table status
        table.setStatus(DiningTable.TableStatus.OCCUPIED);
        tableRepository.save(table);

        notificationService.notifyOrderChange();
        return convertToResponse(saved);
    }

    /**
     * Cancel order item
     */
    @Override
    @Transactional
    public void cancelOrderItem(@NonNull Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found item in order"));

        if (item.isPrepared()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is prepared, cannot cancel");
        }

        Order order = item.getOrder();
        order.getOrderItems().remove(item);
        orderItemRepository.delete(item);

        if (order.getOrderItems().isEmpty()) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            DiningTable table = order.getTable();
            if (table != null) {
                table.setStatus(DiningTable.TableStatus.AVAILABLE);
                tableRepository.save(table);
            }
        } else {
            recalculateOrderTotals(order);
            recalcTableStatus(order);
        }

        orderRepository.save(order);
        notificationService.notifyOrderChange();
    }

    /**
     * Update order item status
     */
    @Override
    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found order item"));

        try {
            OrderItem.OrderItemStatus status = OrderItem.OrderItemStatus.valueOf(newStatus.toUpperCase());
            item.setStatus(status);
            item.setPrepared(status == OrderItem.OrderItemStatus.READY || status == OrderItem.OrderItemStatus.SERVED);
            orderItemRepository.save(item);

            recalcTableStatus(item.getOrder());
            notificationService.notifyOrderChange();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order item status: " + newStatus);
        }
    }

    /**
     * Mark order item as prepared
     */
    @Override
    @Transactional
    public void markItemPrepared(@NonNull Long itemId) {
        updateItemStatus(itemId, "READY");
    }

    /**
     * Get kitchen orders
     */
    @Override
    public List<OrderResponse> getKitchenOrders() {
        return orderRepository.findByStatusIn(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING)).stream()
                .filter(o -> o.getOrderItems().stream()
                        .anyMatch(oi -> oi.getStatus() == OrderItem.OrderItemStatus.PENDING
                                || oi.getStatus() == OrderItem.OrderItemStatus.COOKING))
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get active orders
     */
    @Override
    public List<OrderResponse> getActiveOrders() {
        return orderRepository.findByStatusIn(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING)).stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Get current order by table
     */
    @Override
    public Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId) {
        return orderRepository
                .findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId,
                        List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING))
                .map(this::convertToResponse);
    }

    /**
     * Update order item
     */
    @Override
    @Transactional
    public OrderResponse updateOrderItem(@NonNull Long itemId, int quantity, String notes) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found order item"));

        if (item.isPrepared()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item is prepared, cannot edit");
        }

        item.setQuantity(quantity);
        item.setNotes(notes);
        orderItemRepository.save(item);

        Order order = item.getOrder();
        recalculateOrderTotals(order);
        orderRepository.save(Objects.requireNonNull(order));

        notificationService.notifyOrderChange();
        return convertToResponse(order);
    }

    /**
     * Pay order
     */
    @Override
    @Transactional
    public String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found order"));

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is already paid");
        }

        boolean hasUnprepared = order.getOrderItems().stream().anyMatch(item -> !item.isPrepared());
        if (hasUnprepared) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order is not prepared yet, cannot pay");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found user"));

        // Apply Voucher (if any)
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
        order.setPaymentTime(LocalDateTime.now());

        orderRepository.save(Objects.requireNonNull(order));

        DiningTable table = order.getTable();
        if (table != null) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        notificationService.notifyOrderChange();
        log.info("Paid successfully order ID: {} by: {}", id, currentUser.getFullName());

        return "Paid successfully";
    }

    /**
     * Preview order
     */
    @Override
    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(@NonNull OrderRequest req) {
        BigDecimal subtotalItems = BigDecimal.ZERO;
        BigDecimal subtotalCombos = BigDecimal.ZERO;

        if (req.getItems() != null) {
            for (OrderRequest.OrderItemRequest it : req.getItems()) {
                MenuItem mi = menuItemRepository.findById(Objects.requireNonNull(it.getMenuItemId()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Not found menu item: " + it.getMenuItemId()));

                BigDecimal optionsPrice = BigDecimal.ZERO;
                if (it.getSelectedOptionValueIds() != null && !it.getSelectedOptionValueIds().isEmpty()) {
                    optionsPrice = itemOptionValueRepository
                            .findAllById(Objects.requireNonNull(it.getSelectedOptionValueIds())).stream()
                            .map(ItemOptionValue::getExtraPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }

                BigDecimal unitTotal = mi.getPrice().add(optionsPrice);
                subtotalItems = subtotalItems.add(unitTotal.multiply(BigDecimal.valueOf(it.getQuantity())));
            }
        }

        if (req.getCombos() != null) {
            for (OrderRequest.OrderComboRequest cr : req.getCombos()) {
                Combo combo = comboRepository.findById(Objects.requireNonNull(cr.getComboId()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Combo không tồn tại: " + cr.getComboId()));

                subtotalCombos = subtotalCombos.add(combo.getPrice().multiply(BigDecimal.valueOf(cr.getQuantity())));
            }
        }

        BigDecimal subtotal = subtotalItems.add(subtotalCombos);
        BigDecimal discountVoucher = BigDecimal.ZERO;
        boolean voucherValid = false;
        String voucherMessage = "";

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            VoucherValidateResponse vr = discountService.validateCode(req.getVoucherCode(), subtotal);

            voucherValid = vr.applicable();
            voucherMessage = vr.status();
            discountVoucher = vr.discountValue();
        }

        BigDecimal finalTotal = subtotal.subtract(discountVoucher).max(BigDecimal.ZERO);

        return OrderPreviewResponse.builder()
                .subtotalItems(subtotalItems)
                .subtotalCombos(subtotalCombos)
                .originalTotal(subtotal)
                .discountVoucher(discountVoucher)
                .finalTotal(finalTotal)
                .voucherValid(voucherValid)
                .voucherMessage(voucherMessage)
                .build();
    }

    // Helpers

    /**
     * Resolve table
     */
    private DiningTable resolveTable(OrderRequest req) {
        if (req.getTableCode() != null && !req.getTableCode().isBlank()) {
            return tableRepository.findByTableCode(req.getTableCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found table code"));
        } else if (req.getTableId() != null) {
            return tableRepository.findById(Objects.requireNonNull(req.getTableId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found table"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing table information");
    }

    /**
     * Process items
     */
    private void processItems(OrderRequest req, Order order) {
        if (req.getItems() == null)
            return;

        for (OrderRequest.OrderItemRequest itReq : req.getItems()) {
            MenuItem mi = menuItemRepository.findById(Objects.requireNonNull(itReq.getMenuItemId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found menu item"));

            validateRequiredOptions(mi, itReq.getSelectedOptionValueIds());

            List<ItemOptionValue> selectedValues = itReq.getSelectedOptionValueIds() != null
                    ? itemOptionValueRepository.findAllById(Objects.requireNonNull(itReq.getSelectedOptionValueIds()))
                    : List.of();

            BigDecimal extraPrice = selectedValues.stream()
                    .map(ItemOptionValue::getExtraPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitPrice = mi.getPrice().add(extraPrice);

            // Find duplicate item to merge
            Optional<OrderItem> existing = order.getOrderItems().stream()
                    .filter(oi -> oi.getMenuItem() != null && oi.getMenuItem().getId().equals(mi.getId()))
                    .filter(oi -> Objects.equals(oi.getNotes(), itReq.getNotes()))
                    .filter(oi -> !oi.isPrepared())
                    .filter(oi -> checkOptionsMatch(oi.getOrderItemOptions(), itReq.getSelectedOptionValueIds()))
                    .findFirst();

            if (existing.isPresent()) {
                existing.get().setQuantity(existing.get().getQuantity() + itReq.getQuantity());
            } else {
                OrderItem oi = OrderItem.builder()
                        .order(order)
                        .menuItem(mi)
                        .unitPrice(unitPrice)
                        .quantity(itReq.getQuantity())
                        .notes(itReq.getNotes())
                        .status(OrderItem.OrderItemStatus.PENDING)
                        .build();

                selectedValues.forEach(val -> oi.getOrderItemOptions().add(OrderItemOption.builder()
                        .orderItem(oi)
                        .optionName(val.getItemOption().getName())
                        .optionValueName(val.getName())
                        .extraPrice(val.getExtraPrice())
                        .itemOptionValue(val)
                        .build()));

                order.getOrderItems().add(oi);
            }
        }
    }

    private void processCombos(OrderRequest req, Order order) {
        if (req.getCombos() == null)
            return;

        for (OrderRequest.OrderComboRequest comboReq : req.getCombos()) {
            Combo combo = comboRepository.findById(Objects.requireNonNull(comboReq.getComboId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found combo"));

            Optional<OrderItem> existing = order.getOrderItems().stream()
                    .filter(oi -> oi.getCombo() != null && oi.getCombo().getId().equals(combo.getId()))
                    .filter(oi -> Objects.equals(oi.getNotes(), comboReq.getNotes()))
                    .filter(oi -> !oi.isPrepared())
                    .findFirst();

            if (existing.isPresent()) {
                existing.get().setQuantity(existing.get().getQuantity() + comboReq.getQuantity());
            } else {
                order.getOrderItems().add(OrderItem.builder()
                        .order(order)
                        .combo(combo)
                        .unitPrice(combo.getPrice())
                        .quantity(comboReq.getQuantity())
                        .notes(comboReq.getNotes())
                        .status(OrderItem.OrderItemStatus.PENDING)
                        .build());
            }
        }
    }

    private void validateRequiredOptions(MenuItem mi, List<Long> selectedValueIds) {
        Set<Long> selectedIds = selectedValueIds != null ? new HashSet<>(selectedValueIds) : Set.of();
        mi.getItemOptions().stream()
                .filter(ItemOption::isRequired)
                .forEach(opt -> {
                    boolean ok = opt.getOptionValues().stream().anyMatch(val -> selectedIds.contains(val.getId()));
                    if (!ok) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select: " + opt.getName());
                    }
                });
    }

    /**
     * Check if options match
     */
    private boolean checkOptionsMatch(Collection<OrderItemOption> existing, List<Long> incomingIds) {
        Set<Long> existIds = existing.stream()
                .map(o -> o.getItemOptionValue().getId())
                .collect(Collectors.toSet());
        Set<Long> inIds = incomingIds != null ? new HashSet<>(incomingIds) : Set.of();
        return existIds.equals(inIds);
    }

    /**
     * Recalculate order totals
     */
    private void recalculateOrderTotals(Order order) {
        BigDecimal subtotal = order.getOrderItems().stream()
                .map(oi -> oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setOriginalTotal(subtotal);
        order.setTotalAmount(subtotal.subtract(order.getDiscountVoucher()).max(BigDecimal.ZERO));
    }

    /**
     * Recalculate table status
     */
    private void recalcTableStatus(Order order) {
        DiningTable table = order.getTable();
        if (table == null)
            return;

        if (order.getOrderItems().isEmpty() || order.getStatus() == Order.OrderStatus.CANCELLED) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
        } else {
            boolean allServed = order.getOrderItems().stream()
                    .allMatch(oi -> oi.getStatus() == OrderItem.OrderItemStatus.SERVED
                            || oi.getStatus() == OrderItem.OrderItemStatus.READY);
            table.setStatus(allServed ? DiningTable.TableStatus.WAITING_FOR_PAYMENT : DiningTable.TableStatus.OCCUPIED);
        }
        tableRepository.save(table);
    }

    /**
     * Convert order to response
     */
    private OrderResponse convertToResponse(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getStatus().name(),
                o.getOriginalTotal(),
                o.getDiscountVoucher(),
                o.getVoucherCode(),
                o.getTotalAmount(),
                o.getOrderType().name(),
                o.getPaymentStatus().name(),
                o.getPaymentMethod() != null ? o.getPaymentMethod().name() : null,
                o.getPaidBy() != null ? o.getPaidBy().getFullName() : null,
                o.getPaymentTime(),
                o.getTable() != null
                        ? new OrderResponse.TableSummary(o.getTable().getId(), o.getTable().getTableNumber())
                        : null,
                o.getOrderItems().stream().map(oi -> new OrderResponse.OrderItemResponse(
                        oi.getId(),
                        oi.getCombo() != null ? "[Combo] " + oi.getCombo().getName()
                                : (oi.getMenuItem() != null ? oi.getMenuItem().getName() : "N/A"),
                        oi.getUnitPrice(),
                        oi.getQuantity(),
                        oi.getNotes(),
                        oi.isPrepared(),
                        oi.getStatus().name(),
                        oi.getOrderItemOptions().stream().map(opt -> new OrderResponse.OrderItemOptionResponse(
                                opt.getOptionName(), opt.getOptionValueName(), opt.getExtraPrice())).toList()))
                        .toList(),
                o.getCreatedAt());
    }
}
