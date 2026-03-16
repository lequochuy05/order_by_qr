package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.OrderPreviewResponse;
import com.sacmauquan.qrordering.dto.OrderRequest;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sacmauquan.qrordering.state.OrderState;
import com.sacmauquan.qrordering.state.OrderStateFactory;
import com.sacmauquan.qrordering.event.WebSocketEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository tableRepository;
    private final ComboRepository comboRepository;
    private final DiscountService discountService;
    private final OrderStateFactory orderStateFactory;

    // ===================== GET ALL ORDERS =====================
    public List<Order> getAllOrders() { 
        return orderRepository.findAll(); 
    }

    // ===================== UPDATE STATUS =====================
    @Transactional
    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));
        
        OrderState state = orderStateFactory.getState(status);
        state.handleRequest(order);
        
        Order saved = orderRepository.save(order);
        
        // Cập nhật trạng thái bàn nếu cần
        if ("CANCELLED".equals(order.getStatus()) && order.getOrderItems().isEmpty()) {
            DiningTable table = order.getTable();
            table.setStatus("Trống");
            tableRepository.save(table);
        } else {
             recalcTableStatus(order);
        }
        
        notifyChange();
        return saved;
    }

    // ===================== CREATE ORDER =====================
    @Transactional
    public Order createOrder(OrderRequest req) {
        boolean emptyItems  = (req.getItems()  == null || req.getItems().isEmpty());
        boolean emptyCombos = (req.getCombos() == null || req.getCombos().isEmpty());
        if (emptyItems && emptyCombos) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        }

        DiningTable table = resolveTable(req);

        // lấy / tạo order PENDING
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

        table.setStatus("Đang phục vụ");
        tableRepository.save(table);

        // Gửi WebSocket cập nhật
        notifyChange();
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
        if (req.getItems() == null) return;
        for (OrderRequest.ItemRequest it : req.getItems()) {
            if (it.getMenuItemId() == null || it.getQuantity() <= 0)
                throw new IllegalArgumentException("Món ăn không hợp lệ");

            MenuItem mi = menuItemRepository.findById(it.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món: " + it.getMenuItemId()));

            Optional<OrderItem> exist = order.getOrderItems().stream()
                    .filter(oi -> oi.getMenuItem() != null
                            && oi.getMenuItem().getId().equals(mi.getId())
                            && Objects.equals(oi.getNotes(), it.getNotes())
                            && oi.getCombo() == null
                            && !oi.isPrepared())
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

    private void processCombos(OrderRequest req, Order order) {
        if (req.getCombos() == null) return;
        for (OrderRequest.ComboRequest cr : req.getCombos()) {
            if (cr.getComboId() == null || cr.getQuantity() <= 0) continue;

            Combo combo = comboRepository.findById(cr.getComboId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy combo: " + cr.getComboId()));
            if (combo.getActive() == null || !combo.getActive()) continue;

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
            OrderState cancelledState = orderStateFactory.getState("CANCELLED");
            cancelledState.handleRequest(order);
            orderRepository.save(order);
            DiningTable table = order.getTable();
            table.setStatus("Trống");
            tableRepository.save(table);
        } else {
            orderRepository.save(order);
            recalcTableStatus(order);
        }

        // Gửi WebSocket cập nhật
        notifyChange();
    }

    // ===================== UPDATE ITEM STATUS (KDS) =====================
    @Transactional
    public void updateItemStatus(Long itemId, String newStatus) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        item.setStatus(newStatus);
        
        // Đồng bộ với preprare boolean để tương thích ngược nếu cần
        if ("FINISHED".equals(newStatus)) {
            item.setPrepared(true);
        } else {
            item.setPrepared(false);
        }
        
        orderItemRepository.save(item);

        // Tính toán lại trạng thái bàn (Đang phục vụ -> Chờ thanh toán)
        recalcTableStatus(item.getOrder());
        notifyChange(); // Gửi WebSocket cập nhật
    }

    // Deprecated: dùng updateItemStatus
    @Transactional
    public void markItemPrepared(Long itemId) {
        updateItemStatus(itemId, "FINISHED");
    }

    public List<Order> getKitchenOrders() {
        // Lấy các đơn PENDING (đang phục vụ) có món chưa xong
        return orderRepository.findAll().stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .filter(o -> o.getOrderItems().stream().anyMatch(oi -> !"FINISHED".equals(oi.getStatus())))
                .sorted((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()))
                .toList();
    }

    public Optional<Order> getCurrentOrderByTable(Long tableId) {
        return Optional.ofNullable(orderRepository.findFirstByTableIdAndStatus(tableId, "PENDING"));
    }

    // ===================== UPDATE ITEM =====================
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

        // Gửi WebSocket cập nhật
        notifyChange();

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

        OrderState paidState = orderStateFactory.getState("PAID"); // Adjust if PAID isn't a state, COMPLETED is closest. Assuming PAID is meant to map to COMPLETED or remains as a simple status for now.
        // If PAID is not a formal state in State Pattern currently, keep it as is or add PaidState class. For now just set status:
        order.setStatus("PAID"); 
        
        order.setPaidBy(currentUser);
        order.setPaymentTime(LocalDateTime.now());
        orderRepository.save(order);

        DiningTable table = order.getTable();
        table.setStatus("Trống");
        tableRepository.save(table);

        // Phát sự kiện thay vì tự tạo new Thread
        notifyChange();

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

    // ===================== TABLE STATUS CALCULATION =====================
    private void recalcTableStatus(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        DiningTable table = order.getTable();
        if (items == null || items.isEmpty()) {
            table.setStatus("Trống");
        } else {
            boolean allPrepared = items.stream().allMatch(OrderItem::isPrepared);
            // Nếu tất cả đã xong -> Chờ thanh toán. Ngược lại -> Đang phục vụ
            table.setStatus(allPrepared ? "Chờ thanh toán" : "Đang phục vụ");
        }
        tableRepository.save(table);
    }


    private void notifyChange() {
        // Thông báo cho lễ tân / quản lý bàn
        eventPublisher.publishEvent(new WebSocketEvent(
                "/topic/tables", 
                "UPDATED", 
                "⚡ [WS] Order change -> Sent UPDATED signal to /topic/tables"
        ));

        // Thông báo cho nhà bếp
        eventPublisher.publishEvent(new WebSocketEvent(
                "/topic/kitchen", 
                "UPDATED", 
                "⚡ [WS] Order change -> Sent UPDATED signal to /topic/kitchen"
        ));
    }
}