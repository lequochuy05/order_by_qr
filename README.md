# 🧾 QR Ordering System

QR Ordering System là hệ thống gọi món bằng mã QR dành cho quán ăn.
Khách hàng chỉ cần quét mã QR trên bàn, hệ thống sẽ hiển thị menu đúng với bàn đó, cho phép đặt món và gửi đơn hàng trực tiếp đến nhân viên hoặc bếp.

## 🚀 Tính năng chính

1. Khách hàng
📱 Quét mã QR → hiển thị menu của bàn.
🛒 Chọn món, số lượng, ghi chú (ví dụ: "mì cay cấp 4").
🔁 Nếu bàn có đơn PENDING → tiếp tục thêm món vào đơn.
🧾 Xem tóm tắt đơn hàng trước khi gửi.

2. Nhân viên
👀 Xem danh sách các bàn đang phục vụ.
➕ Thêm món vào đơn đang có.
✅ Cập nhật trạng thái món (chuẩn bị xong).
💵 Xử lý thanh toán để hoàn tất đơn.

3. Quản lý
📋 Quản lý bàn: thêm / sửa / xóa bàn.
🍽️ Quản lý món ăn & danh mục.
📊 Xem thống kê doanh thu, hiệu suất nhân viên.
👤 Quản lý tài khoản nhân viên & phân quyền.

🔄 Quy trình hoạt động
1. Khách hàng quét QR → gọi API lấy menu của bàn.
2. Chọn món → gửi đơn → trạng thái bàn chuyển PENDING.
3. Nhân viên xác nhận chế biến → đơn chuyển ORDERED.
4. Khi hoàn tất & thanh toán → đơn chuyển PAID → bàn sẵn sàng phục vụ bàn tiếp theo.

## 🛠️ Công nghệ sử dụng

| Thành phần        | Công nghệ       |
|------------------|------------------|
| Backend          | Spring Boot, JPA |
| CSDL             | MySQL            |
| Frontend         | HTML, CSS, JS    |
| Giao tiếp API    | RESTful API      |
| Realtime         | WebSocket        |
| Security         | Spring Security  |

📸 Các giao diện chính
1. Quản lý bàn – CRUD bàn, xem trạng thái bàn (FREE, PENDING, PAID).
2. Quản lý món ăn – CRUD món ăn, lọc theo danh mục.
3. Quản lý danh mục – CRUD danh mục món.
4. Quản lý nhân viên – CRUD tài khoản, phân quyền.
5. Thống kê – biểu đồ doanh thu theo ngày, hiệu suất nhân viên.
6. Giao diện nhân viên – nhận đơn, thêm món, cập nhật trạng thái, thanh toán.
7. Giao diện khách hàng – xem menu, chọn món, gửi đơn.

---
👨‍💻 Tác giả
Lê Quốc Huy – Developer
 
