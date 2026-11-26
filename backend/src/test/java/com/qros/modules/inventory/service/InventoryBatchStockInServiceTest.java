package com.qros.modules.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.inventory.dto.request.BatchStockInRequest;
import com.qros.modules.inventory.dto.request.BatchStockInRequest.BatchStockInLine;
import com.qros.modules.inventory.dto.response.BatchStockInResponse;
import com.qros.modules.inventory.mapper.InventoryItemMapper;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.model.enums.StockMovementType;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.OrderItemInventoryReservationRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.shared.event.DomainEvents.InventoryChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class InventoryBatchStockInServiceTest {

    private InventoryItemRepository inventoryItemRepository;
    private StockMovementService stockMovementService;
    private InventoryAvailabilityService inventoryAvailabilityService;
    private ApplicationEventPublisher eventPublisher;
    private InventoryItemService service;

    @BeforeEach
    void setUp() {
        inventoryItemRepository = mock(InventoryItemRepository.class);
        RecipeItemRepository recipeItemRepository = mock(RecipeItemRepository.class);
        OrderItemInventoryReservationRepository reservationRepository =
                mock(OrderItemInventoryReservationRepository.class);
        stockMovementService = mock(StockMovementService.class);
        inventoryAvailabilityService = mock(InventoryAvailabilityService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        service = new InventoryItemService(
                inventoryItemRepository,
                recipeItemRepository,
                reservationRepository,
                new InventoryItemMapper(),
                stockMovementService,
                inventoryAvailabilityService,
                eventPublisher);
    }

    @Test
    void managerCanBatchStockInAndOptionallyUpdateUnitCostAtomically() {
        InventoryItem beef = InventoryItem.builder()
                .id(1L)
                .name("Thịt bò")
                .unit("kg")
                .quantityOnHand(new BigDecimal("2.000"))
                .reservedQuantity(BigDecimal.ZERO)
                .lowStockThreshold(new BigDecimal("1.000"))
                .unitCost(new BigDecimal("200000.00"))
                .active(true)
                .version(1L)
                .build();
        InventoryItem milk = InventoryItem.builder()
                .id(5L)
                .name("Sữa tươi")
                .unit("l")
                .quantityOnHand(new BigDecimal("1.000"))
                .reservedQuantity(BigDecimal.ZERO)
                .lowStockThreshold(new BigDecimal("2.000"))
                .unitCost(new BigDecimal("30000.00"))
                .active(true)
                .version(1L)
                .build();

        when(inventoryItemRepository.findAllByIdInForUpdate(List.of(1L, 5L))).thenReturn(List.of(beef, milk));
        when(inventoryItemRepository.saveAllAndFlush(any())).thenReturn(List.of(beef, milk));

        BatchStockInRequest request = new BatchStockInRequest(
                "An Phú",
                "HD-88",
                "Nhập sáng",
                List.of(
                        new BatchStockInLine(5L, new BigDecimal("4.000"), new BigDecimal("32000.00"), true, null),
                        new BatchStockInLine(1L, new BigDecimal("3.000"), new BigDecimal("220000.00"), false, "Tươi")));

        BatchStockInResponse response = service.batchStockIn(request, true);

        assertThat(response.importedCount()).isEqualTo(2);
        // 4 * 32000 (updated cost) + 3 * 200000 (existing cost kept since updateUnitCost=false) = 728000
        assertThat(response.totalEstimatedValue()).isEqualByComparingTo("728000.00");
        assertThat(milk.getQuantityOnHand()).isEqualByComparingTo("5.000");
        assertThat(milk.getUnitCost()).isEqualByComparingTo("32000.00");
        assertThat(beef.getQuantityOnHand()).isEqualByComparingTo("5.000");
        assertThat(beef.getUnitCost()).isEqualByComparingTo("200000.00");

        verify(stockMovementService)
                .recordMovement(
                        eq(milk),
                        eq(null),
                        eq(StockMovementType.STOCK_IN),
                        eq(new BigDecimal("4.000")),
                        eq(new BigDecimal("1.000")),
                        eq(new BigDecimal("5.000")),
                        eq("An Phú · #HD-88: Nhập sáng"));
        verify(stockMovementService)
                .recordMovement(
                        eq(beef),
                        eq(null),
                        eq(StockMovementType.STOCK_IN),
                        eq(new BigDecimal("3.000")),
                        eq(new BigDecimal("2.000")),
                        eq(new BigDecimal("5.000")),
                        eq("An Phú · #HD-88: Tươi"));
        verify(inventoryAvailabilityService).syncAvailabilityForInventoryItems(List.of(1L, 5L), true);
        verify(eventPublisher).publishEvent(any(InventoryChangeEvent.class));
    }

    @Test
    void rejectsChefAttemptingToUpdateUnitCostWithoutLockingOrWriting() {
        BatchStockInRequest request = new BatchStockInRequest(
                "An Phú",
                "HD-89",
                null,
                List.of(new BatchStockInLine(1L, new BigDecimal("2.000"), new BigDecimal("190000"), true, null)));

        assertThatThrownBy(() -> service.batchStockIn(request, false))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verifyNoInteractions(inventoryItemRepository, stockMovementService, inventoryAvailabilityService);
    }

    @Test
    void throwsNotFoundWhenAnyLockedItemDoesNotExist() {
        InventoryItem existing = InventoryItem.builder()
                .id(1L)
                .name("Thịt bò")
                .unit("kg")
                .active(true)
                .build();
        when(inventoryItemRepository.findAllByIdInForUpdate(anyList())).thenReturn(List.of(existing));

        BatchStockInRequest request = new BatchStockInRequest(
                null,
                null,
                null,
                List.of(
                        new BatchStockInLine(1L, BigDecimal.ONE, null, false, null),
                        new BatchStockInLine(999L, BigDecimal.ONE, null, false, null)));

        assertThatThrownBy(() -> service.batchStockIn(request, true))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVENTORY_ITEM_NOT_FOUND);

        verifyNoInteractions(stockMovementService, inventoryAvailabilityService);
    }
}
