---
trigger: glob
globs: "backend/src/main/**"
---

# Backend Core Patterns (`backend/src/main/**`)

Quy tắc bắt buộc khi thêm hoặc chỉnh sửa mã nguồn backend (`core`, `modules`, `infrastructure`, `shared`) và Flyway migrations (`resources/db/migration`).

## 1. Order State Machine (`com.qros.modules.order.state.*`)

Mọi chuyển trạng thái của `Order` bắt buộc đi qua `OrderStateFactory` (`validateTransition(current, target)` rồi `getState(target).handleRequest(order)`); **cấm** gọi `order.setStatus(...)` trực tiếp bỏ qua State Machine.

| Target State | Implementation | `allowedTransitionsFrom()` | Điều kiện / Ràng buộc trong `handleRequest` |
|---|---|---|---|
| `PENDING` | `PendingState` | `∅` (chỉ là trạng thái khởi tạo) | Không thể chuyển từ trạng thái khác quay về `PENDING`. |
| `SERVING` | `ServingState` | `PENDING`, `AWAITING_PAYMENT` | Chuyển về `SERVING` khi bếp bắt đầu làm hoặc khách gọi thêm món lúc đang chờ thanh toán. |
| `AWAITING_PAYMENT` | `AwaitingPaymentState` | `PENDING`, `SERVING` | Bắt buộc có ít nhất 1 `OrderItem::isBillable` **và** không còn item nào ngoài `FINISHED`/`CANCELLED` (vi phạm → `IllegalStateException`). |
| `COMPLETED` | `CompletedState` | `PENDING`, `SERVING`, `AWAITING_PAYMENT` | **Terminal state** — không có chuyển trạng thái ra ngoài. |
| `CANCELLED` | `CancelledState` | `PENDING`, `SERVING`, `AWAITING_PAYMENT` | **Terminal state** — không có chuyển trạng thái ra ngoài. |

- Khi cần thêm `OrderStatus` mới, tạo `@Component` triển khai `OrderState` để `OrderStateFactory` tự nạp vào `EnumMap`.

## 2. Quy ước Port, Mapper và DTO

- **Port (`*Port` / `*PortImpl`):**
  - Giao tiếp chéo module phải đi qua `*Port` interface (đặt ở module gọi hoặc `com.qros.shared.*`) hoặc service boundary của module sở hữu; triển khai `@Service`/`@Component` `*PortImpl` nằm trong module sở hữu dữ liệu.
  - Tuyệt đối không inject `*Repository` của module khác (vi phạm `BackendDependencyRulesTest`).
- **Mapper (`mapper/*Mapper.java`):**
  - Dùng `@Component` mapper tường minh (như `OrderMapper`, `OrderDiscountMapper`) để chuyển Entity → DTO với xử lý null-safe rõ ràng.
  - Không bao giờ trả JPA Entity trực tiếp ra `@RestController`.
- **DTO (`dto/request/*`, `dto/response/*`):**
  - Ưu tiên Java `record` bất biến, tách biệt hoàn toàn request và response.
  - Request DTO gắn Jakarta Validation (`@NotNull`, `@NotBlank`, `@Valid`, ...) với thông báo lỗi **tiếng Anh** (`BackendMessageLanguageTest`).
  - Response DTO chỉ expose trường cần thiết; tách `Public*Response` cho endpoint khách hàng và `*Response` cho nhân viên/quản trị.
  - Mọi endpoint trả về chuẩn `ApiResponse<T>` (`com.qros.shared.response.ApiResponse`).
- **Side-effect liên module:** Publish record từ `com.qros.shared.event.DomainEvents` qua `ApplicationEventPublisher` thay vì gọi thẳng listener/repository của module khác.

## 3. Checklist Bắt buộc khi Thêm / Sửa Endpoint Mới

