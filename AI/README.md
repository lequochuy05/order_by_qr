# 🤖 Trí tuệ nhân tạo QROS & Tích hợp

Thư mục và tài liệu này bao gồm các mô-đun AI được tích hợp vào Hệ thống Đặt hàng QR Sắc Màu Quán. Hệ thống sử dụng hai phương pháp AI khác nhau để giải quyết các nhu cầu kinh doanh khác nhau: **Thị giác máy tính ngoại tuyến (YOLOv8)** và **Trí tuệ nhân tạo đàm thoại dựa trên đám mây (Gemini)**.

---

## 1. 🧠 Trí tuệ nhân tạo đàm thoại (Tích hợp Gemini)
Hệ thống tận dụng AI Gemini của Google để cung cấp năng lượng cho thành phần `AiChatAssistant` ở giao diện người dùng.

### Tính năng
- **Đề xuất theo ngữ cảnh:** Đề xuất các món ăn dựa trên thời gian hiện tại trong ngày (ví dụ: Cà phê vào buổi sáng, Bia vào buổi tối) và điều kiện thời tiết theo thời gian thực.

- **Bán chéo:** Đề xuất thông minh các món ăn kèm hoặc món phụ khi người dùng thêm sản phẩm vào giỏ hàng.
- **Kiểm tra xác thực phía máy chủ:** Hệ thống máy chủ sử dụng `RestTemplate` với thời gian chờ nghiêm ngặt để ngăn chặn tình trạng tắc nghẽn luồng nếu API của Google chậm hoặc không phản hồi.

### Thiết lập
Đảm bảo các biến sau được thiết lập trong tệp `.env` gốc:

```env
GEMINI_API_KEY=khóa gemini của bạn
GEMINI_API_URL=đường dẫn gemini của bạn
```

---

## 2. 👁️ Thị giác máy tính ngoại tuyến (YOLOv8 & TensorFlow.js)
Mô-đun này cho phép Bảng điều khiển quản trị tự động nhận dạng hình ảnh thực phẩm và điền thông tin biểu mẫu một cách thông minh (Tên món ăn, Giá và Danh mục) **mà không phải trả phí API đám mây**. Mô hình chạy hoàn toàn ngoại tuyến trong trình duyệt thông qua TensorFlow.js.

### 🚀 Hướng dẫn huấn luyện & tích hợp

Để tiết kiệm tài nguyên máy tính cá nhân và tận dụng khả năng của GPU, quá trình huấn luyện được tự động hóa hoàn toàn trên **Google Colab**, sau đó được xuất dưới dạng **TensorFlow.js**.

#### Bước 1: Quản lý dữ liệu (Roboflow)
1. Truy cập [Roboflow](https://roboflow.com/) để tải lên, gắn nhãn và cải thiện dữ liệu hình ảnh thực phẩm của bạn.

2. Tạo một phiên bản tập dữ liệu. Đảm bảo có đủ sự đa dạng về hình ảnh cho mỗi mặt hàng để tăng độ chính xác.

3. Cập nhật thông tin `ROBOFLOW_API_KEY`, Không gian làm việc và Dự án trong môi trường của bạn.

#### Bước 2: Huấn luyện (Google Colab)
1. Mở [Google Colab](https://colab.research.google.com/) và tải lên tệp kịch bản [`scripts/train_classifier.py`](./scripts/train_classifier.py).

2. Thay đổi Thời gian chạy thành **GPU (T4)**.

3. Chạy kịch bản. Hệ thống sẽ:

- Lấy tập dữ liệu từ Roboflow.

- Huấn luyện mô hình phân loại YOLOv8 (`yolov8n-cls.pt`).
- Xuất các trọng số chuẩn sang định dạng TensorFlow.js.

- Đóng gói kết quả vào tệp `model_for_frontend.zip`.

#### Bước 3: Tích hợp vào giao diện người dùng
1. Tải xuống `model_for_frontend.zip` từ Colab.
2. Giải nén và sao chép các tệp vào thư mục frontend tại: `frontend/public/models/dish-classifier/`
3. **Bước quan trọng:** Tạo tệp `labels.json` trong cùng thư mục để cung cấp siêu dữ liệu cho chức năng tự động điền:

```json

{
"0": { "name": "7 Up", "categoryid": 6, "price": 12000 },

"1": { "name": "Stir-fried Corn", "categoryid": 3, "price": 20000 }

}
```
*Lưu ý: Các khóa ID ("0", "1") được YOLO tạo ra phụ thuộc vào thứ tự lớp trong quá trình huấn luyện.*

---

## 📁 Cấu trúc thư mục

* **[scripts/](./scripts)**: Các tập lệnh tự động hóa (`train_classifier.py` và `requirements.txt`) để huấn luyện mô hình YOLOv8.
* **[README.md](./README.md)**: Tệp tài liệu này.