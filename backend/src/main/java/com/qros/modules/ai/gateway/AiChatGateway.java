package com.qros.modules.ai.gateway;

import com.qros.modules.ai.dto.request.AiChatRequest;

public interface AiChatGateway {

    String chat(AiChatRequest request, String menuContext);

    String chat(AiChatRequest request, String systemPrompt, String menuContext);

    default String chatWithImage(String userMessage, String systemPrompt, byte[] imageBytes, String mimeType) {
        return chat(new AiChatRequest(userMessage, java.util.List.of()), systemPrompt, "");
    }
}
