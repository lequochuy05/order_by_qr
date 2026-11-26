# QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa nền tảng

**QROS** là nền tảng quản lý và vận hành nhà hàng toàn diện (All-in-One Restaurant Platform) theo kiến trúc **Modular Monolith** (Backend) kết hợp **Feature-Sliced Design** (Frontend), hỗ trợ **Web SPA, PWA và Mobile App (Android / iOS qua Capacitor 8)**.

Hệ thống bao phủ trọn vẹn quy trình vận hành nhà hàng: đặt món tại bàn qua mã QR có chữ ký bảo mật **HMAC-SHA256**, đơn mang về (**Takeaway**), hiển thị bếp thời gian thực (**KDS — Kanban & Nấu gom món**), quản lý sơ đồ bàn & gộp/tách bàn, định mức kho nguyên liệu tự động, thanh toán **Tiền mặt & VietQR PayOS**, trợ lý **Google Gemini AI** (tư vấn khách, hỗ trợ bếp/phục vụ, sinh mô tả món), báo cáo phân tích & dự báo doanh thu, cùng **giao diện song ngữ Việt/Anh & Dark Mode**.

---

## 1. TÍNH NĂNG CHÍNH & KIẾN TRÚC MODULE

### Backend — 15 Module nghiệp vụ (38 REST Controllers, 3 Infrastructure Adapters, chuẩn ArchUnit 11 rules)
| Module | Tính năng cốt lõi |
|---|---|
| `auth` | JWT Access Token + HttpOnly Refresh Token rotation, **Admin 2FA OTP** (cooldown, giới hạn lần thử), khóa tài khoản chống brute-force, bắt buộc đổi mật khẩu lần đầu, quên/đặt lại mật khẩu qua Email (Brevo HTTPS API / SMTP) & SMS (Twilio), thu hồi token trên Redis. |
| `user` | Quản lý nhân sự theo RBAC 6 vai trò, cập nhật hồ sơ cá nhân & avatar (Cloudinary). |
| `table` | Sơ đồ bàn, sinh mã QR ký **HMAC-SHA256** (`QR_SIGNING_SECRET`), vòng đời phiên bàn (heartbeat, idle/max timeout, giới hạn thiết bị), **gộp bàn / hủy gộp bàn** có audit trail, và hàng đợi **yêu cầu hỗ trợ tại bàn (`TableServiceRequest`)**. |
| `menu` | Quản lý Danh mục, Thực đơn, nhóm tùy chọn/topping (`ItemOption`), Combo và hình ảnh. |
| `order` | Đặt món tại bàn (khách & nhân viên), đơn mang về (`TAKEAWAY`), xem trước tính tiền & voucher (`preview`), cập nhật/hủy món có lý do, chống trùng lặp yêu cầu (`IdempotencyService`). |
| `kitchen` | Màn hình bếp KDS realtime: lọc theo trạng thái, chế độ **Kanban** & **Nấu gom món (`BatchCookingView`)**, cập nhật trạng thái đơn lẻ và hàng loạt (`batch-status`). |
| `inventory` | Quản lý nguyên liệu (kèm đơn giá nhập `unit_cost`), định mức công thức món (`Recipe`), nhập kho đơn lẻ & **nhập kho theo lô (`batch-stock-in`)**, điều chỉnh tồn kho (`StockMovement`), tự động giữ chỗ/khấu trừ tồn kho (Optimistic Locking `@Version`) và kiểm tra khả dụng theo thời gian thực. |
| `payment` | Thanh toán tiền mặt (tiền khách đưa, tiền thối, xác nhận món chưa hoàn tất), **VietQR PayOS** (tạo mã thanh toán, đếm ngược hết hạn, webhook xác nhận tự động) và **khách tự thanh toán tại bàn (`PublicSelfCheckoutController`)**. |
| `promotion` | Quản lý Voucher & Chương trình khuyến mãi, kiểm tra điều kiện áp dụng và giới hạn lượt dùng an toàn concurrency. |
| `analytics` | Dashboard kinh doanh, chuỗi doanh thu, món bán chạy, hiệu suất nhân viên, lịch sử hóa đơn, **dự báo nhu cầu & tồn kho (`Forecast`)**, tổng hợp dữ liệu định kỳ (`@Scheduled`). |
| `ai` | 6 bộ controller **Google Gemini AI** có quota theo IP/tài khoản/ngày và tự chuyển model dự phòng: (1) Tư vấn thực đơn cho khách, (2) Trợ lý vận hành & **phân loại ghi chú dị ứng/ưu tiên cho bếp/phục vụ (`analyze-notes`)**, (3) Sinh mô tả món ăn, (4) **Executive Insights** tóm tắt kinh doanh, (5) **Inventory AI** (gợi ý nhập hàng, trợ lý công thức, **OCR hóa đơn nhập kho**), (6) **Merchandising AI** (đề xuất Combo & Khuyến mãi). |
| `recommendation` | Gợi ý món ăn phổ biến & cá nhân hóa cho khách trên trang thực đơn. |
| `publicapi` | Facade tổng hợp (`PublicCatalogService`) cho API công khai tối ưu hiệu năng có cache đa tầng & rate-limit cho khách quét QR. |
| `notification` | Đồng bộ realtime qua **STOMP over WebSocket / SockJS** (12 topics gồm `/topic/service-requests`) phân quyền theo Role Hierarchy. |
| `settings` | Cấu hình thông tin nhà hàng, logo, mẫu hóa đơn in (Wifi, lời cảm ơn, chống XSS), cờ bật/tắt tự thanh toán, âm thanh/thông báo vận hành. |

