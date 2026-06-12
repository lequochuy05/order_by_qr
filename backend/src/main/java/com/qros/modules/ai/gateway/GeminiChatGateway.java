package com.qros.modules.ai.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.modules.ai.config.GeminiProperties;
import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.support.AiPromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiChatGateway implements AiChatGateway {

    private static final String FALLBACK_REPLY = "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Bạn có thể xem thực đơn trực tiếp trên trang nhé! 😊";

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final AiPromptBuilder promptBuilder;
    private final RestTemplate restTemplate;

    public GeminiChatGateway(
            GeminiProperties properties,
            ObjectMapper objectMapper,
            AiPromptBuilder promptBuilder,
            RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
                .build();
    }

    @Override
    public String chat(AiChatRequest request, String menuContext) {
        if (!properties.enabled()) {
            return "Tính năng trợ lý AI hiện chưa được bật.";
        }

        try {
            String url = properties.apiUrl() + "?key=" + properties.apiKey();

            Map<String, Object> payload = buildPayload(request, menuContext);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return extractText(response.getBody());
        } catch (Exception exception) {
            log.error("Gemini API call failed: {}", exception.getMessage(), exception);
            return FALLBACK_REPLY;
        }
    }

    private Map<String, Object> buildPayload(AiChatRequest request, String menuContext) {
        Map<String, Object> systemInstruction = Map.of(
                "parts",
                List.of(Map.of("text", promptBuilder.buildSystemPrompt(menuContext))));

        List<Map<String, Object>> contents = new ArrayList<>();

        if (request.history() != null) {
            request.history().stream()
                    .filter(message -> message.content() != null && !message.content().isBlank())
                    .limit(10)
                    .forEach(message -> contents.add(Map.of(
                            "role", toGeminiRole(message.role()),
                            "parts", List.of(Map.of("text", message.content())))));
        }

        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.message()))));

        Map<String, Object> generationConfig = Map.of(
                "temperature", properties.temperature(),
                "topP", properties.topP(),
                "maxOutputTokens", properties.maxOutputTokens());

        return Map.of(
                "system_instruction", systemInstruction,
                "contents", contents,
                "generationConfig", generationConfig);
    }

    private String toGeminiRole(String role) {
        return "assistant".equalsIgnoreCase(role) || "model".equalsIgnoreCase(role)
                ? "model"
                : "user";
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");

                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text")
                            .asText("Xin lỗi, tôi chưa hiểu ý bạn. Bạn muốn ăn gì nhỉ? 😊");
                }
            }

            log.warn("Unexpected Gemini response structure: {}", responseBody);
            return "Xin lỗi, tôi chưa hiểu ý bạn. Bạn muốn ăn gì nhỉ? 😊";
        } catch (Exception exception) {
            log.error("Failed to parse Gemini response: {}", exception.getMessage());
            return FALLBACK_REPLY;
        }
    }
}