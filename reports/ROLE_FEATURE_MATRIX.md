# ROLE-FEATURE PERMISSION MATRIX (MA TRẬN PHÂN QUYỀN 6 VAI TRÒ)
**Hệ thống: QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa vai trò**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày kiểm tra: 2026-11-14*
*Cơ sở pháp lý & Kỹ thuật: Bóc tách trực tiếp từ `frontend/src/app/router.jsx`, `AdminSidebar.jsx` và `backend/.../UserRole.java`*

---

## 1. ĐỊNH NGHĨA 6 VAI TRÒ NGƯỜI DÙNG (6 SYSTEM ROLES)

| Vai trò (Role) | Mã định danh hệ thống | Phương thức xác thực & Đăng nhập | Đối tượng người dùng |
|---|---|---|---|
| **Customer** | `CUSTOMER` (Guest) | Quét QR bàn ăn (`/:tableCode/menu`) hoặc xem công khai (`/menu`) | Khách hàng dùng bữa tại nhà hàng |
| **Staff** | `STAFF` | JWT Authentication Form Login (`/login`) | Nhân viên phục vụ bàn, chạy bàn |
| **Chef** | `CHEF` | JWT Authentication Form Login (`/login`) | Đầu bếp, nhân viên chế biến tại bếp |
| **Cashier** | `CASHIER` | JWT Authentication Form Login (`/login`) | Thu ngân, nhân viên xử lý hóa đơn & thanh toán |
| **Manager** | `MANAGER` | JWT Authentication Form Login (`/login`) | Quản lý nhà hàng, điều phối vận hành |
| **Admin** | `ADMIN` | JWT Authentication + 2FA OTP (`/login`) | Chủ nhà hàng, quản trị viên tối cao |

---

## 2. BẢNG MA TRẬN PHÂN QUYỀN TÍNH NĂNG CHI TIẾT (ROLE-FEATURE MATRIX)

**Ký hiệu quy ước:**
- `✓` : Toàn quyền sử dụng (Xem, Thao tác, Thêm/Sửa/Xóa).
- `👁️` : Quyền chỉ xem (Read-only / Restricted access).
- `-` : Bị từ chối truy cập (Bảo vệ bởi `ProtectedRoute`, trả về 403 Forbidden nếu cố tình truy cập).

---

### Phân hệ 1: Trải nghiệm Đặt món của Khách hàng (Customer QR Ordering)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Xem Thực đơn công khai** | `/menu` | `✓` | `✓` | `✓` | `✓` | `✓` | `✓` | Public API, cache HTTP NetworkFirst |
| **Khởi tạo Session bàn qua QR** | `/:tableCode/menu` | `✓` | `✓` | - | `✓` | `✓` | `✓` | Yêu cầu mã bàn hợp lệ (`T01`, `T02`...) |
| **Xem chi tiết & Tùy chọn món** | Modal `ItemOptionsModal` | `✓` | `✓` | - | `✓` | `✓` | `✓` | Chọn Topping, độ ngọt/cay, ghi chú |
| **Quản lý Giỏ hàng của bàn** | Modal `CartModal` | `✓` | `✓` | - | `✓` | `✓` | `✓` | Lưu trữ Client-side kết hợp Session |
| **Gửi đơn đặt món (Submit Order)**| API `/api/v1/public/orders` | `✓` | `✓` | - | `✓` | `✓` | `✓` | Tự động bắn thông báo STOMP xuống Bếp |
| **Theo dõi tiến độ món ăn** | Sheet `CurrentOrderSheet` | `✓` | `✓` | - | `✓` | `✓` | `✓` | Cập nhật Realtime trạng thái món |
| **Chat tư vấn với Trợ lý AI** | Widget `AiChatAssistant` | `✓` | `✓` | - | `✓` | `✓` | `✓` | Tích hợp Google Gemini Flash AI |

---

