package com.sacmauquan.qrordering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AiChatResponse - DTO for AI assistant chat responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    /**
     * The AI-generated reply text.
     */
    private String reply;
}
