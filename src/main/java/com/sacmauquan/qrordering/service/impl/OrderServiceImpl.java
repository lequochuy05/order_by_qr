package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;
import com.sacmauquan.qrordering.service.DiscountService;
import com.sacmauquan.qrordering.service.NotificationService;
import com.sacmauquan.qrordering.service.OrderService;
import com.sacmauquan.qrordering.service.PayosService;
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
import org.springframework.cache.annotation.CacheEvict;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OrderServiceImpl - Comprehensive implementation for managing the restaurant
 * ordering lifecycle.
 * Coordinates between table states, kitchen preparation, and promotional
 * discount application.
 * Implements a state-driven approach for order transitions and real-time
 * WebSocket notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository tableRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;
    private final DiscountService discountService;
    private final PaymentTransactionRepository transactionRepository;
    private final PayosService payosService;
    private final OrderStateFactory orderStateFactory;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Fetches all orders with optimized details pre-fetching.
     */
    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithDetails().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Provides a searchable order history view for administrators.
     */
    @Override
    public Page<OrderResponse> getOrderHistory(String status, LocalDate startDate,
            LocalDate endDate, String orderId, String tableNumber, @NonNull Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            boolean isCountQuery = resultType == Long.class || resultType == long.class;

            if (!isCountQuery) {
                root.fetch("table", JoinType.LEFT);
            }

            List<Predicate> predicates = buildPredicates(root, query, cb, status, startDate, endDate, orderId,
                    tableNumber);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(this::convertToResponse);
    }

    private List<Predicate> buildPredicates(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb, String status,
            LocalDate startDate, LocalDate endDate, String orderId, String tableNumber) {
        List<Predicate> predicates = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            try {
                predicates.add(cb.equal(root.get("status"), Order.OrderStatus.valueOf(status.toUpperCase())));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", status);
            }
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
        }

        if (orderId != null && !orderId.isBlank()) {
            try {
                // Parse ID user from text to Long
                Long parsedId = Long.parseLong(orderId.trim());
                predicates.add(cb.equal(root.get("id"), parsedId));
            } catch (NumberFormatException e) {
                // If user enter invalid ID -> Return no result
                predicates.add(cb.disjunction());
            }
        }

        if (tableNumber != null && !tableNumber.isBlank()) {
            String pattern = "%" + tableNumber.trim().toLowerCase() + "%";

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Order> subRoot = subquery.from(Order.class);
            Join<Order, DiningTable> subTable = subRoot.join("table"); // Inner join ở subquery

            subquery.select(subRoot.get("id"))
                    .where(cb.like(cb.lower(subTable.get("tableNumber")), pattern));

            predicates.add(root.get("id").in(subquery));
        }
        return predicates;
    }

    /**
     * Generates a high-level summary of restaurant performance over a specified
     * period.
     */
    @Override
    public Map<String, Object> getOrderStats(String status, LocalDate startDate, LocalDate endDate, String orderId,
            String tableNumber) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Order> root = cq.from(Order.class);

        List<Predicate> predicates = buildPredicates(root, cq, cb, status, startDate, endDate, orderId, tableNumber);

        cq.multiselect(
                cb.count(root),
                cb.coalesce(cb.sum(root.get("totalAmount")), BigDecimal.ZERO))
                .where(cb.and(predicates.toArray(new Predicate[0])));

        Object[] result = entityManager.createQuery(cq).getSingleResult();

        return Map.of(
                "totalOrders", result[0],
                "totalRevenue", result[1]);
    }

    @Override
    @Transactional
    public OrderResponse reconcileOrder(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // Nếu đã hoàn thành hoặc đã hủy thì không cần tra soát sâu
        if (order.getStatus() == Order.OrderStatus.COMPLETED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            return convertToResponse(order);
        }

        // Tìm giao dịch gần nhất của đơn hàng này
        Optional<PaymentTransaction> latestTx = transactionRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(order.getId());

        if (latestTx.isPresent()) {
            PaymentTransaction tx = latestTx.get();
            // Nếu là PayOS và đang chờ, thử đồng bộ trạng thái thực tế
            if (tx.getPaymentMethod() == PaymentTransaction.PaymentMethod.PAYOS
                    && tx.getStatus() == PaymentTransaction.TransactionStatus.PENDING) {
                payosService.syncPaymentStatus(tx.getId());
                // Reload order after sync
                order = orderRepository.findById(id).orElse(order);
            }
        }

        return convertToResponse(order);
    }

    /**
     * Updates order status by delegating to specialized OrderState handlers.
     */
    @Override
    @Transactional
    @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance", "stats_dish_trend" }, allEntries = true)
    public OrderResponse updateStatus(@NonNull Long id, @NonNull String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        OrderState state = orderStateFactory.getState(status.toUpperCase());
        state.handleRequest(order);

        Order saved = orderRepository.save(Objects.requireNonNull(order));

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
     * Orchestrates new order creation and manages item merging for active sessions.
     */
    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public OrderResponse createOrder(@NonNull OrderRequest req) {
        if ((req.getItems() == null || req.getItems().isEmpty()) &&
                (req.getCombos() == null || req.getCombos().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order content cannot be empty");
        }

        DiningTable table = resolveTable(req);

        // Find existing PENDING order to merge, ensuring session persistence for the
        // same table
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

        table.setStatus(DiningTable.TableStatus.OCCUPIED);
        tableRepository.save(table);

        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        log.info("Order processed for Table {}: ID #{}", table.getTableNumber(), saved.getId());
        return convertToResponse(saved);
    }

    /**
     * Cancels a line item and updates the financial summary of the order.
     */
    @Override
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
     * Updates kitchen preparation status for a specific item.
     */
    @Override
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        try {
            OrderItem.OrderItemStatus status = OrderItem.OrderItemStatus.valueOf(newStatus.toUpperCase());
            item.setStatus(status);
            item.setPrepared(status == OrderItem.OrderItemStatus.READY || status == OrderItem.OrderItemStatus.SERVED
                    || status == OrderItem.OrderItemStatus.FINISHED);
            orderItemRepository.save(item);

            recalcTableStatus(item.getOrder());
            notificationService.notifyOrderChange();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item status code: " + newStatus);
        }
    }

    /**
     * Shortcut to mark an item as READY for service.
     */
    @Override
    @Transactional
    public void markItemPrepared(@NonNull Long itemId) {
        updateItemStatus(itemId, "READY");
    }

    /**
     * Retrieves all items requiring attention in the kitchen views.
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
     * Lists active orders currently occupying tables.
     */
    @Override
    public List<OrderResponse> getActiveOrders() {
        return orderRepository.findByStatusIn(List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING)).stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Locates the active session for a table.
     */
    @Override
    public Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId) {
        return orderRepository
                .findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId,
                        List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING))
                .map(this::convertToResponse);
    }

    /**
     * Adjusts quantity or special notes for an item before preparation starts.
     */
    @Override
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
        recalculateOrderTotals(order);
        orderRepository.save(Objects.requireNonNull(order));

        notificationService.notifyOrderChange();
        return convertToResponse(order);
    }

    /**
     * Finalizes order payment (CASH flow) and closes the session.
     */
    @Override
    @Transactional
    @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
            "stats_dish_trend" }, allEntries = true)
    public String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This order is already settled");
        }

        // Integrity check: All items should be prepared before final settlement
        boolean hasUnprepared = order.getOrderItems().stream().anyMatch(item -> !item.isPrepared());
        if (hasUnprepared) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outstanding items in preparation. Complete kitchen tasks first.");
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
        order.setPaymentTime(LocalDateTime.now());

        orderRepository.save(Objects.requireNonNull(order));

        DiningTable table = order.getTable();
        if (table != null) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        log.info("Order #{} settled via CASH by Staff Member: {}", id, currentUser.getFullName());

        return "Order settled successfully";
    }

    /**
     * Calculates order financials without persisting data.
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
                                "Menu item not found: " + it.getMenuItemId()));

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
                                "Combo not found: " + cr.getComboId()));

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

    /**
     * Administrative override to confirm payment.
     */
    @Override
    @Transactional
    @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
            "stats_dish_trend" }, allEntries = true)
    public OrderResponse confirmPaid(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setPaymentTime(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        DiningTable table = order.getTable();
        if (table != null) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        return convertToResponse(saved);
    }

    /**
     * Terminates an active order session.
     */
    @Override
    @Transactional
    @CacheEvict(value = { "tables", "stats_revenue", "stats_top_dishes", "stats_emp_performance",
            "stats_dish_trend" }, allEntries = true)
    public OrderResponse cancelOrder(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        DiningTable table = order.getTable();
        if (table != null) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }

        notificationService.notifyOrderChange();
        notificationService.notifyTableChange();
        return convertToResponse(saved);
    }

    /**
     * Finds a detailed order view by ID.
     */
    @Override
    public OrderResponse getOrderById(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return convertToResponse(order);
    }

    /**
     * Provides a financial snapshot of a table's current order.
     */
    @Override
    public OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId) {
        Order order = orderRepository.findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId,
                List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active session for this table"));

        return OrderPreviewResponse.builder()
                .originalTotal(order.getOriginalTotal())
                .discountVoucher(order.getDiscountVoucher())
                .finalTotal(order.getTotalAmount())
                .build();
    }

    /**
     * Deletes an order record permanently.
     */
    @Override
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

    // --- Private Orchestration Helpers ---

    private DiningTable resolveTable(OrderRequest req) {
        if (req.getTableCode() != null && !req.getTableCode().isBlank()) {
            return tableRepository.findByTableCode(req.getTableCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Secure Table Code invalid"));
        } else if (req.getTableId() != null) {
            return tableRepository.findById(Objects.requireNonNull(req.getTableId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table ID invalid"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table identification required for order creation");
    }

    /**
     * Processes all items in an order request.
     * 
     * @param req   The order request containing item details
     * @param order The order entity to add items to
     */
    private void processItems(OrderRequest req, Order order) {
        if (req.getItems() == null)
            return;

        for (OrderRequest.OrderItemRequest itReq : req.getItems()) {
            MenuItem mi = menuItemRepository.findById(Objects.requireNonNull(itReq.getMenuItemId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));

            validateRequiredOptions(mi, itReq.getSelectedOptionValueIds());

            List<ItemOptionValue> selectedValues = itReq.getSelectedOptionValueIds() != null
                    ? itemOptionValueRepository.findAllById(Objects.requireNonNull(itReq.getSelectedOptionValueIds()))
                    : List.of();

            BigDecimal extraPrice = selectedValues.stream()
                    .map(ItemOptionValue::getExtraPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unitPrice = mi.getPrice().add(extraPrice);

            // Attempt to merge with an existing unprepared item to keep the bill concise
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

    /**
     * Processes combo meals in an order request.
     * 
     * @param req   The order request containing combo details
     * @param order The order entity to add combos to
     */
    private void processCombos(OrderRequest req, Order order) {
        if (req.getCombos() == null)
            return;

        for (OrderRequest.OrderComboRequest comboReq : req.getCombos()) {
            Combo combo = comboRepository.findById(Objects.requireNonNull(comboReq.getComboId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Combo not found"));

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

    /**
     * Validates that all required options for a menu item are selected.
     * 
     * @param mi               The menu item to validate
     * @param selectedValueIds The list of selected option value IDs
     * @throws ResponseStatusException if required options are missing
     */
    private void validateRequiredOptions(MenuItem mi, List<Long> selectedValueIds) {
        Set<Long> selectedIds = selectedValueIds != null ? new HashSet<>(selectedValueIds) : Set.of();
        mi.getItemOptions().stream()
                .filter(ItemOption::isRequired)
                .forEach(opt -> {
                    boolean ok = opt.getOptionValues().stream().anyMatch(val -> selectedIds.contains(val.getId()));
                    if (!ok) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Required option selection missing: " + opt.getName());
                    }
                });
    }

    /**
     * Checks if the selected option values match the existing option values in an
     * order item.
     * 
     * @param existing    The existing option values
     * @param incomingIds The incoming option value IDs
     * @return true if the option values match, false otherwise
     */
    private boolean checkOptionsMatch(Collection<OrderItemOption> existing, List<Long> incomingIds) {
        Set<Long> existIds = existing.stream()
                .map(o -> o.getItemOptionValue().getId())
                .collect(Collectors.toSet());
        Set<Long> inIds = incomingIds != null ? new HashSet<>(incomingIds) : Set.of();
        return existIds.equals(inIds);
    }

    /**
     * Recalculates the total amounts for an order based on its items.
     * 
     * @param order The order to recalculate totals for
     */
    private void recalculateOrderTotals(Order order) {
        BigDecimal subtotal = order.getOrderItems().stream()
                .map(oi -> oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setOriginalTotal(subtotal);
        order.setTotalAmount(subtotal.subtract(order.getDiscountVoucher()).max(BigDecimal.ZERO));
    }

    /**
     * Recalculates the status of the table based on the order.
     * 
     * @param order The order to recalculate table status for
     */
    private void recalcTableStatus(Order order) {
        DiningTable table = order.getTable();
        if (table == null)
            return;

        if (order.getOrderItems().isEmpty() || order.getStatus() == Order.OrderStatus.CANCELLED
                || order.getStatus() == Order.OrderStatus.COMPLETED) {
            table.setStatus(DiningTable.TableStatus.AVAILABLE);
        } else {
            // Determine if the table is ready for billing or still being served
            boolean allServed = order.getOrderItems().stream()
                    .allMatch(oi -> oi.getStatus() == OrderItem.OrderItemStatus.SERVED
                            || oi.getStatus() == OrderItem.OrderItemStatus.READY
                            || oi.getStatus() == OrderItem.OrderItemStatus.FINISHED);
            table.setStatus(allServed ? DiningTable.TableStatus.WAITING_FOR_PAYMENT : DiningTable.TableStatus.OCCUPIED);
        }
        tableRepository.save(table);
        notificationService.notifyTableChange();
    }

    /**
     * Converts an order entity to an order response.
     * 
     * @param o The order entity to convert
     * @return The order response
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
                        oi.getMenuItem() != null ? new OrderResponse.MenuItemSummary(
                                oi.getMenuItem().getId(),
                                oi.getMenuItem().getName(),
                                oi.getMenuItem().getCategory() != null
                                        ? new OrderResponse.CategorySummary(oi.getMenuItem().getCategory().getName())
                                        : null)
                                : null,
                        oi.getCombo() != null ? new OrderResponse.ComboSummary(
                                oi.getCombo().getId(),
                                oi.getCombo().getName(),
                                oi.getCombo().getPrice()) : null,
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
