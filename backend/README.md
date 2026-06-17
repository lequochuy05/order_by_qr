# QROS Backend

Backend QROS là ứng dụng Spring Boot cung cấp REST API, WebSocket realtime, xác thực JWT, tích hợp thanh toán và các tác vụ nền cho hệ thống đặt món bằng QR.

- Java: `21`
- Spring Boot: `3.5.14`
- Database: PostgreSQL, quản lý schema bằng Flyway
- Cache: Redis
- Auth: JWT access token + HTTP-only refresh cookie

## Kiến trúc

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
│   ├── inventory/           # Inventory items, recipes, stock movements, reservations
│   ├── kitchen/             # Kitchen board and item status updates
│   ├── menu/                # Categories, menu items, combos
│   ├── notification/        # Application events -> WebSocket topics
│   ├── order/               # Order lifecycle, pricing, audit, state machine
│   ├── payment/             # PayOS payment links, webhook, cleanup job
│   ├── promotion/           # Voucher validation and usage tracking
│   ├── publicapi/           # Public customer menu/order/session APIs
│   ├── recommendation/      # Popular, similar, cross-sell recommendations
│   ├── settings/            # Restaurant/system settings
│   ├── table/               # Dining tables, QR generation, table sessions
│   └── user/                # Staff account and profile management
└── shared/
    ├── cache/               # Cache names/constants
    ├── entity/              # BaseEntity auditing
    ├── enums/               # Shared enums
    ├── event/               # Domain event publisher
    ├── exception/           # Global exception handling
    ├── rate_limit/          # Rate limiting filter
    ├── response/            # ApiResponse / ErrorResponse
    ├── security/            # JWT filter, JwtService
    ├── time/                # AppTime
    └── transaction/         # Side effects after transaction commit
```

`QrosApplication` bật `@EnableCaching`, `@EnableJpaAuditing`, `@EnableAsync` và `@EnableScheduling`.

## Công nghệ chính

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

## Cấu hình môi trường

Backend đọc biến môi trường qua Spring config import và `spring-dotenv`. Tạo file `.env` ở root dự án:

```bash
cp .env.example .env
```

Nếu đang đứng trong thư mục `backend/`:

```bash
cp ../.env.example ../.env
```

Backend tự đọc `.env` tại root dự án (`../.env`) hoặc file `.env` trong thư mục `backend/`.

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
| `GEMINI_ENABLED` | Bật/tắt Gemini, mặc định `true` |
| `GEMINI_API_KEY`, `GEMINI_API_URL` | Gemini chat assistant |
| `GEMINI_CONNECT_TIMEOUT_SECONDS`, `GEMINI_READ_TIMEOUT_SECONDS` | Timeout Gemini |
| `GEMINI_TEMPERATURE`, `GEMINI_TOP_P`, `GEMINI_MAX_OUTPUT_TOKENS` | Tham số sinh câu trả lời Gemini |

## Chạy local

Từ thư mục `backend/`:

```bash
mvn spring-boot:run
```

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

Ứng dụng chạy mặc định tại:

```text
http://localhost:8080
```

## Database và migration

Flyway migrations nằm tại:

```text
src/main/resources/db/migration/
```

Các migration hiện có:

```text
V1__Initial.sql
V1.1__FK_Indexes.sql
V1.3__Table_Sessions.sql
```

Cấu hình hiện tại:

- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.flyway.baseline-on-migrate=true`
- `spring.flyway.baseline-version=1`
- JDBC/Hibernate timezone: `Asia/Ho_Chi_Minh`

Vì Hibernate chỉ validate schema, hãy tạo database PostgreSQL trước khi chạy app. Flyway sẽ áp migration khi app khởi động.

## Auth và phân quyền

Backend dùng access token JWT trong header:

```http
Authorization: Bearer <access-token>
```

Refresh token được lưu trong HTTP-only cookie, path `/api`. Cookie có `SameSite=None` khi `JWT_REFRESH_COOKIE_SECURE=true`, ngược lại dùng `Lax`.

