package com.qros.modules.ai.service;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.response.AiChatResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class AiAssistantService {

    private final MenuContextProvider menuContextProvider;
    private final SystemSettingsContextProvider settingsContextProvider;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final Counter aiRequestsCounter;

    public AiAssistantService(
            MenuContextProvider menuContextProvider,
            SystemSettingsContextProvider settingsContextProvider,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            MeterRegistry meterRegistry) {
        this.menuContextProvider = menuContextProvider;
        this.settingsContextProvider = settingsContextProvider;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .description("Total number of AI chat requests processed")
                .register(meterRegistry);
    }

    public AiChatResponse chat(AiChatRequest request) {
        String menuContext = menuContextProvider.buildCurrentMenuContext();
        String settingsContext = settingsContextProvider.buildSettingsContext();
        String systemPrompt = promptBuilder.buildSystemPrompt(menuContext, settingsContext);
        String reply = aiChatGateway.chat(request, systemPrompt, menuContext);

        aiRequestsCounter.increment();

        return new AiChatResponse(reply);
    }
}
