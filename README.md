# 🧾 QR Ordering System

**QR Ordering System** là một hệ thống gọi món hiện đại qua mã QR, được thiết kế để tối ưu hóa quy trình phục vụ tại các nhà hàng và quán ăn. Khách hàng có thể dễ dàng quét mã QR tại bàn để xem thực đơn, nhận các gợi ý món ăn thông minh và đặt hàng trực tiếp mà không cần chờ đợi nhân viên.

---

## 🚀 Tính năng chính

### 1. 📱 Trải nghiệm khách hàng (Customer Side)
- **Quét mã QR:** Tự động nhận diện số bàn và hiển thị thực đơn tương ứng.
- **Thực đơn thông minh:** Phân loại món ăn rõ ràng, hỗ trợ tìm kiếm và lọc theo danh mục.
- **AI Recommendations:** 
    - Gợi ý món ăn cá nhân hóa dựa trên thời điểm trong ngày và điều kiện thời tiết.
    - Gợi ý bán chéo (Cross-sell) các món đi kèm (đồ uống, topping) khi khách thêm món vào giỏ hàng.
- **Giỏ hàng & Đặt món:** Quy trình thanh toán và đặt món nhanh chóng, giao diện mượt mà.

### 2. 👔 Quản lý cửa hàng (Admin Side)
- **Quản lý thực đơn:** Thêm, sửa, xóa món ăn, danh mục và các gói combo.
- **Quản lý đơn hàng:** Theo dõi tình trạng đơn hàng theo thời gian thực (Real-time).
- **Thống kê & Báo cáo:** Biểu đồ doanh thu, món ăn bán chạy và hiệu suất kinh doanh qua Recharts.
- **Quản lý mã QR:** Tạo và quản lý mã QR định danh cho từng bàn.

### 3. 🤖 Tính năng AI đặc sắc
Hệ thống tích hợp **AI** để:
- Phân tích hình ảnh món ăn để tự động điền thông tin (tên, giá ước tính).
- Cung cấp gợi ý món ăn thông minh theo ngữ cảnh thực tế của người dùng.

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| **Backend** | Spring Boot 3, Java 21, Spring Security, Spring Data JPA |
| **CSDL** | PostgreSQL, Cloudinary |
| **Frontend** | React 19, Vite, Tailwind CSS 4 |
| **Realtime** | WebSocket (STOMP & SockJS) |
| **State Management** | Zustand |
| **Deployment** | Docker, Vercel (Frontend) |

---

## 💻 Hướng dẫn cài đặt

### Yêu cầu hệ thống
- Java 21+
- React 19+
- Maven 3.9+

### 1. Cấu hình Backend
Tạo file `.env` tại thư mục gốc với các thông số sau:
```env
DB_URL=jdbc:postgresql://your-db-url
DB_USERNAME=your-username
DB_PASSWORD=your-password
JWT_SECRET=your-secret-key
GEMINI_API_KEY=your-gemini-api-key
CLOUDINARY_CLOUD_NAME=your-name
CLOUDINARY_API_KEY=your-key
CLOUDINARY_API_SECRET=your-secret
```

Chạy ứng dụng:
```bash
mvn spring-boot:run
```

### 2. Cấu hình Frontend
```bash
cd order-by-qr-frontend
npm install
npm run dev
```


---

## 👨‍💻 Tác giả
**WucHuy** – Fullstack Developer
