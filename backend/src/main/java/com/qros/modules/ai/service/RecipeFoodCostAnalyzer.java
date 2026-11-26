package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;

import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse.FoodCostAnalysis;
import com.qros.modules.ai.dto.response.AiRecipeAssistantResponse.SuggestedRecipeLine;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.NoteSafetyAnalyzer;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.menu.model.MenuItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure deterministic food-cost analyzer, ingredient keyword matcher, and prompt context builder
 * for the AI Recipe BOM Assistant.
 */
final class RecipeFoodCostAnalyzer {

    private static final BigDecimal TARGET_FOOD_COST_RATIO = new BigDecimal("0.35");
    private static final BigDecimal ROUNDING_UNIT = new BigDecimal("1000");
    private static final double WARNING_THRESHOLD_PERCENT = 35.0;
    private static final double CRITICAL_THRESHOLD_PERCENT = 45.0;
    private static final int MAX_INVENTORY_CONTEXT = 60;

    private RecipeFoodCostAnalyzer() {}

    static FoodCostAnalysis computeFoodCostAnalysis(BigDecimal sellingPrice, List<SuggestedRecipeLine> lines) {
        BigDecimal safePrice = AiJsonExtractor.safeScale(sellingPrice, 2);
        if (lines == null || lines.isEmpty()) {
            return new FoodCostAnalysis(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    null,
                    safePrice,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    "NO_RECIPE");
        }

        BigDecimal totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int missingUnitCostCount = 0;
        for (SuggestedRecipeLine line : lines) {
            BigDecimal unitCost = AiJsonExtractor.safeScale(line.unitCost(), 2);
            if (unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                missingUnitCostCount++;
            }
            BigDecimal lineCost = unitCost.multiply(line.quantityRequired()).setScale(2, RoundingMode.HALF_UP);
            totalCost = totalCost.add(lineCost);
        }
        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossProfit = safePrice.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);
        Double foodCostPercent = safePrice.compareTo(BigDecimal.ZERO) > 0
                ? totalCost
                        .multiply(BigDecimal.valueOf(100))
                        .divide(safePrice, 1, RoundingMode.HALF_UP)
                        .doubleValue()
                : null;

        BigDecimal recommendedMinPrice = totalCost.compareTo(BigDecimal.ZERO) > 0
                ? totalCost
                        .divide(TARGET_FOOD_COST_RATIO, 4, RoundingMode.CEILING)
                        .divide(ROUNDING_UNIT, 0, RoundingMode.CEILING)
                        .multiply(ROUNDING_UNIT)
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        String marginStatus = resolveMarginStatus(missingUnitCostCount, foodCostPercent);

