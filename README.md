# QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa nền tảng

**QROS** là nền tảng quản lý và vận hành nhà hàng toàn diện (All-in-One Restaurant Platform) theo kiến trúc **Modular Monolith** (Backend) kết hợp **Feature-Sliced Design** (Frontend), hỗ trợ **Web SPA, PWA và Mobile App (Android / iOS qua Capacitor 8)**.

Hệ thống bao phủ trọn vẹn quy trình vận hành nhà hàng: đặt món tại bàn qua mã QR có chữ ký bảo mật **HMAC-SHA256**, đơn mang về (**Takeaway**), hiển thị bếp thời gian thực (**KDS — Kanban & Nấu gom món**), quản lý sơ đồ bàn & gộp/tách bàn, định mức kho nguyên liệu tự động, thanh toán **Tiền mặt & VietQR PayOS**, trợ lý **Google Gemini AI** (tư vấn khách, hỗ trợ bếp/phục vụ, sinh mô tả món), báo cáo phân tích & dự báo doanh thu, cùng **giao diện song ngữ Việt/Anh & Dark Mode**.

---

## 1. TÍNH NĂNG CHÍNH & KIẾN TRÚC MODULE

### Backend — 15 Module nghiệp vụ (31 REST Controllers, chuẩn ArchUnit)
| Module | Tính năng cốt lõi |
|---|---|
| `auth` | JWT Access Token + HttpOnly Refresh Token rotation, **Admin 2FA OTP** (cooldown, giới hạn lần thử), khóa tài khoản chống brute-force, bắt buộc đổi mật khẩu lần đầu, quên/đặt lại mật khẩu qua Email (Brevo HTTPS API / SMTP) & SMS (Twilio), thu hồi token trên Redis. |
| `user` | Quản lý nhân sự theo RBAC 6 vai trò, cập nhật hồ sơ cá nhân & avatar (Cloudinary). |
| `table` | Sơ đồ bàn, sinh mã QR ký **HMAC-SHA256** (`QR_SIGNING_SECRET`), vòng đời phiên bàn (heartbeat, idle/max timeout, giới hạn thiết bị), **gộp bàn / hủy gộp bàn** có audit trail. |
| `menu` | Quản lý Danh mục, Thực đơn, nhóm tùy chọn/topping (`ItemOption`), Combo và hình ảnh. |
| `order` | Đặt món tại bàn (khách & nhân viên), đơn mang về (`TAKEAWAY`), xem trước tính tiền & voucher (`preview`), cập nhật/hủy món có lý do, chống trùng lặp yêu cầu (`IdempotencyService`). |
| `kitchen` | Màn hình bếp KDS realtime: lọc theo trạng thái, chế độ **Kanban** & **Nấu gom món (`BatchCookingView`)**, cập nhật trạng thái đơn lẻ và hàng loạt (`batch-status`). |
| `inventory` | Quản lý nguyên liệu, định mức công thức món (`Recipe`), nhập kho & điều chỉnh tồn kho (`StockMovement`), tự động giữ chỗ/khấu trừ tồn kho (Optimistic Locking `@Version`) và kiểm tra khả dụng theo thời gian thực. |
| `payment` | Thanh toán tiền mặt (tiền khách đưa, tiền thối, xác nhận món chưa hoàn tất) & **VietQR PayOS** (tạo mã thanh toán, đếm ngược hết hạn, webhook xác nhận tự động). |
| `promotion` | Quản lý Voucher & Khuyến mãi, kiểm tra điều kiện áp dụng và giới hạn lượt dùng an toàn concurrency. |
| `analytics` | Dashboard kinh doanh, chuỗi doanh thu, món bán chạy, hiệu suất nhân viên, lịch sử hóa đơn, **dự báo nhu cầu & tồn kho (`Forecast`)**, tổng hợp dữ liệu định kỳ (`@Scheduled`). |
| `ai` | 3 luồng **Google Gemini AI** có quota theo IP/tài khoản/ngày và tự chuyển model dự phòng: (1) Trợ lý tư vấn thực đơn cho khách, (2) Trợ lý vận hành cho bếp/phục vụ, (3) Tự động sinh mô tả & thẻ hương vị món ăn/combo. |
| `recommendation` | Gợi ý món ăn phổ biến & cá nhân hóa cho khách trên trang thực đơn. |
| `publicapi` | API công khai tối ưu hiệu năng có cache đa tầng & rate-limit cho khách quét QR. |
| `notification` | Đồng bộ realtime qua **STOMP over WebSocket / SockJS** phân quyền theo Role Hierarchy. |
| `settings` | Cấu hình thông tin nhà hàng, logo, mẫu hóa đơn in (Wifi, lời cảm ơn, chống XSS), âm thanh/thông báo vận hành. |