### Phân hệ 2: Quản lý Phòng bàn & Vận hành Phục vụ (Dining Table Operations)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Xem Sơ đồ bàn ăn trực quan** | `/admin/tables` | - | `✓` | - | `✓` | `✓` | `✓` | Lưới trạng thái bàn Realtime |
| **Xem chi tiết đơn tại bàn** | Modal `OrderDetailModal` | - | `✓` | - | `✓` | `✓` | `✓` | Danh sách món ăn đang phục vụ |
| **Gọi thêm món tại bàn** | Modal `AddItemModal` | - | `✓` | - | `✓` | `✓` | `✓` | Nhân viên thêm món hộ khách |
| **Gộp bàn ăn (Merge Tables)** | Modal `MergeTableModal` | - | `✓` | - | `✓` | `✓` | `✓` | Hỗ trợ ghép nhiều bàn vào 1 đơn |
| **Hủy gộp bàn (Unmerge)** | `/admin/tables` | - | `✓` | - | `✓` | `✓` | `✓` | Tách các bàn ghép về trạng thái ban đầu |
| **Thêm / Sửa / Xóa bàn ăn** | Modal `TableFormModal` | - | - | - | - | `✓` | `✓` | Chỉ Manager & Admin được sửa cơ cấu bàn |
| **Tạo lại mã QR của bàn** | API `regenerate-qr` | - | - | - | - | `✓` | `✓` | Sinh lại mã QR động lưu Cloudinary |
| **Quản lý Đơn mang về** | `/admin/takeaway` | - | `✓` | - | `✓` | `✓` | `✓` | Xử lý các đơn không ngồi tại bàn |

---

### Phân hệ 3: Màn hình Bếp (KDS) & Quản lý Kho (Kitchen & Inventory)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Màn hình Bếp (KDS Kanban)** | `/admin/kitchen` | - | - | `✓` | - | `✓` | `✓` | Realtime STOMP topic `/topic/kitchen` |
| **Chuyển chế độ Nấu gom món** | `/admin/kitchen` (Batch) | - | - | `✓` | - | `✓` | `✓` | Gom các món giống nhau để nấu cùng lúc |
| **Cập nhật trạng thái món ăn** | `/admin/kitchen` | - | - | `✓` | - | `✓` | `✓` | Chờ nấu ➔ Đang nấu ➔ Đã hoàn thành |
| **Xem tồn kho nguyên liệu** | `/admin/inventory` | - | - | `✓` | - | `✓` | `✓` | Bảng theo dõi nguyên liệu & cảnh báo thiếu |
| **Thêm mới / Sửa nguyên liệu** | Modal `InventoryModal` | - | - | - | - | `✓` | `✓` | Cấu hình đơn vị tính và định mức |
| **Nhập kho / Xuất kho** | Modal `StockModal` | - | - | `✓` | - | `✓` | `✓` | Bếp & Quản lý được phép ghi nhận số lượng |
| **Cấu hình Định lượng món** | Modal `RecipeModal` | - | - | - | - | `✓` | `✓` | Liên kết món ăn với nguyên liệu trừ kho |

---

### Phân hệ 4: Thu ngân & Thanh toán (Cashier & Payment)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Lập phiếu & Mở Modal thanh toán**| Modal `PaymentModal` | - | - | - | `✓` | `✓` | `✓` | Tính tổng tiền, thuế, giảm giá |
| **Thanh toán Tiền mặt (Cash)** | Modal `PaymentModal` | - | - | - | `✓` | `✓` | `✓` | Tính tiền thối tự động cho khách |
| **Thanh toán Chuyển khoản VietQR**| Modal `PaymentModal` | - | - | - | `✓` | `✓` | `✓` | Tích hợp PayOS Webhook tự động khớp lệnh |
| **Áp dụng Mã giảm giá (Voucher)** | Modal `PaymentModal` | - | - | - | `✓` | `✓` | `✓` | Kiểm tra điều kiện & trừ tiền hóa đơn |
| **In hóa đơn / Xuất hóa đơn** | Modal `PaymentModal` | - | - | - | `✓` | `✓` | `✓` | Tạo mẫu in bill nhiệt |
| **Tra cứu Lịch sử hóa đơn** | `/admin/history` | - | - | - | `✓` | `✓` | `✓` | Lọc theo ngày, trạng thái, mã đơn |

