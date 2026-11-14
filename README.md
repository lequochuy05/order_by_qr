# QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Thông minh

QROS là hệ thống quản lý và vận hành nhà hàng toàn diện (All-in-One Restaurant Platform), tích hợp luồng đặt món thông minh bằng mã QR tại bàn (QR Ordering), hệ thống hiển thị bếp thời gian thực (Kitchen Display System - KDS), quản lý sơ đồ bàn, gộp bàn, thanh toán đa phương thức (Tiền mặt & VietQR PayOS), kiểm soát định mức kho nguyên liệu và báo cáo phân tích doanh thu chuyên sâu.

Hệ thống được thiết kế theo kiến trúc hiện đại: **Spring Boot 3.5.x (Java 21 LTS)** + **React 19 (Vite 7, PWA, Tailwind CSS v4)**, đồng bộ thời gian thực qua **Spring STOMP over WebSocket** và bảo vệ bởi **Enterprise Security AAA (Authentication, Authorization, Auditing)**.

---

## 1. CẤU TRÚC DỰ ÁN

```text
.
├── backend/              # Spring Boot REST API, WebSocket STOMP, JPA, Security
├── frontend/             # React 19 SPA, PWA, Tailwind CSS v4 cho Khách & Quản trị
├── docs/                 # Tài liệu thiết kế hệ thống, PRD, Master Automation Prompt
├── reports/              # Báo cáo QA, Discovery, Role-Feature Matrix, Bug Report
├── scripts/              # Kịch bản tự động hóa Playwright, chạy kiểm thử và backup
├── Dockerfile            # Dockerfile build backend production
├── docker-compose.yml    # Docker Compose chạy dịch vụ phụ trợ (Redis, Postgres)
├── .env.example          # Mẫu cấu hình biến môi trường
└── README.md             # Hướng dẫn cài đặt và vận hành hệ thống
```

---

## 2. MA TRẬN 6 VAI TRÒ NGƯỜI DÙNG & TÀI KHOẢN TRẢI NGHIỆM

Hệ thống hỗ trợ phân quyền kiểm soát truy cập dựa trên vai trò (RBAC) nghiêm ngặt cho **6 nhóm người dùng**:

| Vai trò (Role) | Tài khoản mẫu (Email) | Mật khẩu | Phạm vi quyền hạn & Tính năng chính |
|---|---|---|---|
| **Customer** | *(Không cần đăng nhập)* | *(Quét QR)* | Quét mã QR tại bàn (`/T01/menu`) hoặc xem công khai (`/menu`). Chọn món, thêm topping, giỏ hàng, gửi đơn xuống bếp, theo dõi đơn, trò chuyện với Trợ lý AI Gemini. |
| **Staff** | `staff@gmail.com` | `staff123` | Quản lý sơ đồ bàn (`/admin/tables`), mở bàn, gọi món tại bàn, gộp bàn / tách bàn, quản lý đơn mang về (`/admin/takeaway`), hồ sơ cá nhân. |
| **Chef** | `chef@gmail.com` | `chef123` | Màn hình Bếp KDS (`/admin/kitchen`) realtime, chuyển đổi chế độ Kanban & Nấu gom món (`BatchCookingView`), cập nhật trạng thái món, kiểm tra kho nguyên liệu (`/admin/inventory`). |
| **Cashier** | `cashier@gmail.com` | `cashier123` | Sơ đồ bàn thu ngân (`/admin/tables`), lập hóa đơn (`PaymentModal`), thanh toán tiền mặt & chuyển khoản VietQR PayOS, áp mã giảm giá, tra cứu lịch sử hóa đơn (`/admin/history`). |
| **Manager** | `manager@gmail.com` | `manager123` | Dashboard kinh doanh (`/admin/dashboard`), quản trị Thực đơn (`/admin/menu`), Danh mục (`/admin/categories`), Combo (`/admin/combo`), Voucher (`/admin/voucher`), Quản lý Nhân sự (`/admin/staffs`), Báo cáo món bán chạy & hiệu suất nhân viên. |
| **Admin** | `admin@gmail.com` | `admin123` | **Xác thực 2 lớp (2FA OTP)**, toàn quyền hệ thống + **Độc quyền Báo cáo Doanh thu chi tiết (`/admin/statistics/revenue`)**, Cấu hình hệ thống nhà hàng toàn diện (`/admin/settings`). |

> **Lưu ý 2FA của Admin:** Khi đăng nhập tài khoản `admin@gmail.com`, hệ thống sẽ yêu cầu nhập mã OTP 6 số. Trong môi trường local/dev, mã OTP được tự động ghi vào log backend và lưu trữ tại Redis (`qros:2fa:ticket:*`).

---

## 3. CÔNG NGHỆ CỐT LÕI

| Phân hệ | Công nghệ sử dụng | Điểm nổi bật kỹ thuật |
|---|---|---|
| **Backend** | Java 21 LTS, Spring Boot 3.5.14 | Spring Security 6, Virtual Threads ready, Spring Data JPA (Hibernate 6.6) |
| **Database** | PostgreSQL 17, Flyway Migration | Quản lý versioned migrations (V1.0 -> V1.21), Pessimistic & Optimistic Locking (@Version) |
| **Cache & Lock** | Caffeine (Local) & Redis 7 (Distributed) | Multi-tier Caching, Rate limiting chống brute-force qua Lua Script, Token Revocation |
| **Realtime** | Spring STOMP over WebSocket, SockJS | Bi-directional signals, tự động invalidate cache Client không cần polling |
| **Frontend** | React 19.2, Vite 7.2, Tailwind CSS 4 | Single Page Application (SPA), PWA enabled, Lucide React icons, Recharts v3 |
| **Data Fetching** | TanStack React Query v5, Zustand v5 | Network-first caching, Server-state management, Optimistic UI updates |
| **Testing/QA** | Playwright v1.65 (Chromium), JUnit 5 | Kiểm thử tự động E2E đa vai trò, đa thiết bị (Desktop, Tablet, Mobile) |
| **Tích hợp ngoài** | VietQR PayOS, Google Gemini Flash AI, Cloudinary | Thanh toán tự động webhook, AI tư vấn món ăn thông minh, lưu trữ ảnh cloud |

