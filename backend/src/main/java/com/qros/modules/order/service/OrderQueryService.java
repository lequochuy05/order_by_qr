package com.qros.modules.order.service;

import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.order.dto.OrderPreviewResponse;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.payment.service.PayosService;
import com.qros.modules.table.model.DiningTable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PayosService payosService;
    private final OrderMapper orderMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithDetails().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public Page<OrderResponse> getOrderHistory(String status, LocalDate startDate,
            LocalDate endDate, String orderId, String tableNumber, @NonNull Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = buildPredicates(root, query, cb, status, startDate, endDate, orderId,
                    tableNumber);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<Long> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, orderPage.getTotalElements());
        }

        Map<Long, Order> ordersById = orderRepository.findDistinctByIdIn(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<OrderResponse> responses = orderIds.stream()
                .map(ordersById::get)
                .filter(java.util.Objects::nonNull)
                .map(orderMapper::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    @Cacheable(value = "order_stats")
    public Map<String, Object> getOrderStats(String status, LocalDate startDate, LocalDate endDate, String orderId,
            String tableNumber) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Order> root = cq.from(Order.class);

        List<Predicate> predicates = buildPredicates(root, cq, cb, status, startDate, endDate, orderId, tableNumber);

        cq.multiselect(
                cb.count(root),
                cb.coalesce(cb.sum(root.get("totalAmount")), BigDecimal.ZERO));

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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == Order.OrderStatus.COMPLETED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            return orderMapper.toResponse(order);
        }

        Optional<PaymentTransaction> latestTx = transactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
        if (latestTx.isPresent()) {
            PaymentTransaction tx = latestTx.get();
            if (tx.getPaymentMethod() == PaymentTransaction.PaymentMethod.PAYOS
                    && tx.getStatus() == PaymentTransaction.TransactionStatus.PENDING) {
                payosService.syncPaymentStatus(tx.getId());
                order = orderRepository.findById(id).orElse(order);
            }
        }

        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> getActiveOrders() {
        return orderRepository.findByStatusIn(activeStatuses()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId) {
        return orderRepository
                .findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses())
                .map(orderMapper::toResponse);
    }

    public Optional<PublicMenuResponse.Order> getPublicCurrentOrderByTable(@NonNull Long tableId) {
        return orderRepository
                .findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses())
                .map(orderMapper::toPublicResponse);
    }

    @Cacheable(value = "order_by_id", key = "#id")
    public OrderResponse getOrderById(@NonNull Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return orderMapper.toResponse(order);
    }

    public OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId) {
        Order order = orderRepository.findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active session for this table"));

        return OrderPreviewResponse.builder()
                .originalTotal(order.getOriginalTotal())
                .discountVoucher(order.getDiscountVoucher())
                .finalTotal(order.getTotalAmount())
                .build();
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

            subquery.select(subRoot.get("id"))
                    .where(cb.like(cb.lower(subTable.get("tableNumber")), pattern));

            predicates.add(root.get("id").in(subquery));
        }
        return predicates;
    }

    private List<Order.OrderStatus> activeStatuses() {
        return List.of(Order.OrderStatus.PENDING, Order.OrderStatus.SERVING, Order.OrderStatus.AWAITING_PAYMENT);
    }
}
