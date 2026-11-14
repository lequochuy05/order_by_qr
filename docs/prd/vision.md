**Product Vision, Strategic Direction & Core Value Pillars**  
***Mô-đun:*** *`vision.md`*  
   
 ***Giai đoạn:*** *Strategic Product Definition*  
   
 ***Dự án:*** *QROS (Smart QR Order & Restaurant Operating System)*  
  
---  
**1. Tuyên ngôn tầm nhìn sản phẩm (Product Vision Statement)**  
***"Biến mọi bàn ăn nhà hàng thành một điểm tiếp xúc số thông minh – nơi thực khách được phục vụ ngay lập tức mà không cần chờ đợi; đội ngũ nhân viên và đầu bếp làm việc trong sự thảnh thơi, nhịp nhàng; và chủ nhà hàng kiểm soát từng dòng tiền, nguyên vật liệu trong thời gian thực với sự trợ lực của Trí tuệ Nhân tạo."***  
QROS không đơn thuần là một phần mềm tạo mã QR gọi món; QROS là **hệ điều hành vận hành nhà hàng thế hệ mới (Next-Gen Restaurant OS)** hợp nhất toàn bộ dòng chảy thông tin từ lúc khách bước vào quán cho đến khi bước ra, biến dữ liệu vận hành thành lợi thế cạnh tranh sống còn.  
  
---  
**2. Chỉ số Bắc Đẩu (North Star Metric)**  
Để định hướng toàn bộ nỗ lực phát triển sản phẩm, kỹ thuật và vận hành, QROS xác định chỉ số Bắc Đẩu duy nhất:  
**⭐ The Perfect Table Journey Rate (Tỷ lệ Phiên Phục vụ Hoàn hảo - PTJR)**  
Được định nghĩa là: **Tỷ lệ % các bàn ăn hoàn tất toàn bộ chu trình đáp ứng đồng thời 4 tiêu chí:**  
1. Thời gian từ lúc quét QR đến khi đơn vào bếp < 90 giây.  
2. 100% món ăn được phục vụ chính xác theo ghi chú và không có món nào bị quá hạn chờ 20 phút.  
3. Không xảy ra tranh chấp dữ liệu khi gộp/chuyển bàn.  
4. Thời gian thanh toán và đối soát tiền hoàn tất trong vòng 30 giây.  
*Mục tiêu chiến lược:* Đạt chỉ số PTJR ≥ **95%** trên toàn bộ các nhà hàng vận hành QROS.  
  
---  
**3. Ba Trụ cột Giá trị Cốt lõi (Core Value Pillars)**  
```  
                       ┌────────────────────────────────────────┐  
                        │           QROS VALUE ENGINE            │  
                        └───────────────────┬────────────────────┘  
                                            │  
          ┌─────────────────────────────────┼─────────────────────────────────┐  
          │                                 │                                 │  
 ┌────────▼──────────────┐       ┌──────────▼────────────┐       ┌────────────▼────────────┐  
 │ PILLAR 1: SEAMLESS    │       │ PILLAR 2: OPERATIONAL │       │ PILLAR 3: INTELLIGENT   │  
 │ DINING EXPERIENCE     │       │ EXCELLENCE IN KITCHEN │       │ FINANCIALS & INVENTORY  │  
 ├───────────────────────┤       ├───────────────────────┤       ├─────────────────────────┤  
 │ • Zero-install WebApp │       │ • Realtime KDS Kanban │       │ • PayOS VietQR Webhook  │  
 │ • Dynamic Signature QR│       │ • Smart Batch Cooking │       │ • Real-time BOM Stock   │  
 │ • Instant Order Sync  │       │ • Audio Overdue Alert │       │ • Gemini AI Forecasting │  
 │ • Native Table Merge  │       │ • Table Map Sync      │       │ • 2FA Secure Audit Trail│  
 └───────────────────────┘       └───────────────────────┘       └─────────────────────────┘  
   
```  
**Trụ cột 1: Trải nghiệm ẩm thực liền mạch (Seamless Dining Experience)**  
- Trao quyền chủ động tối đa cho thực khách: Không cần tải app, không rào cản đăng ký tài khoản.  
- Menu số sống động, trực quan với hình ảnh sắc nét, phân loại danh mục thông minh và tùy chọn món chi tiết (đường, đá, topping, ghi chú đặc biệt).  
- Hỗ trợ nhóm đông người thông qua cơ chế gộp bàn độc quyền: Cả nhóm cùng quét QR, cùng gọi món vào một giỏ hàng chung, theo dõi tiến độ bếp lên món tức thời.  
**Trụ cột 2: Tối ưu hóa vận hành Sàn & Bếp (Operational Excellence in Kitchen & Floor)**  
- Cắt đứt hoàn toàn sự phụ thuộc vào phiếu order giấy dễ rách, ướt và thất lạc.  
- Màn hình KDS chuẩn hóa 3 trạng thái (`PENDING` -> `COOKING` -> `READY`) kết hợp thuật toán gom mẻ nấu (Batch Cooking) giúp tăng 40% công suất ra món của đầu bếp.  
- Hệ thống chuông cảnh báo âm thanh và đếm giờ thông minh ngăn chặn triệt để tình trạng bỏ quên đơn của khách.  
- Sơ đồ bàn ăn (Table Map) đồng bộ trạng thái hai chiều giữa nhân viên phục vụ và thu ngân qua WebSocket STOMP.  
**Trụ cột 3: Tài chính minh bạch & Quản trị kho thông minh (Intelligent Financials & Inventory)**  
- Tự động hóa thanh toán không chạm qua cổng PayOS: Khách quét VietQR động, tiền vào tài khoản nhà hàng, đơn tự động đóng chỉ trong 1 giây mà không cần nhân viên chụp màn hình xác minh.  
- Định lượng công thức nguyên liệu (BOM) tự động trừ kho khi món hoàn tất chế biến, ngăn chặn thất thoát nguyên liệu thực phẩm (Food Waste).  
- Tích hợp mô hình ngôn ngữ lớn (Gemini AI) phân tích lịch sử giao dịch để dự báo chính xác nhu cầu nguyên vật liệu và doanh số các ngày kế tiếp.  
- Cơ chế bảo mật cấp doanh nghiệp: Phân quyền 6 vai trò, bắt buộc 2FA OTP cho quản trị viên tối cao, lưu vết lịch sử giao dịch toàn diện (Audit Log).  
  
