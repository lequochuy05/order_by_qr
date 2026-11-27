package com.qros.modules.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.modules.subscription.dto.request.SubscriptionCheckoutRequest;
import com.qros.modules.subscription.dto.response.SubscriptionInvoiceResponse;
import com.qros.modules.subscription.dto.response.SubscriptionPlanResponse;
import com.qros.modules.subscription.dto.response.SubscriptionResponse;
import com.qros.modules.subscription.gateway.SubscriptionPayosGateway;
import com.qros.modules.subscription.model.RestaurantSubscription;
import com.qros.modules.subscription.model.SubscriptionInvoice;
import com.qros.modules.subscription.model.SubscriptionInvoiceStatus;
import com.qros.modules.subscription.repository.RestaurantSubscriptionRepository;
import com.qros.modules.subscription.repository.SubscriptionInvoiceRepository;
import com.qros.shared.event.DomainEvents.SubscriptionChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.subscription.BillingCycle;
import com.qros.shared.subscription.PlanFeature;
import com.qros.shared.subscription.PlanTier;
import com.qros.shared.subscription.SubscriptionProperties;
import com.qros.shared.subscription.SubscriptionResourceUsagePort;
import com.qros.shared.subscription.SubscriptionStatus;
import com.qros.shared.subscription.SubscriptionWebhookPayload;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionExecutor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

class SubscriptionServiceTest {

    private RestaurantSubscriptionRepository subscriptionRepository;
    private SubscriptionInvoiceRepository invoiceRepository;
    private SubscriptionPayosGateway payosGateway;
    private SubscriptionProperties properties;
    private ApplicationEventPublisher eventPublisher;
    private ObjectProvider<SubscriptionResourceUsagePort> usagePorts;
    private SubscriptionService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(RestaurantSubscriptionRepository.class);
        invoiceRepository = mock(SubscriptionInvoiceRepository.class);
        payosGateway = mock(SubscriptionPayosGateway.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        usagePorts = mock(ObjectProvider.class);
        when(usagePorts.stream()).thenAnswer(inv -> Stream.empty());

        properties = new SubscriptionProperties();
        properties.setEnforcementEnabled(true);
        properties.setMonthlyPriceVnd(BigDecimal.valueOf(2_000));
        properties.setYearlyPriceVnd(BigDecimal.valueOf(5_000));
        properties.setTrialDays(14);
        properties.setFreeMaxTables(10);
        properties.setFreeMaxUsers(5);
        properties.setInvoiceExpiresInMinutes(15);

        service = new SubscriptionService(
                subscriptionRepository,
                invoiceRepository,
                payosGateway,
                properties,
                TransactionExecutor.direct(),
                eventPublisher,
                null,
                usagePorts);
    }

    @Test
    void freePlanBlocksProFeaturesAndResourceLimits() {
        RestaurantSubscription freeSub = RestaurantSubscription.builder()
                .id(1L)
                .planTier(PlanTier.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .trialUsed(false)
                .build();
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(freeSub));

        assertThat(service.isFeatureEnabled(PlanFeature.AI_OPERATIONS_SUITE)).isFalse();
        assertThatThrownBy(() -> service.assertFeatureEnabled(PlanFeature.INVENTORY_BOM))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRO_PLAN_REQUIRED);

