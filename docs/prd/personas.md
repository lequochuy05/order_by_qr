**User Groups & Needs: Comprehensive Personas Matrix**  
***Mô-đun:*** *`personas.md`*  
   
 ***Giai đoạn:*** *User Research & Empathy Mapping*  
   
 ***Dự án:*** *QROS (Smart QR Order & Restaurant Operating System)*  
   
 ***Đối tượng phân tích:*** *6 nhóm vai trò cốt lõi trong hệ sinh thái nhà hàng*  
  
---  
**1. Khung phân loại người dùng (User Role Matrix Overview)**  
Hệ sinh thái QROS được thiết kế xoay quanh mối liên kết chặt chẽ giữa 6 nhóm đối tượng:  
```  
                  ┌────────────────────────────────────────┐  
                   │       ADMIN / OWNER (Chủ nhà hàng)     │  
                   └───────────────────┬────────────────────┘  
                                       │ Quản lý & Giám sát  
                   ┌───────────────────▼────────────────────┐  
                   │   STORE MANAGER (Quản lý cửa hàng)     │  
                   └─────────┬───────────────────┬──────────┘  
                             │                   │  
          ┌──────────────────▼──────┐   ┌────────▼─────────────────┐  
          │     CASHIER (Thu ngân)  │   │  CHEF (Bếp trưởng / Bar) │  
          └─────────┬───────────────┘   └────────┬─────────────────┘  
                    │ Phục vụ & Đối soát         │ Chế biến món  
          ┌─────────▼────────────────────────────▼─────────────────┐  
          │            STAFF (Nhân viên phục vụ sàn)               │  
          └─────────────────────────┬──────────────────────────────┘  
                                    │ Chăm sóc & Tiếp đón  
                   ┌────────────────▼───────────────────────┐  
                   │          CUSTOMER (Thực khách tại bàn)         │  
                   └────────────────────────────────────────┘  
   
```  
  
---  
**2. Chi tiết 6 Nhóm Persona Đại diện**  
**Persona 1: Customer (Thực khách tại bàn)**  
- **Đại diện:** *Nguyễn Hải Nam, 26 tuổi, Chuyên viên Marketing / Khách đi cùng nhóm đồng nghiệp.*  
- **Bối cảnh sử dụng:** Thường dùng bữa trưa gấp gáp tại các nhà hàng đông đúc hoặc liên hoan nhóm vào buổi tối cuối tuần.  
- **Jobs-to-be-Done (JTBD):**  
  - "Khi ngồi vào bàn, tôi muốn xem menu ngay lập tức với hình ảnh rõ ràng và giá niêm yết mà không phải mất thời gian chờ nhân viên mang menu giấy."  
  - "Khi nhóm của tôi ghép bàn với nhóm bàn bên cạnh, tôi muốn tất cả cùng gọi món vào chung một hóa đơn mà không bị lẫn lộn."  
  - "Khi ăn xong, tôi muốn thanh toán bằng mã VietQR trên điện thoại và nhận hóa đơn điện tử ngay lập tức."  
- **Nhu cầu chính (Key Needs):**  
  - Quét mã QR vào ngay thực đơn (Zero-install, không bắt tải app hay đăng ký tài khoản).  
  - Tùy biến món dễ dàng (ghi chú ít đá, ít ngọt, thêm topping).  
  - Trạng thái món cập nhật thời gian thực (đang nấu, đã xong).  
  - Tự động áp mã giảm giá voucher trực tiếp vào giỏ hàng.  
- **Tiêu chí hài lòng (Success Metrics):** Thời gian từ khi quét QR đến khi đơn vào bếp < 90 giây; nhận đúng món theo ghi chú 100%.  
  
---  
**Persona 2: Staff (Nhân viên phục vụ sàn)**  
- **Đại diện:** *Trần Thị Mai, 21 tuổi, Sinh viên làm thêm ca bán thời gian.*  
- **Bối cảnh sử dụng:** Chạy bàn liên tục trong ca trưa cao điểm, phụ trách khu vực 10-15 bàn ăn.  
- **Jobs-to-be-Done (JTBD):**  
  - "Khi khách lớn tuổi hoặc không có điện thoại thông minh, tôi muốn có thể mở giao diện quản lý bàn để đặt món hộ khách nhanh chóng."  
  - "Khi khách yêu cầu ghép bàn để ngồi nhóm đông, tôi muốn thao tác gộp bàn chỉ với 2 lần chạm mà không làm sai lệch đơn hàng hiện có."  
  - "Tôi muốn nhận thông báo tức thì khi bếp nấu xong món để ra nhận đĩa bê đến đúng bàn khách."  
