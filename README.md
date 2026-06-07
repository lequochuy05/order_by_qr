# QROS - Hệ thống Gọi Món Bằng Mã QR

QROS là hệ thống gọi món bằng mã QR dành cho nhà hàng và quán ăn. Khách hàng chỉ cần quét mã QR tại bàn để xem thực đơn, thêm món ăn hoặc combo vào giỏ hàng, gửi đơn đặt món và theo dõi trạng thái phục vụ theo thời gian thực.

Nhân viên và quản lý vận hành hệ thống thông qua trang quản trị (Admin Dashboard), nơi hỗ trợ quản lý bàn ăn, thực đơn, bếp, thanh toán, voucher, nhân sự, cấu hình hệ thống và báo cáo doanh thu.

## Kiến Trúc Dự Án

Dự án bao gồm hai ứng dụng chính:

* **backend/**: Spring Boot REST API sử dụng PostgreSQL, Redis, WebSocket, PayOS, Cloudinary và Gemini AI.
* **frontend/**: React/Vite Single Page Application dành cho khách hàng và nhân viên quản lý.

---

## Chức Năng Chính

### Khách Hàng

* Quét mã QR theo `tableCode` để truy cập thực đơn.
* Xem danh mục món ăn, combo và các gợi ý món.
* Thêm món vào giỏ hàng và gửi đơn trực tiếp tới nhà bếp.
* Theo dõi trạng thái đơn hàng theo thời gian thực.
* Thanh toán bằng tiền mặt hoặc PayOS.

### Nhân Viên & Quản Lý

* Quản lý danh mục món ăn và combo.
* Quản lý bàn ăn và mã QR.
* Quản lý voucher khuyến mãi.
* Quản lý nhân viên và phân quyền.
* Quản lý thông tin nhà hàng.
* Theo dõi hoạt động bếp theo thời gian thực.
* Xem báo cáo doanh thu và thống kê kinh doanh.

### Bếp

* Nhận đơn hàng theo thời gian thực.
* Cập nhật trạng thái món ăn:

  * `PENDING`
  * `COOKING`
  * `FINISHED`
  * `CANCELLED`

### Hệ Thống

* Hỗ trợ WebSocket STOMP/SockJS cho các tác vụ realtime.
* Tích hợp Gemini AI để hỗ trợ tư vấn và gợi ý món ăn.
* Hỗ trợ nhận diện món ăn bằng TensorFlow.js trên frontend.
* Tích hợp PayOS cho thanh toán trực tuyến.
* Hỗ trợ Redis Cache.
* Hỗ trợ Prometheus Metrics và Spring Actuator.
* Hỗ trợ Flyway Migration để quản lý cơ sở dữ liệu.

---

## Công Nghệ Sử Dụng

| Thành phần       | Công nghệ                                                                |
| ---------------- | ------------------------------------------------------------------------ |
| Backend          | Java 21, Spring Boot 3.4.1, Spring Security, Spring Data JPA, Validation |
| Database         | PostgreSQL, Flyway, Hibernate                                            |
| Cache & Realtime | Redis, STOMP WebSocket, SockJS                                           |
| Authentication   | JWT Access Token, Refresh Token Cookie                                   |
| AI & Tích hợp    | Gemini AI, PayOS, Cloudinary, SMTP                                       |
| Monitoring       | Spring Actuator, Micrometer Prometheus                                   |
| Frontend         | React 19, Vite 7, Tailwind CSS 4                                         |
| Frontend Data    | Axios, Zustand, Recharts, TensorFlow.js                                  |
| Triển khai       | Docker, Nginx, Vercel                                                    |

---

## Cấu Trúc Thư Mục

```text
.
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/qros/
│       ├── core/             # Cấu hình hệ thống
│       ├── infrastructure/   # Các thành phần hạ tầng
│       ├── modules/          # Các module nghiệp vụ
│       └── shared/           # Thành phần dùng chung
│
├── frontend/
│   ├── package.json
│   └── src/
│       ├── app/
│       ├── pages/
│       ├── modules/
│       ├── entities/
│       ├── shared/
│       └── widgets/
│
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Yêu Cầu Môi Trường

* JDK 21 trở lên
* Maven 3.9 trở lên
* Node.js 20 trở lên
* npm 10 trở lên
* PostgreSQL
* Redis

Ngoài ra cần cấu hình các dịch vụ bên thứ ba nếu muốn sử dụng đầy đủ chức năng:

* PayOS
* Cloudinary
* Gemini AI
* SMTP Mail

---

## Cấu Hình Backend

Tạo file `.env` từ file mẫu:

```bash
cp .env.example .env
```

Backend hiện sử dụng symlink:

```text
backend/.env -> ../.env
```

Nếu hệ điều hành không hỗ trợ symlink, hãy sao chép file `.env` vào thư mục `backend`.

### Một số biến môi trường quan trọng

| Biến           | Mô tả                    |
| -------------- | ------------------------ |
| PORT           | Cổng chạy ứng dụng       |
| DB_URL         | URL kết nối PostgreSQL   |
| DB_USERNAME    | Tài khoản PostgreSQL     |
| DB_PASSWORD    | Mật khẩu PostgreSQL      |
| REDIS_HOST     | Địa chỉ Redis            |
| JWT_SECRET     | Khóa ký JWT              |
| PAYOS_*        | Thông tin tích hợp PayOS |
| CLOUDINARY_*   | Cấu hình Cloudinary      |
| GEMINI_API_KEY | API Key của Gemini       |
| MAIL_*         | Cấu hình SMTP            |

---

## Chạy Dự Án Local

### Backend

```bash
cd backend
mvn spring-boot:run
```

Chạy với profile `dev` để tự động tạo tài khoản quản trị mặc định:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Tài khoản mặc định:

```text
Email: admin@gmail.com
Password: admin123
Role: MANAGER
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Địa Chỉ Mặc Định

| Thành phần     | Địa chỉ                                       |
| -------------- | --------------------------------------------- |
| Backend API    | http://localhost:8080                         |
| Frontend       | http://localhost:5173                         |
| WebSocket      | http://localhost:8080/ws                      |
| Trang khách    | http://localhost:5173/menu?tableCode=<ma-ban> |
| Trang quản trị | http://localhost:5173/login                   |

---

## Cơ Sở Dữ Liệu

Hệ thống sử dụng Flyway Migration tại:

```text
backend/src/main/resources/db/migration
```

Hibernate được cấu hình:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Do đó mọi thay đổi cơ sở dữ liệu phải được thực hiện thông qua Flyway trước khi ứng dụng khởi động.

Toàn bộ hệ thống sử dụng múi giờ:

```text
Asia/Ho_Chi_Minh
```

---

## Kiểm Thử

### Backend

```bash
cd backend
mvn test
```

### Frontend

```bash
npm run lint
npm run build
```

Hiện tại frontend chưa tích hợp bộ kiểm thử tự động.

---

## Docker

Build image:

```bash
docker build -t qros-backend:latest .
docker build -t qros-frontend:latest frontend
```

Khởi động:

```bash
docker compose up
```

Sau khi chạy:

* Frontend: `http://localhost`
* Backend: `http://localhost:8080`

Nginx sẽ tự động proxy các request `/api` và `/ws` tới backend.

---

## Tài Liệu Chi Tiết

* `backend/README.md`
* `frontend/README.md`

---

## Lưu Ý Bảo Mật

* Không commit file `.env`.
* Không công khai JWT Secret.
* Không công khai API Key hoặc Secret Key của các dịch vụ bên thứ ba.
* Chỉ trả về các dữ liệu cần thiết cho các API công khai.

---

## Tác Giả

**Lê Quốc Huy**

* Email: [wuchuy05.dev@gmail.com](mailto:wuchuy05.dev@gmail.com)
* GitHub: github.com/wuchuy05
