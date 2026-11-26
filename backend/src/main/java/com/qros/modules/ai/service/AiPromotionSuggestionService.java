package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;
import static com.qros.modules.ai.support.AiJsonExtractor.normalizeLanguage;
import static com.qros.modules.ai.support.AiJsonExtractor.readText;
import static com.qros.modules.ai.support.AiJsonExtractor.truncate;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiMerchandisingRequest;
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse;
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse.PromotionProposal;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.dto.projection.DayOfWeekTrafficProjection;
import com.qros.modules.analytics.dto.projection.HourlyTrafficProjection;
import com.qros.modules.analytics.service.AnalyticsLookupService;
import com.qros.modules.promotion.model.Promotion;
import com.qros.modules.promotion.service.PromotionLookupService;
import com.qros.modules.settings.dto.response.PublicSettingsResponse;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.shared.time.AppTime;
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
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Generates off-peak promotion slot recommendations grounded in 30-day hourly/weekday order
 * traffic and restaurant operating hours, falling back deterministically when AI is unavailable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromotionSuggestionService {

    private static final int TRAFFIC_LOOKBACK_DAYS = 30;
    private static final int MAX_SUGGESTIONS = 3;
    private static final double MIN_PROMO_DISCOUNT = 5.0;
    private static final double MAX_PROMO_DISCOUNT = 40.0;
    private static final LocalTime DEFAULT_OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSING_TIME = LocalTime.of(22, 0);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AnalyticsLookupService analyticsLookupService;
    private final PromotionLookupService promotionLookupService;
    private final SystemSettingsService systemSettingsService;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public AiPromotionSuggestionsResponse suggestPromotions(AiMerchandisingRequest request, Runnable onAiCall) {
        String language = normalizeLanguage(request != null ? request.language() : null);
        LocalDateTime now = AppTime.now();
        LocalDate today = now.toLocalDate();

        List<HourlyTrafficProjection> hourlyTraffic = analyticsLookupService.hourlyOrderTraffic(
                today.minusDays(TRAFFIC_LOOKBACK_DAYS - 1L), today.plusDays(1));
        List<DayOfWeekTrafficProjection> dayTraffic = analyticsLookupService.dayOfWeekOrderTraffic(
                today.minusDays(TRAFFIC_LOOKBACK_DAYS - 1L), today.plusDays(1));
        PublicSettingsResponse settings = systemSettingsService.getPublicSettings();
        List<Promotion> existingPromotions = promotionLookupService.findAllWithDays();

        LocalTime openTime = resolveOpeningTime(settings);
        LocalTime closeTime = resolveClosingTime(settings, openTime);

        String context = buildTrafficContext(openTime, closeTime, hourlyTraffic, dayTraffic, existingPromotions);
        String systemPrompt = promptBuilder.buildPromotionSuggestionPrompt(context, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Suggest off-peak promotion slots based on restaurant traffic.", List.of()),
                systemPrompt,
                "");
        if (onAiCall != null) {
            onAiCall.run();
        }

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildPromotionFallback(language, openTime, closeTime, hourlyTraffic, dayTraffic, now);
        }

        AiPromotionSuggestionsResponse parsed = tryParsePromotionResponse(language, openTime, closeTime, rawReply, now);
        return parsed != null
                ? parsed
                : buildPromotionFallback(language, openTime, closeTime, hourlyTraffic, dayTraffic, now);
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
        TimeSlot quietSlot = findQuietestTimeSlot(openTime, closeTime, hourlyTraffic);
        List<String> slowestDays = findSlowestWeekdays(dayTraffic);

        List<PromotionProposal> proposals = new ArrayList<>();
        if (quietSlot.start().isBefore(quietSlot.end())) {
            String startFmt = quietSlot.start().format(TIME_FORMATTER);
            String endFmt = quietSlot.end().format(TIME_FORMATTER);
            proposals.add(new PromotionProposal(
                    isEnglish(language) ? "Happy Hour Off-Peak Boost" : "Giờ Vàng Thấp Điểm",
                    15.0,
                    startFmt,
                    endFmt,
                    List.of(),
                    isEnglish(language)
                            ? "Window %s-%s has the lowest order volume in the past %d days."
                                    .formatted(startFmt, endFmt, TRAFFIC_LOOKBACK_DAYS)
                            : "Khung %s-%s ghi nhận lượng đơn thấp nhất trong %d ngày qua."
                                    .formatted(startFmt, endFmt, TRAFFIC_LOOKBACK_DAYS),
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

    private static TimeSlot findQuietestTimeSlot(
            LocalTime openTime, LocalTime closeTime, List<HourlyTrafficProjection> hourlyTraffic) {
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
        return new TimeSlot(slotStart, slotEnd);
    }

    private static List<String> findSlowestWeekdays(List<DayOfWeekTrafficProjection> dayTraffic) {
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
        return ordersByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(e -> e.getKey().name())
                .toList();
    }

    private static String buildTrafficContext(
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

    private static String buildPromotionFallbackSummary(String language, int count) {
        if (isEnglish(language)) {
            return "Recommended %d off-peak promotion slots aligned with operating hours and quiet traffic."
                    .formatted(count);
        }
        return "Đề xuất %d khung giờ khuyến mãi thấp điểm phù hợp với giờ mở cửa của quán.".formatted(count);
    }

    private static BigDecimal safeCurrency(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private record TimeSlot(LocalTime start, LocalTime end) {}
}
