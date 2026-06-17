package com.qros.modules.order.service;

import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.PublicOrderResponse;
import com.qros.modules.order.dto.response.TableBoardResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.payment.service.PaymentService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.repository.TableSessionRepository;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final OrderMapper orderMapper;
    private final DiningTableRepository tableRepository;
    private final TableSessionService tableSessionService;
    private final TableSessionRepository tableSessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Temporary full list query.
     * Prefer getOrderHistory(...) with Pageable for admin screens.
     */
    public Page<OrderResponse> getAllOrders(@NonNull Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<Long> orderIds = orderPage.getContent().stream().map(Order::getId).toList();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, orderPage.getTotalElements());
        }

        Map<Long, Order> ordersById = orderRepository.findDistinctByIdIn(orderIds).stream()
                .collect(
                        Collectors.toMap(Order::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<OrderResponse> responses = orderIds.stream()
                .map(ordersById::get)
                .filter(java.util.Objects::nonNull)
                .map(orderMapper::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    public Page<OrderResponse> getOrderHistory(
            String status,
            LocalDate from,
            LocalDate to,
            String orderId,
            String tableNumber,
            @NonNull Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = buildPredicates(root, query, cb, status, from, to, orderId, tableNumber);

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        List<Long> orderIds = orderPage.getContent().stream().map(Order::getId).toList();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, orderPage.getTotalElements());
        }

        Map<Long, Order> ordersById = orderRepository.findDistinctByIdIn(orderIds).stream()
                .collect(
                        Collectors.toMap(Order::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<OrderResponse> responses = orderIds.stream()
                .map(ordersById::get)
                .filter(java.util.Objects::nonNull)
                .map(orderMapper::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    @Cacheable(value = CacheNames.ORDER_STATS)
    public Map<String, Object> getOrderAnalytics(
            String status, LocalDate from, LocalDate to, String orderId, String tableNumber) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Order> root = cq.from(Order.class);

        List<Predicate> predicates = buildPredicates(root, cq, cb, status, from, to, orderId, tableNumber);

        cq.multiselect(cb.count(root), cb.coalesce(cb.sum(root.get("finalAmount")), BigDecimal.ZERO));

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        Object[] result = entityManager.createQuery(cq).getSingleResult();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", result[0]);
        stats.put("totalRevenue", result[1]);

        return stats;
    }

    @Transactional
    public OrderResponse reconcileOrder(@NonNull Long id) {
        Order order =
                orderRepository.findDetailById(id).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            return orderMapper.toResponse(order);
        }

        Optional<PaymentTransaction> latestTx =
                transactionRepository.findFirstByOrderIdAndPaymentMethodAndStatusOrderByCreatedAtDesc(
                        order.getId(), PaymentMethod.PAYOS, PaymentTransactionStatus.PENDING);

        if (latestTx.isPresent()) {
            paymentService.syncPaymentStatus(latestTx.get().getId());

            order = orderRepository.findDetailById(id).orElse(order);
        }

        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> getActiveOrders() {
        return orderRepository.findByStatusIn(activeStatuses()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public TableBoardResponse getTableBoard() {
        Map<Long, TableSession> openSessionsByTableId =
                tableSessionRepository.findByStatus(TableSessionStatus.OPEN).stream()
                        .filter(session -> session.getTable() != null)
                        .collect(Collectors.toMap(
                                session -> session.getTable().getId(), Function.identity(), (left, right) -> left));

        List<TableBoardResponse.TableItem> tables = tableRepository.findAllByOrderByTableNumberAsc().stream()
                .map(table -> {
                    TableSession session = openSessionsByTableId.get(table.getId());

                    return new TableBoardResponse.TableItem(
                            table.getId(),
                            table.getTableNumber(),
                            table.getTableCode(),
                            table.getStatus().name(),
                            table.getCapacity(),
                            table.getQrCodeUrl(),
                            session != null,
                            session != null ? session.getStatus().name() : null,
                            session != null ? session.getOpenedAt() : null,
                            session != null ? session.getLastActivityAt() : null,
                            table.getCreatedAt(),
                            table.getUpdatedAt());
                })
                .toList();

        List<TableBoardResponse.ActiveOrder> activeOrders =
                orderRepository.findActiveOrderSummariesForTableBoard().stream()
                        .map(order -> new TableBoardResponse.ActiveOrder(
                                order.getId(),
                                order.getStatus(),
                                order.getFinalAmount(),
                                order.getTableId(),
                                order.getTableNumber(),
                                order.getCreatedAt()))
                        .toList();

        return new TableBoardResponse(tables, activeOrders);
    }

    public Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId) {
        return orderRepository
                .findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses())
                .map(orderMapper::toResponse);
    }

    public Optional<PublicOrderResponse> getPublicCurrentOrderByTableCode(@NonNull String tableCode) {
        return orderRepository
                .findFirstByTable_TableCodeAndStatusInOrderByCreatedAtDesc(tableCode, activeStatuses())
                .map(orderMapper::toPublicResponse);
    }

    public Optional<PublicOrderResponse> getPublicCurrentOrderBySession(
            @NonNull String tableCode, @NonNull String sessionToken) {
        TableSession session = tableSessionService.requireSessionForRead(tableCode, sessionToken);

        if (!session.isOpen()) {
            return Optional.empty();
        }

        return orderRepository
                .findFirstByTableSessionIdAndStatusInOrderByCreatedAtDesc(session.getId(), activeStatuses())
                .map(orderMapper::toPublicResponse);
    }

    public Optional<PublicOrderResponse> getPublicCurrentOrderByTable(@NonNull Long tableId) {
        return orderRepository
                .findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses())
                .map(orderMapper::toPublicResponse);
    }

    @Cacheable(value = CacheNames.ORDER_BY_ID, key = "#id")
    public OrderResponse getOrderById(@NonNull Long id) {
        Order order =
                orderRepository.findDetailById(id).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return orderMapper.toResponse(order);
    }

    public OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId) {
        Order order = orderRepository
                .findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses())
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No active session for this table"));

        BigDecimal subtotalAmount = safe(order.getSubtotalAmount());
        BigDecimal discountAmount = safe(order.getDiscountAmount());
        BigDecimal finalAmount = safe(order.getFinalAmount());

        return new OrderPreviewResponse(
                subtotalAmount,
                BigDecimal.ZERO,
                subtotalAmount,
                discountAmount,
                finalAmount,
                false,
                "",
                discountAmount);
    }

    private List<Predicate> buildPredicates(
            Root<Order> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String status,
            LocalDate from,
            LocalDate to,
            String orderId,
            String tableNumber) {
        List<Predicate> predicates = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            try {
                predicates.add(cb.equal(
                        root.get("status"), OrderStatus.valueOf(status.trim().toUpperCase())));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid order status: {}", status);
            }
        }

        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
        }

        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(23, 59, 59)));
        }

        if (orderId != null && !orderId.isBlank()) {
            try {
                Long parsedId = Long.parseLong(orderId.trim());
                predicates.add(cb.equal(root.get("id"), parsedId));
            } catch (NumberFormatException e) {
                predicates.add(cb.disjunction());
            }
        }

        if (tableNumber != null && !tableNumber.isBlank()) {
            String pattern = "%" + tableNumber.trim().toLowerCase() + "%";

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Order> subRoot = subquery.from(Order.class);
            Join<Order, DiningTable> subTable = subRoot.join("table");

            subquery.select(subRoot.get("id")).where(cb.like(cb.lower(subTable.get("tableNumber")), pattern));

            predicates.add(root.get("id").in(subquery));
        }

        return predicates;
    }

    private List<OrderStatus> activeStatuses() {
        return List.of(OrderStatus.PENDING, OrderStatus.SERVING, OrderStatus.AWAITING_PAYMENT);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
