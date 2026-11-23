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
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse;
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse.PromotionProposal;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.MenuContextProvider.MenuCatalogSnapshot;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.repository.projection.DayOfWeekTrafficProjection;
import com.qros.modules.analytics.repository.projection.HourlyTrafficProjection;
import com.qros.modules.analytics.repository.projection.TopSellingItemProjection;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.promotion.model.Promotion;
import com.qros.modules.promotion.repository.PromotionRepository;
import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiMerchandisingService {

    private static final int PAIRING_LOOKBACK_DAYS = 60;
    private static final int TRAFFIC_LOOKBACK_DAYS = 30;
    private static final int MAX_SUGGESTIONS = 3;
    private static final int MAX_COMBO_ITEMS = 4;
    private static final int MAX_ITEM_QTY = 3;
    private static final double MIN_PROMO_DISCOUNT = 5.0;
    private static final double MAX_PROMO_DISCOUNT = 40.0;
    private static final LocalTime DEFAULT_OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSING_TIME = LocalTime.of(22, 0);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MenuContextProvider menuContextProvider;
    private final ComboRepository comboRepository;
    private final OrderItemRepository orderItemRepository;
    private final AnalyticsQueryRepository analyticsQueryRepository;
    private final PromotionRepository promotionRepository;
    private final SystemSettingsService systemSettingsService;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiMerchandisingService(
            MenuContextProvider menuContextProvider,
            ComboRepository comboRepository,
            OrderItemRepository orderItemRepository,
            AnalyticsQueryRepository analyticsQueryRepository,
            PromotionRepository promotionRepository,
            SystemSettingsService systemSettingsService,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.menuContextProvider = menuContextProvider;
        this.comboRepository = comboRepository;
        this.orderItemRepository = orderItemRepository;
        this.analyticsQueryRepository = analyticsQueryRepository;
        this.promotionRepository = promotionRepository;
        this.systemSettingsService = systemSettingsService;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "merchandising")
                .description("AI combo and promotion merchandising suggestion requests")
                .register(meterRegistry);
    }

    public AiComboSuggestionsResponse suggestCombos(AiMerchandisingRequest request) {
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
        List<TopSellingItemProjection> topSellers = analyticsQueryRepository.topSellingItems(
                today.minusDays(TRAFFIC_LOOKBACK_DAYS - 1L), today.plusDays(1), 15);
        List<Combo> existingCombos = comboRepository.findAllActiveWithItems();
        Set<Set<Long>> existingSignatures = buildExistingComboSignatures(existingCombos);

        String context = buildComboContext(availableItems, validPairs, topSellers, existingCombos);
        String systemPrompt = promptBuilder.buildComboSuggestionPrompt(context, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Suggest profitable menu combos from co-occurrence and top sellers.", List.of()),
                systemPrompt,
                "");
        aiRequestsCounter.increment();

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildComboFallback(language, availableItems, validPairs, topSellers, existingSignatures, now);
        }

        AiComboSuggestionsResponse parsed =
                tryParseComboResponse(language, availableItems, existingSignatures, rawReply, now);
        return parsed != null
                ? parsed
                : buildComboFallback(language, availableItems, validPairs, topSellers, existingSignatures, now);
    }

    public AiPromotionSuggestionsResponse suggestPromotions(AiMerchandisingRequest request) {
        String language = normalizeLanguage(request != null ? request.language() : null);
        LocalDateTime now = AppTime.now();
        LocalDate today = now.toLocalDate();

        List<HourlyTrafficProjection> hourlyTraffic = analyticsQueryRepository.hourlyOrderTraffic(
                today.minusDays(TRAFFIC_LOOKBACK_DAYS - 1L), today.plusDays(1));
        List<DayOfWeekTrafficProjection> dayTraffic = analyticsQueryRepository.dayOfWeekOrderTraffic(
                today.minusDays(TRAFFIC_LOOKBACK_DAYS - 1L), today.plusDays(1));
        PublicSettingsResponse settings = systemSettingsService.getPublicSettings();
        List<Promotion> existingPromotions = promotionRepository.findAllWithDays();

        LocalTime openTime = resolveOpeningTime(settings);
        LocalTime closeTime = resolveClosingTime(settings, openTime);

        String context = buildTrafficContext(openTime, closeTime, hourlyTraffic, dayTraffic, existingPromotions);
        String systemPrompt = promptBuilder.buildPromotionSuggestionPrompt(context, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Suggest off-peak promotion slots based on restaurant traffic.", List.of()),
                systemPrompt,
                "");
        aiRequestsCounter.increment();

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildPromotionFallback(language, openTime, closeTime, hourlyTraffic, dayTraffic, now);
        }

        AiPromotionSuggestionsResponse parsed = tryParsePromotionResponse(language, openTime, closeTime, rawReply, now);
        return parsed != null
                ? parsed
                : buildPromotionFallback(language, openTime, closeTime, hourlyTraffic, dayTraffic, now);
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

        for (CoOccurrencePair pair : validPairs) {
            if (proposals.size() >= MAX_SUGGESTIONS) {
                break;
            }
            MenuItem first = availableItems.get(pair.firstId());
            MenuItem second = availableItems.get(pair.secondId());
            if (first == null || second == null || Objects.equals(first.getId(), second.getId())) {
                continue;
            }
            Set<Long> signature = Set.of(first.getId(), second.getId());
            if (!usedSignatures.add(signature)) {
                continue;
            }
            String rationale = isEnglish(language)
                    ? "Ordered together %d times in the last %d days."
                            .formatted(pair.pairCount(), PAIRING_LOOKBACK_DAYS)
                    : "Khách thường gọi kèm nhau (%d đơn chung trong %d ngày qua)."
                            .formatted(pair.pairCount(), PAIRING_LOOKBACK_DAYS);
            proposals.add(buildFallbackComboProposal(List.of(first, second), rationale, language));
        }

        // Supplement with distinct popular/cross-category items if co-occurrence history is sparse
        List<MenuItem> prioritizedPool = buildPrioritizedItemPool(availableItems, topSellers);
        for (int i = 0; i < prioritizedPool.size() && proposals.size() < 2; i++) {
            for (int j = i + 1; j < prioritizedPool.size() && proposals.size() < 2; j++) {
                MenuItem a = prioritizedPool.get(i);
                MenuItem b = prioritizedPool.get(j);
                Set<Long> signature = Set.of(a.getId(), b.getId());
                if (!usedSignatures.add(signature)) {
                    continue;
                }
                String rationale = isEnglish(language)
                        ? "Combines popular menu choices to lift average order value."
                        : "Kết hợp các món dễ bán giúp tăng giá trị đơn trung bình.";
                proposals.add(buildFallbackComboProposal(List.of(a, b), rationale, language));
            }
        }

        return new AiComboSuggestionsResponse(
                language, buildComboFallbackSummary(language, proposals.size()), proposals, generatedAt, false);
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

    private AiPromotionSuggestionsResponse tryParsePromotionResponse(
            String language, LocalTime openTime, LocalTime closeTime, String rawReply, LocalDateTime generatedAt) {
        String extracted = AiJsonExtractor.extractJsonObject(rawReply);
        if (extracted == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extracted);
            String summary = readText(root.path("summary"), "");
            JsonNode promosNode = root.path("promotions");
            if (!promosNode.isArray()) {
                return null;
            }

            List<PromotionProposal> proposals = new ArrayList<>();
            for (JsonNode promoNode : promosNode) {
                PromotionProposal validated = parseSinglePromotion(promoNode, openTime, closeTime, language);
                if (validated != null) {
                    proposals.add(validated);
                }
                if (proposals.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            }
            if (proposals.isEmpty()) {
                return null;
            }
            String finalSummary =
                    summary.isBlank() ? buildPromotionFallbackSummary(language, proposals.size()) : summary;
            return new AiPromotionSuggestionsResponse(language, finalSummary, proposals, generatedAt, true);
        } catch (Exception ex) {
            log.debug("Failed to parse AI promotion JSON payload: {}", ex.getMessage());
            return null;
        }
    }

    private PromotionProposal parseSinglePromotion(
            JsonNode promoNode, LocalTime openTime, LocalTime closeTime, String language) {
        LocalTime startTime = parseTimeSafe(readText(promoNode.path("startTime"), ""));
        LocalTime endTime = parseTimeSafe(readText(promoNode.path("endTime"), ""));
        if (startTime == null || endTime == null) {
            return null;
        }
        if (startTime.isBefore(openTime)) {
            startTime = openTime;
        }
        if (endTime.isAfter(closeTime)) {
            endTime = closeTime;
        }
        if (!startTime.isBefore(endTime)) {
            return null;
        }

        double rawDiscount = promoNode.path("discountPercent").asDouble(15.0);
        double discountPercent = Math.round(Math.max(MIN_PROMO_DISCOUNT, Math.min(MAX_PROMO_DISCOUNT, rawDiscount)));

        List<String> validDays = parseValidDays(promoNode.path("daysOfWeek"));
        String name = truncate(
                readText(
                        promoNode.path("name"),
                        isEnglish(language) ? "Off-Peak Happy Hour" : "Ưu đãi giờ vàng thấp điểm"),
                80);
        String rationale = truncate(
                readText(
                        promoNode.path("rationale"),
                        isEnglish(language)
                                ? "Targets quieter hours inside operating schedule."
                                : "Kích cầu khung giờ thưa khách trong giờ mở cửa."),
                240);
        String impact = truncate(
                readText(
                        promoNode.path("expectedImpact"),
                        isEnglish(language)
                                ? "Increase off-peak table turnover."
                                : "Tăng tỷ lệ lấp đầy bàn ngoài giờ cao điểm."),
                180);

        return new PromotionProposal(
                name,
                discountPercent,
                startTime.format(TIME_FORMATTER),
                endTime.format(TIME_FORMATTER),
                validDays,
                rationale,
                impact);
    }

    private AiPromotionSuggestionsResponse buildPromotionFallback(
            String language,
            LocalTime openTime,
            LocalTime closeTime,
            List<HourlyTrafficProjection> hourlyTraffic,
            List<DayOfWeekTrafficProjection> dayTraffic,
            LocalDateTime generatedAt) {
        int openHour = openTime.getHour();
        int closeHour = closeTime.getMinute() > 0 ? closeTime.getHour() + 1 : closeTime.getHour();
        closeHour = Math.min(23, Math.max(openHour + 2, closeHour));

        Map<Integer, Long> ordersByHour = new HashMap<>();
        for (int h = openHour; h < closeHour; h++) {
            ordersByHour.put(h, 0L);
        }
        for (HourlyTrafficProjection row : hourlyTraffic) {
            if (row.getHourOfDay() != null && ordersByHour.containsKey(row.getHourOfDay())) {
                ordersByHour.merge(
                        row.getHourOfDay(), row.getOrderCount() != null ? row.getOrderCount() : 0L, Long::sum);
            }
        }

        // Pick quietest 2-hour continuous window (preferring afternoon hours >= 13 when tied on zero data)
        int windowSpan = (closeHour - openHour) >= 3 ? 3 : 2;
        int bestStartHour = Math.max(openHour, Math.min(14, closeHour - windowSpan));
        long minWindowOrders = Long.MAX_VALUE;
        for (int start = openHour; start <= closeHour - windowSpan; start++) {
            long sum = 0L;
            for (int offset = 0; offset < windowSpan; offset++) {
                sum += ordersByHour.getOrDefault(start + offset, 0L);
            }
            if (sum < minWindowOrders || (sum == minWindowOrders && start == 14)) {
                minWindowOrders = sum;
                bestStartHour = start;
            }
        }
        LocalTime slotStart = LocalTime.of(bestStartHour, 0);
        if (slotStart.isBefore(openTime)) {
            slotStart = openTime;
        }
        LocalTime slotEnd = LocalTime.of(Math.min(23, bestStartHour + windowSpan), 0);
        if (slotEnd.isAfter(closeTime)) {
            slotEnd = closeTime;
        }

        Map<DayOfWeek, Long> ordersByDay = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            ordersByDay.put(day, 0L);
        }
        for (DayOfWeekTrafficProjection row : dayTraffic) {
            if (row.getIsoDayOfWeek() != null && row.getIsoDayOfWeek() >= 1 && row.getIsoDayOfWeek() <= 7) {
                DayOfWeek day = DayOfWeek.of(row.getIsoDayOfWeek());
                ordersByDay.merge(day, row.getOrderCount() != null ? row.getOrderCount() : 0L, Long::sum);
            }
        }
        List<String> slowestDays = ordersByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(e -> e.getKey().name())
                .toList();

        List<PromotionProposal> proposals = new ArrayList<>();
        if (slotStart.isBefore(slotEnd)) {
            proposals.add(new PromotionProposal(
                    isEnglish(language) ? "Happy Hour Off-Peak Boost" : "Giờ Vàng Thấp Điểm",
                    15.0,
                    slotStart.format(TIME_FORMATTER),
                    slotEnd.format(TIME_FORMATTER),
                    List.of(),
                    isEnglish(language)
                            ? "Window %s-%s has the lowest order volume in the past %d days."
                                    .formatted(
                                            slotStart.format(TIME_FORMATTER),
                                            slotEnd.format(TIME_FORMATTER),
                                            TRAFFIC_LOOKBACK_DAYS)
                            : "Khung %s-%s ghi nhận lượng đơn thấp nhất trong %d ngày qua."
                                    .formatted(
                                            slotStart.format(TIME_FORMATTER),
                                            slotEnd.format(TIME_FORMATTER),
                                            TRAFFIC_LOOKBACK_DAYS),
                    isEnglish(language)
                            ? "Pulls price-sensitive guests away from peak congestion."
                            : "Điều tiết khách sang giờ thấp điểm, giảm tải khung giờ đông."));
        }

        LocalTime weekdayStart = openTime;
        LocalTime weekdayEnd = closeTime.isAfter(openTime.plusHours(4)) ? openTime.plusHours(3) : closeTime;
        if (weekdayStart.isBefore(weekdayEnd)) {
            proposals.add(new PromotionProposal(
                    isEnglish(language) ? "Mid-Week Opening Saver" : "Ưu Đãi Đầu Tuần Dịu Nhẹ",
                    10.0,
                    weekdayStart.format(TIME_FORMATTER),
                    weekdayEnd.format(TIME_FORMATTER),
                    slowestDays,
                    isEnglish(language)
                            ? "Targets slower weekdays (%s) during opening hours."
                                    .formatted(String.join(", ", slowestDays))
                            : "Kích cầu các ngày có lượng đơn thấp hơn trong tuần (%s)."
                                    .formatted(String.join(", ", slowestDays)),
                    isEnglish(language)
                            ? "Stabilizes kitchen utilization early in the week."
                            : "Ổn định công suất bàn và doanh thu các ngày đầu/giữa tuần."));
        }

        return new AiPromotionSuggestionsResponse(
                language, buildPromotionFallbackSummary(language, proposals.size()), proposals, generatedAt, false);
    }

    private List<CoOccurrencePair> loadValidCoOccurrencePairs(LocalDateTime now, Map<Long, MenuItem> availableItems) {
        List<Object[]> rawPairs = orderItemRepository.findFrequentlyPairedMenuItems(
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

    private List<MenuItem> buildPrioritizedItemPool(
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

    private String buildComboContext(
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

    private String buildTrafficContext(
            LocalTime openTime,
            LocalTime closeTime,
            List<HourlyTrafficProjection> hourlyTraffic,
            List<DayOfWeekTrafficProjection> dayTraffic,
            List<Promotion> existingPromotions) {
        StringBuilder sb = new StringBuilder();
        sb.append("OPERATING_HOURS: ")
                .append(openTime.format(TIME_FORMATTER))
                .append(" - ")
                .append(closeTime.format(TIME_FORMATTER))
                .append('\n');

        sb.append("HOURLY_TRAFFIC_30D:\n");
        if (hourlyTraffic.isEmpty()) {
            sb.append("- NO_ORDERS_RECORDED\n");
        } else {
            hourlyTraffic.stream()
                    .sorted(Comparator.comparingInt(h -> h.getHourOfDay() != null ? h.getHourOfDay() : 0))
                    .forEach(h -> sb.append("- hour=")
                            .append(h.getHourOfDay())
                            .append(":00 | orders=")
                            .append(h.getOrderCount())
                            .append(" | revenue=")
                            .append(safeCurrency(h.getRevenue()))
                            .append('\n'));
        }

        sb.append("WEEKDAY_TRAFFIC_30D:\n");
        if (dayTraffic.isEmpty()) {
            sb.append("- NO_ORDERS_RECORDED\n");
        } else {
            dayTraffic.stream()
                    .sorted(Comparator.comparingInt(d -> d.getIsoDayOfWeek() != null ? d.getIsoDayOfWeek() : 1))
                    .forEach(d -> sb.append("- ")
                            .append(DayOfWeek.of(Math.max(1, Math.min(7, d.getIsoDayOfWeek())))
                                    .name())
                            .append(" | orders=")
                            .append(d.getOrderCount())
                            .append('\n'));
        }

        sb.append("EXISTING_PROMOTIONS:\n");
        if (existingPromotions.isEmpty()) {
            sb.append("- NONE\n");
        } else {
            existingPromotions.forEach(p -> sb.append("- ")
                    .append(p.getName())
                    .append(" | ")
                    .append(p.getDiscountPercent())
                    .append('%')
                    .append(" | ")
                    .append(p.getStartTime())
                    .append('-')
                    .append(p.getEndTime())
                    .append(" | days=")
                    .append(p.getDaysOfWeek())
                    .append(" | active=")
                    .append(p.getActive())
                    .append('\n'));
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

    private static LocalTime resolveOpeningTime(PublicSettingsResponse settings) {
        return settings != null && settings.openingTime() != null ? settings.openingTime() : DEFAULT_OPENING_TIME;
    }

    private static LocalTime resolveClosingTime(PublicSettingsResponse settings, LocalTime openTime) {
        if (settings != null
                && settings.closingTime() != null
                && settings.closingTime().isAfter(openTime.plusHours(1))) {
            return settings.closingTime();
        }
        return DEFAULT_CLOSING_TIME.isAfter(openTime.plusHours(1)) ? DEFAULT_CLOSING_TIME : openTime.plusHours(8);
    }

    private static LocalTime parseTimeSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() == 4 && trimmed.charAt(1) == ':') {
            trimmed = "0" + trimmed;
        }
        try {
            return LocalTime.parse(trimmed, TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static List<String> parseValidDays(JsonNode daysNode) {
        if (daysNode == null || !daysNode.isArray()) {
            return List.of();
        }
        Set<String> valid = new LinkedHashSet<>();
        for (JsonNode child : daysNode) {
            String raw = child.asString("").trim().toUpperCase(Locale.ROOT);
            if (raw.isBlank()) {
                continue;
            }
            try {
                valid.add(DayOfWeek.valueOf(raw).name());
            } catch (IllegalArgumentException ignored) {
                // Reject invalid weekday names from LLM
            }
        }
        return List.copyOf(valid);
    }

    private static String buildComboFallbackSummary(String language, int count) {
        if (isEnglish(language)) {
            return "Proposed %d curated combos validated against active menu prices and co-occurrence patterns."
                    .formatted(count);
        }
        return "Đề xuất %d Combo tối ưu dựa trên món đang bán và thói quen gọi kèm của khách.".formatted(count);
    }

    private static String buildPromotionFallbackSummary(String language, int count) {
        if (isEnglish(language)) {
            return "Recommended %d off-peak promotion slots aligned with operating hours and quiet traffic."
                    .formatted(count);
        }
        return "Đề xuất %d khung giờ khuyến mãi thấp điểm phù hợp với giờ mở cửa của quán.".formatted(count);
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
