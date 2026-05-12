package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.AiChatRequest;
import com.sacmauquan.qrordering.dto.AiChatResponse;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AiAssistantService - Orchestrates menu filtering and Gemini AI integration
 * to provide intelligent food recommendations via natural conversation.
 * 
 * Architecture: Backend acts as Recommendation Engine → AI generates natural response.
 * AI never accesses the database directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final MenuItemRepository menuItemRepository;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.api-url}")
    private String geminiApiUrl;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Processes a customer chat message:
     * 1. Fetches active menu items from DB
     * 2. Builds a contextual prompt with menu data
     * 3. Sends to Gemini API for natural language response
     *
     * @param request The customer's chat message and history
     * @return AI-generated recommendation reply
     */
    public AiChatResponse chat(AiChatRequest request) {
        List<MenuItem> activeMenu = menuItemRepository.findAllByActiveTrue();
        String menuContext = buildMenuContext(activeMenu);
        String geminiResponse = callGeminiApi(request, menuContext);
        return AiChatResponse.builder().reply(geminiResponse).build();
    }

    /**
     * Builds a structured text representation of the current menu
     * for the AI to reference when making recommendations.
     */
    private String buildMenuContext(List<MenuItem> items) {
        if (items.isEmpty()) return "Hiện tại thực đơn đang trống.";

        Map<String, List<MenuItem>> grouped = items.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getCategory().getName(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        grouped.forEach((category, menuItems) -> {
            sb.append("\n📂 ").append(category).append(":\n");
            menuItems.forEach(item -> {
                sb.append("  - ").append(item.getName())
                        .append(" | Giá: ").append(formatPrice(item.getPrice()))
                        .append("\n");
            });
        });

        return sb.toString();
    }

    /**
     * Formats a BigDecimal price into Vietnamese currency string.
     */
    private String formatPrice(BigDecimal price) {
        if (price == null) return "Liên hệ";
        long value = price.longValue();
        if (value >= 1000) {
            return String.format("%,dk", value / 1000).replace(",", ".");
        }
        return value + "đ";
    }

    /**
     * Calls the Gemini API with the constructed prompt and conversation history.
     * Returns the AI-generated text response.
     */
    private String callGeminiApi(AiChatRequest request, String menuContext) {
        try {
            String url = geminiApiUrl + "?key=" + geminiApiKey;

            Map<String, Object> payload = buildGeminiPayload(request, menuContext);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return extractGeminiText(response.getBody());
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return "Xin lỗi, tôi đang gặp sự cố kỹ thuật. Bạn có thể xem thực đơn trực tiếp trên trang nhé! 😊";
        }
    }

    /**
     * Builds the Gemini API request payload including system instruction,
     * conversation history, and the current user message with menu context.
     */
    private Map<String, Object> buildGeminiPayload(AiChatRequest request, String menuContext) {
        // System instruction
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", buildSystemPrompt(menuContext))));

        // Build conversation contents
        List<Map<String, Object>> contents = new ArrayList<>();

        // Add conversation history
        if (request.getHistory() != null) {
            for (AiChatRequest.ChatMessage msg : request.getHistory()) {
                String role = "user".equals(msg.getRole()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))));
            }
        }

        // Add current user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.getMessage()))));

        // Generation config
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.7,
                "topP", 0.9,
                "maxOutputTokens", 500);

        return Map.of(
                "system_instruction", systemInstruction,
                "contents", contents,
                "generationConfig", generationConfig);
    }

    /**
     * The System Prompt that defines the AI's personality and constraints.
     * This is the core of the AI assistant's behavior.
     */
    private String buildSystemPrompt(String menuContext) {
        return """
                Bạn là "Trợ lý Sắc Màu" — nhân viên tư vấn món ăn thông minh và thân thiện của quán Sắc Màu Quán.

                ## QUY TẮC BẮT BUỘC:
                1. CHỈ tư vấn và gợi ý các món ăn/đồ uống CÓ TRONG thực đơn bên dưới. TUYỆT ĐỐI không bịa ra món không có.
                2. Trả lời ngắn gọn, thân thiện, tự nhiên như một người phục vụ nhiệt tình.
                3. Luôn kèm tên món và giá khi gợi ý.
                4. Nếu khách hỏi ngoài phạm vi thực đơn (thời tiết, tin tức...), nhẹ nhàng hướng khách quay lại chủ đề ăn uống.
                5. Khi khách nói chung chung ("ăn gì ngon", "gợi ý đi"), hãy hỏi lại sở thích hoặc gợi ý 2-3 món phổ biến.
                6. Sử dụng emoji phù hợp để tạo cảm giác gần gũi nhưng không quá nhiều.
                7. Trả lời bằng tiếng Việt.
                8. Không sử dụng markdown phức tạp. Chỉ dùng text thuần.

                ## THỰC ĐƠN HIỆN TẠI:
                %s
                """.formatted(menuContext);
    }

    /**
     * Extracts the generated text from Gemini API JSON response.
     */
    private String extractGeminiText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("Xin lỗi, tôi không hiểu. Bạn có thể nói rõ hơn không?");
                }
            }
            log.warn("Unexpected Gemini response structure: {}", responseBody);
            return "Xin lỗi, tôi chưa hiểu ý bạn. Bạn muốn ăn gì nhỉ? 😊";
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return "Xin lỗi, đã có lỗi xảy ra. Bạn thử hỏi lại nhé!";
        }
    }
}
