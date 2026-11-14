# Connected Task Journey, PRD Scope Groups & Dependency Matrix

> **Mô-đun:** `journey-features.md`  
> **Giai đoạn:** Product Requirements Definition (PRD) & Journey Mapping  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  

---

## 1. Connected End-to-End User Journey Map (Hành trình trải nghiệm kết nối)

Chuỗi hành trình từ lúc khách hàng bước vào nhà hàng cho đến khi hoàn tất thanh toán và dữ liệu được ghi nhận vào kho - kế toán:

```
[1. QUÉT QR BÀN] ───> [2. LỰA MÓN & GỘP BÀN] ───> [3. GỬI ĐƠN VÀO BẾP]
Khách quét camera;     Khách lướt menu số,        Khách xác nhận giỏ;
Hệ thống xác thực      tùy biến topping, ghép     đơn gửi qua STOMP WSS;
phiên qua `sig`.       bàn nếu đi nhóm đông.      bàn đổi `OCCUPIED`.
                                                         │
                                                         ▼
[6. RỜI BÀN & ĐỐI SOÁT] <── [5. CHỐT BILL PAYOS/TIỀN] <── [4. KDS BẾP CHẾ BIẾN]
Bàn về `AVAILABLE`;          Khách quét VietQR động,    KDS Kanban nhận đơn,
Tự trừ kho theo BOM;         PayOS Webhook tự khớp,     gom mẻ Batch Cook,
AI cập nhật dự báo.          in bill trong 1 giây.      báo `READY` khi nấu xong.
```

### Chi tiết các pha trải nghiệm (Journey Phases):

| Pha (Phase) | Hành động của Khách hàng | Tương tác Nhân sự & Sàn | Xử lý Hệ thống Ngầm |
|---|---|---|---|
| **Pha 1: Tiếp cận & Mở bàn** | Quét mã QR dán trên mặt bàn. | Nhân viên đón tiếp, hướng dẫn nếu khách cần. | `TableSessionResolver` kiểm tra bàn; nếu chưa có session thì sinh mới; kiểm tra chữ ký `sig`. |
| **Pha 2: Chọn món & Tùy biến** | Xem danh mục, ảnh món, chọn size, topping, ghi chú không hành. Đi đông thì báo phục vụ gộp bàn. | Phục vụ mở `MergeTableModal` ghép các bàn vật lý nếu khách đi nhóm lớn. | Ghép bàn cập nhật `table_session_tables` với khóa `Pessimistic Lock`. Giỏ hàng của cả nhóm tự đồng bộ. |
| **Pha 3: Đặt món & Nhận đơn** | Bấm "Gửi đơn gọi món". | Phục vụ nhận được notification bàn vừa gọi thêm món. | Tạo bản ghi `Order` & `OrderItems`; phát WebSocket STOMP tới topic `/topic/kitchen`. |
| **Pha 4: Bếp chế biến & Trả món** | Nhìn thấy trạng thái món trên điện thoại: `Đang nấu` -> `Đã xong`. | Đầu bếp thao tác trên KDS; phục vụ bê món ra bàn khi nghe chuông "READY". | KDS đếm timer; phát chuông nhắc nếu món > 20 phút. Khi bấm `READY`, hệ thống tự trừ tồn kho theo `inventory_recipes` (BOM). |
| **Pha 5: Thanh toán & Chốt bill** | Bấm "Gọi thanh toán" trên điện thoại hoặc gọi thu ngân. Chọn trả tiền mặt hoặc VietQR. | Thu ngân mở `PaymentModal`, chọn voucher (nếu có), in hóa đơn nhiệt. | PayOS sinh VietQR động; Webhook tự động bắt giao dịch khớp tiền thành công, đổi trạng thái sang `PAID`. |
| **Pha 6: Giải phóng bàn & Báo cáo** | Khách nhận hóa đơn điện tử, rời bàn vui vẻ. | Nhân viên dọn bàn chuẩn bị đón lượt khách mới. | Đóng `TableSession`, giải phóng tất cả các bàn về trạng thái `AVAILABLE`. Dữ liệu doanh thu cập nhật vào Dashboard và mô hình AI Gemini. |

---

## 2. PRD Scope Groups (Phân nhóm chức năng chi tiết - Epics & Features)

