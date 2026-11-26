package com.qros.modules.ai.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.config.GeminiProperties;
import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.support.AiPromptBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class GeminiChatGatewayTest {

    private final ObjectMapper objectMapper = new JsonMapper();
    private final AiPromptBuilder promptBuilder = new AiPromptBuilder();

    @Test
    void disabledAiReturnsFriendlyNotice() {
        GeminiProperties properties = new GeminiProperties(
                false,
                "secret-key",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                5,
                15,
                0.7,
                0.95,
                500);
        GeminiChatGateway gateway =
                new GeminiChatGateway(properties, objectMapper, promptBuilder, new RestTemplateBuilder());

        String reply = gateway.chat(new AiChatRequest("Xin chào", null), "System prompt", "Menu context");

        assertThat(reply).isEqualTo("Tính năng trợ lý AI hiện chưa được bật.");
    }

    @Test
    void missingApiKeyReturnsFriendlyNotice() {
        GeminiProperties properties = new GeminiProperties(
                true,
                "",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                5,
                15,
                0.7,
                0.95,
                500);
        GeminiChatGateway gateway =
                new GeminiChatGateway(properties, objectMapper, promptBuilder, new RestTemplateBuilder());

        String reply = gateway.chat(new AiChatRequest("Xin chào", null), "System prompt", "Menu context");

        assertThat(reply).isEqualTo("Tính năng trợ lý AI chưa được cấu hình API key.");
    }

    @Test
    void successfulApiCallExtractsGeneratedContent() {
        GeminiProperties properties = new GeminiProperties(
                true,
                "test-api-key",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                5,
                15,
                0.7,
                0.95,
                500);
        RestTemplate restTemplate = mock(RestTemplate.class);
        GeminiChatGateway gateway =
                new GeminiChatGateway(properties, objectMapper, promptBuilder, new RestTemplateBuilder());
        ReflectionTestUtils.setField(gateway, "restTemplate", restTemplate);

        String mockResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "Chào bạn, quán có món Phở Bò rất ngon nhé!" }
                        ]
                      }
                    }
                  ]
                }
                """;

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(mockResponseBody));

        String reply = gateway.chat(
                new AiChatRequest("Món gì ngon?", List.of(new AiChatRequest.ChatMessage("user", "Chào bạn"))),
                "System prompt",
                "Menu context");

        assertThat(reply).isEqualTo("Chào bạn, quán có món Phở Bò rất ngon nhé!");
    }

    @Test
    void customModelUrlIsPreservedWithoutApiKeyQueryParam() {
        GeminiProperties properties = new GeminiProperties(
                true,
                "key123",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent",
                5,
                15,
                0.7,
                0.95,
                500);
        GeminiChatGateway gateway =
                new GeminiChatGateway(properties, objectMapper, promptBuilder, new RestTemplateBuilder());

        String resolved = ReflectionTestUtils.invokeMethod(gateway, "resolveApiUrl");

        assertThat(resolved)
                .isEqualTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent");
        assertThat(resolved).doesNotContain("key=");
    }

    @Test
    void maskSensitiveUrlRedactsApiKeyFromQueryStringsAndErrorMessages() {
        GeminiProperties properties = new GeminiProperties(
                true,
                "secret-gemini-key",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent",
                5,
                15,
                0.7,
                0.95,
                500);
        GeminiChatGateway gateway =
                new GeminiChatGateway(properties, objectMapper, promptBuilder, new RestTemplateBuilder());

        String masked = gateway.maskSensitiveUrl(
                "I/O error on POST request for \"https://generativelanguage.googleapis.com/v1beta/models/gemini:generateContent?key=secret-gemini-key\"");

        assertThat(masked).doesNotContain("secret-gemini-key");
        assertThat(masked).contains("?key=***");
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatWithImageSendsInlineBase64PayloadAndExtractsJsonText() {
        GeminiProperties properties = new GeminiProperties(
                true,
                "test-api-key",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                5,
                15,
                0.7,
                0.95,
                500);
        RestTemplate restTemplate = mock(RestTemplate.class);
        GeminiChatGateway gateway =
                new GeminiChatGateway(properties, objectMapper, promptBuilder, new RestTemplateBuilder());
        ReflectionTestUtils.setField(gateway, "restTemplate", restTemplate);

        String mockResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"supplierName\\":\\"An Phu\\"}" }
                        ]
                      }
                    }
                  ]
                }
                """;
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(mockResponseBody));

        byte[] sampleBytes = "fake-jpeg".getBytes(StandardCharsets.UTF_8);
        String reply = gateway.chatWithImage("Read invoice", "System OCR prompt", sampleBytes, "image/jpeg");

        assertThat(reply).isEqualTo("{\"supplierName\":\"An Phu\"}");

        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        Map<String, Object> body = entityCaptor.getValue().getBody();
        assertThat(body).isNotNull();
        List<Map<String, Object>> contents = (List<Map<String, Object>>) body.get("contents");
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) contents.get(0).get("parts");
        Map<String, Object> inlineData = (Map<String, Object>) parts.get(1).get("inline_data");
        assertThat(inlineData.get("mime_type")).isEqualTo("image/jpeg");
        assertThat(inlineData.get("data")).isEqualTo(Base64.getEncoder().encodeToString(sampleBytes));
    }

    @Test
    void chatWithImageHonorsDisabledFlagWithoutCallingRestTemplate() {
        GeminiProperties properties = new GeminiProperties(
                false,
                "secret-key",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                5,
                15,
                0.7,
                0.95,
                500);
        GeminiChatGateway gateway =
                new GeminiChatGateway(properties, objectMapper, promptBuilder, new RestTemplateBuilder());

        String reply = gateway.chatWithImage("Read invoice", "System OCR", new byte[] {1, 2, 3}, "image/png");

        assertThat(reply).isEqualTo("Tính năng trợ lý AI hiện chưa được bật.");
    }
}
