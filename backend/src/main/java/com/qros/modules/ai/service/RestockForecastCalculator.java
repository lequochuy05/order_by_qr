package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;

import com.qros.modules.ai.dto.response.AiRestockSuggestionResponse.RestockItemProposal;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.analytics.dto.projection.MenuItemDemandProjection;
import com.qros.modules.analytics.dto.projection.TopSellingItemProjection;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.RecipeItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure deterministic BOM demand calculator, urgency classifier, and prompt/fallback builder
 * for inventory restock forecasting.
 */
final class RestockForecastCalculator {

    static final int DEMAND_LOOKBACK_DAYS = 30;
    private static final BigDecimal SAFETY_MULTIPLIER = new BigDecimal("1.10");
    private static final int MAX_DRIVEN_DISHES = 3;
    private static final int MAX_PROMPT_ITEMS = 25;

    private RestockForecastCalculator() {}

    static ForecastEvaluation evaluate(
            List<InventoryItem> activeItems,
            List<TopSellingItemProjection> topSellers,
            List<RecipeItem> activeRecipes,
            int forecastDays,
            String language) {
        Map<Long, Long> sold30dByMenuId = new HashMap<>();
        if (topSellers != null) {
            for (TopSellingItemProjection row : topSellers) {
                if (row.getMenuItemId() != null && row.getQuantitySold() != null) {
                    sold30dByMenuId.merge(row.getMenuItemId(), row.getQuantitySold(), Long::sum);
                }
            }
        }
        return evaluateWithDemandMap(activeItems, sold30dByMenuId, activeRecipes, forecastDays, language);
    }

    static ForecastEvaluation evaluateFromMenuDemand(
            List<InventoryItem> activeItems,
            List<MenuItemDemandProjection> menuDemandRows,
            List<RecipeItem> activeRecipes,
            int forecastDays,
            String language) {
        Map<Long, Long> sold30dByMenuId = new HashMap<>();
        if (menuDemandRows != null) {
            for (MenuItemDemandProjection row : menuDemandRows) {
                if (row.getMenuItemId() != null && row.getQuantitySold() != null) {
                    sold30dByMenuId.merge(row.getMenuItemId(), row.getQuantitySold(), Long::sum);
                }
            }
        }
        return evaluateWithDemandMap(activeItems, sold30dByMenuId, activeRecipes, forecastDays, language);
    }

