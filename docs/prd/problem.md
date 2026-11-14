# Problem Analysis & Current Workflow (As-Is vs. Pain Points)

> **Mô-đun:** `problem.md`  
> **Giai đoạn:** Problem Space Exploration  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  

---

## 1. Executive Problem Statement (Bản tuyên ngôn vấn đề cốt lõi)

Ngành kinh doanh dịch vụ ẩm thực (F&B) truyền thống tại Việt Nam đang đối mặt với bài toán nan giải: **Chi phí nhân sự ngày càng tăng, biên lợi nhuận ròng bị thu hẹp, trong khi sự kiên nhẫn của khách hàng đối với thời gian chờ phục vụ ngày càng giảm**.

Mô hình phục vụ truyền thống dựa trên menu giấy, nhân viên ghi chép tay và truyền đạt mệnh lệnh thủ công bộc lộ những điểm nghẽn nghiêm trọng vào các khung giờ cao điểm. Những sự cố nhầm món, sót đơn, nghẽn bếp, thất thoát tồn kho và tranh chấp khi thanh toán không phải do năng lực cá nhân của nhân sự, mà là hệ quả trực tiếp từ **quy trình vận hành đứt gãy và thiếu tính đồng bộ thời gian thực giữa Khách hàng – Sàn phục vụ – Bếp – Thu ngân – Kho hàng**.

---

## 2. Deep-Dive Pain Points (Phân tích chi tiết các điểm đau vận hành)

### 2.1 Điểm đau của Khách hàng (Customer Friction)
1. **Thời gian chờ đợi kéo dài:** Trong giờ cao điểm, khách mất từ 5-10 phút chỉ để vẫy được nhân viên mang menu tới bàn, và thêm 5 phút nữa để gọi được nhân viên quay lại ghi món.
2. **Menu giấy thiếu trực quan:** Menu in ấn cố định không hiển thị được tình trạng hết món tức thời, không có hình ảnh chi tiết, không hỗ trợ xem topping hay dị ứng thực phẩm.
3. **Món ăn bị phục vụ sai yêu cầu:** Các ghi chú đặc thù ("không cay", "ít đường", "nhiều đá", "không hành") thường bị nhân viên nghe sót hoặc quên ghi vào phiếu, dẫn đến trải nghiệm thất vọng và yêu cầu đổi trả món.
4. **Ức chế khi thanh toán:** Khi dùng bữa xong, khách phải chờ đợi thu ngân đối soát hóa đơn giấy, tính toán tiền thối hoặc chờ mang máy POS quẹt thẻ tới bàn.

### 2.2 Điểm đau của Đội ngũ Phục vụ (Staff Burden)
1. **Quá tải áp lực thể chất:** Nhân viên phải di chuyển liên tục giữa bàn khách, quầy thu ngân và khu vực bếp (trung bình 12.000 - 15.000 bước chân/ca làm việc) chỉ để làm nhiệm vụ chuyển phát thông tin (đưa menu, mang giấy order, nhắc bếp, xin bill).
2. **Xung đột tâm lý và chịu phàn nàn vô lý:** Là đối tượng trực tiếp hứng chịu sự giận dữ của thực khách khi bếp lên món chậm, dù nguyên nhân nằm ở việc điều phối luồng vào của bếp chứ không phải do phục vụ.
3. **Khó xử lý các tình huống gộp bàn:** Khi một nhóm khách 8-12 người ghép 3 bàn nhỏ lại với nhau, nhân viên không biết ghi mã bàn nào, dẫn đến nhầm lẫn khi giao đồ ăn hoặc thu ngân in nhầm hóa đơn của bàn ghép.

### 2.3 Điểm đau của Khu vực Bếp & Bar (Kitchen Bottlenecks)
1. **Nghẽn đơn và mất mát phiếu order giấy:** Phiếu giấy dễ bị ướt, mờ mực, rách hoặc thất lạc khi bếp trưởng thao tác trong môi trường dầu mỡ và nhiệt độ cao.
2. **Thiếu khả năng gom món nấu hàng loạt (No Batching Insight):** Nếu 4 bàn khác nhau gọi cùng một món "Bò bít tết sốt tiêu" trong vòng 5 phút, đầu bếp nhìn 4 phiếu giấy rời rạc sẽ không nhận biết được để gom nấu chung một mẻ chảo, làm lãng phí năng lượng và kéo dài thời gian chờ của cả 4 bàn.
3. **Không đo lường được thời gian chế biến:** Bếp không biết món nào đã chờ quá 15 hay 20 phút để ưu tiên xử lý trước (nguyên tắc FIFO bị phá vỡ hoàn toàn khi đơn dồn về ồ ạt).

### 2.4 Điểm đau của Thu ngân & Quản lý (Cashier & Management Leakage)
1. **Lệch sổ và thất thoát tiền mặt:** Thao tác cộng trừ thủ công, áp dụng voucher giấy không kiểm soát được số lần dùng dẫn đến việc nhân viên lạm dụng mã giảm giá hoặc gian lận tiền thối.
2. **Không đồng bộ tồn kho thời gian thực:** Món đã hết nguyên liệu chính trong kho nhưng nhân viên vẫn nhận order từ khách, đến khi chuyển vào bếp mới phát hiện hết và phải quay ra xin lỗi khách để đổi món (gây sụt giảm nghiêm trọng uy tín quán).
3. **Mù mờ về dữ liệu kinh doanh:** Quản lý chỉ biết tổng doanh thu vào cuối ngày khi kiểm đếm két tiền; không nắm được món nào có biên lợi nhuận tốt, tốc độ quay vòng bàn ra sao, và không có dự báo lượng khách ngày hôm sau để chuẩn bị nguyên vật liệu.

