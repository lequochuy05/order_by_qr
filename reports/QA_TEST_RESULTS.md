# QA AUTOMATION TEST RESULTS & EXECUTION AUDIT REPORT
**Hệ thống: QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa vai trò**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày thực hiện: 2026-11-14*
*Khung kiểm thử: Playwright v1.65.0 (Chromium Headless) | Môi trường: Linux (CachyOS)*

---

## 1. BẢNG TỔNG HỢP CHỈ SỐ KIỂM THỬ (EXECUTIVE QA METRICS)

| Chỉ số kiểm thử (Test Metric) | Giá trị ghi nhận | Tỷ lệ hoàn thành | Đánh giá |
|---|:---:|:---:|:---:|
| **Tổng số vai trò đã kiểm thử (Roles Tested)** | **6 / 6 vai trò** | 100% | ✅ PASS |
| **Tổng số tuyến đường kiểm thử (Routes Tested)** | **26 named routes** (27 gồm `*` wildcard fallback) | 100% | ✅ PASS |
| **Tổng số ảnh chụp màn hình (Total Screenshots)** | **144 ảnh chuẩn hóa** (+ 20 ảnh bổ sung trong `screenshots/`) | 100% | ✅ PASS |
| **Kích thước thiết bị kiểm thử (Viewports)** | **3 chuẩn** (`1440x900`, `768x1024`, `390x844`) | 100% | ✅ PASS |
| **Số lượng Modals đã kích hoạt thực tế** | **15 Modals** | 100% | ✅ PASS |
| **Số lượng Biểu mẫu đã kiểm thử (Forms)** | **14 Forms** | 100% | ✅ PASS |
| **Chạy lần đầu — Trước khi vá (Initial Run, Pre-Fix)** | **138 / 144 PASS**, **6 / 144 bị ảnh hưởng** (`BUG-01` Cashier `/admin/history` trên 3 viewports & các cảnh báo liên quan) | 95.8% PASS / 4.2% FAIL | ⚠️ DEFECTS FOUND |
| **Kiểm thử lại — Sau khi vá (Re-verification, Post-Fix)** | **144 / 144 PASS** (sau khi khắc phục `BUG-01`, `BUG-03`, `BUG-04`, `BUG-05`) | **100% PASS** | ✅ RESOLVED / PASS |

---

## 2. KẾT QUẢ KIỂM THỬ THEO TỪNG VAI TRÒ (ROLE-BY-ROLE BREAKDOWN)

### 2.1 Customer (Khách hàng) — PASS (100%)
- **Tuyến đường:** Landing Page (`/`), Menu mã bàn (`/T01/menu`), Menu công khai (`/menu`).
- **Tương tác:** Mở modal tùy chọn món (`ItemOptionsModal`), thêm món vào giỏ, mở giỏ hàng (`CartModal`), mở Drawer trợ lý AI Gemini (`AiChatAssistant`).
- **Đánh giá:** Giao diện đặt món mượt mà, PWA hiển thị tốt, responsive co giãn chuẩn từ điện thoại đến máy tính bảng.

### 2.2 Staff (Nhân viên phục vụ) — PASS (100%)
- **Tuyến đường:** Sơ đồ bàn (`/admin/tables`), Đơn mang về (`/admin/takeaway`), Hồ sơ cá nhân (`/admin/profile`), Tùy chọn (`/admin/settings`).
- **Tương tác:** Mở Modal gộp bàn (`MergeTableModal`), mở Modal tạo đơn mang về (`takeaway-create`).
- **Đánh giá:** Thao tác bàn nhanh, sơ đồ lưới bàn hiển thị rõ ràng trên cả 3 kích thước thiết bị.

### 2.3 Chef (Đầu bếp) — PASS (100%)
- **Tuyến đường:** KDS Bếp (`/admin/kitchen`), Quản lý kho (`/admin/inventory`), Hồ sơ cá nhân.
- **Tương tác:** Chuyển đổi linh hoạt giữa chế độ Kanban và chế độ Nấu gom món (`BatchCookingView`), kiểm tra bảng tồn kho nguyên liệu.
- **Đánh giá:** Màn hình KDS tối ưu rất tốt cho Tablet đặt tại quầy bếp.

### 2.4 Cashier (Thu ngân) — ✅ RESOLVED (Post-Fix PASS 100% | Initial Run: 80%)
- **Tuyến đường:** Sơ đồ bàn (`/admin/tables`), Lập hóa đơn (`PaymentModal`), Lịch sử đơn hàng (`/admin/history`), Đơn mang về.
- **Tương tác:** Mở Modal thanh toán tiền mặt/chuyển khoản VietQR PayOS thành công.
- **Vấn đề phát hiện (Initial Run) & Khắc phục (Post-Fix):** Trong lần chạy đầu, khi tài khoản `CASHIER` truy cập trang `/admin/history`, hai endpoint thống kê nền (`/api/v1/analytics/orders/history` và `/api/v1/analytics/orders/summary`) trả về **HTTP 403 Forbidden** (`BUG-01`). **Trạng thái hiện tại: ĐÃ KHẮC PHỤC (RESOLVED — Post-Fix PASS)** sau khi mở quyền `GET` cho `MANAGER`, `CASHIER` tại `SecurityConfig.java` (dòng 95–96), `SecurityRoutes.java` và `AnalyticsController.java`, được xác nhận lại trên cả 3 viewports (xem chi tiết tại `reports/BUG_REPORT.md`).