### Frontend — 19 Feature Modules, 16 Entities, 2 Widgets, 28 Named Routes (+ `*`), Đa nền tảng
- **Kiến trúc FSD nghiêm ngặt:** Tách rõ `app → pages → widgets → features → entities → shared` (`admin-layout` & `order-modals` tại `widgets/`, `session`/`settings`/`ai` tại `entities/`), tự động kiểm soát bằng ESLint (0 upward imports, 0 cross-slice imports, bắt buộc Public API barrels `index.js`).
- **Trải nghiệm đa nền tảng:** Web SPA (`Vercel`), **PWA** (Service Worker, chế độ offline `/offline`, trang trạng thái lỗi `/error/:statusCode`), và **Mobile App (Capacitor 8.6)** cho Android (`https://localhost`, `cleartext: false`, `allowBackup: false`) & iOS (`capacitor://localhost`).
- **Song ngữ (i18n Việt / Anh) & Dark Mode:** Chuyển đổi ngôn ngữ và giao diện sáng/tối mượt mà, đồng bộ trạng thái đa tab qua Zustand (`useAdminPreferences`).
- **In hóa đơn an toàn:** Xuất hóa đơn in HTML có escape toàn bộ dữ liệu người dùng (`invoiceGenerator.js`, chống XSS).

---

## 2. MA TRẬN 6 VAI TRÒ NGƯỜI DÙNG (RBAC)

| Vai trò (Role) | Xác thực | Phạm vi quyền hạn & Màn hình chính |
|---|---|---|
| **Customer** | Quét QR bàn (không cần đăng nhập) | Menu tại bàn (`/:tableCode/menu`) hoặc công khai (`/menu`): chọn món/topping/combo, giỏ hàng, gửi đơn, theo dõi trạng thái, **gọi phục vụ tại bàn**, **tự thanh toán VietQR PayOS**, chat AI Gemini. |
| **Staff** | Tài khoản nội bộ | Sơ đồ bàn (`/admin/tables`), **hàng đợi yêu cầu hỗ trợ bàn (`/admin/service-requests`)**, mở bàn, gọi món tại bàn, gộp/tách bàn, đơn mang về (`/admin/takeaway`), trợ lý AI nội bộ, hồ sơ cá nhân (`/admin/profile`). *(Ẩn thao tác thanh toán & báo bếp xong).* |
| **Chef** | Tài khoản nội bộ | Màn hình bếp KDS (`/admin/kitchen`) chế độ Kanban & Nấu gom món, cập nhật trạng thái món, quản lý & điều chỉnh tồn kho nguyên liệu (`/admin/inventory`), trợ lý AI bếp & phân tích ghi chú. |
| **Cashier** | Tài khoản nội bộ | Sơ đồ bàn thu ngân (`/admin/tables`), **hàng đợi yêu cầu bàn (`/admin/service-requests`)**, đơn mang về (`/admin/takeaway`), thanh toán Tiền mặt & PayOS (`PaymentModal`), áp voucher, in hóa đơn, tra cứu lịch sử hóa đơn (`/admin/history`). |
| **Manager** | Tài khoản nội bộ | Dashboard (`/admin/dashboard`), quản trị Danh mục (`/admin/categories`), Thực đơn (`/admin/menu`), Combo (`/admin/combo`), Voucher (`/admin/voucher`), **Khuyến mãi (`/admin/promotions`)**, Nhân sự (`/admin/staffs`), Kho (`/admin/inventory`), Báo cáo món bán chạy (`/admin/statistics/top-dishes`) & hiệu suất nhân viên (`/admin/statistics/staff`), cùng toàn bộ trợ lý AI vận hành/kho/merchandising. |
| **Admin** | Tài khoản nội bộ + **2FA OTP Email** | Toàn quyền hệ thống + **độc quyền Báo cáo Doanh thu chi tiết (`/admin/statistics/revenue`)**, Cấu hình hệ thống (`/admin/settings`), và Prometheus metrics (`/actuator/prometheus`). |

