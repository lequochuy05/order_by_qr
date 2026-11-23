package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;
import static com.qros.modules.ai.support.AiJsonExtractor.normalizeLanguage;
import static com.qros.modules.ai.support.AiJsonExtractor.readText;
import static com.qros.modules.ai.support.AiJsonExtractor.truncate;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiNoteTriageRequest;
import com.qros.modules.ai.dto.response.AiNoteTriageResponse;
import com.qros.modules.ai.dto.response.AiNoteTriageResponse.TriagedNoteAlert;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.ai.support.NoteSafetyAnalyzer;
import com.qros.modules.ai.support.NoteSafetyAnalyzer.NoteSafetyResult;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse.KitchenOrderItemResponse;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.repository.TableServiceRequestRepository;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiNoteTriageService {

    private static final String SOURCE_KITCHEN_ITEM = "KITCHEN_ITEM";
    private static final String SOURCE_SERVICE_REQUEST = "SERVICE_REQUEST";
    private static final int MAX_PROMPT_NOTES = 30;

    private final KitchenService kitchenService;
    private final TableServiceRequestRepository tableServiceRequestRepository;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiNoteTriageService(
            KitchenService kitchenService,
            TableServiceRequestRepository tableServiceRequestRepository,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.kitchenService = kitchenService;
        this.tableServiceRequestRepository = tableServiceRequestRepository;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "note-triage")
                .description("AI customer allergy and urgent note triage requests")
                .register(meterRegistry);
    }

    public AiNoteTriageResponse triage(AiNoteTriageRequest request) {
        String language = normalizeLanguage(request != null ? request.language() : null);
        LocalDateTime now = AppTime.now();

        List<CandidateNote> candidates = collectCandidates(now, language);
        if (candidates.isEmpty()) {
            String emptySummary = isEnglish(language)
                    ? "No active kitchen or service request notes currently need triage."
                    : "Hiện chưa có ghi chú bếp hoặc yêu cầu phục vụ nào cần phân loại.";
            return new AiNoteTriageResponse(language, emptySummary, 0, 0, List.of(), now, false);
        }

        List<CandidateNote> prioritizedCandidates = candidates.stream()
                .sorted(Comparator.<CandidateNote>comparingInt(c ->
                                NoteSafetyAnalyzer.rankRisk(c.deterministic().riskLevel()))
                        .reversed()
                        .thenComparing(Comparator.comparingLong(CandidateNote::waitMinutes)
                                .reversed())
                        .thenComparingLong(CandidateNote::sourceId))
                .limit(MAX_PROMPT_NOTES)
                .toList();

        String notesContext = buildNotesContext(prioritizedCandidates);
        String systemPrompt = promptBuilder.buildNoteTriagePrompt(notesContext, language);
        String rawReply = aiChatGateway.chat(
                new AiChatRequest("Triage active kitchen and table service notes for safety & priority.", List.of()),
                systemPrompt,
                "");
        aiRequestsCounter.increment();

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildDeterministicResponse(language, prioritizedCandidates, now);
        }

        AiNoteTriageResponse parsed = tryParseAiResponse(language, prioritizedCandidates, rawReply, now);
        return parsed != null ? parsed : buildDeterministicResponse(language, prioritizedCandidates, now);
    }

    private List<CandidateNote> collectCandidates(LocalDateTime now, String language) {
        List<CandidateNote> candidates = new ArrayList<>();

        List<KitchenOrderResponse> kitchenOrders = kitchenService.getKitchenOrders();
        if (kitchenOrders != null) {
            for (KitchenOrderResponse order : kitchenOrders) {
                if (order == null || order.orderItems() == null) {
                    continue;
                }
                String tableLabel = resolveOrderTableLabel(order, language);
                for (KitchenOrderItemResponse item : order.orderItems()) {
                    if (item == null
                            || item.id() == null
                            || (item.status() != OrderItemStatus.PENDING && item.status() != OrderItemStatus.COOKING)
                            || item.notes() == null
                            || item.notes().isBlank()) {
                        continue;
                    }
                    String itemName = resolveOrderItemName(item);
                    String rawNote = item.notes().trim();
                    long waitMinutes = computeWaitMinutes(item.createdAt(), now);
                    candidates.add(new CandidateNote(
                            SOURCE_KITCHEN_ITEM,
                            item.id(),
                            tableLabel,
                            itemName,
                            rawNote,
                            waitMinutes,
                            NoteSafetyAnalyzer.analyze(rawNote)));
                }
            }
        }

        List<TableServiceRequest> openRequests = tableServiceRequestRepository.findByStatusesOrderByCreatedAtAsc(
                List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.CLAIMED), null, null);
        if (openRequests != null) {
            for (TableServiceRequest req : openRequests) {
                if (req == null
                        || req.getId() == null
                        || req.getNote() == null
                        || req.getNote().isBlank()) {
                    continue;
                }
                String tableLabel = req.getTable() != null && req.getTable().getTableNumber() != null
                        ? (isEnglish(language) ? "Table " : "Bàn ")
                                + req.getTable().getTableNumber()
                        : "-";
                String rawNote = req.getNote().trim();
                long waitMinutes = computeWaitMinutes(req.getCreatedAt(), now);
                candidates.add(new CandidateNote(
                        SOURCE_SERVICE_REQUEST,
                        req.getId(),
                        tableLabel,
                        formatReasonLabel(req.getReason(), language),
                        rawNote,
                        waitMinutes,
                        NoteSafetyAnalyzer.analyze(rawNote)));
            }
        }

        return candidates;
    }

    private AiNoteTriageResponse tryParseAiResponse(
            String language, List<CandidateNote> candidates, String rawReply, LocalDateTime generatedAt) {
        String extracted = AiJsonExtractor.extractJsonObject(rawReply);
        if (extracted == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extracted);
            String summary = readText(root.path("summary"), "");
            JsonNode triagedNode = root.path("triaged");
            if (!triagedNode.isArray()) {
                return null;
            }

            Map<String, AiTriagedPatch> patchesByKey = new HashMap<>();
            for (JsonNode entry : triagedNode) {
                String sourceType = readText(entry.path("sourceType"), "").toUpperCase(Locale.ROOT);
                long sourceId = entry.path("sourceId").asLong(-1L);
                if (sourceId <= 0
                        || (!SOURCE_KITCHEN_ITEM.equals(sourceType) && !SOURCE_SERVICE_REQUEST.equals(sourceType))) {
                    continue;
                }
                String aiRisk = NoteSafetyAnalyzer.normalizeRisk(readText(entry.path("riskLevel"), ""));
                List<String> extraAllergens = readUppercaseTags(entry.path("extraAllergens"));
                String instruction = truncate(readText(entry.path("actionInstruction"), ""), 180);
                patchesByKey.put(sourceType + ":" + sourceId, new AiTriagedPatch(aiRisk, extraAllergens, instruction));
            }

            if (summary.isBlank() && patchesByKey.isEmpty()) {
                return null;
            }

            List<TriagedNoteAlert> mergedAlerts = candidates.stream()
                    .map(c -> mergeCandidateWithAi(c, patchesByKey.get(c.key()), language))
                    .sorted(alertComparator())
                    .toList();

            int criticalCount = countByRisk(mergedAlerts, NoteSafetyAnalyzer.RISK_CRITICAL);
            int highCount = countByRisk(mergedAlerts, NoteSafetyAnalyzer.RISK_HIGH);
            String finalSummary = summary.isBlank()
                    ? buildFallbackSummary(language, criticalCount, highCount, mergedAlerts.size())
                    : summary;

            return new AiNoteTriageResponse(
                    language, finalSummary, criticalCount, highCount, mergedAlerts, generatedAt, true);
        } catch (Exception ex) {
            log.debug("Failed to parse AI note triage JSON payload: {}", ex.getMessage());
            return null;
        }
    }

    private TriagedNoteAlert mergeCandidateWithAi(CandidateNote candidate, AiTriagedPatch patch, String language) {
        NoteSafetyResult det = candidate.deterministic();
        Set<String> mergedAllergens = new LinkedHashSet<>(det.allergens());
        if (patch != null && patch.extraAllergens() != null) {
            mergedAllergens.addAll(patch.extraAllergens());
        }

        String finalRisk = det.riskLevel();
        if (patch != null) {
            finalRisk = NoteSafetyAnalyzer.maxRisk(finalRisk, patch.riskLevel());
        }
        // Any identified allergen automatically escalates final risk to CRITICAL and can never be downgraded
        if (!mergedAllergens.isEmpty()) {
            finalRisk = NoteSafetyAnalyzer.RISK_CRITICAL;
        }

        String instruction = patch != null && !patch.actionInstruction().isBlank()
                ? patch.actionInstruction()
                : buildDeterministicInstruction(
                        finalRisk,
                        List.copyOf(mergedAllergens),
                        det.urgencyFlags(),
                        det.dietaryFlags(),
                        candidate.rawNote(),
                        language);

        return new TriagedNoteAlert(
                candidate.sourceType(),
                candidate.sourceId(),
                candidate.tableLabel(),
                candidate.itemName(),
                candidate.rawNote(),
                finalRisk,
                List.copyOf(mergedAllergens),
                det.urgencyFlags(),
                det.dietaryFlags(),
                instruction,
                candidate.waitMinutes());
    }

    private AiNoteTriageResponse buildDeterministicResponse(
            String language, List<CandidateNote> candidates, LocalDateTime generatedAt) {
        List<TriagedNoteAlert> alerts = candidates.stream()
                .map(c -> mergeCandidateWithAi(c, null, language))
                .sorted(alertComparator())
                .toList();
        int criticalCount = countByRisk(alerts, NoteSafetyAnalyzer.RISK_CRITICAL);
        int highCount = countByRisk(alerts, NoteSafetyAnalyzer.RISK_HIGH);

        return new AiNoteTriageResponse(
                language,
                buildFallbackSummary(language, criticalCount, highCount, alerts.size()),
                criticalCount,
                highCount,
                alerts,
                generatedAt,
                false);
    }

    private static Comparator<TriagedNoteAlert> alertComparator() {
        return Comparator.<TriagedNoteAlert>comparingInt(a -> NoteSafetyAnalyzer.rankRisk(a.riskLevel()))
                .reversed()
                .thenComparing(
                        Comparator.comparingLong(TriagedNoteAlert::waitMinutes).reversed())
                .thenComparingLong(TriagedNoteAlert::sourceId);
    }

    private static int countByRisk(List<TriagedNoteAlert> alerts, String riskLevel) {
        return (int)
                alerts.stream().filter(a -> riskLevel.equals(a.riskLevel())).count();
    }

    private String buildNotesContext(List<CandidateNote> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("ACTIVE_NOTES:\n");
        for (CandidateNote c : candidates) {
            NoteSafetyResult det = c.deterministic();
            sb.append("- sourceType=")
                    .append(c.sourceType())
                    .append(" | sourceId=")
                    .append(c.sourceId())
                    .append(" | table=")
                    .append(c.tableLabel())
                    .append(" | item=")
                    .append(c.itemName())
                    .append(" | waitMin=")
                    .append(c.waitMinutes())
                    .append(" | deterministicRisk=")
                    .append(det.riskLevel())
                    .append(" | detectedAllergens=")
                    .append(det.allergens())
                    .append(" | detectedUrgency=")
                    .append(det.urgencyFlags())
                    .append(" | detectedDietary=")
                    .append(det.dietaryFlags())
                    .append(" | rawNote=\"")
                    .append(c.rawNote().replace('"', '\''))
                    .append('"')
                    .append('\n');
        }
        return sb.toString();
    }

    private static String buildFallbackSummary(String language, int criticalCount, int highCount, int totalCount) {
        if (isEnglish(language)) {
            return "Triaged %d active note(s): %d allergy/critical alert(s) and %d high-priority request(s)."
                    .formatted(totalCount, criticalCount, highCount);
        }
        return "Đã phân loại %d ghi chú đang chờ: %d cảnh báo dị ứng/khẩn cấp và %d yêu cầu ưu tiên cao."
                .formatted(totalCount, criticalCount, highCount);
    }

    private static String buildDeterministicInstruction(
            String riskLevel,
            List<String> allergens,
            List<String> urgency,
            List<String> dietary,
            String rawNote,
            String language) {
        if (NoteSafetyAnalyzer.RISK_CRITICAL.equals(riskLevel)) {
            String allergenText = allergens.isEmpty() ? rawNote : String.join(", ", allergens);
            return isEnglish(language)
                    ? "Allergy protocol (%s): prepare with sanitized utensils and verify zero cross-contact."
                            .formatted(allergenText)
                    : "Cảnh báo dị ứng (%s): chế biến riêng bằng dụng cụ sạch, tránh nhiễm chéo tuyệt đối."
                            .formatted(allergenText);
        }
        if (NoteSafetyAnalyzer.RISK_HIGH.equals(riskLevel)) {
            return isEnglish(language)
                    ? "Prioritize immediately (%s): inspect note \"%s\" before firing next batch."
                            .formatted(String.join(", ", urgency), truncate(rawNote, 60))
                    : "Ưu tiên xử lý ngay (%s): kiểm tra ghi chú \"%s\" trước khi ra món."
                            .formatted(String.join(", ", urgency), truncate(rawNote, 60));
        }
        if (!dietary.isEmpty()) {
            return isEnglish(language)
                    ? "Follow customer dietary preference (%s).".formatted(String.join(", ", dietary))
                    : "Điều chỉnh gia vị/thành phần theo lưu ý (%s).".formatted(String.join(", ", dietary));
        }
        return isEnglish(language)
                ? "Acknowledge customer note: \"%s\".".formatted(truncate(rawNote, 80))
                : "Lưu ý phục vụ theo ghi chú khách: \"%s\".".formatted(truncate(rawNote, 80));
    }

    private static String resolveOrderTableLabel(KitchenOrderResponse order, String language) {
        if (order.orderType() == OrderType.TAKEAWAY
                || order.table() == null
                || order.table().tableNumber() == null) {
            return isEnglish(language) ? "Takeaway" : "Mang về";
        }
        return (isEnglish(language) ? "Table " : "Bàn ") + order.table().tableNumber();
    }

    private static String resolveOrderItemName(KitchenOrderItemResponse item) {
        if (item.menuItem() != null && item.menuItem().name() != null) {
            return item.menuItem().name();
        }
        if (item.combo() != null && item.combo().name() != null) {
            return item.combo().name();
        }
        return "-";
    }

    private static String formatReasonLabel(ServiceRequestReason reason, String language) {
        if (reason == null) {
            return isEnglish(language) ? "Service request" : "Yêu cầu phục vụ";
        }
        if (isEnglish(language)) {
            return switch (reason) {
                case WATER_ICE -> "Water / Ice";
                case UTENSILS_NAPKINS -> "Utensils / Napkins";
                case CLEAN_TABLE -> "Clean table";
                case ORDER_ASSISTANCE -> "Order assistance";
                case REQUEST_BILL -> "Request bill";
                case OTHER -> "Other request";
            };
        }
        return switch (reason) {
            case WATER_ICE -> "Xin thêm nước / đá";
            case UTENSILS_NAPKINS -> "Xin bát đũa / khăn giấy";
            case CLEAN_TABLE -> "Dọn bàn";
            case ORDER_ASSISTANCE -> "Hỗ trợ gọi món";
            case REQUEST_BILL -> "Yêu cầu thanh toán";
            case OTHER -> "Yêu cầu khác";
        };
    }

    private static long computeWaitMinutes(LocalDateTime createdAt, LocalDateTime now) {
        if (createdAt == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.MINUTES.between(createdAt, now));
    }

    private static List<String> readUppercaseTags(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        for (JsonNode child : node) {
            String value = child.asString("").trim().toUpperCase(Locale.ROOT);
            if (!value.isBlank() && !"NONE".equals(value) && !"UNKNOWN".equals(value)) {
                tags.add(value);
            }
        }
        return List.copyOf(tags);
    }

    private record CandidateNote(
            String sourceType,
            Long sourceId,
            String tableLabel,
            String itemName,
            String rawNote,
            long waitMinutes,
            NoteSafetyResult deterministic) {

        String key() {
            return sourceType + ":" + sourceId;
        }
    }

    private record AiTriagedPatch(String riskLevel, List<String> extraAllergens, String actionInstruction) {}
}
