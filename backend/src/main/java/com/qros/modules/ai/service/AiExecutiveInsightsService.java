package com.qros.modules.ai.service;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiExecutiveSummaryRequest;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse.AnomalyAlert;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse.MetricsSnapshot;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse.TopDishMetric;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.ExecutiveInsightsContextProvider.ExecutiveInsightsData;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiExecutiveInsightsService {

    private static final int DEFAULT_RANGE_DAYS = 7;
    private static final long MAX_RANGE_DAYS = 90L;
    private static final int MAX_LIST_ITEMS = 3;
    private static final String FALLBACK_PREFIX = "Xin lỗi";
    private static final String CONFIG_PREFIX = "Tính năng trợ lý";

    private final ExecutiveInsightsContextProvider contextProvider;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiExecutiveInsightsService(
            ExecutiveInsightsContextProvider contextProvider,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.contextProvider = contextProvider;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "executive-insights")
                .description("AI executive shift and operations summary requests")
                .register(meterRegistry);
    }

    public AiExecutiveSummaryResponse generateSummary(AiExecutiveSummaryRequest request) {
        LocalDateTime now = AppTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate to = request != null && request.to() != null ? request.to() : today;
        LocalDate from =
                request != null && request.from() != null ? request.from() : to.minusDays(DEFAULT_RANGE_DAYS - 1L);

        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "Start date must not be after end date");
        }
        long spanDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (spanDays > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "Date range must not exceed 90 days");
        }

        String language = normalizeLanguage(request != null ? request.language() : null);
        ExecutiveInsightsData data = contextProvider.loadInsights(from, to);

        String systemPrompt = promptBuilder.buildExecutiveSummaryPrompt(data.formattedContext(), language);
        AiChatRequest gatewayRequest = new AiChatRequest(
                "Analyze operational and revenue metrics for %s to %s.".formatted(from, to), List.of());
        String rawReply = aiChatGateway.chat(gatewayRequest, systemPrompt, "");
        aiRequestsCounter.increment();

        if (!isUsableModelOutput(rawReply)) {
            return buildDeterministicFallback(from, to, language, data.metrics(), now);
        }

        AiExecutiveSummaryResponse parsed = tryParseAiResponse(from, to, language, data.metrics(), rawReply, now);
        return parsed != null ? parsed : buildDeterministicFallback(from, to, language, data.metrics(), now);
    }

    private AiExecutiveSummaryResponse tryParseAiResponse(
            LocalDate from,
            LocalDate to,
            String language,
            MetricsSnapshot metrics,
            String rawReply,
            LocalDateTime generatedAt) {
        String json = AiJsonExtractor.extractJsonObject(rawReply);
        if (json == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return null;
            }
            String headline = root.path("headline").asString("").trim();
            List<String> highlights = readStringList(root.path("highlights"));
            List<AnomalyAlert> anomalies = readAnomalies(root.path("anomalies"));
            List<String> recommendations = readStringList(root.path("recommendations"));

            if (headline.isBlank() || recommendations.isEmpty()) {
                return null;
            }

            return new AiExecutiveSummaryResponse(
                    from, to, language, headline, highlights, anomalies, recommendations, metrics, generatedAt, true);
        } catch (Exception ex) {
            log.warn(
                    "Failed to parse AI executive insights response, using deterministic fallback: {}",
                    ex.getMessage());
            return null;
        }
    }

    private AiExecutiveSummaryResponse buildDeterministicFallback(
            LocalDate from, LocalDate to, String language, MetricsSnapshot metrics, LocalDateTime generatedAt) {
        boolean isEn = "en".equals(language);
        String headline = buildFallbackHeadline(from, to, isEn, metrics);
        List<String> highlights = buildFallbackHighlights(isEn, metrics);
        List<AnomalyAlert> anomalies = buildFallbackAnomalies(isEn, metrics);
        List<String> recommendations = buildFallbackRecommendations(isEn, metrics);

        return new AiExecutiveSummaryResponse(
                from, to, language, headline, highlights, anomalies, recommendations, metrics, generatedAt, false);
    }

    private String buildFallbackHeadline(LocalDate from, LocalDate to, boolean isEn, MetricsSnapshot metrics) {
        String revenueText = formatMoney(metrics.revenue());
        if (isEn) {
            return "From %s to %s, the restaurant recorded %d completed orders with %s in revenue."
                    .formatted(from, to, metrics.completedOrders(), revenueText);
        }
        return "Từ %s đến %s, quán ghi nhận %d đơn hoàn tất với doanh thu %s."
                .formatted(from, to, metrics.completedOrders(), revenueText);
    }

    private List<String> buildFallbackHighlights(boolean isEn, MetricsSnapshot metrics) {
        List<String> highlights = new ArrayList<>();
        if (metrics.revenueChangePercent() != null) {
            String sign = metrics.revenueChangePercent() >= 0 ? "+" : "";
            highlights.add(
                    isEn
                            ? "Revenue changed %s%.1f%% compared to the previous period (%s)."
                                    .formatted(
                                            sign,
                                            metrics.revenueChangePercent(),
                                            formatMoney(metrics.previousRevenue()))
                            : "Doanh thu thay đổi %s%.1f%% so với kỳ liền trước (%s)."
                                    .formatted(
                                            sign,
                                            metrics.revenueChangePercent(),
                                            formatMoney(metrics.previousRevenue())));
        } else {
            highlights.add(
                    isEn
                            ? "Average order value reached %s across %d items sold."
                                    .formatted(formatMoney(metrics.averageOrderValue()), metrics.totalItemsSold())
                            : "Giá trị trung bình mỗi đơn đạt %s với %d phần món đã bán."
                                    .formatted(formatMoney(metrics.averageOrderValue()), metrics.totalItemsSold()));
        }

        if (!metrics.topDishes().isEmpty()) {
            TopDishMetric leader = metrics.topDishes().get(0);
            highlights.add(
                    isEn
                            ? "Top driver: %s (%d servings, %s)."
                                    .formatted(leader.name(), leader.quantitySold(), formatMoney(leader.revenue()))
                            : "Món dẫn đầu: %s (%d phần, %s)."
                                    .formatted(leader.name(), leader.quantitySold(), formatMoney(leader.revenue())));
        }

        if (highlights.size() < MAX_LIST_ITEMS) {
            highlights.add(
                    isEn
                            ? "Table utilization right now: %d/%d tables active."
                                    .formatted(metrics.occupiedTables(), metrics.totalTables())
                            : "Công suất bàn hiện tại: %d/%d bàn đang phục vụ."
                                    .formatted(metrics.occupiedTables(), metrics.totalTables()));
        }
        return highlights.stream().limit(MAX_LIST_ITEMS).toList();
    }

    private List<AnomalyAlert> buildFallbackAnomalies(boolean isEn, MetricsSnapshot metrics) {
        List<AnomalyAlert> anomalies = new ArrayList<>();
        if (metrics.overdueKitchenItems() > 0) {
            anomalies.add(new AnomalyAlert(
                    "HIGH",
                    isEn ? "Kitchen SLA Bottleneck" : "Cảnh báo bếp chậm món",
                    isEn
                            ? "%d active item(s) waiting over 20 minutes (longest wait: %dm)."
                                    .formatted(metrics.overdueKitchenItems(), metrics.maxKitchenWaitMinutes())
                            : "Có %d món đang chờ quá 20 phút (lâu nhất %d phút)."
                                    .formatted(metrics.overdueKitchenItems(), metrics.maxKitchenWaitMinutes())));
        }
        if (metrics.outOfStockItems() > 0 || metrics.lowStockItems() > 0) {
            String severity = metrics.outOfStockItems() > 0 ? "HIGH" : "MEDIUM";
            String detailNames =
                    metrics.lowStockItemNames().isEmpty() ? "" : ": " + String.join(", ", metrics.lowStockItemNames());
            anomalies.add(new AnomalyAlert(
                    severity,
                    isEn ? "Inventory Replenishment Alert" : "Cảnh báo tồn kho nguyên liệu",
                    isEn
                            ? "%d low-stock item(s) (%d out of stock)%s."
                                    .formatted(metrics.lowStockItems(), metrics.outOfStockItems(), detailNames)
                            : "%d nguyên liệu chạm ngưỡng thấp (%d đã hết hàng)%s."
                                    .formatted(metrics.lowStockItems(), metrics.outOfStockItems(), detailNames)));
        }
        if (metrics.overdueServiceRequests() > 0 || metrics.cancelledOrders() >= 3) {
            anomalies.add(new AnomalyAlert(
                    metrics.overdueServiceRequests() > 0 ? "HIGH" : "MEDIUM",
                    isEn ? "Service & Cancellation Watch" : "Theo dõi phục vụ & đơn hủy",
                    isEn
                            ? "%d service request(s) exceeded 5m SLA; %d cancelled order(s) recorded."
                                    .formatted(metrics.overdueServiceRequests(), metrics.cancelledOrders())
                            : "Ghi nhận %d yêu cầu gọi bàn quá 5 phút và %d đơn bị hủy."
                                    .formatted(metrics.overdueServiceRequests(), metrics.cancelledOrders())));
        }
        return anomalies.stream().limit(MAX_LIST_ITEMS).toList();
    }

    private List<String> buildFallbackRecommendations(boolean isEn, MetricsSnapshot metrics) {
        List<String> recommendations = new ArrayList<>();
        if (metrics.overdueKitchenItems() > 0) {
            recommendations.add(
                    isEn
                            ? "Prioritize batch cooking on the kitchen board for items waiting over 20 minutes."
                            : "Ưu tiên xử lý chế biến gom mẻ ngay cho nhóm món đang chờ quá 20 phút trên KDS.");
        }
        if (metrics.outOfStockItems() > 0 || metrics.lowStockItems() > 0) {
            recommendations.add(
                    isEn
                            ? "Restock flagged low inventory items or pause affected dishes before peak hours."
                            : "Lập phiếu nhập kho cho nhóm nguyên liệu sắp hết hoặc tạm ẩn món liên quan trước giờ cao điểm.");
        }
        if (metrics.overdueServiceRequests() > 0) {
            recommendations.add(
                    isEn
                            ? "Rebalance floor staff to clear open table service requests within the 5-minute SLA."
                            : "Điều phối thêm nhân viên sàn xử lý nhanh các yêu cầu tại bàn đang vượt ngưỡng 5 phút.");
        }
        if (recommendations.size() < 2) {
            recommendations.add(
                    isEn
                            ? "Promote top-selling items and value combos during upcoming shifts to lift average order value."
                            : "Đẩy mạnh gợi ý món bán chạy và combo ưu đãi trong khung giờ tới để tăng giá trị trung bình đơn.");
        }
        if (recommendations.size() < 2) {
            recommendations.add(
                    isEn
                            ? "Audit cancelled orders and table turnaround speed at the end of shift."
                            : "Rà soát lý do các đơn hủy và tốc độ quay vòng bàn vào cuối ca để giữ ổn định doanh thu.");
        }
        return recommendations.stream().limit(MAX_LIST_ITEMS).toList();
    }

    private List<String> readStringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            if (items.size() >= MAX_LIST_ITEMS) {
                break;
            }
            String text = node.asString("").trim();
            if (!text.isBlank()) {
                items.add(text);
            }
        }
        return items;
    }

    private List<AnomalyAlert> readAnomalies(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<AnomalyAlert> alerts = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            if (alerts.size() >= MAX_LIST_ITEMS) {
                break;
            }
            if (!node.isObject()) {
                continue;
            }
            String severity = normalizeSeverity(node.path("severity").asString("INFO"));
            String title = node.path("title").asString("").trim();
            String detail = node.path("detail").asString("").trim();
            if (!title.isBlank() && !detail.isBlank()) {
                alerts.add(new AnomalyAlert(severity, title, detail));
            }
        }
        return alerts;
    }

    private String normalizeSeverity(String raw) {
        if (raw == null) {
            return "INFO";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if ("HIGH".equals(upper) || "MEDIUM".equals(upper) || "INFO".equals(upper)) {
            return upper;
        }
        return "INFO";
    }

    private String normalizeLanguage(String language) {
        return "en".equalsIgnoreCase(language) ? "en" : "vi";
    }

    private boolean isUsableModelOutput(String reply) {
        return reply != null
                && !reply.isBlank()
                && !reply.startsWith(FALLBACK_PREFIX)
                && !reply.startsWith(CONFIG_PREFIX);
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return "%,.0fđ".formatted(safe).replace(",", ".");
    }
}
