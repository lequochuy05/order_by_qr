# Tài Liệu Thiết Kế Hệ Thống (System Design Document) — QROS

> **Tầng Knowledge:** `04-tech` (System Design & Technical Architecture · Skill: `.agents/skills/se-system-design`)  
> **Dự án:** QROS — Smart QR Order & Restaurant Operating System  
> **Kế thừa bắt buộc từ:** [KNOWLEDGE_HUB.md](../KNOWLEDGE_HUB.md) · [01-po/blueprint.md](../01-po/blueprint.md) · [02-design/PROTOTYPE_SCREEN_MAP.md](../02-design/PROTOTYPE_SCREEN_MAP.md) · [03-ba/PRD_BY_USER_STAGE.md](../03-ba/PRD_BY_USER_STAGE.md) · [03-ba/USER_STORIES.md](../03-ba/USER_STORIES.md)  
> **Kiến trúc chủ đạo:** Modular Monolith (Spring Boot 4.1.2 / Java 25) + Feature-Sliced Design SPA (React 19.2 / Vite 7 / Tailwind CSS v4) + Android Packaging (Capacitor 8.6)  
> **Cơ sở dữ liệu:** PostgreSQL (37 bảng · 28 Flyway migrations `V1`–`V1.27`) · Cache: Caffeine (Dev) / Redis (Prod)  
> **Bàn giao cho (`05-specs`):** [CONSTITUTION.md](../05-specs/CONSTITUTION.md) · `.agents/skills/sdd-coding-sync`  
> **Phạm vi:** Đặc tả kiến trúc kỹ thuật chuyển tiếp trực tiếp từ Yêu cầu nghiệp vụ (`STG-01` → `STG-06`, `US-001` → `US-036`, `BR-01` → `BR-38`) sang giai đoạn Lập trình (Code).

---

## 1. Tổng quan, Yêu cầu Kiến trúc & Ánh xạ Truy vết từ `01-po` / `02-design` / `03-ba`

### 1.1 Mục tiêu hệ thống
QROS số hóa toàn trình vận hành nhà hàng theo thời gian thực cho **6 nhóm người dùng** theo đúng thứ tự `01-po/personas.md`: `PER-01` (`CUSTOMER`), `PER-02` (`STAFF`), `PER-03` (`CHEF`), `PER-04` (`CASHIER`), `PER-05` (`MANAGER`), `PER-06` (`ADMIN`) trên đa thiết bị (**Mobile Web/PWA**, **Android APK**, **Tablet POS**, **Màn hình Bếp KDS**, **Desktop Admin**).

### 1.2 Yêu cầu phi chức năng (Non-Functional Requirements)

| Nhóm yêu cầu | Mục tiêu thiết kế (từ `01-po` & `03-ba`) | Giải pháp triển khai kỹ thuật trong QROS |
|---|---|---|
| **Realtime & Độ trễ thấp** | Đồng bộ trạng thái bàn, yêu cầu phục vụ, đơn hàng, bếp < 1 giây (`BR-27`) | WebSocket STOMP over SockJS với 12 topic phân quyền |
| **Toàn vẹn dữ liệu & Đồng thời** | Không tạo đơn trùng (`BR-06`), chống spam gọi nhân viên (`BR-26`), chống nhận trùng (`BR-28`), không lệch kho (`BR-11`), không lỗi gộp bàn (`BR-14`) | `idempotency_requests` (Durable Idempotency), Optimistic Locking (`V1.17`), `order_item_inventory_reservations`, Pessimistic Write Lock (`SELECT FOR UPDATE`) + `@Version` cho nhận xử lý yêu cầu bàn, phiên bàn & thanh toán, Partial Unique Index + Rate Limit + Cooldown 60s cho `table_service_requests` (`V1.23`) |
| **Bảo mật đa tầng** | Bảo vệ phiên quản trị (`BR-02`, `BR-03`, `BR-04`), QR bàn (`BR-01`) & thanh toán (`BR-18`, `BR-19`) | JWT + HttpOnly Refresh Cookie (`/api/v1`), Admin 2FA OTP, Account Lockout, Rate Limiting, QR HMAC/Session Token, `RoleHierarchy` |
| **Hiệu năng truy vấn** | Báo cáo doanh thu & thực đơn tải nhanh giờ cao điểm (`BR-23`) | Pre-aggregated Reporting (`daily_*_summary`, `order_reporting_fact`), Caffeine/Redis Cache, FK/Composite Indexes |
| **Đa nền tảng & Khả năng mở rộng** | Dễ mở rộng module, song ngữ `VI/EN` & Light/Dark (`BR-24`) | Backend Modular Monolith kiểm soát bằng ArchUnit; Frontend FSD, Design Tokens 2 tầng, PWA, Capacitor 8.6, i18n |

### 1.3 Ánh xạ Yêu cầu (`03-ba` & `02-design`) → Thành phần Kỹ thuật (`04-tech`)

