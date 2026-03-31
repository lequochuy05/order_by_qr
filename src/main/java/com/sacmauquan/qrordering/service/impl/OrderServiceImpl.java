package com.sacmauquan.qrordering.service.impl;
import org.springframework.lang.NonNull;
import java.util.Objects;

import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;
import com.sacmauquan.qrordering.service.DiscountService;
import com.sacmauquan.qrordering.service.NotificationService;
import com.sacmauquan.qrordering.service.OrderService;
import com.sacmauquan.qrordering.state.OrderState;
import com.sacmauquan.qrordering.state.OrderStateFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository tableRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;
    private final DiscountService discountService;
    private final OrderStateFactory orderStateFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Page<Order> getOrderHistory(String status, LocalDateTime startDate,
            LocalDateTime endDate, String search, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
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
        return orderRepository.findAll(spec, pageable);
    }

    @Override
    public Map<String, Object> getOrderStats(String status, LocalDateTime startDate, LocalDateTime endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<Order> root = cq.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(root.get("status"), status));
        }
        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
        }
        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
        }

        cq.multiselect(
                cb.count(root),
                cb.coalesce(cb.sum(root.get("totalAmount")), 0.0))
                .where(cb.and(predicates.toArray(new Predicate[0])));

        Object[] result = entityManager.createQuery(cq).getSingleResult();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", result[0]);
        stats.put("totalRevenue", result[1]);
        return stats;
    }

    @Override
    @Transactional
    public Order updateStatus(@NonNull Long id, @NonNull String status) {
        Order order = orderRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        OrderState state = orderStateFactory.getState(status);
        state.handleRequest(order);

        Order saved = orderRepository.save(order);

        if ("CANCELLED".equals(order.getStatus()) && order.getOrderItems().isEmpty()) {
            DiningTable table = order.getTable();
            table.setStatus(DiningTable.AVAILABLE);
            tableRepository.save(table);
        } else {
            recalcTableStatus(order);
        }

        notificationService.notifyOrderChange();
        return saved;
    }

    @Override
    @Transactional
    public Order createOrder(@NonNull OrderRequest req) {
        Objects.requireNonNull(req);
        boolean emptyItems = (req.getItems() == null || req.getItems().isEmpty());
        boolean emptyCombos = (req.getCombos() == null || req.getCombos().isEmpty());
        if (emptyItems && emptyCombos) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        }

        DiningTable table = resolveTable(req);

        Order order = orderRepository.findFirstByTableIdAndStatus(table.getId(), "PENDING");
        if (order == null) {
            order = new Order();
            order.setTable(table);
            OrderState pendingState = orderStateFactory.getState("PENDING");
            pendingState.handleRequest(order);
            order.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            order.setOrderItems(new ArrayList<>());
            order.setTotalAmount(0d);
        }

        processItems(req, order);
        processCombos(req, order);

        double subtotal = order.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();

        order.setOriginalTotal(subtotal);
        order.setDiscountVoucher(0d);
        order.setTotalAmount(subtotal);

        Order saved = orderRepository.save(order);

        table.setStatus(DiningTable.OCCUPIED);
        tableRepository.save(table);

        notificationService.notifyOrderChange();
        return saved;
    }

    private DiningTable resolveTable(OrderRequest req) {
        if (req.getTableCode() != null && !req.getTableCode().isBlank()) {
            return tableRepository.findByTableCode(req.getTableCode())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn: " + req.getTableCode()));
        } else if (req.getTableId() != null) {
            return tableRepository.findById(req.getTableId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn ID: " + req.getTableId()));
        } else {
            throw new IllegalArgumentException("Thiếu thông tin bàn (tableCode hoặc tableId)");
        }
    }

    private void processItems(OrderRequest req, Order order) {
        if (req.getItems() == null)
            return;
        for (OrderRequest.ItemRequest it : req.getItems()) {
            if (it.getMenuItemId() == null || it.getQuantity() <= 0)
                throw new IllegalArgumentException("Món ăn không hợp lệ");

            MenuItem mi = menuItemRepository.findById(it.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món: " + it.getMenuItemId()));

            List<ItemOption> requiredOptions = mi.getItemOptions() != null
                    ? mi.getItemOptions().stream().filter(ItemOption::isRequired).toList()
                    : new ArrayList<>();
            List<ItemOptionValue> selectedValues = new ArrayList<>();
            if (it.getSelectedOptionValueIds() != null && !it.getSelectedOptionValueIds().isEmpty()) {
                selectedValues = itemOptionValueRepository.findAllById(it.getSelectedOptionValueIds());
            }

            for (ItemOption required : requiredOptions) {
                boolean hasSelected = selectedValues.stream().anyMatch(
                        val -> val.getItemOption() != null && val.getItemOption().getId().equals(required.getId()));
                if (!hasSelected) {
                    throw new IllegalArgumentException("Vui lòng chọn đầy đủ: " + required.getName());
                }
            }

            double extraPrice = selectedValues.stream()
                    .mapToDouble(val -> val.getExtraPrice()).sum();
            double finalUnitPrice = (mi.getPrice()) + extraPrice;

            List<Long> reqOptionIds = it.getSelectedOptionValueIds() != null ? it.getSelectedOptionValueIds()
                    : new ArrayList<>();

            Optional<OrderItem> exist = order.getOrderItems().stream()
                    .filter(oi -> oi.getMenuItem() != null
                            && oi.getMenuItem().getId().equals(mi.getId())
                            && Objects.equals(oi.getNotes(), it.getNotes())
                            && oi.getCombo() == null
                            && !oi.isPrepared()
                            && checkOptionsMatch(oi.getOrderItemOptions(), reqOptionIds))
                    .findFirst();

            if (exist.isPresent()) {
                OrderItem oi = exist.get();
                oi.setQuantity(oi.getQuantity() + it.getQuantity());
            } else {
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setMenuItem(mi);
                oi.setCombo(null);
                oi.setQuantity(it.getQuantity());
                oi.setUnitPrice(finalUnitPrice);
                oi.setNotes(it.getNotes());
                oi.setPrepared(false);

                List<OrderItemOption> orderItemOptions = new ArrayList<>();
                for (ItemOptionValue val : selectedValues) {
                    OrderItemOption option = OrderItemOption.builder()
                            .orderItem(oi)
                            .optionName(val.getItemOption() != null ? val.getItemOption().getName() : "Khác")
                            .optionValueName(val.getName())
                            .extraPrice(val.getExtraPrice())
                            .itemOptionValue(val)
                            .build();
                    orderItemOptions.add(option);
                }
                oi.setOrderItemOptions(orderItemOptions);

                order.getOrderItems().add(oi);
            }
        }
    }

    private boolean checkOptionsMatch(List<OrderItemOption> existingOptions, List<Long> newOptionIds) {
        if (existingOptions == null)
            existingOptions = new ArrayList<>();
        if (newOptionIds == null)
            newOptionIds = new ArrayList<>();
        if (existingOptions.size() != newOptionIds.size())
            return false;

        List<Long> existingIds = existingOptions.stream()
                .map(OrderItemOption::getItemOptionValue)
                .filter(Objects::nonNull)
                .map(ItemOptionValue::getId)
                .toList();

        return existingIds.containsAll(newOptionIds) && newOptionIds.containsAll(existingIds);
    }

    private void processCombos(OrderRequest req, Order order) {
        if (req.getCombos() == null)
            return;
        for (OrderRequest.ComboRequest cr : req.getCombos()) {
            if (cr.getComboId() == null || cr.getQuantity() <= 0)
                continue;

            Combo combo = comboRepository.findById(cr.getComboId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy combo: " + cr.getComboId()));
            if (combo.getActive() == null || !combo.getActive())
                continue;

            Optional<OrderItem> exist = order.getOrderItems().stream()
                    .filter(oi -> oi.getCombo() != null
                            && oi.getCombo().getId().equals(combo.getId())
                            && Objects.equals(oi.getNotes(), cr.getNotes())
                            && !oi.isPrepared())
                    .findFirst();

            if (exist.isPresent()) {
                OrderItem oi = exist.get();
                oi.setQuantity(oi.getQuantity() + cr.getQuantity());
            } else {
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setCombo(combo);
                oi.setMenuItem(null);
                oi.setQuantity(cr.getQuantity());
                oi.setUnitPrice(combo.getPrice());
                oi.setNotes(cr.getNotes());
                oi.setPrepared(false);
                order.getOrderItems().add(oi);
            }
        }
    }

    @Override
    @Transactional
    public void cancelOrderItem(@NonNull Long itemId) {
        OrderItem item = orderItemRepository.findById(Objects.requireNonNull(itemId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared())
            throw new RuntimeException("Món đã làm, không thể hủy");

        Order order = item.getOrder();
        double minus = (Optional.ofNullable(item.getUnitPrice()).orElse(0.0)) * item.getQuantity();
        order.setTotalAmount(Math.max(0, order.getTotalAmount() - minus));

        order.getOrderItems().remove(item);
        orderItemRepository.delete(item);

        if (order.getOrderItems().isEmpty()) {
            orderStateFactory.getState("CANCELLED").handleRequest(order);
            orderRepository.save(order);
            DiningTable table = order.getTable();
            table.setStatus(DiningTable.AVAILABLE);
            tableRepository.save(table);
        } else {
            orderRepository.save(order);
            recalcTableStatus(order);
        }

        notificationService.notifyOrderChange();
    }

    @Override
    @Transactional
    public void updateItemStatus(@NonNull Long itemId, @NonNull String newStatus) {
        OrderItem item = orderItemRepository.findById(Objects.requireNonNull(itemId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        item.setStatus(newStatus);

        if ("FINISHED".equals(newStatus)) {
            item.setPrepared(true);
        } else {
            item.setPrepared(false);
        }

        orderItemRepository.save(item);

        recalcTableStatus(item.getOrder());
        notificationService.notifyOrderChange();
    }

    @Override
    @Transactional
    public void markItemPrepared(@NonNull Long itemId) {
        updateItemStatus(Objects.requireNonNull(itemId), "FINISHED");
    }

    @Override
    public List<Order> getKitchenOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .filter(o -> o.getOrderItems().stream().anyMatch(oi -> !"FINISHED".equals(oi.getStatus())))
                .sorted((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()))
                .toList();
    }

    @Override
    public Optional<Order> getCurrentOrderByTable(@NonNull Long tableId) {
        return orderRepository.findFirstByTableIdAndStatusInOrderByCreatedAtDesc(
                Objects.requireNonNull(tableId), List.of("PENDING"));
    }

    @Override
    @Transactional
    public OrderItem updateOrderItem(@NonNull Long itemId, int quantity, String notes) {
        OrderItem item = orderItemRepository.findById(Objects.requireNonNull(itemId))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared())
            throw new RuntimeException("Món đã làm, không thể sửa");

        item.setQuantity(quantity);
        item.setNotes(notes);

        Order order = item.getOrder();

        double subtotal = order.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();

        order.setOriginalTotal(subtotal);
        order.setDiscountVoucher(0d);
        order.setTotalAmount(subtotal);

        orderItemRepository.save(item);
        orderRepository.save(order);

        recalcTableStatus(order);

        notificationService.notifyOrderChange();

        return item;
    }

    @Override
    @Transactional
    public String payOrder(@NonNull Long id, @NonNull Long userId, String voucherCode) {
        Order order = orderRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Đơn đã được thanh toán hoặc đã hủy.");
        }

        boolean hasUnprepared = order.getOrderItems().stream().anyMatch(item -> !item.isPrepared());
        if (hasUnprepared)
            throw new IllegalStateException("Đơn hàng còn món chưa hoàn tất, không thể thanh toán.");

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        double originalTotal = order.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();

        var result = discountService.applyDiscounts(originalTotal, voucherCode, true);

        order.setOriginalTotal(originalTotal);
        order.setDiscountVoucher(result.getDiscountValue());
        order.setTotalAmount(result.getFinalTotal());

        order.setStatus("PAID");

        order.setPaidBy(currentUser);
        order.setPaymentTime(LocalDateTime.now());
        orderRepository.save(order);

        DiningTable table = order.getTable();
        table.setStatus(DiningTable.AVAILABLE);
        tableRepository.save(table);

        notificationService.notifyOrderChange();

        return "Thanh toán thành công";
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(@NonNull OrderRequest req) {
        Objects.requireNonNull(req);
        boolean emptyItems = (req.getItems() == null || req.getItems().isEmpty());
        boolean emptyCombos = (req.getCombos() == null || req.getCombos().isEmpty());
        if (emptyItems && emptyCombos) {
            throw new IllegalArgumentException("Đơn hàng trống");
        }

        double subtotalItems = 0d;
        double subtotalCombos = 0d;

        if (req.getItems() != null) {
            for (OrderRequest.ItemRequest it : req.getItems()) {
                if (it.getMenuItemId() == null || it.getQuantity() <= 0)
                    continue;
                MenuItem mi = menuItemRepository.findById(it.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy món: " + it.getMenuItemId()));
                Double price = mi.getPrice();

                double extraPrice = 0d;
                if (it.getSelectedOptionValueIds() != null && !it.getSelectedOptionValueIds().isEmpty()) {
                    List<ItemOptionValue> vals = itemOptionValueRepository.findAllById(it.getSelectedOptionValueIds());
                    extraPrice = vals.stream()
                            .mapToDouble(val -> val.getExtraPrice()).sum();
                }

                subtotalItems += ((price) + extraPrice) * it.getQuantity();
            }
        }

        if (req.getCombos() != null) {
            for (OrderRequest.ComboRequest cr : req.getCombos()) {
                Combo combo = comboRepository.findById(cr.getComboId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy combo: " + cr.getComboId()));
                if (combo.getActive() == null || !combo.getActive())
                    continue;
                Double comboPrice = combo.getPrice();
                subtotalCombos += (comboPrice != null ? comboPrice : 0d) * cr.getQuantity();
            }
        }

        double subtotal = subtotalItems + subtotalCombos;
        double finalTotal = subtotal;
        double originalTotal = subtotal;
        boolean voucherValid = false;
        String voucherMessage = null;
        double discountVoucher = 0d;

        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            var vr = discountService.validateVoucher(req.getVoucherCode(), subtotal);
            voucherValid = vr.isValid();
            voucherMessage = vr.getMessage();
            discountVoucher = vr.getDiscount();
            finalTotal = Math.max(0d, subtotal - discountVoucher);
        }

        return new OrderPreviewResponse(
                subtotalItems,
                subtotalCombos,
                finalTotal,
                voucherValid,
                voucherMessage,
                discountVoucher,
                0d,
                originalTotal);
    }

    private void recalcTableStatus(com.sacmauquan.qrordering.model.Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        DiningTable table = order.getTable();
        if (items == null || items.isEmpty()) {
            table.setStatus(DiningTable.AVAILABLE);
        } else {
            boolean allPrepared = items.stream().allMatch(OrderItem::isPrepared);
            table.setStatus(allPrepared ? DiningTable.WAITING_FOR_PAYMENT : DiningTable.OCCUPIED);
        }
        tableRepository.save(table);
    }
}