Hệ thống QROS được phân bổ thành 6 Nhóm phạm vi chức năng (Scope Groups / Epics):

### Epic 1: Guest Ordering & Dynamic QR Table Session Engine
- **F1.1 - Secure QR Verification:** Quét mã QR với cơ chế xác thực chữ ký bảo mật `sig`, chống đặt món giả mạo từ xa.
- **F1.2 - Dynamic Menu Browsing:** Xem danh mục, tìm kiếm món, xem chi tiết ảnh, giá, mô tả, danh sách món bán chạy/món mới.
- **F1.3 - Dish Customization & Add-ons:** Tùy chọn size, mức đường/đá, danh sách topping phụ thu và trường ghi chú đặc biệt cho đầu bếp.
- **F1.4 - Real-time Cart Management:** Giỏ hàng cục bộ và đồng bộ nhóm, tự động tính tổng tiền tạm tính, cảnh báo khi món bị hết trong lúc đang chọn.
- **F1.5 - Dynamic Table Merging (Gộp bàn V1.15):** Gộp nhiều bàn vật lý vào 1 phiên duy nhất (`table_session_tables`), bảo toàn đơn hàng, cho phép mọi người trong bàn ghép cùng gọi món vào 1 bill.

### Epic 2: Kitchen Display System (KDS) & Batch Cooking Engine
- **F2.1 - Real-time Kanban Board:** 3 cột trạng thái trực quan: `PENDING` (Chờ nhận) -> `COOKING` (Đang chế biến) -> `READY` (Sẵn sàng phục vụ).
- **F2.2 - Batch Cooking View:** Thuật toán tự động gom nhóm các món ăn cùng loại từ nhiều bàn khác nhau thành một mẻ nấu duy nhất (Ví dụ: 6 phần Cơm rang dưa bò từ bàn 3, 5, 8).
- **F2.3 - Elapsed Wait-time Timers:** Bộ đếm thời gian chờ thực tế trên từng món; đổi màu cảnh báo từ Xanh (<10p) sang Vàng (10-20p) và Đỏ (>20p).
- **F2.4 - Audio Alerts:** Phát âm thanh chuông thông báo khi có đơn mới và chuông cảnh báo khẩn cấp khi có món quá hạn 20 phút.
- **F2.5 - Item Rejection / Out-of-Stock:** Bếp trưởng bấm hủy món nhanh kèm lý do hết hàng; tự động gửi thông báo cho khách và nhân viên sàn.

### Epic 3: Cashier & Smart Settlement Engine
- **F3.1 - Cash Settlement Calculator:** Giao diện tính tiền mặt tự động tính số tiền thối cần trả lại cho khách, giảm thiểu sai sót nhầm lẫn tiền bạc.
- **F3.2 - PayOS Dynamic VietQR:** Tự động tạo mã VietQR động theo chuẩn Napas247 chứa chính xác số tiền cần thanh toán và mã hóa đơn duy nhất.
- **F3.3 - Webhook Instant Reconciliation:** Lắng nghe Webhook từ PayOS qua kết nối bảo mật HMAC SHA256; tự động chuyển trạng thái đơn sang `PAID` và đóng bàn trong vòng 1 giây.
- **F3.4 - Voucher Discount Engine:** Kiểm tra điều kiện mã giảm giá: giảm theo % hoặc số tiền cố định, giá trị đơn hàng tối thiểu, giới hạn tổng lượt dùng và hạn sử dụng.
- **F3.5 - Thermal Receipt Printing:** Hỗ trợ xuất mẫu in hóa đơn nhiệt khổ tiêu chuẩn 80mm và 58mm với đầy đủ thông tin nhà hàng, chi tiết món và mã tra cứu.

### Epic 4: Table Map & Floor Management
- **F4.1 - Visual Table Grid:** Sơ đồ bàn hiển thị màu sắc theo trạng thái thực tế: `AVAILABLE` (Trống), `OCCUPIED` (Có khách), `WAITING_FOR_PAYMENT` (Chờ tính tiền), `INACTIVE` (Tạm ngưng).
- **F4.2 - Staff Quick Order Modal:** Nhân viên phục vụ có thể mở modal đặt món hộ cho khách trực tiếp trên sơ đồ bàn.
- **F4.3 - Table Merge / Split / Move:** Giao diện kéo thả hoặc chọn bàn để thực hiện gộp bàn, tách bàn hoặc chuyển toàn bộ đơn sang bàn khác.
- **F4.4 - Takeaway Order Handling:** Hỗ trợ tạo đơn khách mua mang về độc lập không gắn với bàn vật lý.

