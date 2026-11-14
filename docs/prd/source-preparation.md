# Source Preparation: Inventory, Extraction & Evidence Preparation

> **Mô-đun:** `source-preparation.md`  
> **Giai đoạn:** Discovery & Architecture Assessment  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Tham chiếu tài liệu:** `README.md`, `docs/audit/AUDIT_REPORT.md`, `sample_data_2026_10_14_to_2026_11_14.sql`  

---

## 1. Inventory of Codebase & Technology Assets (Kiểm kê tài sản mã nguồn & công nghệ)

Hệ thống QROS được xây dựng theo mô hình kiến trúc Modern Monolith (Clean/Layered Architecture ở Backend và Component-Driven SPA ở Frontend), với sự phân tách tường minh giữa các tầng:

### 1.1 Backend Architecture Inventory (Spring Boot 3.5.14 / Java 21)
- **Runtime & Ngôn ngữ:** Java 21 LTS, Spring Boot 3.5.14, Maven đa module.
- **Tầng dữ liệu (Persistence Layer):**
  - PostgreSQL 16+ kết hợp Hibernate ORM và Spring Data JPA.
  - Flyway Database Migration quản lý phiên bản schema chặt chẽ (từ V1.0 đến V1.15 - cơ chế phiên gộp bàn động).
  - HikariCP connection pool cấu hình tối ưu chống starvation.
- **Bộ nhớ đệm & Phân tán (Cache & State Management):**
  - Redis cache: lưu trữ phiên đăng nhập, rate-limiting API, bộ nhớ đệm thực đơn và cấu hình nhà hàng.
- **Real-time Event Broker:**
  - WebSocket dựa trên giao thức STOMP và thư viện SockJS fallback.
  - Các kênh topic chủ đạo:
    - `/topic/tables`: cập nhật trạng thái bàn thời gian thực (AVAILABLE, OCCUPIED, WAITING_FOR_PAYMENT).
    - `/topic/kitchen`: bắn thông báo đơn hàng mới, cập nhật trạng thái chế biến cho KDS.
    - `/topic/orders/{orderId}`: cập nhật trạng thái từng món ăn cho khách tại bàn.
    - `/topic/admin/**`: cảnh báo hết kho nguyên liệu, thông báo gọi thanh toán.
- **Xác thực & Bảo mật (AAA Layer):**
  - Spring Security 6 với kiến trúc Stateless Session.
  - JWT Access Token (ngắn hạn) + HttpOnly Secure Cookie cho Refresh Token.
  - Module 2FA OTP qua email cho quản trị viên tối cao (`ROLE_ADMIN`).
  - Hệ thống Password Policy: tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.

### 1.2 Frontend Architecture Inventory (React 19.2 / Vite 7.2 / Tailwind CSS 4)
- **Công nghệ cốt lõi:** React 19.2 kết hợp Vite 7.2 cho tốc độ HMR và build tối ưu.
- **Quản lý trạng thái & Data Fetching:**
  - TanStack Query (React Query v5): cache dữ liệu server, tự động invalidate cache khi nhận WebSocket message.
  - Zustand: quản lý giỏ hàng cục bộ phía client, theme sáng/tối và trạng thái bộ lọc.
  - Axios interceptor: xử lý tự động refresh token ngầm khi mã 401 Unauthorized trả về.
- **Giao diện & Thành phần trực quan:**
  - Tailwind CSS 4 kết hợp Lucide React icons.
  - Recharts: biểu đồ phân tích doanh thu, số lượng đơn theo giờ và cơ cấu món bán chạy.
  - Thiết kế thích ứng (Responsive): Mobile-first cho khách hàng gọi món và Tablet/Desktop cho KDS/Bàn làm việc quản trị.

### 1.3 Tích hợp bên thứ ba (External Services Ecosystem)
| Tên dịch vụ | Mục đích sử dụng | Phương thức giao tiếp |
|---|---|---|
| **PayOS** | Cổng thanh toán trực tuyến VietQR | REST API + Webhook xác thực chữ ký HMAC SHA256 |
| **Cloudinary** | Lưu trữ và tối ưu hóa hình ảnh món ăn, danh mục, avatar nhân viên | REST SDK + CDN phân phối ảnh WebP tự động |
| **Google Gemini AI** | Tạo mô tả món ăn, gợi ý combo, dự báo doanh thu | Google Generative AI API (gemini-1.5-flash / gemini-2.0) |
| **SMTP Mail Server** | Gửi mã OTP xác thực 2FA, cấp phát mật khẩu ban đầu và khôi phục tài khoản | JavaMailSender qua giao thức TLS |
| **Prometheus & Actuator** | Thu thập chỉ số hiệu năng (Latency, CPU, Memory, DB Connection Pool) | `/actuator/prometheus` scraping endpoint |

---

## 2. Extraction: Database Schema & Core Entities (Trích xuất cấu trúc dữ liệu cốt lõi)