| User Stage | User Stories & Business Rules (`03-ba`) | Màn hình (`02-design`) | Backend Modules (`com.qros.modules.*`) | Frontend Features (`frontend/src/features/*`) | Bảng SQL & Kênh Realtime cốt lõi |
|---|---|---|---|---|---|
| **`STG-01`** | `US-001`–`US-004` · `BR-01`–`BR-04` | `SCR-01..04`, `SCR-06..09`, `SCR-26` | `auth`, `user`, `table`, `publicapi` | `auth`, `pages/landing`, `pages/system` | `users`, `password_reset_tokens`, `table_session_tokens`, Redis (`qros:2fa:ticket:*`) |
| **`STG-02`** | `US-005`–`US-008`, `US-025`, `US-028` · `BR-05`–`BR-08`, `BR-25`–`BR-26`, `BR-29`–`BR-30` | `SCR-05` | `menu`, `order`, `table`, `ai`, `recommendation`, `publicapi` | `customer-menu`, `ai-assistant` | `category`, `menu_item`, `combos`, `orders`, `order_batches`, `idempotency_requests`, `table_service_requests`, WS `/topic/tables`, `/topic/service-requests` |
| **`STG-03`** | `US-009`–`US-012`, `US-030`, `US-032`–`US-034` · `BR-09`–`BR-12`, `BR-32`, `BR-34`–`BR-36` | `SCR-12`, `SCR-18` | `kitchen`, `inventory`, `ai`, `notification` | `kitchen-board`, `inventory-management` | `order_item`, `order_item_status_history`, `inventory_items` (`unit_cost` `V1.24`), `recipe_items`, `order_item_inventory_reservations`, `stock_movements`, WS `/topic/kitchen`, `/topic/inventory` |
| **`STG-04`** | `US-013`–`US-015`, `US-026`–`US-027`, `US-032` · `BR-13`–`BR-15`, `BR-27`–`BR-28`, `BR-34` | `SCR-11`, `SCR-13`, `SCR-27` | `table`, `order`, `ai`, `notification` | `table-management`, `takeaway-management`, `service-requests` | `tables`, `table_sessions`, `table_session_tables`, `table_service_requests`, `orders`, WS `/topic/tables`, `/topic/orders`, `/topic/service-requests` |
| **`STG-05`** | `US-016`–`US-019` · `BR-16`–`BR-19` | `SCR-05`, `SCR-11`, `SCR-13`, `SCR-14` | `payment`, `promotion`, `table`, `order`, `analytics` | `table-management`, `takeaway-management`, `order-management`, `customer-menu` | `payment_transactions`, `vouchers`, `promotions`, `order_discounts`, `order_status_history`, WS `/topic/tables`, `/topic/orders` |
| **`STG-06`** | `US-020`–`US-024`, `US-029`–`US-031`, `US-033`–`US-036` · `BR-20`–`BR-24`, `BR-31`–`BR-33`, `BR-35`–`BR-38` | `SCR-10`, `SCR-15..26`, `SCR-28` | `menu`, `inventory`, `promotion`, `user`, `analytics`, `ai`, `settings`, `subscription` | `dashboard-overview`, `menu-management`, `category-management`, `combo-management`, `voucher-management`, `promotion-management`, `inventory-management`, `user-management`, `profile-management`, `analytics`, `settings-management`, `subscription-billing` | `system_settings`, `restaurant_subscriptions`, `subscription_invoices` (`V1.27`), `order_reporting_fact`, `daily_*_summary`, `inventory_items` (`unit_cost`), `recipe_items`, `combos`, `promotions`, WS `/topic/menu`, `/topic/combos`, `/topic/promotions`, `/topic/settings` |

---

## 2. Kiến trúc tổng thể (High-Level Architecture)

```mermaid
flowchart TB
    subgraph Clients["Tầng Client (Đa nền tảng)"]
        C1["Khách hàng<br/>Mobile Web / PWA (QR Menu)"]
        C2["Phục vụ & Thu ngân<br/>Tablet POS / Android APK"]
        C3["Bếp (KDS)<br/>Tablet / Màn hình Bếp"]
        C4["Quản lý & Admin<br/>Desktop Dashboard"]
    end

    subgraph Edge["Edge / Reverse Proxy"]
        V["Vercel Edge / Nginx<br/>SPA Routing · CSP Headers<br/>Proxy /api/v1 & /ws"]
    end

    subgraph Backend["QROS Backend — Spring Boot 4.1.2 / Java 25 (Modular Monolith)"]
        SEC["Security & Auth Layer<br/>JWT · Refresh Cookie · 2FA · RBAC · Rate Limit"]
        MOD["16 Domain Modules<br/>auth · user · table · menu · order · kitchen<br/>payment · inventory · promotion · analytics<br/>ai · recommendation · notification · settings · subscription · publicapi"]
        WS["STOMP WebSocket Broker<br/>12 Protected & Public Topics"]
        INFRA["3 Infrastructure Adapters<br/>mail · sms · storage"]
    end

    subgraph Data["Tầng Lưu trữ & Cache"]
        PG[("PostgreSQL (Neon)<br/>35 Tables · Flyway V1–V1.26")]
        RD[("Redis (Prod) / Caffeine (Dev)<br/>Cache · 2FA Tickets · Blacklist · Rate Limit")]
    end

    subgraph External["Dịch vụ bên thứ ba (External Integrations)"]
        PAY["PayOS (VietQR)<br/>Payment Link & Webhook"]
        AI["Google Gemini Flash<br/>gemini-3.5-flash-lite / 3.8-flash"]
        MAIL["Brevo REST API (HTTPS)<br/>+ SMTP Fallback"]
        CLD["Cloudinary<br/>Media & Dish Images"]
    end

    C1 & C2 & C3 & C4 -->|"HTTPS / WSS"| V
    V -->|"REST /api/v1"| SEC
    V -->|"STOMP /ws"| WS
    SEC --> MOD
    MOD --> WS
    MOD --> INFRA
    MOD --> PG
    INFRA --> RD
    MOD --> PAY
    PAY -.->|"Signed Webhook"| MOD
    MOD --> AI
    INFRA --> MAIL
    INFRA --> CLD
```

---

## 3. Kiến trúc Backend (Modular Monolith)

Backend được tổ chức dạng **Maven single-module** theo kiến trúc **Modular Monolith** tại `backend/src/main/java/com/qros/`, giúp triển khai đơn giản như Monolith nhưng tách biệt ranh giới nghiệp vụ chặt chẽ như Microservices.

### 3.1 Phân rã tầng & Module

| Tầng | Package | Vai trò |
|---|---|---|
| **Core** | `com.qros.core` | Cấu hình khởi động & bảo mật hệ thống: `AsyncConfig` (bounded thread pool), `AuthConfig`, `CaffeineCacheConfig`, `RedisConfig`, `WebSocketConfig`, `DataInitializer`, `SecurityConfig`, `SecurityRoutes`, `CorsConfig`, `RuntimeEndpointValidator` |
| **Domain Modules (15)** | `com.qros.modules.*` | `ai`, `analytics`, `auth`, `inventory`, `kitchen`, `menu`, `notification`, `order`, `payment`, `promotion`, `publicapi`, `recommendation`, `settings`, `table`, `user` *(giao tiếp chéo hoàn toàn qua Port/Lookup Service như `MenuCatalogOrderUsagePort`, `VoucherUsageCheckPort` — 0 phụ thuộc vòng & 0 gọi chéo Repository)* |
| **Infrastructure (3)** | `com.qros.infrastructure.*` | Adapter kết nối ngoài độc lập nghiệp vụ: `mail` (`EmailService` / `SmtpEmailService` — Brevo REST API HTTPS + SMTP fallback timeout 10s), `sms` (`SmsService` / `TwilioSmsService` / `SmsProperties`), `storage` (`StorageService` / `CloudinaryStorageService` kèm connect/read timeout) |
| **Shared** | `com.qros.shared` | Thành phần nền tảng thấp nhất không phụ thuộc `core`/`infrastructure`/`modules`: `config` (`AppProperties`), `constants` (`ApiRoutes`), `entity` (`BaseEntity`), `enums`, `event`, `exception` (`GlobalExceptionHandler`, `ApiErrorController`), `idempotency`, `observability`, `ratelimit` (`ClientAddressResolver`, `RedisRateLimiter` + Caffeine bounded fallback), `response`, `security` (`JwtAuthFilter`, `CorsOrigins`, `CookieRequestOriginValidator`, `TokenBlacklistService` + Caffeine bounded fallback), `time` (`AppTime`), `transaction`, `validation` (`@MaxUtf8Bytes`) |

