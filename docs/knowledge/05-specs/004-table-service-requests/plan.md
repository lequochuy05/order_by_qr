# Technical Plan `004`: Table Service Requests (Khách gọi nhân viên & Hàng đợi yêu cầu bàn realtime)

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Ảnh hưởng Kiến trúc & Hợp đồng giao tiếp

### Database / Flyway
- Thêm mới `backend/src/main/resources/db/migration/V1.23__Table_Service_Requests.sql` *(cấm sửa `V1`–`V1.22`)*:
  - Tạo bảng `table_service_requests`:
    - `id BIGSERIAL PRIMARY KEY`, `version BIGINT NOT NULL DEFAULT 0` (`@Version`)
    - `table_id BIGINT NOT NULL REFERENCES tables(id)`
    - `table_session_id BIGINT NOT NULL REFERENCES table_sessions(id)`
    - `reason VARCHAR(40) NOT NULL` (`WATER_ICE`, `UTENSILS_NAPKINS`, `CLEAN_TABLE`, `ORDER_ASSISTANCE`, `REQUEST_BILL`, `OTHER`)
    - `note VARCHAR(200)`
    - `status VARCHAR(20) NOT NULL DEFAULT 'PENDING'` (`PENDING`, `CLAIMED`, `RESOLVED`, `CANCELLED`)
    - `client_request_id VARCHAR(64) NOT NULL`
    - `claimed_by_user_id BIGINT REFERENCES users(id)`, `claimed_at TIMESTAMP`
    - `resolved_by_user_id BIGINT REFERENCES users(id)`, `resolved_at TIMESTAMP`
    - `cancelled_by_type VARCHAR(20)` (`CUSTOMER`, `STAFF`, `MANAGER`, `ADMIN`, `SYSTEM`), `cancelled_reason VARCHAR(200)`
    - `created_at TIMESTAMP NOT NULL`, `updated_at TIMESTAMP NOT NULL`, `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`
  - **2 Partial Unique Indexes (khóa cứng chống trùng ở mức DB):**
    - `uq_tsr_active_table_reason` trên `(table_id, reason) WHERE status IN ('PENDING', 'CLAIMED') AND is_deleted = false` → chặn 100% gửi trùng lý do đang mở cho cùng 1 bàn ngay cả khi race condition.
    - `uq_tsr_session_client_request` trên `(table_session_id, client_request_id) WHERE is_deleted = false` → đảm bảo idempotency theo phiên bàn.
  - **Composite Indexes tối ưu truy vấn hàng đợi:**
    - `idx_tsr_status_created` trên `(status, created_at ASC) WHERE is_deleted = false`
    - `idx_tsr_session_created` trên `(table_session_id, created_at DESC) WHERE is_deleted = false`

