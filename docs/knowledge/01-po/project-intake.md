# Project Intake: QROS (Smart QR Order & Restaurant Operating System)

> **Mô-đun:** `project-intake.md`  
> **Giai đoạn:** Discovery & Foundation (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team

---

## 1. Project Brief & Executive Summary

**QROS** là ý tưởng xây dựng một nền tảng quản trị và vận hành nhà hàng hợp nhất, lấy trải nghiệm **gọi món tại bàn qua mã QR thời gian thực** làm điểm chạm trung tâm. Sản phẩm nhằm giải quyết triệt để tình trạng tắc nghẽn giờ cao điểm, sai sót trong khâu ghi nhận món, và độ trễ thông tin giữa 4 mắt xích trọng yếu trong nhà hàng: **Thực khách tại bàn – Nhân viên phục vụ – Khu vực Bếp – Quầy Thu ngân**.

Khác với các giải pháp QR tĩnh (chỉ mở tệp ảnh/PDF để xem) hay các ứng dụng yêu cầu thực khách tải app và tạo tài khoản phức tạp, QROS được định hướng theo triết lý **Quét là gọi — Không cần cài đặt (Zero-Install Web Experience)**:
- Thực khách chỉ cần dùng camera điện thoại quét mã QR gắn cố định trên bàn là có thể xem thực đơn trực quan, tùy biến món ăn, theo dõi tiến độ bếp nấu và thanh toán nhanh chóng ngay tại bàn.
- Toàn bộ đội ngũ nội bộ (Phục vụ, Bếp, Thu ngân, Quản lý, Chủ quán) cùng vận hành trên một hệ thống đồng bộ thời gian thực, hỗ trợ đầy đủ nghiệp vụ đặc thù của nhà hàng Việt Nam như **gộp bàn cho nhóm đông**, **đơn mang về**, **nấu gom theo mẻ**, **tự động trừ nguyên liệu theo công thức**, **đối soát chuyển khoản QR tự động** và **trợ lý AI hỗ trợ bán hàng & dự báo**.

---

## 2. Project Boundary (Ranh giới & Phạm vi sản phẩm)

Để đảm bảo sản phẩm khả thi, tập trung đúng giá trị cốt lõi và không dàn trải nguồn lực, phạm vi được xác định rõ:

### 2.1 In-Scope (Thuộc phạm vi sản phẩm)
- **Trải nghiệm thực khách tại bàn (Dine-in QR Ordering):** Quét mã QR định danh bàn có bảo mật chống đặt đơn giả từ xa, duyệt thực đơn theo danh mục/combo, tùy biến món (kích cỡ, topping, ghi chú), nhận gợi ý món từ trợ lý AI, gửi đơn gọi nhiều đợt, gọi nhân viên hỗ trợ tại bàn theo lý do nhanh kèm ghi chú (có cơ chế chống spam), theo dõi tiến độ chế biến/hỗ trợ trực tiếp và hỗ trợ tự thanh toán QR tại bàn (khi nhà hàng bật tính năng).
- **Nghiệp vụ sàn phục vụ & Đơn mang về:** Sơ đồ bàn trực quan theo trạng thái thực tế, hàng đợi yêu cầu bàn thời gian thực giúp nhân viên nhận xử lý và hoàn tất tránh nhận trùng, nhân viên đặt món hộ khách, gộp/tách nhiều bàn vật lý dùng chung một hóa đơn khi khách đi nhóm đông, và quản lý riêng luồng khách mua mang về (Takeaway).
- **Điều phối khu vực Bếp (Kitchen Display System — KDS):** Bảng điều phối trạng thái món trực quan (*Chờ chế biến → Đang nấu → Hoàn tất*), chế độ gom nhiều đơn cùng món để nấu một mẻ, đồng hồ đếm giờ kèm âm thanh cảnh báo khi món chờ quá lâu (> 20 phút), và báo hết món tức thì.
- **Thu ngân & Thanh toán linh hoạt:** Thanh toán tiền mặt có tự động tính tiền thối, thanh toán chuyển khoản VietQR động tự động xác nhận tiền vào mà không cần kiểm tra ảnh chụp màn hình, áp dụng mã giảm giá (voucher) và chương trình khuyến mãi theo khung giờ/ngày, in hóa đơn và tra cứu lịch sử đối soát theo ca.
- **Kiểm soát kho & Định lượng nguyên liệu (BOM):** Quản lý danh mục nguyên liệu, thiết lập định lượng nguyên liệu cho từng món, tự động giữ/trừ tồn kho theo đơn, phiếu nhập/điều chỉnh kho và cảnh báo nguyên liệu sắp hết.
- **Quản trị, Phân quyền & Phân tích kinh doanh:** Phân chia rạch ròi 6 vai trò người dùng, bảo mật 2 lớp (OTP) cho chủ quán, hỗ trợ giao diện sáng/tối và song ngữ Việt – Anh, báo cáo doanh thu/món bán chạy/hiệu suất nhân sự và AI hỗ trợ soạn mô tả món cùng dự báo nhu cầu.

### 2.2 Out-of-Scope (Ngoài phạm vi giai đoạn hiện tại)
- **Tích hợp sàn giao đồ ăn bên thứ ba:** Không đồng bộ đơn tự động với GrabFood, ShopeeFood hay BeFood.
- **Quản trị chuỗi đa pháp nhân / nhượng quyền phức tạp:** Tập trung chuẩn hóa trọn vẹn mô hình vận hành xuất sắc cho từng điểm bán (Single-Store) trước khi mở rộng đa chi nhánh.
- **Kết nối thiết bị phần cứng IoT bếp chuyên sâu:** Không can thiệp trực tiếp vào cảm biến nhiệt độ, lò nướng hay thiết bị bếp tự động.
- **Ví điện tử nạp tiền nội bộ (Closed-loop e-wallet):** Không xây dựng ví lưu ký tiền khách hàng để tránh rào cản pháp lý về trung gian thanh toán.

---

## 3. Biểu mẫu Tiếp nhận Dự án (Completed Project Intake Form)

### 3.1 Thông tin chung

| Hạng mục | Chi tiết tiếp nhận |
|---|---|
| **Tên sản phẩm** | **QROS (Smart QR Order & Restaurant Operating System)** |
| **Nhóm thực hiện** | Alpha Team |
| **Hình thức sản phẩm** | Nền tảng Web đa thiết bị (Mobile cho thực khách, Tablet/Desktop cho nhân sự & bếp) và ứng dụng di động hỗ trợ vận hành |
| **Thời điểm khởi động** | Tháng 8 / 2026 |
| **Phân khúc mục tiêu** | Nhà hàng, quán ăn, quán cafe phục vụ tại bàn quy mô vừa và nhỏ (20 – 50 bàn) |

### 3.2 Bài toán & Cơ hội kinh doanh
- **Vấn đề tiếp nhận:** Vào hai khung giờ cao điểm (11h30 – 13h30 và 18h30 – 20h30), thực khách mất từ **9 – 15 phút** chỉ để chờ gọi món; tỷ lệ đơn sai sót/nhầm ghi chú lên tới **4.8%**; thất thoát nguyên liệu và chênh lệch tồn kho chưa giải trình chiếm **~3.2% giá vốn hàng bán (tương đương 2.5% – 4.0% doanh thu tháng)**.
- **Cơ hội kinh doanh:** Số hóa khâu gọi món và đồng bộ tức thời giữa Bàn – Bếp – Thu ngân – Kho giúp giảm tối thiểu **50% khối lượng di chuyển ghi chép thủ công** của nhân viên phục vụ, tăng vòng quay bàn giờ cao điểm và minh bạch hoàn toàn dòng tiền.

### 3.3 Danh sách các bên liên quan (Stakeholder Matrix)

| Nhóm liên quan | Mã Persona | Kỳ vọng chính | Tiêu chí thành công |
|---|---|---|---|
| **Thực khách tại bàn (Customer)** | `PER-01` | Xem menu có ảnh & giá ngay, gọi đúng khẩu vị không phải vẫy gọi nhiều lần, thanh toán nhanh gọn. | Thời gian từ quét QR tới gửi đơn < 90 giây; nhận đúng món 100%. |
| **Nhân viên phục vụ (Staff)** | `PER-02` | Giảm chạy bàn ghi chép tay, dễ dàng đặt hộ khách lớn tuổi, gộp bàn nhóm đông không nhầm bill, biết ngay khi bếp xong món. | Giảm ≥ 50% bước chân ghi chép thủ công; 0 nhầm bàn khi gộp bàn. |
| **Đầu bếp (Chef)** | `PER-03` | Nhận đơn rõ ràng không rách/mất phiếu giấy, gom nấu nhiều phần cùng món, có cảnh báo món sắp trễ. | Thời gian lên món giảm; tỷ lệ món trễ > 20 phút dưới 1%. |
| **Thu ngân (Cashier)** | `PER-04` | Chốt bill nhanh cả tiền mặt lẫn chuyển khoản QR tự động khớp tiền, không sai lệch két cuối ca. | Thời gian thanh toán < 30 giây; đối soát cuối ca khớp 100%. |
| **Quản lý cửa hàng (Manager)** | `PER-05` | Cập nhật thực đơn/giá/khuyến mãi tức thời, kiểm soát định lượng kho và nắm rõ hiệu suất nhân sự ca làm. | Rút ngắn thời gian kiểm kê kho từ 2 giờ xuống 15 phút; giảm khiếu nại vận hành. |
| **Chủ nhà hàng (Admin / Owner)** | `PER-06` | Minh bạch doanh thu, kiểm soát thất thoát nguyên liệu, bảo mật dữ liệu tài chính và có dự báo nhu cầu để nhập hàng. | Báo cáo lãi/lỗ tức thời; ngăn chặn gian lận nội bộ; tăng 15–25% doanh thu thuần. |

### 3.4 Kỳ vọng chất lượng & Ràng buộc phi chức năng (Product Constraints)
1. **Tốc độ & Khả năng chịu tải giờ cao điểm:** Thao tác tải menu và gửi đơn phản hồi mượt mà (**trung bình < 300ms**); đáp ứng tốt đồng thời **tối thiểu 30–50 phiên bàn/thiết bị hoạt động song song** trong giờ ăn chính tại một nhà hàng.
2. **Đồng bộ thời gian thực:** Đơn hàng từ bàn tới màn hình bếp và trạng thái thanh toán đồng bộ tức thì (**độ trễ mục tiêu < 1 giây**); tự động kết nối lại khi mạng chập chờn.
3. **An toàn & Chống gian lận vận hành:**
   - Mã QR gắn định danh bàn có cơ chế xác thực chống người ngoài quán quét/đặt đơn phá hoại.
   - Phân quyền chặt chẽ theo 6 vai trò; chủ quán bắt buộc xác thực 2 lớp (OTP) khi truy cập tài chính và cấu hình hệ thống.
   - Ngăn tuyệt đối việc gửi trùng đơn khi khách bấm lặp, hoặc trùng hóa đơn/lệch kho khi nhiều nhân viên thao tác cùng lúc.
4. **Trải nghiệm không rào cản:** Tối ưu ưu tiên điện thoại (Mobile-First) cho thực khách không cần đăng nhập; hỗ trợ song ngữ Việt – Anh và chế độ sáng/tối dễ nhìn trong môi trường ánh sáng nhà hàng.

### 3.5 Giả định & Kế hoạch phòng ngừa rủi ro (Assumptions & Risks)

| Giả định / Rủi ro | Mức độ | Phương án ứng phó ở góc độ sản phẩm |
|---|---|---|
| Mạng Wi-Fi tại nhà hàng chập chờn hoặc mất kết nối đột ngột | Cao | Hiển thị cảnh báo mất mạng rõ ràng trên giao diện khách, bếp và thu ngân; nhân viên có thể dùng 4G/5G trên điện thoại/tablet để duy trì nhận đơn liên tục. |
| Thực khách lớn tuổi hoặc không dùng smartphone gặp khó khi quét QR | Trung bình | Cung cấp chế độ nhân viên phục vụ đặt món hộ trực tiếp theo đúng số bàn, vẫn đẩy đơn thẳng xuống bếp như bình thường. |
| Cổng thanh toán chuyển khoản QR gặp sự cố tạm thời từ phía ngân hàng | Thấp | Luôn duy trì luồng thanh toán tiền mặt dự phòng và nút kiểm tra lại trạng thái giao dịch cho thu ngân để không làm ùn tắc quầy thanh toán. |
| Nhân viên lớn tuổi hoặc nhân sự mới ngại làm quen phần mềm | Trung bình | Thiết kế giao diện tối giản thao tác, trực quan bằng màu sắc trạng thái để nhân viên mới thành thạo sau 5–10 phút hướng dẫn. |