- **Nhu cầu chính (Key Needs):**  
  - Giao diện sơ đồ bàn trực quan (Table Map) phân biệt rõ mã màu: `AVAILABLE` (Xanh), `OCCUPIED` (Cam/Đỏ), `WAITING_FOR_PAYMENT` (Vàng).  
  - Thao tác gộp bàn và chuyển bàn bảo toàn toàn vẹn dữ liệu đơn.  
  - Chuông thông báo âm thanh khi bếp bấm "READY".  
- **Tiêu chí hài lòng:** Giảm ít nhất 50% số bước chân di chuyển không cần thiết; không bị khách phàn nàn vì sót đơn.  
  
---  
**Persona 3: Chef / Kitchen Staff (Bếp trưởng & Nhân viên bếp)**  
- **Đại diện:** *Lê Văn Hùng, 38 tuổi, Bếp trưởng nhà hàng.*  
- **Bối cảnh sử dụng:** Không gian bếp nhiệt độ cao, tiếng ồn lớn, dầu mỡ, nhịp độ làm việc cực kỳ căng thẳng trong giờ ăn chính.  
- **Jobs-to-be-Done (JTBD):**  
  - "Khi có đơn hàng mới từ bàn khách, tôi muốn màn hình KDS phát chuông nhắc và hiển thị rõ tên món, số lượng, bàn nào và ghi chú của khách."  
  - "Khi có 5 bàn cùng gọi món Mì cay cấp độ 2, tôi muốn màn hình tự động gom mẻ (Batch Cooking) để nấu cùng một chảo lớn."  
  - "Khi một món đã chờ quá 20 phút mà chưa hoàn tất, tôi muốn màn hình nhấp nháy đỏ và phát cảnh báo để ưu tiên xử lý trước."  
- **Nhu cầu chính (Key Needs):**  
  - Bảng Kitchen Display System (KDS) 3 cột: `PENDING` -> `COOKING` -> `READY`.  
  - Nút bấm to, thao tác dễ dàng trên màn hình cảm ứng hoặc chuột/bàn phím chống dầu mỡ.  
  - Đồng hồ đếm thời gian chờ thực tế (Elapsed Timers) trên từng món.  
  - Khả năng bấm hủy món nhanh kèm lý do (hết nguyên liệu) để hệ thống tự động thông báo cho khách và thu ngân.  
- **Tiêu chí hài lòng:** Tốc độ ra món trung bình < 10 phút; không có món nào bị quá hạn 25 phút; không sót phiếu giấy.  
  
---  
**Persona 4: Cashier (Nhân viên thu ngân)**  
- **Đại diện:** *Phạm Thanh Thảo, 24 tuổi, Thu ngân quầy thanh toán.*  
- **Bối cảnh sử dụng:** Quầy thanh toán tại lối ra vào, đối mặt với hàng dài khách thanh toán sau khi dùng bữa.  
- **Jobs-to-be-Done (JTBD):**  
  - "Khi khách gọi thanh toán tại bàn hoặc quầy, tôi muốn hiển thị chính xác tổng tiền, danh sách món đã phục vụ, và tiền cọc nếu có."  
  - "Khi khách trả tiền mặt, hệ thống phải tự động tính toán tiền thối để tôi không phải bấm máy tính tay."  
  - "Khi khách quét mã chuyển khoản VietQR PayOS, hệ thống phải tự động lắng nghe Webhook để đổi trạng thái sang 'Đã thanh toán' ngay khi tiền vào tài khoản, không bắt khách đứng chờ chụp màn hình chuyển khoản."  
