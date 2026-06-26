package com.qros.modules.order.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderTableSyncService {

    private final DiningTableRepository tableRepository;

    public void recalcTableStatus(Order order) {
        if (order == null || order.getTable() == null) {
            return;
        }

        DiningTable table = order.getTable();

        TableStatus nextStatus;
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            nextStatus = TableStatus.AVAILABLE;
        } else if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            nextStatus = TableStatus.WAITING_FOR_PAYMENT;
        } else {
            boolean hasBillableItems = order.getOrderItems().stream().anyMatch(OrderItem::isBillable);

            if (!hasBillableItems) {
                nextStatus = TableStatus.AVAILABLE;
            } else {
                boolean allBillableItemsFinished = order.getOrderItems().stream()
                        .filter(OrderItem::isBillable)
                        .allMatch(item -> item.getStatus() == OrderItemStatus.FINISHED);

                nextStatus = allBillableItemsFinished ? TableStatus.WAITING_FOR_PAYMENT : TableStatus.OCCUPIED;
            }
        }

        if (table.getStatus() != nextStatus) {
            table.setStatus(nextStatus);
            tableRepository.save(table);
        }
    }

    public void releaseTable(Order order) {
        if (order == null || order.getTable() == null) {
            return;
        }

        DiningTable table = order.getTable();
        table.setStatus(TableStatus.AVAILABLE);
        tableRepository.save(table);
    }
}
