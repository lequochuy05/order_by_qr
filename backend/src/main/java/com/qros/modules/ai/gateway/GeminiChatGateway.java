package com.qros.modules.ai.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qros.modules.ai.config.GeminiProperties;
import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.support.AiPromptBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class GeminiChatGateway implements AiChatGateway {

    private static final String FALLBACK_REPLY =
            "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Bạn có thể xem thực đơn trực tiếp trên trang nhé! 😊";

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
        return chat(request, promptBuilder.buildSystemPrompt(menuContext), menuContext);
    }

    @Override
    public String chat(AiChatRequest request, String systemPrompt, String menuContext) {
        if (!properties.enabled()) {
            return "Tính năng trợ lý AI hiện chưa được bật.";
        }

        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("Gemini API key is not configured");
            return "Tính năng trợ lý AI chưa được cấu hình API key.";
        }

        Map<String, Object> payload = buildPayload(request, systemPrompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.apiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String primaryUrl = resolveApiUrl();

        List<String> targetUrls = new ArrayList<>();
        targetUrls.add(primaryUrl);
        if (primaryUrl.contains("gemini-3.5-flash-lite")) {
            targetUrls.add(primaryUrl.replace("gemini-3.5-flash-lite", "gemini-3.8-flash"));
        } else if (primaryUrl.contains("gemini-3.8-flash")) {
            targetUrls.add(primaryUrl.replace("gemini-3.8-flash", "gemini-3.5-flash-lite"));
        }

        for (String url : targetUrls) {
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                    return extractText(response.getBody());
                } catch (HttpStatusCodeException httpEx) {
                    int statusCode = httpEx.getStatusCode().value();
                    boolean isTransient = statusCode == 503 || statusCode == 429;
                    if (isTransient && attempt < 2) {
                        log.warn(
                                "Gemini API returned status {} (transient spike), retrying in 1s (attempt {}/2)...",
                                statusCode,
                                attempt);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    if (isTransient && targetUrls.indexOf(url) < targetUrls.size() - 1) {
                        log.warn("Gemini model in {} is overloaded (503/429), switching to alternate model...", url);
                        break;
                    }
                    log.error(
                            "Gemini API call failed with HTTP status {} {}: {}",
                            httpEx.getStatusCode(),
                            httpEx.getStatusText(),
                            httpEx.getResponseBodyAsString());
                    return FALLBACK_REPLY;
                } catch (Exception exception) {
                    log.error("Gemini API call failed: {}", exception.getMessage(), exception);
                    return FALLBACK_REPLY;
                }
            }
        }
        return FALLBACK_REPLY;
    }

    private String resolveApiUrl() {
        String url = properties.apiUrl();
        if (url == null || url.isBlank()) {
            url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";
        }
        if (!url.contains("key=")
                && properties.apiKey() != null
                && !properties.apiKey().isBlank()) {
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "key=" + properties.apiKey().trim();
        }
        return url;
    }

    private Map<String, Object> buildPayload(AiChatRequest request, String systemPrompt) {
        Map<String, Object> systemInstruction = Map.of("parts", List.of(Map.of("text", systemPrompt)));

        List<Map<String, Object>> contents = buildContents(request);

        Map<String, Object> generationConfig = Map.of(
                "temperature", properties.temperature(),
                "topP", properties.topP(),
                "maxOutputTokens", properties.maxOutputTokens());

        return Map.of(
                "system_instruction", systemInstruction,
                "contents", contents,
                "generationConfig", generationConfig);
    }

    private List<Map<String, Object>> buildContents(AiChatRequest request) {
        List<Map<String, Object>> raw = new ArrayList<>();

        if (request.history() != null) {
            request.history().stream()
                    .filter(message ->
                            message.content() != null && !message.content().isBlank())
                    .limit(10)
                    .forEach(message -> raw.add(Map.of(
                            "role", toGeminiRole(message.role()),
                            "parts", List.of(Map.of("text", message.content())))));
        }

        raw.add(Map.of("role", "user", "parts", List.of(Map.of("text", request.message()))));

        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Map<String, Object> item : raw) {
            String role = (String) item.get("role");
            // Gemini requires the multi-turn talk to begin with a user message
            if (sanitized.isEmpty() && !"user".equals(role)) {
                continue;
            }
            // Gemini requires roles to strictly alternate between user and model
            if (!sanitized.isEmpty()
                    && role.equals(sanitized.get(sanitized.size() - 1).get("role"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> prev = sanitized.get(sanitized.size() - 1);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> prevParts = (List<Map<String, Object>>) prev.get("parts");
                String prevText = (String) prevParts.get(0).get("text");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> itemParts = (List<Map<String, Object>>) item.get("parts");
                String itemText = (String) itemParts.get(0).get("text");

                sanitized.set(
                        sanitized.size() - 1,
                        Map.of("role", role, "parts", List.of(Map.of("text", prevText + "\n" + itemText))));
            } else {
                sanitized.add(item);
            }
        }

        // Guarantee at least the user request is present
        if (sanitized.isEmpty()
                || !"user".equals(sanitized.get(sanitized.size() - 1).get("role"))) {
            sanitized.add(Map.of("role", "user", "parts", List.of(Map.of("text", request.message()))));
        }

        return sanitized;
    }

    private String toGeminiRole(String role) {
        return "assistant".equalsIgnoreCase(role) || "model".equalsIgnoreCase(role) ? "model" : "user";
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");

                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("Xin lỗi, tôi chưa hiểu ý bạn. Bạn muốn ăn gì nhỉ? 😊");
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
