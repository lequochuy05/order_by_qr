# User Groups & Needs: Comprehensive Personas Matrix

> **Mô-đun:** `personas.md`  
> **Giai đoạn:** User Research & Empathy Mapping (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Đối tượng phân tích:** 6 nhóm vai trò cốt lõi (`PER-01` → `PER-06`)

---

## 1. Khung phân loại người dùng (User Role Hierarchy Overview)

Hệ sinh thái QROS được thiết kế xoay quanh mối liên kết chặt chẽ giữa **6 nhóm người dùng**:

```text
                   ┌────────────────────────────────────────────┐
                   │   PER-06: ADMIN / OWNER (Chủ nhà hàng)     │
                   └─────────────────────┬──────────────────────┘
                                         │ Quản trị & Chiến lược
                   ┌─────────────────────▼──────────────────────┐
                   │   PER-05: MANAGER (Quản lý cửa hàng)       │
                   └──────────┬───────────────────────┬─────────┘
                              │                       │
           ┌──────────────────▼────────┐   ┌──────────▼─────────────────┐
           │ PER-04: CASHIER (Thu ngân)│   │ PER-03: CHEF (Đầu bếp KDS) │
           └──────────┬────────────────┘   └──────────┬─────────────────┘
                      │ Đối soát & Thanh toán         │ Chế biến & Ra món
           ┌──────────▼───────────────────────────────▼─────────────────┐
           │        PER-02: STAFF (Nhân viên phục vụ sàn)               │
           └─────────────────────────────┬──────────────────────────────┘
                                         │ Đón tiếp & Hỗ trợ tại bàn
                   ┌─────────────────────▼──────────────────────┐
                   │     PER-01: CUSTOMER (Thực khách tại bàn)  │
                   └────────────────────────────────────────────┘
```

---

## 2. Chi tiết 6 Nhóm Persona Đại diện (`PER-01` → `PER-06`)

### `PER-01` — Customer (Thực khách tại bàn)
- **Đại diện:** *Nguyễn Hải Nam, 26 tuổi, Chuyên viên Marketing — thường ăn trưa gấp với đồng nghiệp hoặc liên hoan nhóm cuối tuần.*
- **Bối cảnh sử dụng:** Cầm điện thoại 1 tay tại bàn ăn đông đúc, không muốn cài thêm ứng dụng hay điền form đăng ký tài khoản.
- **Jobs-to-be-Done (JTBD):**
  - *"Khi ngồi vào bàn, tôi muốn xem ngay thực đơn có hình ảnh, giá rõ ràng và tình trạng còn/hết món mà không phải chờ nhân viên mang menu giấy."*
  - *"Khi nhóm tôi ghép nhiều bàn ăn chung, tôi muốn mọi người cùng gọi món vào chung một hóa đơn và gọi thêm món bất cứ lúc nào mà không bị nhầm."*
  - *"Khi cần thêm nước, chén đũa hay dọn bàn giữa bữa, tôi muốn bấm gọi nhân viên ngay trên màn hình bàn kèm lý do cụ thể thay vì phải vẫy tay chờ trong không gian ồn ào."*
  - *"Khi ăn xong, tôi muốn xem rõ tạm tính, áp mã giảm giá và thanh toán nhanh bằng VietQR ngay trên điện thoại hoặc tại quầy."*
- **Nhu cầu chính (Key Needs):**
  - Quét QR vào thẳng thực đơn của bàn (Zero-install, không yêu cầu đăng nhập).
  - Tùy biến kích cỡ, topping, ghi chú khẩu vị và có trợ lý AI gợi ý món theo sở thích/ngân sách.
  - Gọi nhân viên hỗ trợ theo lý do nhanh kèm ghi chú ngắn và thấy rõ khi có nhân viên đang tới bàn.
  - Theo dõi trạng thái món theo thời gian thực (*Đang chờ / Đang nấu / Đã xong*).
  - Hỗ trợ giao diện song ngữ Việt – Anh cho khách nước ngoài.
- **Tiêu chí hài lòng:** Từ lúc quét QR đến khi gửi đơn < 90 giây; nhận đúng món theo ghi chú 100%.

---

### `PER-02` — Staff (Nhân viên phục vụ sàn)
- **Đại diện:** *Trần Thị Mai, 21 tuổi, Nhân viên phục vụ ca trưa/tối phụ trách khu vực 10–15 bàn.*
- **Bối cảnh sử dụng:** Di chuyển liên tục trên sàn phục vụ, cần thao tác nhanh bằng điện thoại hoặc máy tính bảng.
- **Jobs-to-be-Done (JTBD):**
  - *"Khi gặp khách lớn tuổi hoặc không dùng smartphone, tôi muốn đặt món hộ ngay theo đúng số bàn để đơn vẫn vào bếp đồng bộ."*
  - *"Khi nhóm khách đông muốn ghép bàn, tôi muốn gộp bàn chỉ trong vài chạm mà không làm mất hay lệch các món đã gọi."*
  - *"Khi khách mua mang về ghé quầy, tôi muốn tạo đơn mang về riêng mà không chiếm bàn ăn tại chỗ."*
  - *"Khi nhiều bàn cùng cần hỗ trợ giờ cao điểm, tôi muốn xem hàng đợi yêu cầu bàn rõ lý do và nhận xử lý 1 chạm để không bỏ sót khách và không bị 2 nhân viên cùng chạy ra 1 bàn."*
- **Nhu cầu chính (Key Needs):**
  - Sơ đồ bàn trực quan phân biệt 4 trạng thái bằng màu sắc rõ ràng: **Trống**, **Đang có khách**, **Chờ thanh toán**, và **Tạm ngưng phục vụ**.
  - Hàng đợi yêu cầu hỗ trợ bàn theo thời gian thực, phân công nhận xử lý duy nhất cho từng người và báo xong nhanh gọn.
  - Thao tác gộp bàn, hủy gộp và đặt món hộ nhanh gọn.
  - Thông báo ngay khi bếp hoàn tất món để ra bưng đúng bàn.
- **Tiêu chí hài lòng:** Giảm ít nhất 50% số bước chân ghi chép thủ công; không nhầm bàn khi giao món.

---

### `PER-03` — Chef / Kitchen Staff (Bếp trưởng & Nhân viên bếp)
- **Đại diện:** *Lê Văn Hùng, 38 tuổi, Bếp trưởng điều phối khu vực bếp nóng/bar.*
- **Bối cảnh sử dụng:** Môi trường bếp nóng, ồn, nhiều dầu mỡ; cần nhìn rõ từ xa và bấm thao tác lớn, ít bước.
- **Jobs-to-be-Done (JTBD):**
  - *"Khi có đơn mới từ bàn hoặc đơn mang về, tôi muốn màn hình bếp phát chuông và hiển thị rõ món, số lượng, bàn nào và ghi chú dị ứng/khẩu vị."*
  - *"Khi nhiều bàn cùng gọi một món giống nhau, tôi muốn màn hình tự gom tổng số lượng để nấu chung một mẻ cho nhanh."*
  - *"Khi món nào chờ quá 20 phút, tôi muốn hệ thống nổi bật cảnh báo để bếp ưu tiên xử lý ngay."*
  - *"Đầu ca nhập hàng hoặc khi nguyên liệu hao hụt, tôi muốn cập nhật nhanh số lượng kho ngay từ khu vực bếp."*
- **Nhu cầu chính (Key Needs):**
  - Màn hình bếp (KDS) chia 3 cột trực quan: **Chờ chế biến → Đang nấu → Hoàn tất**, kèm chế độ xem gom món theo mẻ.
  - Đồng hồ đếm thời gian chờ và cảnh báo âm thanh + màu sắc khi quá giờ.
  - Nút báo hết món / hủy món kèm lý do để thông báo ngay ra sàn và cho khách.
  - Theo dõi cảnh báo thiếu nguyên liệu và cập nhật nhập/điều chỉnh tồn kho.
- **Tiêu chí hài lòng:** Thời gian ra món trung bình < 10 phút; tỷ lệ món quá hạn 20 phút dưới 1%; không mất phiếu đơn.

---

### `PER-04` — Cashier (Nhân viên thu ngân)
- **Đại diện:** *Phạm Thanh Thảo, 24 tuổi, Thu ngân quầy thanh toán.*
- **Bối cảnh sử dụng:** Đứng tại quầy giờ tan tầm, nhiều bàn cùng gọi thanh toán một lúc, áp lực không được sai tiền két.
- **Jobs-to-be-Done (JTBD):**
  - *"Khi khách thanh toán tiền mặt, hệ thống cần tự tính tiền thối chính xác khi tôi nhập tiền khách đưa và cảnh báo nếu còn món chưa ra xong."*
  - *"Khi khách chuyển khoản VietQR, hệ thống cần sinh mã QR đúng số tiền hóa đơn và tự động xác nhận 'Đã thanh toán' ngay khi tiền vào tài khoản."*
  - *"Cuối ca làm, tôi muốn tra cứu lại toàn bộ lịch sử hóa đơn theo ca/phương thức thanh toán để đối soát két nhanh gọn."*
- **Nhu cầu chính (Key Needs):**
  - Màn hình thanh toán nhanh hiển thị đủ món, khuyến mãi tự động và kiểm tra mã giảm giá (voucher) hợp lệ.
  - Tách biệt rõ quyền thu tiền (chỉ Thu ngân trở lên mới được chốt thanh toán, không lẫn với phục vụ).
  - In hóa đơn rõ ràng và tra cứu lịch sử hóa đơn trong ngày/theo ca.
- **Tiêu chí hài lòng:** Thời gian chốt hóa đơn < 30 giây; đối soát tiền mặt & chuyển khoản cuối ca khớp 100%.

---

### `PER-05` — Store Manager (Quản lý cửa hàng)
- **Đại diện:** *Hoàng Minh Trí, 32 tuổi, Quản lý vận hành chi nhánh.*
- **Bối cảnh sử dụng:** Di chuyển giữa sàn, kho và bàn làm việc; chịu trách nhiệm doanh số, chất lượng món, chi phí nguyên liệu và lịch ca nhân sự.
- **Jobs-to-be-Done (JTBD):**
  - *"Khi ra món mới, đổi giá hoặc tạo combo/khuyến mãi theo khung giờ, tôi muốn cập nhật một lần là hiển thị ngay trên QR của mọi bàn."*
  - *"Tôi muốn thiết lập định lượng nguyên liệu cho từng món để kiểm soát hao hụt mà không phải cộng trừ tay cuối ngày."*
  - *"Tôi muốn xem nhanh món nào bán chạy nhất và hiệu suất xử lý đơn của từng nhân viên để điều phối ca làm hợp lý."*
- **Nhu cầu chính (Key Needs):**
  - Quản trị trọn gói Danh mục, Thực đơn, Combo, Tùy chọn/Topping, Voucher, Chương trình khuyến mãi và tài khoản nhân sự.
  - Quản lý định lượng món (BOM) và kiểm soát cảnh báo tồn kho.
  - Trợ lý AI hỗ trợ soạn mô tả món hấp dẫn và gợi ý combo bán kèm.
- **Tiêu chí hài lòng:** Giảm thời gian kiểm kê kho cuối ngày từ 2 giờ xuống 15 phút; cập nhật thực đơn tức thời không cần in lại menu.

---

### `PER-06` — Admin / Restaurant Owner (Chủ nhà hàng)
- **Đại diện:** *Vũ Quốc Huy, 42 tuổi, Chủ sở hữu nhà hàng.*
- **Bối cảnh sử dụng:** Theo dõi hoạt động từ xa qua laptop hoặc điện thoại; ưu tiên tính minh bạch tài chính, bảo mật tối đa và tầm nhìn mở rộng.
- **Jobs-to-be-Done (JTBD):**
  - *"Tôi muốn nắm được doanh thu thực tế từng ngày/tháng ngay khi mở ứng dụng và xem dự báo nhu cầu bằng AI để lên kế hoạch nhập hàng."*
  - *"Tôi muốn tài khoản quản trị tối cao được bảo vệ bằng xác thực 2 lớp (OTP) để không ai có thể tự ý sửa thông tin tài khoản nhận tiền hay xóa dữ liệu đơn hàng."*
  - *"Tôi muốn chủ động bật hoặc tắt quyền cho phép khách tự thanh toán tại bàn tùy theo mô hình vận hành từng giai đoạn."*
- **Nhu cầu chính (Key Needs):**
  - Báo cáo doanh thu chuyên sâu và dự báo doanh số/nguyên liệu bằng AI (giới hạn riêng cho Chủ quán).
  - Cấu hình toàn diện nhà hàng: thương hiệu, logo, mẫu hóa đơn, kết nối thanh toán, bật/tắt tự thanh toán tại bàn, chính sách thông báo và bảo mật.
  - Toàn quyền kiểm soát các thao tác nhạy cảm và bảo mật xác thực 2 lớp.
- **Tiêu chí hài lòng:** Nắm rõ sức khỏe kinh doanh trong 30 giây; giảm tối đa rủi ro thất thoát doanh thu nội bộ.