---

## 3. Current As-Is Workflow vs. To-Be Vision (Quy trình hiện tại và sự đứt gãy)

```
[BƯỚC 1: TIẾP ĐÓN]
Khách vào nhà hàng ──> Tìm bàn trống ──> Vẫy nhân viên ──> Nhân viên mang menu giấy tới bàn
(Độ trễ: 3 - 7 phút)

[BƯỚC 2: GỌI MÓN]
Khách xem menu giấy ──> Gọi nhân viên lại ──> Nhân viên ghi chép vào sổ tay
(Nguy cơ sai sót: Ghi nhầm món, quên ghi chú topping/dị ứng)

[BƯỚC 3: CHUYỂN ĐƠN]
Nhân viên đi bộ đến quầy POS nhập lại hoặc xé phiếu giấy mang vào bếp dán lên dây treo
(Độ trễ: 2 - 5 phút; Phiếu giấy dễ thất lạc)

[BƯỚC 4: BẾP CHẾ BIẾN]
Đầu bếp đọc phiếu giấy theo trực giác ──> Nấu từng đơn lẻ ──> Chuông bấm gọi món xong
(Điểm nghẽn: Không biết món nào trễ >20 phút, không gom mẻ nấu được)

[BƯỚC 5: TRẢ MÓN]
Nhân viên bê món ra bàn ──> Đối chiếu số bàn trên giấy ──> Nhầm bàn nếu có ghép bàn
(Rủi ro: Sai bàn, phục vụ nhầm khách)

[BƯỚC 6: THANH TOÁN]
Khách vẫy nhân viên xin bill ──> Phục vụ chạy ra quầy báo Thu ngân ──> Thu ngân in hóa đơn
──> Phục vụ cầm hóa đơn ra bàn ──> Khách kiểm tra thấy sai voucher ──> Chạy đi sửa lại
──> Khách trả tiền mặt ──> Thu ngân thối tiền
(Độ trễ: 8 - 15 phút làm tắc nghẽn bàn ăn)
```

---

## 4. Root Cause Analysis (Phân tích nguyên nhân gốc rễ - 5 Whys)

| Cấp độ | Câu hỏi / Nguyên nhân gốc |
|---|---|
| **Tại sao khách hàng phải chờ lâu và thường xuyên bị phục vụ sai món?** | Vì nhân viên phục vụ bị quá tải trong khâu ghi chép và mang thông tin qua lại giữa bàn và bếp. |
| **Tại sao nhân viên phải tốn nhiều thời gian mang thông tin thủ công?** | Vì thông tin thực đơn và đơn hàng nằm trên giấy tĩnh, không có kết nối số trực tiếp từ bàn ăn. |
| **Tại sao không thể để khách tự đặt món và tự kiểm tra bill?** | Vì thiếu một kênh đặt món tại bàn liền mạch (Zero-install), không cần cài app, bảo mật định danh được chính xác bàn ăn. |
| **Tại sao việc gộp bàn và thanh toán thường xuyên bị nhầm lẫn và thất thoát?** | Vì hệ thống POS truyền thống tách biệt trạng thái bàn vật lý với phiên giao dịch số (Session), không có cơ chế khóa bi quan (Locking) và không đồng bộ kho tự động. |
| **Nguyên nhân gốc rễ (Root Cause):** | **Thiếu một nền tảng vận hành hợp nhất theo thời gian thực (Unified Real-time OS)** có khả năng kết nối trực tiếp thực khách tại bàn với chuỗi giá trị của nhà hàng (Bếp KDS, Thu ngân, Kho nguyên liệu). |

---

## 5. Quantified Impact Analysis (Định lượng tổn thất vận hành)

Dựa trên số liệu khảo sát tại các nhà hàng quy mô trung bình (20 - 40 bàn, phục vụ 200 - 400 lượt khách/ngày):

- **Thời gian chết của bàn ăn (Table Dwell Time):** Mỗi bàn mất trung bình **18 phút lãng phí** (chờ menu: 5 phút, chờ nhân viên ghi món: 4 phút, chờ thanh toán và thối tiền: 9 phút). Việc cắt giảm 18 phút này cho phép tăng thêm ít nhất **1.5 lượt quay vòng bàn (table turnover)/ngày**.
- **Tỷ lệ đơn hàng có sai sót (Order Error Rate):** Chiếm **4.8%** tổng số đơn, dẫn đến lãng phí đồ ăn hủy bỏ (food waste) và làm giảm 15% tỷ lệ khách hàng quay lại.
- **Chi phí nhân sự dư thừa:** Nhà hàng phải duy trì thêm 2-3 nhân viên phục vụ chỉ để làm nhiệm vụ "chuyển lời" trong giờ cao điểm, tiêu tốn từ 15.000.000 - 25.000.000 VNĐ/tháng.
- **Thất thoát dòng tiền và nguyên liệu:** Sai lệch giữa tồn kho thực tế và sổ sách kế toán do không trừ kho tự động theo công thức chế biến (BOM) gây tổn thất từ **2.5% đến 4.0% tổng doanh thu hàng tháng**.
