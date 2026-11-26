---
name: se-system-design
description: >-
  Software Engineer (SE) System Design skill for QROS. Translates PO strategy, Design screen map,
  and BA PRD/User Stories into technical architecture, database schema, REST/WebSocket contracts,
  security RBAC, and deployment blueprints in docs/knowledge/04-tech/SYSTEM_DESIGN.md.
---

# SE Skill — System Design & Technical Architecture (`04-tech`)

## Vai trò
Đóng vai **Software Engineer / System Architect**: chuyển hóa toàn bộ yêu cầu nghiệp vụ đã chốt từ **PO + Design + BA** thành thiết kế kỹ thuật khả thi (`docs/knowledge/04-tech/SYSTEM_DESIGN.md`) làm cầu nối chuẩn bị bước sang giai đoạn lập trình (Coding).

## Mandatory Input Gate (Cổng kiểm tra bắt buộc)
Trước khi thiết kế hoặc cập nhật kiến trúc hệ thống, **BẮT BUỘC** đọc:
1. `docs/knowledge/KNOWLEDGE_HUB.md`
2. `docs/knowledge/01-po/blueprint.md` & `roadmap.md`
3. `docs/knowledge/02-design/PROTOTYPE_SCREEN_MAP.md`
4. `docs/knowledge/03-ba/PRD_BY_USER_STAGE.md` & `USER_STORIES.md`

> **Hard Rule:** Mọi endpoint API, WebSocket topic, bảng DB và quy tắc bảo mật phải truy vết được ngược về `US-xxx` và `BR-xx` trong `03-ba/`. Nếu phát hiện yêu cầu mâu thuẫn kỹ thuật, phải phản hồi cập nhật lại `03-ba/` thay vì tự ý code khác đặc tả.

## Quy trình thực thi
1. **Đối chiếu Traceability (§1.3):** Lập bảng ánh xạ đầy đủ từng `STG-01`..`06` / `US-001`..`xxx` / `BR-01`..`xx` / `SCR-01`..`26` xuống module Backend (`com.qros.modules.*`), feature Frontend (`frontend/src/features/*`), bảng SQL và STOMP topic.
2. **Kiến trúc & Ràng buộc phi chức năng:** Thiết kế High-Level Architecture, Modular Monolith (Spring Boot 4.1.2 / Java 25, ArchUnit), Frontend FSD (React 19.2 / Vite 7 / Tailwind v4 / Capacitor 8.6), Idempotency, Optimistic Locking, Caching (Caffeine/Redis), Security (JWT/Cookie/2FA/6-Role RBAC đúng thứ tự `PER-01`..`PER-06`).
3. **Hợp đồng giao tiếp & Triển khai:** Đặc tả REST API, WebSocket STOMP, Sequence Diagrams cho luồng trọng yếu, Quality Gates khớp CI (`.github/workflows/ci.yml`) và hạ tầng triển khai (Vercel, Render/Docker, Neon, Redis, Android APK).

## Output Contract & Handoff
- Cập nhật `docs/knowledge/04-tech/SYSTEM_DESIGN.md` kèm bảng ánh xạ `STG / US / BR / SCR ↔ Module / API / WS / DB`.
- Bàn giao trực tiếp sang **Tầng 5 (`docs/knowledge/05-specs/CONSTITUTION.md` & `.agents/skills/sdd-coding-sync`)** và cập nhật trạng thái trong `docs/knowledge/KNOWLEDGE_HUB.md`.
