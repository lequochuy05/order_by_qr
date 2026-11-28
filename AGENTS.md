# QROS — Codebase Map & Engineering Rules

File này được nạp tự động ở mọi phiên làm việc mới để cung cấp bản đồ kiến trúc và các quy tắc cốt lõi của dự án **QROS** (QR Ordering System).

---

## 1. Tổng quan Kiến trúc & Tech Stack

- **Backend (`backend/`):** Java 25, Spring Boot 4.1.2 (Modular Monolith), PostgreSQL 16+, Flyway 12.4 (`ddl-auto: validate`, `baseline-on-migrate: false` — luôn kiểm tra phiên bản mới nhất trong `backend/src/main/resources/db/migration/` rồi tạo tiếp `V1.xx__*.sql`, cấm sửa hoặc xóa migration đã apply), Redis 7, Caffeine, STOMP/SockJS WebSocket, Spotless, ArchUnit, GraalVM native profile.
- **Frontend (`frontend/`):** React 19.2, Vite 7, Tailwind CSS v4, TanStack Query v5, Zustand v5, PWA, Capacitor 8.6 (Android/iOS), kiến trúc Feature-Sliced Design (FSD).
- **Tích hợp ngoài:** Brevo REST API (fallback SMTP), Google Gemini Flash AI, VietQR PayOS (payment link + webhook), Cloudinary.
- **Phân tầng Backend (`com.qros.*`):**
  - `com.qros.core` — Bootstrap, Security (`SecurityConfig`, `SecurityRoutes`, `RuntimeEndpointValidator`, `CorsConfig`).
  - `com.qros.modules` — 16 domain module nghiệp vụ.
  - `com.qros.infrastructure` — Adapter hạ tầng (`mail`, `sms`, `storage`).
  - `com.qros.shared` — Tầng thấp nhất (`constants`, `event`, `exception`, `ratelimit`, `response`, `security`, `subscription`, `time`).

---

## 2. Khóa Quy tắc Giao tiếp Liên Module (`com.qros.modules.*`)

Tất cả các luật dưới đây được kiểm tra tự động bởi **ArchUnit** (`backend/src/test/java/com/qros/architecture/BackendDependencyRulesTest.java`) và **BackendMessageLanguageTest**:

### Danh sách 16 module dưới `com.qros.modules.*`
`ai`, `analytics`, `auth`, `inventory`, `kitchen`, `menu`, `notification`, `order`, `payment`, `promotion`, `publicapi`, `recommendation`, `settings`, `subscription`, `table`, `user`.

### Nguyên tắc giao tiếp liên module bắt buộc
1. **Cấm inject/import trực tiếp `*Repository` của module khác:** Mọi lớp trong `com.qros.modules.<A>` tuyệt đối không được phụ thuộc vào `com.qros.modules.<B>.repository.*`.
2. **Giao tiếp đồng bộ (sync) chéo module phải đi qua `*Port` hoặc Service boundary:**
   - Khi cần gọi nghiệp vụ/dữ liệu chéo module (đặc biệt để tránh vòng lặp phụ thuộc), định nghĩa interface **`*Port`** ở module gọi (hoặc `com.qros.shared.*` cho cross-cutting) và triển khai **`*PortImpl`** ở module sở hữu dữ liệu.
   - Các Port chuẩn đang dùng trong codebase: `OrderPaymentPort`, `OrderPaymentReconciliationPort`, `OrderInventoryReservationPort`, `MenuCatalogOrderUsagePort`, `MenuInventoryAvailabilityPort`, `VoucherUsageCheckPort`, `UserAuthSessionPort`, và nhóm `com.qros.shared.subscription.*` (`SubscriptionEntitlementPort`, `SubscriptionResourceUsagePort`, `SubscriptionWebhookPort`).
3. **Giao tiếp bất đồng bộ / side-effect chéo module phải dùng Domain Event:**
   - Publish các event record định nghĩa tập trung tại `com.qros.shared.event.DomainEvents` qua `ApplicationEventPublisher` (`OrderChangeEvent`, `OrderSettledEvent`, `PaymentSuccessEvent`, `MenuChangeEvent`, `InventoryChangeEvent`, `TableChangeEvent`, `SubscriptionChangeEvent`, `ServiceRequestChangeEvent`, ...).
   - Các module quan tâm (`notification`, `analytics`, `inventory`, `menu`, `order`) lắng nghe qua listener nội bộ thay vì coupling trực tiếp.
