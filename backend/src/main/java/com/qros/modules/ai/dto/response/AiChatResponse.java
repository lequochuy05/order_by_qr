package com.qros.modules.ai.dto.response;

import java.util.List;

public record AiChatResponse(String reply, List<AiSuggestedItemResponse> suggestions) {

    public AiChatResponse {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    public AiChatResponse(String reply) {
        this(reply, List.of());
    }
}
