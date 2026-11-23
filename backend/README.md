# QROS Backend

Backend QROS là ứng dụng Spring Boot cung cấp REST API, WebSocket realtime, xác thực JWT (kèm 2FA OTP cho Admin), tích hợp thanh toán và các tác vụ nền cho hệ thống đặt món bằng QR.

- Java: `21`
- Spring Boot: `3.5.14`
- Database: PostgreSQL, quản lý schema bằng Flyway (`22` migrations: `V1` -> `V1.21`)
- Cache: Caffeine (mặc định ở `dev`) / Redis (`prod`)
- Auth: JWT access token + HTTP-only refresh cookie (path `/api/v1`)

## Kiến trúc

```text
src/main/java/com/qros/
├── QrosApplication.java
├── core/
│   └── config/              # App config, CORS, Security, Redis, WebSocket, timezone, seed dev
├── infrastructure/
│   ├── cache/               # Cache abstraction + Caffeine/Redis implementation
│   ├── mail/                # SmtpEmailService (Brevo REST API over HTTPS + SMTP fallback)
│   ├── sms/                 # SMS OTP service
│   └── storage/             # Cloudinary upload service
├── modules/
│   ├── ai/                  # Gemini chat assistant, menu description & staff query
│   ├── analytics/           # Revenue, employee, dish stats and forecasts
│   ├── auth/                # Login, 2FA OTP, refresh, logout, password reset
│   ├── inventory/           # Inventory items, recipes, stock movements, reservations
│   ├── kitchen/             # Kitchen board and item status updates
│   ├── menu/                # Categories, menu items, combos
│   ├── notification/        # Application events -> WebSocket topics
│   ├── order/               # Order lifecycle, pricing, audit, state machine
│   ├── payment/             # PayOS payment links, webhook, cleanup job
│   ├── promotion/           # Voucher validation and usage tracking
│   ├── publicapi/           # Public customer menu/order/session/settings/AI APIs
│   ├── recommendation/      # Popular, similar, cross-sell recommendations
│   ├── settings/            # Restaurant/system settings
│   ├── table/               # Dining tables, QR generation, table sessions & merge
│   └── user/                # Staff account and profile management
└── shared/
    ├── cache/               # Cache names/constants
    ├── constants/           # Shared domain/application constants
    ├── entity/              # BaseEntity auditing
    ├── enums/               # Shared enums
    ├── event/               # Domain event publisher
    ├── exception/           # Global exception handling
    ├── idempotency/         # Durable order/request idempotency service
    ├── observability/       # Metrics, logging and tracing helpers
    ├── rate_limit/          # Rate limiting filter
    ├── response/            # ApiResponse / ErrorResponse
    ├── security/            # JWT filter, JwtService
    ├── time/                # AppTime
    ├── transaction/         # Side effects after transaction commit
    └── validation/          # Custom validators
```

`QrosApplication` bật `@EnableCaching`, `@EnableJpaAuditing`, `@EnableAsync`, `@EnableScheduling` và `@ConfigurationPropertiesScan`.

## Công nghệ chính

