# FEATURE INVENTORY & ARCHITECTURAL MAPPING
**Dự án: QROS - QR Ordering Platform**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày khảo sát ban đầu: 2026-10-30 | Cập nhật đồng bộ 6-role RBAC & 2FA: 2026-11-14*
*Chuyên viên đánh giá: Senior QA Engineer, Performance Engineer & Software Architect*

---

## I. TỔNG QUAN HỆ THỐNG VÀ KIẾN TRÚC KỸ THUẬT

### 1. Stack công nghệ

| Thành phần | Công nghệ / Framework | Phiên bản | Vai trò & Đặc điểm kỹ thuật |
|---|---|---|---|
| **Backend Runtime** | Java OpenJDK | 21.0.12.1 (LTS) | Virtual Threads ready, high performance |
| **Backend Framework** | Spring Boot | 3.5.14 | REST API, Spring Data JPA, Spring Security 6 (6-role RBAC: `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF`, `CUSTOMER`), Spring WebSocket (STOMP) |
| **Database Migration** | Flyway | PostgreSQL Edition | `V1` đến `V1.21` (22 versioned SQL migrations) |
| **Primary Database** | PostgreSQL | 16+ (AWS Neon Serverless) | Connection pool HikariCP (max: 10, min-idle: 2, timeout: 60s) |
| **Distributed Cache / Storage** | Redis / Caffeine | Redis 7 Alpine / Caffeine | Cache type cấu hình: `caffeine` (local default), `redis` (cloud distributed) |
| **Frontend Framework** | React + Vite | React 19.2.0, Vite 7.2.4 | Single Page Application (SPA), PWA enabled, Capacitor Android |
| **State & Data Fetching** | TanStack React Query + Zustand | Query v5.101, Zustand v5.0.9 | Server-state caching, optimistic updates, centralized stores |
| **Realtime Messaging** | Spring STOMP over WebSocket + SockJS | @stomp/stompjs 7.2.1 | Bi-directional invalidation signals, 10s heartbeats, RBAC topic guard |
| **Payment Gateway** | PayOS SDK (VietQR) + Cash | payos-java 2.0.1 | Chuyển khoản QR động qua Open Banking Napas247 & Tiền mặt tại quầy |
| **External Integrations** | Cloudinary, Google Gemini AI, Gmail SMTP | SDK HTTP 4.4, Gemini 2.5 Flash | Quản lý ảnh cloud, AI tư vấn món ăn, gửi OTP/mail thông báo |

---

## II. MA TRẬN TÍNH NĂNG TOÀN DIỆN (FEATURE INVENTORY)

Dưới đây là danh mục kiểm kê chi tiết 100% các chức năng, **135 API endpoints** (đã bổ sung 2FA cho `ADMIN`), phân quyền **6 vai trò RBAC** (`ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF`, `CUSTOMER` — với RoleHierarchy `ADMIN > MANAGER > CASHIER/STAFF/CHEF`) và bảng dữ liệu liên quan trong toàn bộ hệ thống QROS:

