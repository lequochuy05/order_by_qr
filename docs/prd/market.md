# Market Analysis: Alternatives, Competitive Landscape & Opportunities

> **Mô-đun:** `market.md`  
> **Giai đoạn:** Market Intelligence & Strategic Positioning  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  

---

## 1. Bối cảnh thị trường F&B Việt Nam (Market Context)

Thị trường F&B Việt Nam đang chứng kiến làn sóng chuyển đổi số mạnh mẽ hậu đại dịch:
- **Tỷ lệ thanh toán không tiền mặt bùng nổ:** Hơn 75% giao dịch tại các đô thị lớn được thực hiện qua mã QR (VietQR / Napas247).
- **Hành vi người tiêu dùng thay đổi:** Thế hệ trẻ (Gen Z, Millennials) ưu tiên sự chủ động, tính cá nhân hóa và tốc độ; họ có xu hướng không thích phải xếp hàng chờ đợi hoặc vẫy gọi nhân viên nhiều lần.
- **Áp lực lạm phát và chi phí mặt bằng / nhân công:** Biên lợi nhuận trung bình ngành F&B giảm từ 18-22% xuống còn 10-14%, buộc các chủ nhà hàng phải tìm kiếm giải pháp tinh gọn bộ máy và cắt giảm lãng phí vận hành.

---

## 2. Competitive Landscape (Bản đồ phân tích đối thủ cạnh tranh)

Hiện tại trên thị trường có 5 nhóm giải pháp thay thế chính:

```
                      Trải nghiệm Khách hàng Cao (High UX)
                                      ▲
                                      │
                                      │        ★ QROS
                                      │   (Real-time QR + KDS + AI)
             Grab / ShopeeFood        │
             Dine-In QR               │
                                      │
   Chi phí Cao ───────────────────────┼─────────────────────── Chi phí Tối ưu
   (High Cost / Khóa nền tảng)        │                       (Low TCO / Độc lập)
                                      │
               iPOS.vn                │       KiotViet / CukCuk
               (Desktop POS cồng kềnh)│
                                      │
                                      │       Menu Giấy / QR PDF tĩnh
                                      │
                                      ▼
                      Trải nghiệm Thấp (Tĩnh / Đứt gãy)
```

### 2.1 Chi tiết các giải pháp thay thế:

1. **iPOS.vn (Thương hiệu dẫn đầu thị trường F&B):**
   - *Ưu điểm:* Độ phủ thương hiệu lớn, hệ sinh thái thiết bị phần cứng phong phú, nhiều tính năng nghiệp vụ sâu.
   - *Hạn chế:* Chi phí bản quyền và thiết bị đắt đỏ; hệ thống desktop POS nặng nề, khó tùy biến; giải pháp QR gọi món thường là module mua thêm, thiếu tính năng gộp bàn động và trải nghiệm KDS thời gian thực chưa thực sự mượt mà.
2. **KiotViet / CukCuk (MISA):**
   - *Ưu điểm:* Giá thành mềm, quản lý kho nguyên liệu và kế toán bài bản.
   - *Hạn chế:* Vốn phát triển từ phần mềm bán lẻ chung (KiotViet) nên trải nghiệm tại bàn chưa sâu sắc; độ trễ đồng bộ đơn hàng giữa bàn và bếp còn cao; không có tính năng gom mẻ nấu (Batch Cooking) cho bếp.
3. **GrabFood / ShopeeFood Dine-In (Ăn tại bàn qua App Giao đồ ăn):**
   - *Ưu điểm:* Khách hàng đã quen thuộc với ứng dụng, có sẵn lượng người dùng lớn.
   - *Hạn chế:* Thu phí chiết khấu giao dịch cao (10% - 25% trên mỗi hóa đơn); nhà hàng bị lệ thuộc nền tảng và **hoàn toàn không sở hữu dữ liệu khách hàng**; không có giải pháp quản trị bếp (KDS) và kho nội bộ.
4. **Menu giấy truyền thống & Mã QR PDF tĩnh (Google Drive / Canva):**
   - *Ưu điểm:* Chi phí ban đầu gần như bằng 0.
   - *Hạn chế:* Hoàn toàn thụ động, khách chỉ xem được hình ảnh tĩnh; không thể đặt món, không biết món còn hay hết; tốn kém chi phí in lại mỗi khi thay đổi giá hoặc thêm món mới.

---

## 3. Competitive Comparison Matrix (Bảng so sánh tính năng toàn diện)

