# QROS Frontend

Frontend QROS là React/Vite SPA (hỗ trợ PWA và đóng gói Android bằng Capacitor 8) cho hai nhóm người dùng:

- Khách hàng: quét QR, xem menu, chọn món/combo, áp voucher, trò chuyện với trợ lý AI Gemini, gửi đơn và theo dõi đơn hiện tại của bàn.
- Nhân sự nhà hàng: đăng nhập trang quản trị để vận hành dashboard, bàn, đơn mang về, bếp KDS, menu, combo, kho, voucher, nhân viên, lịch sử đơn, thống kê, profile và settings.

Ứng dụng dùng STOMP/SockJS để nhận cập nhật realtime từ backend, hỗ trợ song ngữ `VI`/`EN` và Dark Mode (Evondev Signature).

## Công nghệ

| Nhóm | Công nghệ |
| --- | --- |
| Core | React 19.2, React DOM 19.2, Vite 7.2 |
| Routing | React Router DOM 7 |
| UI | Tailwind CSS 4, lucide-react, react-icons, react-hot-toast |
| Data | TanStack Query 5, Axios, Zustand 5 |
| Realtime | `@stomp/stompjs`, SockJS |
| Chart | Recharts 3 |
| QR | qrcode.react |
| PWA & Mobile | `vite-plugin-pwa`, `@capacitor/core` & `@capacitor/android` (`^8.6.2`) |
| Testing | Node.js `node:test` (`npm test`, `npm run test:coverage` — 22 suites / 160 tests) |
| Tooling | ESLint 9 (FSD layer, cross-slice & Public API barrel rules), Prettier, Vite manual chunks |

## Cấu trúc (Feature-Sliced Design — 19 Features, 16 Entities, 2 Widgets)

```text
src/
├── app/                      # App shell, providers, router (28 named routes + *), i18n, theme, global styles
├── pages/
│   ├── admin/                # Admin route pages (bao gồm compose order-modals cho TableMapPage & TakeawayPage)
│   ├── auth/                 # Login, forgot-password, reset-password pages
│   ├── customer/             # Customer menu & ordering pages
│   ├── landing/              # Landing page hướng dẫn quét QR
│   └── system/               # System pages (unauthorized, not-found, offline, error)
├── widgets/
│   ├── admin-layout/         # Layout quản trị (sidebar, header, navigation, mobile bottom nav)
│   └── order-modals/         # Cụm modal dùng chung (AddItemModal, OrderDetailModal, PaymentModal & hooks)
├── features/                 # 19 feature slices độc lập (0 cross-feature imports)
│   ├── ai-assistant/         # Trợ lý AI Gemini (khách hàng & nhân sự)
│   ├── analytics/            # Revenue, top dishes, staff stats & forecast UI/API
│   ├── auth/                 # Login, 2FA, forgot/reset password, protected route
│   ├── category-management/
│   ├── combo-management/
│   ├── customer-menu/        # Menu khách, giỏ hàng (cartStore.js), tạo đơn, tự thanh toán, gọi phục vụ
│   ├── dashboard-overview/
│   ├── inventory-management/
│   ├── kitchen-board/        # KDS Bếp realtime (Kanban & Batch Cooking)
│   ├── menu-management/
│   ├── order-management/
│   ├── profile-management/
│   ├── promotion-management/ # Quản lý chương trình khuyến mãi
│   ├── service-requests/     # Hàng đợi tiếp nhận & xử lý yêu cầu hỗ trợ tại bàn
│   ├── settings-management/
│   ├── table-management/
│   ├── takeaway-management/  # Quản lý đơn mang về (Takeaway)
│   ├── user-management/
│   └── voucher-management/
├── entities/                 # 16 domain entities (0 cross-entity imports, bắt buộc qua index.js)
│   ├── ai/                   # AI service, merchandising drafts, note safety triage UI/lib
│   ├── analytics/
│   ├── category/
│   ├── combo/
│   ├── inventory/
│   ├── menu-item/            # Bao gồm ItemOptionsModal
│   ├── order/
│   ├── payment/
│   ├── promotion/
│   ├── public-menu/
│   ├── service-request/
│   ├── session/              # AuthContext & authService
│   ├── settings/             # settingsStore, settingsService, React Query hooks
│   ├── table/
│   ├── user/
│   └── voucher/
└── shared/                   # Hạ tầng thuần không rò rỉ domain (API client, hooks, lib, stores, shared UI)
```

