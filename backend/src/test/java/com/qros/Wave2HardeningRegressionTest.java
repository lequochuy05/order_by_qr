package com.qros;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.request.AiNoteTriageRequest;
import com.qros.modules.ai.dto.response.AiInvoiceOcrResponse;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.service.AiInvoiceOcrService;
import com.qros.modules.ai.service.AiNoteTriageService;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.analytics.repository.AnalyticsQueryRepository;
import com.qros.modules.analytics.service.AnalyticsForecastService;
import com.qros.modules.analytics.service.AnalyticsService;
import com.qros.modules.auth.config.PasswordResetProperties;
import com.qros.modules.auth.repository.PasswordResetTokenRepository;
import com.qros.modules.auth.service.AccountLockoutService;
import com.qros.modules.auth.service.PasswordResetService;
import com.qros.modules.auth.service.RefreshTokenService;
import com.qros.modules.inventory.dto.request.InventoryItemRequest;
import com.qros.modules.inventory.dto.request.StockInRequest;
import com.qros.modules.inventory.mapper.InventoryItemMapper;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.modules.inventory.repository.OrderItemInventoryReservationRepository;
import com.qros.modules.inventory.repository.RecipeItemRepository;
import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.inventory.service.InventoryItemService;
import com.qros.modules.inventory.service.InventoryLookupService;
import com.qros.modules.inventory.service.StockMovementService;
import com.qros.modules.kitchen.service.KitchenService;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.order.listener.OrderCacheInvalidationService;
import com.qros.modules.order.mapper.OrderMapper;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderItemRepository;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.order.service.OrderAuditService;
import com.qros.modules.order.service.OrderInventoryReservationPort;
import com.qros.modules.order.service.OrderItemWorkflowService;
import com.qros.modules.order.service.OrderPricingService;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.order.service.OrderSettlementService;
import com.qros.modules.order.service.OrderStatusService;
import com.qros.modules.order.service.OrderTableSyncService;
import com.qros.modules.order.service.OrderValidator;
import com.qros.modules.payment.dto.internal.CashSettlementResult;
import com.qros.modules.payment.dto.internal.OnlinePaymentPreparation;
import com.qros.modules.payment.dto.request.PaymentCreateRequest;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.payment.model.enums.PaymentTransactionStatus;
import com.qros.modules.payment.repository.PaymentTransactionRepository;
import com.qros.modules.payment.service.PaymentCompletionService;
import com.qros.modules.payment.service.PaymentPersistenceService;
import com.qros.modules.promotion.model.Promotion;
import com.qros.modules.promotion.service.PromotionService;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.promotion.service.VoucherService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.service.TableLookupService;
import com.qros.modules.table.service.TableSessionService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.service.UserLookupService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.event.DomainEvents;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.ImageFileValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

class Wave2HardeningRegressionTest {

    @Test
    void issue024_happyHourSnapshotSurvivesAfterPromotionWindowEnds() {
        PromotionService promotionService = mock(PromotionService.class);
        SystemSettingsService settingsService = mock(SystemSettingsService.class);
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder().build());

        Promotion happyHour = Promotion.builder()
                .id(1L)
                .name("Happy Hour 15%")
                .discountPercent(BigDecimal.valueOf(15))
                .active(true)
                .build();
        when(promotionService.findBestActivePromotion()).thenReturn(Optional.of(happyHour));

        OrderPricingService pricingService = new OrderPricingService(
                mock(MenuCatalogLookupService.class),
                mock(VoucherCheckoutService.class),
                mock(OrderValidator.class),
                mock(TableSessionService.class),
                promotionService,
                settingsService);

