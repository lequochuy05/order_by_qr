# Execution Tasks `004`: Table Service Requests (Khách gọi nhân viên & Hàng đợi yêu cầu bàn realtime)

- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD bắt buộc (khớp CI):**
  - Backend: `mvn -f backend/pom.xml --batch-mode verify`
  - Frontend: `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Database & Migration (kèm test)
- [x] Tạo migration mới `backend/src/main/resources/db/migration/V1.23__Table_Service_Requests.sql` với bảng `table_service_requests`, các khóa ngoại (`tables`, `table_sessions`, `users`), 2 partial unique index (`uq_tsr_active_table_reason`, `uq_tsr_session_client_request`) và 2 composite index (`idx_tsr_status_created`, `idx_tsr_session_created`). *(Tuyệt đối không sửa migration cũ `V1`–`V1.22`)*
- [x] Tạo entity `TableServiceRequest`, enums `ServiceRequestReason` / `ServiceRequestStatus`, và `TableServiceRequestRepository` (kèm query `@Lock(LockModeType.PESSIMISTIC_WRITE)` và bulk close theo `tableSessionId`).
- [x] Kiểm chứng Flyway migration `V1.23` chạy sạch và mapping JPA hoạt động đúng trong context test Spring Boot.

## Nhóm 2 — Backend Implementation (kèm test ngay trong nhóm)
- [x] Khai báo `ApiRoutes.TABLE_SERVICE_REQUESTS`, các `ErrorCode` mới (`SERVICE_REQUEST_*`), `DomainEvents.ServiceRequestChangeEvent`, và rule rate limit `public:service-requests` (`6 req / 1m`) trong `application.yml`.
- [x] Triển khai DTOs (`CreateServiceRequestDto`, `CancelServiceRequestDto`, `TableServiceRequestResponse`, `CustomerServiceRequestsStateResponse`) và `TableServiceRequestMapper`.
- [x] Triển khai `TableServiceRequestService` đáp ứng đủ `BR-25` → `BR-28`:
  - Xác thực `sessionToken` + bàn không `INACTIVE`, validate lý do `OTHER` bắt buộc ghi chú (2–200 ký tự).
  - Chống spam 3 lớp: idempotency (`clientRequestId`), cooldown 60s/bàn, chặn trùng lý do đang mở, tối đa 3 yêu cầu mở/phiên bàn.
  - Nhận xử lý Single-Claim (`PESSIMISTIC_WRITE`), trả `409 Conflict` kèm tên nhân viên đã nhận nếu tranh chấp đồng thời.
  - Hoàn tất/Hủy theo đúng phân quyền (`assignee` hoặc `MANAGER`/`ADMIN`), và cập nhật `TableSessionService.closeSession` tự động đóng các yêu cầu đang mở khi phiên kết thúc.
- [x] Triển khai `PublicTableServiceRequestController` và `TableServiceRequestController`.
- [x] Cấu hình `SecurityRoutes`, `SecurityConfig` (tách route `STAFF_SERVICE_REQUEST_GET` để chặn `CHEF`), `WebSocketConfig` (`/topic/service-requests`), `NotificationService`, `WebsocketNotificationServiceImpl` và `DomainEventListener` (phát cả `/topic/service-requests` và `/topic/tables`).
- [x] Viết `TableServiceRequestServiceTest` (happy path, validate `OTHER`, cooldown 60s, duplicate active reason, open limit 3, idempotency, concurrent single-claim `409`, resolve permission, auto-close khi đóng phiên bàn) và cập nhật `CoreApiSecurityIntegrationTest`, `SecurityConfigRegressionTest`, `WebSocketConfigTest`, `WebsocketNotificationServiceImplTest`, `TableSessionServiceTest`.
- [x] Chạy `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify` → đạt **0 lỗi**, **ArchUnit**, **BackendMessageLanguageTest** & **Spotless** pass 100%.

## Nhóm 3 — Frontend Implementation (kèm test ngay trong nhóm)
- [x] Bổ sung `queryKeys.serviceRequests`, mã lỗi tiếng Việt `SERVICE_REQUEST_*` trong `errorMessages.js`, và các key song ngữ `VI`/`EN` trong `translations.js`.
- [x] Tạo entity `@entities/service-request` (`serviceRequestService.js`, `serviceRequestQueries.js`, `serviceRequestMutations.js`, `serviceRequestMeta.js`).
- [x] Triển khai luồng Khách gọi nhân viên trên `SCR-05` (`useTableServiceRequest.js`, `CallStaffButton.jsx`, `CallStaffModal.jsx`, cập nhật `CustomerMenuHeader.jsx`, `CustomerMenu.jsx`, `useCustomerMenuRealtime.js`) với 6 chip lý do nhanh, ghi chú, đếm ngược cooldown 60s, theo dõi trạng thái live và nút hủy khi còn chờ.
- [x] Triển khai màn hình Hàng đợi Yêu cầu Bàn `SCR-27` (`features/service-requests/*`, `ServiceRequestsPage.jsx`), tích hợp badge trên thẻ bàn `SCR-11` (`TableCard.jsx`, `TableManagement.jsx`), thêm route `/admin/service-requests` (`router.jsx`), mục menu `AdminSidebar.jsx`, và bảo vệ WebSocket trong `webSocketAccess.js` + `WebSocketInvalidator.jsx` (phát chuông + invalidate tức thì).
- [x] Rà soát đủ 5 trạng thái UI chuẩn (`Default`, `Loading`, `Empty`, `Error & Validation`, `Live Sync & Offline`), Light/Dark WCAG AA, Design Tokens `index.css` (không hardcode hex) và song ngữ `VI`/`EN`.
- [x] Viết `frontend/tests/unit/serviceRequests.test.js` và cập nhật `frontendHardening.test.js`, `errorMessages.test.js`.
- [x] Chạy `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build` → pass 100%.

## Nhóm 4 — Verification & Sync-Back Proposal (Chờ duyệt)
- [x] Đối chiếu thực tế code & hành vi chạy thử với Acceptance Criteria của `US-025`, `US-026`, `US-027` và `BR-25` → `BR-28`.
- [x] Chuyển `Status` trong [spec.md](./spec.md) sang `Implemented (Pending Sync-Back Review)` và lập **Sync-Back Proposal** (nếu có bất kỳ điểm lệch hoặc điều chỉnh nào phát sinh trong quá trình implement) chờ người dùng duyệt.
- [x] *(Chỉ tick sau khi người dùng duyệt)* Áp dụng diff được duyệt vào `docs/knowledge/`, thêm 1 dòng vào bảng `Sync Changelog` trong `KNOWLEDGE_HUB.md` và chuyển `spec.md` sang `Status: Archived`.