---

### Phân hệ 5: Thực đơn, Danh mục & Khuyến mãi (Menu & Promotion Management)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Quản lý Danh mục món ăn** | `/admin/categories` | - | - | - | - | `✓` | `✓` | Thêm, sửa, xóa, tải ảnh danh mục |
| **Quản lý Món ăn (CRUD Menu)** | `/admin/menu` | - | - | - | - | `✓` | `✓` | Tạo món mới, giá, ảnh, topping, ẩn/hiện |
| **Quản lý Gói Combo** | `/admin/combo` | - | - | - | - | `✓` | `✓` | Thiết lập các combo món ăn ưu đãi |
| **Quản lý Voucher Khuyến mãi** | `/admin/voucher` | - | - | - | - | `✓` | `✓` | Tạo mã giảm giá theo %, tiền cố định |

---

### Phân hệ 6: Quản trị Nhân sự & Phân quyền Hệ thống (User & RBAC Administration)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Xem danh sách nhân viên** | `/admin/staffs` | - | - | - | - | `✓` | `✓` | Bảng nhân sự, trạng thái tài khoản |
| **Tạo tài khoản nhân viên mới**| Modal `UserModal` | - | - | - | - | `✓` | `✓` | Phân vai trò: Staff, Chef, Cashier, Manager |
| **Khóa / Mở khóa tài khoản** | `/admin/staffs` | - | - | - | - | `✓` | `✓` | Vô hiệu hóa quyền truy cập ngay lập tức |
| **Buộc đổi mật khẩu lần đầu** | Modal `UserModal` | - | - | - | - | `✓` | `✓` | Cờ `mustChangePassword` bắt buộc đổi pass |
| **Phân quyền vai trò ADMIN** | `/admin/staffs` | - | - | - | - | - | `✓` | **Chỉ Admin** được gán quyền Admin khác |

---

### Phân hệ 7: Báo cáo Thống kê & Phân tích Doanh thu (Analytics & Reporting)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Dashboard Tổng quan** | `/admin/dashboard` | - | - | - | - | `✓` | `✓` | KPI doanh số, đơn hàng, AI insights |
| **Báo cáo Món bán chạy nhất** | `/admin/statistics/top-dishes` | - | - | - | - | `✓` | `✓` | Xếp hạng số lượng & doanh thu theo món |
| **Báo cáo Hiệu suất nhân viên** | `/admin/statistics/staff` | - | - | - | - | `✓` | `✓` | Thống kê số bàn & đơn nhân viên phục vụ |
| **Phân tích Doanh thu chuyên sâu**| `/admin/statistics/revenue` | - | - | - | - | - | **`✓`** | **Độc quyền cho ADMIN** (Biểu đồ doanh thu chi tiết) |

---

### Phân hệ 8: Cài đặt Hệ thống & Tài khoản Cá nhân (Settings & Profile)

| Tính năng chi tiết | Route URL liên quan | Customer | Staff | Chef | Cashier | Manager | Admin | Ghi chú kỹ thuật |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **Xem & Cập nhật Hồ sơ cá nhân** | `/admin/profile` | - | `✓` | `✓` | `✓` | `✓` | `✓` | Đổi tên, số điện thoại, tải ảnh đại diện |
| **Đổi mật khẩu tài khoản** | `/admin/profile` | - | `✓` | `✓` | `✓` | `✓` | `✓` | Xác thực mật khẩu cũ trước khi đổi |
| **Cài đặt Tùy chọn giao diện** | `/admin/settings` | - | `✓` | `✓` | `✓` | `✓` | `✓` | Ngôn ngữ, giao diện sáng/tối (Dark/Light) |
| **Cấu hình Thông tin Nhà hàng** | `/admin/settings` | - | - | - | - | - | **`✓`** | **Chỉ Admin** được sửa thông tin pháp lý nhà hàng |
| **Cấu hình Ngưỡng trễ & Âm thanh** | `/admin/settings` | - | - | - | - | - | **`✓`** | **Chỉ Admin** được cấu hình tham số hệ thống |