### 3.2 Kiểm soát ranh giới kiến trúc tự động (Architecture Fitness Functions)
- **`BackendDependencyRulesTest` (ArchUnit 1.5.1 — 11 rules):** Tự động ép buộc trong mọi lần chạy `mvn verify`: (1) cô lập phân tầng `shared` / `infrastructure` / `modules` / `core`, (2) **0 vòng phụ thuộc chéo (`beFreeOfCycles`)** giữa 15 domain modules, (3) **0 lớp nào trong module được import trực tiếp Repository của module khác**, (4) **0 vòng phụ thuộc constructor giữa các Spring bean**, và (5) tách biệt các biên miền cốt lõi (`menu`, `table`, `kitchen`, `payment`, `inventory`, `order`).
- **`HibernateEntityMetadataTest`:** Khởi dựng offline Hibernate 7.4.13 `MetadataSources` cho toàn bộ các thực thể JPA để xác minh `@NotFound(action = NotFoundAction.IGNORE)` và `@SQLDelete` 2 tham số (`id`, `version`) hợp lệ.
- **`BackendMessageLanguageTest`:** Đảm bảo toàn bộ thông điệp lỗi/phản hồi từ backend tuân thủ quy chuẩn tiếng Anh nhất quán để frontend dịch song ngữ qua i18n.
- **Spotless (`spotless-maven-plugin` 3.11.0):** Tự động kiểm tra định dạng mã nguồn (Palantir Java Format) ở pha `validate`.

---

## 4. Kiến trúc Frontend (Feature-Sliced Design)

Frontend (`frontend/src/`) sử dụng **React 19.2 + Vite 7 + Tailwind CSS v4** theo kiến trúc **Feature-Sliced Design (FSD)** được kiểm soát tự động bằng ESLint (`no-restricted-imports` theo layer, cross-slice và Public API `index.js`), đóng gói thành **PWA** (`vite-plugin-pwa`) và **Android APK** (`@capacitor/android ^8.6.2`).

```mermaid
flowchart TB
    APP["app/<br/>router.jsx (28 named routes + *) · Providers · WebSocketInvalidator · styles/index.css"]
    PAGES["pages/<br/>landing · auth · customer · admin · system"]
    WIDGETS["widgets/<br/>admin-layout (Sidebar · Topbar · Footer) · order-modals (AddItem · OrderDetail · Payment)"]
    FEATURES["features/ (19 slices)<br/>ai-assistant · analytics · auth · category-management · combo-management · customer-menu<br/>dashboard-overview · inventory-management · kitchen-board · menu-management · order-management<br/>profile-management · promotion-management · service-requests · settings-management<br/>table-management · takeaway-management · user-management · voucher-management"]
    ENTITIES["entities/ (16 slices)<br/>ai · analytics · category · combo · inventory · menu-item · order · payment<br/>promotion · public-menu · service-request · session · settings · table · user · voucher"]
    SHARED["shared/<br/>api/httpClient.js · lib/websocket.js · lib/i18n/ · ui/ (Form, Modal, Table, ErrorBoundary)"]

    APP --> PAGES --> WIDGETS --> FEATURES --> ENTITIES --> SHARED
```

### Các quyết định thiết kế Frontend trọng yếu
- **Quản lý State phân tách:**
  - *Server State:* **TanStack React Query** tự động cache, retry và invalidate theo sự kiện WebSocket (`WebSocketInvalidator.jsx`).
  - *Client State:* **Zustand** cho giỏ hàng khách (`src/features/customer-menu/model/cartStore.js`), trạng thái phiên đăng nhập và tùy chọn UI.
- **Dual WebSocket Services:** Tách biệt `publicWsService` (không cần JWT, dùng cho khách hàng tại bàn) và `adminWsService` (đính kèm JWT, dùng cho nhân sự nội bộ).
- **Design System 2 tầng (`Evondev Signature Edition`):** Token tĩnh `@theme` kết hợp biến runtime `:root` / `.dark` trong `index.css`, hỗ trợ `safe-area-inset-*` cho di động và song ngữ `VI`/`EN` qua `useTranslation`.

---

## 5. Thiết kế Cơ sở dữ liệu (Database Design)

Hệ thống sử dụng **PostgreSQL** với **35 bảng** được quản lý nghiêm ngặt qua **27 Flyway migrations** (`V1__Initial.sql` → `V1.26__Wave2_Schema_Constraints_Indexes_And_Promo_Snapshot.sql`, `baseline-on-migrate: false`).

### 5.1 Phân nhóm 35 bảng theo miền nghiệp vụ