### Frontend — 17 Feature Modules, 24 Routes, Đa nền tảng
- **Trải nghiệm đa nền tảng:** Web SPA (`Vercel`), **PWA** (Service Worker, chế độ offline `/offline`, trang trạng thái lỗi `/error/:statusCode`), và **Mobile App (Capacitor 8.6)** cho Android (`https://localhost`, `cleartext: false`, `allowBackup: false`) & iOS (`capacitor://localhost`).
- **Song ngữ (i18n Việt / Anh) & Dark Mode:** Chuyển đổi ngôn ngữ và giao diện sáng/tối mượt mà, đồng bộ trạng thái đa tab qua Zustand (`useAdminPreferences`).
- **In hóa đơn an toàn:** Xuất hóa đơn in HTML có escape toàn bộ dữ liệu người dùng (`invoiceGenerator.js`, chống XSS).

---

## 2. MA TRẬN 6 VAI TRÒ NGƯỜI DÙNG (RBAC)

| Vai trò (Role) | Xác thực | Phạm vi quyền hạn & Màn hình chính |
|---|---|---|
| **Customer** | Quét QR bàn (không cần đăng nhập) | Menu tại bàn (`/:tableCode/menu`) hoặc công khai (`/menu`): chọn món/topping/combo, giỏ hàng, gửi đơn, theo dõi trạng thái, yêu cầu thanh toán, chat AI Gemini. |
| **Staff** | Tài khoản nội bộ | Sơ đồ bàn (`/admin/tables`), mở bàn, gọi món tại bàn, gộp/tách bàn, đơn mang về (`/admin/takeaway`), trợ lý AI nội bộ, hồ sơ cá nhân (`/admin/profile`). *(Ẩn thao tác thanh toán & báo bếp xong).* |
| **Chef** | Tài khoản nội bộ | Màn hình bếp KDS (`/admin/kitchen`) chế độ Kanban & Nấu gom món, cập nhật trạng thái món, quản lý & điều chỉnh tồn kho nguyên liệu (`/admin/inventory`), trợ lý AI bếp. |
| **Cashier** | Tài khoản nội bộ | Sơ đồ bàn thu ngân (`/admin/tables`), đơn mang về (`/admin/takeaway`), thanh toán Tiền mặt & PayOS (`PaymentModal`), áp voucher, in hóa đơn, tra cứu lịch sử hóa đơn (`/admin/history`). |
| **Manager** | Tài khoản nội bộ | Dashboard (`/admin/dashboard`), quản trị Danh mục (`/admin/categories`), Thực đơn (`/admin/menu`), Combo (`/admin/combo`), Voucher (`/admin/voucher`), Nhân sự (`/admin/staffs`), Kho (`/admin/inventory`), Báo cáo món bán chạy (`/admin/statistics/top-dishes`) & hiệu suất nhân viên (`/admin/statistics/staff`). |
| **Admin** | Tài khoản nội bộ + **2FA OTP Email** | Toàn quyền hệ thống + **độc quyền Báo cáo Doanh thu chi tiết (`/admin/statistics/revenue`)**, Cấu hình hệ thống (`/admin/settings`), và Prometheus metrics (`/actuator/prometheus`). |

