# Execution Tasks `001` (Baseline — Dựng ngược từ code): Customer Self-Checkout

- **Type:** `Baseline Verification Checklist` *(dựng ngược từ code có sẵn; các mục đã có trong codebase được đối chiếu trực tiếp, mục Sync-Back giữ mở chờ duyệt)*
- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD (khớp CI):** `mvn -f backend/pom.xml --batch-mode verify` · `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Database & Migration (Đã có trong code — đã đối chiếu file)
- [x] Migration `V1.22__Customer_Self_Checkout_Toggle.sql` thêm cột `customer_self_checkout_enabled BOOLEAN NOT NULL DEFAULT false` vào `system_settings`
- [x] Ánh xạ entity/DTO/mapper tại `SystemSettings.java`, `SystemSettingsResponse.java`, `PublicSettingsResponse.java`, `SystemSettingsMapper.java`

## Nhóm 2 — Backend Implementation & Tests (Đã có trong code — đã đối chiếu file)
- [x] Triển khai `PublicSelfCheckoutController`, `CustomerSelfCheckoutService`, `OrderSettlementService.resolveSessionOrderForSelfCheckout`
- [x] Cấu hình public security (`SecurityRoutes.java`) và rate limit `public:self-checkout` (`application.yml`)
- [x] Có sẵn unit tests: `CustomerSelfCheckoutServiceTest.java`, `SystemSettingsServiceTest.java`

## Nhóm 3 — Frontend Implementation & Tests (Đã có trong code — đã đối chiếu file)
- [x] Triển khai `publicMenuService.js`, `selfCheckoutHelpers.js`, `useCustomerSelfCheckout.js`, `SelfCheckoutPanel` trong `CurrentOrderSheet.jsx` (`SCR-05`)
- [x] Triển khai công tắc `customerSelfCheckoutEnabled` trong `PaymentSettingsTab.jsx` (`SCR-26`)
- [x] Có sẵn unit tests: `frontend/tests/unit/selfCheckout.test.js`, `frontend/tests/unit/restaurantSettings.test.js`

## Nhóm 4 — Verification & Sync-Back Proposal (Đang mở — Chờ người dùng duyệt)
- [x] Đối chiếu code thật với `02-design/`, `03-ba/`, `04-tech/` và xuất bảng **Sync-Back Proposal** (`SB-01` → `SB-04`) trong [spec.md](./spec.md)
- [ ] **Chờ người dùng duyệt** từng dòng `SB-01` → `SB-04`
- [ ] *(Chỉ làm sau khi duyệt)* Áp dụng diff vào `PROTOTYPE_SCREEN_MAP.md`, `PRD_BY_USER_STAGE.md`, `USER_STORIES.md`, `SYSTEM_DESIGN.md`
- [ ] *(Chỉ làm sau khi duyệt)* Thêm 1 dòng vào bảng `Sync Changelog` trong `KNOWLEDGE_HUB.md` và đổi `spec.md` sang `Status: Archived`
