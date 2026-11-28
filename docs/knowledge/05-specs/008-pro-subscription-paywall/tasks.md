# Execution Tasks `008`: Hệ thống Nâng cấp Gói Pro & Paywall (Free vs Pro + PayOS VietQR)

- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD bắt buộc:**
  - Backend: `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify`
  - Frontend: `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Database Migration & Shared Contracts
- [x] Tạo migration `V1.27__Subscription_Plans_And_Invoices.sql` (`restaurant_subscriptions`, `subscription_invoices`, seed `id = 1`).
- [x] Tạo `com.qros.shared.subscription.*` (`PlanTier`, `PlanFeature`, `BillingCycle`, `SubscriptionStatus`, `RequiresPlanFeature`, ports & properties) và cập nhật `ErrorCode`, `ApiRoutes`, `CacheNames`, `DomainEvents`, `RedisConfig`, `CaffeineCacheConfig`, `application.yml`.

## Nhóm 2 — Backend Module `subscription`, Guards, PayOS Webhook & Tests
- [x] Triển khai `com.qros.modules.subscription` (entities, repositories, `SubscriptionPayosGateway`, `SubscriptionService`, DTOs, `SubscriptionController`) + `SecurityRoutes` / `SecurityConfig`.
- [x] Triển khai `SubscriptionFeatureInterceptor` + `WebMvcConfig`, gắn `@RequiresPlanFeature` lên toàn bộ AI endpoints (mở hoàn toàn thao tác kho/BOM thủ công ở `InventoryItemController` và `RecipeController` cho gói `FREE`), gắn limit check lên tạo bàn & nhân sự, đồng bộ `PublicSettingsResponse.aiCustomerAssistantEnabled` & `DomainEventListener`.
- [x] Định tuyến webhook PayOS trong `PayosWebhookController` qua `SubscriptionWebhookPort` (idempotent, 0 cycle).
- [x] Viết tests (`SubscriptionServiceTest`, `SubscriptionFeatureInterceptorTest`, cập nhật `CoreApiSecurityIntegrationTest`, `PayosWebhookControllerTest`) và chạy `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify` → pass 100%.

## Nhóm 3 — Frontend FSD (`SCR-28` & Soft Paywall) & Tests
- [x] Tạo `entities/subscription` (`subscriptionService`, `subscriptionQueries`, `subscriptionMutations`, `subscriptionHelpers`, `useSubscription`, barrel `index.js`), cập nhật `WebSocketInvalidator.jsx` và `errorMessages.js`.
- [x] Tạo `features/subscription-billing` (`ProBadge`, `UpgradeProModal`, `SubscriptionQrModal`, `BillingManagement`) và trang `pages/admin/BillingPage.jsx` + route `/admin/billing` + Sidebar.
- [x] Gắn Soft Paywall lên `SCR-18`, `SCR-10`, `SCR-17`, `SCR-20`, `SCR-12`, `SCR-27` và kiểm soát AI Chat trên `SCR-05`.
- [x] Viết `frontend/tests/unit/subscription.test.js`, đăng ký trong `run-tests.js`, và chạy `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build` → pass 100%.

## Nhóm 4 — Verification & Closure
- [x] Chuyển trạng thái `008-pro-subscription-paywall` trong `spec.md`, `tasks.md` và `KNOWLEDGE_HUB.md` sang `Archived` sau khi người dùng duyệt đồng bộ ngược tài liệu Knowledge (`F6.8`, `BR-37`, `US-035..036`).
