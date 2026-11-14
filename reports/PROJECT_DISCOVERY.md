# PROJECT DISCOVERY & UI/UX COMPONENT INVENTORY
**Hệ thống: QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa vai trò**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày khảo sát: 2026-11-14*
*Đơn vị thực hiện: Senior QA Automation Lead & Frontend Architect*

---

## 1. TỔNG QUAN KIẾN TRÚC HỆ THỐNG (SYSTEM ARCHITECTURE)

QROS là hệ thống quản lý nhà hàng và đặt món thông minh hoạt động theo mô hình Client-Server hiện đại, tích hợp giao tiếp thời gian thực (Realtime WebSocket) và Progressive Web App (PWA).

### 1.1 Stack công nghệ (Technology Stack)
- **Frontend Core:** React 19.2.0, Vite 7.2.4 (Single Page Application - SPA).
- **Styling & Design System:** Tailwind CSS v4, Lucide React icons, React Icons.
- **Routing & Navigation:** React Router DOM v7.12.0 (Hỗ trợ Lazy loading & Suspense, ErrorBoundary đa tầng).
- **State Management & Caching:**
  - TanStack React Query v5.101.0 (Quản lý Server-state, Optimistic updates, Network-first caching).
  - Zustand v5.0.9 (Quản lý Client-state: Preferences, Settings, Cart store).
- **Realtime Communication:** Spring STOMP over WebSocket (`@stomp/stompjs` v7.2.1, `sockjs-client` v1.6.1). Tự động nhận diện tín hiệu để invalidate React Query cache mà không cần polling liên tục.
- **Data Visualization:** Recharts v3.7.0 (Biểu đồ Area, Line, Bar, Pie trực quan).
- **Backend Core:** Java 21 LTS, Spring Boot 3.5.14, Spring Data JPA (Hibernate 6.6), Spring Security 6.
- **Database & Cache:** PostgreSQL 17.11 (AWS Neon Pooler với HikariCP pool), Redis 7 (Alpine).
- **Cổng thanh toán & Tích hợp:** VietQR PayOS (Open Banking), Google Gemini Flash AI (Trợ lý tư vấn món).

---

## 2. TOÀN BỘ CÁC ROUTE TRONG HỆ THỐNG (ROUTE INVENTORY)

Toàn bộ các tuyến đường (routes) được định nghĩa tại `frontend/src/app/router.jsx`:

### 2.1 Tuyến đường công khai & Khách hàng (Public & Customer Routes)
| Route Path | Tên Component | Mục đích nghiệp vụ | Quyền truy cập |
|---|---|---|---|
| `/` | `LandingPage` | Trang đích giới thiệu, hướng dẫn quét mã QR trên bàn | Mọi người (Public) |
| `/:tableCode/menu` | `OrderingPage` (`CustomerMenu`) | Thực đơn đặt món theo mã bàn cụ thể (Session Bàn) | Khách quét mã QR |
| `/menu` | `OrderingPage` (`CustomerMenu`) | Thực đơn xem trước công khai (không gắn bàn) | Mọi người (Public) |
| `/login` | `LoginPage` | Trang đăng nhập dành cho nhân sự (Staff, Chef, Cashier, Manager, Admin) | Public (Guest only) |
| `/forgot-password` | `ForgotPasswordPage` | Yêu cầu khôi phục mật khẩu qua Email | Public (Guest only) |
| `/reset-password` | `ResetPasswordPage` | Đặt lại mật khẩu với token từ link email | Public (Guest only) |
| `/unauthorized` | `UnauthorizedPage` (403) | Màn hình thông báo không có quyền truy cập | Mọi người |
| `/not-found` | `NotFoundPage` (404) | Màn hình thông báo trang không tồn tại | Mọi người |
| `/offline` | `SystemStatusPage` (offline) | Màn hình cảnh báo mất kết nối mạng Internet | Mọi người |
| `/error/:statusCode` | `SystemStatusPage` | Màn hình hiển thị mã lỗi hệ thống (500, 502, 503...) | Mọi người |