> **Lưu ý tạo tài khoản Admin local:** Mặc định ứng dụng **không tự tạo tài khoản mẫu**. Ở môi trường local (`dev`), có thể bật tạo admin tùy chọn bằng cách đặt `DEV_ADMIN_BOOTSTRAP_ENABLED=true`, `DEV_ADMIN_EMAIL` và `DEV_ADMIN_PASSWORD` trong `.env`. Mã OTP 2FA được gửi qua email (Brevo API hoặc SMTP), không ghi ra log backend.

---

## 3. CÔNG NGHỆ & PHIÊN BẢN

| Phân hệ | Công nghệ sử dụng | Chi tiết kỹ thuật |
|---|---|---|
| **Backend** | Java **25 LTS**, Spring Boot **4.1.2** | Spring Security 7, Jackson 3 (`tools.jackson`), Hibernate **7.4**, MapStruct 1.6, Lombok 1.18, Spotless **3.11** |
| **Database** | PostgreSQL **16+**, Flyway **12.4** | `spring-boot-starter-flyway` (`V1` -> `V1.21`), `ddl-auto: validate`, `baseline-on-migrate: false` |
| **Cache & Security** | Caffeine (Dev) & Redis 7 (Prod) | Redis JSON serializer Jackson 3 (`qros:v4:`), Lua rate limiter, Token Blacklist, 2FA & Refresh Token Store |
| **Realtime** | Spring STOMP over WebSocket, SockJS | Phân quyền subscribe theo Role Hierarchy, tự động invalidate cache React Query |
| **Frontend** | React **19.2**, Vite **7.3**, Tailwind CSS **4** | TanStack React Query v5, Zustand v5, React Router v7, Recharts v3, Lucide Icons, `vite-plugin-pwa` |
| **Mobile** | Capacitor **8.6** | Android (`frontend/android/`, Gradle 8.14 / SDK 36, `androidScheme: https`) & hỗ trợ iOS (`capacitor://localhost`) |
| **Testing & CI** | JUnit 5, ArchUnit 1.5, Node Test, ESLint 9, Playwright | **222** backend tests, **99** frontend unit tests, GitHub Actions CI/CD (**Java 25 + Node 22** + GHCR) |
| **Tích hợp ngoài** | PayOS, Google Gemini AI, Cloudinary, Brevo, Twilio | Thanh toán VietQR webhook, AI đa luồng, ảnh cloud, email HTTPS API, SMS OTP |

---

## 4. CẤU TRÚC DỰ ÁN

```text
.
├── backend/                  # Spring Boot 4.1.2 (15 modules, Flyway V1–V1.21, 222 tests)
├── frontend/                 # React 19 SPA/PWA + Capacitor 8 Android (`frontend/android/`)
│   ├── vercel.json           # Cấu hình deploy Vercel (rewrites API/WS, CSP, cache headers)
│   ├── nginx.conf            # Cấu hình Nginx cho Docker (SPA, proxy API/WS, CSP)
│   └── Dockerfile            # Multi-stage Node 22 -> Nginx Alpine
├── .github/workflows/ci.yml  # CI (Java 25, Node 22) & push Docker images lên GHCR
├── docs/                     # Tài liệu thiết kế, PRD, audit, design-system
├── reports/                  # 8 báo cáo QA, Discovery, Feature/Role Matrix, Bug Report
├── scripts/                  # Playwright E2E/Android audit, backup/restore Postgres, run_checks
├── Dockerfile                # Multi-stage Maven 3.9.16 (JDK 25) -> JRE 25 Alpine (`jarmode=tools`)
├── docker-compose.yml        # Chạy cụm container backend + frontend (test đóng gói / VPS)
├── render.yaml               # Blueprint triển khai backend + Postgres + Redis lên Render
├── .env.example              # Mẫu đầy đủ 40 biến môi trường cho Local & Production
└── README.md                 # Tài liệu tổng quan, cài đặt và vận hành
```

---

## 5. YÊU CẦU MÔI TRƯỜNG & CHẠY LOCAL (`dev`)

