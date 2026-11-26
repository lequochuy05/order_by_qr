# Spec `002`: Customer Table Order (Khách quét QR & Đặt món tại bàn)

- **Type:** `Baseline Spec (dựng ngược từ code có sẵn — Mẫu demo luồng cốt lõi Khách đặt món tại bàn)`
- **Branch / Track:** `Nhánh C (Brownfield Baseline / Sync-Back — Luồng cốt lõi phục vụ demo)`
- **Status:** `Implemented (Pending Sync-Back Review)` *(Giữ nguyên trạng thái chờ duyệt để demo quy trình Sync-Back trước thầy; chưa chuyển `Archived`)*
- **Source of Truth:**
  - PO: [journey-features.md](../../01-po/journey-features.md) (`STG-01`, `STG-02`, `F1.1`–`F1.4`)
  - Design: [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-05`, `SCR-08`) · [DESIGN_SYSTEM.md](../../02-design/DESIGN_SYSTEM.md)
  - BA PRD: [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md) (`BR-01`, `BR-05`, `BR-06`, `BR-07`, `BR-10`, `BR-24`)
  - BA User Stories: [USER_STORIES.md](../../03-ba/USER_STORIES.md) (`US-001`, `US-005`, `US-007`, `US-008`)
  - Tech: [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md)

> **Lưu ý minh bạch khi demo:** Quy luật chuẩn cho tính năng mới là **chiều thuận** (`01-po → 02-design → 03-ba → 04-tech → 05-specs → Sinh Code`). Vì luồng *Khách đặt món tại bàn* đã có sẵn trong codebase, tài liệu này là **Baseline Spec dựng ngược từ mã nguồn thực tế** để minh họa trọn vẹn cách một luồng cốt lõi nối từ `PO → Design → BA → Tech → Code` và phát hiện các điểm lệch cần Sync-Back.

---

## 1. Phạm vi & Truy vết
- **Mục tiêu:** Bao phủ trọn vẹn luồng trải nghiệm của Thực khách tại bàn (`PER-01`) trên `SCR-05` (`/:tableCode/menu`): quét QR xác thực phiên bàn → duyệt danh mục/món/combo có kiểm tra tồn kho → tùy biến kích cỡ/topping & ghi chú → xem trước/gửi đơn chống trùng lặp (nhiều đợt) → theo dõi tiến độ bếp trực tiếp và gọi thanh toán.
- **IDs liên quan:** `STG-01`, `STG-02` · `PER-01` · `F1.1`, `F1.2`, `F1.3`, `F1.4` · `SCR-05`, `SCR-08` · `BR-01`, `BR-05`, `BR-06`, `BR-07`, `BR-10`, `BR-24` · `US-001`, `US-005`, `US-007`, `US-008`.
- **Ngoài phạm vi:** Luồng tự thanh toán VietQR của khách (đã tách ở [001-customer-self-checkout](../001-customer-self-checkout/spec.md)), Trợ lý AI chat (`F1.5`), và thao tác Bếp KDS (`STG-03`).

---

## 2. Hành vi thực tế xác minh từ mã nguồn
1. **Khởi tạo & duy trì phiên bàn an toàn (`PublicTableSessionController.java`, `useCustomerMenuSession.js`):**
   - Quét QR gọi `POST /api/v1/public/tables/{tableCode}/start-session?sig=...` kiểm tra chữ ký HMAC bàn và trả `sessionToken`.
   - Duy trì phiên bằng `POST /api/v1/public/sessions/heartbeat`, kiểm tra trạng thái phiên qua `GET /api/v1/public/tables/{tableCode}/session-state`, và gọi nhân viên thanh toán qua `POST /api/v1/public/tables/{tableCode}/request-payment`.
2. **Tải thực đơn hợp nhất kèm khả dụng tồn kho (`PublicMenuController.java`):**
   - `GET /api/v1/public/catalog` trả gộp danh mục, món ăn và combo đã ghép sẵn trạng thái tồn kho từ `InventoryAvailabilityService`, đồng thời tuân theo 2 cấu hình quán: `showUnavailableItems` (ẩn hay hiện badge hết hàng) và `showCombos`.
3. **Gửi đơn chống trùng lặp đa tầng (`CustomerOrderController.java`, `useSubmitCustomerOrder.js`):**
   - Trước khi gửi, khách có thể xem tạm tính qua `POST /api/v1/public/orders/preview`.
   - Khi bấm gửi (`POST /api/v1/public/orders`), frontend sinh `clientRequestId` gắn với **request fingerprint** lưu trong `sessionStorage` (`qros:pending-order-request`) kết hợp `submittingRef` chặn bấm đúp và `IdempotencyService` ở backend.
   - Các điều kiện chặn đặt món ngay ở đầu luồng: bàn đang khóa thanh toán (`ORDER_PAYMENT_IN_PROGRESS`), quán bật chế độ bảo trì (`maintenanceMode`), quán tạm ngưng nhận đơn (`orderingUnavailable`), hoặc phiên hết hạn (`TABLE_SESSION_EXPIRED` / `TABLE_SESSION_INVALID`).
4. **Theo dõi đơn trực tiếp & khôi phục khi tải lại (`CustomerOrderController.java:42-50`, `useCustomerMenuRealtime.js`):**
   - Lấy đơn hiện hành qua `GET /api/v1/public/tables/code/{tableCode}/current-order?sessionToken=...` (`Cache-Control: no-store`).
   - Lắng nghe `/topic/tables` qua `publicWsService` để tự cập nhật trạng thái món và trạng thái bàn.

---

## 3. Sync-Back Proposal (Chờ người dùng duyệt — CHƯA ghi vào nguồn chân lý)

| # | ID / Phạm vi ảnh hưởng | File nguồn chân lý | Điểm lệch phát hiện khi đối chiếu code thật | Diff đề xuất |
|---|---|---|---|---|
| **SB-05** | `BR-05`, `BR-06`, `STG-02` | [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md#L62-L78) | Chưa nêu 2 quy tắc chặn đặt món đang chạy thật: *(1)* khóa gọi thêm món khi bàn đã vào trạng thái **đang xử lý thanh toán**, và *(2)* khóa đặt món khi quán bật **chế độ bảo trì / tạm ngưng nhận đơn**. Ngoài ra chưa ghi tùy chọn ẩn hoặc hiện món hết hàng theo cài đặt quán. | Bổ sung vào `BR-05` / `BR-06` (và `STG-02` Edge Cases): khóa gọi thêm món khi bàn đang thanh toán hoặc quán tạm ngưng/bảo trì; hiển thị/ẩn món hết hàng theo cấu hình quán. |
| **SB-06** | `US-007` | [USER_STORIES.md](../../03-ba/USER_STORIES.md#L97-L104) | AC của `US-007` chưa có kịch bản kiểm thử khi khách cố gọi thêm món lúc bàn đang thanh toán hoặc lúc phiên bàn hết hạn phải quét lại QR. | Bổ sung AC 4 cho `US-007`: chặn gọi thêm món khi bàn đang thanh toán (hiển thị thông báo khóa thanh toán) và yêu cầu làm mới phiên nếu phiên bàn hết hạn. |
| **SB-07** | Sequence Diagram & Public API (§6.1, §6.3) | [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md#L186-L224) | §6.3 Sequence Diagram đang ghi rút gọn `GET /api/v1/public/tables/{code} & menu` và `Header: Idempotency-Key`. Code thực tế dùng `POST /api/v1/public/tables/{tableCode}/start-session?sig=...`, `GET /api/v1/public/catalog`, `POST /api/v1/public/orders/preview`, `GET /api/v1/public/tables/code/{tableCode}/current-order`, và truyền `clientRequestId` trong **JSON body** (kèm `sessionStorage` fingerprint). | Cập nhật §6.1 và §6.3 trong `SYSTEM_DESIGN.md` đúng theo danh sách endpoint và cơ chế `clientRequestId` thực tế. |

> **Hành động tiếp theo:** Người dùng duyệt từng dòng `SB-05` → `SB-07`. Chỉ sau khi duyệt, agent mới áp dụng diff vào `PRD_BY_USER_STAGE.md`, `USER_STORIES.md`, `SYSTEM_DESIGN.md`, ghi 1 dòng vào `Sync Changelog` tại `KNOWLEDGE_HUB.md` và đổi `Status` sang `Archived`.
