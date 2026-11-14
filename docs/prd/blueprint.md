# Business Model & System Ecosystem Blueprint

> **Mô-đun:** `blueprint.md`  
> **Giai đoạn:** Business Architecture & System Blueprint  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  

---

## 1. Business Model Canvas (Mô hình kinh doanh tổng thể)

Mô hình kinh doanh của QROS được thiết kế để mở rộng bền vững (Scalable B2B SaaS) kết hợp với doanh thu phụ trợ từ hệ sinh thái thanh toán và dịch vụ AI giá trị gia tăng:

```
┌──────────────────┬─────────────────┬─────────────────┬─────────────────┬──────────────────┐
│ KEY PARTNERS     │ KEY ACTIVITIES  │ VALUE           │ CUSTOMER        │ CUSTOMER         │
│ (Đối tác chính)  │ (Hoạt động cốt  │ PROPOSITIONS    │ RELATIONSHIPS   │ SEGMENTS         │
│                  │  lõi)           │ (Giá trị cốt    │ (Quan hệ KH)    │ (Phân khúc KH)   │
│ • PayOS          │ • Phát triển    │  lõi)           │ • Tự triển khai │ • Quán ăn & Cafe │
│   (Cổng VietQR)  │   phần mềm      │ • QR gọi món tại│   (Self-serve)  │   vừa và nhỏ     │
│ • Google Cloud / │ • Tối ưu hóa hạ │   bàn Zero-App  │ • Đội ngũ CSKH  │   (20 - 50 bàn)  │
│   Gemini AI      │   tầng & WSS    │ • KDS Bếp gom mẻ│   chuyên trách  │ • Nhà hàng Lẩu,  │
│ • Cloudinary     │ • Chăm sóc và hỗ│ • Gộp bàn động  │ • Cộng đồng F&B │   Nướng, Nhậu    │
│   (Lưu trữ CDN)  │   trợ nhà hàng  │   độc quyền     │   chia sẻ kinh  │   (Đông khách,   │
│ • Đại lý thiết bị│                 │ • Tự trừ kho BOM│   nghiệm        │   hay ghép bàn)  │
│   máy in bill/KDS├─────────────────┤ • Chốt bill 1s  ├─────────────────┤ • Chuỗi Casual   │
│                  │ KEY RESOURCES   │ • AI dự báo     │ CHANNELS        │   Dining nhiều   │
│                  │ (Nguồn lực)     │                 │ (Kênh tiếp cận) │   chi nhánh      │
│                  │ • Mã nguồn QROS │                 │ • Tiếp thị số   │                  │
│                  │ • Cụm Server & DB│                │ • Đội ngũ Sales │                  │
│                  │ • Đội ngũ kỹ sư │                 │ • QR lan tỏa từ │                  │
│                  │   F&B am hiểu   │                 │   chính thực    │                  │
│                  │                 │                 │   khách trải    │                  │
│                  │                 │                 │   nghiệm        │                  │
├──────────────────┴─────────────────┴─────────────────┴─────────────────┴──────────────────┤
│ COST STRUCTURE (Cơ cấu chi phí)                      │ REVENUE STREAMS (Dòng doanh thu)   │
│ • Chi phí hạ tầng Cloud (Render, AWS, PostgreSQL,    │ • Phí thuê bao phần mềm SaaS định  │
│   Redis, Vercel)                                     │   kỳ (Theo tháng / năm)            │
│ • Chi phí API bên thứ 3 (Gemini AI, Mail, Cloudinary)│ • Doanh thu chia sẻ cổng thanh     │
│ • Chi phí R&D, phát triển tính năng và bảo mật       │   toán (Transaction fee sharing)   │
│ • Chi phí Marketing, Bán hàng và Hỗ trợ kỹ thuật 24/7│ • Gói AI nâng cao (Gemini Tokens)  │
└──────────────────────────────────────────────────────┴────────────────────────────────────┘
```

---

## 2. System Ecosystem Architecture Blueprint (Kiến trúc hệ sinh thái kỹ thuật)

Kiến trúc QROS được tổ chức theo mô hình **Modern Micro-Monolith** với độ tin cậy cao, độ trễ cực thấp và khả năng mở rộng độc lập:

```
                            ┌───────────────────────────────────┐
                            │          NGƯỜI DÙNG CUỐI          │
                            │   (Khách bàn, Nhân viên, Quản lý) │
                            └─────────────────┬─────────────────┘
                                              │ HTTPS / WSS
                                              ▼
                            ┌───────────────────────────────────┐
                            │       EDGE / REVERSE PROXY        │
                            │         (Nginx / Cloudflare)      │
                            └───────┬───────────────────┬───────┘
                                    │                   │
                     Static Assets  │                   │ API / WebSocket
                                    ▼                   ▼
                     ┌─────────────────────┐   ┌────────────────────────┐
                     │ CLIENT SPA (Vite)   │   │ BACKEND APPLICATION    │
                     │ • React 19.2        │   │ (Spring Boot 3.5.14)   │
                     │ • TanStack Query v5 │   │ • Java 21 LTS          │
                     │ • Zustand Store     │   │ • Spring Security (JWT)│
                     │ • Tailwind CSS 4    │   │ • WebSocket STOMP Hub  │
                     └─────────────────────┘   └───────────┬────────────┘
                                                           │
        ┌────────────────────────┬─────────────────────────┼────────────────────────┐
        │                        │                         │                        │
        ▼                        ▼                         ▼                        ▼
┌───────────────┐        ┌───────────────┐         ┌───────────────┐        ┌───────────────┐
│  POSTGRESQL   │        │     REDIS     │         │   EXTERNAL    │        │  MONITORING   │
│  (Database)   │        │ (Cache & WSS) │         │  INTEGRATIONS │        │ & OBSERVABLE  │
├───────────────┤        ├───────────────┤         ├───────────────┤        ├───────────────┤
│ • Flyway V1.15│        │ • Rate Limit  │         │ • PayOS VietQR│        │ • Prometheus  │
│ • Pessimistic │        │ • Session Auth│         │ • Cloudinary  │        │ • Actuator    │
│   Row Locks   │        │ • Menu Cache  │         │ • Gemini AI   │        │ • App Logs    │
│ • Audit Trails│        │ • Event Broker│         │ • SMTP Mailer │        │   Rotating    │
└───────────────┘        └───────────────┘         └───────────────┘        └───────────────┘
```

