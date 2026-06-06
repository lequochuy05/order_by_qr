package com.qros.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * AiChatRequest - DTO for customer AI assistant chat messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    /**
     * The customer's current message.
     */
    @NotBlank(message = "Message cannot be empty")
    private String message;

    /**
     * Conversation history for maintaining session context.
     * Each entry contains a role ("user" or "assistant") and content.
     */
    private List<ChatMessage> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
