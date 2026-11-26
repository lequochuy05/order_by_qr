package com.qros.modules.ai.gateway;

import com.qros.modules.ai.config.GeminiProperties;
import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.support.AiPromptBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class GeminiChatGateway implements AiChatGateway {

    private static final String FALLBACK_REPLY =
            "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Bạn có thể xem thực đơn trực tiếp trên trang nhé! 😊";
    private static final long MAX_TOTAL_BUDGET_NANOS = Duration.ofSeconds(45).toNanos();

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
        String precheck = checkAvailabilityNotice();
        if (precheck != null) {
            return precheck;
        }

        Map<String, Object> payload = buildPayload(request, systemPrompt);
        return executeGeminiRequest(payload);
    }

    @Override
    public String chatWithImage(String userMessage, String systemPrompt, byte[] imageBytes, String mimeType) {
        String precheck = checkAvailabilityNotice();
        if (precheck != null) {
            return precheck;
        }

        if (imageBytes == null || imageBytes.length == 0 || mimeType == null || mimeType.isBlank()) {
            return chat(new AiChatRequest(userMessage, List.of()), systemPrompt, "");
        }

        String encodedImage = Base64.getEncoder().encodeToString(imageBytes);
        String promptText = (userMessage == null || userMessage.isBlank())
                ? "Analyze the attached supplier invoice image."
                : userMessage;

        Map<String, Object> payload = Map.of(
                "system_instruction",
                Map.of("parts", List.of(Map.of("text", systemPrompt != null ? systemPrompt : ""))),
                "contents",
                List.of(Map.of(
                        "role",
                        "user",
                        "parts",
                        List.of(
                                Map.of("text", promptText),
                                Map.of("inline_data", Map.of("mime_type", mimeType, "data", encodedImage))))),
                "generationConfig",
                Map.of(
                        "temperature",
                        Math.min(properties.temperature(), 0.2),
                        "topP",
                        properties.topP(),
                        "maxOutputTokens",
                        Math.max(properties.maxOutputTokens(), 2048)));

        return executeGeminiRequest(payload);
    }

    private String checkAvailabilityNotice() {
        if (!properties.enabled()) {
            return "Tính năng trợ lý AI hiện chưa được bật.";
        }

        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("Gemini API key is not configured");
            return "Tính năng trợ lý AI chưa được cấu hình API key.";
        }
        return null;
    }

    private String executeGeminiRequest(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.apiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        String primaryUrl = resolveApiUrl();

        List<String> targetUrls = new ArrayList<>();
        targetUrls.add(primaryUrl);
        String configuredFallback = resolveFallbackApiUrl(primaryUrl);
        if (configuredFallback != null && !configuredFallback.equals(primaryUrl)) {
            targetUrls.add(configuredFallback);
        }

        long deadlineNanos = System.nanoTime() + MAX_TOTAL_BUDGET_NANOS;

        for (int urlIndex = 0; urlIndex < targetUrls.size(); urlIndex++) {
            String url = targetUrls.get(urlIndex);
            boolean hasAlternateModel = urlIndex < targetUrls.size() - 1;
            for (int attempt = 1; attempt <= 2; attempt++) {
                if (urlIndex > 0 || attempt > 1) {
                    if (System.nanoTime() >= deadlineNanos) {
                        log.warn("Gemini API total retry budget exhausted; returning fallback reply");
                        return FALLBACK_REPLY;
                    }
                }
                try {
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                    return extractText(response.getBody());
                } catch (HttpStatusCodeException httpEx) {
                    int statusCode = httpEx.getStatusCode().value();
                    boolean isTransient = statusCode == 503 || statusCode == 429;
                    boolean isModelNotFound = statusCode == 404;
                    if (isModelNotFound && hasAlternateModel) {
                        log.warn(
                                "Gemini model URL {} returned 404, switching to alternate model...",
                                maskSensitiveUrl(url));
                        break;
                    }
                    if (isTransient
                            && attempt < 2
                            && System.nanoTime() + Duration.ofSeconds(2).toNanos() < deadlineNanos) {
                        log.warn(
                                "Gemini API returned status {} (transient spike), retrying in 1s (attempt {}/2)...",
                                statusCode,
                                attempt);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return FALLBACK_REPLY;
                        }
                        continue;
                    }
                    if (isTransient && hasAlternateModel) {
                        log.warn(
                                "Gemini model in {} is overloaded (503/429), switching to alternate model...",
                                maskSensitiveUrl(url));
                        break;
                    }
                    log.error(
                            "Gemini API call failed with HTTP status {} {}: {}",
                            httpEx.getStatusCode(),
                            httpEx.getStatusText(),
                            maskSensitiveUrl(httpEx.getResponseBodyAsString()));
                    return FALLBACK_REPLY;
                } catch (Exception exception) {
                    log.error("Gemini API call failed: {}", maskSensitiveUrl(exception.getMessage()));
                    return FALLBACK_REPLY;
                }
            }
        }
        return FALLBACK_REPLY;
    }

    String maskSensitiveUrl(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = text.replaceAll("([?&]key=)[^&\\s\"']+", "$1***");
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            masked = masked.replace(properties.apiKey().trim(), "***");
        }
        return masked;
    }

    private String resolveApiUrl() {
        String url = properties.apiUrl();
        if (url == null || url.isBlank()) {
            url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";
        }
        // Authenticate exclusively via x-goog-api-key header (never duplicate API key in URL query params).
        return url.trim();
    }

    private String resolveFallbackApiUrl(String primaryUrl) {
        if (properties.fallbackApiUrl() != null && !properties.fallbackApiUrl().isBlank()) {
            return properties.fallbackApiUrl().trim();
        }
        if (primaryUrl.contains("gemini-3.5-flash-lite")) {
            return primaryUrl.replace("gemini-3.5-flash-lite", "gemini-3.8-flash");
        }
        if (primaryUrl.contains("gemini-3.8-flash")) {
            return primaryUrl.replace("gemini-3.8-flash", "gemini-3.5-flash-lite");
        }
        return null;
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
                    return parts.get(0).path("text").asString("Xin lỗi, tôi chưa hiểu ý bạn. Bạn muốn ăn gì nhỉ? 😊");
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