| Tiêu chí so sánh | QROS | iPOS.vn | KiotViet | Grab Dine-in | QR PDF tĩnh |
|---|:---:|:---:|:---:|:---:|:---:|
| **Trải nghiệm khách hàng tại bàn** | **Zero-Install Web** | Web add-on | Web cơ bản | Bắt buộc cài App | Mở file ảnh/PDF |
| **Bảo mật mã QR theo bàn** | **Chữ ký số (`sig`)** | Token tĩnh | Token tĩnh | Định vị GPS/App | Không bảo mật |
| **Cơ chế Gộp bàn động (`table_session_tables`)** | **Hỗ trợ Native** | Khó thao tác | Giới hạn | Không hỗ trợ | Không thể |
| **Độ trễ đồng bộ đơn vào Bếp** | **< 500ms (WebSocket)** | Polling 3-5s | Polling 5-10s | Phụ thuộc App | Thủ công (đi bộ) |
| **Màn hình Bếp KDS chuyên sâu** | **Kanban + Batch Cook + Audio Timer** | KDS cơ bản | Phiếu in bếp | Không có | Phiếu giấy dán |
| **Thanh toán VietQR động** | **PayOS Webhook tự chốt** | Tích hợp cổng riêng | Tích hợp riêng | Ví điện tử GrabPay | Khách chụp màn hình |
| **Tự động trừ kho theo công thức (BOM)** | **Thời gian thực** | Cuối ca | Cuối ca | Không hỗ trợ | Tính tay sổ sách |
| **Trí tuệ nhân tạo (Generative AI)** | **Gemini AI (Menu + Forecast)** | Không có | Không có | Đề xuất mua sắm | Không có |
| **Chi phí triển khai phần cứng** | **Không ràng buộc (Dùng mọi màn hình)** | Rất đắt (Mua máy POS) | Mua máy in/POS | Không mua | Không |
| **Quyền sở hữu dữ liệu (Data Ownership)** | **100% của Nhà hàng** | Lưu trên cloud iPOS | Lưu KiotViet | Grab nắm giữ | Không có dữ liệu |

---

## 4. Market Opportunities & Strategic Differentiators (Cơ hội & Lợi thế cạnh tranh vượt trội)

### 4.1 "Zero-App, Zero-Friction" Dining
Khách hàng không cần tải bất kỳ ứng dụng nào từ App Store/Google Play và không cần đăng ký tài khoản rườm rà. Chỉ với camera điện thoại quét mã QR định danh bàn, toàn bộ menu sống động mở ra tức thì.

### 4.2 Giải quyết bài toán Gộp bàn phức tạp (Native Dynamic Table Merge)
Trong văn hóa ẩm thực Việt Nam, việc khách đi nhóm đông và ghép 2-4 bàn lại với nhau là cực kỳ phổ biến. Đa phần các phần mềm hiện nay buộc nhân viên phải tạo một "bàn ảo" hoặc chỉ chọn 1 bàn đại diện, dẫn đến sai sót khi giao món hoặc nhầm bill. QROS giải quyết triệt để vấn đề này ở cấp cơ sở dữ liệu (`table_session_tables`), cho phép nhiều bàn vật lý cùng thuộc về một phiên phục vụ, bất kỳ khách ở bàn nào trong nhóm quét QR cũng đều nhìn thấy và gọi chung một giỏ hàng.

### 4.3 Hệ thống Bếp thông minh KDS thay thế hoàn toàn máy in bill bếp
Thay vì tốn hàng triệu đồng tiền giấy in nhiệt mỗi tháng và đối mặt với rủi ro kẹt giấy/mất đơn, màn hình KDS của QROS mang lại khả năng phân loại thông minh: tự động gom mẻ món ăn cùng loại để đầu bếp nấu nhanh hơn, cảnh báo món quá 20 phút bằng âm thanh và màu sắc trực quan.

### 4.4 Chốt bill tự động qua PayOS Webhook
Không còn cảnh khách hàng phải chuyển khoản rồi giơ màn hình điện thoại cho thu ngân chụp ảnh đối soát. QROS sinh mã QR VietQR động chứa chính xác số tiền và mã hóa đơn; ngay khi khách bấm chuyển tiền từ app ngân hàng bất kỳ, Webhook của PayOS lập tức bắn tín hiệu xác thực và đổi trạng thái bàn sang "Đã thanh toán", in hóa đơn trong vòng 1 giây.

### 4.5 Ứng dụng Trí tuệ Nhân tạo thiết thực
Không chỉ là khẩu hiệu marketing, AI (Google Gemini) trong QROS giải quyết 3 bài toán cụ thể:
1. Giúp quản lý tạo mô tả món ăn chuyên nghiệp, kích thích vị giác chỉ bằng 1 cú click.
2. Đề xuất các gói Combo tối ưu dựa trên tần suất các món thường được gọi cùng nhau.
3. Dự báo doanh thu và số lượng món cần chuẩn bị cho ngày tiếp theo dựa trên dữ liệu lịch sử.
