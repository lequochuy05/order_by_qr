package com.qros.modules.order.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.table.service.TableSessionResolver;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.event.DomainEvents.TableChangeEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderTableSyncService {

    private final DiningTableRepository tableRepository;
    private final TableSessionResolver tableSessionResolver;
    private final TableSessionService tableSessionService;
    private final ApplicationEventPublisher eventPublisher;

    public void recalcTableStatus(Order order) {
        if (order == null) {
            return;
        }

        List<DiningTable> tables = tablesForOrder(order);
        if (tables.isEmpty()) {
            return;
        }

        TableStatus nextStatus;
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            nextStatus = TableStatus.AVAILABLE;
        } else if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            nextStatus = TableStatus.WAITING_FOR_PAYMENT;
        } else {
            boolean hasBillableItems = order.getOrderItems().stream().anyMatch(OrderItem::isBillable);

            if (!hasBillableItems) {
                nextStatus = TableStatus.OCCUPIED;
            } else {
                boolean allBillableItemsFinished = order.getOrderItems().stream()
                        .filter(OrderItem::isBillable)
                        .allMatch(item -> item.getStatus() == OrderItemStatus.FINISHED);

                nextStatus = allBillableItemsFinished ? TableStatus.WAITING_FOR_PAYMENT : TableStatus.OCCUPIED;
            }
        }

        updateTables(tables, nextStatus);
    }

    public void releaseTable(Order order) {
        if (order == null) {
            return;
        }

        updateTables(tablesForOrder(order), TableStatus.AVAILABLE);
    }

    private List<DiningTable> tablesForOrder(Order order) {
        if (order.getTableSession() != null && order.getTableSession().getId() != null) {
            List<DiningTable> memberTables = tableSessionResolver.getActiveMemberTables(
                    order.getTableSession().getId());
            if (!memberTables.isEmpty()) {
                return memberTables;
            }
        }

        return order.getTable() == null ? List.of() : List.of(order.getTable());
    }

    private void updateTables(List<DiningTable> tables, TableStatus nextStatus) {
        List<DiningTable> changed = tables.stream()
                .filter(table -> table.getStatus() != TableStatus.INACTIVE)
                .filter(table -> table.getStatus() != nextStatus)
                .peek(table -> table.setStatus(nextStatus))
                .toList();

        if (!changed.isEmpty()) {
            tableRepository.saveAll(changed);
            tableSessionService.evictTableSessionCachesForTables(changed);
            eventPublisher.publishEvent(new TableChangeEvent());
        }
    }
}
