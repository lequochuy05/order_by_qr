package com.qros.modules.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiChatRequest(
        @NotBlank(message = "Message cannot be empty") @Size(max = 1000, message = "Message cannot exceed 1000 characters") String message,

        @Valid @Size(max = 20, message = "History cannot exceed 20 messages") List<ChatMessage> history) {

    public record ChatMessage(
            @NotBlank(message = "Role cannot be empty") String role,

            @NotBlank(message = "Content cannot be empty") @Size(max = 5000, message = "Content cannot exceed 5000 characters") String content) {}
}
