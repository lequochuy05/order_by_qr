# Technical Plan `002` : Customer Table Order

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Kiến trúc & Hợp đồng giao tiếp thực tế trong codebase

### Database / Flyway
- Sử dụng các bảng lõi: `tables`, `table_sessions`, `table_session_tables`, `table_session_tokens`, `category`, `menu_item`, `item_option`, `item_option_values`, `combos`, `combo_items`, `orders`, `order_batches`, `order_item`, `order_item_options`, `order_status_history`, `order_item_inventory_reservations`, `idempotency_requests` (`V1`, `V1.2`, `V1.6`, `V1.15`, `V1.17`).

### REST API (Public — xác thực bằng `sig` & `sessionToken`)
- **Phiên bàn (`PublicTableSessionController`):**
  - `POST /api/v1/public/tables/{tableCode}/start-session?sig=...` → mở/khôi phục phiên bàn, cấp `sessionToken`
  - `GET /api/v1/public/tables/{tableCode}/session-state` → kiểm tra trạng thái phiên công khai
  - `POST /api/v1/public/sessions/heartbeat` → duy trì phiên bàn còn hoạt động
  - `POST /api/v1/public/tables/{tableCode}/request-payment` → đổi trạng thái chờ thanh toán & báo thu ngân
- **Thực đơn (`PublicMenuController`):**
  - `GET /api/v1/public/catalog` → tải gộp danh mục, món ăn, combo kèm trạng thái khả dụng tồn kho
  - `GET /api/v1/public/promotions/active`, `/categories`, `/menu-items`, `/combos`, `/tables/by-code/{tableCode}`
- **Đặt món & Theo dõi (`CustomerOrderController`):**
  - `POST /api/v1/public/orders/preview` → xem tạm tính (kèm khuyến mãi)
  - `POST /api/v1/public/orders` → gửi đơn kèm `clientRequestId` trong body (Durable Idempotency)
  - `GET /api/v1/public/tables/code/{tableCode}/current-order?sessionToken=...` → tải đơn hiện tại (`Cache-Control: no-store`)

### WebSocket STOMP
- Client khách dùng `publicWsService` (không cần JWT) subscribe `/topic/tables` để nhận cập nhật trạng thái bàn, món và đóng phiên realtime; backend đồng thời broadcast `/topic/orders` và `/topic/kitchen` cho nhân sự và bếp.

---

## 2. Bản đồ File thực tế trong codebase

### Backend (`backend/`)
- Controllers: `PublicTableSessionController.java`, `PublicMenuController.java`, `CustomerOrderController.java`
- Services cốt lõi: `TableSessionService`, `TableQrSecurityService`, `OrderService` / `OrderCreationService`, `OrderPricingService`, `OrderTableSyncService`, `InventoryAvailabilityService`, `IdempotencyService`
- Tests liên quan: `TableQrSecurityServiceTest.java`, `TableSessionServiceTest.java`, `OrderCreationServiceTest.java`, `OrderPricingServiceTest.java`, `OrderTableSyncServiceTest.java`, `InventoryAvailabilityServiceTest.java`, `IdempotencyServiceTest.java`, `CoreApiSecurityIntegrationTest.java`

### Frontend (`frontend/`)
- Entities: `frontend/src/entities/public-menu/api/{publicMenuService.js, publicMenuQueries.js, tableOrderCatalogService.js}`
- Models / Hooks (`frontend/src/features/customer-menu/model/`): `cartStore.js`, `useCart.js`, `useCustomerCart.js`, `useCustomerMenuSession.js`, `useSubmitCustomerOrder.js`, `useCustomerMenuRealtime.js`, `useCrossSellRecommendations.js`
- UI (`frontend/src/features/customer-menu/ui/`): `CustomerMenu.jsx`, `CustomerMenuHeader.jsx`, `CategoryFilter.jsx`, `MenuItemGrid.jsx`, `MenuCard.jsx`, `ComboSection.jsx`, `ComboCard.jsx`, `CartModal.jsx`, `ShoppingCart.jsx`, `SubmitOrderButton.jsx`, `CurrentOrderBanner.jsx`, `CurrentOrderSheet.jsx`, `OrderingUnavailableBanner.jsx`, `CustomerMenuSkeleton.jsx` + `frontend/src/shared/ui/ItemOptionsModal.jsx`
- Tests liên quan: `frontend/tests/unit/{sessionStartCoordinator.test.js, orderMoney.test.js, cacheUtils.test.js, errorMessages.test.js}`

---

## 3. Đối chiếu với `CONSTITUTION.md`
- **Auto-Enforced:** `BackendDependencyRulesTest`, `BackendMessageLanguageTest`, `spotless:check`, 7 bộ unit/integration test backend nêu trên, `node:test`, `eslint`, `vite build`.
- **Manual Review:** Kiểm tra chữ ký QR (`sig`), xử lý chống gửi trùng (`clientRequestId` + `sessionStorage` fingerprint), trạng thái khóa đặt món khi đang thanh toán/bảo trì, Mobile Safe Area, Light/Dark WCAG AA và song ngữ `VI`/`EN`.
