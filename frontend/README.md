# 📱 Giao diện người dùng QROS (React & Vite)

Cung cấp giao diện hiện đại, tối ưu hóa cao và trải nghiệm mượt mà cho cả khách hàng (quét mã QR để đặt hàng) và quản trị viên (quản lý bảng điều khiển). Hoàn toàn dựa trên sự kiện, loại bỏ việc thăm dò API truyền thống.

## ✨ Các tính năng chính

- **Thực đơn QR dành cho khách hàng:** Đặt hàng nhanh chóng qua mã QR, giỏ hàng trực quan và giao diện người dùng ưu tiên thiết bị di động.

- **Bảng điều khiển quản trị & Phân tích:** Quản lý toàn diện các danh mục, món ăn và nhân viên. Có các biểu đồ tương tác phong phú được hỗ trợ bởi **Recharts**.

- **Kiến trúc hướng sự kiện (Không thăm dò):** Loại bỏ hoàn toàn logic `setInterval`. Cập nhật thời gian thực về trạng thái Thanh toán (PayOS) và đơn đặt hàng của Bếp được đẩy ngay lập tức qua **STOMP WebSockets**.

- **Trợ lý AI đàm thoại (Gemini):** Tích hợp Trợ lý trò chuyện thông minh để đưa ra các đề xuất theo ngữ cảnh cho khách hàng dựa trên thời gian và độ phổ biến của món.
- **Thị giác máy tính ngoại tuyến (TensorFlow.js):** Tích hợp nhận dạng hình ảnh AI (YOLOv8) để nhận diện món ăn và tự động điền vào biểu mẫu mà không phát sinh chi phí API đám mây.

- **Tuân thủ UX doanh nghiệp:** Tuân thủ nghiêm ngặt "Quy tắc thiết kế Maestro" đảm bảo khả năng truy cập tối ưu (độ tương phản AA) và sự hài hòa màu sắc (chủ đề Xanh ngọc/Xanh lục bảo, loại bỏ các sắc thái Tím thông thường).

## 💻 Công nghệ sử dụng
- **Khung phần mềm:** [React 19](https://react.dev/) (Vite) - HMR cực nhanh và các bản dựng được tối ưu hóa.

- **Quản lý trạng thái:** [Zustand](https://zustand-demo.pmnd.rs/) - Quản lý trạng thái toàn cục nhẹ, giảm thiểu việc truy cập props.

- **Giao diện người dùng:** [Tailwind CSS v4](https://tailwindcss.com/) - Thiết kế đáp ứng hiện đại, ưu tiên tiện ích.
- **Trí tuệ nhân tạo & Phân tích:** [TensorFlow.js](https://www.tensorflow.org/js) (cho thị giác máy tính) & [Recharts](https://recharts.org/) (cho phân tích bảng điều khiển).

- **Mạng:** [Axios](https://axios-http.com/) với bộ chặn JWT.

- **Kết nối thời gian thực:** [StompJS](https://stomp-js.github.io/stompjs/) & [SockJS](https://github.com/sockjs/sockjs-client) để kết nối socket hai chiều ổn định.

---

## 🚀 Thiết lập & Thực thi

### 1. Yêu cầu hệ thống
- Môi trường: Node.js (khuyến nghị phiên bản 18 LTS trở lên).

- Trình quản lý gói: npm hoặc yarn.

### 2. Cài đặt các thư viện phụ thuộc
```bash
cd frontend
npm install
```
### 3. Biến môi trường
Đảm bảo `.env.development` và `.env.production` được cấu hình đúng với URL API backend và URL WS ​​của bạn.

### 4. Cấu hình AI Dish Classifier (Tùy chọn)
Tính năng nhận dạng hình ảnh ngoại tuyến (YOLOv8) yêu cầu trọng số mô hình.

1. Huấn luyện mô hình bằng cách sử dụng các tập lệnh Colab được tìm thấy trong `../AI/README.md`.

2. Đặt các tệp kết quả (`model.json`, `.bin` và `labels.json`) vào:

`public/models/dish-classifier/`

### 5. Khởi động môi trường phát triển
```bash
npm run dev
```
Truy cập ứng dụng qua: `http://localhost:5173`.

---

## 📁 Cấu trúc thư mục

```text
src/
├── components/ # Các thành phần giao diện người dùng dùng chung (AiChatAssistant, Skeleton, Modals)
├── context/ # Các nhà cung cấp ngữ cảnh toàn cục (AuthContext)
├── hooks/ # Các hook tùy chỉnh (useWebsocket, useAdminPreferences)
├── pages/ # Các chế độ xem dựa trên tuyến đường (Bảng điều khiển quản trị, Menu khách hàng)
├── services/ # Tích hợp API bên ngoài, cấu hình Axios, logic AI
├── stores/ # Các kho lưu trữ trạng thái (settingsStore, categoryStore)
├── utils/ # Các hàm tiện ích (invoiceGenerator, formatters)
└── App.jsx # Bố cục định tuyến chính
```
