# Market Analysis: Alternatives, Competitive Landscape & Opportunities

> **Mô-đun:** `market.md`  
> **Giai đoạn:** Market Intelligence & Strategic Positioning (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team

---

## 1. Bối cảnh thị trường F&B Việt Nam (Market Context)

Thị trường F&B tại Việt Nam đang chuyển dịch mạnh mẽ theo 3 xu hướng lớn:
- **Thanh toán không tiền mặt trở thành thói quen phổ thông:** Hơn 75% giao dịch ăn uống tại các đô thị lớn thực hiện qua chuyển khoản mã QR (VietQR / Napas247).
- **Hành vi thực khách chuộng chủ động và tốc độ:** Thế hệ Gen Z và dân văn phòng không thích xếp hàng hay vẫy gọi nhân viên nhiều lần; họ muốn tự xem hình ảnh món ăn, tùy chọn khẩu vị rõ ràng và thanh toán nhanh chóng.
- **Áp lực chi phí vận hành thu hẹp biên lợi nhuận:** Chi phí mặt bằng và nhân sự tăng cao đẩy biên lợi nhuận trung bình ngành F&B xuống mức 10–14%, buộc chủ nhà hàng phải tinh gọn bộ máy và cắt giảm tối đa thất thoát nguyên liệu.

---

## 2. Competitive Landscape (Bản đồ phân tích đối thủ cạnh tranh)

Hiện tại trên thị trường có **4 nhóm giải pháp thay thế chính**:

```text
                      Trải nghiệm Khách hàng Cao (High UX)
                                      ▲
                                      │
                                      │        ★ QROS
                                      │   (QR Ordering + KDS + AI)
             Grab / ShopeeFood        │
             Dine-In QR               │
                                      │
   Chi phí Cao ───────────────────────┼─────────────────────── Chi phí Tối ưu
   (High Cost / Khóa nền tảng)        │                       (Low TCO / Độc lập)
                                      │
               iPOS.vn                │       KiotViet / CukCuk
               (POS cồng kềnh)        │
                                      │
                                      │       Menu Giấy / QR PDF tĩnh
                                      │
                                      ▼
                      Trải nghiệm Thấp (Tĩnh / Đứt gãy)
```

### 2.1 Phân tích 4 nhóm giải pháp thay thế
1. **iPOS.vn (Thương hiệu POS lâu năm ngành F&B):**
   - *Ưu điểm:* Độ phủ thương hiệu rộng, hệ sinh thái phần cứng phong phú, nghiệp vụ sâu.
   - *Hạn chế:* Chi phí bản quyền và thiết bị POS chuyên dụng cao; tính năng QR gọi món thường là gói mua thêm rời rạc, thao tác gộp bàn nhóm đông phức tạp và trải nghiệm đồng bộ bếp chưa thực sự linh hoạt cho quán vừa và nhỏ.
2. **KiotViet / CukCuk (MISA):**
   - *Ưu điểm:* Chi phí hợp lý, quản lý sổ sách bán hàng và kho phổ biến.
   - *Hạn chế:* Trải nghiệm tự gọi món tại bàn còn cơ bản; tốc độ đồng bộ giữa bàn và bếp còn độ trễ; thiếu chế độ gom mẻ nấu cho bếp và thiếu hỗ trợ AI vận hành.
3. **GrabFood / ShopeeFood Dine-In (Đặt tại bàn qua ứng dụng giao đồ ăn):**
   - *Ưu điểm:* Tận dụng sẵn lượng người dùng có cài ứng dụng.
   - *Hạn chế:* Thu chiết khấu giao dịch cao (10%–25% mỗi đơn); nhà hàng lệ thuộc nền tảng và **không sở hữu dữ liệu khách hàng**; không giải quyết bài toán vận hành bếp (KDS) và kho nguyên liệu nội bộ.
4. **Menu giấy truyền thống & Mã QR PDF tĩnh (Google Drive / Canva):**
   - *Ưu điểm:* Chi phí khởi tạo gần như bằng 0.
   - *Hạn chế:* Khách chỉ xem được ảnh tĩnh, không gọi được món, không biết món nào vừa hết; mỗi lần đổi giá hay thêm món đều phải in lại hoặc thay tệp thủ công.

---

## 3. Competitive Comparison Matrix (Bảng so sánh sản phẩm)

| Tiêu chí so sánh | QROS | iPOS.vn | KiotViet / CukCuk | Grab Dine-In | Menu giấy / QR PDF |
|---|:---:|:---:|:---:|:---:|:---:|
| **Trải nghiệm gọi món tại bàn** | **Quét QR không cần cài app** | Gói bổ sung | Cơ bản | Bắt buộc cài App | Chỉ xem ảnh tĩnh |
| **Chống đặt đơn giả từ xa** | **QR xác thực tại bàn** | Mã tĩnh | Mã tĩnh | Định vị qua App | Không kiểm soát |
| **Gộp nhiều bàn dùng chung 1 hóa đơn** | **Hỗ trợ trực tiếp** | Phức tạp | Hạn chế | Không hỗ trợ | Không thể |
| **Đồng bộ đơn vào bếp** | **Tức thời (< 1 giây)** | Trễ 3–5 giây | Trễ 5–10 giây | Qua ứng dụng trung gian | Đi bộ đưa phiếu giấy |
| **Màn hình bếp KDS thông minh** | **Kanban + Nấu gom + Cảnh báo trễ** | KDS cơ bản | Chủ yếu in phiếu | Không có | Phiếu giấy dán |
| **Thanh toán VietQR tự xác nhận** | **Tự động khớp + Tự thanh toán tại bàn** | Tích hợp tùy gói | Tích hợp tùy gói | Qua ví nền tảng | Chụp ảnh màn hình |
| **Trừ kho nguyên liệu theo công thức** | **Tự động theo thời gian thực** | Tổng hợp cuối ca | Tổng hợp cuối ca | Không hỗ trợ | Ghi sổ tay |
| **Trợ lý AI (Tư vấn, Mô tả, Dự báo)** | **Tích hợp sẵn 3 luồng AI** | Không có | Không có | Chỉ gợi ý mua sắm | Không có |
| **Ràng buộc phần cứng POS** | **Linh hoạt (Dùng mọi điện thoại/tablet)** | Phải mua bộ POS | Thường mua máy in/POS | Không mua | Không |
| **Quyền sở hữu dữ liệu** | **100% thuộc nhà hàng** | Cloud nhà cung cấp | Cloud nhà cung cấp | Sàn nắm giữ | Không có dữ liệu |

---

## 4. Cơ hội thị trường & 5 Lợi thế khác biệt của QROS

1. **Không cài app — Không rào cản đăng nhập (Zero-App, Zero-Friction):** Thực khách chỉ cần giơ camera quét mã QR tại bàn là xem thực đơn, tùy biến món và theo dõi tiến độ ngay, giảm tối đa tỷ lệ bỏ cuộc.
2. **Giải quyết trọn vẹn văn hóa "Gộp bàn nhóm đông" tại Việt Nam:** Thay vì tạo "bàn ảo" dễ gây nhầm bill, QROS cho phép ghép nhiều bàn vật lý vào chung một phiên phục vụ để mọi thành viên cùng xem thực đơn, gọi thêm nhiều đợt vào chung một hóa đơn và giải phóng đồng loạt khi thanh toán xong.
3. **Bếp thông minh thay thế phiếu giấy:** Màn hình KDS gom nhiều bàn đặt cùng một món thành một mẻ nấu, đi kèm đồng hồ đếm thời gian chờ và âm thanh cảnh báo món quá 20 phút, giảm rõ rệt thời gian lên món giờ cao điểm.
4. **Thanh toán VietQR tự động xác nhận không cần chụp màn hình:** Sinh mã QR động đúng số tiền hóa đơn; ngay khi chuyển khoản hoàn tất, hệ thống tự chốt đơn và giải phóng bàn — đồng thời cho phép chủ quán linh hoạt bật/tắt chế độ khách tự thanh toán tại bàn.
5. **Trí tuệ nhân tạo thiết thực cho vận hành F&B:** AI không chỉ là nhãn mác mà phục vụ trực tiếp 3 việc cụ thể: *(a)* tư vấn món cho thực khách theo khẩu vị/ngân sách, *(b)* hỗ trợ quản lý soạn mô tả món hấp dẫn & trợ lý hỏi đáp vận hành cho nhân viên, và *(c)* dự báo doanh thu & lượng nguyên liệu cần nhập cho những ngày tiếp theo.
