package com.qros.modules.ai.service;

import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffContextProvider {

    private static final int ACTIVE_ITEM_LIMIT = 20;
    private static final int RECENT_ORDER_LIMIT = 8;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderRepository orderRepository;
    private final KitchenService kitchenService;
    private final DiningTableRepository diningTableRepository;

    @Transactional(readOnly = true)
    public String buildStaffContext() {
        LocalDateTime now = AppTime.now();
        StringBuilder builder = new StringBuilder();

        builder.append("THỜI ĐIỂM CẬP NHẬT: ").append(formatDateTime(now)).append("\n\n");
        appendKitchenContext(builder, now);
        appendTableContext(builder);
        appendRecentOrderContext(builder, now);

        return builder.toString();
    }

    private void appendKitchenContext(StringBuilder builder, LocalDateTime now) {
        List<KitchenItemContext> activeItems = kitchenService.getKitchenOrders().stream()
                .flatMap(order -> order.orderItems().stream()
                        .filter(item -> item.status() == OrderItemStatus.PENDING || item.status() == OrderItemStatus.COOKING)
                        .map(item -> toKitchenItemContext(order, item, now)))
                .sorted(Comparator.comparingLong(KitchenItemContext::waitMinutes).reversed())
                .limit(ACTIVE_ITEM_LIMIT)
                .toList();

        builder.append("MÓN ĐANG CHỜ/ĐANG NẤU:\n");
        if (activeItems.isEmpty()) {
            builder.append("  - Không có món nào đang chờ bếp.\n\n");
            return;
        }

        activeItems.forEach(item -> builder.append("  - ").append(item.line()).append("\n"));
        builder.append("\n");
    }

    private KitchenItemContext toKitchenItemContext(
            KitchenOrderResponse order, KitchenOrderResponse.KitchenOrderItemResponse item, LocalDateTime now) {
        LocalDateTime createdAt = item.createdAt() != null ? item.createdAt() : order.createdAt();
        long waitMinutes = minutesSince(createdAt, now);
        String tableNumber = order.table() != null ? order.table().tableNumber() : "chưa gán";
        String itemName = itemName(item);
        String category = categoryName(item);
        String notes = item.notes() != null && !item.notes().isBlank() ? " | ghi chú: " + item.notes().trim() : "";
        String options = optionsText(item.options());
        String optionSuffix = options.isBlank() ? "" : " | tuỳ chọn: " + options;

        String line = "Bàn %s | Đơn #%d | %s x%d | %s | %s | chờ %d phút%s%s"
                .formatted(
                        tableNumber,
                        order.id(),
                        itemName,
                        item.quantity(),
                        category,
                        itemStatusLabel(item.status()),
                        waitMinutes,
                        optionSuffix,
                        notes);

        return new KitchenItemContext(line, waitMinutes);
    }

    private void appendTableContext(StringBuilder builder) {
        List<DiningTable> tables = diningTableRepository.findAllByOrderByTableNumberAsc();
        Map<TableStatus, Long> counts = tables.stream()
                .collect(Collectors.groupingBy(DiningTable::getStatus, () -> new EnumMap<>(TableStatus.class), Collectors.counting()));

        builder.append("TRẠNG THÁI BÀN:\n");
        builder.append("  - Tổng: ").append(tables.size());
        for (TableStatus status : TableStatus.values()) {
            builder.append(" | ")
                    .append(tableStatusLabel(status))
                    .append(": ")
                    .append(counts.getOrDefault(status, 0L));
        }
        builder.append("\n");

        if (tables.isEmpty()) {
            builder.append("  - Chưa có bàn trong hệ thống.\n\n");
            return;
        }

        tables.forEach(table -> builder.append("  - Bàn ")
                .append(table.getTableNumber())
                .append(" | ")
                .append(tableStatusLabel(table.getStatus()))
                .append(" | sức chứa ")
                .append(table.getCapacity())
                .append("\n"));
        builder.append("\n");
    }

    private void appendRecentOrderContext(StringBuilder builder, LocalDateTime now) {
        List<Order> recentOrders = recentOrders();

        builder.append("ORDER GẦN ĐÂY:\n");
        if (recentOrders.isEmpty()) {
            builder.append("  - Chưa có order gần đây.\n");
            return;
        }

        recentOrders.forEach(order -> {
            String tableNumber = order.getTable() != null ? order.getTable().getTableNumber() : "chưa gán";
            long ageMinutes = minutesSince(order.getCreatedAt(), now);
            String items = orderItemsText(order);

            builder.append("  - Đơn #")
                    .append(order.getId())
                    .append(" | Bàn ")
                    .append(tableNumber)
                    .append(" | ")
                    .append(orderStatusLabel(order.getStatus()))
                    .append(" | tạo ")
                    .append(ageMinutes)
                    .append(" phút trước")
                    .append(" | tổng ")
                    .append(formatMoney(order.getFinalAmount()))
                    .append(" | món: ")
                    .append(items)
                    .append("\n");
        });
    }

    private List<Order> recentOrders() {
        PageRequest pageable =
                PageRequest.of(0, RECENT_ORDER_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Long> orderIds = orderRepository.findAll(pageable).getContent().stream()
                .map(Order::getId)
                .toList();

        if (orderIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Order> ordersById = orderRepository.findDistinctByIdIn(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return orderIds.stream()
                .map(ordersById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String orderItemsText(Order order) {
        String text = order.getOrderItems().stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
                .sorted(Comparator.comparing(OrderItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .map(item -> "%s x%d (%s)"
                        .formatted(itemName(item), item.getQuantity(), itemStatusLabel(item.getStatus())))
                .collect(Collectors.joining("; "));

        return text.isBlank() ? "không có món đang tính" : text;
    }

    private String itemName(KitchenOrderResponse.KitchenOrderItemResponse item) {
        if (item.menuItem() != null && item.menuItem().name() != null && !item.menuItem().name().isBlank()) {
            return item.menuItem().name();
        }
        if (item.combo() != null && item.combo().name() != null && !item.combo().name().isBlank()) {
            return item.combo().name();
        }
        return "Món chưa xác định";
    }

    private String itemName(OrderItem item) {
        if (item.getItemNameSnapshot() != null && !item.getItemNameSnapshot().isBlank()) {
            return item.getItemNameSnapshot();
        }
        if (item.getMenuItem() != null && item.getMenuItem().getName() != null) {
            return item.getMenuItem().getName();
        }
        if (item.getCombo() != null && item.getCombo().getName() != null) {
            return item.getCombo().getName();
        }
        return "Món chưa xác định";
    }

    private String categoryName(KitchenOrderResponse.KitchenOrderItemResponse item) {
        if (item.menuItem() != null
                && item.menuItem().category() != null
                && item.menuItem().category().name() != null
                && !item.menuItem().category().name().isBlank()) {
            return item.menuItem().category().name();
        }
        return item.combo() != null ? "Combo" : "Khác";
    }

    private String optionsText(List<KitchenOrderResponse.KitchenOrderItemOptionResponse> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }

        return options.stream()
                .map(option -> option.optionName() + ": " + option.optionValueName())
                .collect(Collectors.joining(", "));
    }

    private long minutesSince(LocalDateTime from, LocalDateTime now) {
        if (from == null || now == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.MINUTES.between(from, now));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "không rõ";
    }

    private String formatMoney(BigDecimal value) {
        return value != null ? "%,.0fđ".formatted(value).replace(",", ".") : "0đ";
    }

    private String orderStatusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING -> "đang nhận món";
            case SERVING -> "đang phục vụ";
            case AWAITING_PAYMENT -> "chờ thanh toán";
            case COMPLETED -> "đã hoàn tất";
            case CANCELLED -> "đã huỷ";
        };
    }

    private String itemStatusLabel(OrderItemStatus status) {
        return switch (status) {
            case PENDING -> "chờ nấu";
            case COOKING -> "đang nấu";
            case FINISHED -> "đã xong";
            case CANCELLED -> "đã huỷ";
        };
    }

    private String tableStatusLabel(TableStatus status) {
        return switch (status) {
            case AVAILABLE -> "trống";
            case OCCUPIED -> "đang dùng";
            case WAITING_FOR_PAYMENT -> "chờ thanh toán";
            case INACTIVE -> "ngưng dùng";
        };
    }

    private record KitchenItemContext(String line, long waitMinutes) {}
}
