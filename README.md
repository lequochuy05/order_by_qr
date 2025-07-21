# 🧾 QR Ordering System

Dự án này là một hệ thống gọi món bằng cách quét mã QR, cho phép khách hàng tại quán ăn/tiệm nước sử dụng điện thoại để đặt món mà không cần nhân viên phục vụ.

## 🚀 Tính năng chính

- 📱 Khách hàng quét QR → hiển thị menu tương ứng với bàn.
- 🛒 Chọn món, số lượng, gửi đơn hàng.
- 🔁 Nếu bàn đã có đơn PENDING → tiếp tục thêm món.
- 🧾 Quản lý đơn hàng theo bàn: PENDING, ORDERED, PAID.
- 🌐 Giao diện tĩnh viết bằng HTML + CSS + JavaScript (dùng Fetch API).
- ☕ Backend Spring Boot kết nối CSDL MySQL.

---

## 🛠️ Công nghệ sử dụng

| Thành phần        | Công nghệ        |
|------------------|------------------|
| Backend          | Spring Boot, JPA |
| CSDL             | MySQL            |
| Frontend         | HTML, CSS, JS    |
| Giao tiếp API    | RESTful API      |

---

## 🧪 API Chính

### 📥 `POST /api/orders`
Tạo đơn hàng mới từ danh sách món.

### 🔁 `POST /api/orders/check-or-create`
Kiểm tra đơn hàng PENDING/ORDERED hiện tại của bàn, nếu không có sẽ tạo đơn mới.

### 🍽️ `POST /api/orders/{orderId}/items`
Thêm món vào đơn hàng đang mở.

### 📖 `GET /api/menu`
Lấy danh sách món ăn.

### 🪑 `GET /api/tables/{id}`
Lấy thông tin bàn theo `tableId`.

---

## 💻 Cách chạy dự án

### 1. Clone và cấu hình

```bash
git clone https://github.com/lequochuy05/order_by_qr.git
```
### 2. Cập nhật file application.properties:
spring.datasource.url=jdbc:mysql://localhost:3306/qr_ordering
spring.datasource.username=root
spring.datasource.password=123456

### 3. 

