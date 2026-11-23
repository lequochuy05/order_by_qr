# Business Model & Service Ecosystem Blueprint

> **Mô-đun:** `blueprint.md`  
> **Giai đoạn:** Business Architecture & Service Blueprint (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team

---

## 1. Business Model Canvas (Mô hình kinh doanh tổng thể)

QROS được định hướng phát triển theo mô hình **B2B SaaS** tinh gọn cho nhà hàng vừa và nhỏ, kết hợp các gói tính năng giá trị gia tăng (AI & Thanh toán không tiền mặt):

```text
┌──────────────────┬─────────────────┬─────────────────┬─────────────────┬──────────────────┐
│ KEY PARTNERS     │ KEY ACTIVITIES  │ VALUE           │ CUSTOMER        │ CUSTOMER         │
│ (Đối tác chính)  │ (Hoạt động cốt  │ PROPOSITIONS    │ RELATIONSHIPS   │ SEGMENTS         │
│                  │  lõi)           │ (Giá trị cốt    │ (Quan hệ KH)    │ (Phân khúc KH)   │
│ • Đối tác cổng   │ • Phát triển &  │  lõi)           │ • Tự triển khai │ • Quán ăn & Cafe │
│   thanh toán     │   tối ưu sản    │ • QR gọi món    │   nhanh chóng   │   quy mô vừa/nhỏ │
│   VietQR         │   phẩm F&B      │   không cài app │ • Hỗ trợ vận    │   (20 – 50 bàn)  │
│ • Nhà cung cấp   │ • Đảm bảo vận   │ • Bếp KDS gom   │   hành ca cao   │ • Nhà hàng Lẩu,  │
│   nền tảng AI    │   hành ổn định  │   mẻ nấu        │   điểm          │   Nướng, Nhậu    │
│ • Đơn vị cung    │   giờ cao điểm  │ • Gộp bàn nhóm  │ • Cộng đồng chủ │   (hay ghép bàn) │
│   cấp máy in     │ • Chăm sóc &    │   đông liền mạch│   quán F&B      │ • Chuỗi Casual   │
│   hóa đơn/tablet │   đào tạo quán  │ • Tự trừ kho BOM├─────────────────┤   Dining muốn    │
│                  ├─────────────────┤ • Chốt bill 1s  │ CHANNELS        │   chuẩn hóa      │
│                  │ KEY RESOURCES   │ • AI tư vấn &   │ (Kênh tiếp cận) │   vận hành       │
│                  │ (Nguồn lực)     │   dự báo        │ • Tiếp thị số   │                  │
│                  │ • Nền tảng QROS │                 │ • Tư vấn bán    │                  │
│                  │ • Đội ngũ sản   │                 │   hàng trực tiếp│                  │
│                  │   phẩm & kỹ sư  │                 │ • Lan tỏa tự    │                  │
│                  │   am hiểu F&B   │                 │   nhiên qua QR  │                  │
│                  │ • Tri thức quy  │                 │   thực khách    │                  │
│                  │   trình nhà hàng│                 │                 │                  │
├──────────────────┴─────────────────┴─────────────────┴─────────────────┴──────────────────┤
│ COST STRUCTURE (Cơ cấu chi phí)                      │ REVENUE STREAMS (Dòng doanh thu)   │
│ • Hạ tầng vận hành đám mây & lưu trữ hình ảnh        │ • Phí thuê bao phần mềm định kỳ    │
│ • Chi phí dịch vụ AI & gửi thông báo/xác thực        │   theo tháng / năm theo quy mô bàn │
│ • Chi phí nghiên cứu, phát triển sản phẩm & bảo mật  │ • Gói mở rộng AI phân tích & dự báo│
│ • Chi phí tiếp thị, bán hàng & hỗ trợ khách hàng     │ • Dịch vụ triển khai & đào tạo quán│
└──────────────────────────────────────────────────────┴────────────────────────────────────┘
```

---

## 2. Service Blueprint (Bản thiết kế Dòng chảy Dịch vụ Nhà hàng)

Thay vì thiết kế chi tiết kỹ thuật (thuộc [SYSTEM_DESIGN.md](../04-tech/SYSTEM_DESIGN.md)), tầng PO xác lập rõ **Service Blueprint** — mối phối hợp giữa những gì khách nhìn thấy (*Frontstage*), công việc phía sau của nhân sự (*Backstage*) và cơ chế tự động hóa hỗ trợ:

| Lớp tương tác | Bước 1: Vào bàn & Mở menu | Bước 2: Chọn món & Gộp bàn | Bước 3: Bếp chế biến | Bước 4: Phục vụ, Hỗ trợ & Mang về | Bước 5: Thanh toán & Rời bàn |
|---|---|---|---|---|---|
| **Hành động Khách hàng** *(Customer Actions)* | Quét mã QR trên bàn bằng camera điện thoại | Lướt menu có ảnh, hỏi AI gợi ý, chọn topping/ghi chú, bấm gửi đơn (có thể gọi thêm nhiều đợt) | Theo dõi tiến độ từng món trực tiếp trên điện thoại (*Đang chờ → Đang nấu → Hoàn tất*) | Bấm gọi nhân viên theo lý do nhanh + ghi chú khi cần hỗ trợ và xem phản hồi nhận xử lý; nhận món nóng đúng bàn hoặc đặt mang về tại quầy | Xem tạm tính, áp mã giảm giá, thanh toán tiền mặt hoặc quét VietQR tại bàn/quầy |
| **Tiếp xúc trực tiếp** *(Frontstage — Staff & Cashier)* | Đón tiếp khách, đặt món hộ nếu khách lớn tuổi không dùng smartphone | Thao tác gộp nhiều bàn cho nhóm đông khi khách yêu cầu | — | Theo dõi hàng đợi yêu cầu bàn, nhận xử lý (1 người nhận mỗi yêu cầu) và xác nhận hoàn tất khi đã hỗ trợ xong; bưng món đến đúng bàn (kể cả bàn gộp) | Thu ngân mở màn hình thanh toán, thu tiền mặt (tính tiền thối) hoặc hiển thị VietQR & in hóa đơn |
| **Hậu trường vận hành** *(Backstage — Chef & Manager)* | Quản lý bật/tắt trạng thái bàn, cập nhật giá/món mới hiển thị tức thì | — | Đầu bếp xem bảng KDS, gom nấu chung các món giống nhau, xử lý cảnh báo món quá 20 phút, cập nhật nhập/điều chỉnh kho | Bếp bấm hoàn tất món hoặc báo hết hàng kèm lý do; Quản lý giám sát thời gian phản hồi yêu cầu bàn giờ cao điểm | Quản lý/Chủ quán xem báo cáo doanh thu, món bán chạy, hiệu suất nhân sự & dự báo nhập kho |
| **Hệ thống hỗ trợ tự động** *(Support Processes)* | Xác thực bàn hợp lệ & chống đặt đơn giả từ xa; mở phiên phục vụ | Chống gửi trùng đơn khi khách bấm lặp; đồng bộ nhóm bàn dùng chung hóa đơn | Đồng bộ đơn tức thì vào bếp; tự giữ/trừ nguyên liệu theo công thức món & cảnh báo sắp hết kho | Chống khách bấm gọi nhân viên spam/trùng lặp; phát tín hiệu hàng đợi tức thì, khóa tránh 2 nhân viên nhận trùng; đồng bộ sơ đồ 4 trạng thái bàn & đơn mang về | Tự xác nhận chuyển khoản VietQR, tự đóng phiên, khép các yêu cầu bàn còn mở & giải phóng toàn bộ bàn (gồm bàn gộp) |

---

## 3. Nguyên tắc Phân tầng Bảo mật & Quản trị Vận hành (Business Governance)

Dưới góc độ quản trị rủi ro kinh doanh của Chủ nhà hàng (`PER-06`), sản phẩm phải tuân thủ 4 nguyên tắc bất biến:
1. **Phân quyền theo đúng chức trách (Separation of Duties):**
   - Thực khách chỉ thao tác trong phạm vi bàn đang ngồi.
   - Phục vụ chỉ chăm sóc sàn & gộp bàn, **không** được chốt thu tiền.
   - Đầu bếp điều phối món và cập nhật nhập/điều chỉnh kho thực tế, **không** được sửa công thức giá vốn hay xóa dữ liệu.
   - Thu ngân xử lý thanh toán và đối soát hóa đơn theo ca.
   - Quản lý điều hành thực đơn, khuyến mãi, nhân sự và báo cáo vận hành.
   - Chủ nhà hàng độc quyền báo cáo doanh thu chi tiết, dự báo AI, xóa dữ liệu nhạy cảm và cấu hình hệ thống.
2. **Bảo vệ tài khoản chủ quán bằng xác thực 2 lớp (2FA):** Ngăn chặn rủi ro bị chiếm quyền làm sai lệch thông tin nhận tiền chuyển khoản hoặc cấu hình quán.
3. **Chống phá hoại từ bên ngoài:** Chỉ khách có mặt tại bàn quét đúng mã QR được cấp mới có thể gửi đơn vào bếp.
4. **Minh bạch lịch sử đối soát:** Mọi thay đổi trạng thái đơn hàng, hủy món và nhập/xuất/điều chỉnh kho đều phải lưu vết đầy đủ (ai làm, lúc nào, lý do gì) để giải trình minh bạch cuối ca.

> *Kiến trúc kỹ thuật chi tiết đáp ứng Service Blueprint này (API, Realtime, Cơ sở dữ liệu, Bảo mật và Hạ tầng triển khai) được chuyển giao sang Tầng 4: [SYSTEM_DESIGN.md](../04-tech/SYSTEM_DESIGN.md).*
