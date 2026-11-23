# Execution Tasks `002` (Baseline — Dựng ngược từ code): Customer Table Order

- **Type:** `Baseline Verification Checklist` *(dựng ngược từ code có sẵn để demo luồng cốt lõi; mục Sync-Back giữ mở chờ người dùng duyệt)*
- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD (khớp CI):** `mvn -f backend/pom.xml --batch-mode verify` · `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Database & Schema (Đã có trong code — đã đối chiếu)
- [x] Bảng phiên bàn & token QR (`tables`, `table_sessions`, `table_session_tables`, `table_session_tokens` — `V1`, `V1.2`, `V1.15`)
- [x] Bảng đơn hàng nhiều đợt, tùy chọn món, chống trùng đơn & giữ kho (`orders`, `order_batches`, `order_item`, `order_item_options`, `idempotency_requests`, `order_item_inventory_reservations` — `V1`, `V1.6`, `V1.17`)

## Nhóm 2 — Backend Implementation & Tests (Đã có trong code — đã đối chiếu)
- [x] Phiên bàn QR công khai: `PublicTableSessionController` (`start-session`, `session-state`, `heartbeat`, `request-payment`) + `TableQrSecurityService`
- [x] Thực đơn công khai ghép tồn kho: `PublicMenuController` (`/catalog`, `/menu-items`, `/combos`, `/promotions/active`) + `InventoryAvailabilityService`
- [x] Đặt món chống trùng & tải đơn hiện hành: `CustomerOrderController` (`/orders/preview`, `/orders`, `/current-order` với `no-store`) + `OrderCreationService` + `IdempotencyService`
- [x] Bộ test có sẵn: `TableQrSecurityServiceTest`, `TableSessionServiceTest`, `OrderCreationServiceTest`, `OrderPricingServiceTest`, `OrderTableSyncServiceTest`, `InventoryAvailabilityServiceTest`, `IdempotencyServiceTest`

## Nhóm 3 — Frontend Implementation & Tests (Đã có trong code — đã đối chiếu)
- [x] Quản lý phiên & realtime: `useCustomerMenuSession.js`, `useCustomerMenuRealtime.js` (`/topic/tables`)
- [x] Thực đơn, tùy chọn & giỏ hàng: `CustomerMenu.jsx`, `MenuItemGrid.jsx`, `ItemOptionsModal.jsx`, `cartStore.js`, `CartModal.jsx`, `OrderingUnavailableBanner.jsx`, `CustomerMenuSkeleton.jsx`
- [x] Gửi đơn chống bấm đúp & theo dõi live: `useSubmitCustomerOrder.js` (`clientRequestId` + `sessionStorage` fingerprint), `CurrentOrderBanner.jsx`, `CurrentOrderSheet.jsx`
- [x] Bộ test có sẵn: `sessionStartCoordinator.test.js`, `orderMoney.test.js`, `cacheUtils.test.js`, `errorMessages.test.js`

## Nhóm 4 — Verification & Sync-Back Proposal (Đang mở — Chờ người dùng duyệt)
- [x] Đối chiếu code thật với `03-ba/` và `04-tech/`, xuất bảng **Sync-Back Proposal (`SB-05` → `SB-07`)** trong [spec.md](./spec.md)
- [ ] **Chờ người dùng duyệt** từng dòng `SB-05` → `SB-07`
- [ ] *(Chỉ làm sau khi duyệt)* Áp dụng diff vào `PRD_BY_USER_STAGE.md`, `USER_STORIES.md`, `SYSTEM_DESIGN.md`
- [ ] *(Chỉ làm sau khi duyệt)* Ghi 1 dòng vào `Sync Changelog` trong `KNOWLEDGE_HUB.md` và đổi `spec.md` sang `Status: Archived`