| STT | Module | Chức năng chi tiết | HTTP Method & API Endpoint | Phân quyền (RBAC) | Bảng Database liên quan | Mức độ quan trọng |
|:---:|:---|:---|:---|:---|:---|:---:|
| **1** | **Authentication** | Đăng nhập tài khoản quản trị/nhân viên | `POST /api/v1/auth/login` | Public (Rate-limited: 10 req/min/IP) | `users`, Redis rate-limit | **CRITICAL** |
| **2** | **Authentication** | Xác thực mã OTP 2FA cho tài khoản Admin | `POST /api/v1/auth/verify-2fa` | Public (Rate-limited + 2FA Ticket) | `users`, Redis `qros:2fa:ticket:*` | **CRITICAL** |
| **3** | **Authentication** | Gửi lại mã OTP 2FA qua Email | `POST /api/v1/auth/resend-2fa` | Public (Rate-limited + 2FA Ticket) | `users`, Redis `qros:2fa:ticket:*`, SMTP | HIGH |
| **4** | **Authentication** | Làm mới Access Token qua Refresh Cookie | `POST /api/v1/auth/refresh` | Public + Origin Guard | `users`, RefreshTokenStore (Redis) | **CRITICAL** |
| **5** | **Authentication** | Đăng xuất và thu hồi Refresh Token | `POST /api/v1/auth/logout` | Public + Origin Guard | RefreshTokenStore (Redis) | HIGH |
| **6** | **Authentication** | Yêu cầu khôi phục mật khẩu qua Email | `POST /api/v1/auth/forgot-password-email` | Public (Rate-limited: 3 req/15m) | `users`, `password_reset_tokens` | HIGH |
| **7** | **Authentication** | Đặt lại mật khẩu với Reset Token (Email) | `POST /api/v1/auth/reset-password-email` | Public (Rate-limited: 5 req/15m) | `users`, `password_reset_tokens` | HIGH |
| **8** | **Authentication** | Yêu cầu khôi phục mật khẩu qua SMS OTP | `POST /api/v1/auth/forgot-password-phone` | Public (Rate-limited: 3 req/15m) | `users`, `password_reset_tokens` | MEDIUM |
| **9** | **Authentication** | Đặt lại mật khẩu với OTP (SMS Phone) | `POST /api/v1/auth/reset-password-phone` | Public (Rate-limited: 5 req/15m) | `users`, `password_reset_tokens` | MEDIUM |
| **10** | **Profile & User Self** | Lấy thông tin cá nhân người dùng hiện tại | `GET /api/v1/users/me` | Authenticated (Any internal role) | `users` | HIGH |
| **11** | **Profile & User Self** | Cập nhật thông tin cá nhân (Họ tên, SĐT) | `PATCH /api/v1/users/me` | Authenticated (Any internal role) | `users` | HIGH |
| **12** | **Profile & User Self** | Đổi mật khẩu cá nhân | `PATCH /api/v1/users/me/password` | Authenticated (Any internal role) | `users` | **CRITICAL** |
| **13** | **Profile & User Self** | Tải lên ảnh đại diện cá nhân | `POST /api/v1/users/me/avatar` | Authenticated (Any internal role) | `users`, Cloudinary | MEDIUM |
| **14** | **User Management** | Danh sách / Tìm kiếm nhân viên có phân trang | `GET /api/v1/users` | `ADMIN`, `MANAGER` | `users` | HIGH |
| **15** | **User Management** | Chi tiết nhân viên theo ID | `GET /api/v1/users/{id}` | `ADMIN`, `MANAGER` | `users` | HIGH |
| **16** | **User Management** | Tạo tài khoản nhân viên mới (Cashier/Staff/Chef/Manager; chỉ Admin tạo Admin) | `POST /api/v1/users` | `ADMIN`, `MANAGER` | `users` | HIGH |
| **17** | **User Management** | Cập nhật thông tin / Role nhân viên | `PUT /api/v1/users/{id}` | `ADMIN`, `MANAGER` | `users` | HIGH |
| **18** | **User Management** | Reset mật khẩu nhân viên bởi Quản lý | `PATCH /api/v1/users/{id}/reset-password` | `ADMIN`, `MANAGER` | `users` | HIGH |
| **19** | **User Management** | Xóa mềm tài khoản nhân viên | `DELETE /api/v1/users/{id}` | `ADMIN`, `MANAGER` | `users` | HIGH |
| **20** | **User Management** | Tải lên ảnh đại diện cho nhân viên | `POST /api/v1/users/{id}/avatar` | `ADMIN`, `MANAGER` | `users`, Cloudinary | MEDIUM |
| **21** | **Dining Table** | Danh sách bàn ăn và trạng thái sắp xếp | `GET /api/v1/tables` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `tables` | **CRITICAL** |
| **22** | **Dining Table** | Lấy thông tin bàn theo ID | `GET /api/v1/tables/{id}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `tables` | HIGH |
| **23** | **Dining Table** | Lấy thông tin bàn theo mã code bàn | `GET /api/v1/tables/code/{tableCode}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `tables` | HIGH |
| **24** | **Dining Table** | Tạo bàn ăn mới (sinh mã QR động) | `POST /api/v1/tables` | `ADMIN`, `MANAGER` | `tables`, Cloudinary | HIGH |
| **25** | **Dining Table** | Cập nhật thông tin bàn ăn (số bàn, sức chứa) | `PUT /api/v1/tables/{id}` | `ADMIN`, `MANAGER` | `tables` | HIGH |
| **26** | **Dining Table** | Cập nhật trạng thái bàn (AVAILABLE/OCCUPIED) | `PATCH /api/v1/tables/{id}/status` | `ADMIN`, `MANAGER` | `tables` | HIGH |
| **27** | **Dining Table** | Xóa bàn ăn (Soft Delete) | `DELETE /api/v1/tables/{id}` | `ADMIN`, `MANAGER` | `tables` | HIGH |
| **28** | **Dining Table** | Tạo lại mã QR cho bàn ăn | `POST /api/v1/tables/{id}/regenerate-qr` | `ADMIN`, `MANAGER` | `tables`, ZXing, Cloudinary | MEDIUM |
| **29** | **Table Session** | Lấy trạng thái phiên bàn công khai (Khách) | `GET /api/v1/public/tables/{tableCode}/session-state` | Public (`CUSTOMER`) (Rate-limited: 30 req/min) | `table_sessions`, `table_session_tables` | **CRITICAL** |
| **30** | **Table Session** | Khởi tạo phiên bàn QR mới (Khách quét mã) | `POST /api/v1/public/tables/{tableCode}/start-session` | Public (`CUSTOMER`) (Rate-limited: 5 req/min) | `table_sessions`, `table_session_tokens`, `table_session_tables` | **CRITICAL** |
| **31** | **Table Session** | Heartbeat duy trì phiên bàn của khách | `POST /api/v1/public/sessions/heartbeat` | Public (`CUSTOMER`) (Rate-limited: 30 req/min) | `table_sessions`, `table_session_tokens` | HIGH |
| **32** | **Table Session** | Gộp nhiều bàn ăn vào một phiên duy nhất | `POST /api/v1/table-sessions/merge` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `table_sessions`, `table_session_tables`, `tables` | **CRITICAL** |
| **33** | **Table Session** | Hủy gộp bàn (Tách các bàn ghép trở lại) | `POST /api/v1/table-sessions/{sessionId}/cancel-merge` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `table_sessions`, `table_session_tables`, `tables` | **CRITICAL** |
| **34** | **Category** | Danh sách danh mục món ăn đang hoạt động | `GET /api/v1/categories/active` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `category` | HIGH |
| **35** | **Category** | Tìm kiếm / Phân trang danh mục quản trị | `GET /api/v1/categories` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `category` | HIGH |
| **36** | **Category** | Tạo danh mục món ăn mới | `POST /api/v1/categories` | `ADMIN`, `MANAGER` | `category` | HIGH |
| **37** | **Category** | Cập nhật danh mục món ăn | `PUT /api/v1/categories/{id}` | `ADMIN`, `MANAGER` | `category` | HIGH |
| **38** | **Category** | Xóa danh mục món ăn | `DELETE /api/v1/categories/{id}` | `ADMIN`, `MANAGER` | `category`, `menu_item` | HIGH |
| **39** | **Category** | Tải ảnh đại diện danh mục | `POST /api/v1/categories/{id}/image` | `ADMIN`, `MANAGER` | `category`, Cloudinary | MEDIUM |
| **40** | **Menu Item** | Tìm kiếm món ăn có lọc danh mục và phân trang | `GET /api/v1/menu-items` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `menu_item`, `category`, `item_option` | **CRITICAL** |
| **41** | **Menu Item** | Chi tiết món ăn kèm tùy chọn (Options, Values) | `GET /api/v1/menu-items/{id}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `menu_item`, `item_option`, `item_option_values` | **CRITICAL** |
| **42** | **Menu Item** | Tạo món ăn mới kèm danh sách tùy chọn | `POST /api/v1/menu-items` | `ADMIN`, `MANAGER` | `menu_item`, `item_option`, `item_option_values` | HIGH |
| **43** | **Menu Item** | Cập nhật món ăn và danh sách tùy chọn | `PUT /api/v1/menu-items/{id}` | `ADMIN`, `MANAGER` | `menu_item`, `item_option`, `item_option_values` | HIGH |
| **44** | **Menu Item** | Xóa món ăn (Soft delete) | `DELETE /api/v1/menu-items/{id}` | `ADMIN`, `MANAGER` | `menu_item` | HIGH |
| **45** | **Menu Item** | Tải ảnh đại diện món ăn | `POST /api/v1/menu-items/{id}/image` | `ADMIN`, `MANAGER` | `menu_item`, Cloudinary | MEDIUM |
| **46** | **Combo** | Tìm kiếm Combo món ăn quản trị | `GET /api/v1/combos` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `combos`, `combo_items` | HIGH |
| **47** | **Combo** | Danh sách Combo đang hoạt động | `GET /api/v1/combos/active` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `combos`, `combo_items` | HIGH |
| **48** | **Combo** | Chi tiết Combo theo ID | `GET /api/v1/combos/{id}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `combos`, `combo_items`, `menu_item` | HIGH |
| **49** | **Combo** | Tạo Combo mới kèm danh sách món con | `POST /api/v1/combos` | `ADMIN`, `MANAGER` | `combos`, `combo_items` | HIGH |
| **50** | **Combo** | Cập nhật Combo | `PUT /api/v1/combos/{id}` | `ADMIN`, `MANAGER` | `combos`, `combo_items` | HIGH |
| **51** | **Combo** | Bật/tắt trạng thái Active của Combo | `PATCH /api/v1/combos/{id}/toggle-active` | `ADMIN`, `MANAGER` | `combos` | HIGH |
| **52** | **Combo** | Bật/tắt trạng thái Còn món (Available) của Combo | `PATCH /api/v1/combos/{id}/toggle-available` | `ADMIN`, `MANAGER` | `combos` | HIGH |
| **53** | **Combo** | Xóa Combo món ăn | `DELETE /api/v1/combos/{id}` | `ADMIN`, `MANAGER` | `combos`, `combo_items` | HIGH |
| **54** | **Public Menu** | Lấy toàn bộ Catalog công khai (Menu, Combo, Cate) | `GET /api/v1/public/catalog` | Public (`CUSTOMER`) | `category`, `menu_item`, `combos`, `system_settings`, `inventory_items` | **CRITICAL** |
| **55** | **Public Menu** | Lấy danh mục món ăn công khai | `GET /api/v1/public/categories` | Public (`CUSTOMER`) | `category` | HIGH |
| **56** | **Public Menu** | Lấy danh sách món ăn công khai (kèm tồn kho) | `GET /api/v1/public/menu-items` | Public (`CUSTOMER`) | `menu_item`, `inventory_items`, `recipe_items` | **CRITICAL** |
| **57** | **Public Menu** | Lấy danh sách combo công khai | `GET /api/v1/public/combos` | Public (`CUSTOMER`) | `combos`, `combo_items`, `inventory_items` | HIGH |
| **58** | **Public Menu** | Lấy thông tin bàn công khai theo code | `GET /api/v1/public/tables/by-code/{tableCode}` | Public (`CUSTOMER`) | `tables` | HIGH |
| **59** | **Customer Order** | Khách xem trước giá / tính toán Order (Preview) | `POST /api/v1/public/orders/preview` | Public (`CUSTOMER`) | `menu_item`, `combos`, `vouchers`, `promotions` | **CRITICAL** |
| **60** | **Customer Order** | Khách đặt món tại bàn qua QR (Dine-in Order) | `POST /api/v1/public/orders` | Public (`CUSTOMER`) (Rate-limited: 10 req/min) | `orders`, `order_batches`, `order_item`, `order_item_options`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **61** | **Customer Order** | Khách xem đơn hàng hiện tại của bàn mình | `GET /api/v1/public/tables/code/{tableCode}/current-order` | Public (`CUSTOMER`, Requires Session Token) | `orders`, `order_item`, `table_sessions` | **CRITICAL** |
| **62** | **Staff Order** | Nhân viên tạo đơn trực tiếp tại quầy / mang đi | `POST /api/v1/orders` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `orders`, `order_batches`, `order_item`, `inventory_items` | **CRITICAL** |
| **63** | **Staff Order** | Nhân viên xem trước giá đơn hàng (Preview) | `POST /api/v1/orders/preview` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `menu_item`, `combos`, `vouchers` | HIGH |
| **64** | **Staff Order** | Danh sách tất cả đơn hàng có phân trang | `GET /api/v1/orders` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `orders` | HIGH |
| **65** | **Staff Order** | Chi tiết một đơn hàng theo ID | `GET /api/v1/orders/{id}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `orders`, `order_item`, `order_batches`, `payment_transactions` | **CRITICAL** |
| **66** | **Staff Order** | Danh sách đơn hàng đang hoạt động (Active Orders) | `GET /api/v1/orders/active` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `orders`, `tables` | **CRITICAL** |
| **67** | **Staff Order** | Danh sách đơn mang về đang hoạt động | `GET /api/v1/orders/active-takeaway` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `orders` | **CRITICAL** |
| **68** | **Staff Order** | Cập nhật trạng thái đơn (Status Transition) | `PATCH /api/v1/orders/{orderId}/status` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `orders`, `order_status_history`, `tables` | **CRITICAL** |
| **69** | **Staff Order** | Hủy toàn bộ đơn hàng (Cancel Order) | `PATCH /api/v1/orders/{orderId}/cancel` | `ADMIN`, `MANAGER` | `orders`, `order_item`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **70** | **Staff Order** | Xóa vĩnh viễn đơn hàng | `DELETE /api/v1/orders/{orderId}` | `ADMIN`, `MANAGER` | `orders` | MEDIUM |
| **71** | **Table Board** | Lấy sơ đồ bàn kèm trạng thái đơn và tiền | `GET /api/v1/orders/table-board` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `tables`, `orders`, `table_sessions`, `table_session_tables` | **CRITICAL** |
| **72** | **Table Board** | Lấy đơn hiện tại của bàn cụ thể | `GET /api/v1/orders/table/{tableId}/current` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `orders`, `tables` | **CRITICAL** |
| **73** | **Table Board** | Xem trước bill thanh toán của bàn | `GET /api/v1/orders/table/{tableId}/preview` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `orders`, `order_item`, `vouchers`, `promotions`, `system_settings` | **CRITICAL** |
| **74** | **Order Item** | Điều chỉnh món trong đơn (Số lượng, ghi chú) | `PATCH /api/v1/orders/items/{itemId}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `order_item`, `orders`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **75** | **Order Item** | Hủy một món trong đơn (Cancel Item) | `DELETE /api/v1/orders/items/{itemId}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `order_item`, `orders`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **76** | **Kitchen Board** | Bếp lấy danh sách đơn chế biến (Kitchen Orders) | `GET /api/v1/kitchen/orders` | `ADMIN`, `MANAGER`, `CHEF` | `orders`, `order_item`, `order_batches` | **CRITICAL** |
| **77** | **Kitchen Board** | Bếp đánh dấu món đã chuẩn bị xong (Prepared) | `PATCH /api/v1/kitchen/items/{itemId}/prepared` | `ADMIN`, `MANAGER`, `CHEF` | `order_item`, `order_item_status_history` | **CRITICAL** |
| **78** | **Kitchen Board** | Bếp chuyển trạng thái món (PENDING -> COOKING -> READY) | `PATCH /api/v1/kitchen/items/{itemId}/status` | `ADMIN`, `MANAGER`, `CHEF` | `order_item`, `order_item_status_history`, `inventory_items` | **CRITICAL** |
| **79** | **Payment** | Khởi tạo thanh toán (Tiền mặt / VietQR PayOS) | `POST /api/v1/payments` | `ADMIN`, `MANAGER`, `CASHIER` *(không gồm `STAFF`)* | `payment_transactions`, `orders`, `vouchers`, `order_discounts`, `table_sessions`, `tables` | **CRITICAL** |
| **80** | **Payment** | Hủy link thanh toán Online (khách đổi hình thức) | `POST /api/v1/payments/{transactionId}/cancel` | `ADMIN`, `MANAGER`, `CASHIER` | `payment_transactions`, PayOS Gateway | **CRITICAL** |
| **81** | **Payment** | Đồng bộ trạng thái giao dịch thanh toán từ Gateway | `GET /api/v1/payments/{transactionId}` | `ADMIN`, `MANAGER`, `CASHIER` | `payment_transactions`, PayOS Gateway | HIGH |
| **82** | **Payment** | Đối soát và chốt đơn (Reconcile Order) | `POST /api/v1/orders/{orderId}/reconcile` | `ADMIN`, `MANAGER`, `CASHIER` | `orders`, `payment_transactions` | HIGH |
| **83** | **Payment** | Webhook nhận thông báo chuyển khoản thành công từ PayOS | `POST /api/v1/webhooks/payos` | Public (HMAC Verified) | `payment_transactions`, `orders`, `vouchers`, `table_sessions`, `tables` | **CRITICAL** |
| **84** | **Inventory Item** | Tìm kiếm nguyên vật liệu tồn kho có phân trang | `GET /api/v1/inventory/items` | `ADMIN`, `MANAGER`, `CHEF` | `inventory_items` | HIGH |
| **85** | **Inventory Item** | Tổng hợp thống kê tồn kho (Tổng số, Sắp hết, Hết hàng) | `GET /api/v1/inventory/items/summary` | `ADMIN`, `MANAGER`, `CHEF` | `inventory_items` | HIGH |
| **86** | **Inventory Item** | Danh sách nguyên liệu đang hoạt động | `GET /api/v1/inventory/items/active` | `ADMIN`, `MANAGER`, `CHEF` | `inventory_items` | HIGH |
| **87** | **Inventory Item** | Danh sách nguyên liệu chạm ngưỡng cảnh báo | `GET /api/v1/inventory/items/low-stock` | `ADMIN`, `MANAGER`, `CHEF` | `inventory_items` | HIGH |
| **88** | **Inventory Item** | Chi tiết một nguyên liệu tồn kho | `GET /api/v1/inventory/items/{id}` | `ADMIN`, `MANAGER`, `CHEF` | `inventory_items` | HIGH |
| **89** | **Inventory Item** | Thêm mới nguyên vật liệu vào kho | `POST /api/v1/inventory/items` | `ADMIN`, `MANAGER` | `inventory_items` | HIGH |
| **90** | **Inventory Item** | Cập nhật nguyên liệu (Optimistic Locking) | `PUT /api/v1/inventory/items/{id}` | `ADMIN`, `MANAGER` | `inventory_items` | HIGH |
| **91** | **Inventory Item** | Xóa nguyên vật liệu (kiểm tra ràng buộc Recipe) | `DELETE /api/v1/inventory/items/{id}` | `ADMIN`, `MANAGER` | `inventory_items`, `recipe_items` | HIGH |
| **92** | **Inventory Item** | Nhập thêm nguyên liệu vào kho (Stock In) | `POST /api/v1/inventory/items/{id}/stock-in` | `ADMIN`, `MANAGER`, `CHEF` | `inventory_items`, `stock_movements` | **CRITICAL** |
| **93** | **Inventory Item** | Kiểm kê và điều chỉnh số lượng tồn kho (Adjust) | `POST /api/v1/inventory/items/{id}/adjust` | `ADMIN`, `MANAGER`, `CHEF` | `inventory_items`, `stock_movements` | **CRITICAL** |
| **94** | **Stock Movements** | Lịch sử biến động xuất nhập tồn kho | `GET /api/v1/inventory/movements` | `ADMIN`, `MANAGER`, `CHEF` | `stock_movements`, `inventory_items` | HIGH |
| **95** | **Recipes (BOM)** | Lấy định lượng công thức pha chế của món ăn | `GET /api/v1/inventory/recipes/{menuItemId}` | `ADMIN`, `MANAGER`, `CHEF` | `recipe_items`, `inventory_items` | HIGH |
| **96** | **Recipes (BOM)** | Cập nhật công thức định lượng món ăn | `PUT /api/v1/inventory/recipes/{menuItemId}` | `ADMIN`, `MANAGER` | `recipe_items`, `inventory_items` | HIGH |
| **97** | **Recipes (BOM)** | Xóa định lượng của món ăn | `DELETE /api/v1/inventory/recipes/{menuItemId}` | `ADMIN`, `MANAGER` | `recipe_items` | MEDIUM |
| **98** | **Vouchers** | Danh sách Voucher khuyến mãi quản trị | `GET /api/v1/vouchers` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `vouchers` | HIGH |
| **99** | **Vouchers** | Chi tiết Voucher theo ID | `GET /api/v1/vouchers/{id}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `vouchers` | HIGH |
| **100** | **Vouchers** | Tạo mã giảm giá mới | `POST /api/v1/vouchers` | `ADMIN`, `MANAGER` | `vouchers` | HIGH |
| **101** | **Vouchers** | Cập nhật thông tin mã giảm giá | `PUT /api/v1/vouchers/{id}` | `ADMIN`, `MANAGER` | `vouchers` | HIGH |
| **102** | **Vouchers** | Xóa mã giảm giá | `DELETE /api/v1/vouchers/{id}` | `ADMIN`, `MANAGER` | `vouchers` | HIGH |
| **103** | **Vouchers** | Kiểm tra tính hợp lệ của mã Voucher (Validate) | `GET /api/v1/vouchers/validate` | Public | `vouchers` | **CRITICAL** |
| **104** | **Promotions** | Danh sách chương trình khuyến mãi tự động | `GET /api/v1/promotions` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `promotions`, `promotion_days` | HIGH |
| **105** | **Promotions** | Tóm tắt quản lý chương trình khuyến mãi | `GET /api/v1/promotions/management-summary` | `ADMIN`, `MANAGER` | `promotions` | HIGH |
| **106** | **Promotions** | Danh sách khuyến mãi đang áp dụng trong ngày/giờ | `GET /api/v1/promotions/active` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `promotions`, `promotion_days` | HIGH |
| **107** | **Promotions** | Chi tiết chương trình khuyến mãi theo ID | `GET /api/v1/promotions/{id}` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | `promotions`, `promotion_days` | HIGH |
| **108** | **Promotions** | Tạo mới chương trình khuyến mãi | `POST /api/v1/promotions` | `ADMIN`, `MANAGER` | `promotions`, `promotion_days` | HIGH |
| **109** | **Promotions** | Cập nhật chương trình khuyến mãi | `PUT /api/v1/promotions/{id}` | `ADMIN`, `MANAGER` | `promotions`, `promotion_days` | HIGH |
| **110** | **Promotions** | Xóa chương trình khuyến mãi | `DELETE /api/v1/promotions/{id}` | `ADMIN`, `MANAGER` | `promotions` | HIGH |
| **111** | **Analytics** | Báo cáo tổng quan Dashboard (Doanh thu, Đơn, Món) | `GET /api/v1/analytics/dashboard` | `ADMIN`, `MANAGER` | `daily_revenue_summary`, `orders` | **CRITICAL** |
| **112** | **Analytics** | Chuỗi số liệu doanh thu theo mốc thời gian | `GET /api/v1/analytics/revenue` | **`ADMIN` only** | `daily_revenue_summary` | HIGH |
| **113** | **Analytics** | Đánh giá hiệu suất nhân viên phục vụ / thanh toán | `GET /api/v1/analytics/users` | `ADMIN`, `MANAGER` | `orders`, `users` | HIGH |
| **114** | **Analytics** | Chi tiết các đơn hàng cho báo cáo phân tích | `GET /api/v1/analytics/orders` | `ADMIN`, `MANAGER` | `orders` | HIGH |
| **115** | **Analytics** | Bảng xếp hạng món ăn bán chạy nhất (Top Items) | `GET /api/v1/analytics/top-items` | `ADMIN`, `MANAGER` | `daily_item_sales_summary`, `order_item` | HIGH |
| **116** | **Analytics** | Xu hướng bán hàng theo khung giờ | `GET /api/v1/analytics/sales-trend` | `ADMIN`, `MANAGER` | `orders` | HIGH |
| **117** | **Analytics** | Lịch sử đơn hàng lọc đa tiêu chí (Order History) | `GET /api/v1/analytics/orders/history` | `ADMIN`, `MANAGER`, `CASHIER` | `orders`, `tables` | **CRITICAL** |
| **118** | **Analytics** | Tổng kết thống kê số lượng & tiền của đơn hàng | `GET /api/v1/analytics/orders/summary` | `ADMIN`, `MANAGER`, `CASHIER` | `orders` | HIGH |
| **119** | **Forecast** | Dự báo doanh thu các ngày tới | `GET /api/v1/analytics/forecast/revenue` | **`ADMIN` only** | `daily_revenue_summary` | MEDIUM |
| **120** | **Forecast** | Dự báo các món ăn sẽ bán chạy trong tương lai | `GET /api/v1/analytics/forecast/popular-items` | **`ADMIN` only** | `daily_item_sales_summary` | MEDIUM |
| **121** | **Recommendation** | Gợi ý món ăn cá nhân hóa theo ngữ cảnh | `GET /api/v1/recommendations/personalized` | Public | `menu_item`, `daily_item_sales_summary` | HIGH |
| **122** | **Recommendation** | Gợi ý danh sách món ăn phổ biến nhất | `GET /api/v1/recommendations/popular` | Public | `menu_item`, `daily_item_sales_summary` | HIGH |
| **123** | **Recommendation** | Gợi ý bán kèm khi khách chọn món (Cross-sell) | `GET /api/v1/recommendations/cross-sell/{itemId}` | Public | `menu_item`, `order_item` | HIGH |
| **124** | **Recommendation** | Gợi ý liên quan theo món đang xem | `GET /api/v1/recommendations/item/{itemId}` | Public | `menu_item` | HIGH |
| **125** | **AI Assistant** | Khách chat hỏi đáp và nhờ AI tư vấn món ăn | `POST /api/v1/public/ai/chat` | Public (Rate-limited: 10 req/min) | Gemini AI 2.5 Flash | HIGH |
| **126** | **AI Assistant** | Nhân viên tra cứu quy trình nghiệp vụ bằng AI | `POST /api/v1/ai/staff/query` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | Gemini AI 2.5 Flash | MEDIUM |
| **127** | **AI Assistant** | Quản lý sinh mô tả món ăn tự động bằng AI | `POST /api/v1/ai/menu-item/description` | `ADMIN`, `MANAGER` | Gemini AI 2.5 Flash | MEDIUM |
| **128** | **System Settings** | Lấy cấu hình hệ thống công khai (Tên quán, VAT) | `GET /api/v1/public/settings` | Public | `system_settings` | **CRITICAL** |
| **129** | **System Settings** | Lấy toàn bộ cấu hình hệ thống quản trị | `GET /api/v1/settings` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | `system_settings` | **CRITICAL** |
| **130** | **System Settings** | Cập nhật cấu hình hệ thống (VAT, Giờ mở cửa, Bật/tắt) | `PUT /api/v1/settings` | **`ADMIN` only** | `system_settings` | **CRITICAL** |
| **131** | **System Settings** | Tải lên Logo nhà hàng | `POST /api/v1/settings/logo` | **`ADMIN` only** | `system_settings`, Cloudinary | MEDIUM |
| **132** | **System Health** | Kiểm tra trạng thái hoạt động (Health check) | `GET /actuator/health` | Public | Spring Actuator | **CRITICAL** |
| **133** | **System Metrics** | Xuất số liệu giám sát Prometheus Metrics | `GET /actuator/prometheus` | **`ADMIN` only** | Micrometer Prometheus | HIGH |
| **134** | **Real-time STOMP** | WebSocket Endpoint kết nối Realtime | `WS /ws` (SockJS + STOMP) | Handshake Auth + Topic RBAC (`RoleHierarchy`) | In-Memory Message Broker | **CRITICAL** |
| **135** | **Error Fallback** | Endpoint bắt lỗi chung của hệ thống | `ANY /error` | Public | ApiErrorController | HIGH |

