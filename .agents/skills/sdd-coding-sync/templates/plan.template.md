# Technical Plan `<NNN>`: `<Tên tính năng>`

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Ảnh hưởng Kiến trúc & Hợp đồng giao tiếp
- **Database / Flyway:** `<Tên file V1.xx__*.sql hoặc "Không đổi schema">`
- **REST API / Security / Rate Limit:** `<Method + Path + Role + Rate limit scope>`
- **WebSocket STOMP Topics:** `<Topic phát tán / lắng nghe hoặc "Không đổi">`

## 2. Danh sách File dự kiến thêm / sửa
### Backend (`backend/`)
- `...`

### Frontend (`frontend/`)
- `...`

## 3. Kiểm tra nhanh theo `CONSTITUTION.md`
- [ ] Ranh giới Modular Monolith (`ArchUnit`) & Thông báo tiếng Anh (`BackendMessageLanguageTest`)
- [ ] Bảo mật RBAC / Auth / Payment / Kho / Idempotency (nếu liên quan)
- [ ] Design Tokens 2 tầng (không hardcode hex), Light/Dark & i18n `VI`/`EN`
- [ ] Chiến lược Test gắn vào từng nhóm task trong `tasks.md`
