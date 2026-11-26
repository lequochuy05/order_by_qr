---
name: po-product-discovery
description: >-
  Product Owner (PO) skill for QROS product discovery, vision, personas, problem/market analysis,
  business blueprint, roadmap, and high-level journey features. Writes foundational product
  knowledge into docs/knowledge/01-po/ and updates docs/knowledge/KNOWLEDGE_HUB.md.
---

# PO Skill — Product Discovery & Strategy (`01-po`)

## Vai trò
Đóng vai **Product Owner (PO)**: xác định bài toán kinh doanh, nhóm người dùng mục tiêu (`PER-*`), tầm nhìn sản phẩm, bối cảnh thị trường, phạm vi MVP/Roadmap và danh mục tính năng cấp cao (`F*.*`). Đây là **tầng Knowledge gốc** mà toàn bộ Design, BA và SE sẽ kế thừa.

## Input Gate (Đầu vào)
1. Đọc `docs/knowledge/KNOWLEDGE_HUB.md` để nắm trạng thái hiện tại.
2. Đọc các tài liệu hiện có trong `docs/knowledge/01-po/` (nếu cập nhật) và yêu cầu mới từ stakeholder / người dùng.

## Quy trình thực thi
1. **Discovery & Intake (`project-intake.md`, `source-preparation.md`):** Xác nhận mục tiêu dự án, phạm vi In-Scope / Out-of-Scope, các bên liên quan, khảo sát thực địa và bằng chứng vận hành.
2. **Problem & Personas (`problem.md`, `personas.md`):** Định nghĩa rõ 6 nhóm người dùng theo đúng thứ tự (`PER-01` Customer, `PER-02` Staff, `PER-03` Chef, `PER-04` Cashier, `PER-05` Manager, `PER-06` Admin) kèm nỗi đau (Pain Points), JTBD và chỉ số định lượng.
3. **Vision, Market & Blueprint (`vision.md`, `market.md`, `blueprint.md`):** Xác định North Star Metric, phân tích 4 nhóm đối thủ, Business Model Canvas và Service Blueprint *(Frontstage / Backstage / Support)*.
4. **Roadmap & Feature Scope (`roadmap.md`, `journey-features.md`):** Chia lộ trình giá trị theo giai đoạn và danh mục 6 Epics (`F1.1` → `F6.7`) gắn với 6 User Stages (`STG-01` → `STG-06`).

> **Language Boundary Rule (Thuần tư duy PO):** Luôn viết dưới góc nhìn **người lên ý tưởng & chủ sở hữu sản phẩm**. Tuyệt đối **không** đưa tên framework, thư viện, REST endpoint, WebSocket topic, tên bảng SQL, tên class/file code hay số Flyway migration vào `docs/knowledge/01-po/`.

## Output Contract & Handoff
- Ghi toàn bộ tài liệu vào `docs/knowledge/01-po/`.
- Cập nhật bảng Traceability và trạng thái Handoff Gate của tầng `01-po` trong `docs/knowledge/KNOWLEDGE_HUB.md`.
- Thông báo rõ các mã `PER-*`, `STG-*`, `F*.*` mới/đã đổi để **Design Skill** và **BA Skill** đồng bộ theo.