---

## III. MỐI QUAN HỆ GIỮA CÁC MODULE VÀ LUỒNG DỮ LIỆU CỐT LÕI

1. **Luồng Khách hàng quét mã QR -> Xem Menu -> Đặt món (Dine-in Order Flow):**
   - Khách quét mã QR tại bàn -> Trình duyệt mở `/:tableCode/menu`.
   - Frontend gọi `GET /api/v1/public/tables/{tableCode}/session-state` và `POST /api/v1/public/tables/{tableCode}/start-session`.
   - Backend sinh `TableSession` và cấp phát `sessionToken` lưu trong cookie/localStorage của khách.
   - Frontend tải Menu qua `GET /api/v1/public/catalog` (kèm dữ liệu tồn kho từ `InventoryAvailabilityService`).
   - Khách chọn món và gửi `POST /api/v1/public/orders` kèm `tableCode`, `sessionToken`, `items`, `options`.
   - Backend chạy Transaction:
     - Khóa bàn và phiên bàn (`findActiveOrderLocksByTableSessionIdForUpdate`).
     - Gom công thức định lượng (Recipe) từ `recipe_items`.
     - Khóa và trừ tồn kho dự giữ (`order_item_inventory_reservations` + `inventory_items.reserved_quantity`).
     - Tạo `orders`, `order_batches`, `order_item`, `order_item_options`.
     - Bắn WebSocket event `/topic/orders` và `/topic/kitchen`.
     - Frontend Quản lý, Phục vụ và Bếp lập tức nhận tín hiệu và cập nhật giao diện trong thời gian thực.

