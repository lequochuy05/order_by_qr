# Execution Tasks `<NNN>`: `<Tên tính năng>`

- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD bắt buộc (khớp CI):**
  - Backend: `mvn -f backend/pom.xml --batch-mode verify`
  - Frontend: `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Database & Migration (kèm test)
- [ ] Tạo migration `V1.xx__*.sql` (chỉ thêm mới, không sửa migration cũ)
- [ ] Kiểm chứng schema & mapping entity qua test backend liên quan

## Nhóm 2 — Backend Implementation (kèm test ngay trong nhóm)
- [ ] Triển khai Model / DTO / Service / Controller / Security & Rate Limit
- [ ] Viết/cập nhật Unit & Integration Test cho luồng mới (gồm happy path, lỗi & phân quyền)
- [ ] Chạy pass `mvn -f backend/pom.xml --batch-mode verify`

## Nhóm 3 — Frontend Implementation (kèm test ngay trong nhóm)
- [ ] Triển khai API client, state/hooks, UI (`shared/ui/`, tokens `index.css`, i18n `VI`/`EN`, Light/Dark)
- [ ] Viết/cập nhật Unit Test tại `frontend/tests/unit/`
- [ ] Chạy pass `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

## Nhóm 4 — Verification & Sync-Back Proposal (Chờ duyệt)
- [ ] Đối chiếu Acceptance Criteria của các `US-*` liên quan
- [ ] Lập **Sync-Back Proposal** (bảng ID lệch + diff đề xuất) chờ người dùng duyệt
- [ ] *(Chỉ tick sau khi người dùng duyệt)* Ghi diff vào `docs/knowledge/`, thêm 1 dòng `Sync Changelog` tại `KNOWLEDGE_HUB.md`, chuyển `spec.md` sang `Status: Archived`
