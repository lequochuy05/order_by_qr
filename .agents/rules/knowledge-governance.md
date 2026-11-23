---
trigger: always_on
---

# QROS Knowledge Governance & Cross-Role Linkage Rules

Toàn bộ tri thức sản phẩm và kỹ thuật của dự án **QROS** được quản lý tập trung tại một nguồn chân lý duy nhất (**Single Source of Truth**): `docs/knowledge/`.

## 1. Quy tắc tập trung Knowledge & Phân tách ngôn ngữ từng vai trò
- **Quy luật chiều thuận là đường chính:** Mọi tính năng mới đi từ **PO (`01-po`) → Design (`02-design`) → BA (`03-ba`) → Tech/SE (`04-tech`) → SDD Execution (`05-specs`) → Sinh Code**.
- Mọi vai trò bắt buộc đọc, ghi vào đúng tầng trong `docs/knowledge/` và **giữ sạch ranh giới ngôn ngữ của vai trò đó**:
  - `docs/knowledge/KNOWLEDGE_HUB.md` — Chỉ mục trung tâm, ma trận truy vết và cổng bàn giao (Handoff Gates).
  - `docs/knowledge/01-po/` — Thuần **ngôn ngữ sản phẩm & kinh doanh** (Intake, Field Research, Problem, Personas, Vision, Market, Business/Service Blueprint, Roadmap, Journey-Features). **Cấm** đưa tên framework, thư viện, API endpoint, tên bảng SQL hay số migration vào tầng PO.
  - `docs/knowledge/02-design/` — Thuần **ngôn ngữ UI/UX & Design Tokens** (`DESIGN_SYSTEM.md`, `figma-tokens.json`, `PROTOTYPE_SCREEN_MAP.md`, tài nguyên Figma). Không lẫn tên file `.jsx`, bảng SQL hay chi tiết backend.
  - `docs/knowledge/03-ba/` — Thuần **ngôn ngữ nghiệp vụ, quy tắc & kiểm thử** (`PRD_BY_USER_STAGE.md`, `USER_STORIES.md`). Không quyết định kiến trúc, REST endpoint, tên bảng SQL hay tên component code (thuộc Tầng 4).
  - `docs/knowledge/04-tech/` — **Kiến trúc kỹ thuật** (`SYSTEM_DESIGN.md`), ánh xạ `US/BR/SCR` xuống Module, API, WebSocket, DB Schema và CI/CD.
  - `docs/knowledge/05-specs/` — Hiến pháp kỹ thuật (`CONSTITUTION.md`) và lịch sử thực thi theo tính năng (áp dụng phương pháp Spec-Driven Development, lấy cảm hứng từ Spec Kit). Các file trong `05-specs/` chỉ là lịch sử thực thi (`Draft → In Progress → Implemented → Archived`), **không phải** bản sao thứ hai của `03-ba/` hay `04-tech/`.

## 2. Quy tắc liên kết bắt buộc giữa các khâu (Mandatory Input Gates)
Không vai trò nào được phép làm việc độc lập mà bỏ qua nguồn Knowledge của khâu trước:
1. **Design Skill** (`design-system-prototype`) **BẮT BUỘC** đọc `docs/knowledge/01-po/` trước khi thiết kế.
2. **BA PRD Skill** (`ba-prd`) **BẮT BUỘC** đọc cả `docs/knowledge/01-po/` **VÀ** `docs/knowledge/02-design/` (`DESIGN_SYSTEM.md` + `PROTOTYPE_SCREEN_MAP.md`) trước khi phân tích yêu cầu.
3. **BA User Story Skill** (`ba-user-story`) **BẮT BUỘC** đọc `01-po/` + `02-design/PROTOTYPE_SCREEN_MAP.md` + `03-ba/PRD_BY_USER_STAGE.md`.
4. **System Design / SE Skill** (`se-system-design`) **BẮT BUỘC** đọc `01-po/` + `02-design/` + `03-ba/` trước khi thiết kế kỹ thuật hoặc sinh code.
5. **SDD Coding & Sync Skill** (`sdd-coding-sync`) **BẮT BUỘC** khai báo rõ nhánh ngay đầu phiên:
   - **Nhánh A (Tính năng mới / đụng vùng nhạy cảm):** Bắt buộc có `US/BR` tối thiểu ở `03-ba/` (và `SCR` nếu có UI) trước khi vào `specify`. Thay đổi đụng **auth, thanh toán/webhook, phân quyền RBAC, kho/locking hoặc idempotency** không được đi đường nhẹ; khi phân vân mặc định chọn Nhánh A.
   - **Nhánh B (Fast Track):** Chỉ áp dụng cho bugfix nhỏ/style/refactor nội bộ không đổi API, DB, `BR-*`, `SCR-*` và không đụng vùng nhạy cảm.
   - **Nhánh C (Brownfield Baseline / Sync-Back):** Áp dụng Just-in-Time cho tính năng đã có code sẵn (*chạm đến module nào thì dựng module đó*), ghi rõ là *Baseline Spec dựng ngược từ code*.

> Nếu phát hiện thiếu tài liệu đầu vào của khâu trước, agent phải **DỪNG LẠI**, liệt kê file còn thiếu và yêu cầu bổ sung thay vì tự suy đoán.

## 3. Quy tắc Sync-Back có kiểm duyệt (Human-in-the-Loop)
- Agent **TUYỆT ĐỐI KHÔNG tự ý ghi đè** `01-po/`, `02-design/`, `03-ba/`, `04-tech/` hay `KNOWLEDGE_HUB.md` khi đồng bộ ngược từ code.
- Thay vào đó, agent phải xuất **Sync-Back Proposal** liệt kê các ID bị ảnh hưởng và diff đề xuất; chỉ ghi sau khi người dùng duyệt, đồng thời bổ sung **1 dòng vào bảng `Sync Changelog`** trong `docs/knowledge/KNOWLEDGE_HUB.md`.

## 4. Quy ước Mã Truy Vết (Traceability IDs)
Mọi tài liệu mới hoặc chỉnh sửa phải duy trì chuỗi truy vết xuyên suốt:
- PO Feature: `F1.1` .. `F6.5` (6 Epics trong `01-po/journey-features.md`)
- User Stage: `STG-01` .. `STG-06`
- Persona / Role: `PER-01` (Customer), `PER-02` (Staff), `PER-03` (Chef), `PER-04` (Cashier), `PER-05` (Manager), `PER-06` (Admin)
- Design Screen: `SCR-01` .. `SCR-26`
- Business Rule: `BR-01` .. `BR-24` (mở rộng tiếp `BR-xx`)
- User Story: `US-001` .. `US-024` (mở rộng tiếp `US-xxx`)