### Yêu cầu hệ thống
- **JDK:** `25` trở lên (Maven `3.9+`).
- **Node.js:** `22+` (npm `10+`).
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
- Email (để nhận OTP 2FA Admin / reset password): `BREVO_API_KEY` + `MAIL_USERNAME` (hoặc SMTP `MAIL_USERNAME` + `MAIL_PASSWORD`)

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
# Backend: Spotless 3.11 + Compile + 222 tests (Unit/Slice/ArchUnit) + Package JAR
mvn -f backend/pom.xml verify -B
mvn -f backend/pom.xml spotless:apply   # Tự động format Java & pom.xml

# Frontend: 99 unit tests + ESLint + Vite production build
npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build

# E2E Playwright Audit (6 vai trò x 3 kích thước màn hình)
node scripts/phase4_full_ui_audit.js
```

---

## 7. TRIỂN KHAI PRODUCTION & DOCKER

### Kiến trúc Production (Render + Vercel)
- **Backend (Render):** tự động build từ [Dockerfile](Dockerfile) theo [render.yaml](render.yaml) trên nhánh `master` (`SPRING_PROFILES_ACTIVE=prod`, health check `/actuator/health/liveness`).
- **Frontend (Vercel):** tự động build từ `frontend/` theo [frontend/vercel.json](frontend/vercel.json) với **Node.js 22.x**.

### Checklist biến môi trường trên Render (`prod`)
- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL` (**không** bọc dấu nháy kép), `DB_USERNAME`, `DB_PASSWORD` (DB đã có bảng `flyway_schema_history` khớp `V1`–`V1.21`)
- `REDIS_HOST`, `REDIS_PORT=6379`, `REDIS_SSL_ENABLED=false` *(đặt `true` nếu dùng Redis TLS ngoài)*
- `JWT_SECRET` và `QR_SIGNING_SECRET` *(2 chuỗi 64 ký tự hex riêng biệt sinh bằng `openssl rand -hex 32`)*
- `APP_BASE_URL=https://order-by-qr.onrender.com`
- `APP_FRONTEND_BASE_URL=https://wqros.vercel.app`
- `APP_CORS_ALLOWED_ORIGINS=https://wqros.vercel.app,https://localhost,capacitor://localhost` *(bắt buộc HTTPS + `capacitor://localhost` cho iOS; không dùng `http://localhost` ở `prod`)*
- `APP_TRUST_PROXY_HEADERS=true`
- `BREVO_API_KEY` + `MAIL_USERNAME` *(sender đã xác minh trên Brevo)*

### Build Docker Images & Chạy cụm Docker Compose (tùy chọn cho demo / VPS)
```bash
docker build -t order-by-qr-backend:latest -f Dockerfile .
docker build -t order-by-qr-frontend:latest -f frontend/Dockerfile frontend
docker compose up -d
```

---

## 8. TÀI LIỆU & BÁO CÁO NGHIỆM THU

Toàn bộ báo cáo khảo sát, phân quyền và kiểm thử được lưu tại [reports/](reports/):
1. [reports/PROJECT_DISCOVERY.md](reports/PROJECT_DISCOVERY.md) — Khảo sát route, modal, form và kiến trúc component.
2. [reports/FEATURE_INVENTORY.md](reports/FEATURE_INVENTORY.md) — Danh mục chi tiết tính năng toàn hệ thống.
3. [reports/ROLE_FEATURE_MATRIX.md](reports/ROLE_FEATURE_MATRIX.md) — Ma trận phân quyền 6 vai trò.
4. [reports/SCREENSHOT_INVENTORY.md](reports/SCREENSHOT_INVENTORY.md) — Mục lục ảnh chụp giao diện theo vai trò và thiết bị.
5. [reports/QA_TEST_RESULTS.md](reports/QA_TEST_RESULTS.md) & [reports/COMPREHENSIVE_TEST_REPORT.md](reports/COMPREHENSIVE_TEST_REPORT.md) — Kết quả kiểm thử tự động & đánh giá chất lượng.
6. [reports/RESPONSIVE_AUDIT.md](reports/RESPONSIVE_AUDIT.md) — Kiểm toán giao diện Desktop, Tablet và Mobile.
7. [reports/BUG_REPORT.md](reports/BUG_REPORT.md) — Danh sách lỗi phát hiện và hướng khắc phục.