| Miền nghiệp vụ | Các bảng chính | Mục đích & cơ chế nổi bật |
|---|---|---|
| **1. Identity & Security** | `users` (`avatar_url VARCHAR(500)`, `full_name VARCHAR(100)` `V1.26`), `password_reset_tokens` (`attempts` `V1.25`), `system_settings` | 5 vai trò nội bộ, bắt buộc đổi mật khẩu lần đầu (`V1.19`), vô hiệu hóa token cũ (`V1.13`), khóa OTP SMS sau 5 lần sai (`V1.25`), căn chỉnh độ dài cột khớp DTO (`V1.26`), cấu hình hệ thống & bật/tắt tự thanh toán (`V1.22`) |
| **2. Catalog & Promotion** | `category`, `menu_item` (`manually_paused` `V1.25`), `item_option`, `item_option_values`, `combos` (`manually_paused` `V1.25`), `combo_items`, `promotions`, `promotion_days`, `vouchers` (`version`, `reserved_count` `V1.25`) | Thực đơn, nhóm tùy chọn/topping, combo (sàn giá `>= 1,000 VND`), cờ tạm ngừng thủ công tách biệt với hết kho, xóa 5 index trùng từ `V1.1` đã bị `V1.9` thay thế (`V1.26`), `CHECK` constraints phòng thủ trên `promotions` (`discount_percent`) và `vouchers` (`used_count`, `reserved_count`, `valid_to > valid_from`) (`V1.26`) |
| **3. Table & Session** | `tables`, `table_sessions`, `table_session_tables`, `table_session_tokens`, `table_service_requests` | Sơ đồ bàn 4 trạng thái (`AVAILABLE`, `OCCUPIED`, `WAITING_FOR_PAYMENT`, `INACTIVE`), phiên phục vụ, **gộp nhiều bàn** (`V1.15`, `V1.16`) chuẩn hóa thứ tự khóa chống deadlock `tables (id ASC) -> table_sessions -> table_session_tables` và tự đóng service requests đang mở của phiên phụ bị hấp thụ, tách tạo QR Cloudinary ra ngoài DB transaction (`DiningTableService`), token QR theo phiên, **hàng đợi gọi nhân viên** (`V1.23`) với khóa chống trùng lý do đang mở (`uq_tsr_active_table_reason`), idempotency (`uq_tsr_session_client_request`), `@Version` & lịch sử giới hạn 100 bản ghi gần nhất giữ nguyên nhãn bàn ghép |
| **4. Order & Kitchen (KDS)** | `orders` (`service_charge_*`, `tax_*` `V1.25`, `applied_promotion_percent NUMERIC(5,2)` + `CHECK [0..100]` `V1.26`), `order_batches`, `order_item`, `order_item_options`, `order_discounts`, `order_status_history`, `order_item_status_history`, `idempotency_requests` | Quản lý đơn hàng nhiều đợt gọi món (`order_batches`), kiểm tra giờ mở cửa (`openingTime`–`closingTime` gồm ca qua đêm) cho đơn QR công khai, snapshot thuế VAT, phí dịch vụ & **tỷ lệ Giờ vàng (`applied_promotion_percent` `V1.26`)**, `@EntityGraph` mở rộng khử N+1 `@NotFound(IGNORE)`, trạng thái bếp đơn hướng (`PENDING → COOKING → FINISHED` không hoàn tác, gộp phát 1 sự kiện khi cập nhật theo mẻ), tái chiếm khóa idempotency hết hạn nguyên tử bằng `ON CONFLICT DO UPDATE` (`IdempotencyRequestRepository`) |
| **5. Inventory & Recipe** | `inventory_items` (`unit_cost` `V1.24`, `CHECK unit_cost >= 0` `V1.26`), `recipe_items`, `order_item_inventory_reservations`, `stock_movements` | Định lượng nguyên liệu theo món, **giá vốn trên mỗi đơn vị nguyên liệu (`unit_cost` `NUMERIC(15,2) >= 0`)** phục vụ Food Cost % & dự toán nhập kho, giữ chỗ tồn kho theo món đang nấu, chống race condition bằng **Optimistic Locking** (`V1.17`), khóa đổi `unit` khi còn BOM/reservation & chặn nhập/điều chỉnh khi `active = false`, chặn xóa khi `reserved_quantity > 0`, sổ cái xuất/nhập kho |
| **6. Payment & Analytics** | `payment_transactions` (`voucher_usage_reserved`, `voucher_code_snapshot` `V1.25`), `order_reporting_fact` (`idx_order_reporting_fact_created_at_desc` `V1.26` + 4 cột thuế/phí + trigger `sync_order_reporting_fact()` sửa `OLD.id` `V1.25`), `daily_revenue_summary`, `daily_item_sales_summary`, `daily_staff_sales_summary`, `daily_order_activity_summary` | Giao dịch tiền mặt (validate `cashReceived >= finalAmount` & server tự tính `changeAmount`), PayOS VietQR (tự hết hạn giao dịch quá hạn trước khi kiểm tra voucher & hủy/thay thế QR cũ đang chờ) & chốt đơn 0đ nội bộ (`V1.14`, `V1.18`, `V1.21`, `V1.25`), bảng sự kiện & tổng hợp báo cáo theo ngày (`DailySummaryRepository` bổ sung fallback thumbnail `COMBO` lateral join) tăng tốc thống kê (`V1.4`, `V1.5`, `V1.7`, `V1.25`, `V1.26`) |

### 5.2 Sơ đồ quan hệ thực thể cốt lõi (Core ERD)

```mermaid
erDiagram
    TABLES ||--o{ TABLE_SESSION_TABLES : "thuộc"
    TABLE_SESSIONS ||--o{ TABLE_SESSION_TABLES : "gộp bàn"
    TABLE_SESSIONS ||--o{ TABLE_SESSION_TOKENS : "cấp token QR"
    TABLE_SESSIONS ||--o{ ORDERS : "chứa"
    TABLES ||--o{ TABLE_SERVICE_REQUESTS : "gửi yêu cầu"
    TABLE_SESSIONS ||--o{ TABLE_SERVICE_REQUESTS : "thuộc phiên"
    USERS ||--o{ TABLE_SERVICE_REQUESTS : "nhận/hoàn tất"
    USERS ||--o{ ORDERS : "phục vụ / tạo"

    ORDERS ||--o{ ORDER_BATCHES : "đợt gọi"
    ORDER_BATCHES ||--o{ ORDER_ITEM : "gồm món"
    MENU_ITEM ||--o{ ORDER_ITEM : "được đặt"
    CATEGORY ||--o{ MENU_ITEM : "phân loại"
    MENU_ITEM ||--o{ ITEM_OPTION : "tùy chọn"
    ITEM_OPTION ||--o{ ITEM_OPTION_VALUES : "giá trị"
    ORDER_ITEM ||--o{ ORDER_ITEM_OPTIONS : "chọn topping"

    MENU_ITEM ||--o{ RECIPE_ITEMS : "định lượng"
    INVENTORY_ITEMS ||--o{ RECIPE_ITEMS : "nguyên liệu"
    ORDER_ITEM ||--o{ ORDER_ITEM_INVENTORY_RESERVATIONS : "giữ kho"
    INVENTORY_ITEMS ||--o{ STOCK_MOVEMENTS : "biến động"

    ORDERS ||--o{ PAYMENT_TRANSACTIONS : "thanh toán"
    ORDERS ||--o{ ORDER_DISCOUNTS : "áp dụng"
    VOUCHERS ||--o{ ORDER_DISCOUNTS : "mã giảm giá"
    ORDERS ||--o{ ORDER_STATUS_HISTORY : "lịch sử đơn"
```

