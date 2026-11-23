# Technical Plan `001` (Baseline — Dựng ngược từ code): Customer Self-Checkout

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Kiến trúc & Hợp đồng giao tiếp thực tế trong codebase
- **Database / Flyway:** `backend/src/main/resources/db/migration/V1.22__Customer_Self_Checkout_Toggle.sql` — thêm cột `customer_self_checkout_enabled BOOLEAN NOT NULL DEFAULT false` vào `system_settings`.
- **REST API (Public, xác thực qua `tableCode` + `sessionToken`):**
  - `POST /api/v1/public/tables/{tableCode}/self-checkout` → khởi tạo giao dịch PayOS VietQR (`PaymentCreateResponse`)
  - `POST /api/v1/public/tables/{tableCode}/self-checkout/{transactionId}/cancel` → hủy link QR đang chờ
  - `GET /api/v1/public/tables/{tableCode}/self-checkout/{transactionId}?sessionToken=...` → đồng bộ trạng thái giao dịch
- **Security & Rate Limit:**
  - `SecurityRoutes.java:32-33` mở public cho 2 route `POST` và route `GET` tương ứng.
  - `application.yml:157-158` cấu hình rate-limit scope `public:self-checkout` trên `/api/v1/public/tables/*/self-checkout/**`.
- **WebSocket STOMP:** Kế thừa `/topic/tables` (khách nhận trạng thái bàn đóng sau webhook PayOS) và `/topic/settings` (Admin bật/tắt công tắc tại `SCR-26`).

## 2. Bản đồ File thực tế trong codebase
### Backend (`backend/`)
- Migration: `backend/src/main/resources/db/migration/V1.22__Customer_Self_Checkout_Toggle.sql`
- Config & Security: `backend/src/main/resources/application.yml`, `backend/src/main/java/com/qros/core/config/security/SecurityRoutes.java`
- Payment & Order: `PublicSelfCheckoutController.java`, `CustomerSelfCheckoutService.java`, `CustomerSelfCheckoutCreateRequest.java`, `CustomerSelfCheckoutCancelRequest.java`, `OrderSettlementService.java`
- Settings: `SystemSettings.java`, `SystemSettingsUpdateRequest.java`, `SystemSettingsResponse.java`, `PublicSettingsResponse.java`, `SystemSettingsMapper.java`, `SystemSettingsService.java`
- Tests: `CustomerSelfCheckoutServiceTest.java`, `SystemSettingsServiceTest.java`

### Frontend (`frontend/`)
- API Client: `frontend/src/entities/public-menu/api/publicMenuService.js`
- Customer Menu (`SCR-05`): `restaurantSettings.js`, `selfCheckoutHelpers.js`, `useCustomerSelfCheckout.js`, `CurrentOrderBanner.jsx`, `CurrentOrderSheet.jsx`, `CustomerMenu.jsx`
- Settings (`SCR-26`): `settingsConstants.js`, `settingsUtils.js`, `tabs/PaymentSettingsTab.jsx`
- Shared Error Mapping: `frontend/src/shared/lib/errorMessages.js`
- Tests: `frontend/tests/unit/selfCheckout.test.js`, `frontend/tests/unit/restaurantSettings.test.js`

## 3. Đối chiếu với `CONSTITUTION.md`
- **Auto-Enforced:** `CustomerSelfCheckoutServiceTest`, `SystemSettingsServiceTest`, `selfCheckout.test.js`, `restaurantSettings.test.js`, `BackendDependencyRulesTest`, `BackendMessageLanguageTest`, `spotless:check`, `eslint`, `vite build`.
- **Manual Review:** Xác nhận migration `V1.22` chỉ thêm cột mới với `DEFAULT false`, UI dùng `shared/ui/` + i18n copy + token màu chuẩn, và luôn kiểm tra `sessionToken` gắn đúng `order` của `tableCode`.