### REST API / Security / Rate Limit
| Method | Endpoint | Auth / Role | Mục đích & Bảo vệ |
|---|---|---|---|
| `GET` | `/api/v1/public/tables/{tableCode}/service-requests?sessionToken=...` | Public (`PUBLIC_GET`) + `sessionToken` hợp lệ | Trả danh sách yêu cầu đang mở/gần nhất của phiên bàn + `cooldownRemainingSeconds` + `maxOpenRequests` (`3`) |
| `POST` | `/api/v1/public/tables/{tableCode}/service-requests` | Public (`PUBLIC_POST`) + `sessionToken` + `RateLimitingFilter` (`public:service-requests`, `6 req / 1m`) | Tạo yêu cầu mới: khóa phiên/bàn `PESSIMISTIC_WRITE`, kiểm tra idempotency (`clientRequestId`), cooldown 60s, trùng lý do đang mở, tối đa 3 yêu cầu mở, validate ghi chú `OTHER` (2–200 ký tự) |
| `POST` | `/api/v1/public/tables/{tableCode}/service-requests/{id}/cancel` | Public (`PUBLIC_POST`) + `sessionToken` | Khách tự hủy yêu cầu thuộc đúng phiên bàn khi còn ở trạng thái `PENDING` |
| `GET` | `/api/v1/table-service-requests?status=OPEN&mineOnly=false` | Protected (`STAFF`, kế thừa `CASHIER`, `MANAGER`, `ADMIN`; chặn `CHEF`) | Lấy hàng đợi yêu cầu bàn (mặc định `PENDING` + `CLAIMED` sắp theo `createdAt ASC`) |
| `PATCH` | `/api/v1/table-service-requests/{id}/claim` | Protected (`STAFF`..`ADMIN`; chặn `CHEF`) | Khóa dòng bằng `SELECT FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`); chỉ chuyển `PENDING → CLAIMED` cho người đầu tiên, trả `409 Conflict` kèm tên người đã nhận nếu đã bị claim |
| `PATCH` | `/api/v1/table-service-requests/{id}/resolve` | Protected (`STAFF`..`ADMIN`; chặn `CHEF`) | Chuyển `CLAIMED → RESOLVED`; chỉ cho phép đúng `claimedByUserId` hoặc `MANAGER`/`ADMIN` |
| `PATCH` | `/api/v1/table-service-requests/{id}/cancel` | Protected (`STAFF`..`ADMIN`; chặn `CHEF`) | Hủy yêu cầu từ phía nhân sự (`PENDING`/`CLAIMED → CANCELLED`); từ `CLAIMED` chỉ cho `MANAGER`/`ADMIN` hoặc người đã nhận |

- **ErrorCodes mới (`ErrorCode.java` — 100% tiếng Anh):**
  - `SERVICE_REQUEST_NOT_FOUND` (`404`)
  - `SERVICE_REQUEST_COOLDOWN` (`429`)
  - `SERVICE_REQUEST_DUPLICATE_ACTIVE` (`409`)
  - `SERVICE_REQUEST_OPEN_LIMIT_REACHED` (`429`)
  - `SERVICE_REQUEST_ALREADY_CLAIMED` (`409`)
  - `SERVICE_REQUEST_INVALID_STATE` (`409`)
  - `SERVICE_REQUEST_NOT_ASSIGNED_TO_USER` (`403`)

### WebSocket STOMP Topics
- `WebSocketConfig.PROTECTED_TOPICS`: thêm `"/topic/service-requests" -> Set.of("ROLE_MANAGER", "ROLE_STAFF")` *(nhờ `RoleHierarchy`, `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` subscribe được; `CHEF` và anonymous bị từ chối)*.
- `DomainEvents.java`: thêm `record ServiceRequestChangeEvent(String action, Long requestId, Long tableId) {}`.
- `DomainEventListener.java` + `NotificationService.java` + `WebsocketNotificationServiceImpl.java`:
  - Phát `/topic/service-requests` *(payload gồm `event`, `id`, `data: { action, tableId }`)*.
  - Phát kèm `/topic/tables` *(event `UPDATED` hoặc `SERVICE_REQUEST_UPDATED`)* để `SCR-05` của khách hàng và `SCR-11` Sơ đồ bàn tự động invalidate dữ liệu realtime mà không cần khách subscribe topic bảo mật.

---

## 2. Danh sách File dự kiến thêm / sửa

### Backend (`backend/`)
- **Migration:**
  - `backend/src/main/resources/db/migration/V1.23__Table_Service_Requests.sql` *(new)*
- **Module `table` (`backend/src/main/java/com/qros/modules/table/`):**
  - `model/enums/ServiceRequestReason.java` *(new)*
  - `model/enums/ServiceRequestStatus.java` *(new)*
  - `model/TableServiceRequest.java` *(new)*
  - `repository/TableServiceRequestRepository.java` *(new)*
  - `dto/request/CreateServiceRequestDto.java` *(new)*
  - `dto/request/CancelServiceRequestDto.java` *(new)*
  - `dto/response/TableServiceRequestResponse.java` *(new)*
  - `dto/response/CustomerServiceRequestsStateResponse.java` *(new)*
  - `mapper/TableServiceRequestMapper.java` *(new)*
  - `service/TableServiceRequestService.java` *(new)*
  - `controller/PublicTableServiceRequestController.java` *(new)*
  - `controller/TableServiceRequestController.java` *(new)*
  - `service/TableSessionService.java` *(modify — gọi đóng yêu cầu mở khi `closeSession` chạy)*