> **Lưu ý tạo tài khoản Admin local:** Mặc định ứng dụng **không tự tạo tài khoản mẫu**. Ở môi trường local (`dev`), có thể bật tạo admin tùy chọn bằng cách đặt `DEV_ADMIN_BOOTSTRAP_ENABLED=true`, `DEV_ADMIN_EMAIL` và `DEV_ADMIN_PASSWORD` trong `.env`. Mã OTP 2FA được gửi qua email (Brevo API hoặc SMTP), không ghi ra log backend.

---

## 3. CÔNG NGHỆ & PHIÊN BẢN

| Phân hệ | Công nghệ sử dụng | Chi tiết kỹ thuật |
|---|---|---|
| **Backend** | Java **25 LTS**, Spring Boot **4.1.2** | Spring Security 7, Jackson 3 (`tools.jackson`), Hibernate **7.4**, MapStruct 1.6, Lombok 1.18, Spotless **3.11** |
| **Database** | PostgreSQL **16+**, Flyway **12.4** | `spring-boot-starter-flyway` (**25 migrations** `V1` -> `V1.24`, **35 bảng**), `ddl-auto: validate`, `baseline-on-migrate: false` |
| **Cache & Security** | Caffeine (Dev) & Redis 7 (Prod) | Redis JSON serializer Jackson 3 (`qros:v4:`), Lua rate limiter, Token Blacklist, 2FA & Refresh Token Store |
| **Realtime** | Spring STOMP over WebSocket, SockJS | 12 topics (gồm `/topic/service-requests`), phân quyền subscribe theo Role Hierarchy, tự động invalidate cache React Query |
| **Frontend** | React **19.2**, Vite **7.2.4** (7.3 runtime), Tailwind CSS **4** | TanStack React Query v5, Zustand v5, React Router v7, Recharts v3, Lucide Icons, `vite-plugin-pwa` |
| **Mobile** | Capacitor **8.6.2** | Android (`frontend/android/`, Gradle 8.14 / SDK 36, `androidScheme: https`) & hỗ trợ iOS (`capacitor://localhost`) |
| **Testing & CI** | JUnit 5, ArchUnit 1.5.1 (11 rules), Node Test, ESLint 9 (FSD strict), Playwright | **319** backend tests (**69** suites), **144** frontend unit tests (**20** suites), GitHub Actions CI/CD (**Java 25 + Node 22** + GHCR) |
| **Tích hợp ngoài** | PayOS, Google Gemini AI, Cloudinary, Brevo, Twilio | Thanh toán VietQR webhook, 6 luồng AI Gemini, ảnh cloud, email HTTPS API, SMS OTP |

---

## 4. CẤU TRÚC DỰ ÁN

```text
.
├── .agents/                  # Workspace Rules & 6 Role/SDD Skills (PO → Design → BA → SE → SDD Coding/Sync)
├── backend/                  # Spring Boot (15 domain modules, 3 infra adapters, Flyway V1–V1.24 / 35 bảng)
├── frontend/                 # React 19 FSD (19 features, 16 entities, 2 widgets) + Capacitor 8 Android (`frontend/android/`)
│   ├── vercel.json           # Cấu hình deploy Vercel (rewrites API/WS, CSP, cache headers)
│   ├── nginx.conf            # Cấu hình Nginx cho Docker (SPA, proxy API/WS, CSP)
│   └── Dockerfile            # Multi-stage Node 22 -> Nginx Alpine
├── .github/workflows/ci.yml  # CI & push Docker images lên GHCR
├── docs/knowledge/           # Centralized Knowledge Hub (01-po → 02-design → 03-ba → 04-tech → 05-specs)
├── reports/                  # 8 báo cáo QA, Discovery, Feature/Role Matrix, Bug Report
├── scripts/                  # Playwright E2E/Android audit, backup/restore Postgres, deploy, run_checks
├── Dockerfile                # Multi-stage build backend production
├── docker-compose.yml        # Cụm container backend + frontend (kèm tùy chọn `--profile local-infra` Postgres & Redis)
├── render.yaml               # Blueprint triển khai backend + Postgres + Redis lên Render
├── .env.example              # Mẫu biến môi trường cho Local & Production
├── package.json              # Workspace scripts (`npm run check`, `backend:verify`, `frontend:*`, `audit:*`)
└── README.md                 # Tài liệu tổng quan, cài đặt và vận hành
```

