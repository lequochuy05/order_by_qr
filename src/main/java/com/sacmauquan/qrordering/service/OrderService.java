package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class OrderService {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private DiningTableRepository tableRepository;
    @Autowired private ComboRepository comboRepository;
    @Autowired private DiscountService discountService;

    // ===================== CREATE ORDER (thêm món / combo, cộng dồn) =====================
    @Transactional
    public Order createOrder(OrderRequest req) {
        if ((req.getItems() == null || req.getItems().isEmpty())
                && (req.getComboIds() == null || req.getComboIds().isEmpty())) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        }

        DiningTable table = tableRepository.findById(req.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn: " + req.getTableId()));

        // 🔑 lấy order PENDING hiện tại, nếu chưa có thì tạo mới
        Order order = orderRepository.findFirstByTableIdAndStatus(table.getId(), "PENDING");
        if (order == null) {
            order = new Order();
            order.setTable(table);
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            order.setOrderItems(new ArrayList<>());
            order.setTotalAmount(0d);
        }

        // ----- 1) Món lẻ -----
        if (req.getItems() != null) {
            for (OrderRequest.ItemRequest it : req.getItems()) {
                if (it.getMenuItemId() == null || it.getQuantity() <= 0)
                    throw new IllegalArgumentException("Món ăn không hợp lệ");

                MenuItem mi = menuItemRepository.findById(it.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy món: " + it.getMenuItemId()));

                // kiểm tra món đã tồn tại trong order chưa (id + notes)
                Optional<OrderItem> exist = order.getOrderItems().stream()
                        .filter(oi -> oi.getMenuItem() != null
                                && oi.getMenuItem().getId().equals(mi.getId())
                                && Objects.equals(oi.getNotes(), it.getNotes())
                                && oi.getCombo() == null) // chỉ áp dụng cho món lẻ
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
                    oi.setUnitPrice(mi.getPrice());
                    oi.setNotes(it.getNotes());
                    oi.setPrepared(false);
                    order.getOrderItems().add(oi);
                }
            }
        }

        // ----- 2) Combo -----
        if (req.getComboIds() != null) {
            for (Long comboId : req.getComboIds()) {
                Combo combo = comboRepository.findById(comboId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy combo: " + comboId));
                if (combo.getActive() == null || !combo.getActive()) continue;

                // kiểm tra combo đã tồn tại trong order chưa
                Optional<OrderItem> exist = order.getOrderItems().stream()
                        .filter(oi -> oi.getCombo() != null && oi.getCombo().getId().equals(combo.getId()))
                        .findFirst();

                if (exist.isPresent()) {
                    OrderItem oi = exist.get();
                    oi.setQuantity(oi.getQuantity() + 1);
                } else {
                    OrderItem oi = new OrderItem();
                    oi.setOrder(order);
                    oi.setCombo(combo);
                    oi.setMenuItem(null);
                    oi.setQuantity(1);
                    oi.setUnitPrice(combo.getPrice());
                    oi.setNotes(null);
                    oi.setPrepared(false);
                    order.getOrderItems().add(oi);
                }
            }
        }

        // ----- 3) Tính lại tổng tiền -----
        double subtotal = order.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();

        double finalTotal = discountService.applyDiscountsFromSubtotal(subtotal, req.getVoucherCode());
        order.setTotalAmount(finalTotal);

        Order saved = orderRepository.save(order);

        // cập nhật trạng thái bàn
        table.setStatus("Đang phục vụ");
        tableRepository.save(table);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
        return saved;
    }

    // ===================== GET ALL ORDERS =====================
    public List<Order> getAllOrders() { return orderRepository.findAll(); }

    // ===================== CANCEL 1 ITEM =====================
    @Transactional
    public void cancelOrderItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared()) {
            throw new RuntimeException("Món đã làm, không thể hủy");
        }

        Order order = item.getOrder();
        double minus = (Optional.ofNullable(item.getUnitPrice()).orElse(0.0)) * item.getQuantity();
        order.setTotalAmount(Math.max(0, order.getTotalAmount() - minus));

        order.getOrderItems().remove(item);
        orderItemRepository.delete(item);

        if (order.getOrderItems().isEmpty()) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);

            DiningTable table = order.getTable();
            table.setStatus("Trống");
            tableRepository.save(table);
        } else {
            orderRepository.save(order);
            recalcTableStatus(order);
        }

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
    }

    // ===================== UPDATE STATUS =====================
    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    // ===================== MARK PREPARED =====================
    @Transactional
    public void markItemPrepared(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        if (!item.isPrepared()) {
            item.setPrepared(true);
            orderItemRepository.save(item);
        }

        Order order = item.getOrder();
        recalcTableStatus(order);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
    }

    // ===================== GET CURRENT ORDER BY TABLE =====================
    public Optional<Order> getCurrentOrderByTable(Long tableId) {
        return Optional.ofNullable(orderRepository.findFirstByTableIdAndStatus(tableId, "PENDING"));
    }

    // ===================== UPDATE ORDER ITEM =====================
    @Transactional
    public OrderItem updateOrderItem(Long itemId, int quantity, String notes) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared()) {
            throw new RuntimeException("Món đã làm, không thể sửa");
        }

        item.setQuantity(quantity);
        item.setNotes(notes);

        Order order = item.getOrder();
        double subtotal = order.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();

        double discounted = discountService.applyDiscountsFromSubtotal(subtotal, null);
        order.setTotalAmount(discounted);

        orderItemRepository.save(item);
        orderRepository.save(order);

        recalcTableStatus(order);
        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
        return item;
    }

    // ===================== PAY =====================
    @Transactional
    public String payOrder(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Đơn đã được thanh toán hoặc đã hủy.");
        }

        boolean hasUnprepared = order.getOrderItems().stream()
                .anyMatch(item -> !item.isPrepared());
        if (hasUnprepared) {
            throw new IllegalStateException("Đơn hàng còn món chưa hoàn tất, không thể thanh toán.");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        double subtotal = order.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();
        double finalAmount = discountService.applyDiscountsFromSubtotal(subtotal, null);
        order.setTotalAmount(finalAmount);

        order.setStatus("PAID");
        order.setPaidBy(currentUser);
        order.setPaymentTime(LocalDateTime.now());
        orderRepository.save(order);

        DiningTable table = order.getTable();
        table.setStatus("Trống");
        tableRepository.save(table);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
        return "Thanh toán thành công";
    }

    // ===================== PREVIEW ORDER (tính thử tiền) =====================
   @Transactional(readOnly = true)