- **Shared, Core, Notification & Config:**
  - `backend/src/main/java/com/qros/shared/constants/ApiRoutes.java` *(modify — thêm `TABLE_SERVICE_REQUESTS`)*
  - `backend/src/main/java/com/qros/shared/exception/ErrorCode.java` *(modify)*
  - `backend/src/main/java/com/qros/shared/event/DomainEvents.java` *(modify)*
  - `backend/src/main/java/com/qros/core/config/security/SecurityRoutes.java` *(modify — `PUBLIC_POST`, `STAFF_SERVICE_REQUEST_GET`, `STAFF_OPERATION_PATCH`)*
  - `backend/src/main/java/com/qros/core/config/security/SecurityConfig.java` *(modify — ánh xạ `STAFF_SERVICE_REQUEST_GET` cho `hasAnyRole("MANAGER", "STAFF")` tách khỏi `OPERATION_GET` để chặn `CHEF`)*
  - `backend/src/main/java/com/qros/core/config/WebSocketConfig.java` *(modify — thêm `/topic/service-requests`)*
  - `backend/src/main/java/com/qros/modules/notification/service/NotificationService.java` *(modify)*
  - `backend/src/main/java/com/qros/modules/notification/service/impl/WebsocketNotificationServiceImpl.java` *(modify)*
  - `backend/src/main/java/com/qros/modules/notification/listener/DomainEventListener.java` *(modify)*
  - `backend/src/main/resources/application.yml` *(modify — rule `public:service-requests`)*
- **Tests (`backend/src/test/java/com/qros/`):**
  - `modules/table/service/TableServiceRequestServiceTest.java` *(new)*
  - `core/config/WebSocketConfigTest.java` *(modify — verify `/topic/service-requests` cho phép `STAFF`/`CASHIER`/`MANAGER`/`ADMIN`, chặn `CHEF` và anonymous)*
  - `core/config/security/CoreApiSecurityIntegrationTest.java` *(modify — verify RBAC `/api/v1/table-service-requests/**` & public validation)*
  - `core/config/SecurityConfigRegressionTest.java` *(modify)*
  - `modules/notification/service/impl/WebsocketNotificationServiceImplTest.java` *(modify)*
  - `service/TableSessionServiceTest.java` *(modify — verify tự đóng yêu cầu khi đóng phiên bàn)*

### Frontend (`frontend/`)
- **Shared (`frontend/src/shared/`):**
  - `api/queryKeys.js` *(modify — thêm `serviceRequests` query keys)*
  - `lib/errorMessages.js` *(modify — dịch tiếng Việt các `SERVICE_REQUEST_*` error codes)*
  - `lib/i18n/translations.js` *(modify — bổ sung key `VI`/`EN` cho `customer` và `serviceRequests`/`tables`)*
- **Entity (`frontend/src/entities/service-request/`):**
  - `index.js` *(new)*
  - `api/serviceRequestService.js` *(new)*
  - `api/serviceRequestQueries.js` *(new)*
  - `api/serviceRequestMutations.js` *(new)*
  - `lib/serviceRequestMeta.js` *(new — 6 lý do chuẩn, màu trạng thái semantic theo `index.css`, SLA > 5 phút)*
- **Feature `customer-menu` (`SCR-05` — `frontend/src/features/customer-menu/`):**
  - `model/useTableServiceRequest.js` *(new — quản lý `clientRequestId`, đồng hồ cooldown 60s, trạng thái danh sách yêu cầu của bàn)*
  - `ui/CallStaffButton.jsx` *(new)*
  - `ui/CallStaffModal.jsx` *(new — Bottom Sheet / Modal 6 chip lý do + ghi chú ≤ 200 ký tự + thẻ trạng thái trực tiếp + nút hủy khi `PENDING`)*
  - `ui/CustomerMenuHeader.jsx` *(modify — gắn `CallStaffButton`)*
  - `ui/CustomerMenu.jsx` *(modify)*
  - `model/useCustomerMenuRealtime.js` *(modify — invalidate `serviceRequests` khi nhận `/topic/tables`)*