---

## 6. Thiết kế Giao tiếp API, Realtime & Luồng nghiệp vụ chính

### 6.1 Thiết kế REST API (`/api/v1`)
- **Public API (không cần JWT):** `/api/v1/public/orders`, `/api/v1/public/tables/{tableCode}/service-requests` (`GET`, `POST`, `POST /{id}/cancel` — xác thực `sessionToken` + rate limit), `/api/v1/public/settings`, `/api/v1/public/ai/chat` (trả `AiChatResponse(reply, suggestions[])` — `MenuContextProvider` lọc bằng `findAllPublicAvailableItems` + `findAllActiveWithItems`, đối chiếu ID/giá/tùy chọn sống từ DB trước khi trả về), `/api/v1/vouchers/validate`, `/api/v1/recommendations/**`, `/api/v1/webhooks/payos`, `/api/v1/auth/**` (login, refresh, verify/resend 2FA, forgot/reset password).
- **Protected API (JWT + RBAC):** Chia theo tài nguyên chuẩn RESTful (`/tables`, `/table-sessions` gồm `POST /{sessionId}/close-empty` cho `MANAGER`/`STAFF`, `/table-service-requests` (`GET`, `PATCH /{id}/claim`, `PATCH /{id}/resolve`, `PATCH /{id}/cancel` cho `STAFF`/`CASHIER`/`MANAGER`/`ADMIN`, chặn `CHEF`), `/menu`, `/categories`, `/combos`, `/orders` (`DELETE /orders/{id}` chỉ `ADMIN`), `/orders/items` (`DELETE /orders/items/{id}` cho `MANAGER`/`STAFF`/`CHEF`), `/kitchen`, `/payments`, `/inventory/items` (`GET` chỉ `MANAGER`/`CHEF`/`ADMIN`, `DELETE` chỉ `ADMIN`, `POST /batch-stock-in` trong `INVENTORY_OPERATION_POST` + `@PreAuthorize("hasAnyRole('MANAGER','CHEF')")` khóa `findAllByIdInForUpdate` theo thứ tự ID tăng dần, chặn `updateUnitCost=true` ở tầng server với `CHEF`), `/promotions`, `/vouchers`, `/users`, `/analytics` & `/forecast` (gồm `/analytics/revenue` và `/forecast/**` mở cho `MANAGER`/`ADMIN`), `/settings` (`PUT` chỉ `ADMIN`), `/subscription` (`GET /current`, `GET /plans` cho mọi nhân sự đăng nhập; `GET /invoices` cho `MANAGER`/`ADMIN`; `POST /trial`, `POST /checkout` có `Idempotency-Key`, `POST /invoices/{id}/sync`, `POST /invoices/{id}/cancel` chỉ `ADMIN`; webhook `/api/v1/webhooks/payos` định tuyến không cycle qua `SubscriptionWebhookPort` trước khi fallback sang thanh toán đơn bàn; kiểm soát tính năng Pro bằng `@RequiresPlanFeature` + `SubscriptionFeatureInterceptor` trả HTTP `402 PRO_PLAN_REQUIRED`), `/ai/staff/query`, `/ai/staff/analyze-notes` (`POST` trong `STAFF_AI_POST` + `@PreAuthorize("hasAnyRole('MANAGER','STAFF','CHEF')")` kèm `NoteSafetyAnalyzer` không hạ cấp `CRITICAL`), `/ai/inventory/restock-suggestions` *(dùng `menuItemDemandIncludingCombos` bung nhu cầu nguyên liệu từ cả `COMBO`)* & `/ai/inventory/recipe-assistant` & `/ai/inventory/invoice-ocr` (`POST`, riêng `invoice-ocr` nhận `multipart/form-data`, kiểm tra chữ ký ảnh bằng `ImageFileValidator`, gọi Gemini Vision `inline_data` ngoài transaction DB và tự quy đổi đơn vị `kg↔g`/`l↔ml`; nằm trong `INVENTORY_AI_POST` + `@PreAuthorize("hasAnyRole('MANAGER','CHEF')")`, quota `operationsUser` `15/1m`, `150/1d`), `/ai/merchandising/combo-suggestions` & `/ai/merchandising/promotion-suggestions` (`POST` trong `MANAGER_POST` + `@PreAuthorize("hasRole('MANAGER')")`, quota `operationsUser`), `/ai/menu-item/description`, `/ai/insights/summary` (`POST` trong `MANAGER_POST` + `@PreAuthorize("hasRole('MANAGER')")` cho `MANAGER`/`ADMIN`; kiểm soát hạn mức `AiQuotaProperties.insightsUser`/`insightsUserDaily`; tổng hợp số liệu từ `ExecutiveInsightsContextProvider` và dự phòng xác định theo quy tắc khi Gemini gián đoạn)).

### 6.2 Thiết kế WebSocket STOMP (`/ws`)

