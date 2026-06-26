package com.qros.modules.ai.service;

import com.qros.modules.ai.dto.request.AiChatRequest;
import com.qros.modules.ai.dto.request.AiStaffRequest;
import com.qros.modules.ai.dto.response.AiStaffResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiStaffService {

    private static final String STAFF_FALLBACK_REPLY =
            "Xin lỗi, tôi chưa thể phân tích dữ liệu vận hành lúc này. Bạn thử lại sau hoặc làm mới trang bếp nhé.";

    private final StaffContextProvider staffContextProvider;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final Counter aiRequestsCounter;

    public AiStaffService(
            StaffContextProvider staffContextProvider,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            MeterRegistry meterRegistry) {
        this.staffContextProvider = staffContextProvider;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "staff")
                .description("AI staff assistant requests")
                .register(meterRegistry);
    }

    public AiStaffResponse query(AiStaffRequest request) {
        String staffContext = staffContextProvider.buildStaffContext();
        String systemPrompt = promptBuilder.buildStaffPrompt(staffContext);
        String reply = aiChatGateway.chat(toChatRequest(request), systemPrompt, staffContext);

        aiRequestsCounter.increment();

        if (reply == null || reply.isBlank() || reply.contains("xem thực đơn trực tiếp")) {
            return new AiStaffResponse(STAFF_FALLBACK_REPLY);
        }

        return new AiStaffResponse(reply);
    }

    private AiChatRequest toChatRequest(AiStaffRequest request) {
        List<AiChatRequest.ChatMessage> history = request.history() == null
                ? null
                : request.history().stream()
                        .map(message -> new AiChatRequest.ChatMessage(message.role(), message.content()))
                        .toList();

        return new AiChatRequest(request.message(), history);
    }
}
