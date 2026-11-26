# Source Preparation: Field Research, Evidence & Operational Baseline

> **Mô-đun:** `source-preparation.md`  
> **Giai đoạn:** Discovery & Evidence Preparation (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team

---

## 1. Mục tiêu thu thập dữ liệu đầu vào (Discovery Inputs)

Trước khi định hình giải pháp sản phẩm QROS, vai trò Product Owner tiến hành tổng hợp thông tin thực địa từ **3 nguồn đầu vào chính** nhằm đảm bảo mọi quyết định tính năng đều dựa trên bằng chứng vận hành thực tế (Evidence-based Product Discovery), tránh suy đoán chủ quan:

1. **Khảo sát & quan sát thực tế tại điểm bán (Field Observation):** Quan sát quy trình phục vụ vào 2 khung giờ cao điểm (trưa 11h30–13h30, tối 18h30–20h30) tại các quán ăn, nhà hàng casual dining quy mô 20–40 bàn.
2. **Phỏng vấn nhanh các nhóm vai trò (Stakeholder Interviews):** Ghi nhận trực tiếp khó khăn hàng ngày của thực khách, nhân viên chạy bàn, bếp trưởng, thu ngân và chủ quán.
3. **Phân tích dữ liệu vận hành mẫu (30 ngày tham chiếu):** Mô phỏng và kiểm chứng cấu trúc giao dịch của một nhà hàng tiêu chuẩn (~1.200 đơn hàng/tháng) để xác định mức độ ưu tiên tính năng.

---

## 2. Bản đồ thực thể nghiệp vụ cốt lõi (Conceptual Domain Model)

Dưới góc nhìn sản phẩm, toàn bộ hoạt động của một nhà hàng vận hành quanh **8 khái niệm nghiệp vụ trung tâm** và mối liên kết giữa chúng (làm đầu vào để Design dựng màn hình, BA viết quy tắc và SE thiết kế cơ sở dữ liệu sau này):

```text
[Bàn ăn vật lý] ──(gộp/mở)──► [Phiên phục vụ] ──► [Đơn hàng (Nhiều đợt gọi món)]
                                                         │
        ┌────────────────────────────────────────────────┼────────────────────────┐
        ▼                                                ▼                        ▼
[Chi tiết món đặt]                              [Ưu đãi / Voucher]       [Giao dịch Thanh toán]
  │  (chọn từ Thực đơn / Combo & Tùy chọn)                                (Tiền mặt / VietQR)
  ▼
[Định lượng công thức (BOM)] ──► [Tồn kho nguyên liệu & Biến động kho]
```

### Ý nghĩa nghiệp vụ của từng nhóm thực thể
1. **Bàn ăn & Phiên phục vụ:** Phân tách rõ *bàn vật lý* với *phiên ăn của khách* để một nhóm khách đông ghép 2–4 bàn vẫn dùng chung một phiên phục vụ và một hóa đơn duy nhất mà không bị lẫn lộn.
2. **Thực đơn, Danh mục, Combo & Tùy chọn:** Hỗ trợ bán món lẻ, gói combo ưu đãi, cùng các lựa chọn bắt buộc/tùy chọn (kích cỡ, độ ngọt/đá, topping tính thêm tiền) và ghi chú riêng.
3. **Đơn hàng & Đợt gọi món:** Một bàn có thể gọi món nhiều lần trong suốt bữa ăn (khai vị, món chính, gọi thêm đồ uống); mỗi lần bấm gửi tạo thành một đợt gọi đẩy xuống bếp nhưng vẫn cộng dồn vào cùng một hóa đơn cuối cùng.
4. **Tiến độ chế biến tại Bếp:** Mỗi món đi qua vòng đời rõ ràng (*Chờ nhận → Đang nấu → Hoàn tất*, hoặc *Hủy kèm lý do hết hàng*) để cả bếp, phục vụ và thực khách đều nhìn thấy chung một trạng thái.
5. **Nguyên liệu & Định lượng (BOM):** Mỗi món ăn gắn với danh sách nguyên liệu tiêu hao tiêu chuẩn; khi chế biến sẽ tự động cập nhật tồn kho và ghi lại sổ cái nhập/xuất/điều chỉnh.
6. **Khuyến mãi & Mã giảm giá:** Hỗ trợ cả chương trình giảm giá tự động theo ngày/khung giờ lẫn mã voucher có điều kiện đơn tối thiểu, thời hạn và giới hạn lượt dùng.
7. **Thanh toán & Đối soát:** Quản lý giao dịch tiền mặt (kèm tiền khách đưa, tiền thối) và chuyển khoản VietQR tự động khớp tiền, liên kết trực tiếp với việc đóng phiên và giải phóng bàn.
8. **Nhân sự & Phân quyền:** Tổ chức rõ 5 vai trò nội bộ (*Phục vụ, Bếp, Thu ngân, Quản lý, Chủ quán*) cùng nhóm *Thực khách tại bàn* (không cần tài khoản) để kiểm soát trách nhiệm và bảo mật tài chính.

---

## 3. Bằng chứng định lượng làm cơ sở ưu tiên tính năng (Evidence Summary)

Tổng hợp từ tập số liệu vận hành tham chiếu trong 30 ngày (~1.200 lượt đơn của nhà hàng 25–35 bàn):

| Chỉ số quan sát thực tế | Kết quả ghi nhận | Hàm ý định hướng sản phẩm cho QROS |
|---|---|---|
| **Tập trung lưu lượng giờ cao điểm** | **68%** tổng lượng đơn dồn vào 2 khung giờ: `11h45–13h15` và `19h00–20h45` | Cần tối ưu tốc độ đặt món tự phục vụ và màn hình bếp gom mẻ để không bị vỡ trận vào giờ vàng. |
| **Xu hướng thanh toán không tiền mặt** | **72%** khách ưu tiên chuyển khoản QR; **28%** trả tiền mặt | Thanh toán VietQR tự động đối soát (kèm tùy chọn khách tự thanh toán tại bàn) là tính năng lõi, không phải tính năng phụ. |
| **Tần suất ghép bàn nhóm đông** | **4–6 nhóm/ngày** (~12% số lượt khách nhưng đóng góp tới **31% doanh thu**) | Tính năng **Gộp bàn dùng chung phiên & hóa đơn** là yêu cầu sống còn đối với nhà hàng Việt Nam. |
| **Tỷ lệ món chờ quá 20 phút** | **8.4%** khi dùng phiếu giấy (mục tiêu sau khi có KDS + cảnh báo âm thanh: **< 1.2%**) | Màn hình bếp cần đồng hồ đếm thời gian chờ và cảnh báo trực quan + âm thanh cho món quá hạn. |
| **Sai sót order & thất thoát kho** | **4.8%** đơn sai/thiếu ghi chú; **~3.2% giá vốn** thất thoát không giải trình | Khách tự chọn topping/ghi chú trên điện thoại kết hợp trừ kho tự động theo định lượng (BOM) là đòn bẩy lợi nhuận lớn nhất. |

---

## 4. Ràng buộc chuyển giao sang các khâu tiếp theo
- **Bàn giao cho Design (`02-design`):** Giao diện phải được thiết kế theo đặc thù bối cảnh sử dụng (khách cầm 1 tay trên điện thoại, bếp nhìn từ xa trong môi trường ánh sáng yếu/dầu mỡ, thu ngân thao tác nhanh trên tablet), có hệ thống màu trạng thái nhất quán và hỗ trợ song ngữ + sáng/tối.
- **Bàn giao cho BA (`03-ba`):** Cần cụ thể hóa toàn bộ ngoại lệ thực tế (mất mạng, bấm gửi đơn 2 lần, hết nguyên liệu giữa chừng, hủy món, gộp bàn đang có đơn, khách đổi voucher khi đang mở mã QR).
- **Bàn giao cho Kỹ thuật (`04-tech`):** Mọi lựa chọn kiến trúc, nền tảng công nghệ, thiết kế cơ sở dữ liệu và cơ chế đồng bộ thời gian thực sẽ được đặc tả chuyên sâu tại [SYSTEM_DESIGN.md](../04-tech/SYSTEM_DESIGN.md).
