# DEFECT & BUG AUDIT REPORT (BÁO CÁO LỖI PHẦN MỀM)
**Hệ thống: QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa vai trò**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày lập báo cáo: 2026-11-14*
*Đơn vị thực hiện: Senior QA Automation Lead & Frontend Architect*

---

## 1. TỔNG HỢP DANH MỤC LỖI (DEFECT SUMMARY)

| Mã lỗi (Bug ID) | Phân hệ / Màn hình | Vai trò ảnh hưởng | Mức độ nghiêm trọng | Trạng thái |
|---|---|---|:---:|:---:|
| **BUG-01** | Lịch sử đơn hàng (`/admin/history`) | `CASHIER` | **MAJOR** | ✅ ĐÃ KHẮC PHỤC (RESOLVED) |
| **BUG-02** | Hình ảnh món ăn Catalog | `CUSTOMER`, `MANAGER` | **MINOR** | Cần đồng bộ CDN Cloudinary |
| **BUG-03** | WebSocket Realtime STOMP | Toàn bộ Role nội bộ | **MINOR** | ✅ ĐÃ KHẮC PHỤC (RESOLVED) |
| **BUG-04** | Cảnh báo unload Event Listener | Mọi màn hình | **TRIVIAL** | ✅ ĐÃ KHẮC PHỤC (RESOLVED) |
| **BUG-05** | Rate Limit Xác thực 2FA Admin | `ADMIN` | **MINOR** | ✅ ĐÃ KHẮC PHỤC (RESOLVED) |

---

## 2. CHI TIẾT TỪNG LỖI VÀ HƯỚNG XỬ LÝ (DETAILED DEFECT TICKETS)

---

### BUG-01: Tài khoản Thu ngân (Cashier) bị lỗi 403 khi tải Lịch sử Đơn hàng
- **Mức độ nghiêm trọng:** **MAJOR**
- **Vai trò bị ảnh hưởng:** `CASHIER`
- **Màn hình xảy ra:** Trang Lịch sử đơn hàng (`/admin/history`) trên mọi thiết bị (Desktop, Tablet, Mobile).
- **Mô tả hiện tượng:**
  - Theo thiết kế router frontend (`router.jsx`), tài khoản vai trò `CASHIER` được cấp quyền truy cập tuyến đường `/admin/history`.
  - Tuy nhiên, trang này gọi ngầm hai API:
    1. `GET /api/v1/analytics/orders/history?from=...&to=...&page=0&size=10`
    2. `GET /api/v1/analytics/orders/summary?from=...&to=...`
  - Backend Spring Security hiện tại chỉ phân quyền tiền tố `/api/v1/analytics/**` cho vai trò `MANAGER` và `ADMIN`. Dẫn đến Thu ngân bị trả về mã lỗi **HTTP 403 Forbidden**.
- **Các bước tái hiện (Steps to Reproduce):**
  1. Đăng nhập bằng tài khoản Thu ngân (`cashier@gmail.com` / `cashier123`).
  2. Điều hướng tới URL `http://localhost:5173/admin/history`.
  3. Mở DevTools tab Network -> Quan sát thấy hai request trên bị đỏ với mã lỗi `403`.
- **Kết quả mong đợi:** Thu ngân xem được danh sách và tổng hợp hóa đơn để đối soát ca làm việc.
- **Kết quả thực tế:** Bảng không tải được dữ liệu và console báo lỗi 403.
- **Ảnh minh chứng:** `screenshots/cashier/desktop/pages/cashier_desktop_pages_order-history_default.png`
- **Đề xuất & Giải pháp đã thực hiện (Resolved):**
  - Đã thêm `CASHIER_ORDER_HISTORY_GET` vào `SecurityRoutes.java` cho hai endpoint `/api/v1/analytics/orders/history` và `/orders/summary`.
  - Cấu hình trong `SecurityConfig.java`: `.requestMatchers(HttpMethod.GET, CASHIER_ORDER_HISTORY_GET).hasAnyRole("MANAGER", "CASHIER")`.
  - Bổ sung `@PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")` tại `AnalyticsController.java` cho `getOrderHistory` và `getOrderAnalytics`.
  - Bổ sung kiểm thử hồi quy tại `SecurityConfigRegressionTest.java`.

---