---

## 5. YÊU CẦU MÔI TRƯỜNG & CHẠY LOCAL (`dev`)

### Yêu cầu hệ thống
- **JDK:** `25` LTS (Maven `3.9+`).
- **Node.js:** `20+` (khuyến nghị `22+`, npm `10+`).
- **Docker:** chạy Redis (`redis:7-alpine`) hoặc build image.
- **PostgreSQL:** `16+` (local hoặc Neon Cloud).

### Bước 1: Khởi động Redis
```bash
docker run -d --name qros-redis -p 6379:6379 redis:7-alpine
# Hoặc bật lại container đã tạo:
docker start qros-redis
```

### Bước 2: Cấu hình `.env`
```bash
cp .env.example .env
```
Cập nhật các biến bắt buộc trong `.env`:
- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Redis: `REDIS_HOST=localhost`, `REDIS_PORT=6379`, `REDIS_SSL_ENABLED=false`
- Bảo mật (tạo 2 chuỗi 64 ký tự hex riêng biệt bằng `openssl rand -hex 32`): `JWT_SECRET`, `QR_SIGNING_SECRET`
- Tích hợp ngoài (khi sử dụng): `BREVO_API_KEY` + `MAIL_USERNAME` (hoặc SMTP), `GEMINI_API_KEY`, `PAYOS_*`, `CLOUDINARY_*`

### Bước 3: Chạy Backend & Frontend
- **Backend (bắt buộc truyền `-Dspring-boot.run.profiles=dev`):**
  ```bash
  cd backend
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  ```
  API chạy tại `http://localhost:8080`, health check tại `http://localhost:8080/actuator/health`.
- **Frontend:**
  ```bash
  cd frontend
  npm ci
  npm run dev
  ```
  Web chạy tại `http://localhost:5173`.
- **Hoặc chạy cả Redis + Backend + Frontend bằng 1 lệnh (tại thư mục gốc):**
  ```bash
  (docker start qros-redis 2>/dev/null || docker run -d --name qros-redis -p 6379:6379 redis:7-alpine) && (trap 'kill 0' INT TERM; mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev & npm --prefix frontend run dev & wait)
  ```

### Đóng gói ứng dụng Android (Capacitor)
```bash
cd frontend
npm run build:android   # Build Vite + đồng bộ sang frontend/android/
npm run cap:open        # Mở dự án trong Android Studio
```

---

## 6. KIỂM THỬ, LINT & FORMAT

```bash
# Chạy toàn bộ CI DoD (Backend verify + Frontend test/lint/build) bằng 1 lệnh từ root:
npm run check
# hoặc: bash scripts/run_checks.sh

# Backend: Spotless 3.11 + Compile + 319 tests (69 suites: Unit/Slice/ArchUnit 11 rules) + Package JAR
mvn -f backend/pom.xml --batch-mode verify
mvn -f backend/pom.xml spotless:apply   # Tự động format Java & pom.xml

# Frontend: 144 unit tests (20 suites) + ESLint FSD strict + Vite production build
npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build

# E2E Playwright Audit (6 vai trò x 3 kích thước màn hình; đặt AUDIT_ADMIN_EMAIL / AUDIT_ADMIN_PASSWORD)
node scripts/phase4_full_ui_audit.js
```

---

## 7. TRIỂN KHAI PRODUCTION & DOCKER

### Kiến trúc Production (Render + Vercel)
- **Backend (Render):** tự động build từ [Dockerfile](Dockerfile) theo [render.yaml](render.yaml) trên nhánh `master` (`SPRING_PROFILES_ACTIVE=prod`, health check `/actuator/health/liveness`).
- **Frontend (Vercel):** tự động build từ `frontend/` theo [frontend/vercel.json](frontend/vercel.json) với **Node.js 22.x**.

### Checklist biến môi trường trên Render (`prod`)
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL` (**không** bọc dấu nháy kép), `DB_USERNAME`, `DB_PASSWORD` (DB đã có bảng `flyway_schema_history` khớp `V1`–`V1.24`)
- `REDIS_HOST`, `REDIS_PORT=6379`, `REDIS_SSL_ENABLED=false` *(đặt `true` nếu dùng Redis TLS ngoài)*
- `JWT_SECRET` và `QR_SIGNING_SECRET` *(2 chuỗi 64 ký tự hex riêng biệt sinh bằng `openssl rand -hex 32`)*
- `APP_BASE_URL=https://order-by-qr.onrender.com`
- `APP_FRONTEND_BASE_URL=https://wqros.vercel.app`
- `APP_CORS_ALLOWED_ORIGINS=https://wqros.vercel.app,https://localhost,capacitor://localhost` *(bắt buộc HTTPS + `capacitor://localhost` cho iOS; không dùng `http://localhost` ở `prod`)*
- `APP_TRUST_PROXY_HEADERS=true`
- `BREVO_API_KEY` + `MAIL_USERNAME` *(sender đã xác minh trên Brevo)*

