package com.qros.modules.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiStaffRequest(
        @NotBlank(message = "Message cannot be empty")
                @Size(max = 500, message = "Message cannot exceed 500 characters")
                String message,
        @Valid @Size(max = 10, message = "History cannot exceed 10 messages") List<StaffChatMessage> history) {

    public record StaffChatMessage(
            @NotBlank(message = "Role cannot be empty") String role,
            @NotBlank(message = "Content cannot be empty")
                    @Size(max = 800, message = "Content cannot exceed 800 characters")
                    String content) {}
}
