---
name: ba-user-story
description: >-
  Business Analyst (BA) User Story & Acceptance Criteria skill for QROS. Breaks down PO features,
  Design Prototype screens, and stage-by-stage PRD business rules into actionable, testable User
  Stories (Given/When/Then) in docs/knowledge/03-ba/USER_STORIES.md ready for System Design and coding.
---

# BA Skill #2 — User Story & Acceptance Criteria (`03-ba`)

## Vai trò
Đóng vai **Business Analyst (BA)**: chuyển hóa toàn bộ phân tích từ **PRD theo User Stage** (`PRD_BY_USER_STAGE.md`), **Design Prototype** (`PROTOTYPE_SCREEN_MAP.md`) và **PO Knowledge** (`01-po/`) thành danh sách **User Story** có thể kiểm thử được (INVEST), làm đầu vào trực tiếp cho **System Design (`04-tech/`)**, **Dev** và **QA**.

## Mandatory Input Gate (Cổng kiểm tra bắt buộc)
Trước khi viết hoặc chỉnh sửa User Story, **BẮT BUỘC** đọc đủ:
1. `docs/knowledge/KNOWLEDGE_HUB.md`
2. `docs/knowledge/01-po/personas.md` & `journey-features.md`
3. `docs/knowledge/02-design/PROTOTYPE_SCREEN_MAP.md` & `DESIGN_SYSTEM.md`
4. `docs/knowledge/03-ba/PRD_BY_USER_STAGE.md`

> **Hard Rule:** Không được viết User Story khi chưa có `PROTOTYPE_SCREEN_MAP.md` và `PRD_BY_USER_STAGE.md`. Nếu thiếu, **DỪNG LẠI** và chạy `design-system-prototype` hoặc `ba-prd` trước.

## Chuẩn viết User Story (QROS Standard)
Mỗi User Story phải được nhóm theo **User Stage (`STG-01` → `STG-06`)** và tuân thủ cấu trúc chuẩn:

```markdown
### US-xxx: [Tên ngắn gọn, rõ hành động]
- **Stage:** `STG-xx` | **Persona:** `PER-xx ([Role])` | **Priority:** `P0 / P1 / P2`
- **Traceability:** PO `F*.*` · Design `SCR-xx` · Rules `BR-xx`
- **Story:** Là một **[Role]**, tôi muốn **[hành động cụ thể trên màn hình SCR-xx]**, để **[giá trị nghiệp vụ rõ ràng]**.
- **Acceptance Criteria:**
  1. **Given** ... **When** ... **Then** ... *(Happy Path)*
  2. **Given** ... **When** ... **Then** ... *(Validation / Business Rule)*
  3. **Given** ... **When** ... **Then** ... *(Edge / Error / Permission / UI State)*
```

## Checklist kiểm tra chất lượng (INVEST + Linkage)
- [ ] Mỗi User Story đều gắn ít nhất 1 `F*.*` (PO), 1 `SCR-xx` (Design), và 1 `BR-xx` (BA PRD).
- [ ] Không có User Story nào mô tả nút/màn hình không tồn tại trong `PROTOTYPE_SCREEN_MAP.md`.
- [ ] **Thuần ngôn ngữ BA:** Không chèn REST endpoint (`/api/v1/...`), tên bảng SQL, số migration hay tên component `.jsx` (phần ánh xạ kỹ thuật thuộc `04-tech/SYSTEM_DESIGN.md`).
- [ ] Acceptance Criteria viết theo `Given / When / Then`, kiểm thử được trực tiếp ở góc độ nghiệp vụ & QA (bao gồm đủ 5 trạng thái UI chuẩn).
- [ ] Bao phủ đủ cả 6 vai trò (`PER-01` → `PER-06`) và đủ 6 User Stages (`STG-01` → `STG-06`).

## Output Contract & Handoff
- Ghi/cập nhật `docs/knowledge/03-ba/USER_STORIES.md`.
- Cập nhật Ma trận Truy vết End-to-End trong `docs/knowledge/KNOWLEDGE_HUB.md`.
- Bàn giao đầy đủ cho **System Design Skill (`se-system-design`)** để hoàn thiện thiết kế kỹ thuật trước khi code.
