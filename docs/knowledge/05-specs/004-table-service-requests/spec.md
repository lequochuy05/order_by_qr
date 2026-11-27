# Spec `004`: Table Service Requests (Khách gọi nhân viên & Hàng đợi yêu cầu bàn realtime)

- **Type:** `New Feature`
- **Branch / Track:** `Nhánh A (Full SDD)` *(Bắt buộc vì thêm bảng DB/migration `V1.23`, API mới, STOMP topic mới `/topic/service-requests`, màn hình `SCR-27`, quy tắc `BR-25` → `BR-28`, cùng cơ chế concurrency locking, rate limit và phân quyền RBAC)*
- **Status:** `Archived` *(Draft → In Progress → Implemented (Pending Sync-Back Review) → Archived)*
- **Source of Truth:**
  - PO: [journey-features.md](../../01-po/journey-features.md) (`F1.6`, `F4.5`) · [personas.md](../../01-po/personas.md) (`PER-01`, `PER-02`, `PER-04..06`)
  - Design: [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-05`, `SCR-11`, `SCR-27`) · [DESIGN_SYSTEM.md](../../02-design/DESIGN_SYSTEM.md)
  - BA PRD: [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md) (`STG-02`, `STG-04`, `BR-25` → `BR-28`)
  - BA User Stories: [USER_STORIES.md](../../03-ba/USER_STORIES.md) (`US-025`, `US-026`, `US-027`)
  - Tech: [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md)

---

## 1. Phạm vi & Truy vết (Không chép lại toàn văn PRD)
- **Mục tiêu ngắn gọn:** Triển khai đồng thời hai nửa nghiệp vụ kết nối thời gian thực *(1)* Thực khách tại bàn (`PER-01`) gọi nhân viên hỗ trợ trên `SCR-05` theo lý do nhanh + ghi chú với cơ chế chống spam 3 lớp, và *(2)* Đội ngũ phục vụ (`PER-02`, kế thừa `PER-04`/`PER-05`/`PER-06`) tiếp nhận, khóa xử lý đơn người (Single-Claim) và hoàn tất yêu cầu trên hàng đợi realtime `SCR-27` kèm badge cảnh báo trên sơ đồ bàn `SCR-11`.
- **IDs liên quan:** `F1.6`, `F4.5` · `STG-02`, `STG-04` · `PER-01`, `PER-02`, `PER-04`, `PER-05`, `PER-06` · `SCR-05`, `SCR-11`, `SCR-27` · `BR-01`, `BR-04`, `BR-24`, `BR-25`, `BR-26`, `BR-27`, `BR-28` · `US-025`, `US-026`, `US-027`.
- **Ngoài phạm vi (Out of Scope):**
  - Không xây dựng báo cáo thống kê SLA lịch sử dài hạn (Analytics Dashboard cho yêu cầu bàn để giai đoạn sau).
  - Không gửi Push Notification hệ điều hành (FCM/APNs) hay tin nhắn SMS ngoài ứng dụng/trình duyệt.
  - Không cho phép `PER-03 (Chef)` và `PER-01 (Customer)` truy cập hàng đợi nội bộ hoặc thao tác nhận/hoàn tất yêu cầu bàn.

## 2. Làm rõ yêu cầu (Clarifications)
*(Người dùng đã xác nhận thông qua `oke triển khai` cho 4 mặc định dưới đây)*:
- [x] **C1 — Ngưỡng chống spam (`BR-26`):** Cooldown mặc định **60 giây / bàn** giữa 2 lần gửi mới và tối đa **3 yêu cầu đang mở đồng thời / phiên bàn** (ngoài ra chặn trùng lý do đang mở và rate limit IP `6 req / 1m`). Ngưỡng này đã phù hợp chưa?
- [x] **C2 — Bộ 6 lý do chọn nhanh (`BR-25`):** `WATER_ICE` (*Thêm nước / Đá*), `UTENSILS_NAPKINS` (*Thêm chén đũa / Khăn giấy*), `CLEAN_TABLE` (*Dọn bàn*), `ORDER_ASSISTANCE` (*Hỗ trợ gọi món*), `REQUEST_BILL` (*Gọi thanh toán*), `OTHER` (*Khác* — bắt buộc ghi chú ≥ 2 ký tự). Có cần thêm hoặc điều chỉnh lý do nào không?
- [x] **C3 — Ngưỡng cảnh báo quá hạn SLA (`BR-27`):** Mặc định đánh dấu `Danger` khi yêu cầu chờ quá **5 phút**. Trước mắt dùng hằng số mặc định trên hệ thống thay vì thêm cột cấu hình mới vào `system_settings` — bạn có đồng ý không?
- [x] **C4 — Tách biệt lý do `REQUEST_BILL` với nút *Yêu cầu thanh toán*:** Đề xuất mặc định giữ tách biệt: chọn `REQUEST_BILL` trong form gọi nhân viên chỉ tạo yêu cầu hỗ trợ để phục vụ/thu ngân tới bàn hỗ trợ (không tự ép chuyển bàn sang `WAITING_FOR_PAYMENT` khi còn món chưa xong ở bếp, tránh xung đột logic `TableActiveOrderChecker`). Bạn xác nhận phương án này chứ?

## 3. Ràng buộc bổ sung (cần lưu ý khi triển khai & Sync-Back)
- Hàng đợi nhân sự sử dụng topic WebSocket bảo mật mới `/topic/service-requests` (`STAFF`/`CASHIER`/`MANAGER`/`ADMIN`, chặn `CHEF` & `CUSTOMER`). Màn hình khách `SCR-05` nhận đồng bộ trạng thái yêu cầu an toàn qua topic công khai hiện có `/topic/tables` để không phát sinh lỗi xác thực STOMP như từng gặp ở topic nội bộ.
- Mọi thông điệp exception ở backend phải viết bằng tiếng Anh theo `BackendMessageLanguageTest`, đồng thời khai báo đầy đủ mã lỗi tiếng Việt tương ứng tại `frontend/src/shared/lib/errorMessages.js`.
- `TableSessionService.closeSession(...)` bắt buộc gọi đóng các yêu cầu đang mở thuộc phiên đó để đảm bảo hàng đợi `SCR-27` và badge trên `SCR-11` sạch sau khi bàn thanh toán xong.
