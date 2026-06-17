# QROS Frontend

Frontend QROS là React/Vite SPA cho hai nhóm người dùng:

- Khách hàng: quét QR, xem menu, chọn món/combo, áp voucher, gửi đơn và theo dõi đơn hiện tại của bàn.
- Nhân sự nhà hàng: đăng nhập trang quản trị để vận hành dashboard, bàn, bếp, menu, combo, kho, voucher, nhân viên, lịch sử đơn, thống kê, profile và settings.

Ứng dụng dùng STOMP/SockJS để nhận cập nhật realtime từ backend.

## Công nghệ

| Nhóm | Công nghệ |
| --- | --- |
| Core | React 19.2, React DOM 19.2, Vite 7.2 |
| Routing | React Router DOM 7 |
| UI | Tailwind CSS 4, lucide-react, react-icons, react-hot-toast |
| Data | TanStack Query 5, Axios, Zustand |
| Realtime | `@stomp/stompjs`, SockJS |
| Chart | Recharts |
| QR | qrcode.react |
| AI local | TensorFlow.js, optional dish classifier model |
| Tooling | ESLint 9, Vite manual chunks |

## Cấu trúc

```text
src/
├── app/                    # App shell, providers, router, global styles
├── pages/
│   ├── admin/              # Admin route pages
│   ├── auth/               # Login page
│   └── customer/           # Customer ordering pages
├── modules/
│   ├── ai-assistant/       # Gemini chat + optional local classifier
│   ├── analytics/          # Revenue, top dishes, staff stats UI/API
│   ├── auth/               # Auth API, context, protected route
│   ├── category-management/
│   ├── combo-management/
│   ├── customer-ordering/
│   ├── dashboard-overview/
│   ├── inventory-management/
│   ├── menu-management/
│   ├── notifications/
│   ├── order-management/
│   ├── payment/
│   ├── profile-management/
│   ├── settings-management/
│   ├── table-management/
│   ├── user-management/
│   └── voucher-management/
├── entities/               # UI/model nhỏ theo domain
├── shared/                 # API client, hooks, lib, stores, shared UI
└── widgets/                # Admin layout, customer menu, kitchen board
```

Alias trong `vite.config.js`:

```text
@        -> src
@app     -> src/app
@pages   -> src/pages
@widgets -> src/widgets
@modules -> src/modules
@entities-> src/entities
@shared  -> src/shared
```

## Biến môi trường

Frontend dùng biến Vite:

| Biến | Mô tả |
| --- | --- |
| `VITE_API_URL` | Base URL backend, ví dụ `http://localhost:8080` |
| `VITE_WS_URL` | SockJS endpoint, ví dụ `http://localhost:8080/ws` |

Local development hiện dùng:

```text
VITE_API_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
```

Production hiện trỏ:

```text
VITE_API_URL=https://order-by-qr.onrender.com
VITE_WS_URL=https://order-by-qr.onrender.com/ws
```

Nếu không đặt `VITE_API_URL`, Axios sẽ gọi relative path `/api/...`; Vite dev server đã proxy `/api` và `/ws` sang `http://localhost:8080`.

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
| `/menu?tableCode=<ma-ban>` | Giao diện khách hàng |
| `/login` | Đăng nhập quản trị |
| `/admin/dashboard` | Dashboard |
| `/admin/tables` | Sơ đồ bàn và thanh toán |
| `/admin/kitchen` | Bảng bếp |
| `/admin/inventory` | Quản lý kho |

Route `/` hiện redirect về `/menu?tableCode=478cae34fafc4030ac69`. Đây là mã bàn mẫu trong router, nên đổi khi triển khai thực tế nếu cần.

## Scripts

| Lệnh | Mô tả |
| --- | --- |
| `npm run dev` | Chạy Vite dev server |
| `npm run build` | Build production vào `dist/` |
| `npm run preview` | Preview bản build |
| `npm run lint` | Chạy ESLint |

Hiện `package.json` chưa có script test tự động.

## Routing và role

Routes chính lấy từ `src/app/router.jsx`:

| Route | Role |
| --- | --- |
| `/menu` | Public |
| `/login` | Public |
| `/admin/dashboard` | `MANAGER`, `STAFF`, `CHEF` |
| `/admin/profile` | `MANAGER`, `STAFF`, `CHEF` |
| `/admin/settings` | `MANAGER`, `STAFF`, `CHEF` |
| `/admin/categories` | `MANAGER` |
| `/admin/menu` | `MANAGER` |
| `/admin/combo` | `MANAGER` |
| `/admin/inventory` | `MANAGER` |
| `/admin/voucher` | `MANAGER` |
| `/admin/staffs` | `MANAGER` |
| `/admin/statistics/revenue` | `MANAGER` |
| `/admin/statistics/top-dishes` | `MANAGER` |
| `/admin/statistics/staff` | `MANAGER` |
| `/admin/tables` | `MANAGER`, `STAFF` |
| `/admin/history` | `MANAGER`, `STAFF` |
| `/admin/kitchen` | `MANAGER`, `CHEF` |