| Nhóm | Thư viện/Công nghệ |
| --- | --- |
| Web/API | Spring Boot Web, Validation |
| Persistence | Spring Data JPA, PostgreSQL driver, Flyway |
| Security | Spring Security, JJWT, RoleHierarchy, Admin 2FA OTP |
| Mapping/Boilerplate | MapStruct, Lombok |
| Realtime | Spring WebSocket, STOMP, SockJS |
| Cache | Caffeine (mặc định dev), Spring Data Redis (prod, token store, rate limit, 2FA) |
| Payment | `vn.payos:payos-java` |
| QR Code | ZXing |
| Storage | Cloudinary |
| Mail | `SmtpEmailService` — Brevo REST API (HTTPS) khi có `BREVO_API_KEY`, fallback Spring Boot Mail (SMTP) |
| Config & Formatting | `spring-dotenv`, Spotless (Maven `validate` phase), GraalVM `native` profile |
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
| `SPRING_PROFILES_ACTIVE` | Profile kích hoạt (`dev`, `prod`, ...) |
| `DB_URL` | JDBC URL PostgreSQL, ví dụ `jdbc:postgresql://localhost:5432/order_by_qr` |
| `DB_USERNAME`, `DB_PASSWORD` | Tài khoản database |
| `SPRING_CACHE_TYPE` | Loại Spring Cache (`caffeine` mặc định ở `dev`, `redis` ở `prod`) |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Kết nối Redis (cache prod, refresh token store, rate limit, 2FA OTP) |
| `REDIS_SSL_ENABLED` | Bật/tắt SSL/TLS khi kết nối Redis cloud (`true`/`false`) |
| `JWT_SECRET` | Secret ký JWT, nên dài và ngẫu nhiên |
| `JWT_EXPIRATION_MS` | Thời gian sống access token |
| `JWT_REFRESH_EXPIRATION_MS` | Thời gian sống refresh token |
| `BREVO_API_KEY` | API key Brevo gửi email qua HTTPS REST API (dùng cho reset mật khẩu & Admin 2FA OTP) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Cấu hình SMTP fallback khi không đặt `BREVO_API_KEY` |
| `SMS_OTP_MESSAGE_TEMPLATE` | Mẫu tin nhắn OTP SMS |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Upload ảnh |
| `APP_BASE_URL` | Public backend URL |
| `APP_FRONTEND_BASE_URL` | Public frontend URL |
| `APP_CORS_ALLOWED_ORIGINS` | Origin được phép, cách nhau bằng dấu phẩy |
| `APP_TRUST_PROXY_HEADERS` | `true` khi backend chạy sau Render/Nginx/Cloudflare để rate limit dùng IP thật |
| `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` | Thanh toán PayOS |
| `GEMINI_ENABLED` | Bật/tắt Gemini (`false` mặc định ở `dev`, `true` ở `prod`) |
| `GEMINI_API_KEY`, `GEMINI_API_URL` | Gemini chat assistant |
| `GEMINI_CONNECT_TIMEOUT_SECONDS`, `GEMINI_READ_TIMEOUT_SECONDS` | Timeout Gemini |
| `GEMINI_TEMPERATURE`, `GEMINI_TOP_P`, `GEMINI_MAX_OUTPUT_TOKENS` | Tham số sinh câu trả lời Gemini |

## Chạy local

Từ thư mục `backend/`:

```bash
mvn spring-boot:run
```

Chạy với profile `dev` (bật `app.security.enable-default-admin=true`). `DataInitializer` sẽ kiểm tra và seed riêng từng tài khoản mẫu qua `seedUserIfNotExists` cho đủ 5 vai trò nếu chưa tồn tại:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Tài khoản seed mặc định cho môi trường `dev`:

```text
- ADMIN:   admin@gmail.com   / admin123   (Yêu cầu mã 2FA OTP từ log/Redis/email)
- MANAGER: manager@gmail.com / manager123 (Quản trị thực đơn, nhân sự, kho)
- CASHIER: cashier@gmail.com / cashier123 (Sơ đồ bàn, thanh toán, lịch sử hóa đơn)
- STAFF:   staff@gmail.com   / staff123   (Sơ đồ bàn, gọi món, gộp bàn, takeaway)
- CHEF:    chef@gmail.com    / chef123    (KDS Bếp realtime, Kanban, nấu gom món, kho)
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

Hiện có **22 migrations** theo thứ tự phiên bản từ `V1__Initial.sql` đến `V1.21__Settlement_Validation_And_Cash_Tendered.sql`:

```text
V1__Initial.sql
V1.1__FK_Indexes.sql
V1.2__Table_Sessions.sql
V1.3__Payment_Indexes.sql
V1.4__Reporting_Summaries.sql
V1.5__Reporting_Facts.sql
V1.6__Durable_Order_Idempotency.sql
V1.7__Incremental_Order_Activity_Summary.sql
V1.8__User_Indexes.sql
V1.9__Menu_Production_Hardening.sql
V1.10__Table_Production_Hardening.sql
V1.11__Settings_Production_Hardening.sql
V1.12__Operational_Bill_Notification_Settings.sql
V1.13__Password_Token_Invalidation.sql
V1.14__Payment_Gateway_Order_Code.sql
V1.15__Merged_Table_Sessions.sql
V1.16__Soft_Delete_Cascade_Table_Session_Memberships.sql
V1.17__Inventory_Optimistic_Locking.sql
V1.18__Payment_Voucher_Usage_Claim.sql
V1.19__User_Must_Change_Password.sql
V1.20__Add_Admin_And_Cashier_Roles.sql
V1.21__Settlement_Validation_And_Cash_Tendered.sql
```

Cấu hình hiện tại:

- `spring.jpa.hibernate.ddl-auto: validate`
- `spring.flyway.baseline-on-migrate: false`
- JDBC/Hibernate timezone: `Asia/Ho_Chi_Minh`

Vì Hibernate chỉ validate schema và Flyway không baseline tự động, hãy tạo database PostgreSQL trống trước khi chạy app. Flyway sẽ áp toàn bộ migration khi app khởi động.

## Auth và phân quyền

Backend dùng access token JWT trong header:

```http
Authorization: Bearer <access-token>
```

Refresh token được lưu trong HTTP-only cookie với path `/api/v1`. Với tài khoản `ADMIN`, luồng đăng nhập yêu cầu xác thực 2FA OTP qua `SmtpEmailService` (Brevo REST API khi có `BREVO_API_KEY`, fallback SMTP; đồng thời ghi `[2FA OTP]` vào log và lưu ticket tại Redis `qros:2fa:ticket:*`).

### 5 vai trò nhân sự & phân cấp quyền (`RoleHierarchy`)

Phân cấp quyền trong Spring Security:

```text
ROLE_ADMIN > ROLE_MANAGER > ROLE_CASHIER > ROLE_STAFF
ROLE_MANAGER > ROLE_CHEF
```

| Role | Quyền chính |
| --- | --- |
| `ADMIN` | Toàn quyền hệ thống (kế thừa `MANAGER`), xác thực 2FA OTP; độc quyền cập nhật cấu hình hệ thống (`PUT /api/v1/settings`, `POST /api/v1/settings/logo`), xóa đơn/kho, xem báo cáo doanh thu (`GET /api/v1/analytics/revenue`), dự báo (`GET /api/v1/analytics/forecast/**`) và metrics `GET /actuator/prometheus` |
| `MANAGER` | Quản trị thực đơn, danh mục, combo, voucher, nhân sự, bàn, kho, bếp, thanh toán, lịch sử hóa đơn, thống kê món bán chạy/nhân sự và sinh mô tả món AI |
| `CASHIER` | Vận hành sơ đồ bàn, thanh toán (`/api/v1/payments/**`), tra cứu lịch sử & tổng hợp hóa đơn (`/api/v1/analytics/orders/history`, `/summary`), kế thừa quyền `STAFF` |
| `STAFF` | Vận hành sơ đồ bàn, mở bàn, gọi món tại bàn, gộp/tách bàn, quản lý đơn mang về, đọc dữ liệu phục vụ vận hành và hỏi trợ lý AI nhân viên |
| `CHEF` | Vận hành màn hình Bếp KDS (`/api/v1/kitchen`), cập nhật trạng thái món, theo dõi kho nguyên liệu và hỏi trợ lý AI nhân viên |

### Nhóm route theo `SecurityRoutes`

- **Public:** `/api/v1/auth/login`, `/api/v1/auth/verify-2fa`, `/api/v1/auth/resend-2fa`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, `/api/v1/auth/forgot-password-*`, `/api/v1/auth/reset-password-*`, `/api/v1/vouchers/validate`, `/api/v1/recommendations/**`, `/api/v1/public/**` (bao gồm `/api/v1/public/ai/chat`, `/api/v1/public/settings`), `/api/v1/webhooks/**`, `/ws/**`, `/actuator/health`.
- **Self profile:** `/api/v1/users/me`, `/api/v1/users/me/password`, `/api/v1/users/me/avatar`.
- **Admin-only:** `PUT /api/v1/settings`, `POST /api/v1/settings/logo`, `DELETE /api/v1/orders/**`, `DELETE /api/v1/inventory/**`, `GET /api/v1/analytics/revenue`, `GET /api/v1/analytics/forecast/**`, `GET /actuator/prometheus`.
- **Manager-only write (kế thừa bởi `ADMIN`):** quản trị user, menu, category, combo, inventory, table, voucher, `POST /api/v1/ai/menu-item/description`.
- **Payment & Cashier analytics (`ADMIN`, `MANAGER`, `CASHIER`):** `/api/v1/payments/**` (không mở cho `STAFF`/`CHEF`), `/api/v1/analytics/orders/history`, `/api/v1/analytics/orders/summary`.
- **Operation:** orders, tables, table-sessions, kitchen, inventory, `POST /api/v1/ai/staff/query`.

## API map

| Nhóm | Endpoint chính | Mô tả |
| --- | --- | --- |
| Auth | `/api/v1/auth/*` | Login, 2FA (`verify-2fa`, `resend-2fa`), refresh, logout, forgot/reset mật khẩu |
| Public customer | `/api/v1/public/*` | Menu public, table by code, table session, current order, order preview/create |
| Public settings | `/api/v1/public/settings` | Lấy cấu hình công khai cho trang khách hàng |
| AI (Customer) | `POST /api/v1/public/ai/chat` | Trợ lý AI Gemini công khai tư vấn món ăn cho khách hàng |
| AI (Manager/Admin) | `POST /api/v1/ai/menu-item/description` | Sinh mô tả món ăn bằng AI (`MANAGER`/`ADMIN`) |
| AI (Staff) | `POST /api/v1/ai/staff/query` | Trợ lý AI tra cứu vận hành cho nhân sự (`STAFF`/`CHEF`/`CASHIER`/`MANAGER`/`ADMIN`) |
| Categories | `/api/v1/categories` | CRUD danh mục, search, upload ảnh |
| Menu items | `/api/v1/menu-items` | CRUD món, lọc theo danh mục, upload ảnh |
| Combos | `/api/v1/combos` | CRUD combo, active combos, toggle active |
| Inventory | `/api/v1/inventory/*` | Nguyên liệu, công thức, nhập/xuất kho, reservation |
| Tables | `/api/v1/tables` | CRUD bàn, lấy theo code, regenerate QR |
| Table sessions | `/api/v1/table-sessions` | Quản lý phiên bàn, gộp bàn / tách bàn |
| Orders | `/api/v1/orders`, `/api/v1/orders/items` | Tạo đơn, active, current order, preview, reconcile, thao tác item trong đơn |
| Kitchen | `/api/v1/kitchen` | Danh sách món bếp, update item status/prepared |
| Payments | `/api/v1/payments` | Thanh toán tiền mặt/PayOS (`ADMIN`/`MANAGER`/`CASHIER`), hủy link, đồng bộ trạng thái |
| Webhooks | `/api/v1/webhooks/payos` | PayOS callback |
| Vouchers & Promotions | `/api/v1/vouchers`, `/api/v1/promotions` | CRUD voucher/khuyến mãi, validate code công khai (`/api/v1/vouchers/validate`) |
| Analytics & Forecast | `/api/v1/analytics`, `/api/v1/analytics/forecast` | Doanh thu (`ADMIN`), dự báo (`ADMIN`), lịch sử/tổng hợp đơn (`ADMIN`/`MANAGER`/`CASHIER`), nhân viên, top món, dashboard |
| Settings | `/api/v1/settings` | Xem cấu hình; cập nhật cấu hình & logo (`ADMIN`) |
| Users | `/api/v1/users` | Hồ sơ cá nhân, avatar, quản trị nhân viên, reset password |
| Recommendations | `/api/v1/recommendations` | API recommendation public/admin |

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
| `/topic/kitchen` | Cập nhật bảng bếp KDS | `ROLE_MANAGER`, `ROLE_CHEF` (cùng `ROLE_ADMIN` qua hierarchy; không mở cho `STAFF`/`CASHIER`) |
| `/topic/orders` | Cập nhật đơn hàng nội bộ cho quản trị/vận hành | `ROLE_MANAGER`, `ROLE_STAFF`, `ROLE_CHEF` (cùng `ROLE_ADMIN`, `ROLE_CASHIER` qua hierarchy) |
| `/topic/users` | Cập nhật nhân viên | `ROLE_MANAGER` (cùng `ROLE_ADMIN` qua hierarchy) |
| `/topic/vouchers` | Cập nhật voucher | `ROLE_MANAGER`, `ROLE_STAFF` |
| `/topic/promotions` | Cập nhật chương trình khuyến mãi | `ROLE_MANAGER`, `ROLE_STAFF` |
| `/topic/inventory` | Cập nhật tồn kho nguyên liệu | `ROLE_MANAGER`, `ROLE_STAFF`, `ROLE_CHEF` |
| `/topic/menu` | Cập nhật món ăn | Public subscribe |
| `/topic/categories` | Cập nhật danh mục | Public subscribe |
| `/topic/combos` | Cập nhật combo | Public subscribe |
| `/topic/settings` | Cập nhật settings public | Public subscribe |

Client gửi header STOMP khi subscribe protected topics:

```text
Authorization: Bearer <access-token>
```

Các protected topic sẽ từ chối subscribe nếu thiếu token hoặc role không phù hợp.

## Thanh toán

Payment flow chính:

- `POST /api/v1/payments`: tạo thanh toán với `paymentMethod` là `PAYOS` hoặc `CASH` (chỉ `ADMIN`, `MANAGER`, `CASHIER`).
- `GET /api/v1/payments/{transactionId}`: đồng bộ trạng thái giao dịch online.
- `POST /api/v1/payments/{transactionId}/cancel`: hủy link thanh toán online.
- `POST /api/v1/webhooks/payos`: nhận webhook PayOS.
- `PaymentCleanupService`: chạy mỗi 5 phút để xử lý giao dịch `PENDING` bị treo.

Thanh toán tiền mặt dùng cùng endpoint:

```json
POST /api/v1/payments
{
  "orderId": 123,
  "paymentMethod": "CASH",
  "voucherCode": null
}
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

Spring Cache mặc định dùng **Caffeine** (`SPRING_CACHE_TYPE=caffeine`) ở môi trường `dev` và chuyển sang **Redis** ở môi trường `prod` (`application-prod.yml`), phục vụ cache cho:

- Tables/QR codes
- Menu, categories, combos
- Recommendations/popular items
- Vouchers
- Settings
- Order detail/stats
- Revenue/top dishes/employee performance/dashboard stats

Metrics:

- Health: `GET /actuator/health` (Public)
- Prometheus: `GET /actuator/prometheus` (chỉ `ADMIN`)

Redis failure behavior:

- Public API rate-limit dùng fail-open: nếu Redis lỗi, request được đi tiếp và backend ghi warning.
- AI quota dùng fail-closed: nếu Redis lỗi, Gemini bị chặn tạm thời để tránh rủi ro chi phí.
- Refresh token store dùng fail-closed: login/refresh không cấp token mới nếu Redis không lưu/đọc được token.
- Spring cache lỗi sẽ fallback về dữ liệu nguồn thay vì làm request fail.

## Production operations

Trước khi public production:

- Kích hoạt profile `prod` (`SPRING_PROFILES_ACTIVE=prod`) để dùng Redis cache và cấu hình bảo mật production.
- Bật `APP_TRUST_PROXY_HEADERS=true` khi deploy sau Render/Nginx/Cloudflare.
- Đặt `APP_CORS_ALLOWED_ORIGINS` là origin chính xác, không dùng wildcard.
- Đặt `JWT_SECRET` dài, ngẫu nhiên và riêng cho production.
- Cấu hình `BREVO_API_KEY` (hoặc SMTP fallback) cho email reset mật khẩu và Admin 2FA OTP.
- Cấu hình `GEMINI_API_KEY` khi bật `GEMINI_ENABLED=true`.
- Theo dõi `/actuator/health`, `/actuator/prometheus`, Redis, DB connection pool và webhook/payment failures.

Backup PostgreSQL:

```bash
scripts/backup-postgres.sh
```

Restore vào môi trường đã chọn trong `.env`:

```bash
CONFIRM_RESTORE=YES scripts/restore-postgres.sh backups/qros-YYYYMMDD-HHMMSS.dump
```

## Test, Format và Build

Định dạng mã nguồn bằng Spotless (`spotless:check` tự chạy ở phase `validate` của Maven):

```bash
mvn spotless:apply
mvn spotless:check
```

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

Build GraalVM native image (với profile `native`):

```bash
mvn -Pnative native:compile
```

Bộ kiểm thử gồm **44 file test** tại `backend/src/test/java/com/qros/`, bao phủ kiến trúc, bảo mật, AI, đơn hàng, thanh toán và gộp bàn — tiêu biểu:

- `BackendDependencyRulesTest` (ArchUnit kiểm soát ranh giới layer)
- `BackendMessageLanguageTest` (đảm bảo thông báo backend chuẩn tiếng Anh)
- `SecurityConfigRegressionTest`, `Admin2faServiceTest`, `WebSocketConfigTest`, `TimeZoneConfigTest`
- `GeminiChatGatewayTest`, `AiQuotaServiceTest`
- `TableMergeServiceTest`, `TableSessionServiceTest`, `IdempotencyServiceTest`
- `DiscountServiceTest`, `OrderCreationServiceTest`, `OrderPricingServiceTest`, `OrderServiceImplTest`, `OrderStatusServiceTest`
- `PayosServiceImplTest`, `RecommendationServiceTest`

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