Roles hiện có:

| Role | Quyền chính |
| --- | --- |
| `MANAGER` | Quản trị menu, bàn, voucher, nhân viên, kho, thống kê, bếp, thanh toán, settings |
| `STAFF` | Vận hành bàn, đơn, thanh toán, lịch sử, đọc dữ liệu phục vụ vận hành |
| `CHEF` | Xem/cập nhật bếp và truy cập một số màn hình vận hành |

Một số nhóm route theo `SecurityRoutes`:

- Public: `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`, `/api/public/**`, `/api/webhooks/**`, `/ws/**`, `/actuator/health`.
- Self profile: `/api/users/me`, `/api/users/me/password`, `/api/users/me/avatar`.
- Manager-only write: user, menu, category, combo, inventory, table, voucher, settings, AI chat.
- Operation: orders, tables, kitchen, inventory, analytics.
- Payment: `/api/payments/**`.

## API map

| Nhóm | Endpoint chính | Mô tả |
| --- | --- | --- |
| Auth | `/api/auth/*` | Login, refresh, logout, reset mật khẩu email/phone |
| Public customer | `/api/public/*` | Menu public, table by code, table session, current order, order preview/create |
| AI | `POST /api/ai/chat` | Chat gợi ý món qua Gemini |
| Categories | `/api/categories` | CRUD danh mục, search, upload ảnh |
| Menu items | `/api/menu-items` | CRUD món, lọc theo danh mục, upload ảnh |
| Combos | `/api/combos` | CRUD combo, active combos, toggle active |
| Inventory | `/api/inventory/*` | Nguyên liệu, công thức, nhập/xuất kho, reservation |
| Tables | `/api/tables` | CRUD bàn, lấy theo code, regenerate QR |
| Orders | `/api/orders` | Tạo đơn, history, analytics, active, current order, preview, pay, reconcile |
| Kitchen | `/api/kitchen` | Danh sách món bếp, update item status/prepared |
| Payments | `/api/payments/payos` | Tạo link PayOS, hủy link, đồng bộ trạng thái |
| Webhooks | `/api/webhooks/payos` | PayOS callback |
| Vouchers | `/api/vouchers` | CRUD voucher, validate code |
| Analytics | `/api/analytics` | Doanh thu, nhân viên, đơn, top món, trend, forecast, dashboard |
| Settings | `/api/settings` | Xem/cập nhật cấu hình nhà hàng |
| Users | `/api/users` | Hồ sơ cá nhân, avatar, nhân viên, reset password |
| Recommendations | `/api/recommendations` | API recommendation public/admin |

Mọi response chuẩn được bọc bởi `ApiResponse<T>` trừ webhook PayOS trả `ResponseEntity<Map<...>>`.

## WebSocket realtime

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

Topics frontend đang subscribe:

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
| `/topic/orders` | Cập nhật order/current order | Public/auth tùy payload |

Client có thể gửi header STOMP:

```text
Authorization: Bearer <access-token>
```

Các protected topic sẽ từ chối subscribe nếu thiếu token hoặc role không phù hợp.

## Thanh toán

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

## Trạng thái đơn và món

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

## Cache và metrics

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

`/actuator/prometheus` yêu cầu quyền `MANAGER`; `/actuator/health` public.

## Test và build

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
- `OrderCreationServiceTest`
- `OrderPricingServiceTest`
- `OrderServiceImplTest`
- `OrderStatusServiceTest`
- `PayosServiceImplTest`
- `RecommendationServiceTest`
- `TableSessionServiceTest`

## Docker

Backend image được build từ `Dockerfile` ở root dự án, không phải từ thư mục `backend/`.

Nếu đang đứng ở root dự án:

```bash
docker build -t order-by-qr-backend:latest .
```

Nếu đang đứng trong thư mục `backend/`:

```bash
docker build -t order-by-qr-backend:latest ..
```

Container expose cổng `8080`, chạy bằng Spring Boot layered jar và JVM timezone `Asia/Ho_Chi_Minh`.
