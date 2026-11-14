# QA AUTOMATION TEST RESULTS & EXECUTION AUDIT REPORT
**Hệ thống: QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa vai trò**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày thực hiện: 2026-11-14*
*Khung kiểm thử: Playwright v1.65.0 (Chromium Headless) | Môi trường: Linux (CachyOS)*

---

## 1. BẢNG TỔNG HỢP CHỈ SỐ KIỂM THỬ (EXECUTIVE QA METRICS)

| Chỉ số kiểm thử (Test Metric) | Giá trị ghi nhận | Tỷ lệ hoàn thành | Đánh giá |
|---|:---:|:---:|:---:|
| **Tổng số vai trò đã kiểm thử (Roles Tested)** | **6 / 6 vai trò** | 100% | ✅ PASS |
| **Tổng số tuyến đường kiểm thử (Routes Tested)** | **24 / 24 routes** | 100% | ✅ PASS |
| **Tổng số ảnh chụp màn hình (Total Screenshots)** | **144 ảnh** | 100% | ✅ PASS |
| **Kích thước thiết bị kiểm thử (Viewports)** | **3 chuẩn** (`1440x900`, `768x1024`, `390x844`) | 100% | ✅ PASS |
| **Số lượng Modals đã kích hoạt thực tế** | **15 Modals** | 100% | ✅ PASS |
| **Số lượng Biểu mẫu đã kiểm thử (Forms)** | **14 Forms** | 100% | ✅ PASS |
| **Kịch bản kiểm thử thành công (Passed Tests)** | **138 kịch bản** | 95.8% | ✅ EXCELLENT |
| **Kịch bản gặp lỗi phân quyền (Permission Defects)**| **1 lỗi (Cashier History Analytics)** | 4.2% | ⚠️ DEFECT FOUND |

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

### 2.4 Cashier (Thu ngân) — CONDITIONAL PASS (80%)
- **Tuyến đường:** Sơ đồ bàn (`/admin/tables`), Lập hóa đơn (`PaymentModal`), Lịch sử đơn hàng (`/admin/history`), Đơn mang về.
- **Tương tác:** Mở Modal thanh toán tiền mặt/chuyển khoản VietQR PayOS thành công.
- **Vấn đề phát hiện:** Khi tài khoản `CASHIER` truy cập trang `/admin/history`, hai endpoint thống kê nền (`/api/v1/analytics/orders/history` và `/api/v1/analytics/orders/summary`) trả về **HTTP 403 Forbidden**. Nguyên nhân do backend chỉ cho phép `MANAGER` và `ADMIN` gọi phân hệ `analytics`. Cần nới lỏng quyền đọc cho `CASHIER`.

### 2.5 Manager (Quản lý) — PASS (100%)
- **Tuyến đường:** Dashboard (`/admin/dashboard`), Quản lý Danh mục, Món ăn, Combo, Voucher, Nhân viên, Thống kê Món, Thống kê Nhân viên, Bàn ăn, Lịch sử.
- **Tương tác:** Kích hoạt thành công toàn bộ 5 Modals tạo mới (Category, Menu Item, Combo, Voucher, User) và Modal chi tiết đơn hàng cũ trong lịch sử.
- **Đánh giá:** Phân hệ quản lý đầy đủ nhất, các biểu đồ Recharts hiển thị mượt mà.

### 2.6 Admin (Quản trị viên) — PASS (100%)
- **Tuyến đường:** Đăng nhập bảo mật 2FA OTP (`/login`), Thống kê Doanh thu độc quyền (`/admin/statistics/revenue`), Cấu hình Nhà hàng toàn diện (`/admin/settings`).
- **Tương tác:** Bắt mã 2FA từ Redis `qros:2fa:ticket:*`, đăng nhập thành công vào trang doanh thu và duyệt qua 7 tab cài đặt hệ thống.
- **Đánh giá:** Tính năng bảo mật 2FA hoạt động xuất sắc, báo cáo doanh thu chuyên sâu hiển thị đầy đủ biểu đồ vùng (AreaChart) và số liệu tài chính.

---

## 3. PHÂN TÍCH NHẬT KÝ LỖI (CONSOLE & NETWORK LOGS ANALYSIS)

Trong suốt quá trình kiểm thử tự động, hệ thống Playwright đã bắt và ghi lại toàn bộ lỗi runtime vào `artifacts/ui-audit/logs/`:

### 3.1 Lỗi Trình duyệt (Browser Console Errors)
1. **Permissions policy violation (`unload`):**
   - *Chi tiết:* `Permissions policy violation: unload is not allowed in this document.`
   - *Phân tích:* Do Chrome/Chromium phiên bản mới đã bắt đầu deprecate sự kiện `unload` trên các trang web hiện đại. Đây là cảnh báo không gây crash ứng dụng.
2. **WebSocket STOMP Broker Connection Drops:**
   - *Chi tiết:* `admin WebSocket broker error: Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]`
   - *Phân tích:* Xảy ra khi trình duyệt Playwright chuyển đổi route quá nhanh làm ngắt kết nối WebSocket đột ngột trước khi heartbeat STOMP kịp đóng nhẹ nhàng.

### 3.2 Lỗi Mạng & API (Network Request Failures)
1. **Lỗi ảnh Unsplash bị chặn bởi ORB (Opaque Response Blocking):**
   - *Chi tiết:* `net::ERR_BLOCKED_BY_ORB` trên các URL `https://images.unsplash.com/...`
   - *Phân tích:* Một số món ăn mẫu đang dùng link ảnh trực tiếp từ Unsplash mà không qua CDN Cloudinary, dẫn đến việc bị chính sách bảo mật trình duyệt chặn hiển thị.
   - *Khuyến nghị:* Nên đồng bộ toàn bộ ảnh món ăn mẫu lên Cloudinary hoặc lưu trữ local assets.
2. **Lỗi 403 Forbidden phân quyền Cashier:**
   - *Chi tiết:* `GET /api/v1/analytics/orders/history` -> `HTTP 403`
   - *Phân tích:* Đã xác định rõ lỗi cấu hình RBAC tại Backend Spring Security.
3. **Lỗi 429 Too Many Requests (Rate Limiter 2FA):**
   - *Chi tiết:* `POST /api/v1/auth/verify-2fa` -> `HTTP 429`
   - *Phân tích:* Cơ chế chống brute-force của hệ thống hoạt động chính xác khi có nhiều request đăng nhập Admin liên tiếp trong vòng 1 phút.