---

## 3. End-to-End Data Flow Blueprint (Dòng chảy dữ liệu nghiệp vụ chuẩn)

### Chu trình gọi món & xử lý bếp thời gian thực (The Order-to-Kitchen Loop):
1. **Khách quét QR:** Camera điện thoại đọc URL `https://domain.com/:tableCode/menu?sig=xyz`. Frontend gửi request kiểm tra chữ ký bàn và trạng thái phiên `TableSessionResolver`.
2. **Tạo/Nhận diện phiên:** Nếu bàn chưa có khách, hệ thống tạo `TableSession` mới và đổi trạng thái bàn sang `OCCUPIED`. Nếu bàn đã nằm trong một nhóm gộp (`table_session_tables`), trả về phiên dùng chung.
3. **Thêm giỏ & Đặt món:** Khách chọn món, tùy biến topping, gửi giỏ hàng `POST /api/v1/orders`.
4. **Bắn thông báo Bếp (WebSocket Event):** Backend lưu `Order` và `OrderItems`, đồng thời phát STOMP message tới topic `/topic/kitchen`.
5. **KDS tiếp nhận:** Màn hình KDS tại quầy bếp phát chuông âm thanh nhắc nhở, hiển thị thẻ món ở cột `PENDING`. Nếu có các món giống nhau từ bàn khác, chế độ Batch Cooking tự động gom số lượng.
6. **Bếp chuyển trạng thái:** Đầu bếp bấm nhận món (`COOKING`) -> bấm hoàn tất (`READY`). Backend cập nhật DB và phát WebSocket tới `/topic/orders/{orderId}` để khách hàng tại bàn biết món đã xong.
7. **Trừ kho tự động (BOM Depletion):** Ngay khi món chuyển sang `READY`, hệ thống tra cứu bảng `inventory_recipes` và trừ trực tiếp số lượng nguyên liệu thô trong kho.

### Chu trình thanh toán & đối soát tự động (The Cashier Settlement Loop):
1. **Yêu cầu thanh toán:** Khách bấm gọi thanh toán trên điện thoại hoặc báo phục vụ -> Trạng thái bàn chuyển sang `WAITING_FOR_PAYMENT`.
2. **Khởi tạo thanh toán PayOS:** Thu ngân mở `PaymentModal`, chọn cổng PayOS. Backend gọi API PayOS sinh mã VietQR động chứa mã giao dịch và số tiền chính xác.
3. **Khách hàng chuyển khoản:** Khách quét QR qua app ngân hàng bất kỳ.
4. **Xác thực Webhook:** PayOS gửi Webhook kèm chữ ký số SHA256 về backend `POST /api/v1/payments/payos-webhook`.
5. **Đóng phiên & Giải phóng bàn:**
   - Backend xác thực chữ ký Webhook hợp lệ.
   - Sử dụng `Pessimistic Lock` cập nhật trạng thái đơn hàng sang `PAID`.
   - Đóng `TableSession`, giải phóng tất cả các bàn vật lý liên kết trong `table_session_tables` về trạng thái `AVAILABLE`.
   - Bắn WebSocket tới `/topic/tables` để cập nhật sơ đồ bàn cho toàn bộ nhân viên.

---

## 4. Security & AAA Architecture Blueprint (Bảo mật & Phân quyền)

Hệ thống tuân thủ tiêu chuẩn bảo mật đa tầng:

```
[Level 1: Network & Proxy] ──> Cloudflare SSL/TLS, Rate Limiting chống DDoS
           │
[Level 2: Authentication]  ──> Stateless JWT Access Token + HttpOnly Refresh Cookie
           │
[Level 3: Enforced 2FA]    ──> Bắt buộc mã OTP qua Email đối với ROLE_ADMIN
           │
[Level 4: Authorization]   ──> Phân cấp vai trò nghiêm ngặt (Spring Security Method Security)
                               ADMIN > MANAGER > CASHIER > STAFF
                               MANAGER > CHEF
           │
[Level 5: Data Integrity]  ──> Database Pessimistic Locking cho các tác vụ tiền tệ & gộp bàn
```

- **Phòng chống gian lận QR bàn:** Mã QR bàn không chỉ chứa số bàn tĩnh mà đi kèm tham số chữ ký số HMAC `sig`. Ngăn chặn trường hợp kẻ xấu ngồi ngoài quán quét mã bàn trong quán để đặt món phá hoại.
- **Toàn vẹn kho & Doanh thu:** Mọi thay đổi về giá bán, sửa đơn, hủy món hoặc điều chỉnh kho đều được ghi nhận vào bảng vết kiểm toán (`audit_logs`) với thông tin người thực hiện (`performed_by`), thời gian và giá trị trước/sau khi thay đổi.
