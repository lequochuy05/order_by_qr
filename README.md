# QROS - Hệ thống gọi món bằng mã QR

QROS là nền tảng vận hành nhà hàng/quán ăn với luồng đặt món bằng mã QR tại bàn. Khách hàng quét mã QR để xem thực đơn, thêm món hoặc combo vào giỏ hàng, gửi đơn đến bếp và theo dõi trạng thái phục vụ theo thời gian thực.

Nhân viên và quản lý sử dụng trang quản trị để vận hành bàn, đơn hàng, bếp, thanh toán, kho, voucher, nhân sự, cấu hình nhà hàng và báo cáo kinh doanh.

## Tổng quan

```text
.
├── backend/              # Spring Boot REST API, WebSocket, business modules
├── frontend/             # React/Vite SPA cho khách và trang quản trị
├── Dockerfile            # Dockerfile backend
├── docker-compose.yml    # Chạy image backend/frontend đã build
├── .env.example          # Mẫu biến môi trường backend
└── README.md
```

Tài liệu backend chi tiết hơn nằm ở `backend/README.md`.

## Chức năng chính

### Khách hàng

- Quét mã QR theo `tableCode` để mở thực đơn tại bàn.
- Xem danh mục, món ăn, combo và gợi ý món.
- Thêm món vào giỏ hàng, gửi đơn trực tiếp tới bếp.
- Theo dõi đơn đang phục vụ theo thời gian thực.
- Thanh toán bằng tiền mặt hoặc PayOS.

### Nhân viên và quản lý

- Quản lý bàn ăn, trạng thái bàn và mã QR.
- Quản lý danh mục, món ăn, combo và ảnh món.
- Quản lý kho, nguyên liệu, công thức và biến động tồn kho.
- Quản lý voucher, nhân sự, hồ sơ cá nhân và phân quyền.
- Theo dõi bếp, lịch sử đơn, thanh toán và đối soát trạng thái.
- Xem dashboard, doanh thu, món bán chạy, hiệu suất nhân viên và dự báo.
- Cấu hình thông tin nhà hàng và các thiết lập hệ thống.

### Bếp và realtime

- Bếp nhận đơn mới qua WebSocket.
- Cập nhật trạng thái món: `PENDING`, `COOKING`, `FINISHED`, `CANCELLED`.
- Frontend tự làm mới dữ liệu liên quan khi có sự kiện realtime cho bàn, bếp, menu, combo, voucher, nhân viên và settings.

### Tích hợp

- PayOS cho thanh toán trực tuyến và webhook.
- Cloudinary cho upload ảnh.
- Gemini AI cho trợ lý gợi ý món.
- Redis cache.
- Spring Actuator và Prometheus metrics.
- Flyway migration cho database schema.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.14, Spring Security, Spring Data JPA, Validation |
| Database | PostgreSQL, Flyway, Hibernate |
| Cache & Realtime | Redis, STOMP WebSocket, SockJS |
| Authentication | JWT access token, HTTP-only refresh cookie |
| Mapping/Boilerplate | MapStruct, Lombok |
| AI & tích hợp | Gemini AI, PayOS, Cloudinary, SMTP |
| Monitoring | Spring Actuator, Micrometer Prometheus |
| Frontend | React 19.2, Vite 7.2, Tailwind CSS 4 |
| Frontend data/UI | TanStack Query, Axios, Zustand, Recharts, TensorFlow.js, lucide-react |
| Triển khai | Docker, Nginx, Vercel/Render |

## Yêu cầu môi trường

- JDK 21+
- Maven 3.9+
- Node.js 20+
- npm 10+
- PostgreSQL
- Redis

Các dịch vụ ngoài cần cấu hình nếu dùng đầy đủ chức năng:

- PayOS
- Cloudinary
- Gemini AI
- SMTP Mail

## Cấu hình môi trường

Tạo file `.env` ở root dự án:

```bash
cp .env.example .env
```

Backend đọc biến môi trường từ `.env` tại root hoặc từ `backend/.env` nếu bạn chạy trong thư mục backend.

Các biến quan trọng:

| Biến | Mô tả |
| --- | --- |
| `PORT` | Cổng backend, mặc định `8080` |
| `DB_URL` | JDBC URL PostgreSQL, ví dụ `jdbc:postgresql://localhost:5432/order_by_qr` |
| `DB_USERNAME`, `DB_PASSWORD` | Tài khoản database |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Kết nối Redis |
| `JWT_SECRET` | Secret ký JWT |
| `JWT_EXPIRATION_MS` | Thời gian sống access token |
| `JWT_REFRESH_EXPIRATION_MS` | Thời gian sống refresh token |
| `JWT_REFRESH_COOKIE_NAME` | Tên refresh cookie |
| `JWT_REFRESH_COOKIE_SECURE` | `true` khi chạy production HTTPS |
| `MAIL_*` | Cấu hình SMTP |
| `CLOUDINARY_*` | Cấu hình Cloudinary |
| `APP_BASE_URL` | Public URL backend |
| `APP_FRONTEND_BASE_URL` | Public URL frontend |
| `APP_CORS_ALLOWED_ORIGINS` | Danh sách origin được phép, cách nhau bằng dấu phẩy |
| `PAYOS_*` | Thông tin PayOS |
| `GEMINI_API_KEY`, `GEMINI_API_URL` | Cấu hình Gemini |
| `GEMINI_ENABLED` | Bật/tắt Gemini, mặc định `true` |

