---
name: sdd-coding-sync
description: >-
  Spec-Driven Development (SDD, inspired by GitHub Spec Kit) workflow for QROS implementation
  and knowledge sync-back. Activate ONLY when the user explicitly asks to: (1) specify, plan,
  break into tasks, or implement a feature against docs/knowledge/, (2) run a Fast Track task
  with DoD for a small non-sensitive bugfix/refactor, or (3) create a reverse-engineered baseline
  spec or run sync-back reconciliation between existing code and docs/knowledge/. Do NOT activate
  for general Q&A, pure code reading, or standalone documentation edits.
---

# SDD Coding & Knowledge Sync Skill (`05-specs`)

## Vai trò
Áp dụng **phương pháp Spec-Driven Development (SDD), lấy cảm hứng từ GitHub Spec Kit** làm cầu nối thực thi theo **chiều thuận** từ Knowledge (`01-po` → `02-design` → `03-ba` → `04-tech`) → Spec/Plan/Tasks (`05-specs`) → **Sinh ra Code** (`backend/`, `frontend/`). Nhánh Baseline dựng ngược từ code (Nhánh C) chỉ là công cụ bổ trợ ngoại lệ cho phần code có sẵn theo nguyên tắc Just-in-Time, không thay thế quy trình chiều thuận.

---

## Bước 0 — Khai báo Nhánh & Kiểm tra Input Gate (BẮT BUỘC ngay đầu phiên)

Ngay đầu mỗi phiên làm việc, agent **PHẢI khai báo rõ đang chọn nhánh nào** (Nhánh A / B / C) và lý do, để người dùng có thể xác nhận hoặc đổi nhánh trước khi đi tiếp.

| Nhánh | Điều kiện áp dụng | Cổng đầu vào (Mandatory Input Gate) |
|---|---|---|
| **Nhánh A — Tính năng mới (Full SDD)** | Đụng bất kỳ yếu tố nào sau đây: **API, DB migration, Business Rule (`BR-*`), Màn hình (`SCR-*`)**, HOẶC đụng vùng nhạy cảm (**auth, thanh toán/webhook, phân quyền RBAC, kho/locking, idempotency**). *Khi phân vân → mặc định chọn Nhánh A.* | Đọc `KNOWLEDGE_HUB.md`, `05-specs/CONSTITUTION.md`, và xác minh đã có `US-xxx`/`BR-xx` tối thiểu ở `03-ba/` (và `SCR-xx` ở `02-design/` nếu có UI). **Thiếu → DỪNG**, yêu cầu chạy `ba-prd` / `ba-user-story` / `design-system-prototype` trước. |
| **Nhánh B — Đường nhẹ (Fast Track)** | Bugfix nhỏ, chỉnh style, refactor nội bộ **KHÔNG đổi** API, DB, `BR-*`, `SCR-*` và **KHÔNG đụng** auth, thanh toán, phân quyền, kho hay idempotency. | Đọc `05-specs/CONSTITUTION.md`. Bỏ `specify` & `plan`, chỉ tạo checklist `tasks` ngắn + chạy DoD. **Chỉ chạy Sync-Back nếu phát sinh ID bị ảnh hưởng.** |
| **Nhánh C — Brownfield Baseline / Sync-Back** | Tính năng **đã có code sẵn** trong repo cần lập baseline hoặc rà soát lệch với `docs/knowledge/`. Áp dụng nguyên tắc **Just-in-Time ("chạm đến module nào thì dựng module đó")** — không dựng baseline hàng loạt cho toàn bộ 16 module. | Đọc mã nguồn thật + đối chiếu `02-design/`, `03-ba/`, `04-tech/`. Ghi rõ đầu file `Type: Baseline Spec (dựng ngược từ code có sẵn)`. |

---

## Quy trình 5 Pha (Nhánh A) & Vòng đời `05-specs/`

Mọi thư mục `docs/knowledge/05-specs/<NNN-feature>/` chỉ là **lịch sử thực thi theo tính năng**, KHÔNG phải bản sao thứ hai của `03-ba/` hay `04-tech/`.

- **Vòng đời bắt buộc:** `Draft` → `In Progress` → `Implemented (Pending Sync-Back Review)` → `Archived` *(chỉ chuyển `Archived` sau khi người dùng duyệt Sync-Back và đã ghi changelog)*.
- **Header bắt buộc trong `spec.md`:** `Type`, `Status`, `Source of Truth` (trỏ link tới `03-ba/` & `04-tech/`).

### Pha 1 — `specify` (dùng `templates/spec.template.md`)
- Tạo `docs/knowledge/05-specs/<NNN-feature>/spec.md`.
- Chỉ **tham chiếu ID** (`F*.*`, `SCR-*`, `BR-*`, `US-*`) từ `docs/knowledge/`, không sao chép lại toàn văn PRD/User Story.
- Liệt kê câu hỏi mở cần chốt (`Clarifications`) trước khi sang `plan`.

### Pha 2 — `plan` (dùng `templates/plan.template.md`)
- Tạo `plan.md`: liệt kê chính xác file thêm/sửa ở `backend/` và `frontend/`, hợp đồng REST/WS, migration Flyway (kiểm tra số phiên bản mới nhất trong `backend/src/main/resources/db/migration/` rồi tạo tiếp `V1.xx__*.sql`), và bảng rà soát nhanh với `05-specs/CONSTITUTION.md`.

### Pha 3 — `tasks` (dùng `templates/tasks.template.md`)
- Tạo `tasks.md` chia theo lớp, **test bắt buộc đi kèm ngay trong từng nhóm** (không dồn test về cuối):
  1. **Nhóm 1 — Database & Migration** (+ test verify schema/migration).
  2. **Nhóm 2 — Backend** (+ Unit / Integration / Security test ngay trong nhóm + `mvn verify`).
  3. **Nhóm 3 — Frontend** (+ Unit test `node:test`, `lint`, `build` ngay trong nhóm).
  4. **Nhóm 4 — Verification & Sync-Back Proposal**.

### Pha 4 — `implement`
- Đổi `Status` sang `In Progress`.
- Code tuần tự theo từng nhóm task, chạy kiểm thử ngay sau mỗi nhóm và tick `[x]` chỉ khi lệnh verify thực sự pass.
- Chạy trọn bộ DoD khớp CI trong `05-specs/CONSTITUTION.md` (`mvn -f backend/pom.xml --batch-mode verify` và `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`).

### Pha 5 — `sync-back` (Human-in-the-Loop — CẤM tự ghi đè)
- Đổi `Status` sang `Implemented (Pending Sync-Back Review)`.
- **TUYỆT ĐỐI KHÔNG tự ý sửa** `01-po/`, `02-design/`, `03-ba/`, `04-tech/` hay `KNOWLEDGE_HUB.md`.
- Xuất **Sync-Back Proposal** gồm:
  1. Bảng các ID bị ảnh hưởng (`ID | File nguồn chân lý | Điểm lệch giữa Code & Doc | Diff đề xuất`).
  2. Chờ người dùng duyệt từng mục (chấp nhận / chỉnh sửa / bỏ qua).
- **Chỉ sau khi người dùng duyệt:** áp dụng các diff đã duyệt vào tài liệu gốc, thêm **1 dòng vào bảng `Sync Changelog`** cuối `docs/knowledge/KNOWLEDGE_HUB.md`, và chuyển `Status` của spec sang `Archived`.
