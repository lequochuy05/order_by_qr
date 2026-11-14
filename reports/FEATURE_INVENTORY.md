# FEATURE INVENTORY & ARCHITECTURAL MAPPING
**Dự án: QROS - QR Ordering Platform**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày khảo sát: 2026-10-30*
*Chuyên viên đánh giá: Senior QA Engineer, Performance Engineer & Software Architect*

---

## I. TỔNG QUAN HỆ THỐNG VÀ KIẾN TRÚC KỸ THUẬT

### 1. Stack công nghệ

| Thành phần | Công nghệ / Framework | Phiên bản | Vai trò & Đặc điểm kỹ thuật |
|---|---|---|---|
| **Backend Runtime** | Java OpenJDK | 21.0.12.1 (LTS) | Virtual Threads ready, high performance |
| **Backend Framework** | Spring Boot | 3.5.14 | REST API, Spring Data JPA, Spring Security 6, Spring WebSocket (STOMP) |
| **Database Migration** | Flyway | PostgreSQL Edition | V1.0 đến V1.18 versioned SQL migrations |
| **Primary Database** | PostgreSQL | 16+ (AWS Neon Serverless) | Connection pool HikariCP (max: 10, min-idle: 2, timeout: 60s) |
| **Distributed Cache / Storage** | Redis / Caffeine | Redis 7 Alpine / Caffeine | Cache type cấu hình: `caffeine` (local default), `redis` (cloud distributed) |
| **Frontend Framework** | React + Vite | React 19.2.0, Vite 7.2.4 | Single Page Application (SPA), PWA enabled, Capacitor Android |
| **State & Data Fetching** | TanStack React Query + Zustand | Query v5.101, Zustand v5.0.9 | Server-state caching, optimistic updates, centralized stores |
| **Realtime Messaging** | Spring STOMP over WebSocket + SockJS | @stomp/stompjs 7.2.1 | Bi-directional invalidation signals, 10s heartbeats, RBAC topic guard |
| **Payment Gateway** | PayOS SDK (VietQR) + Cash | payos-java 2.0.1 | Chuyển khoản QR động qua Open Banking Napas247 & Tiền mặt tại quầy |
| **External Integrations** | Cloudinary, Google Gemini AI, Gmail SMTP | SDK HTTP 4.4, Gemini 2.5 Flash | Quản lý ảnh cloud, AI tư vấn món ăn, gửi OTP/mail thông báo |

---

## II. MA TRẬN TÍNH NĂNG TOÀN DIỆN (FEATURE INVENTORY)

Dưới đây là danh mục kiểm kê chi tiết 100% các chức năng, API endpoints, quyền truy cập và bảng dữ liệu liên quan trong toàn bộ hệ thống QROS:

| STT | Module | Chức năng chi tiết | HTTP Method & API Endpoint | Phân quyền (RBAC) | Bảng Database liên quan | Mức độ quan trọng |
|:---:|:---|:---|:---|:---|:---|:---:|
| **1** | **Authentication** | Đăng nhập tài khoản quản trị/nhân viên | `POST /api/v1/auth/login` | Public (Rate-limited: 10 req/min/IP) | `users`, Redis rate-limit | **CRITICAL** |
| **2** | **Authentication** | Làm mới Access Token qua Refresh Cookie | `POST /api/v1/auth/refresh` | Public + Origin Guard | `users`, RefreshTokenStore (Redis) | **CRITICAL** |
| **3** | **Authentication** | Đăng xuất và thu hồi Refresh Token | `POST /api/v1/auth/logout` | Public + Origin Guard | RefreshTokenStore (Redis) | HIGH |
| **4** | **Authentication** | Yêu cầu khôi phục mật khẩu qua Email | `POST /api/v1/auth/forgot-password-email` | Public (Rate-limited: 3 req/15m) | `users`, `password_reset_tokens` | HIGH |
| **5** | **Authentication** | Đặt lại mật khẩu với Reset Token (Email) | `POST /api/v1/auth/reset-password-email` | Public (Rate-limited: 5 req/15m) | `users`, `password_reset_tokens` | HIGH |
| **6** | **Authentication** | Yêu cầu khôi phục mật khẩu qua SMS OTP | `POST /api/v1/auth/forgot-password-phone` | Public (Rate-limited: 3 req/15m) | `users`, `password_reset_tokens` | MEDIUM |
| **7** | **Authentication** | Đặt lại mật khẩu với OTP (SMS Phone) | `POST /api/v1/auth/reset-password-phone` | Public (Rate-limited: 5 req/15m) | `users`, `password_reset_tokens` | MEDIUM |
| **8** | **Profile & User Self** | Lấy thông tin cá nhân người dùng hiện tại | `GET /api/v1/users/me` | Authenticated (Any role) | `users` | HIGH |
| **9** | **Profile & User Self** | Cập nhật thông tin cá nhân (Họ tên, SĐT) | `PATCH /api/v1/users/me` | Authenticated (Any role) | `users` | HIGH |
| **10** | **Profile & User Self** | Đổi mật khẩu cá nhân | `PATCH /api/v1/users/me/password` | Authenticated (Any role) | `users` | **CRITICAL** |
| **11** | **Profile & User Self** | Tải lên ảnh đại diện cá nhân | `POST /api/v1/users/me/avatar` | Authenticated (Any role) | `users`, Cloudinary | MEDIUM |
| **12** | **User Management** | Danh sách / Tìm kiếm nhân viên có phân trang | `GET /api/v1/users` | `MANAGER` | `users` | HIGH |
| **13** | **User Management** | Chi tiết nhân viên theo ID | `GET /api/v1/users/{id}` | `MANAGER` | `users` | HIGH |
| **14** | **User Management** | Tạo tài khoản nhân viên mới (Staff/Chef) | `POST /api/v1/users` | `MANAGER` | `users` | HIGH |
| **15** | **User Management** | Cập nhật thông tin / Role nhân viên | `PUT /api/v1/users/{id}` | `MANAGER` | `users` | HIGH |
| **16** | **User Management** | Reset mật khẩu nhân viên bởi Quản lý | `PATCH /api/v1/users/{id}/reset-password` | `MANAGER` | `users` | HIGH |
| **17** | **User Management** | Xóa mềm tài khoản nhân viên | `DELETE /api/v1/users/{id}` | `MANAGER` | `users` | HIGH |
| **18** | **User Management** | Tải lên ảnh đại diện cho nhân viên | `POST /api/v1/users/{id}/avatar` | `MANAGER` | `users`, Cloudinary | MEDIUM |
| **19** | **Dining Table** | Danh sách bàn ăn và trạng thái sắp xếp | `GET /api/v1/tables` | `MANAGER`, `STAFF`, `CHEF` | `tables` | **CRITICAL** |
| **20** | **Dining Table** | Lấy thông tin bàn theo ID | `GET /api/v1/tables/{id}` | `MANAGER`, `STAFF`, `CHEF` | `tables` | HIGH |
| **21** | **Dining Table** | Lấy thông tin bàn theo mã code bàn | `GET /api/v1/tables/code/{tableCode}` | `MANAGER`, `STAFF`, `CHEF` | `tables` | HIGH |
| **22** | **Dining Table** | Tạo bàn ăn mới (sinh mã QR động) | `POST /api/v1/tables` | `MANAGER` | `tables`, Cloudinary | HIGH |
| **23** | **Dining Table** | Cập nhật thông tin bàn ăn (số bàn, sức chứa) | `PUT /api/v1/tables/{id}` | `MANAGER` | `tables` | HIGH |
| **24** | **Dining Table** | Cập nhật trạng thái bàn (AVAILABLE/OCCUPIED) | `PATCH /api/v1/tables/{id}/status` | `MANAGER` | `tables` | HIGH |
| **25** | **Dining Table** | Xóa bàn ăn (Soft Delete) | `DELETE /api/v1/tables/{id}` | `MANAGER` | `tables` | HIGH |
| **26** | **Dining Table** | Tạo lại mã QR cho bàn ăn | `POST /api/v1/tables/{id}/regenerate-qr` | `MANAGER` | `tables`, ZXing, Cloudinary | MEDIUM |
| **27** | **Table Session** | Lấy trạng thái phiên bàn công khai (Khách) | `GET /api/v1/public/tables/{tableCode}/session-state` | Public (Rate-limited: 30 req/min) | `table_sessions`, `table_session_tables` | **CRITICAL** |
| **28** | **Table Session** | Khởi tạo phiên bàn QR mới (Khách quét mã) | `POST /api/v1/public/tables/{tableCode}/start-session` | Public (Rate-limited: 5 req/min) | `table_sessions`, `table_session_tokens`, `table_session_tables` | **CRITICAL** |
| **29** | **Table Session** | Heartbeat duy trì phiên bàn của khách | `POST /api/v1/public/sessions/heartbeat` | Public (Rate-limited: 30 req/min) | `table_sessions`, `table_session_tokens` | HIGH |
| **30** | **Table Session** | Gộp nhiều bàn ăn vào một phiên duy nhất | `POST /api/v1/table-sessions/merge` | `MANAGER`, `STAFF` | `table_sessions`, `table_session_tables`, `tables` | **CRITICAL** |
| **31** | **Table Session** | Hủy gộp bàn (Tách các bàn ghép trở lại) | `POST /api/v1/table-sessions/{sessionId}/cancel-merge` | `MANAGER`, `STAFF` | `table_sessions`, `table_session_tables`, `tables` | **CRITICAL** |
| **32** | **Category** | Danh sách danh mục món ăn đang hoạt động | `GET /api/v1/categories/active` | `MANAGER`, `STAFF`, `CHEF` | `category` | HIGH |
| **33** | **Category** | Tìm kiếm / Phân trang danh mục quản trị | `GET /api/v1/categories` | `MANAGER`, `STAFF`, `CHEF` | `category` | HIGH |
| **34** | **Category** | Tạo danh mục món ăn mới | `POST /api/v1/categories` | `MANAGER` | `category` | HIGH |
| **35** | **Category** | Cập nhật danh mục món ăn | `PUT /api/v1/categories/{id}` | `MANAGER` | `category` | HIGH |
| **36** | **Category** | Xóa danh mục món ăn | `DELETE /api/v1/categories/{id}` | `MANAGER` | `category`, `menu_item` | HIGH |
| **37** | **Category** | Tải ảnh đại diện danh mục | `POST /api/v1/categories/{id}/image` | `MANAGER` | `category`, Cloudinary | MEDIUM |
| **38** | **Menu Item** | Tìm kiếm món ăn có lọc danh mục và phân trang | `GET /api/v1/menu-items` | `MANAGER`, `STAFF`, `CHEF` | `menu_item`, `category`, `item_option` | **CRITICAL** |
| **39** | **Menu Item** | Chi tiết món ăn kèm tùy chọn (Options, Values) | `GET /api/v1/menu-items/{id}` | `MANAGER`, `STAFF`, `CHEF` | `menu_item`, `item_option`, `item_option_values` | **CRITICAL** |
| **40** | **Menu Item** | Tạo món ăn mới kèm danh sách tùy chọn | `POST /api/v1/menu-items` | `MANAGER` | `menu_item`, `item_option`, `item_option_values` | HIGH |
| **41** | **Menu Item** | Cập nhật món ăn và danh sách tùy chọn | `PUT /api/v1/menu-items/{id}` | `MANAGER` | `menu_item`, `item_option`, `item_option_values` | HIGH |
| **42** | **Menu Item** | Xóa món ăn (Soft delete) | `DELETE /api/v1/menu-items/{id}` | `MANAGER` | `menu_item` | HIGH |
| **43** | **Menu Item** | Tải ảnh đại diện món ăn | `POST /api/v1/menu-items/{id}/image` | `MANAGER` | `menu_item`, Cloudinary | MEDIUM |
| **44** | **Combo** | Tìm kiếm Combo món ăn quản trị | `GET /api/v1/combos` | `MANAGER`, `STAFF`, `CHEF` | `combos`, `combo_items` | HIGH |
| **45** | **Combo** | Danh sách Combo đang hoạt động | `GET /api/v1/combos/active` | `MANAGER`, `STAFF`, `CHEF` | `combos`, `combo_items` | HIGH |
| **46** | **Combo** | Chi tiết Combo theo ID | `GET /api/v1/combos/{id}` | `MANAGER`, `STAFF`, `CHEF` | `combos`, `combo_items`, `menu_item` | HIGH |
| **47** | **Combo** | Tạo Combo mới kèm danh sách món con | `POST /api/v1/combos` | `MANAGER` | `combos`, `combo_items` | HIGH |
| **48** | **Combo** | Cập nhật Combo | `PUT /api/v1/combos/{id}` | `MANAGER` | `combos`, `combo_items` | HIGH |
| **49** | **Combo** | Bật/tắt trạng thái Active của Combo | `PATCH /api/v1/combos/{id}/toggle-active` | `MANAGER` | `combos` | HIGH |
| **50** | **Combo** | Bật/tắt trạng thái Còn món (Available) của Combo | `PATCH /api/v1/combos/{id}/toggle-available` | `MANAGER` | `combos` | HIGH |
| **51** | **Combo** | Xóa Combo món ăn | `DELETE /api/v1/combos/{id}` | `MANAGER` | `combos`, `combo_items` | HIGH |
| **52** | **Public Menu** | Lấy toàn bộ Catalog công khai (Menu, Combo, Cate) | `GET /api/v1/public/catalog` | Public | `category`, `menu_item`, `combos`, `system_settings`, `inventory_items` | **CRITICAL** |
| **53** | **Public Menu** | Lấy danh mục món ăn công khai | `GET /api/v1/public/categories` | Public | `category` | HIGH |
| **54** | **Public Menu** | Lấy danh sách món ăn công khai (kèm tồn kho) | `GET /api/v1/public/menu-items` | Public | `menu_item`, `inventory_items`, `recipe_items` | **CRITICAL** |
| **55** | **Public Menu** | Lấy danh sách combo công khai | `GET /api/v1/public/combos` | Public | `combos`, `combo_items`, `inventory_items` | HIGH |
| **56** | **Public Menu** | Lấy thông tin bàn công khai theo code | `GET /api/v1/public/tables/by-code/{tableCode}` | Public | `tables` | HIGH |
| **57** | **Customer Order** | Khách xem trước giá / tính toán Order (Preview) | `POST /api/v1/public/orders/preview` | Public | `menu_item`, `combos`, `vouchers`, `promotions` | **CRITICAL** |
| **58** | **Customer Order** | Khách đặt món tại bàn qua QR (Dine-in Order) | `POST /api/v1/public/orders` | Public (Rate-limited: 10 req/min) | `orders`, `order_batches`, `order_item`, `order_item_options`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **59** | **Customer Order** | Khách xem đơn hàng hiện tại của bàn mình | `GET /api/v1/public/tables/code/{tableCode}/current-order` | Public (Requires Session Token) | `orders`, `order_item`, `table_sessions` | **CRITICAL** |
| **60** | **Staff Order** | Nhân viên tạo đơn trực tiếp tại quầy / mang đi | `POST /api/v1/orders` | `MANAGER`, `STAFF` | `orders`, `order_batches`, `order_item`, `inventory_items` | **CRITICAL** |
| **61** | **Staff Order** | Nhân viên xem trước giá đơn hàng (Preview) | `POST /api/v1/orders/preview` | `MANAGER`, `STAFF` | `menu_item`, `combos`, `vouchers` | HIGH |
| **62** | **Staff Order** | Danh sách tất cả đơn hàng có phân trang | `GET /api/v1/orders` | `MANAGER`, `STAFF`, `CHEF` | `orders` | HIGH |
| **63** | **Staff Order** | Chi tiết một đơn hàng theo ID | `GET /api/v1/orders/{id}` | `MANAGER`, `STAFF`, `CHEF` | `orders`, `order_item`, `order_batches`, `payment_transactions` | **CRITICAL** |
| **64** | **Staff Order** | Danh sách đơn hàng đang hoạt động (Active Orders) | `GET /api/v1/orders/active` | `MANAGER`, `STAFF`, `CHEF` | `orders`, `tables` | **CRITICAL** |
| **65** | **Staff Order** | Danh sách đơn mang về đang hoạt động | `GET /api/v1/orders/active-takeaway` | `MANAGER`, `STAFF`, `CHEF` | `orders` | **CRITICAL** |
| **66** | **Staff Order** | Cập nhật trạng thái đơn (Status Transition) | `PATCH /api/v1/orders/{orderId}/status` | `MANAGER`, `STAFF` | `orders`, `order_status_history`, `tables` | **CRITICAL** |
| **67** | **Staff Order** | Hủy toàn bộ đơn hàng (Cancel Order) | `PATCH /api/v1/orders/{orderId}/cancel` | `MANAGER` | `orders`, `order_item`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **68** | **Staff Order** | Xóa vĩnh viễn đơn hàng | `DELETE /api/v1/orders/{orderId}` | `MANAGER` | `orders` | MEDIUM |
| **69** | **Table Board** | Lấy sơ đồ bàn kèm trạng thái đơn và tiền | `GET /api/v1/orders/table-board` | `MANAGER`, `STAFF`, `CHEF` | `tables`, `orders`, `table_sessions`, `table_session_tables` | **CRITICAL** |
| **70** | **Table Board** | Lấy đơn hiện tại của bàn cụ thể | `GET /api/v1/orders/table/{tableId}/current` | `MANAGER`, `STAFF`, `CHEF` | `orders`, `tables` | **CRITICAL** |
| **71** | **Table Board** | Xem trước bill thanh toán của bàn | `GET /api/v1/orders/table/{tableId}/preview` | `MANAGER`, `STAFF`, `CHEF` | `orders`, `order_item`, `vouchers`, `promotions`, `system_settings` | **CRITICAL** |
| **72** | **Order Item** | Điều chỉnh món trong đơn (Số lượng, ghi chú) | `PATCH /api/v1/orders/items/{itemId}` | `MANAGER`, `STAFF` | `order_item`, `orders`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **73** | **Order Item** | Hủy một món trong đơn (Cancel Item) | `DELETE /api/v1/orders/items/{itemId}` | `MANAGER`, `STAFF` | `order_item`, `orders`, `inventory_items`, `order_item_inventory_reservations` | **CRITICAL** |
| **74** | **Kitchen Board** | Bếp lấy danh sách đơn chế biến (Kitchen Orders) | `GET /api/v1/kitchen/orders` | `MANAGER`, `CHEF` | `orders`, `order_item`, `order_batches` | **CRITICAL** |
| **75** | **Kitchen Board** | Bếp đánh dấu món đã chuẩn bị xong (Prepared) | `PATCH /api/v1/kitchen/items/{itemId}/prepared` | `MANAGER`, `CHEF` | `order_item`, `order_item_status_history` | **CRITICAL** |
| **76** | **Kitchen Board** | Bếp chuyển trạng thái món (PENDING -> COOKING -> READY) | `PATCH /api/v1/kitchen/items/{itemId}/status` | `MANAGER`, `CHEF` | `order_item`, `order_item_status_history`, `inventory_items` | **CRITICAL** |
| **77** | **Payment** | Khởi tạo thanh toán (Tiền mặt / VietQR PayOS) | `POST /api/v1/payments` | `MANAGER`, `STAFF` | `payment_transactions`, `orders`, `vouchers`, `order_discounts`, `table_sessions`, `tables` | **CRITICAL** |
| **78** | **Payment** | Hủy link thanh toán Online (khách đổi hình thức) | `POST /api/v1/payments/{transactionId}/cancel` | `MANAGER`, `STAFF` | `payment_transactions`, PayOS Gateway | **CRITICAL** |
| **79** | **Payment** | Đồng bộ trạng thái giao dịch thanh toán từ Gateway | `GET /api/v1/payments/{transactionId}` | `MANAGER`, `STAFF` | `payment_transactions`, PayOS Gateway | HIGH |
| **80** | **Payment** | Đối soát và chốt đơn (Reconcile Order) | `POST /api/v1/orders/{orderId}/reconcile` | `MANAGER`, `STAFF` | `orders`, `payment_transactions` | HIGH |
| **81** | **Payment** | Webhook nhận thông báo chuyển khoản thành công từ PayOS | `POST /api/v1/webhooks/payos` | Public (HMAC Verified) | `payment_transactions`, `orders`, `vouchers`, `table_sessions`, `tables` | **CRITICAL** |
| **82** | **Inventory Item** | Tìm kiếm nguyên vật liệu tồn kho có phân trang | `GET /api/v1/inventory/items` | `MANAGER`, `STAFF`, `CHEF` | `inventory_items` | HIGH |
| **83** | **Inventory Item** | Tổng hợp thống kê tồn kho (Tổng số, Sắp hết, Hết hàng) | `GET /api/v1/inventory/items/summary` | `MANAGER`, `STAFF`, `CHEF` | `inventory_items` | HIGH |
| **84** | **Inventory Item** | Danh sách nguyên liệu đang hoạt động | `GET /api/v1/inventory/items/active` | `MANAGER`, `STAFF`, `CHEF` | `inventory_items` | HIGH |
| **85** | **Inventory Item** | Danh sách nguyên liệu chạm ngưỡng cảnh báo | `GET /api/v1/inventory/items/low-stock` | `MANAGER`, `STAFF`, `CHEF` | `inventory_items` | HIGH |
| **86** | **Inventory Item** | Chi tiết một nguyên liệu tồn kho | `GET /api/v1/inventory/items/{id}` | `MANAGER`, `STAFF`, `CHEF` | `inventory_items` | HIGH |
| **87** | **Inventory Item** | Thêm mới nguyên vật liệu vào kho | `POST /api/v1/inventory/items` | `MANAGER` | `inventory_items` | HIGH |
| **88** | **Inventory Item** | Cập nhật nguyên liệu (Optimistic Locking) | `PUT /api/v1/inventory/items/{id}` | `MANAGER` | `inventory_items` | HIGH |
| **89** | **Inventory Item** | Xóa nguyên vật liệu (kiểm tra ràng buộc Recipe) | `DELETE /api/v1/inventory/items/{id}` | `MANAGER` | `inventory_items`, `recipe_items` | HIGH |
| **90** | **Inventory Item** | Nhập thêm nguyên liệu vào kho (Stock In) | `POST /api/v1/inventory/items/{id}/stock-in` | `MANAGER` | `inventory_items`, `stock_movements` | **CRITICAL** |
| **91** | **Inventory Item** | Kiểm kê và điều chỉnh số lượng tồn kho (Adjust) | `POST /api/v1/inventory/items/{id}/adjust` | `MANAGER` | `inventory_items`, `stock_movements` | **CRITICAL** |
| **92** | **Stock Movements** | Lịch sử biến động xuất nhập tồn kho | `GET /api/v1/inventory/movements` | `MANAGER`, `STAFF`, `CHEF` | `stock_movements`, `inventory_items` | HIGH |
| **93** | **Recipes (BOM)** | Lấy định lượng công thức pha chế của món ăn | `GET /api/v1/inventory/recipes/{menuItemId}` | `MANAGER`, `STAFF`, `CHEF` | `recipe_items`, `inventory_items` | HIGH |
| **94** | **Recipes (BOM)** | Cập nhật công thức định lượng món ăn | `PUT /api/v1/inventory/recipes/{menuItemId}` | `MANAGER` | `recipe_items`, `inventory_items` | HIGH |
| **95** | **Recipes (BOM)** | Xóa định lượng của món ăn | `DELETE /api/v1/inventory/recipes/{menuItemId}` | `MANAGER` | `recipe_items` | MEDIUM |
| **96** | **Vouchers** | Danh sách Voucher khuyến mãi quản trị | `GET /api/v1/vouchers` | `MANAGER`, `STAFF`, `CHEF` | `vouchers` | HIGH |
| **97** | **Vouchers** | Chi tiết Voucher theo ID | `GET /api/v1/vouchers/{id}` | `MANAGER`, `STAFF`, `CHEF` | `vouchers` | HIGH |
| **98** | **Vouchers** | Tạo mã giảm giá mới | `POST /api/v1/vouchers` | `MANAGER` | `vouchers` | HIGH |
| **99** | **Vouchers** | Cập nhật thông tin mã giảm giá | `PUT /api/v1/vouchers/{id}` | `MANAGER` | `vouchers` | HIGH |
| **100** | **Vouchers** | Xóa mã giảm giá | `DELETE /api/v1/vouchers/{id}` | `MANAGER` | `vouchers` | HIGH |
| **101** | **Vouchers** | Kiểm tra tính hợp lệ của mã Voucher (Validate) | `GET /api/v1/vouchers/validate` | Public | `vouchers` | **CRITICAL** |
| **102** | **Promotions** | Danh sách chương trình khuyến mãi tự động | `GET /api/v1/promotions` | `MANAGER`, `STAFF`, `CHEF` | `promotions`, `promotion_days` | HIGH |
| **103** | **Promotions** | Tóm tắt quản lý chương trình khuyến mãi | `GET /api/v1/promotions/management-summary` | `MANAGER`, `STAFF`, `CHEF` | `promotions` | HIGH |
| **104** | **Promotions** | Danh sách khuyến mãi đang áp dụng trong ngày/giờ | `GET /api/v1/promotions/active` | `MANAGER`, `STAFF`, `CHEF` | `promotions`, `promotion_days` | HIGH |
| **105** | **Promotions** | Chi tiết chương trình khuyến mãi theo ID | `GET /api/v1/promotions/{id}` | `MANAGER`, `STAFF`, `CHEF` | `promotions`, `promotion_days` | HIGH |
| **106** | **Promotions** | Tạo mới chương trình khuyến mãi | `POST /api/v1/promotions` | `MANAGER` | `promotions`, `promotion_days` | HIGH |
| **107** | **Promotions** | Cập nhật chương trình khuyến mãi | `PUT /api/v1/promotions/{id}` | `MANAGER` | `promotions`, `promotion_days` | HIGH |
| **108** | **Promotions** | Xóa chương trình khuyến mãi | `DELETE /api/v1/promotions/{id}` | `MANAGER` | `promotions` | HIGH |
| **109** | **Analytics** | Báo cáo tổng quan Dashboard (Doanh thu, Đơn, Món) | `GET /api/v1/analytics/dashboard` | `MANAGER`, `STAFF`, `CHEF` | `daily_revenue_summary`, `orders` | **CRITICAL** |
| **110** | **Analytics** | Chuỗi số liệu doanh thu theo mốc thời gian | `GET /api/v1/analytics/revenue` | `MANAGER`, `STAFF`, `CHEF` | `daily_revenue_summary` | HIGH |
| **111** | **Analytics** | Đánh giá hiệu suất nhân viên phục vụ / thanh toán | `GET /api/v1/analytics/users` | `MANAGER`, `STAFF`, `CHEF` | `orders`, `users` | HIGH |
| **112** | **Analytics** | Chi tiết các đơn hàng cho báo cáo phân tích | `GET /api/v1/analytics/orders` | `MANAGER`, `STAFF`, `CHEF` | `orders` | HIGH |
| **113** | **Analytics** | Bảng xếp hạng món ăn bán chạy nhất (Top Items) | `GET /api/v1/analytics/top-items` | `MANAGER`, `STAFF`, `CHEF` | `daily_item_sales_summary`, `order_item` | HIGH |
| **114** | **Analytics** | Xu hướng bán hàng theo khung giờ | `GET /api/v1/analytics/sales-trend` | `MANAGER`, `STAFF`, `CHEF` | `orders` | HIGH |
| **115** | **Analytics** | Lịch sử đơn hàng lọc đa tiêu chí (Order History) | `GET /api/v1/analytics/orders/history` | `MANAGER` | `orders`, `tables` | **CRITICAL** |
| **116** | **Analytics** | Tổng kết thống kê số lượng & tiền của đơn hàng | `GET /api/v1/analytics/orders/summary` | `MANAGER` | `orders` | HIGH |
| **117** | **Forecast** | Dự báo doanh thu các ngày tới | `GET /api/v1/analytics/forecast/revenue` | `MANAGER`, `STAFF`, `CHEF` | `daily_revenue_summary` | MEDIUM |
| **118** | **Forecast** | Dự báo các món ăn sẽ bán chạy trong tương lai | `GET /api/v1/analytics/forecast/popular-items` | `MANAGER`, `STAFF`, `CHEF` | `daily_item_sales_summary` | MEDIUM |
| **119** | **Recommendation** | Gợi ý món ăn cá nhân hóa theo ngữ cảnh | `GET /api/v1/recommendations/personalized` | Public | `menu_item`, `daily_item_sales_summary` | HIGH |
| **120** | **Recommendation** | Gợi ý danh sách món ăn phổ biến nhất | `GET /api/v1/recommendations/popular` | Public | `menu_item`, `daily_item_sales_summary` | HIGH |
| **121** | **Recommendation** | Gợi ý bán kèm khi khách chọn món (Cross-sell) | `GET /api/v1/recommendations/cross-sell/{itemId}` | Public | `menu_item`, `order_item` | HIGH |
| **122** | **Recommendation** | Gợi ý liên quan theo món đang xem | `GET /api/v1/recommendations/item/{itemId}` | Public | `menu_item` | HIGH |
| **123** | **AI Assistant** | Khách chat hỏi đáp và nhờ AI tư vấn món ăn | `POST /api/v1/public/ai/chat` | Public (Rate-limited: 10 req/min) | Gemini AI 2.5 Flash | HIGH |
| **124** | **AI Assistant** | Nhân viên tra cứu quy trình nghiệp vụ bằng AI | `POST /api/v1/ai/staff/query` | `MANAGER`, `STAFF`, `CHEF` | Gemini AI 2.5 Flash | MEDIUM |
| **125** | **AI Assistant** | Quản lý sinh mô tả món ăn tự động bằng AI | `POST /api/v1/ai/menu-item/description` | `MANAGER` | Gemini AI 2.5 Flash | MEDIUM |
| **126** | **System Settings** | Lấy cấu hình hệ thống công khai (Tên quán, VAT) | `GET /api/v1/public/settings` | Public | `system_settings` | **CRITICAL** |
| **127** | **System Settings** | Lấy toàn bộ cấu hình hệ thống quản trị | `GET /api/v1/settings` | `MANAGER`, `STAFF`, `CHEF` | `system_settings` | **CRITICAL** |
| **128** | **System Settings** | Cập nhật cấu hình hệ thống (VAT, Giờ mở cửa, Bật/tắt) | `PUT /api/v1/settings` | `MANAGER` | `system_settings` | **CRITICAL** |
| **129** | **System Settings** | Tải lên Logo nhà hàng | `POST /api/v1/settings/logo` | `MANAGER` | `system_settings`, Cloudinary | MEDIUM |
| **130** | **System Health** | Kiểm tra trạng thái hoạt động (Health check) | `GET /actuator/health` | Public | Spring Actuator | **CRITICAL** |
| **131** | **System Metrics** | Xuất số liệu giám sát Prometheus Metrics | `GET /actuator/prometheus` | `MANAGER` | Micrometer Prometheus | HIGH |
| **132** | **Real-time STOMP** | WebSocket Endpoint kết nối Realtime | `WS /ws` (SockJS + STOMP) | Handshake Auth + Topic RBAC | In-Memory Message Broker | **CRITICAL** |
| **133** | **Error Fallback** | Endpoint bắt lỗi chung của hệ thống | `ANY /error` | Public | ApiErrorController | HIGH |

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
   - Khách yêu cầu tính tiền -> Phục vụ mở `PaymentModal` trên `TableGrid`.
   - Phục vụ chọn Tiền mặt hoặc Quét mã VietQR (PayOS).
   - Backend tạo `PaymentTransaction` với `idempotencyKey`.
   - Nếu là PayOS: Sinh mã QR động Napas247 chứa số tiền chính xác và nội dung chuyển khoản.
   - Khi khách chuyển khoản thành công, PayOS gửi Webhook đến `POST /api/v1/webhooks/payos` với chữ ký HMAC.
   - Backend xác thực chữ ký, chốt đơn `orders.payment_status = 'PAID'`, cập nhật doanh thu `daily_revenue_summary`, đóng `table_sessions`, chuyển trạng thái bàn về `AVAILABLE`, bắn WebSocket `/topic/orders` và `/topic/tables`.
