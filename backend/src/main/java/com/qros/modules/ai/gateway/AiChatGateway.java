package com.qros.modules.ai.gateway;

import com.qros.modules.ai.dto.request.AiChatRequest;

public interface AiChatGateway {

    String chat(AiChatRequest request, String menuContext);

    String chat(AiChatRequest request, String systemPrompt, String menuContext);
}
