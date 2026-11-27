package com.qros.modules.subscription.service;

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
import com.qros.shared.cache.CacheHelper;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.event.DomainEvents.SubscriptionChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.subscription.BillingCycle;
import com.qros.shared.subscription.PlanFeature;
import com.qros.shared.subscription.PlanTier;
import com.qros.shared.subscription.SubscriptionEntitlementPort;
import com.qros.shared.subscription.SubscriptionProperties;
import com.qros.shared.subscription.SubscriptionResourceUsagePort;
import com.qros.shared.subscription.SubscriptionResourceUsagePort.ResourceType;
import com.qros.shared.subscription.SubscriptionStatus;
import com.qros.shared.subscription.SubscriptionWebhookPayload;
import com.qros.shared.subscription.SubscriptionWebhookPort;
import com.qros.shared.time.AppTime;
import com.qros.shared.transaction.TransactionExecutor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SubscriptionService implements SubscriptionEntitlementPort, SubscriptionWebhookPort {

    public static final long SUBSCRIPTION_ORDER_CODE_BASE = 900_000_000_000L;
    private static final int MONTHLY_DURATION_DAYS = 30;
    private static final int YEARLY_DURATION_DAYS = 365;
    private static final List<PlanFeature> PRO_FEATURES = List.of(PlanFeature.values());

    private final RestaurantSubscriptionRepository subscriptionRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final SubscriptionPayosGateway payosGateway;
    private final SubscriptionProperties properties;
    private final TransactionExecutor transactionExecutor;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;
    private final ObjectProvider<SubscriptionResourceUsagePort> usagePorts;

    public SubscriptionService(
            RestaurantSubscriptionRepository subscriptionRepository,
            SubscriptionInvoiceRepository invoiceRepository,
            SubscriptionPayosGateway payosGateway,
            SubscriptionProperties properties,
            TransactionExecutor transactionExecutor,
            ApplicationEventPublisher eventPublisher,
            CacheManager cacheManager,
            ObjectProvider<SubscriptionResourceUsagePort> usagePorts) {
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.payosGateway = payosGateway;
        this.properties = properties;
        this.transactionExecutor = transactionExecutor;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
        this.usagePorts = usagePorts;
    }

    @Override
    public boolean isFeatureEnabled(PlanFeature feature) {
        if (!properties.isEnforcementEnabled()) {
            return true;
        }
        RestaurantSubscription subscription = loadOrDefaultSubscriptionReadOnly();
        return isProCurrentlyActive(subscription, AppTime.now());
    }

    @Override
    public void assertFeatureEnabled(PlanFeature feature) {
        if (!isFeatureEnabled(feature)) {
            throw new BusinessException(
                    ErrorCode.PRO_PLAN_REQUIRED,
                    "This feature requires an active Pro subscription",
                    Map.of("feature", feature.name(), "requiredPlan", PlanTier.PRO.name()));
        }
    }

    @Override
    public void assertTableLimitNotExceeded(long currentActiveTables) {
        if (isFeatureEnabled(PlanFeature.UNLIMITED_RESOURCES)) {
            return;
        }
        int maxTables = Math.max(1, properties.getFreeMaxTables());
        if (currentActiveTables >= maxTables) {
            throw new BusinessException(
                    ErrorCode.PLAN_LIMIT_EXCEEDED,
                    "Free plan supports up to " + maxTables + " active tables. Upgrade to Pro for unlimited tables.",
                    Map.of("resource", "TABLES", "limit", maxTables, "requiredPlan", PlanTier.PRO.name()));
        }
    }

    @Override
    public void assertUserLimitNotExceeded(long currentActiveUsers) {
        if (isFeatureEnabled(PlanFeature.UNLIMITED_RESOURCES)) {
            return;
        }
        int maxUsers = Math.max(1, properties.getFreeMaxUsers());
        if (currentActiveUsers >= maxUsers) {
            throw new BusinessException(
                    ErrorCode.PLAN_LIMIT_EXCEEDED,
                    "Free plan supports up to "
                            + maxUsers
                            + " active staff accounts. Upgrade to Pro for unlimited staff accounts.",
                    Map.of("resource", "USERS", "limit", maxUsers, "requiredPlan", PlanTier.PRO.name()));
        }
    }

    public SubscriptionResponse getCurrentSubscription() {
        RestaurantSubscription subscription = refreshAndPersistExpiryIfNeeded();
        LocalDateTime now = AppTime.now();
        boolean proActive = isProCurrentlyActive(subscription, now);
        boolean effectivePro = !properties.isEnforcementEnabled() || proActive;

        long activeTables = countUsage(ResourceType.ACTIVE_TABLES);
        long activeUsers = countUsage(ResourceType.ACTIVE_USERS);

        return new SubscriptionResponse(
                proActive ? PlanTier.PRO : PlanTier.FREE,
                subscription.getStatus(),
                subscription.getBillingCycle(),
                subscription.getStartedAt(),
                subscription.getExpiresAt(),
                Boolean.TRUE.equals(subscription.getTrialUsed()),
                proActive,
                properties.isEnforcementEnabled(),
                effectivePro ? PRO_FEATURES : List.of(),
                new SubscriptionResponse.ResourceUsage(
                        activeTables,
                        effectivePro ? null : properties.getFreeMaxTables(),
                        activeUsers,
                        effectivePro ? null : properties.getFreeMaxUsers()));
    }

    public List<SubscriptionPlanResponse> getAvailablePlans() {
        return List.of(
                new SubscriptionPlanResponse(
                        "FREE",
                        PlanTier.FREE,
                        null,
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        "VND",
                        null,
                        properties.getFreeMaxTables(),
                        properties.getFreeMaxUsers(),
                        false,
                        List.of()),
                new SubscriptionPlanResponse(
                        "PRO_MONTHLY",
                        PlanTier.PRO,
                        BillingCycle.MONTHLY,
                        normalizeAmount(properties.getMonthlyPriceVnd()),
                        "VND",
                        MONTHLY_DURATION_DAYS,
                        null,
                        null,
                        false,
                        PRO_FEATURES),
                new SubscriptionPlanResponse(
                        "PRO_YEARLY",
                        PlanTier.PRO,
                        BillingCycle.YEARLY,
                        normalizeAmount(properties.getYearlyPriceVnd()),
                        "VND",
                        YEARLY_DURATION_DAYS,
                        null,
                        null,
                        true,
                        PRO_FEATURES));
    }

    public SubscriptionResponse activateTrial(@NonNull String actorEmail) {
        transactionExecutor.write(() -> {
            LocalDateTime now = AppTime.now();
            RestaurantSubscription sub = loadOrCreateSingletonForUpdate(now);

            if (Boolean.TRUE.equals(sub.getTrialUsed())) {
                throw new BusinessException(ErrorCode.SUBSCRIPTION_TRIAL_ALREADY_USED);
            }
            if (isProCurrentlyActive(sub, now)) {
                throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
            }

            int trialDays = Math.max(1, properties.getTrialDays());
            sub.setPlanTier(PlanTier.PRO);
            sub.setStatus(SubscriptionStatus.TRIALING);
            sub.setBillingCycle(BillingCycle.TRIAL);
            sub.setStartedAt(now);
            sub.setExpiresAt(now.plusDays(trialDays));
            sub.setTrialUsed(true);
            subscriptionRepository.saveAndFlush(sub);

            clearSubscriptionCaches();
            eventPublisher.publishEvent(
                    new SubscriptionChangeEvent(PlanTier.PRO.name(), SubscriptionStatus.TRIALING.name()));
            log.info("Activated {} day Pro trial for restaurant by {}", trialDays, actorEmail);
            return sub;
        });

        return getCurrentSubscription();
    }

    public SubscriptionInvoiceResponse createCheckout(
            @NonNull SubscriptionCheckoutRequest request, String idempotencyKey, @NonNull String actorEmail) {
        BillingCycle billingCycle = request.billingCycle();
        if (billingCycle != BillingCycle.MONTHLY && billingCycle != BillingCycle.YEARLY) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Billing cycle must be MONTHLY or YEARLY");
        }

        String normalizedKey = (idempotencyKey == null || idempotencyKey.isBlank())
                ? "sub-" + UUID.randomUUID()
                : idempotencyKey.trim();
        BigDecimal price = resolvePrice(billingCycle);

        CheckoutPreparation preparation = transactionExecutor.write(() -> {
            SubscriptionInvoice existing =
                    invoiceRepository.findByIdempotencyKey(normalizedKey).orElse(null);
            if (existing != null) {
                if (existing.getBillingCycle() != billingCycle) {
                    throw new BusinessException(
                            ErrorCode.IDEMPOTENCY_CONFLICT,
                            "Idempotency key was already used for a different billing cycle");
                }
                return new CheckoutPreparation(existing, false);
            }

            LocalDateTime now = AppTime.now();
            int expiresMinutes = Math.max(5, properties.getInvoiceExpiresInMinutes());
            String tempCode =
                    "SUB-TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            SubscriptionInvoice invoice = SubscriptionInvoice.builder()
                    .invoiceCode(tempCode)
                    .planTier(PlanTier.PRO)
                    .billingCycle(billingCycle)
                    .amount(price)
                    .currency("VND")
                    .status(SubscriptionInvoiceStatus.PENDING)
                    .idempotencyKey(normalizedKey)
                    .createdByEmail(actorEmail)
                    .expiresAt(now.plusMinutes(expiresMinutes))
                    .build();

            SubscriptionInvoice saved = invoiceRepository.saveAndFlush(invoice);
            saved.setGatewayOrderCode(SUBSCRIPTION_ORDER_CODE_BASE + saved.getId());
            saved.setInvoiceCode("SUB-" + String.format("%06d", saved.getId()));
            SubscriptionInvoice finalized = invoiceRepository.saveAndFlush(saved);
            return new CheckoutPreparation(finalized, true);
        });

        SubscriptionInvoice invoice = preparation.invoice();
        if (!preparation.newlyCreated()) {
            return toInvoiceResponse(invoice);
        }

        try {
            SubscriptionPayosGateway.GatewayLinkResult link =
                    payosGateway.createPaymentLink(invoice, request.returnPath());
            SubscriptionInvoice updated = transactionExecutor.write(() -> {
                SubscriptionInvoice locked = invoiceRepository
                        .findByIdForUpdate(invoice.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_INVOICE_NOT_FOUND));
                locked.setCheckoutUrl(link.checkoutUrl());
                locked.setQrCode(link.qrCode());
                locked.setPaymentLinkId(link.paymentLinkId());
                locked.setProviderPayload(link.providerPayload());
                return invoiceRepository.saveAndFlush(locked);
            });
            return toInvoiceResponse(updated);
        } catch (RuntimeException ex) {
            transactionExecutor.write(() -> {
                invoiceRepository.findByIdForUpdate(invoice.getId()).ifPresent(locked -> {
                    if (locked.getStatus() == SubscriptionInvoiceStatus.PENDING) {
                        locked.setStatus(SubscriptionInvoiceStatus.CANCELLED);
                        invoiceRepository.saveAndFlush(locked);
                    }
                });
                return null;
            });
            throw ex;
        }
    }

    @Override
    public boolean tryHandleWebhook(SubscriptionWebhookPayload payload) {
        if (payload == null
                || payload.gatewayOrderCode() == null
                || payload.gatewayOrderCode() < SUBSCRIPTION_ORDER_CODE_BASE) {
            return false;
        }

        return transactionExecutor.write(() -> {
            SubscriptionInvoice invoice = invoiceRepository
                    .findByGatewayOrderCodeForUpdate(payload.gatewayOrderCode())
                    .orElse(null);
            if (invoice == null) {
                return false;
            }
            if (payload.amount() == null || payload.amount().compareTo(invoice.getAmount()) < 0) {
                throw new BusinessException(
                        ErrorCode.PAYMENT_WEBHOOK_INVALID, "Webhook amount does not cover subscription invoice amount");
            }
            settleInvoiceAsPaidLocked(invoice, payload.externalReference(), payload.providerPayload());
            return true;
        });
    }

    public SubscriptionInvoiceResponse syncInvoiceStatus(@NonNull Long invoiceId) {
        SubscriptionInvoice snapshot = invoiceRepository
                .findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_INVOICE_NOT_FOUND));

        if (snapshot.getStatus() != SubscriptionInvoiceStatus.PENDING || snapshot.getGatewayOrderCode() == null) {
            return toInvoiceResponse(snapshot);
        }

        SubscriptionPayosGateway.GatewayStatusResult statusResult = payosGateway.getPaymentStatus(snapshot);

        SubscriptionInvoice updated = transactionExecutor.write(() -> {
            SubscriptionInvoice locked = invoiceRepository
                    .findByIdForUpdate(invoiceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_INVOICE_NOT_FOUND));

            if (locked.getStatus() != SubscriptionInvoiceStatus.PENDING) {
                return locked;
            }

            if (statusResult.status() == SubscriptionInvoiceStatus.PAID) {
                return settleInvoiceAsPaidLocked(
                        locked, statusResult.externalReference(), statusResult.providerPayload());
            }

            if (statusResult.status() == SubscriptionInvoiceStatus.CANCELLED
                    || statusResult.status() == SubscriptionInvoiceStatus.EXPIRED) {
                locked.setStatus(statusResult.status());
                locked.setProviderPayload(statusResult.providerPayload());
                return invoiceRepository.saveAndFlush(locked);
            }

            if (locked.getExpiresAt() != null && !locked.getExpiresAt().isAfter(AppTime.now())) {
                locked.setStatus(SubscriptionInvoiceStatus.EXPIRED);
                return invoiceRepository.saveAndFlush(locked);
            }

            return locked;
        });

        return toInvoiceResponse(updated);
    }

    public SubscriptionInvoiceResponse cancelInvoice(@NonNull Long invoiceId) {
        SubscriptionInvoice snapshot = invoiceRepository
                .findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_INVOICE_NOT_FOUND));

        if (snapshot.getStatus() != SubscriptionInvoiceStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.SUBSCRIPTION_INVOICE_INVALID_STATE,
                    "Only pending subscription invoices can be cancelled");
        }

        if (snapshot.getGatewayOrderCode() != null) {
            payosGateway.cancelPayment(snapshot, "Cancelled by restaurant admin");
        }

        SubscriptionInvoice updated = transactionExecutor.write(() -> {
            SubscriptionInvoice locked = invoiceRepository
                    .findByIdForUpdate(invoiceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_INVOICE_NOT_FOUND));
            if (locked.getStatus() != SubscriptionInvoiceStatus.PENDING) {
                throw new BusinessException(
                        ErrorCode.SUBSCRIPTION_INVOICE_INVALID_STATE,
                        "Only pending subscription invoices can be cancelled");
            }
            locked.setStatus(SubscriptionInvoiceStatus.CANCELLED);
            return invoiceRepository.saveAndFlush(locked);
        });

        return toInvoiceResponse(updated);
    }

    public List<SubscriptionInvoiceResponse> listInvoices() {
        return invoiceRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toInvoiceResponse)
                .toList();
    }

    private SubscriptionInvoice settleInvoiceAsPaidLocked(
            SubscriptionInvoice invoice, String externalReference, String providerPayload) {
        if (invoice.getStatus() == SubscriptionInvoiceStatus.PAID) {
            return invoice;
        }

        LocalDateTime now = AppTime.now();
        invoice.setStatus(SubscriptionInvoiceStatus.PAID);
        invoice.setPaidAt(now);
        if (externalReference != null && !externalReference.isBlank()) {
            invoice.setExternalReference(externalReference);
        }
        if (providerPayload != null && !providerPayload.isBlank()) {
            invoice.setProviderPayload(providerPayload);
        }
        SubscriptionInvoice savedInvoice = invoiceRepository.saveAndFlush(invoice);

        RestaurantSubscription sub = loadOrCreateSingletonForUpdate(now);
        int durationDays =
                invoice.getBillingCycle() == BillingCycle.YEARLY ? YEARLY_DURATION_DAYS : MONTHLY_DURATION_DAYS;
        boolean currentlyPro = isProCurrentlyActive(sub, now);
        LocalDateTime baseExpiry = (currentlyPro && sub.getExpiresAt() != null) ? sub.getExpiresAt() : now;

        if (!currentlyPro) {
            sub.setStartedAt(now);
        }
        sub.setPlanTier(PlanTier.PRO);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setBillingCycle(invoice.getBillingCycle());
        sub.setExpiresAt(baseExpiry.plusDays(durationDays));
        subscriptionRepository.saveAndFlush(sub);

        clearSubscriptionCaches();
        eventPublisher.publishEvent(new SubscriptionChangeEvent(PlanTier.PRO.name(), SubscriptionStatus.ACTIVE.name()));
        log.info(
                "Settled subscription invoice {} ({}) -> PRO expiresAt={}",
                savedInvoice.getInvoiceCode(),
                savedInvoice.getBillingCycle(),
                sub.getExpiresAt());
        return savedInvoice;
    }

    private RestaurantSubscription refreshAndPersistExpiryIfNeeded() {
        RestaurantSubscription current = loadOrDefaultSubscriptionReadOnly();
        LocalDateTime now = AppTime.now();
        if (current.getPlanTier() == PlanTier.PRO
                && current.getExpiresAt() != null
                && !current.getExpiresAt().isAfter(now)) {
            return transactionExecutor.write(() -> {
                RestaurantSubscription locked = loadOrCreateSingletonForUpdate(now);
                if (locked.getPlanTier() == PlanTier.PRO
                        && locked.getExpiresAt() != null
                        && !locked.getExpiresAt().isAfter(now)) {
                    locked.setPlanTier(PlanTier.FREE);
                    locked.setStatus(SubscriptionStatus.EXPIRED);
                    RestaurantSubscription saved = subscriptionRepository.saveAndFlush(locked);
                    clearSubscriptionCaches();
                    eventPublisher.publishEvent(
                            new SubscriptionChangeEvent(PlanTier.FREE.name(), SubscriptionStatus.EXPIRED.name()));
                    return saved;
                }
                return locked;
            });
        }
        return current;
    }

    private RestaurantSubscription loadOrDefaultSubscriptionReadOnly() {
        return subscriptionRepository
                .findById(RestaurantSubscription.SINGLETON_ID)
                .orElseGet(this::defaultFreeSubscription);
    }

    private RestaurantSubscription loadOrCreateSingletonForUpdate(LocalDateTime now) {
        return subscriptionRepository
                .findByIdForUpdate(RestaurantSubscription.SINGLETON_ID)
                .orElseGet(() -> subscriptionRepository.saveAndFlush(RestaurantSubscription.builder()
                        .id(RestaurantSubscription.SINGLETON_ID)
                        .planTier(PlanTier.FREE)
                        .status(SubscriptionStatus.ACTIVE)
                        .startedAt(now)
                        .trialUsed(false)
                        .build()));
    }

    private RestaurantSubscription defaultFreeSubscription() {
        return RestaurantSubscription.builder()
                .id(RestaurantSubscription.SINGLETON_ID)
                .planTier(PlanTier.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .trialUsed(false)
                .build();
    }

    private boolean isProCurrentlyActive(RestaurantSubscription subscription, LocalDateTime now) {
        if (subscription == null || subscription.getPlanTier() != PlanTier.PRO) {
            return false;
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                && subscription.getStatus() != SubscriptionStatus.TRIALING) {
            return false;
        }
        return subscription.getExpiresAt() == null
                || subscription.getExpiresAt().isAfter(now);
    }

    private BigDecimal resolvePrice(BillingCycle cycle) {
        BigDecimal raw =
                cycle == BillingCycle.YEARLY ? properties.getYearlyPriceVnd() : properties.getMonthlyPriceVnd();
        return normalizeAmount(raw);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        BigDecimal safe = value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.valueOf(2_000);
        return safe.setScale(2, RoundingMode.HALF_UP);
    }

    private long countUsage(ResourceType resourceType) {
        if (usagePorts == null) {
            return 0L;
        }
        return usagePorts.stream()
                .filter(port -> port.getResourceType() == resourceType)
                .mapToLong(SubscriptionResourceUsagePort::countActive)
                .sum();
    }

    private void clearSubscriptionCaches() {
        CacheHelper.clear(cacheManager, CacheNames.SUBSCRIPTION, CacheNames.SETTINGS);
    }

    private SubscriptionInvoiceResponse toInvoiceResponse(SubscriptionInvoice invoice) {
        SubscriptionInvoiceStatus effectiveStatus = invoice.getStatus();
        if (effectiveStatus == SubscriptionInvoiceStatus.PENDING
                && invoice.getExpiresAt() != null
                && !invoice.getExpiresAt().isAfter(AppTime.now())) {
            effectiveStatus = SubscriptionInvoiceStatus.EXPIRED;
        }
        return new SubscriptionInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getPlanTier(),
                invoice.getBillingCycle(),
                invoice.getAmount(),
                invoice.getCurrency(),
                effectiveStatus,
                invoice.getGatewayOrderCode(),
                invoice.getPaymentLinkId(),
                invoice.getCheckoutUrl(),
                invoice.getQrCode(),
                invoice.getCreatedByEmail(),
                invoice.getPaidAt(),
                invoice.getExpiresAt(),
                invoice.getCreatedAt());
    }

    private record CheckoutPreparation(SubscriptionInvoice invoice, boolean newlyCreated) {}
}
