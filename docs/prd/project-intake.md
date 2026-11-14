**Project Intake: QROS (Smart QR Ordering & Restaurant Operating System)**  
***Mô-đun:*** *`project-intake.md`*  
   
 ***Giai đoạn:*** *Discovery & Foundation*  
   
 ***Trạng thái:*** *Approved / Ready for Execution*  
   
 ***Chủ quản dự án:*** *Ban Quản trị & Nhóm Kỹ thuật Nền tảng F&B*  
  
---  
**1. Project Brief & Executive Summary**  
**QROS** là nền tảng quản trị và vận hành nhà hàng toàn diện, tập trung vào trải nghiệm gọi món tại bàn thông qua mã QR thời gian thực. Hệ thống giải quyết bài toán tắc nghẽn giờ cao điểm, sai sót trong khâu ghi nhận đơn hàng, và độ trễ giao tiếp giữa 3 mắt xích trọng yếu: **Khách hàng tại bàn – Nhân viên phục vụ – Bếp (KDS) – Thu ngân**.  
Khác với các giải pháp QR tĩnh (chỉ hiển thị menu PDF) hoặc các ứng dụng bắt buộc cài đặt phức tạp, QROS hoạt động theo cơ chế **Zero-Install Web App (PWA/SPA)**:  
- Khách hàng chỉ cần quét mã QR định danh bàn (chứa chữ ký bảo mật `sig`) là có thể lập tức xem thực đơn động, tùy biến món, áp dụng voucher, gộp bàn khi đi nhóm đông và gửi lệnh order trực tiếp vào màn hình bếp KDS.  
- Nhân sự và cấp quản lý vận hành qua cổng quản trị phân quyền đa tầng (RBAC) với 6 vai trò chuyên biệt, hỗ trợ cập nhật kho tự động theo công thức (BOM), thanh toán tích hợp PayOS/tiền mặt, và dự báo doanh số thông minh bằng AI (Google Gemini).  
  
---  
**2. Project Boundary (Ranh giới & Phạm vi dự án)**  
Để đảm bảo tính khả thi, tối ưu tài nguyên và bám sát mục tiêu cốt lõi, ranh giới dự án được xác định rõ ràng:  
**2.1 In-Scope (Thuộc phạm vi thực hiện)**  
- **Kênh thực khách tại bàn (Dine-in):** Quét mã QR theo bàn, xác thực phiên hợp lệ, xem danh mục/món ăn/combo, ghi chú món (ít đá, không hành), giỏ hàng thời gian thực, gửi đơn vào bếp, kiểm tra trạng thái món theo thời gian thực (STOMP WebSocket), gọi thanh toán.  
- **Hỗ trợ nghiệp vụ bàn phức tạp:** Tính năng gộp nhiều bàn vật lý vào một phiên thanh toán duy nhất (`table_session_tables`), chuyển bàn, tách bàn và bảo toàn đơn hàng.  
- **Màn hình bếp chuyên dụng (Kitchen Display System - KDS):** Kanban điều phối 3 cột (`PENDING`, `COOKING`, `READY`), chế độ nấu gom (Batch Cooking), cảnh báo âm thanh món quá thời gian chờ (>20 phút).  
- **Thu ngân & Thanh toán thông minh:** Hỗ trợ thanh toán tiền mặt (tự động tính tiền thối), tích hợp thanh toán mã QR động tự động đối soát qua PayOS Webhook, áp dụng mã giảm giá/voucher (theo % hoặc số tiền cố định, giới hạn lượt dùng, hóa đơn tối thiểu).  
- **Quản lý kho & Định lượng tự động (BOM - Bill of Materials):** Khai báo nguyên liệu, đơn vị tính, thiết lập công thức món ăn, tự động trừ kho khi món hoàn tất chế biến, cảnh báo tồn kho an toàn, phiếu nhập kho/điều chỉnh kho.  
- **Hệ thống phân quyền & Bảo mật (RBAC + AAA):** Phân chia 6 vai trò (`CUSTOMER`, `STAFF`, `CHEF`, `CASHIER`, `MANAGER`, `ADMIN`), xác thực JWT + HttpOnly Cookie, xác thực hai lớp (2FA OTP) đối với tài khoản `ADMIN`.  
- **Trí tuệ nhân tạo (AI Engine):** Tích hợp Gemini AI hỗ trợ tạo mô tả món ăn hấp dẫn, gợi ý combo thông minh cho quản lý và trợ lý hỏi đáp thông số kinh doanh cho nhân viên.  
**2.2 Out-of-Scope (Ngoại vi - Không thực hiện trong giai đoạn này)**  
- **Sàn giao dịch giao hàng ngoại vi (Food Delivery Aggregator):** Không tích hợp đồng bộ tự động với GrabFood, ShopeeFood, Baemin hay BeFood.  
- **Hệ thống chuỗi nhượng quyền đa pháp nhân (Multi-Tenant Organization Hierarchy):** Giai đoạn này tập trung hoàn thiện mô hình Single-Store / Single-Tenant chuẩn hóa cao trước khi mở rộng mô hình Franchise.  
- **Tích hợp phần cứng IoT bếp chuyên sâu:** Không can thiệp vào máy chiên, lò nướng hoặc cảm biến nhiệt độ thông minh.  
- **Ví điện tử nội bộ tích điểm đổi tiền mặt (Closed-loop e-wallet):** Không triển khai mô hình nạp tiền lưu ký vào ví thành viên để tránh các rủi ro pháp lý về giấy phép trung gian thanh toán.  
  