### Epic 5: Inventory, Recipe & Auto-BOM Engine
- **F5.1 - Ingredient Master Data:** Quản lý danh mục nguyên vật liệu thô (đơn vị tính: gram, ml, quả, lon; giá vốn bình quân; mức tồn kho tối thiểu).
- **F5.2 - Bill of Materials (BOM) Recipe Editor:** Thiết lập công thức chế biến cho từng món ăn/combo (Ví dụ: 1 đĩa Bò lúc lắc = 200g thịt bò thăn + 50g ớt chuông + 10ml dầu hào).
- **F5.3 - Auto-Depletion on Dish Completion:** Hệ thống tự động trừ kho nguyên liệu ngay khi đầu bếp bấm `READY` trên màn hình KDS.
- **F5.4 - Stock-In & Adjustment Vouchers:** Quản lý phiếu nhập kho từ nhà cung cấp và phiếu kiểm kê điều chỉnh chênh lệch thực tế.
- **F5.5 - Low Stock Warning:** Cảnh báo nổi bật trên Dashboard và chặn đặt món tự động khi nguyên liệu chính chạm đáy an toàn.

### Epic 6: Analytics, AI Forecasting & Multi-role Admin
- **F6.1 - Generative AI Content Assistant:** Ứng dụng Gemini AI tự động viết mô tả món ăn hấp dẫn chuẩn phong cách F&B chỉ từ tên món và nguyên liệu.
- **F6.2 - AI Predictive Revenue Forecasting:** Mô hình học máy dự báo doanh thu và số lượng khách theo ngày/tuần dựa trên dữ liệu lịch sử và chu kỳ bán hàng.
- **F6.3 - Financial & Operational Dashboards:** Biểu đồ doanh thu trực quan, cơ cấu món bán chạy nhất, giờ cao điểm và hiệu suất làm việc của từng nhân viên.
- **F6.4 - RBAC & Security 2FA:** Phân cấp nghiêm ngặt 6 vai trò; bắt buộc xác thực 2FA OTP qua email cho quản trị viên tối cao.

---

## 3. Traceability & Dependency Matrix (Ma trận phụ thuộc kỹ thuật)

| Mô-đun phụ thuộc (Downstream) | Yêu cầu tiên quyết (Upstream Prerequisites) | Cơ chế ràng buộc & Xử lý lỗi |
|---|---|---|
| **Đặt món tại bàn (F1.4)** | Quét QR hợp lệ (F1.1) & Tạo TableSession (F4.1) | Nếu mã bàn không có chữ ký `sig`, từ chối request; nếu bàn đang bị khóa, chuyển hướng về thông báo lỗi. |
| **Gộp bàn (F1.5)** | 2+ bàn ở trạng thái `AVAILABLE` hoặc `OCCUPIED` | Dùng `Pessimistic Lock` theo danh sách `table_id`; khóa hàng DB để ngăn race condition khi 2 nhân viên cùng gộp. |
| **KDS Bếp nhận đơn (F2.1)** | Đặt món thành công (F1.4) | Gửi qua STOMP `/topic/kitchen`; nếu mất kết nối WSS, KDS tự động fallback polling sau mỗi 5 giây. |
| **Tự trừ kho BOM (F5.3)** | Món chuyển sang `READY` trên KDS (F2.1) | Nếu nguyên liệu không đủ trừ, vẫn cho hoàn tất món nhưng gắn cờ cảnh báo `AMBER_INSUFFICIENT_STOCK` trong nhật ký kiểm toán. |
| **Thanh toán PayOS (F3.2)** | Đơn hàng ở trạng thái `UNPAID` | Tạo `payment_link_id` duy nhất; nếu đơn đã thanh toán trước đó, lập tức từ chối tạo mã QR mới. |
| **Đóng bàn & Giải phóng (F3.3)** | Webhook PayOS khớp (F3.3) hoặc Thu ngân bấm tiền mặt (F3.1) | Mọi bàn trong `table_session_tables` phải được cập nhật đồng thời trong một `@Transactional` duy nhất. |
