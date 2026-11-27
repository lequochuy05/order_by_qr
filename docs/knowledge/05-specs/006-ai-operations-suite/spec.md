# Spec `006`: AI Operations Suite (Restock, Merchandising, Note Safety & BOM Costing)

- **Type:** `New Feature`
- **Branch / Track:** `Nhánh A (Full SDD)` *(Bổ sung migration `V1.24`, 4 nhóm API AI mới có phân quyền RBAC/quota theo `STAFF`/`CHEF`/`MANAGER`/`ADMIN`, các quy tắc `BR-32` → `BR-35`, và cập nhật UI trên `SCR-11`, `SCR-12`, `SCR-17`, `SCR-18`, `SCR-20`, `SCR-27`)*
- **Status:** `Archived` *(Draft → In Progress → Implemented (Pending Sync-Back Review) → Archived)*
- **Source of Truth:**
  - PO: [journey-features.md](../../01-po/journey-features.md) (`F2.6`, `F5.6`, `F5.7`, `F6.7`) · [personas.md](../../01-po/personas.md) (`PER-02`..`PER-06`)
  - Design: [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-11`, `SCR-12`, `SCR-17`, `SCR-18`, `SCR-20`, `SCR-27`) · [DESIGN_SYSTEM.md](../../02-design/DESIGN_SYSTEM.md)
  - BA PRD: [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md) (`STG-03`, `STG-04`, `STG-06`, `BR-32` → `BR-35`)
  - BA User Stories: [USER_STORIES.md](../../03-ba/USER_STORIES.md) (`US-030` → `US-033`)
  - Tech: [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md)

---

## 1. Phạm vi & Truy vết
- **4 Năng lực AI vận hành tích hợp:**
  1. **AI Gợi ý nhập kho thông minh (`F5.6`, `SCR-18`, `BR-32`, `US-030`):** BOM × sức bán 30 ngày × khung `1..30 ngày` × buffer 10%, tính thiếu hụt, số ngày còn đủ bán & chi phí nhập dự kiến theo `unit_cost`, nút *"Nhập nhanh"*.
  2. **AI Đề xuất Combo & Khung giờ Khuyến mãi (`F6.7`, `SCR-17`/`SCR-20`, `BR-33`, `US-031`):** Phân tích cặp món hay gọi chung (60 ngày) và lưu lượng đơn theo giờ/ngày trong tuần (30 ngày); kiểm chứng sống danh mục & mở draft 1 chạm trước khi Quản lý lưu.
  3. **AI Cảnh báo dị ứng & Ưu tiên ghi chú tự do (`F2.6`, `SCR-12`/`SCR-27`/`SCR-11`, `BR-34`, `US-032`):** Phân tích 2 lớp (deterministic `<1ms` + AI deep triage); bất biến an toàn thực phẩm không hạ cấp `CRITICAL`.
  4. **AI Gợi ý công thức BOM & Kiểm tra giá vốn (`F5.7`, `SCR-18`, `BR-35`, `US-033`):** Thêm `inventory_items.unit_cost` (`V1.24`), tính trực tiếp COGS % & biên lợi nhuận (`>45%` CRITICAL, `>35%` WARNING), gợi ý BOM đối chiếu nguyên liệu sống (`PER-05`/`PER-06` áp dụng & lưu; `PER-03` chỉ xem).
- **Ngoài phạm vi (Out of Scope):**
  - Không tự động tạo đơn đặt hàng nhà cung cấp hay tự động phát hành Combo/Khuyến mãi không qua xác nhận Quản lý.
  - Không làm hỏng các migration cũ `V1`–`V1.23`.