---

## 4. YÊU CẦU MÔI TRƯỜNG & HẠ TẦNG

- **Java Development Kit (JDK):** Phiên bản `21` trở lên.
- **Node.js & npm:** Node.js `20+`, npm `10+`.
- **Maven:** Phiên bản `3.9+`.
- **Docker:** Chạy Redis container (`redis:7-alpine`).
- **PostgreSQL:** Phiên bản `16+` (hoặc kết nối AWS Neon Cloud).

---

## 5. HƯỚNG DẪN CÀI ĐẶT & CHẠY LOCAL (LOCAL SETUP)

### Bước 1: Khởi động Redis
```bash
docker run -d --name qros-redis -p 6379:6379 redis:7-alpine
# Hoặc nếu container đã có sẵn:
docker start qros-redis
```

### Bước 2: Cấu hình biến môi trường
Tạo file `.env` tại thư mục gốc từ file mẫu:
```bash
cp .env.example .env
```
Điền các thông số kết nối Database `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và kết nối Redis `REDIS_HOST=localhost`, `REDIS_PORT=6379`.

### Bước 3: Khởi động Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- Backend sẽ tự động chạy Flyway migration để cập nhật database lên phiên bản mới nhất.
- Địa chỉ phục vụ: `http://localhost:8080`
- Kiểm tra sức khỏe: `http://localhost:8080/actuator/health`

### Bước 4: Khởi động Frontend (React / Vite)
Mở một cửa sổ terminal mới:
```bash
cd frontend
npm install
npm run dev
```
- Địa chỉ ứng dụng web: `http://localhost:5173`

---

## 6. HƯỚNG DẪN CHẠY KIỂM THỬ TỰ ĐỘNG PLAYWRIGHT (QA AUTOMATION)

Dự án đã tích hợp sẵn bộ kịch bản kiểm thử tự động toàn diện cho 6 vai trò và 3 kích thước thiết bị tại [scripts/phase4_full_ui_audit.js](scripts/phase4_full_ui_audit.js).

Để kích hoạt kiểm thử tự động và thu thập 144 ảnh chụp màn hình:
```bash
node scripts/phase4_full_ui_audit.js
```

Sau khi hoàn tất:
- Toàn bộ ảnh chụp màn hình chất lượng cao được lưu tại: `screenshots/`
- Báo cáo chỉ số kiểm thử JSON: `artifacts/ui-audit/audit_summary.json`
- Log lỗi trình duyệt và mạng: `artifacts/ui-audit/logs/`

---

## 7. CÁC TÀI LIỆU BÁO CÁO NGIỆM THU (REPORTS)

Chi tiết kết quả khảo sát và kiểm toán chất lượng được lưu trữ trong thư mục [reports/](reports/):

1. [reports/PROJECT_DISCOVERY.md](reports/PROJECT_DISCOVERY.md): Khảo sát 24 Route, 18 Modals, 14 Forms và kiến trúc component.
2. [reports/ROLE_FEATURE_MATRIX.md](reports/ROLE_FEATURE_MATRIX.md): Ma trận phân quyền chi tiết 6 vai trò trên toàn hệ thống.
3. [reports/SCREENSHOT_INVENTORY.md](reports/SCREENSHOT_INVENTORY.md): Mục lục chi tiết 144 ảnh chụp giao diện theo vai trò và thiết bị.
4. [reports/QA_TEST_RESULTS.md](reports/QA_TEST_RESULTS.md): Kết quả kiểm thử tự động, tỷ lệ pass và phân tích log lỗi runtime.
5. [reports/RESPONSIVE_AUDIT.md](reports/RESPONSIVE_AUDIT.md): Kiểm toán độ tương thích responsive trên Desktop, Tablet (iPad) và Mobile (iPhone).
6. [reports/BUG_REPORT.md](reports/BUG_REPORT.md): Báo cáo 5 lỗi phát hiện được và giải pháp khắc phục triệt để.

---

## 8. HƯỚNG DẪN MERGE & TRIỂN KHAI LÊN PRODUCTION

### Bước 1: Build và kiểm tra mã nguồn
```bash
# Kiểm tra backend
mvn -f backend/pom.xml clean test-compile

# Kiểm tra frontend build
npm --prefix frontend run build
```

### Bước 2: Commit các thay đổi trên nhánh refactor
```bash
git add -A
git commit -m "feat(core): harden security rbac, websocket hierarchy, 2fa rate limit and clean gitignore"
git push origin refactor/architecture-improvements
```

### Bước 3: Hợp nhất (Merge) vào nhánh `master`
```bash
git checkout master
git pull origin master
git merge refactor/architecture-improvements
git push origin master
```

### Bước 4: Triển khai Production (Docker / Cloud Hosting)
Build Docker container cho backend:
```bash
docker build -t qros-backend:latest .
```
Hoặc cấu hình pipeline tự động triển khai qua [render.yaml](render.yaml) cho dịch vụ đám mây (Render / Vercel).
