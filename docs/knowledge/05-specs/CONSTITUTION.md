# QROS Project Constitution — Hiến pháp Kỹ thuật (`05-specs`)

> **Vai trò:** Bảng chỉ mục luật bất biến cho **Tầng 5 — Spec-Driven Development (`sdd-coding-sync`)**.  
> **Quy luật cốt lõi:** Mọi tính năng mới đi theo **chiều thuận** `01-po → 02-design → 03-ba → 04-tech → 05-specs → Sinh Code`. File này **không sao chép lại nội dung chi tiết** mà trỏ trực tiếp về các file nguồn chân lý gốc để tránh lệch tài liệu. Mọi luật đều gắn rõ nhãn kiểm tra tự động (**Auto**) hay thủ công (**Manual**).

---

## 1. Bảng Luật Bất Biến (Invariants & Verification Mode)

| # | Luật kỹ thuật | Nguồn chân lý (Link) | Cơ chế kiểm tra |
|---|---|---|---|
| 1 | Ranh giới Modular Monolith (`core`, `modules`, `infrastructure`, `shared`), 0 vòng phụ thuộc giữa 16 module, 0 cross-module repository import & 0 vòng phụ thuộc Spring bean | [SYSTEM_DESIGN.md §3](../04-tech/SYSTEM_DESIGN.md#3-kiến-trúc-backend-modular-monolith) | **Auto** (`BackendDependencyRulesTest` / ArchUnit 1.5.1 — 11 rules) |
| 2 | Toàn bộ thông điệp lỗi/phản hồi backend dùng tiếng Anh | [AGENTS.md](../../../AGENTS.md) | **Auto** (`BackendMessageLanguageTest`) |
| 3 | Format mã nguồn Java & `pom.xml` (Palantir Java Format) | [backend/pom.xml](../../../backend/pom.xml) | **Auto** (`spotless:check` trong `mvn verify`) |
| 4 | Lint, unit test & production build Frontend | [frontend/package.json](../../../frontend/package.json) | **Auto** (`npm test`, `npm run lint`, `npm run build`) |
| 5 | Phân quyền 5 system roles (`UserRole` + `RoleHierarchy`) + 1 public guest (`PER-01` qua phiên QR/bàn), `SecurityRoutes` & Admin 2FA | [SYSTEM_DESIGN.md §7](../04-tech/SYSTEM_DESIGN.md#7-kiến-trúc-bảo-mật--phân-quyền-security--6-role-rbac) | **Auto** (`CoreApiSecurityIntegrationTest`, `SecurityConfigRegressionTest`, `Admin2faServiceTest`) + **Manual (review PR)** |
| 6 | Chống trùng đơn/thanh toán (Idempotency) & Pessimistic/Optimistic Locking (`@Lock(PESSIMISTIC_WRITE)`/`FOR UPDATE` & `@Version`) | [SYSTEM_DESIGN.md §5, §6](../04-tech/SYSTEM_DESIGN.md#5-thiết-kế-cơ-sở-dữ-liệu-database-design) | **Auto** (`IdempotencyServiceTest`, unit tests theo module) + **Manual (review PR)** |
| 7 | Flyway chỉ thêm migration mới (kiểm tra bản mới nhất trong `db/migration/` rồi tạo tiếp `V1.xx__*.sql`), cấm sửa migration đã áp dụng | [SYSTEM_DESIGN.md §5](../04-tech/SYSTEM_DESIGN.md#5-thiết-kế-cơ-sở-dữ-liệu-database-design) | **Auto** (`PostgresFlywayAndDomainIT` trên Testcontainers PG16) + **Manual (review PR)** *(Flyway bắt checksum drift trên DB đã chạy bản cũ)* |
| 8 | Chiều phụ thuộc FSD (`app → pages → widgets → features → entities → shared`), 0 cross-slice, Public API barrels (`index.js`) & tái sử dụng `shared/ui/` | [SYSTEM_DESIGN.md §4](../04-tech/SYSTEM_DESIGN.md#4-kiến-trúc-frontend-feature-sliced-design) | **Auto** (`frontend/eslint.config.js` — FSD layer, cross-slice & barrel rules) + **Manual (review PR)** |
| 9 | Dùng Design Tokens 2 tầng (`index.css`), không hardcode hex, đạt tương phản Light/Dark WCAG 2.1 AA | [DESIGN_SYSTEM.md](../02-design/DESIGN_SYSTEM.md) | **Auto** (`frontend/tests/unit/designSystem.test.js`) + **Manual (review PR / visual check)** |
| 10 | Đầy đủ song ngữ `VI`/`EN` (`useTranslation`) cho mọi màn hình vận hành | [frontend/README.md](../../../frontend/README.md) | **Manual (review PR)** |

---

## 2. Definition of Done (DoD) — Khớp 100% CI ([ci.yml](../../../.github/workflows/ci.yml))

Mọi thay đổi code trước khi hoàn tất phải vượt qua đúng bộ lệnh CI (hoặc chạy nhanh `npm run check` / `bash scripts/run_checks.sh`):

- **Backend** *(CI chạy trên Java 25)*:
  ```bash
  mvn -f backend/pom.xml --batch-mode verify
  ```
  *(Bao gồm `spotless:check`, `ArchUnit` 11 rules, `HibernateEntityMetadataTest`, `BackendMessageLanguageTest`, **357** Surefire unit/MVC tests + **7** Failsafe Testcontainers PG16 & Redis 7 `*IT.java` = **364 test cases**)*
- **Frontend** *(CI chạy trên Node 22)*:
  ```bash
  npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build
  ```
  *(Bao gồm **160 test cases** `node:test`, ESLint FSD strict rules, và Vite production build + Capacitor `build:capacitor`)*
- **Knowledge Alignment:** Nếu thay đổi đụng bất kỳ ID nào (`F*.*`, `SCR-*`, `BR-*`, `US-*`, API, WebSocket, bảng DB) → bắt buộc tạo **Sync-Back Proposal** chờ người dùng duyệt trước khi ghi vào `02-design/`, `03-ba/`, `04-tech/` và `KNOWLEDGE_HUB.md`.