1. **Định nghĩa Route constant:** Khai báo prefix/path trong `com.qros.shared.constants.ApiRoutes` — **cấm** hardcode chuỗi `"/api/..."` trong `SecurityRoutes`.
2. **Cập nhật `SecurityRoutes` & `SecurityConfig` (`com.qros.core.config.security`):**
   - Đăng ký route + HTTP method vào đúng nhóm quyền trong `SecurityRoutes` và đảm bảo được map trong `SecurityConfig` theo `RoleHierarchy` (`ADMIN > MANAGER > CASHIER > STAFF` và `MANAGER > CHEF`).
   - Tuyệt đối không đưa `ApiRoutes.ORDERS`, `ApiRoutes.AI`, hay `ApiRoutes.PAYMENTS` vào `PUBLIC_POST`.
3. **Method-level Guard & Plan Entitlement:**
   - Gắn `@PreAuthorize(...)` trên controller/method cho mọi thao tác nhạy cảm (defense-in-depth).
   - Gắn `@RequiresPlanFeature(PlanFeature.*)` nếu tính năng phụ thuộc gói subscription.
4. **Kiểm tra `RuntimeEndpointValidator`:**
   - Khi thay đổi cấu hình base URL, frontend URL hoặc CORS origins (`app.base-url`, `app.frontend.base-url`, `app.cors.allowed-origins`), đảm bảo tuân thủ `RuntimeEndpointValidator`: bắt buộc dùng HTTPS ngoài profile `dev`/`test` (`capacitor://localhost` chỉ được phép ở CORS origins).
5. **Chạy test xác minh ngay sau khi thêm endpoint:**
   - `SecurityConfigRegressionTest`, `RbacHardeningSecurityTest`, `RuntimeEndpointValidatorTest`, `RuntimeEndpointValidatorIntegrationTest`, `BackendDependencyRulesTest`, `BackendMessageLanguageTest` (hoặc `mvn -f backend/pom.xml --batch-mode verify`).

## 4. Database (Flyway) & Vùng nhạy cảm (Idempotency / Concurrency)

- **Flyway (`backend/src/main/resources/db/migration/`):**
  - Hệ thống chạy `ddl-auto: validate` và `baseline-on-migrate: false`.
  - **Cấm sửa hoặc xóa** bất kỳ file `V*__*.sql` nào đã tồn tại/đã áp dụng; mọi thay đổi schema phải kiểm tra phiên bản mới nhất trong `db/migration/` rồi tạo file mới tăng dần (`V1.xx__*.sql`).
- **Idempotency (Chống trùng đơn & thanh toán):**
  - Giữ nguyên cơ chế idempotency cho luồng tạo đơn qua `IdempotencyService` (`com.qros.shared.idempotency`).
  - Bảo toàn tính idempotency và chống thanh toán lặp ở `PayosWebhookController`, `PaymentPersistenceService` (`idempotencyKey`, `recoverIdempotency`) và `PaymentCompletionService` (xử lý duplicate payment khi đơn đã settled).
- **Concurrency & Locking:**
  - Mọi thao tác đổi trạng thái đơn/thanh toán hoặc giữ/hoàn tồn kho (`InventoryReservationService`) bắt buộc giữ nguyên transaction boundary và 2 lớp khóa hiện có:
    - *Pessimistic lock* (`@Lock(LockModeType.PESSIMISTIC_WRITE)` / `FOR UPDATE` / `FOR UPDATE SKIP LOCKED`) qua `ActiveOrderLockProjection` và các repository trọng yếu (`OrderRepository`, `PaymentTransactionRepository`, `InventoryItemRepository`, `OrderItemInventoryReservationRepository`, `VoucherRepository`, `DiningTableRepository`, `TableSessionRepository`, `RestaurantSubscriptionRepository`).
    - *Optimistic lock* (`@Version`) trên các entity trọng yếu (`Order`, `InventoryItem`, `Voucher`, `DiningTable`, `TableSession`, `RestaurantSubscription`, `SubscriptionInvoice`, ...).
