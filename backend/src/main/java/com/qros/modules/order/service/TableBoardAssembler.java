package com.qros.modules.order.service;

import com.qros.modules.order.dto.response.TableBoardResponse;
import com.qros.modules.order.repository.projection.ActiveOrderSummary;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.table.model.TableSessionTable;
import com.qros.modules.table.service.TableNumberComparator;
import com.qros.modules.table.service.TableSessionResolver;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pure read-model assembler transforming active table memberships, dining tables, and
 * active order summaries into {@link TableBoardResponse}.
 */
final class TableBoardAssembler {

    private TableBoardAssembler() {}

    static TableBoardResponse assemble(
            List<TableSessionTable> activeMemberships,
            List<DiningTable> orderedTables,
            List<ActiveOrderSummary> activeOrderSummaries,
            TableSessionResolver tableSessionResolver) {
        Map<Long, TableSessionTable> membershipByTableId = activeMemberships.stream()
                .collect(Collectors.toMap(
                        membership -> membership.getTable().getId(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
        Map<Long, List<DiningTable>> tablesBySessionId = activeMemberships.stream()
                .collect(Collectors.groupingBy(
                        membership -> membership.getSession().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(TableSessionTable::getTable, Collectors.toList())));
        tablesBySessionId.replaceAll((sessionId, tables) -> sortedTables(tables));

        List<TableBoardResponse.TableItem> tableItems = orderedTables.stream()
                .map(table -> toTableItem(table, membershipByTableId, tablesBySessionId, tableSessionResolver))
                .toList();

        List<TableBoardResponse.ActiveOrder> activeOrders = activeOrderSummaries.stream()
                .map(order -> toActiveOrder(order, tablesBySessionId, tableSessionResolver))
                .toList();

        return new TableBoardResponse(tableItems, activeOrders);
    }

    private static TableBoardResponse.TableItem toTableItem(
            DiningTable table,
            Map<Long, TableSessionTable> membershipByTableId,
            Map<Long, List<DiningTable>> tablesBySessionId,
            TableSessionResolver tableSessionResolver) {
        TableSessionTable membership = membershipByTableId.get(table.getId());
        TableSession session = membership != null ? membership.getSession() : null;
        List<DiningTable> memberTables =
                session != null ? tablesBySessionId.getOrDefault(session.getId(), List.of()) : List.of();
        DiningTable primaryTable = session != null ? session.getTable() : null;

        return new TableBoardResponse.TableItem(
                table.getId(),
                table.getTableNumber(),
                table.getTableCode(),
                table.getStatus().name(),
                table.getCapacity(),
                table.getQrCodeUrl(),
                session != null,
                session != null ? session.getStatus().name() : null,
                session != null ? session.getOpenedAt() : null,
                session != null ? session.getLastActivityAt() : null,
                session != null ? session.getId() : null,
                primaryTable != null ? primaryTable.getId() : null,
                memberTables.size() > 1,
                memberTables.stream().map(DiningTable::getId).toList(),
                memberTables.stream().map(DiningTable::getTableNumber).toList(),
                memberTables.isEmpty() ? null : tableSessionResolver.displayName(memberTables),
                session != null ? session.getGuestCount() : null,
                memberTables.isEmpty() ? table.getCapacity() : tableSessionResolver.totalCapacity(memberTables),
                table.getCreatedAt(),
                table.getUpdatedAt());
    }

    private static TableBoardResponse.ActiveOrder toActiveOrder(
            ActiveOrderSummary order,
            Map<Long, List<DiningTable>> tablesBySessionId,
            TableSessionResolver tableSessionResolver) {
        List<DiningTable> memberTables = order.getSessionId() != null
                ? tablesBySessionId.getOrDefault(order.getSessionId(), List.of())
                : List.of();
        Long primaryTableId = memberTables.isEmpty()
                ? order.getTableId()
                : memberTables.getFirst().getId();
        String displayName = memberTables.isEmpty()
                ? "Table " + order.getTableNumber()
                : tableSessionResolver.displayName(memberTables);

        return new TableBoardResponse.ActiveOrder(
                order.getId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getFinalAmount(),
                order.getTableId(),
                order.getTableNumber(),
                order.getSessionId(),
                primaryTableId,
                memberTables.stream().map(DiningTable::getId).toList(),
                memberTables.stream().map(DiningTable::getTableNumber).toList(),
                displayName,
                order.getCreatedAt());
    }

    private static List<DiningTable> sortedTables(List<DiningTable> tables) {
        return tables.stream()
                .sorted(Comparator.comparing(DiningTable::getTableNumber, TableNumberComparator::compare))
                .toList();
    }
}