| STOMP Topic | Phạm vi | Quyền Subscribe (đã áp dụng `RoleHierarchy`) | Sự kiện phát tán |
|---|---|---|---|
| `/topic/tables` | Public + Admin | Mọi client (gồm Customer tại bàn) | Thay đổi trạng thái bàn, gộp/hủy gộp bàn, đóng phiên, đồng bộ trạng thái yêu cầu gọi nhân viên an toàn cho màn hình khách (`SCR-05`) |
| `/topic/service-requests` | Protected | `STAFF`, `CASHIER`, `MANAGER`, `ADMIN` *(chặn `CHEF` & `CUSTOMER`)* | Hàng đợi yêu cầu bàn realtime: tạo mới, nhận xử lý (Single-Claim), hoàn tất, hủy & đóng khi kết thúc phiên |
| `/topic/orders` | Protected | `STAFF`, `CASHIER`, `CHEF`, `MANAGER`, `ADMIN` | Tạo đơn mới, cập nhật món, đổi trạng thái đơn |
| `/topic/kitchen` | Protected | `CHEF`, `MANAGER`, `ADMIN` *(chặn `STAFF`/`CASHIER`)* | Đợt gọi món mới đẩy xuống KDS, đổi trạng thái chế biến |
| `/topic/inventory` | Protected | `STAFF`, `CASHIER`, `CHEF`, `MANAGER`, `ADMIN` | Biến động tồn kho, cảnh báo sắp hết nguyên liệu |
| `/topic/promotions` | Protected | `STAFF`, `CASHIER`, `MANAGER`, `ADMIN` | Cập nhật chương trình khuyến mãi |
| `/topic/menu`, `/topic/categories`, `/topic/combos`, `/topic/vouchers`, `/topic/users`, `/topic/settings` | Mixed / Admin | Đồng bộ cache danh mục & cấu hình realtime | Invalidate React Query cache ngay lập tức |

### 6.3 Luồng 1: Khách quét QR → Đặt món Idempotent → KDS Realtime

```mermaid
sequenceDiagram
    actor Customer as Khách hàng (Mobile)
    participant FE as Frontend SPA
    participant API as OrderController & Service
    participant DB as PostgreSQL
    participant WS as STOMP Broker
    actor Chef as Bếp (KDS)

    Customer->>FE: Quét QR bàn (tableCode + session token)
    FE->>API: GET /api/v1/public/tables/{code} & menu
    API-->>FE: Menu + trạng thái bàn hiện tại
    Customer->>FE: Chọn món, topping & bấm "Gửi đơn"
    FE->>API: POST /api/v1/public/orders (Header: Idempotency-Key)
    API->>DB: Kiểm tra idempotency_requests (chống bấm đúp)
    API->>DB: Lưu orders + order_batches + order_item<br/>Giữ kho (Optimistic Lock) + ghi order_status_history
    DB-->>API: Commit thành công
    API->>WS: Broadcast /topic/orders, /topic/kitchen, /topic/tables
    WS-->>Chef: Hiển thị phiếu món mới trên Kanban + phát âm thanh
    API-->>FE: 201 Created (chi tiết đơn)
    FE-->>Customer: Cập nhật trạng thái đơn trực tiếp
```

### 6.4 Luồng 2: Thanh toán VietQR PayOS & Đóng phiên bàn tự động

```mermaid
sequenceDiagram
    actor Actor as Thu ngân / Khách
    participant FE as Frontend POS / Menu
    participant API as PaymentController & PayosService
    participant PayOS as PayOS Gateway
    participant DB as PostgreSQL
    participant WS as STOMP Broker

    Actor->>FE: Chọn thanh toán VietQR (áp dụng voucher nếu có)
    FE->>API: POST /api/v1/payments/payos/create
    API->>DB: Tạo payment_transactions (PENDING) + claim voucher
    API->>PayOS: Tạo link/mã VietQR (gateway_order_code)
    PayOS-->>API: Trả QR code + checkoutUrl
    API-->>FE: Hiển thị mã VietQR
    Actor->>PayOS: Quét QR chuyển khoản ngân hàng
    PayOS->>API: POST /api/v1/webhooks/payos (kèm chữ ký HMAC)
    API->>API: Xác thực chữ ký webhook & chống xử lý lặp
    API->>DB: Cập nhật payment PAID, order COMPLETED,<br/>đóng table_sessions, chuyển bàn về AVAILABLE,<br/>cập nhật daily_*_summary
    API->>WS: Broadcast /topic/tables & /topic/orders
    WS-->>FE: Tự động đóng modal QR & báo thanh toán thành công
```

### 6.5 Luồng 3: Khách gọi nhân viên (Anti-Spam) → Hàng đợi Staff Realtime (Single-Claim Lock)

```mermaid
sequenceDiagram
    actor Customer as Khách hàng (SCR-05)
    participant API as ServiceRequestControllers & Service
    participant DB as PostgreSQL
    participant WS as STOMP Broker
    actor StaffA as Nhân viên A (SCR-27)
    actor StaffB as Nhân viên B (SCR-27)

    Customer->>API: POST /api/v1/public/tables/{tableCode}/service-requests<br/>(sessionToken, reasonCode, note, clientRequestId)
    API->>API: Kiểm tra Rate Limit + Session Token + Validate lý do/ghi chú
    API->>DB: Lock bàn/phiên & kiểm tra idempotency,<br/>cooldown 60s, trùng lý do mở & tối đa 3 yêu cầu mở
    DB-->>API: Lưu table_service_requests (PENDING)
    API->>WS: Broadcast /topic/service-requests & /topic/tables
    WS-->>StaffA: Hiện thẻ yêu cầu mới + phát chuông
    WS-->>StaffB: Hiện thẻ yêu cầu mới + phát chuông
    WS-->>Customer: Hiện thẻ "Đang chờ tiếp nhận" + đếm ngược 60s

    par Hai nhân viên bấm "Nhận xử lý" đồng thời
        StaffA->>API: PATCH /api/v1/table-service-requests/{id}/claim
        StaffB->>API: PATCH /api/v1/table-service-requests/{id}/claim
    end

    API->>DB: SELECT ... FOR UPDATE (Pessimistic Lock)
    DB-->>API: Staff A khóa thành công -> CLAIMED (assignee = Staff A)
    API-->>StaffA: 200 OK (CLAIMED)
    API->>WS: Broadcast /topic/service-requests & /topic/tables
    WS-->>Customer: Cập nhật "Nhân viên A đang tới"

    DB-->>API: Staff B đọc lại thấy status = CLAIMED
    API-->>StaffB: 409 Conflict ("Đã được Nhân viên A tiếp nhận")
    WS-->>StaffB: Hàng đợi tự cập nhật thẻ sang CLAIMED

    StaffA->>API: PATCH /api/v1/table-service-requests/{id}/resolve
    API->>DB: Kiểm tra assignee == Staff A -> RESOLVED
    API->>WS: Broadcast /topic/service-requests & /topic/tables
    WS-->>Customer: Cập nhật "Đã hoàn tất" & giảm badge trên SCR-11
```

