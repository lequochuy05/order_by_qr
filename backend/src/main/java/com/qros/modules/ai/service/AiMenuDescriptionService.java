package com.qros.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiMenuDescriptionRequest;
import com.qros.modules.ai.dto.response.AiMenuDescriptionResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiMenuDescriptionService {

    private static final String FALLBACK_PREFIX = "Xin lỗi";
    private static final String ERROR_PREFIX = "Tính năng trợ lý";

    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiMenuDescriptionService(
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "menu-description")
                .description("AI menu description generation requests")
                .register(meterRegistry);
    }

    public AiMenuDescriptionResponse generate(AiMenuDescriptionRequest request) {
        String ingredients = request.ingredients() != null ? String.join(", ", request.ingredients()) : "";
        String price = request.price() != null ? request.price() : "";

        String systemPrompt = promptBuilder.buildMenuDescriptionPrompt(
                request.itemName(), request.categoryName(), price, ingredients);

        AiChatRequest chatRequest = new AiChatRequest("Tạo mô tả món ăn", null);
        String reply = aiChatGateway.chat(chatRequest, systemPrompt, "");

        aiRequestsCounter.increment();

        if (reply == null || reply.isBlank() || reply.startsWith(FALLBACK_PREFIX) || reply.startsWith(ERROR_PREFIX)) {
            log.warn("AI description generation failed: {}", reply);
            throw new BusinessException(
                    ErrorCode.APP_ERROR, "AI không thể tạo mô tả ngay lúc này. Vui lòng thử lại sau.");
        }

        return parseResponse(reply);
    }

    private AiMenuDescriptionResponse parseResponse(String reply) {
        try {
            String json = reply;
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7, json.lastIndexOf("```"));
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3, json.lastIndexOf("```"));
            }
            json = json.trim();

            if (!json.startsWith("{")) {
                log.warn("AI response is not JSON (missing opening brace)");
                throw new BusinessException(
                        ErrorCode.APP_ERROR, "AI không thể tạo mô tả ngay lúc này. Vui lòng thử lại sau.");
            }

            JsonNode root = objectMapper.readTree(json);

            List<String> tasteTags = new ArrayList<>();
            JsonNode tagsNode = root.path("tasteTags");
            if (tagsNode.isArray()) {
                tagsNode.forEach(tag -> tasteTags.add(tag.asText()));
            }

            return new AiMenuDescriptionResponse(
                    root.path("shortDescription").asText(""),
                    root.path("engagingDescription").asText(""),
                    tasteTags);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse AI description response: {}", e.getMessage());
            throw new BusinessException(
                    ErrorCode.APP_ERROR, "AI không thể tạo mô tả ngay lúc này. Vui lòng thử lại sau.");
        }
    }
}
