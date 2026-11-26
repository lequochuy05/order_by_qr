package com.qros.modules.ai.service;

import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse.MetricsSnapshot;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse.TopDishMetric;
import com.qros.modules.analytics.dto.projection.DashboardSummaryProjection;
import com.qros.modules.analytics.dto.projection.OrderFilterSummaryProjection;
import com.qros.modules.analytics.dto.projection.TableSummaryProjection;
import com.qros.modules.analytics.dto.projection.TopSellingItemProjection;
import com.qros.modules.analytics.service.AnalyticsLookupService;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.service.InventoryLookupService;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.service.TableLookupService;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExecutiveInsightsContextProvider {

    private static final long KITCHEN_OVERDUE_MINUTES = 20L;
    private static final long SERVICE_REQUEST_SLA_MINUTES = 5L;
    private static final int TOP_DISH_LIMIT = 5;
    private static final int LOW_STOCK_NAME_LIMIT = 5;

    private final AnalyticsLookupService analyticsLookupService;
    private final KitchenService kitchenService;
    private final TableLookupService tableLookupService;
    private final InventoryLookupService inventoryLookupService;

    @Transactional(readOnly = true)
    public ExecutiveInsightsData loadInsights(LocalDate from, LocalDate to) {
        LocalDate toExclusive = to.plusDays(1);
        long spanDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousFrom = from.minusDays(spanDays);
        LocalDate previousToExclusive = from;
        LocalDateTime now = AppTime.now();

        DashboardSummaryProjection currentSummary = analyticsLookupService.dashboardSummary(from, toExclusive);
        DashboardSummaryProjection previousSummary =
                analyticsLookupService.dashboardSummary(previousFrom, previousToExclusive);
        OrderFilterSummaryProjection cancelledSummary =
                analyticsLookupService.orderFilterSummary("CANCELLED", from, toExclusive, null, null);
        TableSummaryProjection tableSummary = analyticsLookupService.tableSummary();

        List<TopDishMetric> topDishes =
                analyticsLookupService.topSellingItems(from, toExclusive, TOP_DISH_LIMIT).stream()
                        .map(this::toTopDishMetric)
                        .toList();

        KitchenMetrics kitchenMetrics = collectKitchenMetrics(now);
        ServiceRequestMetrics serviceRequestMetrics =
                collectServiceRequestMetrics(from.atStartOfDay(), toExclusive.atStartOfDay(), now);
        InventoryMetrics inventoryMetrics = collectInventoryMetrics();

        BigDecimal revenue = safeMoney(currentSummary != null ? currentSummary.getTotalRevenue() : null);
        BigDecimal previousRevenue = safeMoney(previousSummary != null ? previousSummary.getTotalRevenue() : null);
        long completedOrders = safeLong(currentSummary != null ? currentSummary.getTotalOrders() : null);
        long previousCompletedOrders = safeLong(previousSummary != null ? previousSummary.getTotalOrders() : null);
        long cancelledOrders = safeLong(cancelledSummary != null ? cancelledSummary.getTotalOrders() : null);
        BigDecimal averageOrderValue = safeMoney(currentSummary != null ? currentSummary.getAverageOrderValue() : null);
        long totalItemsSold = safeLong(currentSummary != null ? currentSummary.getTotalItemsSold() : null);
        long occupiedTables = safeLong(tableSummary != null ? tableSummary.getOccupiedTables() : null);
        long totalTables = safeLong(tableSummary != null ? tableSummary.getTotalTables() : null);

        MetricsSnapshot metrics = new MetricsSnapshot(
                revenue,
                previousRevenue,
                computeChangePercent(revenue, previousRevenue),
                completedOrders,
                previousCompletedOrders,
                cancelledOrders,
                averageOrderValue,
                totalItemsSold,
                occupiedTables,
                totalTables,
                kitchenMetrics.activeItems(),
                kitchenMetrics.overdueItems(),
                kitchenMetrics.maxWaitMinutes(),
                serviceRequestMetrics.totalRequests(),
                serviceRequestMetrics.openRequests(),
                serviceRequestMetrics.overdueRequests(),
                serviceRequestMetrics.topReason(),
                inventoryMetrics.lowStockCount(),
                inventoryMetrics.outOfStockCount(),
                inventoryMetrics.lowStockNames(),
                topDishes);

        return new ExecutiveInsightsData(metrics, formatContext(from, to, spanDays, metrics));
    }

    private TopDishMetric toTopDishMetric(TopSellingItemProjection row) {
        String name = row.getItemName() != null && !row.getItemName().isBlank() ? row.getItemName() : "Unknown";
        return new TopDishMetric(name, safeLong(row.getQuantitySold()), safeMoney(row.getRevenue()));
    }

    private KitchenMetrics collectKitchenMetrics(LocalDateTime now) {
        List<KitchenOrderResponse> orders = kitchenService.getKitchenOrders();
        if (orders == null || orders.isEmpty()) {
            return new KitchenMetrics(0L, 0L, 0L);
        }

        long activeItems = 0L;
        long overdueItems = 0L;
        long maxWaitMinutes = 0L;

        for (KitchenOrderResponse order : orders) {
            if (order.orderItems() == null) {
                continue;
            }
            for (KitchenOrderResponse.KitchenOrderItemResponse item : order.orderItems()) {
                if (item.status() != OrderItemStatus.PENDING && item.status() != OrderItemStatus.COOKING) {
                    continue;
                }
                activeItems++;
                LocalDateTime itemTime = item.createdAt() != null ? item.createdAt() : order.createdAt();
                long waitMinutes = itemTime != null ? Math.max(0L, ChronoUnit.MINUTES.between(itemTime, now)) : 0L;
                if (waitMinutes > maxWaitMinutes) {
                    maxWaitMinutes = waitMinutes;
                }
                if (waitMinutes > KITCHEN_OVERDUE_MINUTES) {
                    overdueItems++;
                }
            }
        }

        return new KitchenMetrics(activeItems, overdueItems, maxWaitMinutes);
    }

    private ServiceRequestMetrics collectServiceRequestMetrics(
            LocalDateTime fromStart, LocalDateTime toExclusiveStart, LocalDateTime now) {
        List<TableServiceRequest> requests =
                tableLookupService.findServiceRequestsByCreatedAtRange(fromStart, toExclusiveStart);
        if (requests == null || requests.isEmpty()) {
            return new ServiceRequestMetrics(0L, 0L, 0L, null);
        }

        long openRequests = 0L;
        long overdueRequests = 0L;
        Map<ServiceRequestReason, Long> reasonCounts = new EnumMap<>(ServiceRequestReason.class);

        for (TableServiceRequest request : requests) {
            ServiceRequestStatus status = request.getStatus();
            if (status == ServiceRequestStatus.PENDING || status == ServiceRequestStatus.CLAIMED) {
                openRequests++;
            }
            if (isOverdueServiceRequest(request, now)) {
                overdueRequests++;
            }
            if (request.getReason() != null) {
                reasonCounts.merge(request.getReason(), 1L, Long::sum);
            }
        }

        String topReason = reasonCounts.entrySet().stream()
                .max(Map.Entry.<ServiceRequestReason, Long>comparingByValue()
                        .thenComparing(entry -> entry.getKey().name()))
                .map(entry -> entry.getKey().name())
                .orElse(null);

        return new ServiceRequestMetrics(requests.size(), openRequests, overdueRequests, topReason);
    }

    private boolean isOverdueServiceRequest(TableServiceRequest request, LocalDateTime now) {
        if (request.getCreatedAt() == null) {
            return false;
        }
        if (request.getStatus() == ServiceRequestStatus.PENDING) {
            return ChronoUnit.MINUTES.between(request.getCreatedAt(), now) > SERVICE_REQUEST_SLA_MINUTES;
        }
        if (request.getClaimedAt() != null) {
            return ChronoUnit.MINUTES.between(request.getCreatedAt(), request.getClaimedAt())
                    > SERVICE_REQUEST_SLA_MINUTES;
        }
        return false;
    }

    private InventoryMetrics collectInventoryMetrics() {
        List<InventoryItem> lowStockItems = inventoryLookupService.findLowStockActiveItems();
        long outOfStockCount = inventoryLookupService.countOutOfStockActiveItems();
        if (lowStockItems == null || lowStockItems.isEmpty()) {
            return new InventoryMetrics(0L, outOfStockCount, List.of());
        }

        List<String> names = lowStockItems.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(InventoryItem::availableQuantity))
                .limit(LOW_STOCK_NAME_LIMIT)
                .map(item -> "%s (%s %s)"
                        .formatted(
                                item.getName(),
                                item.availableQuantity().stripTrailingZeros().toPlainString(),
                                item.getUnit()))
                .toList();

        return new InventoryMetrics(lowStockItems.size(), outOfStockCount, names);
    }

    private Double computeChangePercent(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = safeMoney(current);
        BigDecimal safePrevious = safeMoney(previous);
        if (safePrevious.compareTo(BigDecimal.ZERO) <= 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : null;
        }
        return safeCurrent
                .subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String formatContext(LocalDate from, LocalDate to, long spanDays, MetricsSnapshot metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("PERIOD: ")
                .append(from)
                .append(" to ")
                .append(to)
                .append(" (")
                .append(spanDays)
                .append(" day(s))\n");
        sb.append("REVENUE_CURRENT_VND: ")
                .append(formatMoney(metrics.revenue()))
                .append("\n");
        sb.append("REVENUE_PREVIOUS_VND: ")
                .append(formatMoney(metrics.previousRevenue()))
                .append("\n");
        sb.append("REVENUE_CHANGE_PERCENT: ")
                .append(metrics.revenueChangePercent() != null ? metrics.revenueChangePercent() + "%" : "N/A")
                .append("\n");
        sb.append("COMPLETED_ORDERS: ")
                .append(metrics.completedOrders())
                .append(" (previous period: ")
                .append(metrics.previousCompletedOrders())
                .append(")\n");
        sb.append("CANCELLED_ORDERS: ").append(metrics.cancelledOrders()).append("\n");
        sb.append("AVG_ORDER_VALUE_VND: ")
                .append(formatMoney(metrics.averageOrderValue()))
                .append("\n");
        sb.append("TOTAL_ITEMS_SOLD: ").append(metrics.totalItemsSold()).append("\n");
        sb.append("TABLES_OCCUPIED_NOW: ")
                .append(metrics.occupiedTables())
                .append("/")
                .append(metrics.totalTables())
                .append("\n");
        sb.append("KITCHEN_LIVE: active=")
                .append(metrics.activeKitchenItems())
                .append(", overdue_over_20m=")
                .append(metrics.overdueKitchenItems())
                .append(", max_wait_minutes=")
                .append(metrics.maxKitchenWaitMinutes())
                .append("\n");
        sb.append("SERVICE_REQUESTS: total=")
                .append(metrics.totalServiceRequests())
                .append(", open=")
                .append(metrics.openServiceRequests())
                .append(", sla_overdue_over_5m=")
                .append(metrics.overdueServiceRequests())
                .append(", top_reason=")
                .append(metrics.topServiceRequestReason() != null ? metrics.topServiceRequestReason() : "NONE")
                .append("\n");
        sb.append("INVENTORY_ALERTS: low_stock=")
                .append(metrics.lowStockItems())
                .append(", out_of_stock=")
                .append(metrics.outOfStockItems());
        if (!metrics.lowStockItemNames().isEmpty()) {
            sb.append(", items=[")
                    .append(String.join(", ", metrics.lowStockItemNames()))
                    .append("]");
        }
        sb.append("\nTOP_SELLING_ITEMS:\n");
        if (metrics.topDishes().isEmpty()) {
            sb.append("  - None recorded\n");
        } else {
            for (TopDishMetric dish : metrics.topDishes()) {
                sb.append("  - ")
                        .append(dish.name())
                        .append(" | qty=")
                        .append(dish.quantitySold())
                        .append(" | revenue=")
                        .append(formatMoney(dish.revenue()))
                        .append("\n");
            }
        }
        return sb.toString();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return com.qros.shared.constants.NumericDefaults.safeMoney(value);
    }

    private long safeLong(Long value) {
        return com.qros.shared.constants.NumericDefaults.safeLong(value);
    }

    private String formatMoney(BigDecimal value) {
        return com.qros.modules.ai.support.AiJsonExtractor.formatVndMoney(value);
    }

    public record ExecutiveInsightsData(MetricsSnapshot metrics, String formattedContext) {}

    private record KitchenMetrics(long activeItems, long overdueItems, long maxWaitMinutes) {}

    private record ServiceRequestMetrics(
            long totalRequests, long openRequests, long overdueRequests, String topReason) {}

    private record InventoryMetrics(long lowStockCount, long outOfStockCount, List<String> lowStockNames) {}
}
