# Problem Analysis & Current Workflow (As-Is vs. Pain Points)

> **Mô-đun:** `problem.md`  
> **Giai đoạn:** Problem Space Exploration (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team

---

## 1. Executive Problem Statement (Bản tuyên ngôn vấn đề cốt lõi)

Ngành kinh doanh dịch vụ ẩm thực (F&B) truyền thống tại Việt Nam đang đối mặt với bài toán nan giải: **Chi phí nhân sự và mặt bằng ngày càng tăng, biên lợi nhuận ròng bị thu hẹp, trong khi sự kiên nhẫn của thực khách đối với thời gian chờ phục vụ ngày càng giảm**.

Mô hình phục vụ truyền thống dựa trên menu giấy, nhân viên ghi chép tay và truyền đạt mệnh lệnh thủ công bộc lộ những điểm nghẽn nghiêm trọng vào các khung giờ cao điểm. Những sự cố nhầm món, sót đơn, nghẽn bếp, thất thoát tồn kho và tranh chấp khi thanh toán không phải do năng lực cá nhân của nhân sự, mà là hệ quả trực tiếp từ **quy trình vận hành đứt gãy và thiếu tính đồng bộ thời gian thực giữa Thực khách – Sàn phục vụ – Bếp – Thu ngân – Kho hàng**.

---

## 2. Deep-Dive Pain Points (Phân tích chi tiết các điểm đau vận hành)

### 2.1 Điểm đau của Thực khách (`PER-01` — Customer Friction)
1. **Thời gian chờ đợi kéo dài:** Trong giờ cao điểm, khách mất từ 5–10 phút chỉ để vẫy được nhân viên mang menu tới bàn, và thêm 4–5 phút nữa để gọi nhân viên quay lại ghi món.
2. **Menu giấy thiếu trực quan & lỗi thời:** Menu in ấn cố định không báo được món nào vừa hết hàng, khó thể hiện hết hình ảnh hấp dẫn, các tùy chọn topping hay thông tin món đặc trưng.
3. **Món ăn bị phục vụ sai yêu cầu:** Các ghi chú đặc thù ("không cay", "ít đường", "ít đá", "không hành") thường bị nhân viên nghe sót hoặc ghi tắt khó đọc, dẫn đến trải nghiệm thất vọng và phải đổi trả món.
4. **Ức chế khi thanh toán:** Khi dùng bữa xong, khách phải chờ gọi hóa đơn, chờ đối soát giảm giá, chờ thối tiền hoặc giơ màn hình chuyển khoản để thu ngân kiểm tra từng giao dịch.
5. **Bất tiện khi cần hỗ trợ giữa bữa ăn:** Khi cần xin thêm nước, chén đũa, khăn giấy hay nhờ dọn bàn trong không gian ồn ào, khách phải ngó tìm và vẫy tay nhiều lần nhưng không biết nhân viên đã tiếp nhận yêu cầu hay chưa.

### 2.2 Điểm đau của Đội ngũ Phục vụ (`PER-02` — Staff Burden)
1. **Quá tải áp lực thể chất:** Nhân viên phải di chuyển liên tục giữa bàn khách, quầy thu ngân và bếp (trung bình 12.000 – 15.000 bước chân/ca) chỉ để làm nhiệm vụ "chuyển phát thông tin" (đưa menu, mang phiếu order, nhắc bếp, xin bill).
2. **Chịu phàn nàn gián tiếp:** Là người trực tiếp đối mặt với sự khó chịu của khách khi món lên chậm, dù nút thắt thực sự nằm ở việc không điều phối được hàng đợi trong bếp.
3. **Lúng túng khi khách ghép bàn nhóm đông:** Khi nhóm 8–12 người ghép 2–4 bàn nhỏ lại, việc ghi mã bàn nào để giao đúng món và gom đúng một hóa đơn rất dễ gây nhầm lẫn.
4. **Chồng chéo hoặc bỏ sót yêu cầu tại bàn:** Khi nhiều bàn cùng gọi hỗ trợ trong giờ cao điểm mà thiếu hàng đợi chung, hai nhân viên dễ chạy trùng ra cùng một bàn trong khi bàn khác chờ lâu bị bỏ quên.

### 2.3 Điểm đau của Khu vực Bếp & Bar (`PER-03` — Kitchen Bottlenecks)
1. **Nghẽn đơn và mất phiếu order giấy:** Phiếu giấy dễ bị ướt, mờ chữ, rách hoặc thất lạc trong môi trường nhiệt độ cao và dầu mỡ.
2. **Không gom được mẻ nấu (No Batching Insight):** Khi 4 bàn khác nhau cùng gọi món "Cơm rang dưa bò" trong 5 phút, nhìn 4 tờ phiếu rời rạc khiến đầu bếp phải nấu lẻ từng phần thay vì gom chung một mẻ, vừa tốn thời gian vừa kéo dài chờ đợi.
3. **Không theo dõi được thời gian chờ từng món:** Bếp không biết món nào đã chờ quá 15–20 phút để ưu tiên xử lý trước, dễ dẫn tới tình trạng khách vào trước nhưng nhận món sau.

### 2.4 Điểm đau của Thu ngân, Quản lý & Chủ quán (`PER-04`, `PER-05`, `PER-06`)
1. **Lệch sổ và thất thoát tại quầy thu ngân:** Cộng trừ thủ công, áp dụng mã giảm giá không kiểm soát giới hạn lượt dùng hoặc xác nhận chuyển khoản bằng mắt thường dễ gây sai sót và kẽ hở gian lận.
2. **Mất đồng bộ với kho nguyên liệu:** Nguyên liệu trong bếp đã hết nhưng ngoài sàn vẫn nhận gọi món, đến khi bếp báo lại mới phải ra xin lỗi và nhờ khách đổi món, làm giảm mạnh uy tín nhà hàng.
3. **Thiếu dữ liệu tức thời để ra quyết định:** Quản lý và chủ quán chỉ biết tổng doanh thu cuối ngày khi kiểm két; thiếu góc nhìn theo thời gian thực về món bán chạy, hiệu suất nhân viên và dự báo lượng nguyên liệu cần nhập cho hôm sau.

---

## 3. Current As-Is Workflow (Quy trình truyền thống và các điểm đứt gãy)

```text
[BƯỚC 1: TIẾP ĐÓN]
Khách vào nhà hàng ──► Tìm bàn trống ──► Vẫy nhân viên ──► Nhân viên mang menu giấy tới bàn
(Độ trễ: 3 – 7 phút)

[BƯỚC 2: GỌI MÓN]
Khách xem menu giấy ──► Gọi nhân viên lại ──► Nhân viên ghi chép vào sổ tay
(Nguy cơ sai sót: Ghi nhầm món, thiếu ghi chú topping / khẩu vị)

[BƯỚC 3: CHUYỂN ĐƠN]
Nhân viên đi bộ ra quầy nhập lại hoặc xé phiếu giấy mang vào bếp dán lên dây treo
(Độ trễ: 2 – 5 phút; Phiếu giấy dễ thất lạc hoặc nhòe mực)

[BƯỚC 4: BẾP CHẾ BIẾN]
Đầu bếp đọc phiếu giấy theo trực giác ──► Nấu từng đơn lẻ ──► Gọi phục vụ ra lấy món
(Điểm nghẽn: Không biết món nào trễ > 20 phút, không gom mẻ nấu được)

[BƯỚC 5: TRẢ MÓN]
Nhân viên bê món ra bàn ──► Đối chiếu số bàn trên giấy ──► Nhầm bàn khi có ghép bàn
(Rủi ro: Giao nhầm khách, phục vụ thiếu món)

[BƯỚC 6: THANH TOÁN]
Khách vẫy xin bill ──► Phục vụ báo Thu ngân ──► Thu ngân kiểm tra & in hóa đơn
──► Mang ra bàn ──► Kiểm tra sai giảm giá / chuyển khoản chụp màn hình ──► Thối tiền
(Độ trễ: 8 – 15 phút làm chậm quay vòng bàn ăn)
```

---

## 4. Root Cause Analysis (Phân tích nguyên nhân gốc rễ — 5 Whys)

| Cấp độ | Câu hỏi & Phân tích nguyên nhân |
|---|---|
| **Why 1:** Tại sao khách phải chờ lâu và hay nhận sai món? | Vì nhân viên phục vụ quá tải trong khâu chạy đi chạy lại để ghi chép và truyền tin giữa bàn với bếp. |
| **Why 2:** Tại sao nhân viên phải tốn quá nhiều công chuyển phát thông tin? | Vì thực đơn và phiếu gọi món vẫn là tài liệu tĩnh tách rời, không có kênh kết nối trực tiếp từ bàn ăn vào bếp. |
| **Why 3:** Tại sao khách không thể tự đặt món và theo dõi hóa đơn tại bàn? | Vì nhà hàng thiếu một kênh đặt món không cần cài app, định danh chính xác bàn ăn và tự cập nhật tình trạng còn/hết món. |
| **Why 4:** Tại sao việc gộp bàn, trừ kho và thanh toán hay xảy ra sai lệch? | Vì các công cụ hiện có tách rời trạng thái bàn ăn, phiên hóa đơn, bếp chế biến và sổ kho thành những khâu độc lập, phải đối chiếu thủ công. |
| **Nguyên nhân gốc rễ (Root Cause):** | **Thiếu một nền tảng vận hành nhà hàng hợp nhất theo thời gian thực**, kết nối xuyên suốt từ điểm chạm của thực khách tại bàn đến Bếp, Phục vụ, Thu ngân và Kho nguyên liệu. |

---

## 5. Quantified Impact Analysis (Định lượng tổn thất vận hành)

Dựa trên số liệu khảo sát tại các nhà hàng quy mô trung bình (20 – 40 bàn, phục vụ 200 – 400 lượt khách/ngày):

- **Thời gian chết của bàn ăn (Table Dwell Time):** Mỗi bàn lãng phí trung bình **18 phút** — gồm **9–15 phút chờ gọi món** (chờ menu và chờ ghi món vào giờ cao điểm) và **9 phút chờ thanh toán**. Cắt giảm thời gian chết này giúp tăng thêm ít nhất **1.5 lượt quay vòng bàn/ngày**.
- **Tỷ lệ đơn hàng sai sót (Order Error Rate):** Chiếm **4.8%** tổng số đơn, trực tiếp gây lãng phí thực phẩm phải làm lại/hủy bỏ và làm giảm ~15% tỷ lệ khách quay lại.
- **Chi phí nhân sự dư thừa:** Nhà hàng thường phải thuê thêm 2–3 nhân sự phục vụ giờ cao điểm chỉ để ghi chép và chạy tin, tốn thêm **15 – 25 triệu VNĐ/tháng**.
- **Thất thoát nguyên liệu và dòng tiền:** Chênh lệch tồn kho không giải trình do không trừ nguyên liệu tự động theo công thức món gây tổn thất **~3.2% tổng giá vốn hàng bán (COGS)**, tương đương **2.5% – 4.0% doanh thu mỗi tháng**.