### Build Docker Images & Chạy cụm Docker Compose (tùy chọn cho demo / VPS)
```bash
# Build và chạy backend + frontend với DB/Redis ngoài (.env):
docker compose up -d --build

# Hoặc chạy trọn gói kèm PostgreSQL 16 + Redis 7 local:
docker compose --profile local-infra up -d --build
```

---

## 8. KNOWLEDGE HUB, WORKSPACE SKILLS & BÁO CÁO NGHIỆM THU

### Trung tâm tri thức tập trung (`docs/knowledge/`) & Bộ Skills (`.agents/skills/`)
Toàn bộ tài liệu sản phẩm và kỹ thuật được quản lý tập trung tại [docs/knowledge/KNOWLEDGE_HUB.md](docs/knowledge/KNOWLEDGE_HUB.md) với liên kết bắt buộc giữa 4 tầng (`PO → Design → BA → SE`):
- **Tầng 1 — PO (`01-po/`):** [project-intake.md](docs/knowledge/01-po/project-intake.md), [vision.md](docs/knowledge/01-po/vision.md), [personas.md](docs/knowledge/01-po/personas.md), [journey-features.md](docs/knowledge/01-po/journey-features.md), [blueprint.md](docs/knowledge/01-po/blueprint.md)... · Skill: `po-product-discovery`
- **Tầng 2 — Design (`02-design/`):** [DESIGN_SYSTEM.md](docs/knowledge/02-design/DESIGN_SYSTEM.md), [figma-tokens.json](docs/knowledge/02-design/figma-tokens.json), [PROTOTYPE_SCREEN_MAP.md](docs/knowledge/02-design/PROTOTYPE_SCREEN_MAP.md) · Skill: `design-system-prototype`
- **Tầng 3 — BA (`03-ba/`):** [PRD_BY_USER_STAGE.md](docs/knowledge/03-ba/PRD_BY_USER_STAGE.md), [USER_STORIES.md](docs/knowledge/03-ba/USER_STORIES.md) · Skills: `ba-prd`, `ba-user-story`
- **Tầng 4 — System Design (`04-tech/`):** [SYSTEM_DESIGN.md](docs/knowledge/04-tech/SYSTEM_DESIGN.md) · Skill: `se-system-design`
- **Tầng 5 — SDD Coding & Sync (`05-specs/`):** [CONSTITUTION.md](docs/knowledge/05-specs/CONSTITUTION.md) & Baseline/Feature Specs · Skill: `sdd-coding-sync` *(phương pháp Spec-Driven Development, lấy cảm hứng từ Spec Kit, có Sync-Back duyệt thủ công)*

### Báo cáo khảo sát & nghiệm thu (`reports/`)
Toàn bộ báo cáo khảo sát, phân quyền và kiểm thử được lưu tại [reports/](reports/):
1. [reports/PROJECT_DISCOVERY.md](reports/PROJECT_DISCOVERY.md) — Khảo sát route, modal, form và kiến trúc component.
2. [reports/FEATURE_INVENTORY.md](reports/FEATURE_INVENTORY.md) — Danh mục chi tiết tính năng toàn hệ thống.
3. [reports/ROLE_FEATURE_MATRIX.md](reports/ROLE_FEATURE_MATRIX.md) — Ma trận phân quyền 6 vai trò.
4. [reports/SCREENSHOT_INVENTORY.md](reports/SCREENSHOT_INVENTORY.md) — Mục lục ảnh chụp giao diện theo vai trò và thiết bị.
5. [reports/QA_TEST_RESULTS.md](reports/QA_TEST_RESULTS.md) & [reports/COMPREHENSIVE_TEST_REPORT.md](reports/COMPREHENSIVE_TEST_REPORT.md) — Kết quả kiểm thử tự động & đánh giá chất lượng.
6. [reports/RESPONSIVE_AUDIT.md](reports/RESPONSIVE_AUDIT.md) — Kiểm toán giao diện Desktop, Tablet và Mobile.
7. [reports/BUG_REPORT.md](reports/BUG_REPORT.md) — Danh sách lỗi phát hiện và hướng khắc phục.
