package com.qros.modules.ai.service;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.response.AiChatResponse;
import com.qros.modules.ai.dto.response.AiSuggestedItemResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.MenuContextProvider.MenuCatalogSnapshot;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiAssistantService {

    private static final int MAX_SUGGESTIONS = 5;
    private static final int MAX_QUANTITY = 10;
    private static final int MAX_REASON_LENGTH = 120;
    private static final String TYPE_MENU_ITEM = "MENU_ITEM";
    private static final String TYPE_COMBO = "COMBO";
    private static final Pattern INTERNAL_ID_TAG_PATTERN =
            Pattern.compile("\\s*\\[(?:ITEM|COMBO):\\d+]\\s*", Pattern.CASE_INSENSITIVE);

    private final MenuContextProvider menuContextProvider;
    private final SystemSettingsContextProvider settingsContextProvider;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiAssistantService(
            MenuContextProvider menuContextProvider,
            SystemSettingsContextProvider settingsContextProvider,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.menuContextProvider = menuContextProvider;
        this.settingsContextProvider = settingsContextProvider;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "assistant")
                .description("Total number of AI chat requests processed")
                .register(meterRegistry);
    }

    public AiChatResponse chat(AiChatRequest request) {
        String menuContext = menuContextProvider.buildCurrentMenuContext();
        String settingsContext = settingsContextProvider.buildSettingsContext();
        String systemPrompt = promptBuilder.buildSystemPrompt(menuContext, settingsContext);
        String rawReply = aiChatGateway.chat(request, systemPrompt, menuContext);

        aiRequestsCounter.increment();

        return parseAndHydrateResponse(rawReply);
    }

    private AiChatResponse parseAndHydrateResponse(String rawReply) {
        if (rawReply == null || rawReply.isBlank()) {
            return new AiChatResponse("");
        }

        String json = AiJsonExtractor.extractJsonObject(rawReply);
        if (json == null) {
            return new AiChatResponse(sanitizeReplyText(rawReply));
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return new AiChatResponse(sanitizeReplyText(rawReply));
            }

            String reply = sanitizeReplyText(root.path("reply").asString(""));
            if (reply.isBlank()) {
                return new AiChatResponse(sanitizeReplyText(rawReply));
            }

            JsonNode suggestionsNode = root.path("suggestions");
            if (!suggestionsNode.isArray() || suggestionsNode.isEmpty()) {
                return new AiChatResponse(reply, List.of());
            }

            MenuCatalogSnapshot catalog = menuContextProvider.loadAvailableCatalog();
            List<AiSuggestedItemResponse> hydrated = hydrateSuggestions(suggestionsNode, catalog);
            return new AiChatResponse(reply, hydrated);
        } catch (Exception ex) {
            log.debug("AI assistant response fell back to plain text: {}", ex.getMessage());
            return new AiChatResponse(sanitizeReplyText(rawReply));
        }
    }

    private List<AiSuggestedItemResponse> hydrateSuggestions(JsonNode suggestionsNode, MenuCatalogSnapshot catalog) {
        List<AiSuggestedItemResponse> resolved = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();

        for (JsonNode node : suggestionsNode) {
            if (resolved.size() >= MAX_SUGGESTIONS) {
                break;
            }
            if (node == null || !node.isObject()) {
                continue;
            }

            String type = normalizeType(node.path("type").asString(""));
            long id = parsePositiveId(node.path("id"));
            if (type == null || id <= 0) {
                continue;
            }

            String dedupKey = type + ":" + id;
            if (!seenKeys.add(dedupKey)) {
                continue;
            }

            int quantity = clampQuantity(node.path("quantity").asInt(1));
            String reason = sanitizeReason(node.path("reason").asString(""));

            AiSuggestedItemResponse suggestedItem = TYPE_COMBO.equals(type)
                    ? toSuggestedCombo(catalog.combosById().get(id), quantity, reason)
                    : toSuggestedMenuItem(catalog.menuItemsById().get(id), quantity, reason);

            if (suggestedItem != null) {
                resolved.add(suggestedItem);
            }
        }

        return resolved;
    }

    private AiSuggestedItemResponse toSuggestedMenuItem(MenuItem item, int quantity, String reason) {
        if (item == null || item.getId() == null) {
            return null;
        }
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        boolean hasOptions = item.getItemOptions() != null
                && item.getItemOptions().stream().anyMatch(option -> option != null && !option.isDeleted());

        return new AiSuggestedItemResponse(
                TYPE_MENU_ITEM,
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getImg(),
                categoryName,
                quantity,
                reason,
                hasOptions,
                true);
    }

    private AiSuggestedItemResponse toSuggestedCombo(Combo combo, int quantity, String reason) {
        if (combo == null || combo.getId() == null) {
            return null;
        }
        String imageUrl = combo.getItems() == null
                ? null
                : combo.getItems().stream()
                        .filter(Objects::nonNull)
                        .filter(ci -> !ci.isDeleted() && ci.getMenuItem() != null)
                        .map(ComboItem::getMenuItem)
                        .map(MenuItem::getImg)
                        .filter(img -> img != null && !img.isBlank())
                        .findFirst()
                        .orElse(null);

        return new AiSuggestedItemResponse(
                TYPE_COMBO,
                combo.getId(),
                combo.getName(),
                combo.getPrice(),
                imageUrl,
                "Combo",
                quantity,
                reason,
                false,
                true);
    }

    private String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return TYPE_MENU_ITEM;
        }
        String upper = rawType.trim().toUpperCase(Locale.ROOT);
        if ("COMBO".equals(upper)) {
            return TYPE_COMBO;
        }
        if ("MENU_ITEM".equals(upper) || "ITEM".equals(upper) || "DISH".equals(upper)) {
            return TYPE_MENU_ITEM;
        }
        return null;
    }

    private long parsePositiveId(JsonNode idNode) {
        if (idNode == null || idNode.isNull() || idNode.isMissingNode()) {
            return -1L;
        }
        if (idNode.isIntegralNumber()) {
            return idNode.asLong(-1L);
        }
        if (idNode.isString()) {
            try {
                return Long.parseLong(idNode.asString().trim());
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }
        return -1L;
    }

    private int clampQuantity(int rawQty) {
        if (rawQty < 1) {
            return 1;
        }
        return Math.min(rawQty, MAX_QUANTITY);
    }

    private String sanitizeReason(String rawReason) {
        if (rawReason == null || rawReason.isBlank()) {
            return "";
        }
        String cleaned = INTERNAL_ID_TAG_PATTERN
                .matcher(rawReason.trim())
                .replaceAll(" ")
                .trim();
        if (cleaned.length() <= MAX_REASON_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_REASON_LENGTH - 1).trim() + "…";
    }

    private String sanitizeReplyText(String text) {
        if (text == null) {
            return "";
        }
        return INTERNAL_ID_TAG_PATTERN.matcher(text).replaceAll(" ").trim();
    }
}