### 2.2 Tuyến đường nội bộ quản trị (`/admin/*` - Protected Routes)
Tất cả các route này được bảo vệ bởi `ProtectedRoute` và bọc trong layout chung `AdminLayout`:

| Route Path | Component | Chức năng nghiệp vụ | Vai trò được phép (RBAC) |
|---|---|---|---|
| `/admin/profile` | `ProfilePage` | Xem/sửa thông tin cá nhân, avatar, đổi mật khẩu | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` |
| `/admin/settings` | `SettingsPage` | Cài đặt hệ thống, ngưỡng cảnh báo, tùy chọn cá nhân | `ADMIN` (Toàn quyền), các role khác (Tùy chọn) |
| `/admin/dashboard` | `DashboardPage` | Tổng quan kinh doanh, KPI doanh thu, biểu đồ dự báo | `ADMIN`, `MANAGER` |
| `/admin/categories` | `CategoryPage` | Quản lý danh mục món ăn (CRUD, tải ảnh) | `ADMIN`, `MANAGER` |
| `/admin/menu` | `MenuPage` | Quản lý danh sách món ăn, định giá, tùy chọn topping | `ADMIN`, `MANAGER` |
| `/admin/combo` | `ComboPage` | Quản lý combo gói món ăn ưu đãi | `ADMIN`, `MANAGER` |
| `/admin/voucher` | `VoucherPage` | Quản lý mã giảm giá, khuyến mãi, hạn mức | `ADMIN`, `MANAGER` |
| `/admin/staffs` | `UserPage` | Quản lý danh sách tài khoản nhân viên, đổi vai trò | `ADMIN`, `MANAGER` |
| `/admin/statistics/revenue` | `RevenueStatsPage` | Báo cáo phân tích doanh thu chuyên sâu | **Chỉ `ADMIN`** |
| `/admin/statistics/top-dishes` | `TopDishesStatsPage` | Báo cáo món ăn bán chạy nhất theo kỳ | `ADMIN`, `MANAGER` |
| `/admin/statistics/staff` | `StaffStatsPage` | Báo cáo năng suất phục vụ của từng nhân viên | `ADMIN`, `MANAGER` |
| `/admin/tables` | `TableMapPage` | Sơ đồ bàn ăn trực quan, trạng thái, gọi món, gộp bàn | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` |
| `/admin/takeaway` | `TakeawayPage` | Quản lý đơn hàng mang về (Takeaway) | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` |
| `/admin/history` | `OrderHistoryPage` | Lịch sử tra cứu hóa đơn, chi tiết thanh toán cũ | `ADMIN`, `MANAGER`, `CASHIER` |
| `/admin/kitchen` | `KitchenPage` (`KitchenBoard`) | Bếp KDS: Nhận đơn realtime, chuyển trạng thái nấu | `ADMIN`, `MANAGER`, `CHEF` |
| `/admin/inventory` | `InventoryPage` | Quản lý định mức nguyên liệu kho, nhập/xuất kho | `ADMIN`, `MANAGER`, `CHEF` |

---

## 3. DANH MỤC MODALS & DIALOGS (MODAL INVENTORY)

Toàn bộ các Modal và Dialog trong hệ thống đã được xác minh từ mã nguồn:

| STT | Tên Modal | File mã nguồn | Mô tả & Trạng thái kích hoạt |
|:---:|---|---|---|
| **1** | `ItemOptionsModal` | `frontend/src/shared/ui/modal/ItemOptionsModal.jsx` | Khách/Nhân viên bấm chọn món ăn để chọn topping, mức cay, định lượng và nhập ghi chú. |
| **2** | `CartModal` | `frontend/src/features/customer-menu/ui/CartModal.jsx` | Xem giỏ hàng của khách, điều chỉnh số lượng, xác nhận gửi đơn xuống bếp. |
| **3** | `CurrentOrderSheet` | `frontend/src/features/customer-menu/ui/CurrentOrderSheet.jsx` | Bảng trượt hiển thị tiến độ món ăn đã đặt của bàn (Đang nấu, Đã phục vụ). |
| **4** | `TableFormModal` | `frontend/src/features/table-management/ui/TableFormModal.jsx` | Form thêm mới hoặc chỉnh sửa thông tin bàn ăn (Mã bàn, Sức chứa, Vị trí). |
| **5** | `OrderDetailModal` | `frontend/src/features/table-management/ui/OrderDetailModal.jsx` | Xem danh sách các món ăn đang phục vụ tại một bàn cụ thể. |
| **6** | `AddItemModal` | `frontend/src/features/table-management/ui/AddItemModal.jsx` | Nhân viên/Thu ngân gọi thêm món trực tiếp vào bàn đang mở. |
| **7** | `PaymentModal` | `frontend/src/features/table-management/ui/PaymentModal.jsx` | Thu ngân lập hóa đơn thanh toán (Tiền mặt / Quét mã QR VietQR PayOS, áp Voucher). |
| **8** | `MergeTableModal` | `frontend/src/features/table-management/ui/MergeTableModal.jsx` | Gộp nhiều bàn ăn thành 1 phiên thanh toán chung hoặc tách bàn trở lại. |
| **9** | `CategoryModal` | `frontend/src/features/category-management/ui/CategoryModal.jsx` | Form thêm mới hoặc cập nhật thông tin danh mục, tải ảnh đại diện. |
| **10** | `MenuModal` | `frontend/src/features/menu-management/ui/MenuModal.jsx` | Form tạo/sửa món ăn, gắn danh mục, giá bán, công thức nguyên liệu, topping. |
| **11** | `ComboModal` | `frontend/src/features/combo-management/ui/ComboModal.jsx` | Form tạo/sửa gói combo món ăn, chọn món con và giá ưu đãi. |
| **12** | `VoucherModal` | `frontend/src/features/voucher-management/ui/VoucherModal.jsx` | Form tạo mã giảm giá (Theo % hoặc cố định VND, điều kiện áp dụng). |
| **13** | `UserModal` | `frontend/src/features/user-management/ui/UserModal.jsx` | Form tạo/sửa tài khoản nhân viên, gán vai trò (`ADMIN`, `MANAGER`, `STAFF`...). |
| **14** | `InventoryModal` | `frontend/src/features/inventory-management/ui/modals/InventoryModal.jsx` | Form tạo mới nguyên liệu kho và định mức cảnh báo. |
| **15** | `StockModal` | `frontend/src/features/inventory-management/ui/modals/StockModal.jsx` | Form ghi nhận giao dịch nhập kho hoặc xuất kho nguyên liệu. |
| **16** | `RecipeModal` | `frontend/src/features/inventory-management/ui/modals/RecipeModal.jsx` | Form cấu hình công thức định lượng trừ kho tự động cho từng món ăn. |
| **17** | `GlobalStatusModal` | `frontend/src/shared/ui/GlobalStatusModal.jsx` | Modal thông báo kết quả thao tác thành công / thất bại toàn cục. |
| **18** | `GlobalConfirmModal` | `frontend/src/shared/ui/GlobalConfirmModal.jsx` | Modal xác nhận các hành động nguy hiểm (Hủy bàn, Xóa món, Khóa tài khoản). |

---

## 4. DANH MỤC BIỂU MẪU & DỮ LIỆU ĐẦU VÀO (FORM INVENTORY)

| STT | Biểu mẫu | Vị trí màn hình | Các trường nhập liệu & Quy chuẩn kiểm thử |
|:---:|---|---|---|
| **1** | Form Đăng nhập | `/login` | `email` (Email hợp lệ), `password` (>= 6 ký tự), `otp` (Mã 2FA 6 số cho Admin). |
| **2** | Form Quên mật khẩu | `/forgot-password` | `email` (Kiểm tra rate limit 3 req/15 phút). |
| **3** | Form Đặt lại mật khẩu | `/reset-password` | `newPassword`, `confirmPassword`, token hợp lệ từ link email. |
| **4** | Form Hồ sơ cá nhân | `/admin/profile` | `fullName`, `phone`, tải lên ảnh đại diện `avatarUrl`. |
| **5** | Form Đổi mật khẩu | `/admin/profile` | `currentPassword`, `newPassword`, `confirmPassword` (Bắt buộc khi `mustChangePassword=true`). |
| **6** | Form Cài đặt hệ thống | `/admin/settings` | Tên nhà hàng, thông tin hóa đơn, ngưỡng trễ bếp (phút), âm thanh thông báo. |
| **7** | Form Bàn ăn | `/admin/tables` | Tên/Mã bàn (`tableCode`), Sức chứa ghế ngồi, Vị trí khu vực. |
| **8** | Form Danh mục | `/admin/categories` | Tên danh mục, mô tả, thứ tự hiển thị, tải ảnh đại diện. |
| **9** | Form Món ăn | `/admin/menu` | Tên món, đơn giá, danh mục cha, định lượng, ảnh món, tùy chọn topping. |
| **10** | Form Combo | `/admin/combo` | Tên combo, giá gói, danh sách món con trong combo, thời hạn hiệu lực. |
| **11** | Form Voucher | `/admin/voucher` | Mã voucher, kiểu giảm (`PERCENT`/`FIXED_AMOUNT`), giá trị, đơn tối thiểu, ngày hết hạn. |
| **12** | Form Nhân viên | `/admin/staffs` | Họ tên, email, SĐT, Role (`ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF`). |
| **13** | Form Nhập/Xuất kho | `/admin/inventory` | Loại giao dịch (`IMPORT`/`EXPORT`), nguyên liệu, số lượng, đơn giá, ghi chú. |
| **14** | Form Thanh toán | `/admin/tables` (Modal) | Phương thức (`CASH`/`VIETQR`), tiền khách đưa, voucher áp dụng. |

---

## 5. DANH MỤC CÁC WIDGET & LINH KIỆN TÁI SỬ DỤNG (REUSABLE UI WIDGETS)

1. **Header & Navigation:**
   - `AdminHeader`: Hiển thị thông tin người dùng, nút bật/tắt Theme Dark/Light, danh sách thông báo Realtime.
   - `AdminSidebar`: Sidebar co giãn thông minh theo kích thước màn hình, lọc danh mục theo quyền hạn của tài khoản.
2. **Data & Visualization:**
   - `DataTable`: Bảng hiển thị danh sách có sắp xếp (Sort), phân trang (Pagination), trạng thái rỗng (`EmptyState`), trạng thái tải (`LoadingState`).
   - `AreaChart & LineChart`: Biểu đồ dự báo doanh thu và xu hướng đặt món (Recharts).
   - `PieChart`: Biểu đồ tỷ trọng cơ cấu danh mục bán chạy.
   - `KitchenSummaryCards`: Thẻ tóm tắt số lượng món chờ nấu, đang nấu, quá hạn.
3. **Interactive Components:**
   - `TableGrid & TableCard`: Lưới sơ đồ bàn ăn hiển thị trực quan trạng thái bàn (Trống - xanh lá, Đang ngồi - đỏ cam, Chờ thanh toán - vàng).
   - `KitchenColumn`: Cột trạng thái đơn bếp dạng Kanban kéo/chuyển trạng thái trực quan.
   - `AiChatAssistant`: Hộp thoại trò chuyện nổi (Floating Chatbot) tư vấn chọn món ăn cho khách hàng bằng Gemini Flash.
   - `PullToRefresh`: Bộ điều khiển kéo để làm mới chuyên dụng cho giao diện Mobile/Tablet.
   - `OfflineIndicator`: Thanh cảnh báo nổi khi mất kết nối mạng.
