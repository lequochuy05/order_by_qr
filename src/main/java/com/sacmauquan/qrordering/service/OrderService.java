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

    // ===================== CREATE ORDER =====================
    @Transactional
    public Order createOrder(OrderRequest req) {
        boolean emptyItems  = (req.getItems()  == null || req.getItems().isEmpty());
        boolean emptyCombos = (req.getCombos() == null || req.getCombos().isEmpty());
        if (emptyItems && emptyCombos) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        }

        DiningTable table = tableRepository.findByTableCode(req.getTableCode())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn: " + req.getTableId()));

        // lấy / tạo order PENDING
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

                Optional<OrderItem> exist = order.getOrderItems().stream()
                        .filter(oi -> oi.getMenuItem() != null
                                && oi.getMenuItem().getId().equals(mi.getId())
                                && Objects.equals(oi.getNotes(), it.getNotes())
                                && oi.getCombo() == null)
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

        // ----- 2) Combo (có notes & quantity) -----
        if (req.getCombos() != null) {
            for (OrderRequest.ComboRequest cr : req.getCombos()) {
                if (cr.getComboId() == null || cr.getQuantity() <= 0) continue;

                Combo combo = comboRepository.findById(cr.getComboId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy combo: " + cr.getComboId()));
                if (combo.getActive() == null || !combo.getActive()) continue;

                Optional<OrderItem> exist = order.getOrderItems().stream()
                        .filter(oi -> oi.getCombo() != null
                                && oi.getCombo().getId().equals(combo.getId())
                                && Objects.equals(oi.getNotes(), cr.getNotes()))
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

        // ----- 3) Tổng tạm thời -----
        double subtotal = order.getOrderItems().stream()
                .mapToDouble(oi -> oi.getUnitPrice() * oi.getQuantity())
                .sum();

        order.setOriginalTotal(subtotal);
        order.setDiscountVoucher(0d);
        order.setTotalAmount(subtotal);

        Order saved = orderRepository.save(order);

        table.setStatus("Đang phục vụ");
        tableRepository.save(table);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
        return saved;
    }

    public List<Order> getAllOrders() { return orderRepository.findAll(); }

    // ===================== CANCEL ITEM =====================
    @Transactional
    public void cancelOrderItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared()) throw new RuntimeException("Món đã làm, không thể hủy");

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

    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public void markItemPrepared(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        if (!item.isPrepared()) {
            item.setPrepared(true);
            orderItemRepository.save(item);
        }

        recalcTableStatus(item.getOrder());
        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
    }

    public Optional<Order> getCurrentOrderByTable(Long tableId) {
        return Optional.ofNullable(orderRepository.findFirstByTableIdAndStatus(tableId, "PENDING"));
    }

    @Transactional
    public OrderItem updateOrderItem(Long itemId, int quantity, String notes) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared()) throw new RuntimeException("Món đã làm, không thể sửa");

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
        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
        return item;
    }

    // ===================== PAY =====================
    @Transactional
    public String payOrder(Long id, Long userId, String voucherCode) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Đơn đã được thanh toán hoặc đã hủy.");
        }

        boolean hasUnprepared = order.getOrderItems().stream().anyMatch(item -> !item.isPrepared());
        if (hasUnprepared) throw new IllegalStateException("Đơn hàng còn món chưa hoàn tất, không thể thanh toán.");

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
        table.setStatus("Trống");
        tableRepository.save(table);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
        return "Thanh toán thành công";
    }

    // ===================== PREVIEW =====================
    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(OrderRequest req) {
        boolean emptyItems  = (req.getItems()  == null || req.getItems().isEmpty());
        boolean emptyCombos = (req.getCombos() == null || req.getCombos().isEmpty());
        if (emptyItems && emptyCombos) {
            throw new IllegalArgumentException("Đơn hàng trống");
        }

        double subtotalItems = 0d;
        double subtotalCombos = 0d;

        // món lẻ
        if (req.getItems() != null) {
            for (OrderRequest.ItemRequest it : req.getItems()) {
                if (it.getMenuItemId() == null || it.getQuantity() <= 0) continue;
                MenuItem mi = menuItemRepository.findById(it.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy món: " + it.getMenuItemId()));
                Double price = mi.getPrice();
                subtotalItems += (price != null ? price : 0d) * it.getQuantity();
            }
        }

        // combos
        if (req.getCombos() != null) {
            for (OrderRequest.ComboRequest cr : req.getCombos()) {
                Combo combo = comboRepository.findById(cr.getComboId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy combo: " + cr.getComboId()));
                if (combo.getActive() == null || !combo.getActive()) continue;
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
                originalTotal
        );
    }

    // ===================== TABLE STATUS =====================
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