- **Nhu cầu chính (Key Needs):**  
  - Quick Settlement Modal: Đổi trạng thái bàn sang `WAITING_FOR_PAYMENT` ngay khi khách bấm xin thanh toán.  
  - Bộ công cụ áp dụng voucher: Tự động kiểm tra tính hợp lệ của voucher (ngày hết hạn, số tiền đơn tối thiểu, số lượng còn lại).  
  - Tích hợp in hóa đơn nhiệt khổ 80mm/58mm chuẩn hóa.  
- **Tiêu chí hài lòng:** Thời gian chốt bill < 20 giây; đối soát tiền két cuối ca khớp 100% với báo cáo hệ thống.  
  
---  
**Persona 5: Store Manager (Quản lý cửa hàng)**  
- **Đại diện:** *Hoàng Minh Trí, 32 tuổi, Quản lý điều hành chi nhánh.*  
- **Bối cảnh sử dụng:** Di chuyển liên tục giữa sàn phục vụ, kho và văn phòng quản lý; chịu trách nhiệm KPI doanh số và chi phí.  
- **Jobs-to-be-Done (JTBD):**  
  - "Khi có món mới hoặc điều chỉnh giá bán theo mùa, tôi muốn cập nhật ngay trên trang quản trị và hiển thị tức thì trên QR của tất cả các bàn."  
  - "Tôi muốn khai báo công thức định lượng (BOM) cho từng món để hệ thống tự trừ thịt, rau, gia vị trong kho khi bếp nấu xong."  
  - "Tôi muốn theo dõi báo cáo top món bán chạy và doanh số theo nhân viên trong ngày để khen thưởng hoặc đào tạo lại."  
- **Nhu cầu chính (Key Needs):**  
  - CRUD Danh mục, Món ăn, Combo kèm tùy chọn Options linh hoạt (size, đường, đá, topping).  
  - Quản lý kho: Phiếu nhập kho nguyên liệu, kiểm kê chênh lệch thực tế, cảnh báo chạm ngưỡng an toàn.  
  - AI Assistant (Gemini): Tự động viết mô tả món ăn lôi cuốn và đề xuất kết hợp combo tăng giá trị đơn hàng trung bình (AOV).  
- **Tiêu chí hài lòng:** Giảm thời gian kiểm kê kho cuối ngày từ 2 tiếng xuống 15 phút; kiểm soát hoàn toàn menu và nhân sự ca.  
  
---  
**Persona 6: Admin / Restaurant Owner (Chủ nhà hàng / Ban Giám đốc)**  
- **Đại diện:** *Vũ Quốc Huy, 42 tuổi, Nhà sáng lập & Tổng Giám đốc chuỗi F&B.*  
- **Bối cảnh sử dụng:** Giám sát từ xa qua điện thoại hoặc laptop cá nhân; ra quyết định chiến lược về mở rộng và dòng tiền.  
- **Jobs-to-be-Done (JTBD):**  
  - "Tôi muốn xem biểu đồ doanh thu theo thời gian thực từ bất kỳ đâu, kèm dự báo doanh số của các ngày tiếp theo dựa trên AI để chủ động đặt mua nguyên vật liệu."  
  - "Tôi muốn bảo vệ dữ liệu kinh doanh tối mật bằng cơ chế xác thực hai lớp (2FA OTP) để không bị kẻ xấu đánh cắp quyền truy cập."  
  - "Tôi muốn kiểm soát toàn bộ cấu hình lõi: tài khoản ngân hàng thụ hưởng PayOS, phân quyền tài khoản quản lý và thông tin thương hiệu."  
- **Nhu cầu chính (Key Needs):**  
  - Bảng điều khiển tài chính chuyên sâu (Revenue Stats, Profit Margin, Occupancy Rate).  
  - AI Forecasting: Dự báo nhu cầu khách hàng theo dữ liệu lịch sử và yếu tố thời tiết/cuối tuần.  
  - Bảo mật tối cao: Phân quyền vai trò nghiêm ngặt (`ROLE_ADMIN` độc quyền các tác vụ xóa vĩnh viễn và cấu hình tiền tệ).  
- **Tiêu chí hài lòng:** Nắm bắt chính xác sức khỏe kinh doanh trong 30 giây mở app; ngăn chặn 100% rủi ro thất thoát doanh thu nội bộ.  