- **Feature `service-requests` (`SCR-27`) & Sơ đồ bàn (`SCR-11`):**
  - `frontend/src/features/service-requests/index.js` *(new)*
  - `frontend/src/features/service-requests/model/useServiceRequestsQueue.js` *(new — lọc, `mineOnly`, âm thanh cảnh báo, timer 1s kiểm tra quá SLA 5 phút)*
  - `frontend/src/features/service-requests/ui/ServiceRequestQueue.jsx` *(new)*
  - `frontend/src/features/service-requests/ui/ServiceRequestCard.jsx` *(new)*
  - `frontend/src/features/service-requests/ui/ServiceRequestFilters.jsx` *(new)*
  - `frontend/src/pages/admin/ServiceRequestsPage.jsx` *(new)*
  - `frontend/src/features/table-management/ui/TableCard.jsx` *(modify — badge yêu cầu đang chờ trên thẻ bàn)*
  - `frontend/src/features/table-management/ui/TableManagement.jsx` *(modify — tải badge yêu cầu đang mở + nút chuyển nhanh sang `/admin/service-requests`)*
  - `frontend/src/widgets/admin-layout/AdminSidebar.jsx` *(modify — mục menu *"Yêu cầu bàn"* cho `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` kèm badge đếm realtime)*
  - `frontend/src/app/router.jsx` *(modify — route `/admin/service-requests` trong nhóm `['ADMIN', 'MANAGER', 'CASHIER', 'STAFF']`)*
  - `frontend/src/app/webSocketAccess.js` *(modify — thêm `canSubscribeToServiceRequests`)*
  - `frontend/src/app/WebSocketInvalidator.jsx` *(modify — lắng nghe `/topic/service-requests` & invalidate + âm thanh thông báo)*
- **Tests (`frontend/tests/unit/`):**
  - `serviceRequests.test.js` *(new — test SLA 5 phút, cooldown, 6 lý do, sắp xếp hàng đợi, helper hiển thị)*
  - `frontendHardening.test.js` *(modify — test `canSubscribeToServiceRequests` chặn `CHEF` & cho phép `STAFF/CASHIER/MANAGER/ADMIN`)*
  - `errorMessages.test.js` *(modify — test dịch `SERVICE_REQUEST_*`)*

---

## 3. Kiểm tra nhanh theo `CONSTITUTION.md`
- [ ] **Ranh giới Modular Monolith (`ArchUnit`) & Thông báo tiếng Anh (`BackendMessageLanguageTest`):** `TableServiceRequest` nằm hoàn toàn trong `com.qros.modules.table`, không import `com.qros.modules.order`; giao tiếp sang `notification` qua `DomainEvents`; 100% thông báo lỗi & exception dùng tiếng Anh.
- [ ] **Bảo mật RBAC / Concurrency / Rate Limit / Idempotency:** Route `/api/v1/table-service-requests/**` và `/topic/service-requests` chặn `CHEF` và anonymous; `claim` dùng `SELECT FOR UPDATE` + `@Version`; chống spam khách bằng `RateLimitingFilter`, cooldown 60s, giới hạn 3 yêu cầu mở, `clientRequestId` và partial unique indexes.
- [ ] **Flyway:** Chỉ thêm mới `V1.23__Table_Service_Requests.sql`, không chỉnh sửa `V1`–`V1.22`.
- [ ] **FSD & Design Tokens 2 tầng:** Đúng chiều `app → pages → widgets → features → entities → shared`; không hardcode mã hex mới; dùng token semantic từ `index.css` bảo đảm Light/Dark WCAG AA và đủ 5 trạng thái UI + song ngữ `VI`/`EN`.
- [ ] **Chiến lược Test gắn vào từng nhóm task:** Mỗi nhóm DB / Backend / Frontend trong `tasks.md` đều có unit/integration test và lệnh DoD đi kèm ngay trong nhóm.
