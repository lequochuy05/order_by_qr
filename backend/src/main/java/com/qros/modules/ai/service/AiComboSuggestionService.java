package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;
import static com.qros.modules.ai.support.AiJsonExtractor.normalizeLanguage;
import static com.qros.modules.ai.support.AiJsonExtractor.readText;
import static com.qros.modules.ai.support.AiJsonExtractor.truncate;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiMerchandisingRequest;
import com.qros.modules.ai.dto.response.AiComboSuggestionsResponse;
import com.qros.modules.ai.dto.response.AiComboSuggestionsResponse.ComboItemProposal;
import com.qros.modules.ai.dto.response.AiComboSuggestionsResponse.ComboProposal;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.MenuContextProvider.MenuCatalogSnapshot;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.dto.projection.TopSellingItemProjection;
import com.qros.modules.analytics.service.AnalyticsLookupService;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.order.service.OrderLookupService;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Generates menu combo recommendations grounded in order co-occurrence, top-seller metrics,
 * and existing active combos, falling back deterministically when LLM output is unavailable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiComboSuggestionService {

    private static final int PAIRING_LOOKBACK_DAYS = 60;
    private static final int TRAFFIC_LOOKBACK_DAYS = 30;
    private static final int MAX_SUGGESTIONS = 3;
    private static final int MAX_COMBO_ITEMS = 4;
    private static final int MAX_ITEM_QTY = 3;

    private final MenuContextProvider menuContextProvider;
    private final MenuCatalogLookupService menuCatalogLookupService;
    private final OrderLookupService orderLookupService;
    private final AnalyticsLookupService analyticsLookupService;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public AiComboSuggestionsResponse suggestCombos(AiMerchandisingRequest request, Runnable onAiCall) {
        String language = normalizeLanguage(request != null ? request.language() : null);
        LocalDateTime now = AppTime.now();
        LocalDate today = now.toLocalDate();

        MenuCatalogSnapshot snapshot = menuContextProvider.loadAvailableCatalog();
        Map<Long, MenuItem> availableItems = snapshot.menuItemsById();
        if (availableItems.size() < 2) {
            String emptyMsg = isEnglish(language)
                    ? "At least 2 active, available menu items are required to recommend combos."
                    : "Cần tối thiểu 2 món đang mở bán để AI gợi ý ghép Combo.";
            return new AiComboSuggestionsResponse(language, emptyMsg, List.of(), now, false);
        }

        List<CoOccurrencePair> validPairs = loadValidCoOccurrencePairs(now, availableItems);
        List<TopSellingItemProjection> topSellers = analyticsLookupService.topSellingItems(
                today.minusDays(TRAFFIC_LOOKBACK_DAYS - 1L), today.plusDays(1), 15);
        List<Combo> existingCombos = menuCatalogLookupService.findAllActiveCombosWithItems();
        Set<Set<Long>> existingSignatures = buildExistingComboSignatures(existingCombos);

        String context = buildComboContext(availableItems, validPairs, topSellers, existingCombos);
        String systemPrompt = promptBuilder.buildComboSuggestionPrompt(context, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Suggest profitable menu combos from co-occurrence and top sellers.", List.of()),
                systemPrompt,
                "");
        if (onAiCall != null) {
            onAiCall.run();
        }

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildComboFallback(language, availableItems, validPairs, topSellers, existingSignatures, now);
        }

        AiComboSuggestionsResponse parsed =
                tryParseComboResponse(language, availableItems, existingSignatures, rawReply, now);
        return parsed != null
                ? parsed
                : buildComboFallback(language, availableItems, validPairs, topSellers, existingSignatures, now);
    }

    private AiComboSuggestionsResponse tryParseComboResponse(
            String language,
            Map<Long, MenuItem> availableItems,
            Set<Set<Long>> existingSignatures,
            String rawReply,
            LocalDateTime generatedAt) {
        String extracted = AiJsonExtractor.extractJsonObject(rawReply);
        if (extracted == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extracted);
            String summary = readText(root.path("summary"), "");
            JsonNode combosNode = root.path("combos");
            if (!combosNode.isArray()) {
                return null;
            }

            List<ComboProposal> proposals = new ArrayList<>();
            Set<Set<Long>> seenSignatures = new LinkedHashSet<>(existingSignatures);

            for (JsonNode comboNode : combosNode) {
                ComboProposal validated = parseSingleCombo(comboNode, availableItems, language);
                if (validated == null) {
                    continue;
                }
                Set<Long> signature = validated.items().stream()
                        .map(ComboItemProposal::menuItemId)
                        .collect(Collectors.toSet());
                if (!seenSignatures.add(signature)) {
                    continue;
                }
                proposals.add(validated);
                if (proposals.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            }

            if (proposals.isEmpty()) {
                return null;
            }

            String finalSummary = summary.isBlank() ? buildComboFallbackSummary(language, proposals.size()) : summary;
            return new AiComboSuggestionsResponse(language, finalSummary, proposals, generatedAt, true);
        } catch (Exception ex) {
            log.debug("Failed to parse AI combo JSON payload: {}", ex.getMessage());
            return null;
        }
    }

    private ComboProposal parseSingleCombo(JsonNode comboNode, Map<Long, MenuItem> availableItems, String language) {
        JsonNode itemsNode = comboNode.path("items");
        if (!itemsNode.isArray()) {
            return null;
        }

        Map<Long, ComboItemProposal> dedupedItems = new LinkedHashMap<>();
        for (JsonNode entry : itemsNode) {
            long menuItemId = entry.path("menuItemId").asLong(-1L);
            MenuItem menuItem = availableItems.get(menuItemId);
            if (menuItem == null || dedupedItems.containsKey(menuItemId)) {
                continue;
            }
            int qty = Math.max(1, Math.min(MAX_ITEM_QTY, entry.path("quantity").asInt(1)));
            String categoryName =
                    menuItem.getCategory() != null ? menuItem.getCategory().getName() : "";
            dedupedItems.put(
                    menuItemId,
                    new ComboItemProposal(
                            menuItem.getId(),
                            menuItem.getName(),
                            safeCurrency(menuItem.getPrice()),
                            qty,
                            categoryName));
            if (dedupedItems.size() >= MAX_COMBO_ITEMS) {
                break;
            }
        }

        if (dedupedItems.size() < 2) {
            return null;
        }

        List<ComboItemProposal> items = List.copyOf(dedupedItems.values());
        BigDecimal originalTotal = calculateOriginalTotal(items);
        BigDecimal suggestedPrice =
                normalizeComboPrice(readBigDecimal(comboNode.path("suggestedPrice")), originalTotal);
        int discountPercent = computeDiscountPercent(originalTotal, suggestedPrice);

        String defaultName =
                items.stream().map(ComboItemProposal::name).limit(2).collect(Collectors.joining(" + "));
        String name = truncate(readText(comboNode.path("name"), "Combo " + defaultName), 90);
        String defaultDesc = isEnglish(language)
                ? "Curated pairing of %s".formatted(defaultName)
                : "Bộ đôi ưu đãi gồm %s".formatted(defaultName);
        String description = truncate(readText(comboNode.path("description"), defaultDesc), 200);
        String defaultRationale = isEnglish(language)
                ? "Encourages higher average check with ~%d%% bundle savings.".formatted(discountPercent)
                : "Giúp tăng giá trị hóa đơn trung bình với mức tiết kiệm ~%d%%.".formatted(discountPercent);
        String rationale = truncate(readText(comboNode.path("rationale"), defaultRationale), 240);

        return new ComboProposal(name, description, items, originalTotal, suggestedPrice, discountPercent, rationale);
    }

    private AiComboSuggestionsResponse buildComboFallback(
            String language,
            Map<Long, MenuItem> availableItems,
            List<CoOccurrencePair> validPairs,
            List<TopSellingItemProjection> topSellers,
            Set<Set<Long>> existingSignatures,
            LocalDateTime generatedAt) {
        List<ComboProposal> proposals = new ArrayList<>();
        Set<Set<Long>> usedSignatures = new LinkedHashSet<>(existingSignatures);

        appendCoOccurrenceProposals(proposals, usedSignatures, availableItems, validPairs, language);
        appendTopSellerFallbackProposals(proposals, usedSignatures, availableItems, topSellers, language);

        return new AiComboSuggestionsResponse(
                language, buildComboFallbackSummary(language, proposals.size()), proposals, generatedAt, false);
    }

    private void appendCoOccurrenceProposals(
            List<ComboProposal> proposals,
            Set<Set<Long>> usedSignatures,
            Map<Long, MenuItem> availableItems,
            List<CoOccurrencePair> validPairs,
            String language) {
        for (CoOccurrencePair pair : validPairs) {
            if (proposals.size() >= MAX_SUGGESTIONS) {
                break;
            }
            MenuItem first = availableItems.get(pair.firstId());
            MenuItem second = availableItems.get(pair.secondId());
            if (first == null || second == null || Objects.equals(first.getId(), second.getId())) {
                continue;
            }
            if (!usedSignatures.add(Set.of(first.getId(), second.getId()))) {
                continue;
            }
            String rationale = isEnglish(language)
                    ? "Ordered together %d times in the last %d days."
                            .formatted(pair.pairCount(), PAIRING_LOOKBACK_DAYS)
                    : "Khách thường gọi kèm nhau (%d đơn chung trong %d ngày qua)."
                            .formatted(pair.pairCount(), PAIRING_LOOKBACK_DAYS);
            proposals.add(buildFallbackComboProposal(List.of(first, second), rationale, language));
        }
    }

    private void appendTopSellerFallbackProposals(
            List<ComboProposal> proposals,
            Set<Set<Long>> usedSignatures,
            Map<Long, MenuItem> availableItems,
            List<TopSellingItemProjection> topSellers,
            String language) {
        List<MenuItem> prioritizedPool = buildPrioritizedItemPool(availableItems, topSellers);
        for (int i = 0; i < prioritizedPool.size() && proposals.size() < 2; i++) {
            for (int j = i + 1; j < prioritizedPool.size() && proposals.size() < 2; j++) {
                MenuItem a = prioritizedPool.get(i);
                MenuItem b = prioritizedPool.get(j);
                if (!usedSignatures.add(Set.of(a.getId(), b.getId()))) {
                    continue;
                }
                String rationale = isEnglish(language)
                        ? "Combines popular menu choices to lift average order value."
                        : "Kết hợp các món dễ bán giúp tăng giá trị đơn trung bình.";
                proposals.add(buildFallbackComboProposal(List.of(a, b), rationale, language));
            }
        }
    }

    private ComboProposal buildFallbackComboProposal(List<MenuItem> sourceItems, String rationale, String language) {
        List<ComboItemProposal> items = sourceItems.stream()
                .map(item -> new ComboItemProposal(
                        item.getId(),
                        item.getName(),
                        safeCurrency(item.getPrice()),
                        1,
                        item.getCategory() != null ? item.getCategory().getName() : ""))
                .toList();
        BigDecimal originalTotal = calculateOriginalTotal(items);
        BigDecimal suggestedPrice = normalizeComboPrice(null, originalTotal);
        int discountPercent = computeDiscountPercent(originalTotal, suggestedPrice);
        String joinedNames = items.stream().map(ComboItemProposal::name).collect(Collectors.joining(" + "));
        String name = truncate("Combo " + joinedNames, 90);
        String description = isEnglish(language)
                ? "Bundle %s & save %d%%".formatted(joinedNames, discountPercent)
                : "Combo %s tiết kiệm %d%%".formatted(joinedNames, discountPercent);
        return new ComboProposal(
                name, truncate(description, 200), items, originalTotal, suggestedPrice, discountPercent, rationale);
    }

    private List<CoOccurrencePair> loadValidCoOccurrencePairs(LocalDateTime now, Map<Long, MenuItem> availableItems) {
        List<Object[]> rawPairs = orderLookupService.findFrequentlyPairedMenuItems(
                now.minusDays(PAIRING_LOOKBACK_DAYS), PageRequest.of(0, 12));
        List<CoOccurrencePair> valid = new ArrayList<>();
        for (Object[] row : rawPairs) {
            if (row == null || row.length < 5 || !(row[0] instanceof Number) || !(row[2] instanceof Number)) {
                continue;
            }
            Long id1 = ((Number) row[0]).longValue();
            Long id2 = ((Number) row[2]).longValue();
            long count = row[4] instanceof Number num ? num.longValue() : 0L;
            if (availableItems.containsKey(id1) && availableItems.containsKey(id2) && !Objects.equals(id1, id2)) {
                valid.add(new CoOccurrencePair(id1, id2, count));
            }
        }
        return valid;
    }

    private static Set<Set<Long>> buildExistingComboSignatures(List<Combo> existingCombos) {
        Set<Set<Long>> signatures = new LinkedHashSet<>();
        for (Combo combo : existingCombos) {
            if (combo.getItems() == null) {
                continue;
            }
            Set<Long> itemIds = combo.getItems().stream()
                    .filter(ci -> !ci.isDeleted()
                            && ci.getMenuItem() != null
                            && ci.getMenuItem().getId() != null)
                    .map(ci -> ci.getMenuItem().getId())
                    .collect(Collectors.toSet());
            if (itemIds.size() >= 2) {
                signatures.add(itemIds);
            }
        }
        return signatures;
    }

    private static List<MenuItem> buildPrioritizedItemPool(
            Map<Long, MenuItem> availableItems, List<TopSellingItemProjection> topSellers) {
        LinkedHashMap<Long, MenuItem> ordered = new LinkedHashMap<>();
        for (TopSellingItemProjection top : topSellers) {
            if (top == null || top.getMenuItemId() == null) {
                continue;
            }
            MenuItem item = availableItems.get(top.getMenuItemId());
            if (item != null) {
                ordered.putIfAbsent(item.getId(), item);
            }
        }
        availableItems.values().forEach(item -> ordered.putIfAbsent(item.getId(), item));
        return List.copyOf(ordered.values());
    }

    private static String buildComboContext(
            Map<Long, MenuItem> availableItems,
            List<CoOccurrencePair> pairs,
            List<TopSellingItemProjection> topSellers,
            List<Combo> existingCombos) {
        StringBuilder sb = new StringBuilder();
        sb.append("AVAILABLE_MENU_ITEMS:\n");
        availableItems.values().stream()
                .limit(45)
                .forEach(item -> sb.append("- id=")
                        .append(item.getId())
                        .append(" | name=")
                        .append(item.getName())
                        .append(" | category=")
                        .append(item.getCategory() != null ? item.getCategory().getName() : "N/A")
                        .append(" | priceVND=")
                        .append(safeCurrency(item.getPrice()))
                        .append('\n'));

        sb.append("FREQUENT_CO_OCCURRENCE_PAIRS:\n");
        if (pairs.isEmpty()) {
            sb.append("- NONE\n");
        } else {
            pairs.forEach(p -> sb.append("- [")
                    .append(p.firstId())
                    .append("] ")
                    .append(availableItems.get(p.firstId()).getName())
                    .append(" + [")
                    .append(p.secondId())
                    .append("] ")
                    .append(availableItems.get(p.secondId()).getName())
                    .append(" -> ")
                    .append(p.pairCount())
                    .append(" shared orders\n"));
        }

        sb.append("TOP_SELLING_ITEMS_30D:\n");
        topSellers.stream()
                .filter(t -> t != null && t.getMenuItemId() != null && availableItems.containsKey(t.getMenuItemId()))
                .forEach(t -> sb.append("- [")
                        .append(t.getMenuItemId())
                        .append("] ")
                        .append(t.getItemName())
                        .append(" | sold=")
                        .append(t.getQuantitySold())
                        .append('\n'));

        sb.append("EXISTING_COMBOS:\n");
        if (existingCombos.isEmpty()) {
            sb.append("- NONE\n");
        } else {
            existingCombos.forEach(c -> {
                String components = c.getItems() == null
                        ? ""
                        : c.getItems().stream()
                                .filter(ci -> !ci.isDeleted() && ci.getMenuItem() != null)
                                .map(ci -> ci.getMenuItem().getId() + ":"
                                        + ci.getMenuItem().getName())
                                .collect(Collectors.joining(", "));
                sb.append("- ")
                        .append(c.getName())
                        .append(" (price=")
                        .append(safeCurrency(c.getPrice()))
                        .append(") -> [")
                        .append(components)
                        .append("]\n");
            });
        }
        return sb.toString();
    }

    private static BigDecimal calculateOriginalTotal(List<ComboItemProposal> items) {
        return items.stream()
                .map(i -> safeCurrency(i.unitPrice()).multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeComboPrice(BigDecimal rawSuggested, BigDecimal originalTotal) {
        if (originalTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (originalTotal.compareTo(new BigDecimal("1000")) <= 0) {
            return originalTotal
                    .multiply(new BigDecimal("0.90"))
                    .setScale(0, RoundingMode.HALF_UP)
                    .setScale(2);
        }

        BigDecimal maxAllowed = originalTotal.subtract(new BigDecimal("1000"));
        BigDecimal candidate;
        if (rawSuggested != null
                && rawSuggested.compareTo(BigDecimal.ZERO) > 0
                && rawSuggested.compareTo(originalTotal) < 0) {
            candidate = rawSuggested;
        } else {
            candidate = originalTotal.multiply(new BigDecimal("0.88"));
        }

        long roundedThousand = Math.round(candidate.doubleValue() / 1000.0) * 1000L;
        BigDecimal normalized = BigDecimal.valueOf(Math.max(1000L, roundedThousand));
        if (normalized.compareTo(originalTotal) >= 0) {
            long floorThousand = ((long) (maxAllowed.doubleValue() / 1000.0)) * 1000L;
            normalized = BigDecimal.valueOf(Math.max(1000L, floorThousand));
        }
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    private static int computeDiscountPercent(BigDecimal originalTotal, BigDecimal suggestedPrice) {
        if (originalTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        double ratio = 1.0 - (suggestedPrice.doubleValue() / originalTotal.doubleValue());
        return Math.max(1, (int) Math.round(ratio * 100.0));
    }

    private static String buildComboFallbackSummary(String language, int count) {
        if (isEnglish(language)) {
            return "Proposed %d curated combos validated against active menu prices and co-occurrence patterns."
                    .formatted(count);
        }
        return "Đề xuất %d Combo tối ưu dựa trên món đang bán và thói quen gọi kèm của khách.".formatted(count);
    }

    private static BigDecimal readBigDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return node.isNumber()
                    ? node.decimalValue()
                    : new BigDecimal(node.asString("").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal safeCurrency(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private record CoOccurrencePair(Long firstId, Long secondId, long pairCount) {}
}
