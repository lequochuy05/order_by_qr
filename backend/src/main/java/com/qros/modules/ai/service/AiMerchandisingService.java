package com.qros.modules.ai.service;

import com.qros.modules.ai.dto.request.AiMerchandisingRequest;
import com.qros.modules.ai.dto.response.AiComboSuggestionsResponse;
import com.qros.modules.ai.dto.response.AiPromotionSuggestionsResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.service.AnalyticsLookupService;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.order.service.OrderLookupService;
import com.qros.modules.promotion.service.PromotionLookupService;
import com.qros.modules.settings.service.SystemSettingsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Facade delegating AI merchandising requests to dedicated combo and off-peak promotion engines.
 */
@Service
public class AiMerchandisingService {

    private final AiComboSuggestionService comboSuggestionService;
    private final AiPromotionSuggestionService promotionSuggestionService;
    private final Counter aiRequestsCounter;

    @Autowired
    public AiMerchandisingService(
            AiComboSuggestionService comboSuggestionService,
            AiPromotionSuggestionService promotionSuggestionService,
            MeterRegistry meterRegistry) {
        this.comboSuggestionService = comboSuggestionService;
        this.promotionSuggestionService = promotionSuggestionService;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "merchandising")
                .description("AI combo and promotion merchandising suggestion requests")
                .register(meterRegistry);
    }

    public AiMerchandisingService(
            MenuContextProvider menuContextProvider,
            MenuCatalogLookupService menuCatalogLookupService,
            OrderLookupService orderLookupService,
            AnalyticsLookupService analyticsLookupService,
            PromotionLookupService promotionLookupService,
            SystemSettingsService systemSettingsService,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this(
                new AiComboSuggestionService(
                        menuContextProvider,
                        menuCatalogLookupService,
                        orderLookupService,
                        analyticsLookupService,
                        aiChatGateway,
                        promptBuilder,
                        objectMapper),
                new AiPromotionSuggestionService(
                        analyticsLookupService,
                        promotionLookupService,
                        systemSettingsService,
                        aiChatGateway,
                        promptBuilder,
                        objectMapper),
                meterRegistry);
    }

    public AiComboSuggestionsResponse suggestCombos(AiMerchandisingRequest request) {
        return comboSuggestionService.suggestCombos(request, aiRequestsCounter::increment);
    }

    public AiPromotionSuggestionsResponse suggestPromotions(AiMerchandisingRequest request) {
        return promotionSuggestionService.suggestPromotions(request, aiRequestsCounter::increment);
    }
}