Frontend dùng các biến Vite:

```text
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

File local hiện có:

- `frontend/.env.development`
- `frontend/.env.production`

## Chạy local

### Backend

Tạo database PostgreSQL trước, cấu hình `.env`, sau đó chạy:

```bash
cd backend
mvn spring-boot:run
```

Chạy với profile `dev` để seed tài khoản quản lý khi bảng `users` đang rỗng:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Tài khoản seed:

```text
Email: admin@gmail.com
Password: admin123
Role: MANAGER
```

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

## Địa chỉ mặc định

| Thành phần | Địa chỉ |
| --- | --- |
| Backend API | `http://localhost:8080` |
| WebSocket | `http://localhost:8080/ws` |
| Frontend | `http://localhost:5173` |
| Trang khách | `http://localhost:5173/menu?tableCode=<ma-ban>` |
| Đăng nhập quản trị | `http://localhost:5173/login` |
| Dashboard | `http://localhost:5173/admin/dashboard` |
| Prometheus metrics | `http://localhost:8080/actuator/prometheus` |

## Route chính

### Frontend

| Route | Quyền |
| --- | --- |
| `/menu?tableCode=<ma-ban>` | Public |
| `/login` | Public |
| `/admin/dashboard`, `/admin/profile`, `/admin/settings` | `MANAGER`, `STAFF`, `CHEF` |
| `/admin/tables`, `/admin/history` | `MANAGER`, `STAFF` |
| `/admin/kitchen` | `MANAGER`, `CHEF` |
| `/admin/categories`, `/admin/menu`, `/admin/combo`, `/admin/inventory`, `/admin/voucher`, `/admin/staffs` | `MANAGER` |
| `/admin/statistics/revenue`, `/admin/statistics/top-dishes`, `/admin/statistics/staff` | `MANAGER` |

### Backend

| Nhóm | Endpoint |
| --- | --- |
| Auth | `/api/auth/*` |
| Public customer | `/api/public/*` |
| AI | `/api/ai/*` |
| Menu/category/combo | `/api/categories`, `/api/menu-items`, `/api/combos` |
| Table/order/kitchen | `/api/tables`, `/api/orders`, `/api/kitchen` |
| Payment | `/api/payments/payos`, `/api/webhooks/payos` |
| Inventory | `/api/inventory/*` |
| Voucher/analytics/settings/users | `/api/vouchers`, `/api/analytics`, `/api/settings`, `/api/users` |

## Database và migration

Flyway migrations nằm tại:

```text
backend/src/main/resources/db/migration/
```

Backend đang cấu hình:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh
```

Vì Hibernate chỉ validate schema, mọi thay đổi database nên đi qua Flyway migration trước khi ứng dụng khởi động.

## Kiểm thử và build

### Format code

Trước khi commit, maintainer nên chạy format cho phần code đã thay đổi:

```bash
cd frontend
npx prettier --write .
```

```bash
cd backend
mvn spotless:apply
mvn spotless:check
```

### Backend

```bash
cd backend
mvn test
mvn clean package
```

### Frontend

```bash
cd frontend
npm run lint
npm run build
```

Hiện frontend chưa có bộ test tự động riêng; `lint` và `build` là bước kiểm tra chính.

## Docker

Build image backend:

```bash
docker build -t order-by-qr-backend:latest .
```

Build image frontend:

```bash
cd frontend
docker build -t order-by-qr-frontend:latest .
```

Chạy bằng Docker Compose từ root:

```bash
docker compose up
```

`docker-compose.yml` hiện chỉ chạy hai image `backend` và `frontend`; PostgreSQL/Redis cần được cấu hình bên ngoài thông qua `.env`.

## Ghi chú vận hành

- Múi giờ hệ thống: `Asia/Ho_Chi_Minh`.
- Backend expose health và prometheus qua Actuator.
- Refresh token dùng HTTP-only cookie, access token gửi qua header `Authorization: Bearer <token>`.
- Khi chạy production HTTPS, đặt `JWT_REFRESH_COOKIE_SECURE=true` và cấu hình `APP_CORS_ALLOWED_ORIGINS` đúng domain frontend.
