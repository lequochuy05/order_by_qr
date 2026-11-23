package com.qros.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.request.AiExecutiveSummaryRequest;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse.MetricsSnapshot;
import com.qros.modules.ai.dto.response.AiExecutiveSummaryResponse.TopDishMetric;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.ExecutiveInsightsContextProvider.ExecutiveInsightsData;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.repository.projection.DashboardSummaryProjection;
import com.qros.modules.analytics.repository.projection.OrderFilterSummaryProjection;
import com.qros.modules.analytics.repository.projection.TableSummaryProjection;
import com.qros.modules.analytics.repository.projection.TopSellingItemProjection;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse;
import com.qros.modules.kitchen.dto.response.KitchenOrderResponse.KitchenOrderItemResponse;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.enums.ServiceRequestReason;
import com.qros.modules.table.model.enums.ServiceRequestStatus;
import com.qros.modules.table.repository.TableServiceRequestRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiExecutiveInsightsServiceTest {

    private ExecutiveInsightsContextProvider contextProvider;
    private AiChatGateway aiChatGateway;
    private AiExecutiveInsightsService service;

    @BeforeEach
    void setUp() {
        contextProvider = mock(ExecutiveInsightsContextProvider.class);
        aiChatGateway = mock(AiChatGateway.class);

        service = new AiExecutiveInsightsService(
                contextProvider, aiChatGateway, new AiPromptBuilder(), new ObjectMapper(), new SimpleMeterRegistry());
    }

    @Test
    void returnsAiGeneratedExecutiveSummaryWhenGeminiReturnsValidJson() {
        LocalDate from = LocalDate.of(2026, 11, 15);
        LocalDate to = LocalDate.of(2026, 11, 22);
        MetricsSnapshot snapshot = sampleMetrics();

        when(contextProvider.loadInsights(from, to)).thenReturn(new ExecutiveInsightsData(snapshot, "CTX"));
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("""
                {
                  "headline": "Doanh thu tăng 18.5% nhờ món Phở Bò dẫn đầu.",
                  "highlights": ["Doanh thu đạt 12.500.000đ (+18.5%).", "Hoàn tất 62 đơn."],
                  "anomalies": [
                    {"severity": "high", "title": "Nghẽn bếp", "detail": "Có 2 món chờ trên 20 phút."}
                  ],
                  "recommendations": ["Ưu tiên nấu gom món chờ lâu.", "Nhập thêm Thịt bò Mỹ."]
                }
                """);

        AiExecutiveSummaryResponse response = service.generateSummary(new AiExecutiveSummaryRequest(from, to, "vi"));

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.language()).isEqualTo("vi");
        assertThat(response.headline()).contains("18.5%");
        assertThat(response.highlights()).hasSize(2);
        assertThat(response.anomalies()).hasSize(1);
        assertThat(response.anomalies().get(0).severity()).isEqualTo("HIGH");
        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.metrics()).isEqualTo(snapshot);
    }

    @Test
    void usesBilingualDeterministicFallbackWhenAiGatewayIsUnavailable() {
        LocalDate from = LocalDate.of(2026, 11, 16);
        LocalDate to = LocalDate.of(2026, 11, 22);
        MetricsSnapshot snapshot = sampleMetrics();

        when(contextProvider.loadInsights(from, to)).thenReturn(new ExecutiveInsightsData(snapshot, "CTX"));
        when(aiChatGateway.chat(any(), anyString(), anyString())).thenReturn("Tính năng trợ lý AI hiện chưa được bật.");

        AiExecutiveSummaryResponse enResponse = service.generateSummary(new AiExecutiveSummaryRequest(from, to, "en"));

        assertThat(enResponse.aiGenerated()).isFalse();
        assertThat(enResponse.language()).isEqualTo("en");
        assertThat(enResponse.headline()).contains("62 completed orders");
        assertThat(enResponse.anomalies()).isNotEmpty();
        assertThat(enResponse.recommendations()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void rejectsReversedOrOverlyLongDateRangesWithEnglishErrorMessage() {
        LocalDate today = LocalDate.of(2026, 11, 22);

        assertThatThrownBy(
                        () -> service.generateSummary(new AiExecutiveSummaryRequest(today, today.minusDays(2), "vi")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException biz = (BusinessException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
                    assertThat(biz.getMessage()).isEqualTo("Start date must not be after end date");
                });

        assertThatThrownBy(
                        () -> service.generateSummary(new AiExecutiveSummaryRequest(today.minusDays(120), today, "vi")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException biz = (BusinessException) ex;
                    assertThat(biz.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
                    assertThat(biz.getMessage()).contains("90 days");
                });
    }

    @Test
    void contextProviderAggregatesRevenueDeltaKitchenSlaServiceRequestsAndInventory() {
        AnalyticsQueryRepository analyticsRepo = mock(AnalyticsQueryRepository.class);
        KitchenService kitchenService = mock(KitchenService.class);
        TableServiceRequestRepository serviceRequestRepo = mock(TableServiceRequestRepository.class);
        InventoryItemRepository inventoryRepo = mock(InventoryItemRepository.class);

        LocalDate from = LocalDate.of(2026, 11, 20);
        LocalDate to = LocalDate.of(2026, 11, 22);

        DashboardSummaryProjection currentProj = mock(DashboardSummaryProjection.class);
        when(currentProj.getTotalRevenue()).thenReturn(new BigDecimal("1500000"));
        when(currentProj.getTotalOrders()).thenReturn(15L);
        when(currentProj.getTotalItemsSold()).thenReturn(40L);
        when(currentProj.getAverageOrderValue()).thenReturn(new BigDecimal("100000"));

        DashboardSummaryProjection prevProj = mock(DashboardSummaryProjection.class);
        when(prevProj.getTotalRevenue()).thenReturn(new BigDecimal("1000000"));
        when(prevProj.getTotalOrders()).thenReturn(10L);

        when(analyticsRepo.dashboardSummary(from, to.plusDays(1))).thenReturn(currentProj);
        when(analyticsRepo.dashboardSummary(LocalDate.of(2026, 11, 17), from)).thenReturn(prevProj);

        OrderFilterSummaryProjection cancelledProj = mock(OrderFilterSummaryProjection.class);
        when(cancelledProj.getTotalOrders()).thenReturn(2L);
        when(analyticsRepo.orderFilterSummary("CANCELLED", from, to.plusDays(1), null, null))
                .thenReturn(cancelledProj);

        TableSummaryProjection tableProj = mock(TableSummaryProjection.class);
        when(tableProj.getOccupiedTables()).thenReturn(4L);
        when(tableProj.getTotalTables()).thenReturn(12L);
        when(analyticsRepo.tableSummary()).thenReturn(tableProj);

        TopSellingItemProjection dishProj = mock(TopSellingItemProjection.class);
        when(dishProj.getItemName()).thenReturn("Bún Bò Huế");
        when(dishProj.getQuantitySold()).thenReturn(20L);
        when(dishProj.getRevenue()).thenReturn(new BigDecimal("900000"));
        when(analyticsRepo.topSellingItems(from, to.plusDays(1), 5)).thenReturn(List.of(dishProj));

        LocalDateTime now = AppTime.now();
        KitchenOrderItemResponse delayedItem = new KitchenOrderItemResponse(
                100L,
                1L,
                1,
                null,
                null,
                new BigDecimal("50000"),
                1,
                null,
                false,
                OrderItemStatus.COOKING,
                List.of(),
                now.minusMinutes(26),
                now.minusMinutes(20));
        when(kitchenService.getKitchenOrders())
                .thenReturn(List.of(new KitchenOrderResponse(
                        1L,
                        OrderStatus.SERVING,
                        new BigDecimal("50000"),
                        null,
                        OrderType.DINE_IN,
                        List.of(delayedItem),
                        now.minusMinutes(26))));

        TableServiceRequest pendingOld = TableServiceRequest.builder()
                .id(1L)
                .table(DiningTable.builder().id(1L).tableNumber("T01").build())
                .reason(ServiceRequestReason.WATER_ICE)
                .status(ServiceRequestStatus.PENDING)
                .createdAt(now.minusMinutes(8))
                .build();
        when(serviceRequestRepo.findByCreatedAtRange(
                        from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(pendingOld));

        InventoryItem lowItem = InventoryItem.builder()
                .id(7L)
                .name("Thịt bò")
                .unit("kg")
                .quantityOnHand(new BigDecimal("1.500"))
                .reservedQuantity(BigDecimal.ZERO)
                .lowStockThreshold(new BigDecimal("5.000"))
                .active(true)
                .build();
        when(inventoryRepo.findLowStockActiveItems()).thenReturn(List.of(lowItem));
        when(inventoryRepo.countOutOfStockActiveItems()).thenReturn(0L);

        ExecutiveInsightsContextProvider provider =
                new ExecutiveInsightsContextProvider(analyticsRepo, kitchenService, serviceRequestRepo, inventoryRepo);

        ExecutiveInsightsData data = provider.loadInsights(from, to);

        assertThat(data.metrics().revenueChangePercent()).isEqualTo(50.0);
        assertThat(data.metrics().cancelledOrders()).isEqualTo(2L);
        assertThat(data.metrics().overdueKitchenItems()).isEqualTo(1L);
        assertThat(data.metrics().maxKitchenWaitMinutes()).isGreaterThanOrEqualTo(25L);
        assertThat(data.metrics().overdueServiceRequests()).isEqualTo(1L);
        assertThat(data.metrics().topServiceRequestReason()).isEqualTo("WATER_ICE");
        assertThat(data.metrics().lowStockItems()).isEqualTo(1L);
        assertThat(data.formattedContext()).contains("Bún Bò Huế").contains("Thịt bò");
    }

    private MetricsSnapshot sampleMetrics() {
        return new MetricsSnapshot(
                new BigDecimal("12500000"),
                new BigDecimal("10550000"),
                18.5,
                62L,
                50L,
                2L,
                new BigDecimal("201613"),
                148L,
                6L,
                14L,
                4L,
                2L,
                24L,
                9L,
                1L,
                1L,
                "WATER_ICE",
                2L,
                1L,
                List.of("Thịt bò Mỹ (0.5 kg)"),
                List.of(new TopDishMetric("Phở Bò Đặc Biệt", 32L, new BigDecimal("2560000"))));
    }
}