Alias trong `vite.config.js`:

```text
@         -> src
@app      -> src/app
@pages    -> src/pages
@widgets  -> src/widgets
@features -> src/features
@entities -> src/entities
@shared   -> src/shared
```

> **Kiểm soát ranh giới FSD tự động (`eslint.config.js`):** ESLint tự động chặn mọi import ngược chiều layer, mọi cross-entity/cross-feature import, và mọi deep import bỏ qua `index.js` barrel của slice bên ngoài.

## Biến môi trường

Frontend dùng biến Vite:

| Biến | Mô tả |
| --- | --- |
| `VITE_API_URL` | Origin backend, ví dụ `http://localhost:8080`; client tự nối `/api/v1` |
| `VITE_WS_URL` | SockJS endpoint, ví dụ `http://localhost:8080/ws` |
| `VITE_RESTAURANT_LAT` | Vĩ độ vị trí nhà hàng (phục vụ kiểm tra khoảng cách/định vị khi cần) |
| `VITE_RESTAURANT_LON` | Kinh độ vị trí nhà hàng |

Local development hiện dùng:

```text
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

Production Web (`frontend/.env.production` — đi qua Vercel/Nginx reverse proxy same-origin):

```text
VITE_API_URL=
VITE_WS_URL=
```

Mobile Capacitor (`frontend/.env.capacitor` — dùng qua `npm run build:capacitor` / `npm run build:android`):

```text
VITE_API_URL=https://order-by-qr.onrender.com
VITE_WS_URL=https://order-by-qr.onrender.com/ws
```

Nếu không đặt `VITE_API_URL`, Axios sẽ gọi relative path `/api/v1/...`; Vite dev server đã proxy `/api/v1` và `/ws` sang `http://localhost:8080`.

Production nên đặt `VITE_WS_URL` trỏ trực tiếp backend WebSocket (`https://<backend>/ws`) thay vì phụ thuộc rewrite `/ws` trên Vercel. Nếu dùng Nginx reverse proxy, bảo đảm proxy có `Upgrade`/`Connection` header như `frontend/nginx.conf`.

## Cài đặt và chạy local

```bash
npm ci
npm run dev
```

Ứng dụng chạy mặc định tại:

```text
http://localhost:5173
```

Backend local mặc định:

```text
http://localhost:8080
```

Trang thường dùng:

| URL | Mục đích |
| --- | --- |
| `/` | Landing page hướng dẫn khách quét mã QR tại bàn |
| `/menu?tableCode=<ma-ban>` hoặc `/:tableCode/menu` | Giao diện khách hàng |
| `/login` | Đăng nhập quản trị |
| `/admin/dashboard` | Dashboard |
| `/admin/tables` | Sơ đồ bàn và thanh toán |
| `/admin/kitchen` | Bảng bếp KDS |
| `/admin/inventory` | Quản lý kho |

## Scripts

| Lệnh | Mô tả |
| --- | --- |
| `npm run dev` | Chạy Vite dev server |
| `npm run build` | Build production Web/PWA vào `dist/` (Workbox precache `1.47 MB`) |
| `npm run build:capacitor` | Build production chế độ Capacitor (`--mode capacitor`, đọc `.env.capacitor`) |
| `npm run preview` | Preview bản build |
| `npm run lint` | Chạy ESLint (FSD strict rules) |
| `npm test` | Chạy 22 bộ unit test (160 tests) bằng Node.js `node:test` (`tests/run-tests.js`) |
| `npm run test:coverage` | Chạy unit test kèm báo cáo coverage (`--experimental-test-coverage`) |
| `npm run cap:sync` | Đồng bộ web build sang project Capacitor Android (`cap sync android`) |
| `npm run cap:open` | Mở project Android trong Android Studio (`cap open android`) |
| `npm run build:android` | Build chế độ Capacitor rồi đồng bộ sang Android (`npm run build:capacitor && cap sync android`) |
| `npm run format` | Định dạng mã nguồn bằng Prettier (`src/**/*.{js,jsx,css,json}`) |
| `npm run format:check` | Kiểm tra định dạng Prettier không ghi đè file |

## Routing và phân quyền (RBAC)

Routes chính được định nghĩa tại `src/app/router.jsx`:

| Route | Vai trò được phép (Allowed Roles) | Ghi chú |
| --- | --- | --- |
| `/` | Mọi người (Public) | Landing page hướng dẫn quét QR |
| `/:tableCode/menu` | Khách hàng (Quét mã QR bàn) | Giao diện đặt món chính |
| `/menu` | Mọi người (Public) | Xem thực đơn công khai |
| `/login` | Mọi người (Public) | Đăng nhập (hỗ trợ 2FA Admin) |
| `/forgot-password` | Mọi người (Public) | Quên mật khẩu (gửi yêu cầu đặt lại) |
| `/reset-password` | Mọi người (Public) | Đặt lại mật khẩu từ link/token |
| `/unauthorized` | Mọi người (Public) | Trang thông báo không đủ quyền truy cập |
| `/not-found` | Mọi người (Public) | Trang 404 không tìm thấy đường dẫn |
| `/offline` | Mọi người (Public) | Trang thông báo mất kết nối mạng (PWA) |
| `/error/:statusCode` | Mọi người (Public) | Trang hiển thị lỗi HTTP theo mã trạng thái |
| `/admin/profile` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | Hồ sơ cá nhân, đổi mật khẩu |
| `/admin/settings` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | Các tab cấu hình hệ thống chỉ mở cho `ADMIN`; tùy chọn cá nhân (ngôn ngữ, giao diện) mở cho cả 5 vai trò |
| `/admin/dashboard` | `ADMIN`, `MANAGER` | Tổng quan kinh doanh & KPI |
| `/admin/categories` | `ADMIN`, `MANAGER` | Quản trị danh mục |
| `/admin/menu` | `ADMIN`, `MANAGER` | Quản trị món ăn & topping |
| `/admin/combo` | `ADMIN`, `MANAGER` | Quản trị gói combo |
| `/admin/inventory` | `ADMIN`, `MANAGER`, `CHEF` | Quản lý kho & nguyên liệu |
| `/admin/voucher` | `ADMIN`, `MANAGER` | Quản trị mã giảm giá |
| `/admin/promotions` | `ADMIN`, `MANAGER` | Quản trị chương trình khuyến mãi |
| `/admin/staffs` | `ADMIN`, `MANAGER` | Quản lý tài khoản nhân sự |
| `/admin/statistics/revenue` | `ADMIN`, `MANAGER` | Phân tích doanh thu chuyên sâu & dự báo AI |
| `/admin/statistics/top-dishes` | `ADMIN`, `MANAGER` | Thống kê món bán chạy |
| `/admin/statistics/staff` | `ADMIN`, `MANAGER` | Thống kê hiệu suất nhân viên |
| `/admin/tables` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | Sơ đồ bàn, gọi món, gộp bàn |
| `/admin/service-requests` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | Hàng đợi tiếp nhận & xử lý yêu cầu hỗ trợ tại bàn |
| `/admin/takeaway` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | Quản lý đơn mang về |
| `/admin/history` | `ADMIN`, `MANAGER`, `CASHIER` | Lịch sử & tra soát hóa đơn |
| `/admin/kitchen` | `ADMIN`, `MANAGER`, `CHEF` | KDS Bếp realtime (Kanban/Gom món) |

`ProtectedRoute` kiểm tra role trong `AuthContext` (`@entities/session`). Nếu không đủ quyền, user được chuyển hướng tới trang `/unauthorized`.

## Auth và API client

HTTP client nằm ở:

```text
src/shared/api/httpClient.js
```

Đặc điểm:

- Dùng Axios với `withCredentials: true`.
- Nối `API_PREFIX = '/api/v1'` vào `baseURL` (`VITE_API_URL` hoặc relative `/api/v1`).
- Lưu access token trong memory qua `setAccessToken`.
- Gửi `Authorization: Bearer <token>` khi có access token.
- Tự unwrap backend `ApiResponse<T>`: trả về `data` khi `success=true`.
- Tự refresh access token khi admin route nhận `401`, dùng refresh cookie từ backend.

Auth context nằm ở:

```text
src/entities/session/model/AuthContext.jsx   (export qua @entities/session)
```

Ghi chú:

