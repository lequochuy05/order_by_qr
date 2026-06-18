package com.qros.modules.ai.service;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.response.AiChatResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class AiAssistantService {

    private final MenuContextProvider menuContextProvider;
    private final AiChatGateway aiChatGateway;
    private final Counter aiRequestsCounter;

    public AiAssistantService(
            MenuContextProvider menuContextProvider, AiChatGateway aiChatGateway, MeterRegistry meterRegistry) {
        this.menuContextProvider = menuContextProvider;
        this.aiChatGateway = aiChatGateway;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .description("Total number of AI chat requests processed")
                .register(meterRegistry);
    }

    public AiChatResponse chat(AiChatRequest request) {
        String menuContext = menuContextProvider.buildCurrentMenuContext();
        String reply = aiChatGateway.chat(request, menuContext);

        aiRequestsCounter.increment();

        return new AiChatResponse(reply);
    }
}