2. **Luồng Bếp chế biến món (Kitchen Workflow):**
   - Bếp kết nối `/ws` và lắng nghe `/topic/kitchen`.
   - Bếp bấm "Chế biến" -> `PATCH /api/v1/kitchen/items/{itemId}/status` (chuyển sang `COOKING`).
   - Bếp bấm "Hoàn thành" -> `PATCH /api/v1/kitchen/items/{itemId}/prepared` (chuyển sang `FINISHED`).
   - Backend giải phóng `reserved_quantity` và thực xuất nguyên liệu khỏi kho qua `StockMovement` (loại `RECIPE_CONSUMPTION`).
   - Bắn WebSocket event `/topic/orders` để phục vụ mang món ra bàn.

3. **Luồng Gộp bàn (Merged Table Sessions):**
   - Khách đi đông ngồi nhiều bàn (ví dụ Bàn 1 + Bàn 2 + Bàn 3).
   - Phục vụ chọn Bàn 1 (bàn chính) và tích chọn Bàn 2, Bàn 3 -> Gọi `POST /api/v1/table-sessions/merge`.
   - Backend sắp xếp ID `[1, 2, 3]` và khóa bi-directional bằng `findAllByIdInForUpdate`.
   - Liên kết các bàn phụ vào cùng một `TableSession` chính thông qua bảng trung gian `table_session_tables`.
   - Tất cả các đơn hàng sau đó của cả 3 bàn đều dùng chung một Order và một Bill thanh toán duy nhất!

4. **Luồng Thanh toán và Đối soát (Payment & Settlement Flow):**
   - Khách yêu cầu tính tiền -> Thu ngân (`CASHIER`) / Quản lý (`MANAGER`) / `ADMIN` mở `PaymentModal` trên `TableGrid` *(nhân viên phục vụ `STAFF` không có quyền thu tiền)*.
   - Thu ngân chọn Tiền mặt hoặc Quét mã VietQR (PayOS).
   - Backend tạo `PaymentTransaction` với `idempotencyKey`.
   - Nếu là PayOS: Sinh mã QR động Napas247 chứa số tiền chính xác và nội dung chuyển khoản.
   - Khi khách chuyển khoản thành công, PayOS gửi Webhook đến `POST /api/v1/webhooks/payos` với chữ ký HMAC.
   - Backend xác thực chữ ký, chốt đơn `orders.payment_status = 'PAID'`, cập nhật doanh thu `daily_revenue_summary`, đóng `table_sessions`, chuyển trạng thái bàn về `AVAILABLE`, bắn WebSocket `/topic/orders` và `/topic/tables`.