        return new FoodCostAnalysis(
                totalCost, foodCostPercent, grossProfit, recommendedMinPrice, missingUnitCostCount, marginStatus);
    }

    static List<SuggestedRecipeLine> annotateExistingLines(List<SuggestedRecipeLine> lines, String language) {
        String reason =
                isEnglish(language) ? "Verified from current BOM line" : "Giữ theo định mức nguyên liệu hiện tại";
        return lines.stream()
                .map(line -> new SuggestedRecipeLine(
                        line.inventoryItemId(),
                        line.inventoryItemName(),
                        line.unit(),
                        line.quantityRequired(),
                        line.unitCost(),
                        line.lineCost(),
                        line.reason() != null && !line.reason().isBlank() ? line.reason() : reason))
                .toList();
    }

    static List<SuggestedRecipeLine> matchIngredientsByName(
            String dishName, List<InventoryItem> activeInventory, String language) {
        if (dishName == null || dishName.isBlank() || activeInventory.isEmpty()) {
            return List.of();
        }
        String normalizedDish = NoteSafetyAnalyzer.foldAccents(dishName);
        Set<String> dishTokens = Arrays.stream(normalizedDish.split(" "))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (dishTokens.isEmpty()) {
            return List.of();
        }

        List<SuggestedRecipeLine> matches = new ArrayList<>();
        for (InventoryItem inv : activeInventory) {
            String normalizedInv = NoteSafetyAnalyzer.foldAccents(inv.getName() == null ? "" : inv.getName());
            boolean tokenHit = normalizedDish.contains(normalizedInv)
                    || dishTokens.stream().anyMatch(normalizedInv::contains);
            if (!tokenHit) {
                continue;
            }
            String reason =
                    isEnglish(language) ? "Matched ingredient keyword in dish name" : "Khớp tên nguyên liệu từ tên món";
            matches.add(toLine(inv, defaultQuantityForUnit(inv.getUnit()), reason));
            if (matches.size() >= 4) {
                break;
            }
        }
        return matches;
    }

    static List<String> buildDefaultCostWarnings(
            BigDecimal sellingPrice, FoodCostAnalysis current, FoodCostAnalysis target, String language) {
        List<String> warnings = new ArrayList<>();
        FoodCostAnalysis focus = !"NO_RECIPE".equals(target.marginStatus()) ? target : current;
        if ("NO_RECIPE".equals(focus.marginStatus())) {
            warnings.add(
                    isEnglish(language)
                            ? "This dish has no BOM recipe yet; add ingredients to track true food cost and margin."
                            : "Món này chưa có định mức nguyên liệu (BOM); hãy thêm nguyên liệu để tính giá vốn chính xác.");
            return warnings;
        }
        if (focus.missingUnitCostCount() > 0) {
            warnings.add(
                    isEnglish(language)
                            ? "%d ingredient(s) have unit cost 0; update unit prices in Inventory to prevent distorted margins."
                                    .formatted(focus.missingUnitCostCount())
                            : "%d nguyên liệu chưa khai báo đơn giá nhập (0đ); cần cập nhật để tránh sai lệch biên lợi nhuận."
                                    .formatted(focus.missingUnitCostCount()));
        }
        if (focus.foodCostPercent() != null && focus.foodCostPercent() > CRITICAL_THRESHOLD_PERCENT) {
            warnings.add(
                    isEnglish(language)
                            ? "Food cost ratio is %.1f%% (>45%% critical threshold), squeezing gross profit."
                                    .formatted(focus.foodCostPercent())
                            : "Tỷ lệ giá vốn %.1f%% vượt ngưỡng rủi ro (>45%%), biên lợi nhuận gộp bị thu hẹp."
                                    .formatted(focus.foodCostPercent()));
        } else if (focus.foodCostPercent() != null && focus.foodCostPercent() > WARNING_THRESHOLD_PERCENT) {
            warnings.add(
                    isEnglish(language)
                            ? "Food cost ratio is %.1f%% (above 35%% target).".formatted(focus.foodCostPercent())
                            : "Tỷ lệ giá vốn %.1f%% đang cao hơn mức mục tiêu 35%%."
                                    .formatted(focus.foodCostPercent()));
        } else if (warnings.isEmpty()) {
            warnings.add(
                    isEnglish(language)
                            ? "Food cost is healthy at %.1f%% vs selling price %s."
                                    .formatted(
                                            focus.foodCostPercent() != null ? focus.foodCostPercent() : 0.0,
                                            sellingPrice.toPlainString())
                            : "Tỷ lệ giá vốn đang ở mức an toàn (%.1f%%)."
                                    .formatted(focus.foodCostPercent() != null ? focus.foodCostPercent() : 0.0));
        }
        return warnings;
    }

    static List<String> buildDefaultPricingAdvice(
            BigDecimal sellingPrice, FoodCostAnalysis current, FoodCostAnalysis target, String language) {
        List<String> advice = new ArrayList<>();
        FoodCostAnalysis focus = !"NO_RECIPE".equals(target.marginStatus()) ? target : current;
        if (focus.recommendedMinPrice().compareTo(sellingPrice) > 0
                && focus.totalFoodCost().compareTo(BigDecimal.ZERO) > 0) {
            String recPrice = "%,.0fđ".formatted(focus.recommendedMinPrice()).replace(",", ".");
            advice.add(
                    isEnglish(language)
                            ? "Consider raising menu price to at least %s to keep food cost <= 35%%."
                                    .formatted(recPrice)
                            : "Cân nhắc điều chỉnh giá bán tối thiểu lên %s để giữ giá vốn <= 35%%."
                                    .formatted(recPrice));
        } else if ("HEALTHY".equals(focus.marginStatus())) {
            advice.add(
                    isEnglish(language)
                            ? "Current margin supports bundling this item in value combos or off-peak promos."
                            : "Biên lợi nhuận tốt, phù hợp đưa món này vào Combo hoặc khuyến mãi khung giờ thấp điểm.");
        } else {
            advice.add(
                    isEnglish(language)
                            ? "Review portion weights on top-cost ingredients or negotiate bulk supplier rates."
                            : "Rà soát lại định lượng thành phần đắt nhất hoặc nhập sỉ để tối ưu giá vốn đơn vị.");
        }
        return advice;
    }

    static String buildRecipeContext(
            MenuItem menuItem,
            BigDecimal sellingPrice,
            List<SuggestedRecipeLine> currentLines,
            FoodCostAnalysis currentAnalysis,
            List<InventoryItem> activeInventory) {
        StringBuilder sb = new StringBuilder();
        sb.append("DISH: id=")
                .append(menuItem.getId())
                .append(" | name=")
                .append(menuItem.getName())
                .append(" | category=")
                .append(menuItem.getCategory() != null ? menuItem.getCategory().getName() : "N/A")
                .append(" | sellingPriceVND=")
                .append(sellingPrice)
                .append(" | description=")
                .append(menuItem.getDescription() != null ? menuItem.getDescription() : "")
                .append('\n');

        sb.append("CURRENT_ANALYSIS: totalCost=")
                .append(currentAnalysis.totalFoodCost())
                .append(", foodCostPercent=")
                .append(currentAnalysis.foodCostPercent())
                .append(", status=")
                .append(currentAnalysis.marginStatus())
                .append('\n');

        sb.append("CURRENT_BOM:\n");
        if (currentLines.isEmpty()) {
            sb.append("- NONE\n");
        } else {
            for (SuggestedRecipeLine line : currentLines) {
                sb.append("- inventoryItemId=")
                        .append(line.inventoryItemId())
                        .append(" | name=")
                        .append(line.inventoryItemName())
                        .append(" | qty=")
                        .append(line.quantityRequired())
                        .append(' ')
                        .append(line.unit())
                        .append(" | unitCost=")
                        .append(line.unitCost())
                        .append('\n');
            }
        }

        sb.append("ACTIVE_INVENTORY_ITEMS:\n");
        activeInventory.stream()
                .limit(MAX_INVENTORY_CONTEXT)
                .forEach(inv -> sb.append("- id=")
                        .append(inv.getId())
                        .append(" | name=")
                        .append(inv.getName())
                        .append(" | unit=")
                        .append(inv.getUnit())
                        .append(" | unitCost=")
                        .append(AiJsonExtractor.safeScale(inv.getUnitCost(), 2))
                        .append('\n'));
        return sb.toString();
    }

    static SuggestedRecipeLine toLine(InventoryItem inv, BigDecimal qty, String reason) {
        BigDecimal unitCost = AiJsonExtractor.safeScale(inv.getUnitCost(), 2);
        BigDecimal normalizedQty = qty.setScale(3, RoundingMode.HALF_UP);
        BigDecimal lineCost = unitCost.multiply(normalizedQty).setScale(2, RoundingMode.HALF_UP);
        return new SuggestedRecipeLine(
                inv.getId(), inv.getName(), inv.getUnit(), normalizedQty, unitCost, lineCost, reason);
    }

    private static String resolveMarginStatus(int missingUnitCostCount, Double foodCostPercent) {
        if (missingUnitCostCount > 0) {
            return "MISSING_COST";
        }
        if (foodCostPercent != null && foodCostPercent > CRITICAL_THRESHOLD_PERCENT) {
            return "CRITICAL";
        }
        if (foodCostPercent != null && foodCostPercent > WARNING_THRESHOLD_PERCENT) {
            return "WARNING";
        }
        return "HEALTHY";
    }

    private static BigDecimal defaultQuantityForUnit(String unit) {
        if (unit == null) {
            return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        }
        String normalized = unit.trim().toLowerCase();
        if ("g".equals(normalized) || "gr".equals(normalized) || "gram".equals(normalized) || "ml".equals(normalized)) {
            return new BigDecimal("100.000");
        }
        if ("kg".equals(normalized) || "l".equals(normalized) || "lit".equals(normalized)) {
            return new BigDecimal("0.150");
        }
        return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
    }
}
