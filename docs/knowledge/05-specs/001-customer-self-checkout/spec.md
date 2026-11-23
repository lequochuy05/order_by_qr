# Spec `001`: Customer Self-Checkout (VietQR PayOS tại bàn)

- **Type:** `Baseline Spec (dựng ngược từ code có sẵn — Reverse-Engineered)`
- **Branch / Track:** `Nhánh C (Brownfield Baseline / Sync-Back — Ví dụ minh họa đối chiếu cho buổi trình bày)`
- **Status:** `Implemented (Pending Sync-Back Review)` *(Chưa chuyển `Archived` cho tới khi người dùng duyệt Sync-Back Proposal ở cuối tài liệu)*
- **Source of Truth:**
  - PO: [journey-features.md](../../01-po/journey-features.md) (`F3.2`, `F3.3`, `F6.5`)
  - Design: [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-05`, `SCR-26`)
  - BA PRD: [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md) (`STG-05`, `STG-06`, `BR-17`, `BR-18`, `BR-19`, `BR-22`)
  - BA User Stories: [USER_STORIES.md](../../03-ba/USER_STORIES.md) (`US-018`, `US-024`)
  - Tech: [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md)

> **Lưu ý minh bạch:** Quy luật chuẩn của QROS là **chiều thuận (`01-po → 02-design → 03-ba → 04-tech → 05-specs → Sinh Code`)**. Riêng `001-customer-self-checkout` là **Baseline Spec dựng ngược từ mã nguồn có sẵn (`V1.22`)** dùng làm ví dụ minh họa quy trình phát hiện lệch & duyệt Sync-Back cho buổi trình bày. Từ `002-` trở đi, mọi tính năng mới đều đi theo chiều thuận.

---

## 1. Phạm vi & Truy vết
- **Mục tiêu:** Cho phép khách hàng tại bàn (`PER-01`) tự khởi tạo thanh toán **VietQR PayOS**, áp dụng mã giảm giá, hủy/đổi mã QR đang chờ và đồng bộ trạng thái thanh toán ngay trên `SCR-05` (`CurrentOrderSheet` / `SelfCheckoutPanel`) khi Chủ quán (`PER-06`) bật cờ trong `SCR-26` (`PaymentSettingsTab`).
- **IDs liên quan:** `F3.2`, `F3.3`, `F6.5` · `STG-05`, `STG-06` · `SCR-05`, `SCR-26` · `BR-17`, `BR-18`, `BR-19`, `BR-22` · `US-018`, `US-024`.
- **Ngoài phạm vi:** Thanh toán tiền mặt tại quầy (`PER-04` Cashier), báo cáo doanh thu (`SCR-22`), và luồng đặt món (`STG-02`).

## 2. Hành vi thực tế xác minh từ mã nguồn
1. **Điều kiện bật kép (`CustomerSelfCheckoutService.java:82-91`):** Tính năng chỉ hoạt động khi **đồng thời** `customer_self_checkout_enabled = true` (`V1.22`, mặc định `false`) **VÀ** `online_payment_enabled = true`. Nếu một trong hai tắt → backend ném `BusinessException(ErrorCode.FEATURE_DISABLED)`.
2. **Giới hạn phương thức & unfinished items (`CustomerSelfCheckoutService.java:36-45`):** Luôn ép `PaymentMethod.PAYOS` (không cho tiền mặt), nhận `voucherCode` + `idempotencyKey`, và truyền cờ `allowSettlementWithUnfinishedItems` từ `SystemSettings`.
3. **3 thao tác vòng đời giao dịch (`PublicSelfCheckoutController.java`):**
   - Khởi tạo VietQR: `POST /api/v1/public/tables/{tableCode}/self-checkout`
   - Hủy link QR đang chờ (vd. khách đổi voucher): `POST /api/v1/public/tables/{tableCode}/self-checkout/{transactionId}/cancel`
   - Đồng bộ trạng thái giao dịch: `GET /api/v1/public/tables/{tableCode}/self-checkout/{transactionId}` *(kể cả khi phiên bàn vừa đóng ngay sau webhook, nếu giao dịch đã `PAID` và đúng `tableCode` vẫn trả kết quả `PAID` — `CustomerSelfCheckoutService.java:67-70`)*.
4. **Bảo vệ chống spam (`application.yml:157-158`):** Áp dụng rate limit scope riêng `public:self-checkout` trên `/api/v1/public/tables/*/self-checkout/**`.
5. **Khôi phục nháp trên thiết bị khách (`useCustomerSelfCheckout.js`, `selfCheckoutHelpers.js`):** Lưu draft theo key `qros:self-checkout:{tableCode}:{orderId}` để khách tải lại trang không mất mã QR đang chờ.

---

## 3. Sync-Back Proposal (Chờ người dùng duyệt — CHƯA ghi vào nguồn chân lý)

*Mỗi đề xuất chỉ cập nhật đúng loại tri thức thuộc vai trò của từng tầng (Design nhận hành vi UI, BA nhận quy tắc nghiệp vụ/AC, Tech nhận tên cột DB/API/rate limit).*

| # | ID / Phạm vi ảnh hưởng | File nguồn chân lý | Điểm lệch phát hiện khi đối chiếu code thật | Diff đề xuất |
|---|---|---|---|---|
| **SB-01** | `BR-19`, `STG-05` | [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md#L135-L140) | `BR-19` mới nêu điều kiện Chủ quán bật công tắc tự thanh toán tại `SCR-26`, chưa nêu ràng buộc kép là **cả thanh toán trực tuyến VietQR cũng phải đang bật** và tuân theo cấu hình cho phép chốt đơn khi còn món chưa hoàn tất. | Bổ sung vào `BR-19`: chế độ tự thanh toán tại bàn chỉ khả dụng khi **đồng thời** bật cả *Thanh toán trực tuyến* lẫn *Tự thanh toán tại bàn* (mặc định tắt), và áp dụng cùng chính sách chốt đơn khi còn món chưa hoàn tất. |
| **SB-02** | `US-018` | [USER_STORIES.md](../../03-ba/USER_STORIES.md#L178-L185) | AC 3 mới nêu hủy mã QR để đổi sang tiền mặt; chưa nêu 2 tình huống thực tế đã triển khai: *(a)* khách hủy mã QR đang chờ để **đổi mã voucher**, và *(b)* kiểm tra lại trạng thái thanh toán vẫn trả về *Đã thanh toán* ngay cả khi phiên bàn vừa tự đóng sau xác nhận chuyển khoản. | Bổ sung 2 nhánh kiểm chứng trên vào Acceptance Criteria của `US-018`. |
| **SB-03** | `SCR-05` | [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md#L35) | Mô tả `SCR-05` đã nêu khung tự thanh toán VietQR + nhập voucher, nhưng chưa ghi rõ hành vi giữ nháp phiên mã QR khi khách tải lại trang và nút hủy mã QR để đổi voucher trong ngăn kéo đơn hiện tại. | Bổ sung vào cột thành phần UI của `SCR-05`: hỗ trợ lưu nháp mã QR đang chờ khi tải lại trang và nút hủy mã QR để đổi voucher. |
| **SB-04** | API Contract, DB & Rate Limit | [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md#L145-L187) | §6.1 Public API chưa liệt kê 3 endpoint của `PublicSelfCheckoutController`, chưa ghi tên cột chính xác `customer_self_checkout_enabled` (`V1.22`) và rate-limit scope riêng `public:self-checkout`. | Bổ sung 3 endpoint `/api/v1/public/tables/{tableCode}/self-checkout*`, cột `customer_self_checkout_enabled` (`V1.22`) và scope rate limit `public:self-checkout` vào `SYSTEM_DESIGN.md`. |

> **Hành động tiếp theo:** Người dùng duyệt từng dòng `SB-01` → `SB-04`. Chỉ sau khi duyệt, agent mới ghi diff vào các file nguồn chân lý, thêm 1 dòng vào `Sync Changelog` trong `KNOWLEDGE_HUB.md` và đổi `Status` tài liệu này sang `Archived`.
