# QROS Backend

Backend QROS là ứng dụng Spring Boot cung cấp REST API, WebSocket realtime, tích hợp thanh toán và các tác vụ nền cho hệ thống đặt món bằng QR.

- Java: `21`
- Spring Boot: `3.4.1`
- Database: PostgreSQL, quản lý schema bằng Flyway
- Cache: Redis
- Auth: JWT access token + HTTP-only refresh cookie

## Kiến Trúc

```text
src/main/java/com/qros/
├── QrosApplication.java
├── core/
│   └── config/              # App config, CORS, Security, Redis, WebSocket, timezone, seed dev
├── infrastructure/
│   ├── cache/               # Cache abstraction + Redis implementation
│   ├── mail/                # SMTP email service
│   └── storage/             # Cloudinary upload service
├── modules/
│   ├── ai/                  # Gemini chat assistant
│   ├── analytics/           # Revenue, employee, dish stats and forecasts
│   ├── auth/                # Login, refresh, logout, password reset
│   ├── kitchen/             # Kitchen board and item status updates
│   ├── menu/                # Categories, menu items, combos, public menu
│   ├── notification/        # Application events -> WebSocket topics
│   ├── order/               # Order lifecycle, pricing, audit, state machine
│   ├── payment/             # PayOS payment links, webhook, cleanup job
│   ├── promotion/           # Voucher validation and usage tracking
│   ├── recommendation/      # Popular, similar, cross-sell recommendations
│   ├── settings/            # Restaurant/system settings
│   ├── table/               # Dining tables and QR generation
│   └── user/                # Staff account and profile management
└── shared/
    ├── entity/              # BaseEntity auditing
    ├── exception/           # Global exception handling
    ├── response/            # ApiResponse / ErrorResponse
    ├── security/            # JWT filter, JwtService, rate limiting
    ├── transaction/         # Side effects after transaction commit
    └── util/                # AppTime and shared utilities
```

`QrosApplication` bật `@EnableCaching`, `@EnableJpaAuditing`, `@EnableAsync` và `@EnableScheduling`.

## Công Nghệ Chính

| Nhóm | Thư viện/Công nghệ |
| --- | --- |
| Web/API | Spring Boot Web, Validation |
| Persistence | Spring Data JPA, PostgreSQL driver, Flyway |
| Security | Spring Security, JJWT |
| Mapping/Boilerplate | MapStruct, Lombok |
| Realtime | Spring WebSocket, STOMP, SockJS |
| Cache | Spring Data Redis |
| Payment | `vn.payos:payos-java` |
| Storage | Cloudinary |
| Mail | Spring Boot Mail |
| Metrics | Spring Actuator, Micrometer Prometheus |
| Test | JUnit 5, Mockito, Spring Boot Test, ArchUnit |

## Cấu Hình Môi Trường

Backend đọc biến môi trường qua Spring và `spring-dotenv`. Trong repo hiện tại, `backend/.env` là symlink trỏ tới `../.env`.

Tạo file `.env` ở root. Nếu đang đứng trong thư mục `backend/`:

```bash
cp ../.env.example ../.env
```

Nếu đang đứng ở root dự án:

```bash
cp .env.example .env
```

Các biến bắt buộc/quan trọng:

