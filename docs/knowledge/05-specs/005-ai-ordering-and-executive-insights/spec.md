# Spec `005`: Actionable Customer AI Ordering & AI Executive Insights

- **Type:** `New Feature`
- **Branch / Track:** `Nhánh A (Full SDD)` *(Thay đổi API contract công khai `/api/v1/public/ai/chat`, bổ sung endpoint `/api/v1/ai/insights/summary` có RBAC & quota, bổ sung quy tắc `BR-29` → `BR-31`, `US-028` → `US-029`, và cập nhật UI trên `SCR-05`, `SCR-10`)*
- **Status:** `Archived` *(Draft → In Progress → Implemented (Pending Sync-Back Review) → Archived)*
- **Source of Truth:**
  - PO: [journey-features.md](../../01-po/journey-features.md) (`F1.5`, `F6.6`) · [personas.md](../../01-po/personas.md) (`PER-01`, `PER-05`, `PER-06`)
  - Design: [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-05`, `SCR-10`) · [DESIGN_SYSTEM.md](../../02-design/DESIGN_SYSTEM.md)
  - BA PRD: [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md) (`STG-02`, `STG-06`, `BR-29` → `BR-31`)
  - BA User Stories: [USER_STORIES.md](../../03-ba/USER_STORIES.md) (`US-028`, `US-029`)
  - Tech: [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md)

---

## 1. Phạm vi & Truy vết
- **Mục tiêu ngắn gọn:**
  1. **Đặt món trực tiếp từ Trợ lý AI (`PER-01` · `SCR-05` · `F1.5`):** Nâng cấp khung chat AI của thực khách để trả lời kèm tối đa 5 thẻ món ăn hoặc combo **đang mở bán và còn hàng** theo dữ liệu sống từ DB, cho phép bấm `+ Thêm` hoặc `Chọn tùy chọn` (tự mở `ItemOptionsModal`) trực tiếp vào giỏ hàng dùng chung.
  2. **AI Tóm tắt ca / Báo cáo điều hành thông minh (`PER-05`, `PER-06` · `SCR-10` · `F6.6`):** Bổ sung thẻ phân tích điều hành AI theo yêu cầu ngay trên Dashboard `/admin/dashboard` cho khung thời gian *Hôm nay / 7 ngày / 30 ngày* (tối đa 90 ngày), tổng hợp KPI & điểm nghẽn thực tế (doanh thu so kỳ trước, top món, đơn hủy, bếp chờ > 20 phút, yêu cầu bàn quá SLA > 5 phút, tồn kho thấp) kết hợp Gemini AI và cơ chế dự phòng xác định theo quy tắc khi AI gián đoạn.
- **IDs liên quan:** `F1.5`, `F6.6` · `STG-02`, `STG-06` · `PER-01`, `PER-05`, `PER-06` · `SCR-05`, `SCR-10` · `BR-01`, `BR-04`, `BR-05`, `BR-08`, `BR-24`, `BR-29`, `BR-30`, `BR-31` · `US-028`, `US-029`.
- **Ngoài phạm vi (Out of Scope):**
  - Không thay đổi schema DB hay thêm Flyway migration mới (sử dụng dữ liệu tổng hợp hiện có).
  - Không cho phép AI tự động gửi thẳng đơn xuống bếp bỏ qua xác nhận giỏ hàng của khách (`BR-30`).
  - Không cho phép `PER-02 (Staff)`, `PER-03 (Chef)`, `PER-04 (Cashier)` truy cập API hoặc UI tóm tắt điều hành (`BR-31`).

## 2. Làm rõ yêu cầu (Clarifications & Defaults)
- [x] **C1 — Chuẩn hóa dữ liệu thẻ món AI (`BR-29`):** LLM chỉ đề xuất `type` (`MENU_ITEM` | `COMBO`), `id`, `quantity`, `reason`; Backend đối chiếu live DB qua `findAllPublicAvailableItems` + `findAllActiveWithItems` để lấy `name`, `price`, `imageUrl`, `categoryName`, `hasOptions`, `available=true`, loại bỏ ID ảo hoặc món hết hàng, giới hạn tối đa 5 thẻ.
- [x] **C2 — Luồng tùy chọn món khi thêm từ Chat (`BR-30`):** Nếu món có nhóm tùy chọn (`hasOptions=true`), bấm nút trên thẻ chat tự đóng/ẩn tạm khung chat và mở `ItemOptionsModal` để khách chọn kích cỡ/topping trước khi vào giỏ.
- [x] **C3 — Giới hạn ngày & dự phòng điều hành (`BR-31`):** API `/api/v1/ai/insights/summary` mặc định 7 ngày gần nhất, giới hạn tối đa 90 ngày, kiểm soát quota riêng (`ai.quota.insights-user`: `10/1m`, `80/1d`), và tự tạo tóm tắt xác định theo quy tắc (`aiGenerated=false`) khi Gemini tắt, lỗi mạng hoặc sai JSON.