4. **Đồ thị phụ thuộc DAG & Ranh giới tầng (zero cycles):**
   - Zero inter-module cycles (`modules_must_be_free_of_cycles`) và zero Spring bean constructor cycles (`spring_beans_must_not_have_constructor_dependency_cycles`).
   - `menu` **không** phụ thuộc `order`; `table` **không** phụ thuộc `order`.
   - `kitchen` và `payment` **không** phụ thuộc lẫn nhau (phối hợp gián tiếp qua `order`).
   - `payment` **không** phụ thuộc `order.repository` hoặc `order.listener`; `inventory` **không** phụ thuộc `order.repository`.
   - `shared` không phụ thuộc `core`/`infrastructure`/`modules`; `infrastructure` không phụ thuộc `core`/`modules`; `modules` không phụ thuộc `core`.
5. **Ngôn ngữ thông báo Backend:** Toàn bộ thông báo lỗi và user-facing message trong backend bắt buộc viết bằng **tiếng Anh** (`BackendMessageLanguageTest`).

---

## 3. Workflow 6 Skill (`.agents/skills/`) & Knowledge Governance

Toàn bộ tri thức dự án tập trung tại `docs/knowledge/` (chỉ mục tại `docs/knowledge/KNOWLEDGE_HUB.md`) theo chuỗi chiều thuận của **6 skill** trong `.agents/skills/`:

1. `po-product-discovery` → `docs/knowledge/01-po/`
2. `design-system-prototype` → `docs/knowledge/02-design/`
3. `ba-prd` → `docs/knowledge/03-ba/PRD_BY_USER_STAGE.md`
4. `ba-user-story` → `docs/knowledge/03-ba/USER_STORIES.md`
5. `se-system-design` → `docs/knowledge/04-tech/SYSTEM_DESIGN.md`
6. `sdd-coding-sync` → `docs/knowledge/05-specs/` (tuân thủ `docs/knowledge/05-specs/CONSTITUTION.md`)

### Quy định thực thi bắt buộc
- **Khi làm tính năng mới, refactor lớn, hoặc đụng vùng nhạy cảm** (auth, thanh toán/webhook, RBAC, kho/locking, idempotency, DB migration, API contract): **bắt buộc đi theo skill `sdd-coding-sync`** — khai báo nhánh (A/B/C) ngay đầu phiên và thực hiện đủ chuỗi **`spec` → `plan` → `tasks` → `implement` → `sync-back`**.
- **Trước khi kết thúc mọi tác vụ code/spec:** Bắt buộc rà soát `.agents/rules/knowledge-governance.md`:
  - Giữ đúng mã truy vết (`F*.*`, `STG-*`, `PER-*`, `SCR-*`, `BR-*`, `US-*`) và ranh giới ngôn ngữ từng tầng.
  - **Không tự ý ghi đè** `01-po/`, `02-design/`, `03-ba/`, `04-tech/` hay `KNOWLEDGE_HUB.md` — phải xuất **Sync-Back Proposal** để người dùng duyệt trước khi cập nhật và ghi `Sync Changelog`.

---

## 4. Scoped Rules theo Vùng Code (`.agents/rules/`)

- `.agents/rules/knowledge-governance.md` (`always_on`) — Quản trị `docs/knowledge/`, Input Gates và quy tắc Sync-Back.
- `.agents/rules/backend-core-patterns.md` (`glob`: `backend/src/main/**`) — Order State Machine, Port/Mapper/DTO, checklist `SecurityRoutes` & `RuntimeEndpointValidator`, Flyway & Idempotency/Locking.
- `.agents/rules/ai-module-rules.md` (`glob`: `backend/src/main/java/com/qros/modules/ai/**`) — Quy chuẩn `AiChatGateway`, `AiQuotaService`, `AiPromptBuilder`, `*ContextProvider`.
- `.agents/rules/frontend-fsd.md` (`glob`: `frontend/src/**`) — Quy tắc import một chiều, Public API barrel, anti-cross-slice của FSD, UI Tokens (`index.css`) & i18n (`VI`/`EN`).

---

## 5. Lệnh Kiểm chứng (Verification)

- **Toàn bộ workspace:** `npm run check` (`bash scripts/run_checks.sh`)
- **Backend (tests + Spotless + ArchUnit):** `mvn -f backend/pom.xml --batch-mode verify`
- **Backend format:** `mvn -f backend/pom.xml spotless:apply`
- **Frontend:** `npm --prefix frontend run lint`, `npm --prefix frontend test`, `npm --prefix frontend run build`
