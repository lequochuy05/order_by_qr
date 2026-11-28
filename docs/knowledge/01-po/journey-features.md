# Connected Task Journey, Product Scope Groups & Dependency Matrix

> **Mô-đun:** `journey-features.md`  
> **Giai đoạn:** Product Journey & Feature Scope (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team  
> **Bàn giao cho:** Design (`02-design/`) & BA (`03-ba/`)

---

## 1. Hành trình Trải nghiệm Kết nối theo 6 Giai đoạn (`STG-01` → `STG-06`)

Toàn bộ vòng đời sử dụng sản phẩm QROS — từ thực khách bước vào bàn đến chủ quán tổng kết kinh doanh — được chia thành **6 User Stages** liên hoàn làm xương sống thống nhất cho Design, BA và Kỹ thuật:

```text
[STG-01: Khám phá & Mở phiên] ──► [STG-02: Duyệt Menu & Đặt món] ──► [STG-03: Bếp KDS & Tồn kho]
Quét QR bàn có xác thực;           Xem ảnh, tùy biến topping, hỏi AI,   KDS nhận đơn tức thì, gom mẻ,
Nhân sự đăng nhập phân quyền/2FA.  gọi thêm đợt, gọi hỗ trợ tại bàn.    cảnh báo trễ >20p, trừ kho BOM.
                                                                                   │
                                                                                   ▼
[STG-06: Quản trị & Phân tích] ◄── [STG-05: Thanh toán & Đóng bàn] ◄── [STG-04: Phục vụ & Mang về]
Cập nhật thực đơn, kho, nhân sự;    Áp voucher/khuyến mãi, thu tiền mặt  Sơ đồ bàn, hàng đợi yêu cầu bàn
Báo cáo doanh thu & dự báo AI.      hoặc VietQR tự chốt, giải phóng bàn. nhận xử lý không trùng, gộp/mang về.
```

### Tóm tắt phối hợp giữa các vai trò trong 6 giai đoạn

| User Stage | Vai trò tham gia (`01-po/personas.md`) | Trải nghiệm & Hành động chính | Kết quả nghiệp vụ đầu ra |
|---|---|---|---|
| **`STG-01`** Khám phá, Quét QR & Xác thực | `PER-01..06` | Khách quét QR mở đúng bàn không cần cài app; Nhân sự đăng nhập vào đúng không gian vai trò (Admin xác thực 2FA). | Phiên bàn hợp lệ được mở; phiên làm việc nhân sự được phân quyền an toàn. |
| **`STG-02`** Duyệt thực đơn, AI gợi ý & Đặt món | `PER-01` | Xem danh mục/combo, tùy biến món/topping, hỏi trợ lý AI gợi ý & chọn đặt món trực tiếp từ khung chat, gửi đơn nhiều đợt, gọi nhân viên hỗ trợ nhanh theo lý do và theo dõi tiến độ trực tiếp. | Đơn hàng chính xác theo khẩu vị đẩy tức thì xuống bếp; khách thêm món gợi ý từ AI vào giỏ trong 1 chạm; yêu cầu hỗ trợ gửi đúng bàn, chống bấm spam. |
| **`STG-03`** Tiếp nhận Bếp (KDS) & Kiểm soát kho | `PER-03`, `PER-05` | Bếp xử lý 3 trạng thái (*Chờ → Đang nấu → Hoàn tất*), nấu gom món, nhận cảnh báo quá 20 phút, phát hiện dị ứng/ghi chú khẩn ưu tiên từ AI, báo hết món, chụp ảnh hóa đơn để nhập kho hàng loạt, dùng AI gợi ý định mức/giá vốn và gợi ý nhập kho thông minh từ BOM + dự báo bán. | Món ra nhanh, giảm trễ giờ và tránh rủi ro dị ứng cho khách; tồn kho được giữ/trừ, nhập bù bằng hóa đơn và dự báo theo sức bán thực tế. |
| **`STG-04`** Phục vụ tại bàn, Gộp bàn & Mang về | `PER-02`, `PER-04` | Theo dõi sơ đồ 4 trạng thái bàn, tiếp nhận hàng đợi yêu cầu từ bàn theo thời gian thực (nhận xử lý & hoàn tất, tránh nhận trùng, lọc ưu tiên yêu cầu khẩn), đặt món hộ, gộp/hủy gộp bàn và tiếp nhận đơn mang về. | Sàn phục vụ phối hợp nhịp nhàng, đáp ứng bàn kịp thời không chạy trùng hay bỏ sót, không nhầm bàn ghép/mang về. |
| **`STG-05`** Thanh toán & Đóng phiên | `PER-01`, `PER-04`, `PER-05` | Áp khuyến mãi/voucher, thanh toán tiền mặt (tự tính tiền thối) hoặc VietQR tự xác nhận (tại quầy hoặc khách tự thanh toán khi quán bật), in & tra cứu hóa đơn. | Hóa đơn chốt chính xác trong < 30 giây, toàn bộ bàn (gồm bàn gộp) tự giải phóng. |
| **`STG-06`** Quản trị, Báo cáo & Dự báo AI | `PER-05`, `PER-06` | Quản trị thực đơn/combo/kho/khuyến mãi/nhân sự, quản lý gói dịch vụ (Cơ bản / Pro, kích hoạt dùng thử 14 ngày hoặc nâng cấp tự động qua VietQR), dùng AI viết mô tả món, chụp ảnh hóa đơn nhập kho nhanh, gợi ý định mức & kiểm tra giá vốn món, gợi ý nhập kho thông minh, đề xuất Combo & khung giờ Khuyến mãi giờ thấp điểm, yêu cầu AI tóm tắt ca & phân tích điều hành tức thì từ số liệu thực tế, xem dashboard, báo cáo doanh thu, dự báo AI và cấu hình nhà hàng (song ngữ, sáng/tối). | Nhà hàng làm chủ 100% dữ liệu, mở khóa linh hoạt gói nâng cao theo quy mô vận hành, rút ngắn thao tác nhập kho cuối ngày, kiểm soát chặt giá vốn món ăn, tối ưu doanh thu khung giờ vắng và nắm bắt nhanh điểm bất thường trong ca. |

---

## 2. Danh mục Phạm vi Tính năng cấp PO (6 Epics · `F1.1` → `F6.8` — 38 tính năng)

Đây là danh mục mã tính năng gốc (`F*.*`) mà **Design (`02-design`)** và **BA (`03-ba`)** sẽ kế thừa để thiết kế màn hình và viết đặc tả chi tiết:

### Epic 1 (`F1.*`): Trải nghiệm Gọi món Khách hàng & Phiên bàn QR
- **`F1.1` — Xác thực mã QR tại bàn:** Quét QR mở đúng phiên bàn, chặn bàn tạm ngưng và ngăn đặt đơn giả từ bên ngoài nhà hàng.
- **`F1.2` — Thực đơn số trực quan & Cập nhật tức thì:** Duyệt danh mục, combo, tìm kiếm món, hiển thị rõ ảnh, giá và trạng thái còn/hết món.
- **`F1.3` — Tùy biến món ăn & Ghi chú khẩu vị:** Chọn kích cỡ, mức đường/đá, topping phụ thu và ghi chú riêng gửi thẳng tới đầu bếp.
- **`F1.4` — Giỏ hàng thông minh, Đặt nhiều đợt & Theo dõi trực tiếp:** Gửi đơn chống trùng lặp, gọi thêm món vào chung hóa đơn của bàn và xem tiến độ chế biến live.
- **`F1.5` — Trợ lý AI tư vấn & Đặt món trực tiếp từ Chat:** Khách hỏi gợi ý món theo khẩu vị, số lượng người, dị ứng hoặc ngân sách; AI trả lời kèm thẻ món/combo **đang còn bán** để khách bấm thêm vào giỏ ngay trong 1 chạm (tự mở chọn kích cỡ/topping khi món có tùy chọn).
- **`F1.6` — Gọi nhân viên hỗ trợ tại bàn:** Thực khách chọn nhanh lý do phổ biến (*Thêm nước/đá, Thêm chén đũa/khăn giấy, Dọn bàn, Hỗ trợ gọi món, Gọi thanh toán, Khác*), bổ sung ghi chú ngắn, theo dõi phản hồi nhận xử lý trực tiếp ngay trên điện thoại và có cơ chế chống bấm lặp/spam.

### Epic 2 (`F2.*`): Điều phối Bếp Thông minh (KDS)
- **`F2.1` — Bảng điều phối trạng thái bếp (Kanban):** Quản lý tiến độ từng món rõ ràng qua 3 bước: *Chờ chế biến → Đang nấu → Hoàn tất*.
- **`F2.2` — Chế độ nấu gom theo mẻ (Batch Cooking):** Tự động tổng hợp các phần món giống nhau từ nhiều bàn để đầu bếp chế biến cùng một mẻ.
- **`F2.3` — Đồng hồ chờ & Cảnh báo quá giờ:** Đếm thời gian chờ thực tế của từng món và đổi màu cảnh báo nổi bật khi vượt ngưỡng (20 phút).
- **`F2.4` — Thông báo âm thanh:** Phát chuông khi có đơn mới và cảnh báo khẩn khi có món sắp trễ hạn.
- **`F2.5` — Hủy món / Báo hết hàng tức thời:** Đầu bếp từ chối món kèm lý do hết nguyên liệu và thông báo ngay tới phục vụ và thực khách.
- **`F2.6` — AI Cảnh báo dị ứng & Ưu tiên ghi chú tự do:** Tự động gắn nhãn an toàn ngay lập tức và dùng AI phân loại mức rủi ro (*Nguy cấp / Ưu tiên cao / Khẩu vị*) cho ghi chú món ăn từ khách và ghi chú yêu cầu phục vụ tại bàn, giúp Bếp (`PER-03`) và Phục vụ (`PER-02`) ưu tiên xử lý và tránh sai sót dị ứng thực phẩm.

### Epic 3 (`F3.*`): Thanh toán Thông minh & Đối soát Hóa đơn
- **`F3.1` — Thanh toán tiền mặt & Tính tiền thối:** Kiểm tra tiền khách đưa đủ tổng hóa đơn, tính tiền thừa tự động và cảnh báo nếu còn món chưa hoàn tất.
- **`F3.2` — Thanh toán VietQR động & Tự thanh toán tại bàn:** Tạo mã chuyển khoản QR đúng số tiền hóa đơn cho thu ngân tại quầy, đồng thời hỗ trợ khách tự thanh toán tại bàn khi chủ quán cho phép.
- **`F3.3` — Tự động đối soát & Giải phóng bàn:** Xác nhận giao dịch chuyển khoản thành công tự động, đóng phiên phục vụ và giải phóng đồng loạt toàn bộ bàn (bao gồm bàn đã gộp).
- **`F3.4` — Quản lý & Áp dụng Voucher / Khuyến mãi:** Kiểm tra điều kiện hợp lệ (hạn dùng, đơn tối thiểu, lượt dùng tối đa) và tự động áp dụng khuyến mãi theo ngày/khung giờ.
- **`F3.5` — In hóa đơn & Tra cứu lịch sử đối soát ca:** Xuất hóa đơn chuẩn cho khách và cho phép thu ngân/quản lý tra cứu, lọc lịch sử hóa đơn cuối ca.

### Epic 4 (`F4.*`): Quản lý Sàn phục vụ, Gộp bàn & Đơn Mang về
- **`F4.1` — Sơ đồ bàn trực quan 4 trạng thái:** Hiển thị đồng bộ trạng thái từng bàn (*Trống / Đang có khách / Chờ thanh toán / Tạm ngưng*).
- **`F4.2` — Đặt món hộ khách tại bàn:** Nhân viên phục vụ chọn món và gửi đơn vào bếp thay cho thực khách không dùng điện thoại.
- **`F4.3` — Gộp bàn & Hủy gộp bàn nhóm đông:** Ghép nhiều bàn vật lý vào cùng một phiên phục vụ dùng chung hóa đơn và tách lại an toàn khi cần.
- **`F4.4` — Quản lý đơn mua mang về (Takeaway):** Tiếp nhận, theo dõi bếp và thanh toán cho khách mang về mà không chiếm dụng bàn ăn.
- **`F4.5` — Hàng đợi yêu cầu bàn thời gian thực:** Nhân viên phục vụ theo dõi tập trung các yêu cầu hỗ trợ từ bàn ăn kèm mức độ ưu tiên theo thời gian chờ, nhận xử lý (tránh 2 nhân viên nhận trùng một yêu cầu) và xác nhận hoàn tất tức thì.

### Epic 5 (`F5.*`): Quản lý Kho Nguyên liệu & Định lượng Tự động (BOM)
- **`F5.1` — Danh mục nguyên vật liệu:** Quản lý tên nguyên liệu, đơn vị tính, giá vốn và ngưỡng tồn kho tối thiểu.
- **`F5.2` — Thiết lập công thức định lượng món (BOM):** Khai báo tỷ lệ nguyên liệu tiêu hao cho từng món ăn/combo.
- **`F5.3` — Tự động giữ chỗ & trừ tồn kho:** Tự động kiểm soát lượng nguyên liệu khi nhận/hoàn tất món và hoàn trả khi món bị hủy.
- **`F5.4` — Nhập kho & Điều chỉnh tồn kho:** Ghi nhận phiếu nhập nguyên liệu mới, điều chỉnh hao hụt và lưu đầy đủ lịch sử biến động kho.
- **`F5.5` — Cảnh báo tồn kho thấp:** Cảnh báo nổi bật nguyên liệu chạm đáy an toàn để bếp và quản lý kịp thời nhập hàng hoặc tạm tắt món.
- **`F5.6` — AI Gợi ý nhập kho thông minh từ BOM + Dự báo bán:** Kết hợp công thức định lượng từng món với tốc độ tiêu thụ 30 ngày gần nhất và tồn kho khả dụng hiện tại để tính số ngày còn đủ bán, xếp hạng khẩn cấp các nguyên liệu sắp thiếu theo khung dự báo (3/7/14 ngày), ước tính chi phí nhập và hỗ trợ mở nhanh phiếu nhập kho 1 chạm.
- **`F5.7` — AI Gợi ý công thức BOM & Kiểm tra giá vốn món:** Giúp Quản lý (`PER-05`) và Đầu bếp (`PER-03`) ước tính tỷ lệ giá vốn nguyên liệu trên giá bán (Food Cost %), phát hiện nguy cơ lỗ biên lợi nhuận hoặc thiếu đơn giá nhập và để AI đề xuất danh sách nguyên liệu định lượng từ kho hiện có kèm gợi ý giá bán tối thiểu hợp lý.
- **`F5.8` — Nhập kho tự động bằng chụp ảnh hóa đơn (AI Vision):** Cho phép Quản lý (`PER-05`) và Đầu bếp (`PER-03`) chụp ảnh hoặc tải lên ảnh hóa đơn mua hàng từ nhà cung cấp để AI tự động đọc tên hàng, số lượng, đơn vị, đơn giá, đối chiếu vào danh mục nguyên liệu hiện có kèm tự quy đổi đơn vị phổ biến, cho phép rà soát/chỉnh sửa trực quan trước khi xác nhận nhập kho hàng loạt trong một lần bấm.

### Epic 6 (`F6.*`): Quản trị Cửa hàng, Báo cáo, AI & Bảo mật
- **`F6.1` — Quản trị Thực đơn, Danh mục & Combo (kèm AI mô tả):** Tạo/sửa thực đơn, ảnh món, nhóm tùy chọn, combo và dùng AI hỗ trợ viết mô tả hấp dẫn.
- **`F6.2` — Dự báo Doanh thu & Nhu cầu bằng AI:** Phân tích dữ liệu lịch sử để dự báo lượng khách, doanh thu và gợi ý nhập nguyên liệu cho chủ quán.
- **`F6.3` — Bảng điều khiển & Báo cáo Kinh doanh:** Thống kê tổng quan, doanh thu chi tiết, món bán chạy nhất và hiệu suất nhân sự theo thời gian.
- **`F6.4` — Quản lý Nhân sự, Phân quyền 6 vai trò & Bảo mật 2FA:** Phân tách rõ quyền hạn từng vai trò, yêu cầu đổi mật khẩu lần đầu và xác thực 2 lớp (OTP) cho chủ quán.
- **`F6.5` — Cấu hình Nhà hàng, Song ngữ & Chế độ Sáng/Tối:** Thiết lập thông tin quán, thương hiệu, tùy chọn thanh toán (gồm bật/tắt tự thanh toán tại bàn), ngôn ngữ Việt – Anh và giao diện Sáng/Tối.
- **`F6.6` — AI Tóm tắt ca & Phân tích điều hành thông minh:** Tổng hợp tình hình doanh thu so với kỳ trước, món bán chạy, đơn/món bị hủy, cảnh báo chậm bếp (> 20 phút), tồn đọng yêu cầu phục vụ bàn (> 5 phút) và nguyên liệu sắp hết theo ngày/ca chọn lọc, đưa ra điểm nhấn, cảnh báo rủi ro và 2–3 hành động khuyến nghị cụ thể cho Quản lý & Chủ quán.
- **`F6.7` — AI Đề xuất tạo Combo & Khung giờ Khuyến mãi:** Phân tích lịch sử các cặp món thường được đặt cùng nhau và biểu đồ lưu lượng đơn hàng theo khung giờ/ngày trong tuần để đề xuất các gói Combo hấp dẫn (kèm giá ưu đãi hợp lý) và khung giờ Khuyến mãi kích cầu giờ vắng khách, cho phép Quản lý tạo bản nháp hoàn chỉnh trong 1 chạm trước khi lưu áp dụng.
- **`F6.8` — Quản lý Gói dịch vụ (Free / Pro) & Nâng cấp tự động:** Phân tầng rõ ràng giữa gói Cơ bản (đáp ứng trọn luồng bán hàng, gọi món QR, bếp KDS và thu ngân tại quầy với hạn mức bàn/nhân sự cơ bản) và gói Pro (mở khóa bộ trợ lý AI khách hàng & vận hành, quản trị kho nguyên liệu BOM và không giới hạn quy mô bàn/nhân sự); cho phép Chủ quán kích hoạt dùng thử Pro 14 ngày miễn phí một lần hoặc thanh toán nâng cấp tự động qua VietQR kèm hạ gói an toàn không mất dữ liệu khi hết hạn.

---

## 3. Ma trận Phụ thuộc Nghiệp vụ (Business Dependency Matrix)

| Tính năng phụ thuộc (Downstream) | Điều kiện nghiệp vụ tiên quyết (Upstream) | Nguyên tắc xử lý ngoại lệ ở góc độ sản phẩm |
|---|---|---|
| **Gửi đơn đặt món (`F1.4`)** | Quét QR bàn hợp lệ (`F1.1`) & Bàn đang hoạt động (`F4.1`) | Từ chối đặt món nếu bàn đang tạm ngưng hoặc mã bàn không hợp lệ; chặn tạo đơn trùng khi khách bấm gửi nhiều lần. |
| **Gộp bàn nhóm đông (`F4.3`)** | Có từ 2 bàn trở lên đang hoạt động (`F4.1`) | Bảo toàn toàn bộ món các bàn đã gọi trước đó vào chung 1 hóa đơn; không cho phép gộp bàn đang tạm ngưng. |
| **Bếp nhận & nấu gom món (`F2.1`, `F2.2`)** | Có đơn hợp lệ từ bàn (`F1.4`), nhân viên (`F4.2`) hoặc mang về (`F4.4`) | Giữ ổn định danh sách món chờ; nếu mất kết nối tạm thời phải cảnh báo và tự đồng bộ lại ngay khi có mạng. |
| **Tự động giữ/trừ kho (`F5.3`)** | Món đã được khai báo công thức (`F5.2`) & có biến động đơn/bếp (`F2.1`) | Nếu nguyên liệu xuống dưới ngưỡng hoặc không đủ, cảnh báo ngay cho bếp và quản lý (`F5.5`) mà không làm đứng quy trình phục vụ. |
| **Thanh toán VietQR / Tự thanh toán (`F3.2`)** | Đơn đang chờ thanh toán; với khách tự thanh toán cần chủ quán bật tính năng (`F6.5`) | Từ chối tạo mã QR nếu đơn đã thanh toán xong; cho phép hủy mã QR đang chờ nếu khách đổi mã giảm giá hoặc đổi sang tiền mặt. |
| **Đóng phiên & Giải phóng bàn (`F3.3`)** | Giao dịch chuyển khoản được xác nhận (`F3.2`) hoặc Thu ngân chốt tiền mặt (`F3.1`) | Toàn bộ các bàn trong cùng nhóm gộp phải được giải phóng đồng thời về trạng thái trống để đón lượt khách mới. |
| **Gọi nhân viên & Hàng đợi hỗ trợ (`F1.6`, `F4.5`)** | Quét QR bàn hợp lệ (`F1.1`) & Bàn đang hoạt động (`F4.1`) | Chống khách bấm spam/gửi trùng lặp theo khoảng chờ và giới hạn yêu cầu mở; khóa nhận xử lý duy nhất cho 1 nhân viên để tránh 2 người chạy trùng ra cùng một bàn; tự khép yêu cầu còn mở khi bàn kết thúc phiên. |
