# Product Vision, Strategic Direction & Core Value Pillars

> **Mô-đun:** `vision.md`  
> **Giai đoạn:** Strategic Product Definition (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team

---

## 1. Tuyên ngôn tầm nhìn sản phẩm (Product Vision Statement)

> *"Biến mọi bàn ăn nhà hàng thành một điểm tiếp xúc số thông minh — nơi thực khách gọi món và thanh toán liền mạch không cần chờ đợi; đội ngũ phục vụ và đầu bếp phối hợp nhịp nhàng không giấy tờ; và chủ nhà hàng kiểm soát minh bạch từng dòng tiền, từng gram nguyên liệu theo thời gian thực với sự trợ lực của Trí tuệ Nhân tạo."*

QROS không chỉ là một công cụ tạo mã QR xem thực đơn; QROS được định vị là **hệ điều hành vận hành nhà hàng hợp nhất (Unified Restaurant Operating System)**, khép kín toàn bộ vòng đời trải nghiệm từ lúc thực khách ngồi xuống bàn cho đến khi bàn được giải phóng và dữ liệu kinh doanh được chuyển hóa thành dự báo cho ngày kế tiếp.

---

## 2. Chỉ số Bắc Đẩu (North Star Metric)

Để định hướng thống nhất mọi quyết định về sản phẩm, thiết kế và vận hành, QROS theo đuổi một chỉ số Bắc Đẩu duy nhất:

### The Perfect Table Journey Rate (Tỷ lệ Phiên Phục vụ Hoàn hảo — PTJR)

Được định nghĩa là **tỷ lệ % lượt bàn hoàn tất trọn vẹn bữa ăn đáp ứng đồng thời 4 tiêu chí**:
1. Thời gian từ lúc thực khách quét QR đến khi đơn đầu tiên vào tới bếp **< 90 giây**.
2. **100%** món ăn phục vụ đúng yêu cầu/ghi chú và không có món nào chờ quá **20 phút**.
3. Không phát sinh nhầm lẫn đơn hay hóa đơn (kể cả khi bàn được gộp cho nhóm đông).
4. Quá trình thanh toán và giải phóng bàn hoàn tất trong vòng **30 giây**.

*Mục tiêu chiến lược:* Đạt chỉ số PTJR **≥ 95%** trên các nhà hàng áp dụng QROS.

---

## 3. Ba Trụ cột Giá trị Cốt lõi (Core Value Pillars)

```text
                        ┌────────────────────────────────────────┐
                        │           QROS VALUE ENGINE            │
                        └───────────────────┬────────────────────┘
                                            │
          ┌─────────────────────────────────┼─────────────────────────────────┐
          │                                 │                                 │
 ┌────────▼──────────────┐       ┌──────────▼────────────┐       ┌────────────▼────────────┐
 │ PILLAR 1: SEAMLESS    │       │ PILLAR 2: OPERATIONAL │       │ PILLAR 3: INTELLIGENT   │
 │ DINING EXPERIENCE     │       │ EXCELLENCE IN KITCHEN │       │ FINANCIALS & INVENTORY  │
 ├───────────────────────┤       ├───────────────────────┤       ├─────────────────────────┤
 │ • Quét QR không app   │       │ • Màn hình Bếp KDS    │       │ • VietQR tự đối soát    │
 │ • QR bảo mật tại bàn  │       │ • Nấu gom theo mẻ     │       │ • Trừ kho tự động (BOM) │
 │ • Theo dõi món live   │       │ • Cảnh báo trễ món    │       │ • AI tư vấn & dự báo    │
 │ • Gộp bàn nhóm đông   │       │ • Sơ đồ bàn đồng bộ   │       │ • 6 vai trò & 2FA OTP   │
 └───────────────────────┘       └───────────────────────┘       └─────────────────────────┘
```

### Trụ cột 1: Trải nghiệm ẩm thực liền mạch (Seamless Dining Experience)
- Trao quyền chủ động hoàn toàn cho thực khách: quét QR mở ngay thực đơn trên trình duyệt, không cài ứng dụng, không đăng ký tài khoản.
- Hình ảnh trực quan, tự chọn kích cỡ/topping/ghi chú chính xác, có trợ lý AI gợi ý món ăn theo sở thích và hỗ trợ song ngữ Việt – Anh.
- Giải quyết triệt để tình huống đi nhóm đông bằng cơ chế **gộp bàn chung hóa đơn**: mọi thành viên trong nhóm đều theo dõi và gọi thêm món trên cùng một phiên bàn.

### Trụ cột 2: Vận hành Sàn & Bếp tinh gọn (Operational Excellence in Kitchen & Floor)
- Thay thế hoàn toàn phiếu order giấy dễ rách, ướt và thất lạc.
- Chuẩn hóa màn hình Bếp (KDS) theo 3 bước rõ ràng (*Chờ chế biến → Đang nấu → Hoàn tất*) kết hợp **nấu gom theo mẻ** và cảnh báo quá giờ 20 phút bằng màu sắc + âm thanh.
- Sơ đồ bàn và hàng đợi đơn mang về đồng bộ tức thời giữa phục vụ, bếp và thu ngân.

### Trụ cột 3: Tài chính minh bạch & Quản trị thông minh (Intelligent Financials & Inventory)
- Thanh toán không chạm qua mã VietQR động đúng số tiền hóa đơn, tự động xác nhận ngay khi tiền về tài khoản (hỗ trợ cả thu ngân thao tác lẫn khách tự thanh toán tại bàn khi chủ quán cho phép).
- Tự động trừ nguyên liệu theo công thức định lượng món (BOM), ghi vết đầy đủ mọi lịch sử thay đổi trạng thái đơn hàng, món ăn và biến động kho để loại bỏ thất thoát nội bộ.
- Ứng dụng Trí tuệ Nhân tạo (AI) tạo mô tả món ăn hấp dẫn, tư vấn món tại bàn và dự báo doanh thu/nguyên liệu cần chuẩn bị.
- Bảo vệ chặt chẽ dữ liệu tài chính bằng phân quyền 6 vai trò và xác thực 2 lớp (OTP) cho chủ quán.

---

## 4. Bảng phân phối giá trị cho 6 bên liên quan (Value Proposition Canvas)

| Vai trò | Giá trị nhận được từ QROS | Tác động kinh doanh trực tiếp |
|---|---|---|
| **Thực khách (`PER-01`)** | Không chờ vẫy nhân viên; gọi món đúng khẩu vị 100%; theo dõi món trực tiếp và thanh toán QR tiện lợi. | Tăng ~35% mức độ hài lòng; tăng ~20% khả năng quay lại. |
| **Nhân viên phục vụ (`PER-02`)** | Giảm ≥ 50% di chuyển ghi chép thủ công; gộp bàn và đặt món hộ dễ dàng; tập trung chăm sóc khách. | Giảm kiệt sức giờ cao điểm; giảm tỷ lệ nghỉ việc của nhân sự ca. |
| **Đầu bếp (`PER-03`)** | Không gian bếp gọn gàng không giấy vụn; gom mẻ nấu nhanh; nhận cảnh báo sớm món sắp trễ. | Rút ngắn thời gian lên món; giảm mạnh món làm sai phải hủy. |
| **Thu ngân (`PER-04`)** | Tự động tính tiền thối và tự động khớp chuyển khoản VietQR; kiểm soát voucher chặt chẽ. | Chốt hóa đơn trong 15–30 giây; đối soát cuối ca khớp 100%. |
| **Quản lý cửa hàng (`PER-05`)** | Cập nhật thực đơn/khuyến mãi tức thì; kiểm soát định lượng kho và hiệu suất nhân viên theo thời gian thực. | Giảm thời gian kiểm kê kho từ 2 giờ xuống 15 phút; giảm khiếu nại vận hành. |
| **Chủ nhà hàng (`PER-06`)** | Minh bạch doanh thu, kiểm soát thất thoát nguyên liệu, bảo mật 2 lớp và có dự báo AI hỗ trợ nhập hàng. | Tăng 15–25% doanh thu thuần; tối ưu ~20% chi phí vận hành hàng tháng. |

---

## 5. Mục tiêu chiến lược định lượng (Strategic OKRs)

### Objective 1: Tăng công suất phục vụ bàn ăn và loại bỏ thời gian chết
- **KR 1.1:** Thời gian từ lúc khách ngồi bàn đến khi bếp nhận đơn **< 90 giây**.
- **KR 1.2:** Tăng tốc độ quay vòng bàn giờ cao điểm thêm tối thiểu **25%**.
- **KR 1.3:** Tỷ lệ món ăn chờ quá hạn (> 20 phút) duy trì **< 1.0%**.

### Objective 2: Triệt tiêu sai sót vận hành và thất thoát dòng tiền / nguyên liệu
- **KR 2.1:** Tỷ lệ nhầm món / nhầm bàn / thiếu ghi chú khẩu vị giảm xuống **gần 0%**.
- **KR 2.2:** Chênh lệch giữa sổ kho hệ thống và kiểm kê thực tế **< 0.5%**.
- **KR 2.3:** Tỷ lệ đối soát tự động thành công các giao dịch VietQR đạt **≥ 99.9%**.

### Objective 3: Nâng cao giá trị đơn hàng trung bình (AOV) bằng AI & trải nghiệm số
- **KR 3.1:** 100% thực đơn có mô tả hấp dẫn, chuẩn hóa (được AI hỗ trợ biên soạn).
- **KR 3.2:** Tăng **15–18%** tỷ lệ khách gọi thêm Combo/Topping thông qua gợi ý thông minh tại bàn.
- **KR 3.3:** Độ chính xác của dự báo nhu cầu & doanh thu bằng AI đạt **> 85%** so với thực tế.