---  
**4. Bảng phân phối giá trị cho các bên liên quan (Value Proposition Canvas)**  
  
|  |  |  |  
| - | - | - |  
| **Bên liên quan** |  **Giá trị nhận được từ QROS** |  **Tác động kinh doanh trực tiếp** |  
| **Khách hàng (Customer)** |  Không phải chờ đợi vẫy nhân viên; gọi món đúng ý 100%; thanh toán QR tiện lợi tại bàn. |  Tăng 35% mức độ hài lòng; tăng 20% khả năng quay lại nhà hàng. |  
| **Nhân viên (Staff)** |  Giảm 50% áp lực di chuyển và ghi chép tay; tập trung vào thái độ chăm sóc khách niềm nở. |  Giảm tỷ lệ kiệt sức (burnout); giảm tỷ lệ nhảy việc của nhân sự ca. |  
| **Bếp trưởng (Chef)** |  Không gian bếp yên tĩnh, ngăn nắp; biết chính xác cần nấu bao nhiêu đĩa cùng loại; không bị sót đơn. |  Rút ngắn 45% thời gian chế biến; giảm 90% đồ ăn bị làm sai phải hủy bỏ. |  
| **Thu ngân (Cashier)** |  Tự động hóa tính tiền thối và đối soát VietQR; không lo lệch sổ tiền két cuối ngày. |  Chốt bill chỉ trong 15-30 giây; loại bỏ 100% gian lận voucher. |  
| **Chủ nhà hàng (Owner)** |  Tối ưu hóa chi phí nhân sự; tăng tốc độ quay vòng bàn; kiểm soát thất thoát kho và dòng tiền chặt chẽ. |  Tăng 15 - 25% doanh thu thuần; tiết kiệm 20% chi phí vận hành hàng tháng. |  
  
   
  
---  
**5. Mục tiêu chiến lược định lượng (Strategic OKRs)**  
**Objective 1: Tối đa hóa công suất phục vụ bàn ăn và cắt giảm thời gian chết**  
- **KR 1.1:** Rút ngắn thời gian từ lúc khách ngồi vào bàn đến khi bếp nhận đơn xuống dưới 90 giây.  
- **KR 1.2:** Tăng chỉ số quay vòng bàn (Table Turnover Rate) trong khung giờ cao điểm thêm tối thiểu 25%.  
- **KR 1.3:** Tỷ lệ món ăn bị quá hạn chế biến (>20 phút) duy trì dưới 1.0%.  
**Objective 2: Triệt tiêu hoàn toàn sai sót và thất thoát vận hành**  
- **KR 2.1:** Tỷ lệ gọi nhầm món / nhầm bàn / nhầm ghi chú topping giảm về 0%.  
- **KR 2.2:** Sai lệch giữa số liệu kho trên hệ thống và số liệu kiểm kê thực tế < 0.5%.  
- **KR 2.3:** Tỷ lệ đối soát tự động thành công của các giao dịch PayOS VietQR đạt 99.9%.  
**Objective 3: Gia tăng giá trị đơn hàng trung bình (AOV) bằng Trí tuệ Nhân tạo**  
- **KR 3.1:** 100% món ăn trên menu được hỗ trợ sinh mô tả hấp dẫn bằng Gemini AI.  
- **KR 3.2:** Tăng 18% tỷ lệ khách hàng lựa chọn gọi thêm các gói Combo hoặc Topping được gợi ý thông minh trên giao diện gọi món.  
- **KR 3.3:** Độ chính xác của mô hình AI dự báo doanh thu đạt trên 85% so với doanh số thực tế.  