public OrderPreviewResponse preview(OrderRequest req) {
    if ((req.getItems() == null || req.getItems().isEmpty())
            && (req.getComboIds() == null || req.getComboIds().isEmpty())) {
        throw new IllegalArgumentException("Đơn hàng trống");
    }

    double subtotalItems = 0d;
    double subtotalCombos = 0d;

    // ===== 1. Tính tiền món lẻ =====
    if (req.getItems() != null) {
            for (OrderRequest.ItemRequest it : req.getItems()) {
                if (it.getMenuItemId() == null || it.getQuantity() <= 0) continue;

                MenuItem mi = menuItemRepository.findById(it.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy món: " + it.getMenuItemId()));

                Double price = mi.getPrice();  // lấy ra wrapper
                subtotalItems += (price != null ? price : 0d) * it.getQuantity();
            }
        }

    // ===== 2. Tính tiền combos =====
    if (req.getComboIds() != null) {
        for (Long comboId : req.getComboIds()) {
            Combo combo = comboRepository.findById(comboId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy combo: " + comboId));
            if (combo.getActive() == null || !combo.getActive()) continue;

            Double comboPrice = combo.getPrice();
            subtotalCombos += (comboPrice != null ? comboPrice : 0d);
        }
    }

    double subtotal = subtotalItems + subtotalCombos;

    // ===== 3. giảm giá / voucher =====
    double finalTotal = subtotal;  // không áp dụng giảm giá

    return new OrderPreviewResponse(
            subtotalItems,
            subtotalCombos,
            finalTotal,
            false,       // voucherValid
            null,        // voucherMessage
            0d,          // discountVoucher
            0d           // discountPromotion
    );
}


    // ===================== RULE: TÍNH LẠI TRẠNG THÁI BÀN =====================
    private void recalcTableStatus(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        DiningTable table = order.getTable();

        if (items == null || items.isEmpty()) {
            table.setStatus("Trống");
        } else {
            boolean allPrepared = items.stream().allMatch(OrderItem::isPrepared);
            table.setStatus(allPrepared ? "Chờ thanh toán" : "Đang phục vụ");
        }
        tableRepository.save(table);
    }
}
