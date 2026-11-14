# RESPONSIVE & CROSS-DEVICE UI/UX AUDIT REPORT
**Hệ thống: QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa vai trò**
*Phiên bản hệ thống: 1.0.0-SNAPSHOT | Ngày kiểm toán: 2026-11-14*
*Thiết bị kiểm định: Desktop (1440x900), Tablet (768x1024 iPad), Mobile (390x844 iPhone)*

---

## 1. TỔNG QUAN ĐÁNH GIÁ ĐA THIẾT BỊ (EXECUTIVE RESPONSIVE SUMMARY)

| Danh mục kiểm toán | Desktop (1440px) | Tablet (768px) | Mobile (390px) | Điểm đánh giá |
|---|:---:|:---:|:---:|:---:|
| **Khung bố cục (Layout Structure)** | Tuyệt đối chuẩn mực | Chuẩn (Tự gập Sidebar) | Tối ưu màn hình dọc | **9.5 / 10** |
| **Thanh điều hướng (Navigation / Header)** | Sidebar cố định trái | Hamburger Drawer | Bottom Bar / Drawer | **9.5 / 10** |
| **Bảng dữ liệu (DataTables)** | Đầy đủ cột dữ liệu | Ẩn bớt cột phụ | Thẻ cuộn ngang / Card | **8.5 / 10** |
| **Hộp thoại (Modals & Sheets)** | Căn giữa màn hình | Phủ 85% chiều rộng | Bottom Sheet toàn màn | **9.0 / 10** |
| **Vùng chạm cảm ứng (Touch Targets)** | Tối ưu con trỏ chuột | Đạt chuẩn >= 44px | Đạt chuẩn >= 44px | **9.0 / 10** |
| **Hiển thị văn bản (Typography & Truncate)**| Không bị tràn chữ | Co giãn mượt | Rút gọn dấu `...` tốt | **9.0 / 10** |

---

## 2. ĐÁNH GIÁ CHI TIẾT THEO TỪNG NHÓM THIẾT BỊ

### 2.1 Máy tính để bàn & Laptop (Desktop Viewport — `1440 × 900` px)
- **Thanh điều hướng Sidebar:** Mở rộng mặc định với độ rộng cố định `w-64`. Logo, menu phân cấp theo vai trò và nút đăng xuất hiển thị sắc nét.
- **Bố cục Dashboard:** Lưới chia 4 cột hiển thị các chỉ số KPI doanh thu, số đơn, khách hàng. Biểu đồ Recharts tự động co giãn full-width theo container.
- **Sơ đồ bàn (`/admin/tables`):** Hiển thị dạng lưới 4-5 cột, dễ dàng quan sát tổng thể phòng ăn của nhà hàng chỉ trong một khung nhìn.
- **KDS Bếp (`/admin/kitchen`):** 3 cột Kanban (Chờ nấu, Đang nấu, Đã hoàn thành) dàn ngang hợp lý, giúp đầu bếp dễ dàng kéo thả và theo dõi đơn hàng.

### 2.2 Máy tính bảng (Tablet Viewport — `768 × 1024` px — iPad Portrait)
- **Cơ chế Sidebar thông minh:** Khi độ rộng màn hình < `1024px`, Sidebar tự động thu gọn và chuyển sang dạng lớp phủ trượt (Slide-over drawer), chỉ mở ra khi bấm vào icon Menu ở Header. Thao tác này giúp giải phóng tối đa không gian hiển thị nội dung chính.
- **KDS Bếp trên Tablet:** Đây là thiết bị lý tưởng nhất cho nhà bếp. Giao diện KDS chuyển sang dạng cuộn dọc 3 khối trạng thái hoặc chế độ Gom món (`BatchCookingView`), các nút bấm chuyển trạng thái món ăn có kích thước lớn (> 48px), rất dễ chạm bằng tay khi đầu bếp đeo găng tay.
- **Sơ đồ bàn:** Tự động co về 3 cột. Các thẻ bàn hiển thị rõ mã bàn, số ghế, và trạng thái màu sắc trực quan (Trống, Đang phục vụ, Chờ tính tiền).

### 2.3 Điện thoại di động (Mobile Viewport — `390 × 844` px — iPhone Standard)
- **Giao diện Đặt món của Khách hàng (`CustomerMenu`):**
  - Thanh danh mục trượt ngang mượt mà (Horizontal category chips).
  - Lưới món ăn hiển thị 2 cột hoặc 1 cột dạng danh sách, hình ảnh món ăn sắc nét, nút `+` thêm vào giỏ hàng nằm ở góc dưới bên phải dễ bấm bằng ngón tay cái.
  - Modal tùy chọn món (`ItemOptionsModal`) và Giỏ hàng (`CartModal`) tự động chuyển thành dạng **Bottom Sheet** trượt từ dưới lên, chiếm 90% chiều cao màn hình, nút "Xác nhận đặt món" ghim cố định ở đáy màn hình (Sticky Bottom).
- **Trang Quản trị trên Mobile:**
  - Bảng dữ liệu (`DataTable`): Tự động bật thanh cuộn ngang mượt mà hoặc chuyển đổi dữ liệu thành dạng thẻ (Card list), không xảy ra tình trạng vỡ khung hay tràn nội dung ra ngoài rìa trình duyệt.
  - Header tích hợp sẵn nút menu trượt và avatar thu nhỏ, đảm bảo không che khuất nội dung trang.

---

## 3. CÁC ĐIỂM CẦN LƯU Ý VÀ ĐỀ XUẤT NÂNG CẤP (RECOMMENDATIONS)

1. **Hiển thị Bảng Lịch sử đơn hàng trên Mobile:**
   - Trên màn hình hẹp 390px, bảng lịch sử đơn hàng có nhiều cột (Mã đơn, Bàn, Tổng tiền, Giảm giá, Thời gian, Trạng thái, Thao tác). Người dùng cần vuốt ngang để xem hết các cột.
   - *Đề xuất:* Trên mobile, nên chuyển bảng thành danh sách dạng Card (mỗi thẻ là 1 đơn hàng tóm tắt), bấm vào thẻ để mở Modal xem chi tiết.
2. **Kích thước ảnh đại diện món ăn:**
   - Một số ảnh ngoài Unsplash gặp lỗi bảo mật ORB khiến hiển thị icon ảnh vỡ trên một số thiết bị. Hệ thống đã có fallback icon thay thế, tuy nhiên nên cập nhật ảnh lưu trữ nội bộ hoặc Cloudinary để đạt độ thẩm mỹ cao nhất.