---

## 7. Kiến trúc Bảo mật & Phân quyền (Security & 6-Role RBAC)

### 7.1 Mô hình phân cấp quyền (`RoleHierarchy`)

```mermaid
flowchart LR
    ADMIN["PER-06: ADMIN<br/>Toàn quyền hệ thống<br/>Settings · Delete Order/Inventory · Prometheus"]
    MANAGER["PER-05: MANAGER<br/>Quản lý vận hành<br/>Menu · BOM · Staff · Voucher · Promotion · Revenue · Forecast"]
    CASHIER["PER-04: CASHIER<br/>Thu ngân<br/>Payments · Order History · Takeaway · Tables"]
    STAFF["PER-02: STAFF<br/>Phục vụ<br/>Table Map · Service Requests Queue · Order · Takeaway · AI Staff"]
    CHEF["PER-03: CHEF<br/>Bếp (KDS)<br/>Kitchen Board · Inventory View/Stock-In/Adjust"]
    CUSTOMER["PER-01: CUSTOMER (Public)<br/>QR Menu · Self-Order · Call Staff · AI Chat · Self-Checkout"]

    ADMIN --> MANAGER
    MANAGER --> CASHIER
    CASHIER --> STAFF
    MANAGER --> CHEF
```

### 7.2 Các lớp phòng thủ bảo mật
1. **Xác thực hai token đa thiết bị & Thu hồi tức thời:** Access Token (JWT ngắn hạn gửi qua Header `Authorization: Bearer`) + Refresh Token lưu trong **HttpOnly Cookie** (`Path=/api/v1`, tránh đánh cắp qua XSS) theo **nhiều session family độc lập trên mỗi tài khoản** (`auth:refresh:user:{id}:session:{fid}`, tối đa **10 thiết bị/user**). Đăng xuất chỉ thu hồi family hiện tại (kèm blacklist Access Token qua `TokenBlacklistService`), trong khi đổi mật khẩu, đặt lại mật khẩu hoặc khóa/ngưng hoạt động tài khoản thu hồi **toàn bộ family trên mọi thiết bị**; `WebSocketConfig` kiểm tra `mustChangePassword` khi bắt tay STOMP; Frontend `AuthContext` tự làm mới JWT bất đồng bộ trước khi STOMP kết nối lại, miễn tự động đăng xuất 20 phút cho màn hình KDS (`/admin/kitchen`) và không chặn điều hướng route công khai khi bootstrap phiên.
2. **Xác thực 2 bước cho Admin (2FA OTP):** `Admin2faService` lưu phiếu 2FA trong Redis Hash (`qros:2fa:ticket:*`) với mã OTP băm **SHA-256**, đối chiếu nguyên tử bằng **Lua script**, giới hạn tối đa **5 lần nhập sai** và **3 lần gửi lại** trong thời hạn tuyệt đối 5 phút kèm khóa cooldown gửi lại 60 giây nguyên tử (`setIfAbsent`) bảo toàn `attempts` (`put`/`increment`); gửi đồng bộ qua **Brevo REST API** (HTTPS) / SMTP và hủy phiếu + trả `503` khi gửi mail thất bại.
3. **Chống Brute-Force, BCrypt 72-Byte & Bảo vệ Admin:** Kết hợp `AuthRateLimitService`, `AccountLockoutService`, `PasswordResetService` (chặn tài khoản `INACTIVE`/`BANNED` khởi tạo hoặc hoàn tất đặt lại mật khẩu nhưng vẫn trả thông báo trung lập chống dò email) & `PasswordResetAttemptRecorder` (`REQUIRES_NEW`, khóa OTP SMS sau 5 lần sai), `@MaxUtf8Bytes(72)` trên toàn bộ DTO mật khẩu, `RedisRateLimiter` (dùng `PathPatternParser` + fallback Caffeine có giới hạn), `ClientAddressResolver` (chỉ tin `X-Forwarded-For` / `CF-Connecting-IP` từ proxy tin cậy kể cả dải CGNAT `100.64.0.0/10`), và bảo vệ tối thiểu 1 `ADMIN` đang hoạt động (`LAST_ACTIVE_ADMIN_REQUIRED`, chặn tự đổi vai trò/tự khóa).
4. **Bảo vệ QR & Tự thanh toán tại bàn:** `TableQrSecurityService` xác thực chữ ký/token phiên bàn (giữ nguyên tham số chữ ký QR khi phiên cũ hết hạn để khách mở phiên mới mượt mà); `CustomerSelfCheckoutService` xác thực `sessionToken` + quyền thành viên bàn (kể cả bàn gộp) khi đồng bộ trạng thái giao dịch (chống IDOR).
5. **Bảo vệ gọi nhân viên & hàng đợi yêu cầu bàn:** Mọi yêu cầu `POST /api/v1/public/tables/{tableCode}/service-requests` phải xác thực `sessionToken` hợp lệ của phiên đang mở kết hợp `RateLimitingFilter` (`public:service-requests`) và cooldown DB; các endpoint `/api/v1/table-service-requests/**` và STOMP `/topic/service-requests` chỉ cấp cho `STAFF`/`CASHIER`/`MANAGER`/`ADMIN` (chặn `CHEF` và `CUSTOMER`).
6. **Bảo mật tầng HTTP Edge, AI Prompt Isolation & Đồng bộ Cache:** `vercel.json` (`VITE_API_URL: ""`, header `/sw.js`), `nginx.conf` (chạy non-root `nginxinc/nginx-unprivileged:stable-alpine` cổng `8080` + `HEALTHCHECK`, `proxy_read_timeout 90s`, mở rộng `connect-src`), `render.yaml` (`runtime: docker`, `QR_SIGNING_SECRET: sync: false`) & `SecurityConfig` thiết lập CSP, `Permissions-Policy`, CORS theo origin hợp lệ; `GeminiChatGateway` loại bỏ hoàn toàn `?key=` khỏi URL query (chỉ gửi qua header `x-goog-api-key`) và `AiPromptBuilder` cô lập dữ liệu khách trong thẻ `<untrusted_customer_data>`; `OrderCacheInvalidationService` xóa cache đồng bộ tại pha `AFTER_COMMIT` không dùng `@Async`.

