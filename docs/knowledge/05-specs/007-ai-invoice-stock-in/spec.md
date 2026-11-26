# Spec `007`: Nhập kho tự động bằng chụp ảnh hóa đơn — Gemini Vision

- **Type:** `New Feature`
- **Branch / Track:** `Nhánh A (Full SDD)` *(Tích hợp Gemini Vision `inline_data`, endpoint trích xuất ảnh hóa đơn `POST /api/v1/ai/inventory/invoice-ocr`, endpoint nhập kho nguyên tử `POST /api/v1/inventory/items/batch-stock-in`, quy tắc `BR-36` và modal rà soát trên `SCR-18`)*
- **Status:** `Archived` *(Draft → In Progress → Implemented (Pending Sync-Back Review) → Archived)*
- **Source of Truth:**
  - PO: [journey-features.md](../../01-po/journey-features.md) (`F5.8`) · [personas.md](../../01-po/personas.md) (`PER-03`, `PER-05`, `PER-06`)
  - Design: [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-18`) · [DESIGN_SYSTEM.md](../../02-design/DESIGN_SYSTEM.md)
  - BA PRD: [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md) (`STG-03`, `STG-06`, `BR-11`, `BR-21`, `BR-24`, `BR-36`)
  - BA User Stories: [USER_STORIES.md](../../03-ba/USER_STORIES.md) (`US-034`)
  - Tech: [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md)

---

## 1. Phạm vi & Nguyên tắc an toàn
- **Chức năng cốt lõi (`F5.8`, `US-034`, `BR-36`):**
  1. Cho phép Quản lý (`PER-05`), Chủ quán (`PER-06`) và Đầu bếp (`PER-03`) chụp ảnh hoặc tải lên hóa đơn (`JPEG`/`PNG`/`WebP` ≤ 5MB) ngay trên màn hình Kho `SCR-18`.
  2. Gemini Vision đọc thông tin nhà cung cấp, số/ngày hóa đơn và danh sách dòng hàng; hệ thống tự đối chiếu danh mục nguyên liệu đang hoạt động, tự quy đổi đơn vị (`kg ↔ g`, `l ↔ ml`) và gắn cảnh báo `UNIT_MISMATCH` / `UNMATCHED` cho các dòng cần chỉnh tay.
  3. **Human-in-the-Loop bắt buộc:** Không tự động cộng kho sau khi OCR; người dùng chọn dòng, chỉnh lại nguyên liệu/số lượng và xác nhận nhập kho nguyên tử 1 lần.
  4. **Phân quyền giá vốn:** Chỉ `PER-05` / `PER-06` được cập nhật `unit_cost` từ hóa đơn (kiểm tra cả trên UI lẫn server-side RBAC).
- **Ngoài phạm vi:**
  - Không lưu ảnh hóa đơn công khai lên Cloudinary/disk khi chỉ phục vụ trích xuất OCR trong phiên.
  - Không tự tạo mới `InventoryItem` chưa tồn tại trong danh mục từ kết quả OCR chưa kiểm duyệt.