| Biến | Mô tả |
| --- | --- |
| `PORT` | Cổng backend, mặc định `8080` |
| `DB_URL` | JDBC URL PostgreSQL, ví dụ `jdbc:postgresql://localhost:5432/order_by_qr` |
| `DB_USERNAME`, `DB_PASSWORD` | Tài khoản database |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis cache |
| `JWT_SECRET` | Secret ký JWT, nên dài và ngẫu nhiên |
| `JWT_EXPIRATION_MS` | Thời gian sống access token |
| `JWT_REFRESH_EXPIRATION_MS` | Thời gian sống refresh token |
| `JWT_REFRESH_COOKIE_NAME` | Tên cookie refresh token |
| `JWT_REFRESH_COOKIE_SECURE` | `true` cho production HTTPS, `false` cho local HTTP |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP gửi email reset mật khẩu |
| `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | Cấu hình SMTP |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Upload ảnh |
| `APP_BASE_URL` | Public backend URL |
| `APP_FRONTEND_BASE_URL` | Public frontend URL |
| `APP_CORS_ALLOWED_ORIGINS` | Origin được phép, cách nhau bằng dấu phẩy |
| `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` | Thanh toán PayOS |
| `GEMINI_API_KEY`, `GEMINI_API_URL` | Gemini chat assistant |

## Chạy Local

Từ thư mục `backend/`:

```bash
mvn spring-boot:run
```

Backend tự đọc `.env` ở root dự án (`../.env`) hoặc trong thư mục `backend/` nếu có.

Chạy với profile `dev` để seed tài khoản quản lý khi bảng `users` đang rỗng:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Tài khoản seed chỉ tạo khi profile `dev` được bật:

```text
Email: admin@gmail.com
Password: admin123
Role: MANAGER
```

Ứng dụng chạy mặc định tại `http://localhost:8080`.

## Database Và Migration

Flyway migrations nằm tại:

```text
src/main/resources/db/migration/
```

Cấu hình hiện tại:

- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.flyway.baseline-on-migrate=true`
- `spring.flyway.baseline-version=1`
- JDBC/Hibernate timezone: `Asia/Ho_Chi_Minh`

Vì Hibernate chỉ validate schema, hãy tạo database PostgreSQL trước khi chạy app. Flyway sẽ áp migration khi app khởi động.

## Auth Và Phân Quyền

Backend dùng access token JWT trong header:

```http
Authorization: Bearer <access-token>
```

Refresh token được lưu trong HTTP-only cookie, path `/api`. Cookie có `SameSite=None` khi `JWT_REFRESH_COOKIE_SECURE=true`, ngược lại dùng `Lax`.

Roles hiện có:

| Role | Quyền chính |
| --- | --- |
| `MANAGER` | Toàn quyền quản trị menu, bàn, voucher, nhân viên, thống kê, bếp, thanh toán |
| `STAFF` | Vận hành bàn, đơn, thanh toán, lịch sử, một số báo cáo |
| `CHEF` | Xem/cập nhật bếp và một số dữ liệu phục vụ vận hành |

## API Map

| Nhóm | Endpoint chính | Mô tả |
| --- | --- | --- |
| Auth | `/api/auth/*` | Login, refresh, logout, reset mật khẩu email/phone |
| Public customer | `/api/public/*` | Menu public, table by code, current order, recommendations |
| AI | `POST /api/ai/chat` | Chat gợi ý món qua Gemini |
| Categories | `/api/categories` | CRUD danh mục, search, upload ảnh |
| Menu items | `/api/menu-items` | CRUD món, lọc theo danh mục, upload ảnh |
| Combos | `/api/combos` | CRUD combo, active combos, toggle active |
| Tables | `/api/tables` | CRUD bàn, lấy theo code, regenerate QR |
| Orders | `/api/orders` | Tạo đơn, history, stats, active, current order, preview, pay, reconcile |
| Kitchen | `/api/kitchen` | Danh sách món bếp, update item status/prepared |
| Payments | `/api/payments/payos` | Tạo link PayOS, hủy link, đồng bộ trạng thái |
| Webhooks | `/api/webhooks/payos` | PayOS callback |
| Vouchers | `/api/vouchers` | CRUD voucher, validate code |
| Stats | `/api/stats` | Doanh thu, nhân viên, đơn, top món, trend, forecast, dashboard |
| Settings | `/api/settings` | Xem/cập nhật cấu hình nhà hàng |
| Users | `/api/users` | Hồ sơ cá nhân, avatar, nhân viên, reset password |
| Recommendations | `/api/recommendations` | API recommendation nội bộ/admin |

Mọi response chuẩn được bọc bởi `ApiResponse<T>` trừ webhook PayOS trả `ResponseEntity<Map<...>>`.

## WebSocket Realtime

Endpoint SockJS/STOMP:

```text
/ws
```

Broker prefix:

```text
/topic
```

Application prefix:

```text
/app
```

Heartbeat được cấu hình 10 giây mỗi chiều.

Topics đang được frontend subscribe:

| Topic | Mục đích | Bảo vệ |
| --- | --- | --- |
| `/topic/tables` | Trạng thái bàn, đơn, thanh toán tối thiểu cho admin/khách | Public |
| `/topic/kitchen` | Cập nhật bảng bếp | `MANAGER`, `STAFF`, `CHEF` |
| `/topic/users` | Cập nhật nhân viên | `MANAGER` |
| `/topic/vouchers` | Cập nhật voucher | `MANAGER`, `STAFF` |
| `/topic/menu` | Cập nhật món ăn | Public subscribe |
| `/topic/categories` | Cập nhật danh mục | Public subscribe |
| `/topic/combos` | Cập nhật combo | Public subscribe |
| `/topic/settings` | Cập nhật settings public | Public subscribe |

Client có thể gửi header STOMP:

```text
Authorization: Bearer <access-token>
```

Các protected topic sẽ từ chối subscribe nếu thiếu token hoặc role không phù hợp.

## Thanh Toán

PayOS flow chính:

- `POST /api/payments/payos`: tạo payment link cho order.
- `GET /api/payments/payos/{transactionId}`: đồng bộ trạng thái từ PayOS.
- `POST /api/payments/payos/{transactionId}/cancellation`: hủy link thanh toán.
- `POST /api/webhooks/payos`: nhận webhook PayOS.
- `PaymentCleanupService`: chạy mỗi 5 phút để xử lý giao dịch `PENDING` bị treo.

Thanh toán tiền mặt dùng:

```text
POST /api/orders/{orderId}/pay
```

## Trạng Thái Đơn Và Món

Order status:

```text
PENDING -> SERVING -> AWAITING_PAYMENT -> COMPLETED
```

Nhánh hủy:

```text
PENDING | SERVING | AWAITING_PAYMENT -> CANCELLED
```

Order item status:

```text
PENDING -> COOKING -> FINISHED
PENDING | COOKING | FINISHED -> CANCELLED
```

Logic chuyển trạng thái được gom trong `modules/order/state` và `OrderStatusService`. Lịch sử thay đổi được ghi vào các bảng audit tương ứng.

## Cache Và Metrics

Redis cache đang dùng cho:

- Tables/QR codes
- Menu, categories, combos
- Recommendations/popular items
- Vouchers
- Settings
- Order detail/stats
- Revenue/top dishes/employee performance/dashboard stats

Metrics:

- Health: `GET /actuator/health`
- Prometheus: `GET /actuator/prometheus`

## Test Và Build

Chạy test:

```bash
mvn test
```

Build jar:

```bash
mvn clean package
```

Build không chạy test:

```bash
mvn clean package -DskipTests
```

Test hiện có:

- `BackendDependencyRulesTest`
- `SecurityConfigRegressionTest`
- `TimeZoneConfigTest`
- `WebSocketConfigTest`
- `DiscountServiceTest`
- `OrderServiceImplTest`
- `PayosServiceImplTest`
- `RecommendationServiceTest`

## Docker

Backend image được build từ `Dockerfile` ở root dự án, không phải từ thư mục `backend/`:

```bash
docker build -t order-by-qr-backend:latest ..
```

Nếu đang đứng ở root dự án:

```bash
docker build -t order-by-qr-backend:latest .
```

Container expose cổng `8080`, chạy bằng Spring Boot layered jar và JVM timezone `Asia/Ho_Chi_Minh`.