### BUG-02: Link ảnh món ăn từ Unsplash bị chặn bởi chính sách bảo mật ORB
- **Mức độ nghiêm trọng:** **MINOR**
- **Vai trò bị ảnh hưởng:** `CUSTOMER`, `MANAGER`
- **Màn hình xảy ra:** Thực đơn khách (`/T01/menu`), Quản lý món ăn (`/admin/menu`).
- **Mô tả hiện tượng:**
  - Trong dữ liệu mẫu seed data ban đầu, một số món ăn được gán URL ảnh trực tiếp từ miền `https://images.unsplash.com/...`.
  - Trình duyệt Chromium áp dụng cơ chế bảo mật ORB (Opaque Response Blocking) chặn các tài nguyên ảnh ngoại lai không kèm tiêu đề CORS chuẩn, gây lỗi `net::ERR_BLOCKED_BY_ORB`.
- **Kết quả mong đợi:** Ảnh món ăn tải mượt mà.
- **Kết quả thực tế:** Ảnh bị chặn và ứng dụng phải kích hoạt icon placeholder thay thế.
- **Đề xuất khắc phục:** Chạy script di chuyển (migrate) các ảnh seed sang Cloudinary của dự án (`res.cloudinary.com/...`).

---

### BUG-03: Lỗi ngắt kết nối WebSocket STOMP khi chuyển trang nhanh
- **Mức độ nghiêm trọng:** **MINOR**
- **Vai trò bị ảnh hưởng:** `STAFF`, `CHEF`, `CASHIER`, `ADMIN`
- **Mô tả hiện tượng:**
  - Khi người dùng click chuyển đổi giữa các menu quản trị nhanh, component unmount nhưng kết nối STOMP socket vẫn đang trong tiến trình gửi gói tin heartbeat hoặc subscribe, dẫn tới lỗi:
    `admin WebSocket broker error: Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]`.
  - Nguyên nhân cốt lõi: Backend `WebSocketConfig.java` kiểm tra `PROTECTED_TOPICS` bằng `auth.getAuthorities()` trực tiếp mà không thông qua `RoleHierarchy`, khiến tài khoản `ADMIN` và `CASHIER` bị từ chối quyền đăng ký các topic vận hành (`/topic/orders`, `/topic/users`...).
- **Kết quả mong đợi:** Đăng ký topic và chuyển trang mượt mà không sinh log lỗi console.
- **Đề xuất & Giải pháp đã thực hiện (Resolved):**
  - Tích hợp `RoleHierarchy` vào `WebSocketConfig.java` và đánh giá quyền bằng `roleHierarchy.getReachableGrantedAuthorities(auth.getAuthorities())`.
  - Cập nhật hàm `disconnect()` trong `frontend/src/shared/lib/websocket.js` bọc `try-catch` khi gọi `subscription.unsubscribe()`, ngăn ngừa ngoại lệ khi kết nối đang đóng.

---

### BUG-04: Cảnh báo vi phạm chính sách unload của trình duyệt hiện đại
- **Mức độ nghiêm trọng:** **TRIVIAL** (Không ảnh hưởng chức năng)
- **Mô tả hiện tượng:**
  - Console xuất hiện nhiều cảnh báo: `Permissions policy violation: unload is not allowed in this document.`.
- **Nguyên nhân:** Thư viện bên ngoài (PWA workbox hoặc SockJS) đăng ký sự kiện `window.addEventListener('unload')`, vốn bị Chromium 115+ cấm mặc định nếu không khai báo Permissions Policy.
- **Đề xuất & Giải pháp đã thực hiện (Resolved):**
  - Cập nhật `Permissions-Policy` header trong `backend/.../SecurityConfig.java`: bổ sung chỉ thị `unload=(self)`, cho phép ứng dụng dọn dẹp kết nối an toàn mà không bị trình duyệt cảnh báo vi phạm.

---

### BUG-05: Rate Limit đăng nhập 2FA Admin trả về 429 chưa có Popup đếm lùi
- **Mức độ nghiêm trọng:** **MINOR**
- **Vai trò bị ảnh hưởng:** `ADMIN`
- **Mô tả hiện tượng:**
  - Khi Admin nhập sai hoặc gửi mã 2FA nhiều lần liên tiếp, Redis Rate Limiter trả về HTTP 429.
  - Giao diện trước đây chỉ hiển thị thông báo lỗi chung, chưa có bộ đếm lùi thời gian (Countdown timer: "Vui lòng thử lại sau X giây").
- **Đề xuất & Giải pháp đã thực hiện (Resolved):**
  - Cập nhật `frontend/src/pages/auth/LoginPage.jsx`: bắt mã lỗi `429` và đọc `retryAfterSeconds` từ response details.
  - Kích hoạt state đếm ngược `rateLimitCooldown`, hiển thị thông báo thời gian chờ thực tế và tạm khóa nút bấm với nhãn `THỬ LẠI SAU (Xs)` cho đến khi hết thời gian khóa.