`ProtectedRoute` kiểm tra role trong `AuthContext`. Nếu không đủ quyền, user được chuyển đến `/unauthorized`.

## Auth và API client

HTTP client nằm ở:

```text
src/shared/api/httpClient.js
```

Đặc điểm:

- Dùng Axios với `withCredentials: true`.
- Tự thêm prefix `/api` cho URL nội bộ chưa có `/api`.
- Lưu access token trong memory qua `setAccessToken`.
- Gửi `Authorization: Bearer <token>` khi có access token.
- Tự unwrap backend `ApiResponse<T>`: trả về `data` khi `success=true`.
- Tự refresh access token khi admin route nhận `401`, dùng refresh cookie từ backend.

Auth context nằm ở:

```text
src/modules/auth/model/AuthContext.jsx
```

Ghi chú:

- Trang khách `/menu` không tự gọi refresh token.
- Admin session tự logout sau 20 phút không hoạt động.
- Logout sẽ disconnect WebSocket và xóa access token trong memory.

## Realtime

WebSocket service:

```text
src/shared/lib/websocket.js
src/shared/hooks/useWebSocket.js
```

Cấu hình:

- SockJS endpoint: `VITE_WS_URL` hoặc fallback `/ws`.
- STOMP reconnect delay: 5 giây.
- Heartbeat incoming/outgoing: 10 giây.
- Header `Authorization` được gửi khi có access token.

Topics frontend đang dùng:

| Topic | Nơi dùng |
| --- | --- |
| `/topic/tables` | Sơ đồ bàn, payment modal, menu khách |
| `/topic/kitchen` | Bảng bếp |
| `/topic/menu` | Quản lý menu, menu khách |
| `/topic/categories` | Quản lý danh mục, menu khách |
| `/topic/combos` | Quản lý combo, menu khách |
| `/topic/vouchers` | Quản lý voucher |
| `/topic/users` | Quản lý nhân viên |
| `/topic/settings` | Settings và menu khách |
| `/topic/orders` | Menu khách/current order updates |

## State management

Zustand đang được dùng cho:

- `modules/customer-ordering/model/cartStore.js`: giỏ hàng khách.
- `shared/model/settingsStore.js`: settings nhà hàng và cache client-side.
- `entities/category/model/categoryStore.js`: danh mục và invalidate/refetch.

Auth user state dùng React Context thay vì Zustand.

## Các module chức năng

| Module | Mô tả |
| --- | --- |
| `customer-ordering` | Menu khách, cart, tạo đơn, đơn hiện tại |
| `ai-assistant` | Chat Gemini qua backend và classifier local tùy chọn |
| `auth` | Login, refresh session, protected route |
| `dashboard-overview` | Tổng quan vận hành cho dashboard |
| `table-management` | Sơ đồ bàn, modal thanh toán, in hóa đơn, QR |
| `kitchen-board` | Board bếp realtime |
| `menu-management` | CRUD món và upload ảnh |
| `category-management` | CRUD/search danh mục và upload ảnh |
| `combo-management` | CRUD combo |
| `inventory-management` | Quản lý nguyên liệu, tồn kho, công thức và nhập/xuất kho |
| `voucher-management` | CRUD voucher |
| `user-management` | CRUD nhân viên và avatar |
| `profile-management` | Hồ sơ cá nhân, đổi mật khẩu, avatar |
| `settings-management` | Cấu hình nhà hàng, Wi-Fi, VAT, bật/tắt PayOS/cash/AI |
| `analytics` | Doanh thu, top món, hiệu suất nhân viên, forecast |
| `order-management` | Lịch sử đơn và chi tiết đơn |
| `payment` | Gọi API thanh toán |
| `notifications` | Âm báo và browser notification |

## Build và preview

```bash
npm run lint
npm run build
npm run preview
```

Vite đang tách chunk vendor cho React, TensorFlow, icon, QR và data libs để giảm cảnh báo chunk lớn.

## Docker

Build frontend image từ thư mục `frontend/`:

```bash
docker build -t order-by-qr-frontend:latest \
  --build-arg VITE_API_URL=http://localhost:8080 \
  --build-arg VITE_WS_URL=http://localhost:8080/ws \
  .
```

Runtime dùng Nginx:

- Serve SPA từ `/usr/share/nginx/html`.
- Rewrite route về `index.html`.
- Proxy `/api` và `/ws` sang service `backend:8080`.
- Bật cache static asset và gzip.

Nếu build từ root dự án:

```bash
docker build -t order-by-qr-frontend:latest \
  --build-arg VITE_API_URL=http://localhost:8080 \
  --build-arg VITE_WS_URL=http://localhost:8080/ws \
  frontend
```

## Deploy gợi ý

- Vercel: `vercel.json` rewrite mọi route về `/index.html`.
- Nginx container: dùng `frontend/nginx.conf`.
- Khi deploy khác domain backend, cập nhật `VITE_API_URL`, `VITE_WS_URL` và backend `APP_CORS_ALLOWED_ORIGINS`.