---  
**3. Completed Project Intake Form (Biểu mẫu tiếp nhận dự án hoàn chỉnh)**  
**3.1 Thông tin chung**  
  
|  |  |  
| - | - |  
| **Mục tiêu** |  **Chi tiết tiếp nhận** |  
| **Tên dự án** |  **QROS (Smart QR Order & Restaurant Operating System)** |  
| **Mã định danh dự án** |  `PRJ-FNB-QROS-2026` |  
| **Nhà tài trợ / Sponsor** |  Ban Giám đốc Khối Công nghệ & Khối Vận hành Nhà hàng |  
| **Product Owner** |  Lead Product Manager & F&B Operations Architect |  
| **Technical Lead** |  Senior Backend Architect (Java/Spring Boot) & Lead Frontend Architect (React) |  
| **Thời gian bắt đầu** |  Quý 1 / 2026 |  
| **Mục tiêu triển khai chính** |  Vận hành thử nghiệm tại 5 chi nhánh chuỗi ẩm thực; nhân rộng 50+ điểm bán trong 12 tháng |  
  
   
**3.2 Tuyên ngôn vấn đề & Cơ hội kinh doanh**  
- **Vấn đề tiếp nhận:** Vào các khung giờ cao điểm (11h30 - 13h30 và 18h30 - 20h30), thời gian chờ gọi món của khách lên tới 10-15 phút do quá tải nhân sự phục vụ. Tỷ lệ sai sót order tay là 4.8%. Tỷ lệ thất thoát nguyên liệu chưa giải trình được chiếm 3.2% tổng giá vốn hàng bán (COGS).  
- **Cơ hội kinh doanh:** Tự động hóa khâu gọi món giúp giảm tải ít nhất 40% khối lượng công việc của nhân viên phục vụ, tăng tốc độ quay vòng bàn (table turnover) thêm 25%, nâng biên lợi nhuận ròng nhờ cắt giảm thất thoát và chi phí nhân sự ca đỉnh điểm.  
**3.3 Danh sách bên liên quan (Stakeholder Matrix)**  
  
