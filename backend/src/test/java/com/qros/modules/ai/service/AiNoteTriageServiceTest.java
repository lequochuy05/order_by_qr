package com.qros.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.request.AiNoteTriageRequest;
import com.qros.modules.ai.dto.response.AiNoteTriageResponse;
import com.qros.modules.ai.dto.response.AiNoteTriageResponse.TriagedNoteAlert;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.ai.support.NoteSafetyAnalyzer;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse.CategorySummary;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse.KitchenOrderItemResponse;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse.MenuItemSummary;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse.TableSummary;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.service.TableLookupService;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiNoteTriageServiceTest {

    private KitchenService kitchenService;
    private TableLookupService tableLookupService;
    private AiChatGateway aiChatGateway;
    private AiNoteTriageService service;

    @BeforeEach
    void setUp() {
        kitchenService = mock(KitchenService.class);
        tableLookupService = mock(TableLookupService.class);
        aiChatGateway = mock(AiChatGateway.class);

        service = new AiNoteTriageService(
                kitchenService,
                tableLookupService,
                aiChatGateway,
                new AiPromptBuilder(),
                new ObjectMapper(),
                new SimpleMeterRegistry());
    }

    @Test
    void returnsEmptyWithoutCallingAiWhenNoNotesExist() {
        when(kitchenService.getKitchenOrders()).thenReturn(List.of());
        when(tableLookupService.findServiceRequestsByStatuses(any(), any(), any()))
                .thenReturn(List.of());

        AiNoteTriageResponse response = service.triage(new AiNoteTriageRequest("vi"));

        assertThat(response.alerts()).isEmpty();
        assertThat(response.criticalCount()).isEqualTo(0);
        assertThat(response.aiGenerated()).isFalse();
        verifyNoInteractions(aiChatGateway);
    }

    @Test
    void combinesKitchenAndServiceNotesAndNeverDowngradesCriticalRisk() {
        LocalDateTime now = AppTime.now();
        KitchenOrderItemResponse peanutItem = new KitchenOrderItemResponse(
                10L,
                1L,
                1,
                new MenuItemSummary(100L, "Gỏi Cuốn", new CategorySummary("Khai vị")),
                null,
                new BigDecimal("45000"),
                1,
                "Khách dị ứng đậu phộng nặng",
                false,
                OrderItemStatus.PENDING,
                List.of(),
                now.minusMinutes(8),
                now.minusMinutes(8));
        KitchenOrderItemResponse finishedIgnoredItem = new KitchenOrderItemResponse(
                11L,
                1L,
                1,
                new MenuItemSummary(101L, "Trà đá", new CategorySummary("Đồ uống")),
                null,
                new BigDecimal("10000"),
                1,
                "Dị ứng nhưng đã xong",
                true,
                OrderItemStatus.FINISHED,
                List.of(),
                now.minusMinutes(20),
                now.minusMinutes(5));

        when(kitchenService.getKitchenOrders())
                .thenReturn(List.of(new KitchenOrderResponse(
                        1L,
                        OrderStatus.PENDING,
                        new BigDecimal("55000"),
                        new TableSummary(5L, "05"),
                        OrderType.DINE_IN,
                        List.of(peanutItem, finishedIgnoredItem),
                        now.minusMinutes(10))));

        TableServiceRequest urgentServiceReq = TableServiceRequest.builder()
                .id(20L)
                .table(DiningTable.builder().id(2L).tableNumber("02").build())
                .reason(ServiceRequestReason.UTENSILS_NAPKINS)
                .note("Bàn có em bé cần gấp ghế trẻ em")
                .status(ServiceRequestStatus.PENDING)
                .build();
        urgentServiceReq.setCreatedAt(now.minusMinutes(12));

        when(tableLookupService.findServiceRequestsByStatuses(any(), any(), any()))
                .thenReturn(List.of(urgentServiceReq));

        // Even if LLM returns NORMAL for item 10, deterministic CRITICAL cannot be downgraded
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("""
                {
                  "summary": "Có 1 cảnh báo dị ứng cần chế biến riêng và 1 yêu cầu cho trẻ em.",
                  "triaged": [
                    {
                      "sourceType": "KITCHEN_ITEM",
                      "sourceId": 10,
                      "riskLevel": "NORMAL",
                      "extraAllergens": ["SESAME"],
                      "actionInstruction": "Chế biến riêng không dùng nước chấm đậu phộng."
                    }
                  ]
                }
                """);

        AiNoteTriageResponse response = service.triage(new AiNoteTriageRequest("vi"));

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.criticalCount()).isEqualTo(1);
        assertThat(response.highCount()).isEqualTo(1);
        assertThat(response.alerts()).hasSize(2);

        TriagedNoteAlert first = response.alerts().get(0);
        assertThat(first.sourceType()).isEqualTo("KITCHEN_ITEM");
        assertThat(first.sourceId()).isEqualTo(10L);
        assertThat(first.riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_CRITICAL);
        assertThat(first.allergens()).contains("ALLERGY", "PEANUT_NUTS", "SESAME");
        assertThat(first.actionInstruction()).contains("Chế biến riêng");

        TriagedNoteAlert second = response.alerts().get(1);
        assertThat(second.sourceType()).isEqualTo("SERVICE_REQUEST");
        assertThat(second.sourceId()).isEqualTo(20L);
        assertThat(second.riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_HIGH);
        assertThat(second.tableLabel()).isEqualTo("Bàn 02");
    }

    @Test
    void usesEnglishFallbackSortedByRiskThenWaitMinutes() {
        LocalDateTime now = AppTime.now();
        KitchenOrderItemResponse mildOldItem = new KitchenOrderItemResponse(
                31L,
                1L,
                1,
                new MenuItemSummary(102L, "Noodle Soup", new CategorySummary("Mains")),
                null,
                new BigDecimal("60000"),
                1,
                "No spicy please",
                false,
                OrderItemStatus.COOKING,
                List.of(),
                now.minusMinutes(30),
                now.minusMinutes(25));
        KitchenOrderItemResponse allergyFreshItem = new KitchenOrderItemResponse(
                32L,
                1L,
                1,
                new MenuItemSummary(103L, "Fried Rice", new CategorySummary("Mains")),
                null,
                new BigDecimal("50000"),
                1,
                "Severe shrimp allergy",
                false,
                OrderItemStatus.PENDING,
                List.of(),
                now.minusMinutes(2),
                now.minusMinutes(2));

        when(kitchenService.getKitchenOrders())
                .thenReturn(List.of(new KitchenOrderResponse(
                        2L,
                        OrderStatus.PENDING,
                        new BigDecimal("110000"),
                        null,
                        OrderType.TAKEAWAY,
                        List.of(mildOldItem, allergyFreshItem),
                        now.minusMinutes(30))));
        when(tableLookupService.findServiceRequestsByStatuses(any(), any(), any()))
                .thenReturn(List.of());
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("Xin lỗi");

        AiNoteTriageResponse response = service.triage(new AiNoteTriageRequest("en"));

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.alerts()).hasSize(2);
        assertThat(response.alerts().get(0).sourceId()).isEqualTo(32L);
        assertThat(response.alerts().get(0).riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_CRITICAL);
        assertThat(response.alerts().get(0).tableLabel()).isEqualTo("Takeaway");
        assertThat(response.alerts().get(0).actionInstruction()).contains("Allergy protocol");
        assertThat(response.alerts().get(1).sourceId()).isEqualTo(31L);
        assertThat(response.alerts().get(1).dietaryFlags()).contains("NO_SPICY");
    }
}
