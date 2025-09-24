package com.sacmauquan.qrordering.service;

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

    // ===================== CREATE ORDER (thêm món) =====================
    @Transactional
    public Order createOrder(OrderRequest orderRequest) {
        if (orderRequest == null || orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ");
        }

        DiningTable table = tableRepository.findById(orderRequest.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + orderRequest.getTableId()));

        Order order = orderRepository.findFirstByTableIdAndStatus(table.getId(), "PENDING");
        if (order == null) {
            order = new Order();
            order.setTable(table);
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
            order.setOrderItems(new ArrayList<>());
            order.setTotalAmount(0);
        }

        double totalAmount = order.getTotalAmount();

        for (OrderRequest.ItemRequest itemReq : orderRequest.getItems()) {
            if (itemReq.getMenuItemId() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Món ăn không hợp lệ");
            }
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn với ID: " + itemReq.getMenuItemId()));

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(menuItem);
            oi.setQuantity(itemReq.getQuantity());
            oi.setUnitPrice(menuItem.getPrice());
            oi.setNotes(itemReq.getNotes());
            oi.setPrepared(false);           // món mới luôn là chưa làm

            totalAmount += menuItem.getPrice() * itemReq.getQuantity();
            order.getOrderItems().add(oi);
        }

        order.setTotalAmount(totalAmount);
        Order saved = orderRepository.save(order);

        // Có món chưa làm -> chắc chắn "Đang phục vụ"
        DiningTable t = saved.getTable();
        t.setStatus("Đang phục vụ");
        tableRepository.save(t);

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
        return saved;
    }

    public List<Order> getAllOrders() { return orderRepository.findAll(); }

    // ===================== CANCEL 1 ITEM (hủy món lẻ) =====================
    @Transactional
    public void cancelOrderItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared()) {
            throw new RuntimeException("Món đã làm, không thể hủy");
        }

        Order order = item.getOrder();

        // Trừ tiền đơn
        double minus = (Optional.ofNullable(item.getUnitPrice()).orElse(0.0)) * item.getQuantity();
        order.setTotalAmount(Math.max(0, order.getTotalAmount() - minus));

        // Xóa món
        order.getOrderItems().remove(item);         // tránh orphan trong bộ nhớ
        orderItemRepository.delete(item);

        // Nếu hết món -> CANCEL đơn & bàn Trống; nếu còn -> giữ PENDING và tính lại trạng thái
        List<OrderItem> left = orderItemRepository.findByOrderId(order.getId());
        if (left.isEmpty()) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);

            DiningTable table = order.getTable();
            table.setStatus("Trống");
            tableRepository.save(table);
        } else {
            orderRepository.save(order);
            recalcTableStatus(order);               // <-- QUAN TRỌNG
        }

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
    }

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

        Order order = orderRepository.findById(item.getOrder().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        recalcTableStatus(order);                   // <-- dùng chung 1 luật

        messagingTemplate.convertAndSend("/topic/tables", "UPDATED");
    }

    public Optional<Order> getCurrentOrderByTable(Long tableId) {
        return Optional.ofNullable(orderRepository.findFirstByTableIdAndStatus(tableId, "PENDING"));
    }

    // ===================== UPDATE ORDER ITEM (sửa món) =====================
    @Transactional
    public OrderItem updateOrderItem(Long itemId, int quantity, String notes) {
        OrderItem item = orderItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy món"));

        if (item.isPrepared()) {
            throw new RuntimeException("Món đã làm, không thể sửa");
        }

        Order order = item.getOrder();

        // Trừ đi tiền cũ
        double oldLine = item.getUnitPrice() * item.getQuantity();
        order.setTotalAmount(Math.max(0, order.getTotalAmount() - oldLine));

        // Cập nhật
        item.setQuantity(quantity);
        item.setNotes(notes);

        // Cộng lại tiền mới
        double newLine = item.getUnitPrice() * item.getQuantity();
        order.setTotalAmount(order.getTotalAmount() + newLine);

        orderItemRepository.save(item);
        orderRepository.save(order);

        // cập nhật trạng thái bàn
        recalcTableStatus(order);

        // bắn WS cho UI tự reload
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

        // Kiểm tra xem có món nào chưa làm (prepared = 0)
        boolean hasUnprepared = order.getOrderItems().stream()
            .anyMatch(item -> !item.isPrepared());
        if (hasUnprepared) {
            throw new IllegalStateException("Đơn hàng còn món chưa hoàn tất, không thể thanh toán.");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

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
