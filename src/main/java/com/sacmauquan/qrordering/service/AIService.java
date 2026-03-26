package com.sacmauquan.qrordering.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIService {

    @Value("${google.ai.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";

    public String analyzeDishImage(MultipartFile file) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "{\"error\": \"Chưa cấu hình Gemini API Key\"}";
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType();

            String prompt = "Analyze this food image. Return ONLY a valid JSON object with exactly 2 keys: 'name' (Vietnamese name of the dish), 'price' (estimated integer price in VND, e.g. 35000)";
            
            Map<String, Object> inlineData = Map.of(
                    "mime_type", mimeType,
                    "data", base64Image
            );
            
            Map<String, Object> partText = Map.of("text", prompt);
            Map<String, Object> partImage = Map.of("inline_data", inlineData);

            return callGemini(List.of(partText, partImage));

        } catch (Exception e) {
            log.error("Error analyzing dish image", e);
            return "{\"error\": \"Không thể phân tích hình ảnh lúc này.\"}";
        }
    }

    public String getMenuRecommendations(String menuJson, String timeContext, String weatherContext) {
        String prompt = String.format(
                "Based on the following menu items (JSON): %s\n" +
                "And the current context: Time of day: %s, Weather: %s.\n" +
                "Recommend 3-5 most suitable dishes. Return ONLY a valid JSON array of dish IDs (integers). " +
                "Example: [1, 2, 3]",
                menuJson, timeContext, weatherContext
        );
        return callGemini(List.of(Map.of("text", prompt)));
    }

    public String getCrossSellRecommendations(String dishName, String menuJson) {
        String prompt = String.format(
                "The customer just added '%s' to their cart. Based on the menu: %s\n" +
                "Recommend 2-3 items that go well with it (e.g., drinks for food, toppings for drinks). " +
                "Return ONLY a valid JSON array of dish IDs (integers). Example: [4, 5]",
                dishName, menuJson
        );
        return callGemini(List.of(Map.of("text", prompt)));
    }

    private String callGemini(List<Map<String, Object>> parts) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "[]";
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", parts)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL + apiKey, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return extractTextFromGeminiResponse(response.getBody());
            }
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("AI Quota exceeded");
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
        }
        return "[]";
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");

            if (!textNode.isMissingNode()) {
                String result = textNode.asText().trim();

                if (result.startsWith("```json")) {
                    result = result.substring(7);
                } else if (result.startsWith("```")) {
                    result = result.substring(3);
                }
                if (result.endsWith("```")) {
                    result = result.substring(0, result.length() - 3);
                }

                return result.trim();
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
        }
        return "[]";
    }
}