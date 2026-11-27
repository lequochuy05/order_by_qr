# Execution Plan `008`: Hệ thống Nâng cấp Gói Pro & Paywall (Free vs Pro + PayOS VietQR)

- **Spec:** [spec.md](./spec.md)
- **Tuân thủ Hiến pháp (`CONSTITUTION.md`):**
  - 0 vòng phụ thuộc giữa 16 module và 0 cross-module repository imports (`BackendDependencyRulesTest`).
  - Thông điệp lỗi backend 100% tiếng Anh (`BackendMessageLanguageTest`).
  - Chỉ thêm Flyway migration mới `V1.27__Subscription_Plans_And_Invoices.sql`.
  - Frontend tuân thủ chặt chẽ FSD (`app → pages → widgets → features → entities → shared`), Public API barrels, Design Tokens 2 tầng, Light/Dark WCAG AA, song ngữ `VI`/`EN`.

## 1. Database & Shared Contracts
- `backend/src/main/resources/db/migration/V1.27__Subscription_Plans_And_Invoices.sql` (`restaurant_subscriptions`, `subscription_invoices`, seed singleton `id = 1`).
- `com.qros.shared.subscription.*`: `PlanTier`, `PlanFeature`, `BillingCycle`, `SubscriptionStatus`, `RequiresPlanFeature`, `SubscriptionEntitlementPort`, `SubscriptionUsagePort`, `SubscriptionWebhookPort`, `SubscriptionProperties`.
- Cập nhật `ErrorCode.java`, `ApiRoutes.java`, `CacheNames.java`, `DomainEvents.java`, `RedisConfig.java`, `CaffeineCacheConfig.java`, `application.yml`.

## 2. Backend Module `subscription`, Guards & Webhook Routing
- `com.qros.modules.subscription.*`:
  - `model/`: `RestaurantSubscription`, `SubscriptionInvoice`, `SubscriptionInvoiceStatus`
  - `repository/`: `RestaurantSubscriptionRepository`, `SubscriptionInvoiceRepository`
  - `gateway/`: `SubscriptionPayosGateway`
  - `service/`: `SubscriptionService` (implements `SubscriptionEntitlementPort` & `SubscriptionWebhookPort`)
  - `dto/`: `SubscriptionCheckoutRequest`, `SubscriptionResponse`, `SubscriptionPlanResponse`, `SubscriptionInvoiceResponse`
  - `controller/`: `SubscriptionController`
- `com.qros.core.config.SubscriptionFeatureInterceptor` + `WebMvcConfig` (chặn `@RequiresPlanFeature`).
- Gắn `@RequiresPlanFeature` vào `AiInsightsController`, `AiInventoryController`, `AiMerchandisingController`, `AiStaffController#analyzeNotes`, `AiChatController`, `InventoryItemController` (write endpoints), `RecipeController#updateRecipe`.
- Gắn `SubscriptionUsagePort` cho `modules.table` và `modules.user` + check limit khi tạo mới bàn/tài khoản ở gói `FREE`.
- Cập nhật `PayosWebhookController` định tuyến `subscriptionWebhookPort.tryHandleWebhook(result)` trước `paymentService.confirmPaymentFromWebhook(result)`.
- Cập nhật `PublicSettingsResponse` / `SystemSettingsService` bổ sung cờ `aiCustomerAssistantEnabled` + `DomainEventListener` xử lý `SubscriptionChangeEvent`.

## 3. Frontend FSD (`SCR-28` & Soft Paywall)
- `frontend/src/entities/subscription/`: `subscriptionService.js`, `subscriptionQueries.js`, `subscriptionMutations.js`, `subscriptionHelpers.js`, `useSubscription.js`, `index.js`.
- `frontend/src/features/subscription-billing/`: `ProBadge.jsx`, `UpgradeProModal.jsx`, `SubscriptionQrModal.jsx`, `BillingManagement.jsx`, `index.js`.
- `frontend/src/pages/admin/BillingPage.jsx`, route `/admin/billing`, Sidebar menu item + plan pill.
- Gắn Soft Paywall trên `SCR-18`, `SCR-10`, `SCR-17`, `SCR-20`, `SCR-12`, `SCR-27`, ẩn AI Chat trên `SCR-05` khi `aiCustomerAssistantEnabled === false`.
- Cập nhật `WebSocketInvalidator.jsx` invalidate `['subscription']` khi nhận event trên `/topic/settings`, và `errorMessages.js` dịch `PRO_PLAN_REQUIRED`, `PLAN_LIMIT_EXCEEDED`, `SUBSCRIPTION_TRIAL_ALREADY_USED`.