        Order order = Order.builder().id(10L).build();
        order.addItem(OrderItem.builder()
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(50000))
                .lineTotal(BigDecimal.valueOf(100000))
                .status(OrderItemStatus.PENDING)
                .build());

        pricingService.recalculateOrderTotals(order);
        assertThat(order.getAppliedPromotionPercent()).isEqualByComparingTo("15.00");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("15000.00");

        // Happy Hour ends -> live promotion is now empty, but snapshot keeps the 15% discount!
        when(promotionService.findBestActivePromotion()).thenReturn(Optional.empty());
        pricingService.recalculateOrderTotals(order);
        assertThat(order.getAppliedPromotionPercent()).isEqualByComparingTo("15.00");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("15000.00");
        assertThat(order.getFinalAmount()).isEqualByComparingTo("85000.00");
    }

    @Test
    void issue024_settleCashRejectsShortTenderAndComputesChangeOnServer() {
        PaymentTransactionRepository txRepo = mock(PaymentTransactionRepository.class);
        OrderSettlementService settlementService = mock(OrderSettlementService.class);
        PaymentCompletionService completionService = mock(PaymentCompletionService.class);
        UserLookupService userLookup = mock(UserLookupService.class);
        SystemSettingsService settingsService = mock(SystemSettingsService.class);

        Order order = Order.builder()
                .id(20L)
                .status(OrderStatus.SERVING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(120000))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.valueOf(120000))
                .build();
        when(txRepo.findFirstByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(settlementService.prepareForPayment(eq(20L), any(), anyBoolean())).thenReturn(order);
        when(txRepo.saveAndFlush(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(txRepo.findPendingOnlineTransactionsByOrderId(anyLong())).thenReturn(List.of());

        PaymentPersistenceService service = new PaymentPersistenceService(
                txRepo, settlementService, completionService, userLookup, settingsService, mock(VoucherService.class));

        PaymentCreateRequest shortCash = new PaymentCreateRequest(
                20L, PaymentMethod.CASH, null, "idem-short", BigDecimal.valueOf(100000), BigDecimal.ZERO, false);
        assertThatThrownBy(() -> service.settleCash(shortCash, null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_PAYMENT_INVALID));

        PaymentCreateRequest spoofedChange = new PaymentCreateRequest(
                20L,
                PaymentMethod.CASH,
                null,
                "idem-valid",
                BigDecimal.valueOf(200000),
                BigDecimal.valueOf(999999),
                false);
        CashSettlementResult result = service.settleCash(spoofedChange, null);
        assertThat(result.transaction().getProviderPayload())
                .contains("\"cashReceived\":200000")
                .contains("\"changeAmount\":80000")
                .doesNotContain("999999");
    }

    @Test
    void new02_prepareOnlineCancelsSupersededPendingPayosTransactionAndReturnsIt() {
        PaymentTransactionRepository txRepo = mock(PaymentTransactionRepository.class);
        OrderSettlementService settlementService = mock(OrderSettlementService.class);
        SystemSettingsService settingsService = mock(SystemSettingsService.class);
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .onlinePaymentEnabled(true)
                        .paymentQrExpiresInMinutes(15)
                        .build());

        Order order = Order.builder()
                .id(33L)
                .status(OrderStatus.SERVING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(100000))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.valueOf(100000))
                .build();
        when(txRepo.findFirstByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(settlementService.prepareForPayment(eq(33L), any())).thenReturn(order);

        PaymentTransaction oldPending = PaymentTransaction.builder()
                .id(501L)
                .order(order)
                .paymentMethod(PaymentMethod.PAYOS)
                .status(PaymentTransactionStatus.PENDING)
                .amount(BigDecimal.valueOf(90000)) // different amount -> cannot be reused, must be superseded
                .gatewayOrderCode(33000123L)
                .expiresAt(AppTime.now().plusMinutes(10))
                .build();
        when(txRepo.findPendingOnlineTransactionsByOrderId(33L)).thenReturn(List.of(oldPending));
        when(txRepo.findFirstByOrderIdAndPaymentMethodAndStatusInOrderByCreatedAtDesc(
                        eq(33L), eq(PaymentMethod.PAYOS), any()))
                .thenReturn(Optional.of(oldPending));
        when(txRepo.saveAndFlush(any(PaymentTransaction.class))).thenAnswer(inv -> {
            PaymentTransaction tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(502L);
            }
            return tx;
        });

        PaymentPersistenceService service = new PaymentPersistenceService(
                txRepo,
                settlementService,
                mock(PaymentCompletionService.class),
                mock(UserLookupService.class),
                settingsService,
                mock(VoucherService.class));

        PaymentCreateRequest request =
                new PaymentCreateRequest(33L, PaymentMethod.PAYOS, null, "idem-new-qr", null, null, false);
        OnlinePaymentPreparation prep = service.prepareOnline(request);

        assertThat(prep.gatewayCallRequired()).isTrue();
        assertThat(oldPending.getStatus()).isEqualTo(PaymentTransactionStatus.CANCELLED);
        assertThat(prep.cancelledOnlineTransactions()).containsExactly(oldPending);
    }

    @Test
    void issue025_operatingHoursValidatorBlocksOutsideWindowIncludingOvernightSupport() {
        SystemSettingsService settingsService = mock(SystemSettingsService.class);
        OrderValidator validator = new OrderValidator(settingsService);

        LocalTime now = AppTime.now().toLocalTime();
        LocalTime openInFuture = now.plusHours(2);
        LocalTime closeInFuture = now.plusHours(4);
        if (!openInFuture.isBefore(closeInFuture)) {
            openInFuture = now.minusHours(4);
            closeInFuture = now.minusHours(2);
        }

        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .openingTime(openInFuture)
                        .closingTime(closeInFuture)
                        .build());

        assertThatThrownBy(validator::validateCustomerOperatingHours)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_INVALID_STATE));

        // Overnight window covering current time (open = now - 1h, close = now - 2h wraps around midnight)
        when(settingsService.getSettingsEntity())
                .thenReturn(SystemSettings.builder()
                        .openingTime(now.minusHours(1))
                        .closingTime(now.minusHours(2))
                        .build());
        validator.validateCustomerOperatingHours();
    }

    @Test
    void issue026_analyticsCapsDateRangeAt366DaysAndUsesStatsDashboardCache() throws NoSuchMethodException {
        AnalyticsService analyticsService = new AnalyticsService(
                mock(AnalyticsQueryRepository.class), mock(AnalyticsForecastService.class), mock(OrderService.class));

        Cacheable annotation = AnalyticsService.class
                .getMethod("getDashboardSummary", LocalDate.class, LocalDate.class)
                .getAnnotation(Cacheable.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(CacheNames.STATS_DASHBOARD);

        assertThatThrownBy(() -> analyticsService.getRevenueSeries(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    void issue028_batchKitchenItemUpdateSortsIdsAndEmitsSingleOrderChangeEvent() {
        OrderItemRepository itemRepo = mock(OrderItemRepository.class);
        OrderRepository orderRepo = mock(OrderRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        OrderStatusService statusService = mock(OrderStatusService.class);

        Order order = Order.builder().id(50L).status(OrderStatus.SERVING).build();
        OrderItem item1 = OrderItem.builder()
                .id(10L)
                .order(order)
                .status(OrderItemStatus.PENDING)
                .build();
        OrderItem item2 = OrderItem.builder()
                .id(20L)
                .order(order)
                .status(OrderItemStatus.PENDING)
                .build();
        order.addItem(item1);
        order.addItem(item2);
        when(itemRepo.findDetailByIdForUpdate(10L)).thenReturn(Optional.of(item1));
        when(itemRepo.findDetailByIdForUpdate(20L)).thenReturn(Optional.of(item2));
        when(orderRepo.findWorkflowByIdForUpdate(50L)).thenReturn(Optional.of(order));
        when(orderRepo.save(order)).thenReturn(order);

        OrderItemWorkflowService workflow = new OrderItemWorkflowService(
                itemRepo,
                orderRepo,
                mock(UserLookupService.class),
                mock(OrderInventoryReservationPort.class),
                mock(OrderPricingService.class),
                mock(OrderAuditService.class),
                statusService,
                mock(OrderTableSyncService.class),
                mock(OrderCacheInvalidationService.class),
                eventPublisher,
                mock(OrderMapper.class));

        // Pass unsorted IDs with duplicate
        workflow.updateBatchItemStatus(List.of(20L, 10L, 20L), OrderItemStatus.COOKING, 99L);

        assertThat(item1.getStatus()).isEqualTo(OrderItemStatus.COOKING);
        assertThat(item2.getStatus()).isEqualTo(OrderItemStatus.COOKING);
        verify(orderRepo, times(1)).findWorkflowByIdForUpdate(50L);
        verify(eventPublisher, times(1)).publishEvent(any(DomainEvents.OrderChangeEvent.class));
    }

    @Test
    void issue060_and_issue062_ocrAvoidsSubstringCollisionGuardsZeroRoundingAndDefersQuota() {
        ImageFileValidator validator = mock(ImageFileValidator.class);
        InventoryLookupService inventoryLookup = mock(InventoryLookupService.class);
        AiChatGateway gateway = mock(AiChatGateway.class);

        InventoryItem butter = InventoryItem.builder()
                .id(1L)
                .name("Bơ")
                .unit("kg")
                .active(true)
                .build();
        InventoryItem fish = InventoryItem.builder()
                .id(2L)
                .name("Cá")
                .unit("kg")
                .active(true)
                .build();
        InventoryItem beef = InventoryItem.builder()
                .id(3L)
                .name("Thịt bò Úc")
                .unit("kg")
                .active(true)
                .build();
        when(inventoryLookup.findActiveInventoryItemsOrdered()).thenReturn(List.of(butter, fish, beef));
        when(gateway.chatWithImage(any(), any(), any(), any())).thenReturn("""
                {
                  "supplierName": "Fresh Mart",
                  "invoiceNumber": "HD-99",
                  "warnings": [],
                  "lines": [
                    {"rawName": "Thịt bò Úc nhập khẩu", "quantity": 0.2, "unit": "g", "unitPrice": 1000, "lineTotal": 200},
                    {"rawName": "Cà chua bi", "quantity": 2, "unit": "kg", "unitPrice": 35000, "lineTotal": 70000}
                  ]
                }
                """);

        AiInvoiceOcrService ocrService = new AiInvoiceOcrService(
                validator,
                inventoryLookup,
                gateway,
                new AiPromptBuilder(),
                new JsonMapper(),
                new SimpleMeterRegistry());

        MockMultipartFile file = new MockMultipartFile("file", "inv.jpg", "image/jpeg", new byte[] {1, 2, 3});
        AtomicBoolean quotaCalled = new AtomicBoolean(false);
        AiInvoiceOcrResponse response = ocrService.parseInvoice(file, "vi", () -> quotaCalled.set(true));

        assertThat(quotaCalled.get()).isTrue();
        assertThat(response.lines()).hasSize(2);
        // "Thịt bò Úc nhập khẩu" matches "Thịt bò Úc" (NOT "Bơ") and 0.2g -> kg rounds to 0.000 => UNIT_MISMATCH
        assertThat(response.lines().get(0).matchedInventoryItemId()).isEqualTo(3L);
        assertThat(response.lines().get(0).matchStatus()).isEqualTo("UNIT_MISMATCH");
        assertThat(response.lines().get(0).unitMismatch()).isTrue();
        assertThat(response.lines().get(0).convertedQuantity()).isEqualByComparingTo("0.200");
        // "Cà chua bi" does NOT collide with "Cá"
        assertThat(response.lines().get(1).matchedInventoryItemId()).isNull();
        assertThat(response.lines().get(1).matchStatus()).isEqualTo("UNMATCHED");
    }

    @Test
    void issue062_noteTriageSkipsQuotaWhenNoNotesExistAndPromptIsolatesUntrustedData() {
        KitchenService kitchenService = mock(KitchenService.class);
        TableLookupService tableLookup = mock(TableLookupService.class);
        when(kitchenService.getKitchenOrders()).thenReturn(List.of());
        when(tableLookup.findServiceRequestsByStatuses(any(), any(), any())).thenReturn(List.of());

        AiNoteTriageService triageService = new AiNoteTriageService(
                kitchenService,
                tableLookup,
                mock(AiChatGateway.class),
                new AiPromptBuilder(),
                new JsonMapper(),
                new SimpleMeterRegistry());

        AtomicBoolean quotaCalled = new AtomicBoolean(false);
        triageService.triage(new AiNoteTriageRequest("vi"), () -> quotaCalled.set(true));
        assertThat(quotaCalled.get()).isFalse();

        AiPromptBuilder builder = new AiPromptBuilder();
        assertThat(builder.buildStaffPrompt("")).contains("<untrusted_customer_data>");
        assertThat(builder.buildNoteTriagePrompt("", "vi")).contains("<untrusted_customer_data>");
    }

    @Test
    void issue061_inventoryBlocksUnitChangeInUseAndStockInOnInactiveItem() {
        InventoryItemRepository itemRepo = mock(InventoryItemRepository.class);
        RecipeItemRepository recipeRepo = mock(RecipeItemRepository.class);
        OrderItemInventoryReservationRepository reservationRepo = mock(OrderItemInventoryReservationRepository.class);

        InventoryItemService service = new InventoryItemService(
                itemRepo,
                recipeRepo,
                reservationRepo,
                mock(InventoryItemMapper.class),
                mock(StockMovementService.class),
                mock(InventoryAvailabilityService.class),
                mock(ApplicationEventPublisher.class));

        InventoryItem usedItem = InventoryItem.builder()
                .id(10L)
                .name("Sugar")
                .unit("kg")
                .version(2L)
                .active(true)
                .reservedQuantity(BigDecimal.ZERO)
                .build();
        when(itemRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(usedItem));
        when(itemRepo.existsByNameIgnoreCaseAndIdNot("Sugar", 10L)).thenReturn(false);
        when(recipeRepo.existsByInventoryItem_Id(10L)).thenReturn(true);

        InventoryItemRequest changeUnitReq =
                new InventoryItemRequest("Sugar", "g", BigDecimal.ONE, BigDecimal.valueOf(20000), true, 2L);
        assertThatThrownBy(() -> service.update(10L, changeUnitReq))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVENTORY_ITEM_IN_USE));

        InventoryItem inactiveItem = InventoryItem.builder()
                .id(11L)
                .name("Old Item")
                .unit("kg")
                .active(false)
                .build();
        when(itemRepo.findByIdForUpdate(11L)).thenReturn(Optional.of(inactiveItem));

        assertThatThrownBy(() -> service.stockIn(11L, new StockInRequest(BigDecimal.ONE, "restock")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void issue064_passwordResetBlocksInactiveAndBannedUsers() {
        UserLookupService userLookup = mock(UserLookupService.class);
        PasswordResetTokenRepository tokenRepo = mock(PasswordResetTokenRepository.class);
        User banned = User.builder()
                .id(5L)
                .email("banned@qros.vn")
                .role(UserRole.STAFF)
                .status(UserStatus.BANNED)
                .build();
        when(userLookup.findByEmailIgnoreCase("banned@qros.vn")).thenReturn(Optional.of(banned));

        PasswordResetService service = new PasswordResetService(
                userLookup,
                tokenRepo,
                mock(com.qros.infrastructure.mail.EmailService.class),
                mock(com.qros.infrastructure.sms.SmsService.class),
                new PasswordResetProperties(),
                mock(Environment.class),
                mock(PasswordEncoder.class),
                mock(TransactionSideEffectService.class),
                mock(RefreshTokenService.class),
                mock(AccountLockoutService.class),
                mock(ApplicationEventPublisher.class));

        // Anti-enumeration: inactive/banned users throw EMAIL_NOT_FOUND without saving or sending a token
        assertThatThrownBy(() -> service.createPasswordResetToken("banned@qros.vn"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_FOUND));
        verify(tokenRepo, never()).save(any());
    }
}