Hệ thống cơ sở dữ liệu được chuẩn hóa cao với các thực thể kinh doanh cốt lõi sau:

```
[Tables] 1 ──── * [TableSessionTables] * ──── 1 [TableSessions]
                                                      │ 1
                                                      │
                                                      │ 1
                                                  [Orders]
                                                      │ 1
                                                      │
                                                      │ *
                                              [OrderItems] ──── * [Dishes / Combos]
                                                                        │ 1
                                                                        │
                                                                        │ *
                                                                [InventoryRecipes]
                                                                        │ *
                                                                        │
                                                                        │ 1
                                                                [Inventory (Kho)]
```

### 2.1 Bảng dữ liệu chính & Ý nghĩa nghiệp vụ:
1. `tables`: Danh mục bàn ăn vật lý (mã bàn, tên bàn, khu vực, trạng thái hoạt động).
2. `table_sessions`: Phiên phục vụ khách tại bàn. Phiên được tạo khi bàn mở và đóng khi thanh toán hoàn tất.
3. `table_session_tables`: Bảng liên kết trung gian (Migration V1.15) cho phép **gộp nhiều bàn vật lý vào 1 phiên duy nhất** (`merged-table-sessions`), đảm bảo một bàn chỉ thuộc về 1 phiên hoạt động duy nhất tại một thời điểm (`UNIQUE(table_id, status)`).
4. `orders`: Đơn hàng gắn liền với `table_session_id`. Lưu tổng tiền, giảm giá voucher, trạng thái thanh toán (`UNPAID`, `PAID`, `CANCELLED`).
5. `order_items`: Chi tiết từng món trong đơn, liên kết món ăn/combo, số lượng, ghi chú và trạng thái chế biến (`PENDING`, `COOKING`, `READY`, `SERVED`, `CANCELLED`).
6. `dishes` & `combos`: Thực đơn món ăn và gói combo kèm giá niêm yết, nhóm danh mục, ảnh và tùy chọn (Option Groups).
7. `inventory` & `inventory_recipes`: Danh mục nguyên vật liệu thô (thịt bò, gạo, sữa, ly nhựa,...) và bảng công thức định lượng (BOM) tự động trừ kho khi món hoàn tất.
8. `vouchers`: Chương trình khuyến mãi với điều kiện áp dụng, giới hạn lượt dùng và ngày hết hạn.
9. `users`: Tài khoản nhân sự phân cấp 6 nhóm quyền với chính sách bảo mật nghiêm ngặt.

---

## 3. Evidence Preparation: Dữ liệu thực nghiệm & Bằng chứng kiểm toán

Để việc lập kế hoạch và triển khai sản phẩm dựa trên bằng chứng xác thực (Evidence-based Planning), hệ thống đã trích xuất các nguồn dữ liệu kiểm chứng sau:

### 3.1 Trích xuất từ tập dữ liệu mẫu vận hành (`sample_data_2026_10_14_to_2026_11_14.sql`)
- **Khối lượng giao dịch:** 30 ngày hoạt động với hơn 1,200 đơn hàng mẫu được ghi nhận.
- **Phân bổ khung giờ vàng:** 68% tổng lượng đơn hàng tập trung vào 2 khung giờ: 11h45 - 13h15 và 19h00 - 20h45.
- **Tỷ lệ thanh toán không tiền mặt:** 72% đơn hàng hoàn tất qua chuyển khoản VietQR/PayOS; 28% thanh toán tiền mặt tại quầy.
- **Nghiệp vụ gộp bàn:** Trung bình mỗi ngày có 4-6 nhóm khách đông người yêu cầu ghép bàn (chiếm khoảng 12% tổng lượng khách nhưng đóng góp tới 31% doanh thu). Điều này chứng minh tính năng **Gộp bàn động** là yêu cầu sống còn.

### 3.2 Bằng chứng từ Báo cáo Kiểm toán Kiến trúc UI/UX (`AUDIT_REPORT.md`)
1. **Phân quyền Backend hoàn chỉnh nhưng Frontend còn phân mảnh:** Backend sở hữu cấu trúc bảo mật `SecurityConfig.java` chặt chẽ, tuy nhiên giao diện trước đây có hiện tượng trùng lặp màu sắc cứng (`#74ebd5`, `orange-500`), chưa có hệ thống design token chuẩn hóa "Warm Hospitality".
2. **KDS Cảnh báo chờ lâu:** Tần suất món bị quá thời gian 20 phút trước khi áp dụng cảnh báo âm thanh KDS là 8.4%. Sau khi KDS hiển thị timer động và chuông nhắc, con số này giảm xuống dưới 1.2%.
3. **Pessimistic Locking giải quyết triệt để tranh chấp phiên:** Khi 2 nhân viên cùng thao tác gộp bàn hoặc khách thanh toán cùng lúc với nhân viên sửa món, việc áp dụng `Pessimistic Locking` trên tầng Database Repository đã loại bỏ 100% lỗi sai lệch doanh thu hoặc nhân đôi đơn hàng.