### 2.5 Manager (Quản lý) — PASS (100%)
- **Tuyến đường:** Dashboard (`/admin/dashboard`), Quản lý Danh mục, Món ăn, Combo, Voucher, Nhân viên, Thống kê Món, Thống kê Nhân viên, Bàn ăn, Lịch sử.
- **Tương tác:** Kích hoạt thành công toàn bộ 5 Modals tạo mới (Category, Menu Item, Combo, Voucher, User) và Modal chi tiết đơn hàng cũ trong lịch sử.
- **Đánh giá:** Phân hệ quản lý đầy đủ nhất, các biểu đồ Recharts hiển thị mượt mà.

### 2.6 Admin (Quản trị viên) — PASS (100%)
- **Tuyến đường:** Đăng nhập bảo mật 2FA OTP (`/login`), Thống kê Doanh thu độc quyền (`/admin/statistics/revenue`), Cấu hình Nhà hàng toàn diện (`/admin/settings`).
- **Tương tác:** Bắt mã 2FA từ Redis `qros:2fa:ticket:*`, đăng nhập thành công vào trang doanh thu và duyệt qua **8 tab cài đặt** (1 tab Tùy chọn cá nhân + 7 tab Cài đặt hệ thống).
- **Đánh giá:** Tính năng bảo mật 2FA hoạt động xuất sắc, báo cáo doanh thu chuyên sâu hiển thị đầy đủ biểu đồ vùng (AreaChart) và số liệu tài chính.

---

## 3. PHÂN TÍCH NHẬT KÝ LỖI (CONSOLE & NETWORK LOGS ANALYSIS)

Trong suốt quá trình kiểm thử tự động, hệ thống Playwright đã bắt và ghi lại toàn bộ lỗi runtime vào `artifacts/ui-audit/logs/`:

### 3.1 Lỗi Trình duyệt (Browser Console Errors)
1. **Permissions policy violation (`unload`) — ✅ RESOLVED (`BUG-04`):**
   - *Chi tiết:* `Permissions policy violation: unload is not allowed in this document.`
   - *Phân tích & Khắc phục:* Do Chrome/Chromium phiên bản mới deprecate sự kiện `unload` nếu không khai báo Permissions Policy. Đã khắc phục bằng cách bổ sung `unload=(self)` vào header `Permissions-Policy` trong `SecurityConfig.java` (xem `reports/BUG_REPORT.md` — `BUG-04`).
2. **WebSocket STOMP Broker Connection Drops — ✅ RESOLVED (`BUG-03`):**
   - *Chi tiết:* `admin WebSocket broker error: Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]`
   - *Phân tích & Khắc phục:* Xảy ra do `WebSocketConfig.java` chưa đánh giá quyền qua `RoleHierarchy` và `websocket.js` chưa bọc `try-catch` khi unmount nhanh. Đã khắc phục hoàn toàn ở cả backend và frontend (xem `reports/BUG_REPORT.md` — `BUG-03`).

### 3.2 Lỗi Mạng & API (Network Request Failures)
1. **Lỗi ảnh Unsplash bị chặn bởi ORB (Opaque Response Blocking) — OPEN / MINOR (`BUG-02`):**
   - *Chi tiết:* `net::ERR_BLOCKED_BY_ORB` trên các URL `https://images.unsplash.com/...`
   - *Phân tích:* Không còn URL `unsplash.com` trong mã nguồn hay Flyway migrations; các lỗi ghi nhận xuất phát từ bản ghi legacy trên Neon DB chưa migrate sang Cloudinary.
   - *Khuyến nghị:* Chạy script đồng bộ toàn bộ URL ảnh legacy trên Neon DB sang Cloudinary.
2. **Lỗi 403 Forbidden phân quyền Cashier — ✅ RESOLVED (`BUG-01`):**
   - *Chi tiết:* `GET /api/v1/analytics/orders/history` và `GET /api/v1/analytics/orders/summary` -> `HTTP 403` (trong Initial Run).
   - *Phân tích & Khắc phục:* Đã mở quyền RBAC cho `CASHIER` tại `SecurityConfig.java:95-96` và `AnalyticsController.java`, xác nhận PASS ở lần chạy kiểm thử lại (xem `reports/BUG_REPORT.md` — `BUG-01`).
3. **Lỗi 429 Too Many Requests (Rate Limiter 2FA) — ✅ RESOLVED (`BUG-05`):**
   - *Chi tiết:* `POST /api/v1/auth/verify-2fa` -> `HTTP 429`
   - *Phân tích & Khắc phục:* Cơ chế chống brute-force của hệ thống hoạt động chính xác; đồng thời đã bổ sung giao diện đếm ngược `rateLimitCooldown` tại `LoginPage.jsx` hiển thị `THỬ LẠI SAU (Xs)` từ `retryAfterSeconds` (xem `reports/BUG_REPORT.md` — `BUG-05`).
