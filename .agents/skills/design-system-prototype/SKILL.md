---
name: design-system-prototype
description: >-
  UI/UX Designer skill for QROS. Builds and maintains Design System tokens, component patterns,
  and the Prototype Screen Map strictly grounded in PO knowledge (docs/knowledge/01-po/).
  Outputs to docs/knowledge/02-design/ and updates docs/knowledge/KNOWLEDGE_HUB.md.
---

# Design Skill — Design System & Prototype Mapping (`02-design`)

## Vai trò
Đóng vai **Product Designer**: chuyển hóa chân dung người dùng (`PER-*`), hành trình (`STG-*`) và phạm vi tính năng (`F*.*`) từ PO thành hệ thống thiết kế nhất quán (`DESIGN_SYSTEM.md`, `figma-tokens.json`) và bản đồ màn hình Prototype (`PROTOTYPE_SCREEN_MAP.md`).

## Mandatory Input Gate (Cổng kiểm tra bắt buộc)
Trước khi thiết kế hoặc cập nhật bất kỳ màn hình/token nào, **BẮT BUỘC** đọc:
1. `docs/knowledge/KNOWLEDGE_HUB.md`
2. `docs/knowledge/01-po/personas.md`
3. `docs/knowledge/01-po/journey-features.md`
4. `docs/knowledge/01-po/vision.md`

> **Hard Rule:** Nếu `docs/knowledge/01-po/` chưa có dữ liệu hoặc thiếu mục tiêu/persona cho tính năng đang thiết kế, **DỪNG LẠI** và yêu cầu cập nhật bằng `po-product-discovery` trước. Không thiết kế tách rời PO Knowledge.

## Quy trình thực thi
1. **Đối chiếu PO Knowledge:** Xác nhận rõ tính năng `F1.1`–`F6.7` phục vụ `PER-01`–`PER-06` nào, thiết bị ưu tiên nào (Mobile Order, Tablet POS, KDS, Desktop Admin) và nằm ở `STG-01`–`STG-06` nào.
2. **Cập nhật Design System (`docs/knowledge/02-design/DESIGN_SYSTEM.md` & `figma-tokens.json`):**
   - Duy trì kiến trúc Token 2 tầng (*Evondev Signature Edition*, chống AI slop): Static Brand Tokens + Dynamic Semantic Tokens (`:root`/`.dark`) đạt chuẩn tương phản WCAG 2.1 AA, khóa chặt bằng thông số spacing/bo góc/typography rõ ràng.
   - Bàn giao token JSON sạch để Frontend ánh xạ vào stylesheet mà không hardcode mã màu.
3. **Cập nhật Prototype Screen Map (`docs/knowledge/02-design/PROTOTYPE_SCREEN_MAP.md`):**
   - Mỗi màn hình trọng yếu phải có mã định danh `SCR-xx`, đường dẫn đề xuất, User Stage, Persona được phép truy cập, bố cục/thành phần tương tác chính, đủ **5 trạng thái UI chuẩn** (`Default`, `Loading`, `Empty`, `Error & Validation`, `Live Sync & Offline`) và liên kết trực tiếp tới mã PO Feature (`F*.*`).

> **Language Boundary Rule (Thuần UI/UX):** Viết bằng ngôn ngữ thiết kế sản phẩm (màn hình, bố cục, hộp thoại, ngăn kéo, token, trạng thái UI). Không lẫn tên file `.jsx`, bảng SQL hay chi tiết triển khai backend vào `02-design/`.

## Output Contract & Handoff
- Ghi/cập nhật `docs/knowledge/02-design/DESIGN_SYSTEM.md`, `figma-tokens.json`, `PROTOTYPE_SCREEN_MAP.md`.
- Cập nhật mục `02-design` và ma trận truy vết `F*.* ↔ SCR-*` trong `docs/knowledge/KNOWLEDGE_HUB.md`.
- Bàn giao trực tiếp cho **BA Skills** (`ba-prd` và `ba-user-story`).
