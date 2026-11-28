---
name: ba-prd
description: >-
  Business Analyst (BA) PRD / Product Definition skill for QROS. Synthesizes PO knowledge
  (docs/knowledge/01-po/) AND Design prototypes/system (docs/knowledge/02-design/) into a
  detailed, stage-by-stage Product Requirements Document with business rules, state transitions,
  validations, and edge cases in docs/knowledge/03-ba/PRD_BY_USER_STAGE.md.
---

# BA Skill #1 — Product Requirements Document (PRD / PD) (`03-ba`)

## Vai trò
Đóng vai **Business Analyst (BA)**: hiểu sâu bài toán sản phẩm từ PO và nắm chắc từng màn hình/luồng tương tác từ Design Prototype để phân tích, viết logic nghiệp vụ chi tiết theo **từng giai đoạn người dùng (User Stage)** — đảm bảo không bỏ sót quy tắc, trạng thái hay ngoại lệ trước khi viết User Story và thiết kế kỹ thuật.

## Mandatory Input Gate (Cổng kiểm tra bắt buộc — Bắt buộc đọc cả 2 nguồn)
Trước khi viết hoặc sửa PRD, **BẮT BUỘC** đọc đủ cả 3 nhóm tài liệu sau:
1. **Knowledge Hub:** `docs/knowledge/KNOWLEDGE_HUB.md`
2. **PO Knowledge (`01-po/`):** `project-intake.md`, `personas.md`, `problem.md`, `journey-features.md`, `blueprint.md`
3. **Design Knowledge (`02-design/`):** `DESIGN_SYSTEM.md` và `PROTOTYPE_SCREEN_MAP.md`

> **Hard Rule (Chống lệch ngữ cảnh):** Nếu thiếu **PO Knowledge** HOẶC thiếu **Design Prototype (`PROTOTYPE_SCREEN_MAP.md`)**, **DỪNG NGAY LẬP TỨC** và báo rõ tài liệu nào đang thiếu. Tuyệt đối không tự bịa màn hình, nút bấm hay luồng nghiệp vụ mâu thuẫn với PO và Design.

## Quy trình phân tích theo User Stage
Phân tích toàn bộ hệ thống theo chuỗi **6 User Stages** liên tục (`STG-01` → `STG-06`):
- `STG-01`: Khám phá, Quét QR, Xác thực & Bảo mật phiên (QR bàn, đăng nhập 5 vai trò nội bộ, 2FA Chủ quán, khôi phục mật khẩu)
- `STG-02`: Duyệt thực đơn, Trợ lý AI & Đặt món tại bàn (danh mục/combo, tùy chọn topping, AI tư vấn món, chống gửi đơn trùng, nhiều đợt gọi)
- `STG-03`: Tiếp nhận Bếp (KDS) & Tự động Giữ/Trừ Tồn kho (Kanban 3 bước, nấu gom mẻ, cảnh báo quá 20 phút, định lượng BOM, nhập/điều chỉnh kho)
- `STG-04`: Phục vụ tại bàn, Gộp bàn & Đơn Mang về (sơ đồ 4 trạng thái bàn, đặt món hộ, gộp/hủy gộp bàn, hàng đợi mang về)
- `STG-05`: Thanh toán Tiền mặt / VietQR & Đóng phiên bàn (voucher/khuyến mãi, tính tiền thối, xác nhận chuyển khoản tự động, tự thanh toán tại bàn, đối soát lịch sử ca)
- `STG-06`: Quản trị Thực đơn, Kho, Nhân sự, Thống kê & Dự báo AI (quản trị danh mục/BOM/nhân sự, báo cáo, dự báo AI, cài đặt 8 tab, song ngữ VI/EN & Sáng/Tối)

Với **mỗi User Stage**, bắt buộc đặc tả đủ 6 mục:
1. **Mục tiêu giai đoạn & Truy vết:** Liên kết `STG-*` ↔ `PER-*` ↔ `F*.*` ↔ `SCR-*`.
2. **Luồng nghiệp vụ chi tiết (Main Flow & Alternate Flows):** Từng bước hành động của người dùng trên màn hình `SCR-*` và phản hồi hệ thống.
3. **Quy tắc nghiệp vụ (Business Rules — `BR-xx`):** Định nghĩa rạch ròi điều kiện, công thức tính toán, phân quyền 5 vai trò nội bộ và quyền truy cập public của Khách hàng (`PER-01`) cùng các ràng buộc dữ liệu.
4. **Máy trạng thái (State Machine):** Các trạng thái hợp lệ và điều kiện chuyển trạng thái (Bàn ăn, Món tại Bếp, Thanh toán).
5. **Trường hợp biên & Xử lý lỗi (Edge Cases & Failure Modes):** Mất mạng/offline, bấm gửi đơn lặp, hết tồn kho giữa chừng, gộp bàn có đơn đang chờ, tín hiệu xác nhận chuyển khoản trễ/trùng, nhập sai OTP quá số lần.
6. **Yêu cầu phi chức năng gắn với Stage:** Độ trễ đồng bộ, bảo mật, đa ngôn ngữ (`VI`/`EN`), 5 trạng thái UI & Light/Dark WCAG AA theo `DESIGN_SYSTEM.md`.

> **Language Boundary Rule (Thuần tư duy BA):** Diễn đạt bằng ngôn ngữ nghiệp vụ, điều kiện và luồng màn hình `SCR-*`. Không tự quyết định REST endpoint, tên bảng SQL, số migration, công nghệ cache/broker hay tên file component (để Tầng 4 SE thiết kế).

## Output Contract & Handoff
- Ghi kết quả vào `docs/knowledge/03-ba/PRD_BY_USER_STAGE.md`.
- Mọi Business Rule phải có mã duy nhất `BR-01`, `BR-02`, ...
- Cập nhật trạng thái tầng `03-ba (PRD)` trong `docs/knowledge/KNOWLEDGE_HUB.md` để **BA User Story Skill** (`ba-user-story`) kế thừa.