    private static ForecastEvaluation evaluateWithDemandMap(
            List<InventoryItem> activeItems,
            Map<Long, Long> sold30dByMenuId,
            List<RecipeItem> activeRecipes,
            int forecastDays,
            String language) {
        DemandProjection demand = projectIngredientDemand(sold30dByMenuId, activeRecipes, forecastDays);
        List<EvaluatedItem> evaluated = activeItems.stream()
                .map(item -> evaluateSingleItem(item, demand, forecastDays, language))
                .sorted(Comparator.comparingInt(EvaluatedItem::rank)
                        .reversed()
                        .thenComparing((EvaluatedItem e) -> e.proposal().shortfall(), Comparator.reverseOrder())
                        .thenComparing(e -> e.proposal().daysUntilStockout() != null
                                ? e.proposal().daysUntilStockout()
                                : Double.MAX_VALUE)
                        .thenComparing(e -> e.proposal().name(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<RestockItemProposal> proposals =
                evaluated.stream().map(EvaluatedItem::proposal).toList();
        int criticalCount = (int)
                proposals.stream().filter(p -> "CRITICAL".equals(p.urgency())).count();
        int highCount =
                (int) proposals.stream().filter(p -> "HIGH".equals(p.urgency())).count();
        BigDecimal totalEstimatedCost = proposals.stream()
                .filter(p -> p.suggestedRestockQty().compareTo(BigDecimal.ZERO) > 0)
                .map(RestockItemProposal::estimatedCost)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        return new ForecastEvaluation(proposals, criticalCount, highCount, totalEstimatedCost);
    }

    private static DemandProjection projectIngredientDemand(
            Map<Long, Long> sold30dByMenuId, List<RecipeItem> activeRecipes, int forecastDays) {
        Map<Long, BigDecimal> dailyUsageByInventoryId = new HashMap<>();
        Map<Long, Map<String, BigDecimal>> dishDemandByInventoryId = new HashMap<>();

        for (RecipeItem recipe : activeRecipes) {
            if (recipe.getMenuItem() == null
                    || recipe.getInventoryItem() == null
                    || recipe.getQuantityRequired() == null) {
                continue;
            }
            Long menuId = recipe.getMenuItem().getId();
            Long invId = recipe.getInventoryItem().getId();
            long sold30d = sold30dByMenuId.getOrDefault(menuId, 0L);
            if (sold30d <= 0) {
                continue;
            }
            BigDecimal dailyDishRate = BigDecimal.valueOf(sold30d)
                    .divide(BigDecimal.valueOf(DEMAND_LOOKBACK_DAYS), 6, RoundingMode.HALF_UP);
            BigDecimal dailyIngredientUsage = dailyDishRate.multiply(recipe.getQuantityRequired());
            dailyUsageByInventoryId.merge(invId, dailyIngredientUsage, BigDecimal::add);

            BigDecimal projectedDishIngredient = dailyIngredientUsage
                    .multiply(BigDecimal.valueOf(forecastDays))
                    .multiply(SAFETY_MULTIPLIER);
            dishDemandByInventoryId
                    .computeIfAbsent(invId, k -> new LinkedHashMap<>())
                    .merge(recipe.getMenuItem().getName(), projectedDishIngredient, BigDecimal::add);
        }

        return new DemandProjection(dailyUsageByInventoryId, dishDemandByInventoryId);
    }

    private static EvaluatedItem evaluateSingleItem(
            InventoryItem item, DemandProjection demand, int forecastDays, String language) {
        BigDecimal available = item.availableQuantity();
        BigDecimal threshold = AiJsonExtractor.safeScale(item.getLowStockThreshold(), 3);
        BigDecimal unitCost = AiJsonExtractor.safeScale(item.getUnitCost(), 2);
        BigDecimal dailyUsage = demand.dailyUsageByInventoryId().getOrDefault(item.getId(), BigDecimal.ZERO);

        BigDecimal projectedUsage = dailyUsage
                .multiply(BigDecimal.valueOf(forecastDays))
                .multiply(SAFETY_MULTIPLIER)
                .setScale(3, RoundingMode.HALF_UP);
        BigDecimal shortfall =
                projectedUsage.subtract(available).max(BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
        BigDecimal thresholdGap = threshold.subtract(available).max(BigDecimal.ZERO);
        BigDecimal suggestedRestockQty = shortfall.max(thresholdGap).setScale(3, RoundingMode.HALF_UP);
        BigDecimal estimatedCost = suggestedRestockQty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

        Double daysUntilStockout = dailyUsage.compareTo(BigDecimal.ZERO) > 0
                ? available.divide(dailyUsage, 1, RoundingMode.HALF_UP).doubleValue()
                : null;

        String urgency = resolveUrgency(available, daysUntilStockout, shortfall, item.isLowStock());
        List<String> drivenByDishes =
                demand.dishDemandByInventoryId().getOrDefault(item.getId(), Map.of()).entrySet().stream()
                        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                        .limit(MAX_DRIVEN_DISHES)
                        .map(Map.Entry::getKey)
                        .toList();

        String defaultReason = buildDeterministicReason(
                urgency, available, projectedUsage, daysUntilStockout, threshold, item.getUnit(), language);

        return new EvaluatedItem(
                new RestockItemProposal(
                        item.getId(),
                        item.getName(),
                        item.getUnit(),
                        available,
                        threshold,
                        unitCost,
                        projectedUsage,
                        shortfall,
                        suggestedRestockQty,
                        estimatedCost,
                        daysUntilStockout,
                        urgency,
                        drivenByDishes,
                        defaultReason),
                urgencyRank(urgency));
    }

    static String buildContextString(int forecastDays, ForecastEvaluation evaluation) {
        StringBuilder sb = new StringBuilder();
        sb.append("FORECAST_DAYS: ").append(forecastDays).append('\n');
        sb.append("CRITICAL_COUNT: ")
                .append(evaluation.criticalCount())
                .append(", HIGH_COUNT: ")
                .append(evaluation.highCount())
                .append(", TOTAL_EST_COST_VND: ")
                .append(evaluation.totalEstimatedCost())
                .append('\n');
        sb.append("PRIORITY_ITEMS:\n");
        evaluation.proposals().stream()
                .limit(MAX_PROMPT_ITEMS)
                .forEach(item -> sb.append("- id=")
                        .append(item.inventoryItemId())
                        .append(" | name=")
                        .append(item.name())
                        .append(" | unit=")
                        .append(item.unit())
                        .append(" | available=")
                        .append(item.availableQuantity())
                        .append(" | threshold=")
                        .append(item.lowStockThreshold())
                        .append(" | projected=")
                        .append(item.projectedUsage())
                        .append(" | shortfall=")
                        .append(item.shortfall())
                        .append(" | suggestedQty=")
                        .append(item.suggestedRestockQty())
                        .append(" | daysLeft=")
                        .append(item.daysUntilStockout() != null ? item.daysUntilStockout() : "N/A")
                        .append(" | urgency=")
                        .append(item.urgency())
                        .append(" | drivenBy=")
                        .append(String.join(", ", item.drivenByDishes()))
                        .append('\n'));
        return sb.toString();
    }

    static String buildDefaultSummary(
            int forecastDays, String language, int criticalCount, int highCount, int totalItems) {
        if (isEnglish(language)) {
            if (criticalCount == 0 && highCount == 0) {
                return "Stock across %d active ingredients looks stable for the next %d days."
                        .formatted(totalItems, forecastDays);
            }
            return "Forecast over %d days flags %d critical and %d high-priority ingredients needing restock."
                    .formatted(forecastDays, criticalCount, highCount);
        }
        if (criticalCount == 0 && highCount == 0) {
            return "Tồn kho của %d nguyên liệu hiện đủ đáp ứng nhu cầu dự kiến trong %d ngày tới."
                    .formatted(totalItems, forecastDays);
        }
        return "Dự báo %d ngày tới ghi nhận %d nguyên liệu khẩn cấp và %d nguyên liệu thiếu hụt cần nhập thêm."
                .formatted(forecastDays, criticalCount, highCount);
    }

    static List<String> buildDefaultRecommendations(
            int forecastDays, String language, List<RestockItemProposal> proposals) {
        List<String> recs = new ArrayList<>();
        List<RestockItemProposal> urgent = proposals.stream()
                .filter(p -> "CRITICAL".equals(p.urgency()) || "HIGH".equals(p.urgency()))
                .limit(2)
                .toList();
        for (RestockItemProposal item : urgent) {
            if (isEnglish(language)) {
                recs.add("Restock %s: suggested +%s %s to cover %d-day BOM demand."
                        .formatted(
                                item.name(),
                                item.suggestedRestockQty().stripTrailingZeros().toPlainString(),
                                item.unit(),
                                forecastDays));
            } else {
                recs.add("Ưu tiên nhập %s: đề xuất +%s %s để đáp ứng định mức %d ngày."
                        .formatted(
                                item.name(),
                                item.suggestedRestockQty().stripTrailingZeros().toPlainString(),
                                item.unit(),
                                forecastDays));
            }
        }
        if (recs.isEmpty()) {
            recs.add(
                    isEnglish(language)
                            ? "Maintain regular cycle counts and keep supplier lead times updated."
                            : "Duy trì kiểm kho định kỳ và rà soát ngưỡng cảnh báo cho các món bán chạy.");
        }
        return recs;
    }

    private static String buildDeterministicReason(
            String urgency,
            BigDecimal available,
            BigDecimal projectedUsage,
            Double daysUntilStockout,
            BigDecimal threshold,
            String unit,
            String language) {
        String availStr = available.stripTrailingZeros().toPlainString();
        String projStr = projectedUsage.stripTrailingZeros().toPlainString();
        if (isEnglish(language)) {
            return switch (urgency) {
                case "CRITICAL" ->
                    available.compareTo(BigDecimal.ZERO) <= 0
                            ? "Out of stock (0 %s available)".formatted(unit)
                            : "Covers only ~%s days vs forecast %s %s".formatted(daysUntilStockout, projStr, unit);
                case "HIGH" -> "Available %s %s < BOM forecast %s %s".formatted(availStr, unit, projStr, unit);
                case "MEDIUM" ->
                    "At or below low-stock threshold (%s %s)"
                            .formatted(threshold.stripTrailingZeros().toPlainString(), unit);
                default -> "Sufficient for forecast demand (%s %s available)".formatted(availStr, unit);
            };
        }
        return switch (urgency) {
            case "CRITICAL" ->
                available.compareTo(BigDecimal.ZERO) <= 0
                        ? "Đã hết hàng khả dụng (0 %s)".formatted(unit)
                        : "Chỉ đủ dùng khoảng %s ngày (dự báo cần %s %s)".formatted(daysUntilStockout, projStr, unit);
            case "HIGH" -> "Tồn %s %s thấp hơn dự báo tiêu thụ %s %s".formatted(availStr, unit, projStr, unit);
            case "MEDIUM" ->
                "Chạm hoặc dưới ngưỡng cảnh báo (%s %s)"
                        .formatted(threshold.stripTrailingZeros().toPlainString(), unit);
            default -> "Đủ đáp ứng dự báo (%s %s khả dụng)".formatted(availStr, unit);
        };
    }

    private static String resolveUrgency(
            BigDecimal available, Double daysUntilStockout, BigDecimal shortfall, boolean isLowStock) {
        if (available.compareTo(BigDecimal.ZERO) <= 0 || (daysUntilStockout != null && daysUntilStockout < 2.0)) {
            return "CRITICAL";
        }
        if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
            return "HIGH";
        }
        if (isLowStock) {
            return "MEDIUM";
        }
        return "OK";
    }

    private static int urgencyRank(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    record ForecastEvaluation(
            List<RestockItemProposal> proposals, int criticalCount, int highCount, BigDecimal totalEstimatedCost) {}

    private record DemandProjection(
            Map<Long, BigDecimal> dailyUsageByInventoryId,
            Map<Long, Map<String, BigDecimal>> dishDemandByInventoryId) {}

    private record EvaluatedItem(RestockItemProposal proposal, int rank) {}
}
