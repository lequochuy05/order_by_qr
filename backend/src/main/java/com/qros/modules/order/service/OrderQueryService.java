package com.qros.modules.order.service;

import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.dto.response.OrderResponse;
import com.qros.modules.order.dto.response.PublicOrderResponse;
import com.qros.modules.order.dto.response.TableBoardResponse;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.enums.TableSessionStatus;
import com.qros.modules.table.service.TableLookupService;
import com.qros.modules.table.service.TableSessionResolver;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.cache.CacheNames;
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
    private final OrderMapper orderMapper;
    private final TableLookupService tableLookupService;
    private final TableSessionService tableSessionService;
    private final TableSessionResolver tableSessionResolver;
    private final OrderPricingService orderPricingService;
    private final VoucherCheckoutService voucherCheckoutService;

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

        Predicate completedPaid = cb.and(
                cb.equal(root.get("status"), OrderStatus.COMPLETED),
                cb.equal(root.get("paymentStatus"), PaymentStatus.PAID));
        var revenueExpr = cb.<BigDecimal>selectCase()
                .when(completedPaid, root.get("finalAmount"))
                .otherwise(BigDecimal.ZERO);

        cq.multiselect(cb.count(root), cb.coalesce(cb.sum(revenueExpr), BigDecimal.ZERO));

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        Object[] result = entityManager.createQuery(cq).getSingleResult();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", result[0]);
        stats.put("totalRevenue", result[1]);

        return stats;
    }

    public List<OrderResponse> getActiveOrders() {
        return orderRepository.findByStatusIn(activeStatuses()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getActiveTakeawayOrders() {
        return orderRepository.findActiveOrdersByType(OrderType.TAKEAWAY, activeStatuses()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public TableBoardResponse getTableBoard() {
        return TableBoardAssembler.assemble(
                tableSessionResolver.findActiveBySessionStatus(TableSessionStatus.OPEN),
                tableLookupService.findAllTablesOrdered(),
                orderRepository.findActiveOrderSummariesForTableBoard(),
                tableSessionResolver);
    }

    public Optional<OrderResponse> getCurrentOrderByTable(@NonNull Long tableId) {
        return findLatestActiveOrderByTableId(tableId).map(orderMapper::toResponse);
    }

    public Optional<PublicOrderResponse> getPublicCurrentOrderByTableCode(@NonNull String tableCode) {
        Optional<TableSession> session = tableSessionResolver.findOpenSessionByTableCode(tableCode);
        if (session.isPresent()) {
            return orderRepository
                    .findFirstByTableSessionIdAndStatusInOrderByCreatedAtDesc(
                            session.get().getId(), activeStatuses())
                    .map(orderMapper::toPublicResponse);
        }

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
        return findLatestActiveOrderByTableId(tableId).map(orderMapper::toPublicResponse);
    }

    private Optional<Order> findLatestActiveOrderByTableId(Long tableId) {
        Optional<TableSession> session = tableSessionResolver.findOpenSessionByTableId(tableId);
        if (session.isPresent()) {
            return orderRepository.findFirstByTableSessionIdAndStatusInOrderByCreatedAtDesc(
                    session.get().getId(), activeStatuses());
        }
        return orderRepository.findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses());
    }

    @Cacheable(value = CacheNames.ORDER_ID_CACHE, key = "#id")
    public OrderResponse getOrderById(@NonNull Long id) {
        Order order =
                orderRepository.findDetailById(id).orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return orderMapper.toResponse(order);
    }

    public OrderPreviewResponse getOrderPreviewByTableId(@NonNull Long tableId) {
        Order order = tableSessionResolver
                .findOpenSessionByTableId(tableId)
                .flatMap(session -> orderRepository.findFirstByTableSessionIdAndStatusInOrderByCreatedAtDesc(
                        session.getId(), activeStatuses()))
                .or(() -> orderRepository.findFirstByTableIdAndStatusInOrderByCreatedAtDesc(tableId, activeStatuses()))
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "No active session for this table"));

        BigDecimal subtotalAmount = safe(order.getSubtotalAmount());
        BigDecimal discountPromotion = safe(orderPricingService.calculatePromotionDiscount(subtotalAmount));
        String voucherCode = order.getVoucherCode();
        boolean voucherValid = false;
        String voucherMessage = "";
        BigDecimal voucherDiscount = BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.isBlank()) {
            DiscountResult preview = voucherCheckoutService.previewVoucher(voucherCode, subtotalAmount);
            voucherValid = preview.voucherId() != null;
            voucherMessage = voucherValid ? "Voucher applied successfully" : "Voucher is invalid or expired";
            voucherDiscount = safe(preview.appliedDiscountAmount());
        }

        BigDecimal effectiveDiscount = discountPromotion
                .add(voucherDiscount)
                .max(safe(order.getDiscountAmount()))
                .min(subtotalAmount);
        OrderPricingService.PriceBreakdown breakdown =
                orderPricingService.calculatePriceBreakdown(subtotalAmount, effectiveDiscount);

        return new OrderPreviewResponse(
                subtotalAmount,
                BigDecimal.ZERO,
                breakdown.subtotalAmount(),
                breakdown.discountAmount(),
                breakdown.serviceChargePercent(),
                breakdown.serviceChargeAmount(),
                breakdown.taxPercent(),
                breakdown.taxAmount(),
                breakdown.finalAmount(),
                voucherValid,
                voucherMessage,
                discountPromotion);
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
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid order status: " + status.trim());
            }
        }

        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
        }

        if (to != null) {
            predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
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
            String escaped = escapeLikePattern(tableNumber.trim().toLowerCase());
            String pattern = "%" + escaped + "%";

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Order> subRoot = subquery.from(Order.class);
            Join<Order, DiningTable> subTable = subRoot.join("table");

            subquery.select(subRoot.get("id")).where(cb.like(cb.lower(subTable.get("tableNumber")), pattern, '\\'));

            predicates.add(root.get("id").in(subquery));
        }

        return predicates;
    }

    private String escapeLikePattern(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<OrderStatus> activeStatuses() {
        return OrderStatus.ACTIVE_STATUSES;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