---

## 8. Hiệu năng, Độ tin cậy & Khả năng quan sát

- **Runtime & JVM Tuning:** Bật **Java 25 Virtual Threads** (`spring.threads.virtual.enabled: true`) cho thông lượng cao trên tác vụ I/O đồng thời (DB, PayOS, Gemini, Cloudinary); `Dockerfile` sử dụng **G1GC** kết hợp đầy đủ **C2 JIT** (`-XX:+UseG1GC`) thay cho `-XX:TieredStopAtLevel=1`; PostgreSQL container ấn định `TZ=Asia/Ho_Chi_Minh`.
- **Chiến lược Caching 2 môi trường:** Mặc định dùng **Caffeine** in-memory ở môi trường Dev để khởi động nhanh không cần dịch vụ phụ, tự động chuyển sang **Redis** (`SPRING_CACHE_TYPE=redis`) ở Production để chia sẻ cache (gồm `SystemSettingsSnapshotCache` giảm truy vấn bảng cấu hình trên mỗi đơn và `STATS_DASHBOARD` kèm trần lọc tối đa 366 ngày), blacklist JWT, multi-device refresh families, 2FA ticket và rate-limit counter giữa các instance.
- **Tối ưu báo cáo & Frontend Bundle:** Thay vì quét toàn bộ bảng `orders` và `order_item` cho biểu đồ doanh thu, `DailySummaryRefreshService` cập nhật tăng dần vào `order_reporting_fact` và 4 bảng `daily_*_summary`. Ở Frontend, Workbox (`vite-plugin-pwa`) áp dụng `globIgnores` loại trừ chunk admin/chart khỏi precache ban đầu (**69 entries / 1.47 MB**), đồng thời tách chế độ build **`build:capacitor`** (`frontend/.env.capacitor`) riêng biệt cho Android APK.
- **Cô lập lỗi dịch vụ ngoài (Graceful Degradation):**
  - *Gemini AI:* `GeminiProperties` (gắn `@ConstructorBinding` + chuẩn hóa connect/read timeout) và `GeminiChatGateway` tự fallback sang model dự phòng khi gặp `404`/`429`/`503` trong tổng ngân sách retry **`<= 45s`**; `AiQuotaService` trì hoãn trừ hạn mức tới sau khi gọi upstream thành công; `AiInvoiceOcrService` đối chiếu mờ theo ranh giới từ (`>= 2` token) và gắn cờ `UNIT_MISMATCH` khi quy đổi nhỏ hơn `0.001` thay vì làm tròn `0.000`.
  - *Email:* Ưu tiên **Brevo REST API qua HTTPS** (tránh bị cloud provider như Render chặn cổng SMTP 25/465/587), tự động dự phòng SMTP khi chạy local không cấu hình API key.
  - *Backup & Restore:* `scripts/backup-postgres.sh` & `scripts/restore-postgres.sh` dùng bộ đọc `.env` an toàn (không `source` trực tiếp tránh command injection) và chuẩn hóa chuỗi JDBC trước khi truyền cho `pg_dump`/`pg_restore`.
- **Observability:** Spring Boot Actuator expose `/actuator/health` (public cho healthcheck) và `/actuator/prometheus` (giới hạn quyền `ADMIN`) để giám sát JVM, HikariCP pool và độ trễ API.

---

## 9. Kiến trúc Triển khai & Quy trình CI/CD

### 9.1 Hạ tầng triển khai Production

```mermaid
flowchart LR
    GIT["GitHub Repository<br/>(CI: mvn verify · ArchUnit · Spotless · ESLint · node:test · build)"]
    VERCEL["Vercel<br/>React SPA + PWA<br/>Edge Proxy /api/v1 & /ws"]
    RENDER["Render / Docker Container<br/>Spring Boot 4.1.2 (Java 25)<br/>(Hỗ trợ Dockerfile & Dockerfile.native)"]
    NEON[("Neon PostgreSQL<br/>Managed Serverless DB")]
    REDIS[("Redis Cloud<br/>Cache · 2FA · Rate Limit")]
    APK["Android APK<br/>Capacitor 8.6 (frontend/android/)"]

    GIT --> VERCEL
    GIT --> RENDER
    GIT --> APK
    VERCEL -->|"HTTPS / WSS"| RENDER
    APK -->|"HTTPS / WSS"| RENDER
    RENDER --> NEON
    RENDER --> REDIS
```

### 9.2 Cổng kiểm soát chất lượng (Quality Gates — khớp CI & `05-specs/CONSTITUTION.md`)
- **Backend *(Java 25)*:** `mvn -f backend/pom.xml --batch-mode verify` chạy toàn bộ bộ test (**364** test cases: **357** Surefire unit/MVC tests gồm `CoreApiSecurityIntegrationTest`, `RbacHardeningSecurityTest`, `HibernateEntityMetadataTest`, `BackendDependencyRulesTest` 11 ArchUnit rules, `BackendMessageLanguageTest`, `Wave2HardeningRegressionTest` + **7** Failsafe Testcontainers PostgreSQL 16 & Redis 7 integration tests `PostgresFlywayAndDomainIT` & `RedisAuthAndConcurrencyIT` qua `maven-failsafe-plugin`) kèm `spotless:check`.
- **Frontend *(Node 22+)*:** `npm --prefix frontend test` (**160** test cases với `node:test`, gồm `wave2Hardening.test.js`) + `npm --prefix frontend run lint` (ESLint FSD strict, 0 errors/0 warnings) + `npm --prefix frontend run build` (Web PWA `1.47 MB`) & `npm --prefix frontend run build:capacitor` (Android APK).
- **E2E & Visual QA:** Bộ kịch bản Playwright (`scripts/phase4_full_ui_audit.js`, `scripts/android_audit.js`) kiểm thử toàn bộ 6 vai trò trên 3 kích thước màn hình (**164** ảnh chụp kiểm chứng tại `screenshots/`).
- **Chuyển giao sang Pha Code (`05-specs/`):** Mọi tính năng mới triển khai theo quy trình SDD tại [CONSTITUTION.md](../05-specs/CONSTITUTION.md) và `.agents/skills/sdd-coding-sync`.