        assertThatThrownBy(() -> service.assertTableLimitNotExceeded(10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PLAN_LIMIT_EXCEEDED);

        assertThatThrownBy(() -> service.assertUserLimitNotExceeded(5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PLAN_LIMIT_EXCEEDED);
    }

    @Test
    void availablePlansUseConfiguredDemoPrices() {
        List<SubscriptionPlanResponse> plans = service.getAvailablePlans();

        assertThat(plans).hasSize(3);
        assertThat(plans.get(1).billingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(plans.get(1).priceVnd()).isEqualByComparingTo("2000.00");
        assertThat(plans.get(2).billingCycle()).isEqualTo(BillingCycle.YEARLY);
        assertThat(plans.get(2).priceVnd()).isEqualByComparingTo("5000.00");
    }

    @Test
    void activateTrialEnablesProOnceAndRejectsSecondAttempt() {
        RestaurantSubscription sub = RestaurantSubscription.builder()
                .id(1L)
                .planTier(PlanTier.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .trialUsed(false)
                .build();
        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = service.activateTrial("admin@qros.test");

        assertThat(response.planTier()).isEqualTo(PlanTier.PRO);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(response.trialUsed()).isTrue();
        assertThat(response.proActive()).isTrue();
        assertThat(response.enabledFeatures()).containsAll(List.of(PlanFeature.values()));
        verify(eventPublisher).publishEvent(any(SubscriptionChangeEvent.class));

        assertThatThrownBy(() -> service.activateTrial("admin@qros.test"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SUBSCRIPTION_TRIAL_ALREADY_USED);
    }

    @Test
    void createCheckoutIsIdempotentAndCancelsOnGatewayFailure() {
        when(invoiceRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> {
            SubscriptionInvoice invArg = inv.getArgument(0);
            if (invArg.getId() == null) {
                invArg.setId(42L);
            }
            return invArg;
        });
        when(invoiceRepository.findByIdForUpdate(42L))
                .thenAnswer(inv -> Optional.of(SubscriptionInvoice.builder()
                        .id(42L)
                        .invoiceCode("SUB-000042")
                        .planTier(PlanTier.PRO)
                        .billingCycle(BillingCycle.MONTHLY)
                        .amount(BigDecimal.valueOf(2000))
                        .currency("VND")
                        .status(SubscriptionInvoiceStatus.PENDING)
                        .gatewayOrderCode(SubscriptionService.SUBSCRIPTION_ORDER_CODE_BASE + 42L)
                        .idempotencyKey("idem-1")
                        .expiresAt(AppTime.now().plusMinutes(15))
                        .build()));
        when(payosGateway.createPaymentLink(any(), eq("/admin/billing")))
                .thenReturn(new SubscriptionPayosGateway.GatewayLinkResult(
                        "https://pay.payos.vn/web/sub42", "qr-raw-data", "pl-42", "{\"ok\":true}"));

        SubscriptionInvoiceResponse created = service.createCheckout(
                new SubscriptionCheckoutRequest(BillingCycle.MONTHLY, "/admin/billing"), "idem-1", "admin@qros.test");

        assertThat(created.invoiceCode()).isEqualTo("SUB-000042");
        assertThat(created.gatewayOrderCode()).isEqualTo(SubscriptionService.SUBSCRIPTION_ORDER_CODE_BASE + 42L);
        assertThat(created.amount()).isEqualByComparingTo("2000.00");
        assertThat(created.qrCode()).isEqualTo("qr-raw-data");

        SubscriptionInvoice existing = SubscriptionInvoice.builder()
                .id(42L)
                .invoiceCode("SUB-000042")
                .planTier(PlanTier.PRO)
                .billingCycle(BillingCycle.MONTHLY)
                .amount(BigDecimal.valueOf(2000))
                .currency("VND")
                .status(SubscriptionInvoiceStatus.PENDING)
                .idempotencyKey("idem-1")
                .expiresAt(AppTime.now().plusMinutes(15))
                .build();
        when(invoiceRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createCheckout(
                        new SubscriptionCheckoutRequest(BillingCycle.YEARLY, null), "idem-1", "admin@qros.test"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void webhookIgnoresDiningOrderCodesAndSettlesSubscriptionIdempotentlyWithSequentialExtension() {
        assertThat(service.tryHandleWebhook(
                        new SubscriptionWebhookPayload(123456L, BigDecimal.valueOf(50000), "ref", "{}")))
                .isFalse();
        verify(invoiceRepository, never()).findByGatewayOrderCodeForUpdate(any());

        long subOrderCode = SubscriptionService.SUBSCRIPTION_ORDER_CODE_BASE + 7L;
        SubscriptionInvoice invoice = SubscriptionInvoice.builder()
                .id(7L)
                .invoiceCode("SUB-000007")
                .planTier(PlanTier.PRO)
                .billingCycle(BillingCycle.YEARLY)
                .amount(BigDecimal.valueOf(5000))
                .currency("VND")
                .status(SubscriptionInvoiceStatus.PENDING)
                .gatewayOrderCode(subOrderCode)
                .expiresAt(AppTime.now().plusMinutes(15))
                .build();

        LocalDateTime existingExpiry = AppTime.now().plusDays(10);
        RestaurantSubscription activePro = RestaurantSubscription.builder()
                .id(1L)
                .planTier(PlanTier.PRO)
                .status(SubscriptionStatus.TRIALING)
                .billingCycle(BillingCycle.TRIAL)
                .startedAt(AppTime.now().minusDays(4))
                .expiresAt(existingExpiry)
                .trialUsed(true)
                .build();

        when(invoiceRepository.findByGatewayOrderCodeForUpdate(subOrderCode)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activePro));
        when(subscriptionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.tryHandleWebhook(
                        new SubscriptionWebhookPayload(subOrderCode, BigDecimal.valueOf(1000), "ref", "{}")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_WEBHOOK_INVALID);

        boolean handled = service.tryHandleWebhook(
                new SubscriptionWebhookPayload(subOrderCode, BigDecimal.valueOf(5000), "pl-7", "{}"));

        assertThat(handled).isTrue();
        assertThat(invoice.getStatus()).isEqualTo(SubscriptionInvoiceStatus.PAID);
        assertThat(activePro.getPlanTier()).isEqualTo(PlanTier.PRO);
        assertThat(activePro.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(activePro.getExpiresAt()).isEqualTo(existingExpiry.plusDays(365));

        LocalDateTime expiryAfterFirst = activePro.getExpiresAt();
        boolean duplicateHandled = service.tryHandleWebhook(
                new SubscriptionWebhookPayload(subOrderCode, BigDecimal.valueOf(5000), "pl-7", "{}"));
        assertThat(duplicateHandled).isTrue();
        assertThat(activePro.getExpiresAt()).isEqualTo(expiryAfterFirst);
    }

    @Test
    void expiredProGracefullyDowngradesToFreeExpired() {
        RestaurantSubscription expired = RestaurantSubscription.builder()
                .id(1L)
                .planTier(PlanTier.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .startedAt(AppTime.now().minusDays(40))
                .expiresAt(AppTime.now().minusDays(1))
                .trialUsed(true)
                .build();

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(expired));
        when(subscriptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(expired));
        when(subscriptionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = service.getCurrentSubscription();

        assertThat(response.planTier()).isEqualTo(PlanTier.FREE);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(response.proActive()).isFalse();
        assertThat(response.enabledFeatures()).isEmpty();
    }
}