- Trang khách `/menu` và các route công khai không bị chặn điều hướng khi khởi tạo phiên và không tự gọi refresh token.
- Admin session tự logout sau 20 phút không hoạt động; riêng màn hình Bếp KDS (`/admin/kitchen`) được **miễn tự động đăng xuất** để vận hành treo tường liên tục.
- Trước khi STOMP reconnect, `AuthContext` tự làm mới access token bất đồng bộ để không đứt kênh realtime; khi logout sẽ ngắt WebSocket và xóa access token trong memory.

## Realtime

WebSocket service và hook:

```text
src/shared/lib/websocket.js
src/shared/hooks/useWebSocket.js
```

Ứng dụng tách biệt hai kết nối WebSocket theo ngữ cảnh:

- `publicWsService`: dùng cho trang khách hàng (không gửi JWT), chỉ subscribe các topic công khai.
- `adminWsService`: dùng cho khu vực quản trị/vận hành, đính kèm header `Authorization: Bearer <access-token>` khi kết nối để subscribe các protected topics.

Cấu hình chung:

- SockJS endpoint: `VITE_WS_URL` hoặc fallback `/ws`.
- Production ưu tiên `VITE_WS_URL` trực tiếp tới backend; fallback `/ws` chỉ phù hợp khi reverse proxy hỗ trợ WebSocket upgrade rõ ràng.
- STOMP reconnect delay: 5 giây.
- Heartbeat incoming/outgoing: 10 giây.

Topics frontend đang dùng (12 topics):

| Topic | Service | Nơi dùng |
| --- | --- | --- |
| `/topic/tables` | `publicWsService` / `adminWsService` | Sơ đồ bàn, payment modal, cập nhật bàn & đơn cho menu khách |
| `/topic/menu` | `publicWsService` / `adminWsService` | Quản lý menu, menu khách |
| `/topic/categories` | `publicWsService` / `adminWsService` | Quản lý danh mục, menu khách |
| `/topic/combos` | `publicWsService` / `adminWsService` | Quản lý combo, menu khách |
| `/topic/settings` | `publicWsService` / `adminWsService` | Settings và menu khách |
| `/topic/orders` | `adminWsService` (Admin-only) | Quản lý đơn hàng & lịch sử/vận hành nội bộ (menu khách dùng `/topic/tables`) |
| `/topic/service-requests` | `adminWsService` | Hàng đợi yêu cầu hỗ trợ tại bàn |
| `/topic/kitchen` | `adminWsService` | Bảng bếp KDS |
| `/topic/vouchers` | `adminWsService` | Quản lý voucher |
| `/topic/promotions` | `adminWsService` | Quản lý khuyến mãi |
| `/topic/inventory` | `adminWsService` | Quản lý kho nguyên liệu |
| `/topic/users` | `adminWsService` | Quản lý nhân viên |

## State management

Zustand đang được dùng cho:

- `src/features/customer-menu/model/cartStore.js`: giỏ hàng khách.
- `src/entities/settings/model/settingsStore.js` (export qua `@entities/settings`): settings nhà hàng và cache client-side.
- `src/shared/stores/useAdminPreferences.js`: ngôn ngữ, giao diện sáng/tối và tùy chọn cá nhân.

Auth user state dùng React Context (`@entities/session`) thay vì Zustand.

## Các feature chức năng (19 Features)

| Module | Mô tả |
| --- | --- |
| `customer-menu` | Menu khách, cart (`cartStore.js`), tạo đơn, tự thanh toán VietQR, gọi phục vụ, theo dõi đơn hiện tại |
| `ai-assistant` | Trợ lý AI Gemini qua backend cho khách hàng và nhân sự |
| `auth` | Login, Admin 2FA OTP, quên/đặt lại mật khẩu, protected route |
| `dashboard-overview` | Tổng quan vận hành, dự báo & tóm tắt AI cho dashboard |
| `table-management` | Sơ đồ bàn, gộp/tách bàn, QR (kết hợp `@widgets/order-modals` tại page layer) |
| `service-requests` | Hàng đợi tiếp nhận, xử lý và hủy yêu cầu hỗ trợ tại bàn realtime |
| `takeaway-management` | Quản lý đơn mang về (Takeaway) |
| `kitchen-board` | Bảng bếp KDS realtime (Kanban, Batch Cooking & phân tích ghi chú AI) |
| `menu-management` | CRUD món, sinh mô tả AI và upload ảnh |
| `category-management` | CRUD/search danh mục và upload ảnh |
| `combo-management` | CRUD combo và gợi ý combo AI |
| `inventory-management` | Quản lý nguyên liệu, tồn kho, công thức, nhập kho theo lô, OCR hóa đơn & trợ lý kho AI |
| `voucher-management` | CRUD voucher |
| `promotion-management` | CRUD chương trình khuyến mãi và gợi ý khuyến mãi AI |
| `user-management` | CRUD nhân viên và avatar |
| `profile-management` | Hồ sơ cá nhân, đổi mật khẩu, avatar |
| `settings-management` | Cấu hình nhà hàng, Wi-Fi, VAT, bật/tắt tự thanh toán/PayOS/cash/AI và tùy chọn cá nhân |
| `analytics` | Doanh thu, top món, hiệu suất nhân viên, forecast |
| `order-management` | Lịch sử đơn và chi tiết đơn |