|  |  |  |  
| - | - | - |  
| **Nhóm liên quan** |  **Kỳ vọng chính** |  **Tiêu chí thành công** |  
| **Chủ nhà hàng / Ban Giám đốc** |  Tối ưu hóa chi phí vận hành, minh bạch dòng tiền, kiểm soát kho chặt chẽ, báo cáo doanh thu tức thời. |  Báo cáo P&L chính xác, tỷ lệ thất thoát kho < 0.8%, ROI đạt điểm hòa vốn sau 6 tháng. |  
| **Quản lý nhà hàng (Store Manager)** |  Dễ dàng quản lý menu, điều phối ca làm việc, giải quyết sự cố gộp/chuyển bàn mượt mà, kiểm soát voucher. |  Giảm 90% khiếu nại của khách hàng về nhầm bill hoặc chờ món quá lâu. |  
| **Bếp trưởng (Head Chef)** |  Nhận đơn tức thì, rõ ràng món/topping/ghi chú, không bị sót đơn giấy, có chế độ nấu gom món cùng loại. |  Thời gian lên món trung bình giảm từ 18 phút xuống dưới 9 phút. |  
| **Thu ngân (Cashier)** |  Tốc độ thanh toán nhanh, hỗ trợ cả tiền mặt lẫn chuyển khoản QR VietQR tự động khớp tiền, không bị lệch sổ. |  Thời gian chốt bill < 30 giây/bàn, tỷ lệ sai lệch đối soát = 0%. |  
| **Thực khách (Dine-in Customer)** |  Không cần xếp hàng hay chờ gọi nhân viên, giao diện dễ xem, món ăn trực quan, thanh toán tiện lợi tại bàn. |  Điểm hài lòng khách hàng (CSAT) > 4.7/5.0. |  
  
   
**3.4 Ràng buộc kỹ thuật & Tiêu chuẩn phi chức năng (Non-Negotiable Constraints)**  
1. **Hiệu năng & Tải trọng (Performance):** Thời gian phản hồi API trung bình < 200ms; hỗ trợ ít nhất 1,000 kết nối đồng thời (concurrent connections) trên mỗi instance.  
2. **Real-time Engine:** Độ trễ nhận đơn giữa khách hàng và bếp KDS qua WebSocket STOMP < 500ms; tự động tái kết nối (auto-reconnect) khi rớt mạng tạm thời.  
3. **Bảo mật & Toàn vẹn dữ liệu (Security & Integrity):**  
  - Không cho phép gọi món bừa bãi ngoài bàn: QR code mã hóa kèm chữ ký số xác thực bàn (`sig`).  
  - Mật khẩu mã hóa BCrypt, cơ chế 2FA bắt buộc cho tài khoản `ADMIN`.  
  - Cơ chế khóa bi quan (Pessimistic Locking) trong cơ sở dữ liệu khi thao tác gộp bàn hoặc thanh toán nhằm triệt tiêu hoàn toàn Race Condition.  
4. **Trải nghiệm người dùng:** Giao diện gọi món tối ưu 100% cho thiết bị di động (Mobile First), không yêu cầu đăng nhập đối với khách tại bàn.  
**3.5 Giả định & Kế hoạch phòng ngừa rủi ro (Assumptions & Risks)**  
  
|  |  |  |  
| - | - | - |  
| **Giả định / Rủi ro** |  **Mức độ** |  **Kế hoạch dự phòng / Khắc phục** |  
| Mạng Internet tại nhà hàng bị mất kết nối |  Cao |  Triển khai cơ chế Offline Banner, cảnh báo mất kết nối ngay trên màn hình thu ngân và bếp; hỗ trợ nhân viên dùng mạng 4G/5G cục bộ để tiếp tục ghi nhận đơn. |  
| Thực khách cao tuổi không quen thao tác QR |  Trung bình |  Nhân viên phục vụ sở hữu ứng dụng nội bộ (`/admin/tables`) để hỗ trợ đặt món hộ trực tiếp trên hệ thống mà không phá vỡ luồng KDS. |  
| Sự cố rớt mạng từ cổng thanh toán PayOS |  Thấp |  Cung cấp luồng thanh toán tiền mặt tức thời dự phòng, kèm cơ chế Webhook Retry và nút kiểm tra trạng thái thanh toán thủ công cho thu ngân. |  
  
   
