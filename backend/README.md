# ⚙️ QROS Backend (Spring Boot Core)

Đây là phân hệ Backend (Máy chủ) cốt lõi của **Hệ thống Đặt hàng QR Sắc Màu Quán**, được xây dựng với kiến trúc hướng sự kiện (Event-driven), tối ưu hóa thời gian thực (Real-time) và đạt chuẩn Enterprise.

Được phát triển trên nền tảng **Java 21** và **Spring Boot 3**.

---

## 🏗️ Cấu trúc Thư mục Hệ thống

```text
backend/src/main/java/com/sacmauquan/qrordering/
├── config/         # Cấu hình hệ thống (Security, WebSockets, CORS, Redis, PayOS)
├── controller/     # Các API Endpoints RESTful xử lý request từ Frontend
├── dto/            # Data Transfer Objects (Request/Response payload)
├── exception/      # Quản lý lỗi tập trung (@RestControllerAdvice)
├── mapper/         # Các interface MapStruct tự động chuyển đổi Entity <-> DTO
├── model/          # Các thực thể JPA (Users, Orders, Tables, Menu...)
├── repository/     # Lớp truy cập cơ sở dữ liệu (Spring Data JPA)
├── security/       # Bộ lọc bảo mật JWT, Rate Limiting, XSS Protection
├── service/        # Chứa logic nghiệp vụ (Business logic)
└── state/          # Quản lý State Machine (Quy trình chuyển đổi trạng thái đơn hàng)
```

---

## 🗄️ Kiến trúc Dữ liệu & Công nghệ

### 1. Database (PostgreSQL)
Hệ thống sử dụng PostgreSQL làm CSDL chính. Các Entity được thiết kế tối ưu với:
- **Lombok `@SuperBuilder`**: Kế thừa chuẩn mực từ `BaseEntity`, tự động quản lý các trường Audit (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`).
- **Entity Relationships**: Quản lý vòng đời dữ liệu bằng JPA Cascade, tối ưu hóa các lệnh fetch (`FetchType.LAZY`) chống lỗi N+1 Query.

### 2. Bộ đệm & Session (Redis Cloud)
- **Tối ưu tốc độ (Caching)**: Dữ liệu tĩnh như danh sách bàn (`tables`), thống kê doanh thu (`stats_revenue`) được cache vào Redis và tự động xóa bỏ (`@CacheEvict`) khi có giao dịch mới.
- **Bảo mật RCE**: Jackson Deserialization được cấu hình cứng `DefaultTyping.NON_FINAL` để chặn đứng các cuộc tấn công nhúng mã độc qua Redis.

---

## 🌟 Các Tính năng Kỹ thuật Cốt lõi

### 1. Hướng sự kiện thời gian thực (Real-time Event-Driven)
- Thay thế hoàn toàn cơ chế Polling (gọi liên tục API) bằng **STOMP WebSockets**.
- Hệ thống duy trì kết nối cực kỳ bền bỉ với tính năng **Heartbeats (10s)** do `ConcurrentTaskScheduler` quản lý.
- Mọi trạng thái thanh toán thành công (`PAYMENT_SUCCESS`) hoặc đổi bàn đều được phát qua `@Async` Event Listeners với cơ chế Exponential Backoff để chống nghẽn luồng.

### 2. Cổng Thanh toán Tự động (PayOS)
- Tích hợp sâu Webhooks để lắng nghe và xác nhận giao dịch chuyển khoản 100% tự động.
- Có cơ chế **Cron Job (Scheduled)**: `PaymentCleanupService` tự động chạy ngầm mỗi 5 phút để dọn dẹp, kiểm tra lại và đóng các giao dịch `PENDING` bị treo mạng.

### 3. Trí tuệ nhân tạo Đàm thoại (Gemini AI)
- Tích hợp Gemini API trực tiếp trong Backend qua `RestTemplate`.
- Phân tích thời tiết, thời gian thực và lịch sử đơn hàng để gợi ý món ăn thông minh.
- Thiết lập Timeout chặt chẽ (5s Connect, 15s Read) đảm bảo Server không bao giờ bị đứng (Thread Starvation) nếu máy chủ Google chậm phản hồi.

### 4. Quản lý trạng thái (Enterprise State Machine)
- Loại bỏ hoàn toàn vòng lặp if/else rườm rà.
- Các trạng thái của đơn hàng (`PENDING` -> `PREPARING` -> `SERVED` -> `COMPLETED`) bị kiểm soát chặt chẽ bởi `OrderStateFactory`, cấm tuyệt đối các thao tác đảo ngược trạng thái trái phép.

### 5. DDoS Protection & JWT
- Hệ thống In-memory Rate Limiting tự phát triển bằng **Thuật toán Sliding Window** để chống Spam/DDoS vào các API public và API AI.
- Xác thực JWT siêu tốc, kèm bước kiểm tra độ dài an toàn của Chuỗi bí mật (`SECRET_KEY`) ngay lúc Server mới khởi động.

---

## 🚀 Hướng dẫn Cài đặt & Vận hành

### Yêu cầu Hệ thống
- JDK 21 trở lên.
- Maven 3.9+.
- PostgreSQL 15+ đang chạy.
- Redis (Local hoặc Cloud) đang chạy.

### 1. Thiết lập Biến môi trường
Copy file `.env.exemple` (từ thư mục gốc) thành file `.env` và đặt chung tại thư mục gốc của dự án. Đảm bảo bạn đã điền các cấu hình quan trọng sau:
```env
# Cấu hình Database
DB_URL=jdbc:postgresql://localhost:5432/order_by_qr
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Cấu hình Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT & AI
JWT_SECRET=mot_chuoi_bi_mat_rat_dai_va_an_toan_hon_32_ky_tu
GEMINI_API_KEY=your_gemini_key

# Cổng thanh toán
PAYOS_CLIENT_ID=your_id
PAYOS_API_KEY=your_api_key
PAYOS_CHECKSUM_KEY=your_checksum
```

### 2. Chạy ứng dụng
Mở Terminal tại thư mục `backend` và chạy các lệnh:
```bash
# Xóa bản build cũ và biên dịch lại code
mvn clean compile

# Chạy Server
mvn spring-boot:run
```

Server sẽ khởi động mặc định tại port `8080` (hoặc tuỳ theo biến `PORT` trong file `.env`).

---

## 🧪 Testing
Backend được bao phủ bởi các Unit Test chặt chẽ sử dụng **JUnit 5** và **Mockito**.
- Chạy toàn bộ Test Suite:
```bash
mvn clean test
```
- Các bài test đã bao gồm việc cô lập Metric (Sử dụng `SimpleMeterRegistry` thay vì Mock) giúp chống lỗi NullPointerException ở môi trường ảo hóa.