## Tính năng trải nghiệm & Đóng gói

### Đa ngôn ngữ — Bilingual i18n (`VI` / `EN`)

- Hỗ trợ chuyển đổi tức thì giữa Tiếng Việt (`VI`) và Tiếng Anh (`EN`) trên cả giao diện khách hàng và khu vực quản trị.
- Ngôn ngữ người dùng chọn được lưu bền vững trên trình duyệt và áp dụng thống nhất cho nhãn điều hướng, form, thông báo và màn hình hệ thống.

### Dark Mode (Evondev Signature)

- Hỗ trợ chuyển đổi Light Mode / Dark Mode theo hệ thống thiết kế **Evondev Signature** (nền Deep Midnight `#0B0F17`, accent Indigo `#6366F1` & Royal Violet `#8B5CF6`, độ tương phản đạt chuẩn WCAG AA).
- Tối ưu hiển thị cho môi trường thiếu sáng tại quầy thu ngân và màn hình Bếp KDS.

### Progressive Web App (PWA)

- Tích hợp `vite-plugin-pwa` để đăng ký Service Worker, manifest và hỗ trợ cài đặt trực tiếp lên màn hình chính (Home Screen) trên điện thoại/tablet; Workbox áp dụng `globIgnores` loại trừ chunk admin/chart giúp giảm dung lượng precache ban đầu xuống **`1.47 MB`** (`69` entries).
- Có sẵn trang `/offline` khi thiết bị mất kết nối mạng.

### Đóng gói Android (Capacitor 8)

- Sử dụng `@capacitor/core` và `@capacitor/android` (`^8.6.2`) để đóng gói bản web SPA thành ứng dụng Android gốc cho thiết bị POS/Tablet/Bếp.
- Quy trình build nhanh:

```bash
npm run build:android
npm run cap:open
```

## Build và preview

```bash
npm run lint
npm test
npm run build
npm run preview
```

Vite đang tách chunk vendor cho React, icon, chart, QR và data libs để tối ưu kích thước bundle và giảm cảnh báo chunk lớn.

## Docker

Build frontend image từ thư mục `frontend/`:

```bash
docker build -t order-by-qr-frontend:latest \
  --build-arg VITE_API_URL=http://localhost:8080 \
  --build-arg VITE_WS_URL=http://localhost:8080/ws \
  .
```

Runtime dùng **`nginxinc/nginx-unprivileged:stable-alpine`** (chạy non-root trên cổng **`8080`** kèm `HEALTHCHECK`):

- Serve SPA từ `/usr/share/nginx/html`.
- Rewrite route về `index.html`.
- Proxy `/api/v1` và `/ws` sang service `backend:8080`.
- Bật cache static asset, CSP headers và gzip.

Nếu build từ root dự án:

```bash
docker build -t order-by-qr-frontend:latest \
  --build-arg VITE_API_URL=http://localhost:8080 \
  --build-arg VITE_WS_URL=http://localhost:8080/ws \
  frontend
```

## Deploy gợi ý

- Vercel: `vercel.json` cấu hình rewrite `/api/v1/(.*)` và `/ws(.*)` sang backend, SPA fallback về `/index.html`, cùng các header bảo mật và CSP.
- Nginx container: dùng `frontend/nginx.conf`.
- Khi deploy khác domain backend, cập nhật `VITE_API_URL`, `VITE_WS_URL` và backend `APP_CORS_ALLOWED_ORIGINS`.
