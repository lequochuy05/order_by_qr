# Backlog User Stories & Acceptance Criteria — QROS (`03-ba`)

> **Tầng Knowledge:** `03-ba` (Business Analysis — User Stories & Acceptance Criteria)  
> **Sinh bởi Skill:** `.agents/skills/ba-user-story/SKILL.md`  
> **Kế thừa bắt buộc từ:**
> - **PO (`01-po/`):** [personas.md](../01-po/personas.md) (`PER-01` → `PER-06`) · [journey-features.md](../01-po/journey-features.md) (`STG-01` → `STG-06`, `F1.1` → `F6.7`)
> - **Design (`02-design/`):** [PROTOTYPE_SCREEN_MAP.md](../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-01` → `SCR-27`) · [DESIGN_SYSTEM.md](../02-design/DESIGN_SYSTEM.md)
> - **BA PRD (`03-ba/`):** [PRD_BY_USER_STAGE.md](./PRD_BY_USER_STAGE.md) (`BR-01` → `BR-36`)  
> **Bàn giao cho:** [SYSTEM_DESIGN.md](../04-tech/SYSTEM_DESIGN.md) (Tầng 4 — SE thiết kế kỹ thuật) & QA *(Tổng: 34 User Stories `US-001` → `US-034`)*

---

## Stage 1 (`STG-01`): Khám phá, Quét QR, Xác thực & Bảo mật phiên

### `US-001`: Quét QR bàn để mở Menu tức thì không cần cài app
- **Stage:** `STG-01` | **Persona:** `PER-01 (Customer)` | **Priority:** `P0`
- **Traceability:** PO `F1.1` · Design `SCR-01`, `SCR-05`, `SCR-08` · Rules `BR-01`, `BR-24`
- **Story:** Là một **Thực khách**, tôi muốn quét mã QR trên bàn để mở ngay thực đơn đúng số bàn trên điện thoại mà không cần cài ứng dụng hay đăng nhập, để bắt đầu gọi món trong dưới 90 giây.
- **Acceptance Criteria:**
  1. **Given** mã QR của bàn hợp lệ và bàn không ở trạng thái *Tạm ngưng*, **When** tôi mở liên kết QR, **Then** màn hình `SCR-05` hiển thị đúng số bàn, thực đơn hiện hành và cho phép đổi ngôn ngữ `VI/EN`.
  2. **Given** bàn đang ở trạng thái *Tạm ngưng* hoặc mã QR bị sửa đổi không hợp lệ, **When** tôi truy cập `SCR-05`, **Then** hệ thống vô hiệu hóa thao tác đặt món và hiển thị thông báo mời liên hệ nhân viên.
  3. **Given** thiết bị mất kết nối Internet, **When** tôi tải trang hoặc thao tác, **Then** hiển thị `SCR-08` hoặc dải cảnh báo mất kết nối có nút thử lại.

### `US-002`: Đăng nhập nhân sự & điều hướng đúng không gian theo vai trò
- **Stage:** `STG-01` | **Persona:** `PER-02..05 (Staff/Chef/Cashier/Manager)` | **Priority:** `P0`
- **Traceability:** PO `F6.4` · Design `SCR-02`, `SCR-06` · Rules `BR-03`, `BR-04`
- **Story:** Là một **Nhân sự nhà hàng**, tôi muốn đăng nhập tại `SCR-02` và được đưa thẳng tới màn hình làm việc đúng vai trò của mình, để vào ca nhanh chóng mà không lạc hướng.
- **Acceptance Criteria:**
  1. **Given** tôi nhập đúng tài khoản của `PER-03 (Chef)`, **When** tôi đăng nhập thành công trên `SCR-02`, **Then** hệ thống đưa tôi thẳng tới màn hình Bếp `SCR-12` *(tương tự: `PER-02`/`PER-04` → `SCR-11`, `PER-05` → `SCR-10`)*.
  2. **Given** tôi nhập sai mật khẩu liên tiếp vượt ngưỡng cho phép, **When** tôi thử đăng nhập tiếp, **Then** tài khoản bị khóa tạm thời và `SCR-02` thông báo rõ thời điểm có thể thử lại.
  3. **Given** tôi là `PER-02 (Staff)` nhưng cố mở màn hình báo cáo doanh thu `SCR-22`, **When** hệ thống kiểm tra quyền, **Then** chuyển hướng ngay sang `SCR-06` (Không có quyền truy cập).

### `US-003`: Xác thực 2 lớp (2FA OTP) cho tài khoản Chủ nhà hàng
- **Stage:** `STG-01` | **Persona:** `PER-06 (Admin)` | **Priority:** `P0`
- **Traceability:** PO `F6.4` · Design `SCR-02`, `SCR-10` · Rules `BR-02`
- **Story:** Là một **Chủ nhà hàng**, tôi muốn phải nhập thêm mã xác thực OTP 6 số gửi qua email mỗi khi đăng nhập, để bảo vệ dữ liệu doanh thu và cấu hình nhận tiền khỏi truy cập trái phép.
- **Acceptance Criteria:**
  1. **Given** tôi nhập đúng email và mật khẩu của `PER-06`, **When** bấm đăng nhập trên `SCR-02`, **Then** hệ thống gửi mã OTP 6 số có thời hạn ngắn và chuyển giao diện sang bước nhập 6 ô OTP.
  2. **Given** tôi nhập đúng mã OTP còn hiệu lực, **When** bấm xác nhận, **Then** tôi được cấp phiên làm việc và chuyển vào Dashboard `SCR-10`.
  3. **Given** tôi bấm "Gửi lại OTP" quá nhanh liên tiếp, **When** vượt tần suất cho phép, **Then** nút gửi lại tạm khóa kèm đồng hồ đếm ngược thời gian chờ trên `SCR-02`.

### `US-004`: Khôi phục mật khẩu an toàn & bắt buộc đổi mật khẩu lần đầu
- **Stage:** `STG-01` | **Persona:** `PER-02..06` | **Priority:** `P1`
- **Traceability:** PO `F6.4` · Design `SCR-03`, `SCR-04`, `SCR-26` · Rules `BR-03`
- **Story:** Là một **Nhân sự**, tôi muốn tự đặt lại mật khẩu qua email khi quên và đổi mật khẩu mặc định khi mới được cấp tài khoản, để đảm bảo an toàn tài khoản cá nhân.
- **Acceptance Criteria:**
  1. **Given** tôi nhập một địa chỉ email hợp lệ định dạng trên `SCR-03`, **When** gửi yêu cầu khôi phục, **Then** hệ thống luôn hiển thị thông báo xác nhận trung tính (không tiết lộ email có tồn tại hay không).
  2. **Given** tôi mở liên kết đặt lại mật khẩu còn hiệu lực trên `SCR-04` và nhập mật khẩu mới đạt chuẩn độ mạnh, **When** xác nhận, **Then** mật khẩu được cập nhật, mọi phiên đăng nhập cũ bị hủy hiệu lực và tôi đăng nhập lại được bằng mật khẩu mới.
  3. **Given** tôi đăng nhập lần đầu bằng tài khoản mới cấp có cờ bắt buộc đổi mật khẩu, **When** đăng nhập thành công, **Then** hệ thống dẫn tới `SCR-26` yêu cầu đổi mật khẩu trước khi thao tác các chức năng khác.

---

## Stage 2 (`STG-02`): Duyệt Thực đơn, Trợ lý AI & Đặt món

### `US-005`: Duyệt thực đơn theo danh mục/combo & tùy biến kích cỡ, topping, ghi chú
- **Stage:** `STG-02` | **Persona:** `PER-01 (Customer)` | **Priority:** `P0`
- **Traceability:** PO `F1.2`, `F1.3` · Design `SCR-05` · Rules `BR-05`, `BR-24`
- **Story:** Là một **Thực khách**, tôi muốn xem ảnh món rõ nét, lọc theo danh mục/combo và chọn kích cỡ, topping, ghi chú khẩu vị ngay trên `SCR-05`, để gọi đúng món theo ý muốn.
- **Acceptance Criteria:**
  1. **Given** tôi đang xem `SCR-05`, **When** tôi chọn một món có nhóm tùy chọn, **Then** hộp thoại tùy chọn mở lên, cập nhật lại giá tạm tính theo topping đã chọn và lưu được ghi chú khẩu vị.
  2. **Given** một món đã hết hàng hoặc tạm ngưng bán, **When** thực đơn hiển thị, **Then** thẻ món gắn nhãn *Hết hàng* và vô hiệu hóa nút thêm vào giỏ.
  3. **Given** danh mục hoặc từ khóa tìm kiếm không có món phù hợp, **When** kết quả trả về rỗng, **Then** hiển thị trạng thái rỗng (`Empty State`) thân thiện với nút xóa bộ lọc.

### `US-006`: Hỏi Trợ lý AI gợi ý món ăn tại bàn
- **Stage:** `STG-02` | **Persona:** `PER-01 (Customer)` | **Priority:** `P1`
- **Traceability:** PO `F1.5` · Design `SCR-05` · Rules `BR-08`
- **Story:** Là một **Thực khách**, tôi muốn trò chuyện nhanh với Trợ lý AI ngay trên màn hình thực đơn `SCR-05` để xin gợi ý món theo khẩu vị, dị ứng hoặc ngân sách, để chọn món nhanh hơn.
- **Acceptance Criteria:**
  1. **Given** Trợ lý AI đang hoạt động và tôi còn lượt hỏi, **When** tôi gửi câu hỏi trên `SCR-05`, **Then** AI trả về gợi ý bám sát các món đang mở bán trong thực đơn hiện tại.
  2. **Given** tôi vượt giới hạn lượt hỏi hoặc dịch vụ AI tạm gián đoạn, **When** gửi câu hỏi, **Then** khung chat báo thông điệp thân thiện và toàn bộ việc chọn món, gửi đơn trên `SCR-05` vẫn hoạt động bình thường.

### `US-007`: Gửi đơn chống trùng lặp & gọi thêm nhiều đợt vào chung hóa đơn
- **Stage:** `STG-02` | **Persona:** `PER-01 (Customer)` | **Priority:** `P0`
- **Traceability:** PO `F1.4` · Design `SCR-05` · Rules `BR-05`, `BR-06`, `BR-07`
- **Story:** Là một **Thực khách**, tôi muốn xác nhận giỏ hàng để gửi món xuống bếp an toàn không bị đặt trùng khi mạng chậm, và có thể gọi thêm món vào chung một hóa đơn trong suốt bữa ăn.
- **Acceptance Criteria:**
  1. **Given** giỏ hàng trên `SCR-05` có ít nhất 1 món hợp lệ, **When** tôi bấm "Gửi đơn", **Then** nút chuyển sang trạng thái đang xử lý (chặn bấm đúp), đơn được tạo thành một đợt gọi mới, giỏ hàng được làm trống và hiển thị xác nhận.
  2. **Given** mạng chập chờn khiến yêu cầu gửi cùng một giỏ hàng bị gửi lặp 2 lần, **When** hệ thống tiếp nhận, **Then** chỉ tạo duy nhất 1 đợt gọi món (không nhân đôi đơn).
  3. **Given** bàn tôi đã gọi đợt 1, **When** tôi chọn thêm đồ uống/món phụ và bấm gửi lần 2, **Then** đợt gọi mới được cộng dồn vào chung hóa đơn hiện tại của phiên bàn.

### `US-008`: Theo dõi tiến độ chế biến từng món trực tiếp tại bàn
- **Stage:** `STG-02` | **Persona:** `PER-01 (Customer)` | **Priority:** `P1`
- **Traceability:** PO `F1.4` · Design `SCR-05` · Rules `BR-10`, `BR-24`
- **Story:** Là một **Thực khách**, tôi muốn nhìn thấy trạng thái từng món đã gọi (*Đang chờ / Đang nấu / Hoàn tất*) tự động cập nhật trên `SCR-05`, để an tâm chờ món mà không phải gọi hỏi nhân viên liên tục.
- **Acceptance Criteria:**
  1. **Given** tôi đang mở ngăn kéo đơn hiện tại trên `SCR-05`, **When** đầu bếp đổi trạng thái món trên `SCR-12`, **Then** nhãn trạng thái (`Status Pill`) tự cập nhật trong thời gian thực mà không cần tải lại trang.
  2. **Given** tôi lỡ tắt trình duyệt rồi quét lại mã QR của bàn, **When** `SCR-05` tải xong, **Then** danh sách các món đã gọi và trạng thái mới nhất của bàn vẫn hiển thị đầy đủ.

### `US-025`: Gọi nhân viên tại bàn theo lý do nhanh + ghi chú và chống bấm spam
- **Stage:** `STG-02` | **Persona:** `PER-01 (Customer)` | **Priority:** `P0`
- **Traceability:** PO `F1.6` · Design `SCR-05` · Rules `BR-01`, `BR-24`, `BR-25`, `BR-26`
- **Story:** Là một **Thực khách tại bàn**, tôi muốn chạm nút *Gọi nhân viên* trên `SCR-05`, chọn nhanh lý do kèm ghi chú ngắn và thấy trạng thái phản hồi trực tiếp, để được phục vụ kịp thời mà không phải vẫy tay chờ và không vô tình gửi spam lặp.
- **Acceptance Criteria:**
  1. **Given** tôi đang ngồi tại bàn hợp lệ (không *Tạm ngưng*) với phiên QR đang mở trên `SCR-05`, **When** tôi chạm nút *Gọi nhân viên*, chọn chip lý do nhanh *(ví dụ: "Thêm nước / Đá")*, nhập ghi chú ngắn (≤ 200 ký tự) và bấm gửi, **Then** hệ thống tạo yêu cầu hỗ trợ gắn đúng số bàn vật lý, hiển thị thẻ trạng thái *Đang chờ tiếp nhận* (màu `Warning`) kèm nút hủy khi còn chờ và kích hoạt đồng hồ đếm ngược 60 giây khóa bấm gửi lặp. *(Happy Path)*
  2. **Given** tôi chọn lý do *"Khác"* nhưng để trống ghi chú hoặc ghi chú dưới 2 ký tự, **When** tôi bấm gửi, **Then** hệ thống báo lỗi ngay dưới ô ghi chú mà không làm mất dữ liệu đang nhập; tương tự, hệ thống từ chối kèm thông báo rõ ràng nếu bàn đang còn thời gian chờ cooldown 60 giây, đã có yêu cầu đang mở trùng lý do đó, hoặc đã đạt tối đa 3 yêu cầu đang mở trong phiên bàn — đồng thời chống gửi trùng khi mạng chập chờn bấm gửi lặp. *(Validation & Anti-Spam Business Rules)*
  3. **Given** tôi vừa gửi yêu cầu gọi nhân viên và vẫn mở `SCR-05` (hoặc tải lại trang / quét lại QR bàn), **When** nhân viên nhận xử lý hoặc hoàn tất yêu cầu, **Then** thẻ theo dõi trên `SCR-05` tự cập nhật trực tiếp trạng thái *"Nhân viên [Tên] đang tới"* (`Info`) rồi *"Đã hoàn tất"* (`Success`) theo giao diện `VI`/`EN` và Light/Dark; nếu bàn chuyển *Tạm ngưng*, phiên hết hạn hoặc thiết bị mất kết nối, giao diện thông báo rõ ràng và giữ nguyên nội dung để thử lại an toàn. *(Live Sync, Error & Offline State)*

### `US-028`: Đặt món & combo trực tiếp từ thẻ gợi ý trong Trợ lý AI
- **Stage:** `STG-02` | **Persona:** `PER-01 (Customer)` | **Priority:** `P1`
- **Traceability:** PO `F1.5` · Design `SCR-05` · Rules `BR-01`, `BR-05`, `BR-08`, `BR-24`, `BR-29`, `BR-30`
- **Story:** Là một **Thực khách tại bàn**, tôi muốn khi hỏi gợi ý ăn uống trong Trợ lý AI trên `SCR-05`, AI trả về thẻ món/combo đang còn hàng kèm nút thêm nhanh vào giỏ, để tôi chốt đơn ngay mà không phải thoát khung chat đi tìm lại từng món.
- **Acceptance Criteria:**
  1. **Given** tôi mở khung Trợ lý AI trên `SCR-05` khi bàn hợp lệ, **When** tôi gửi yêu cầu gợi ý theo khẩu vị, số người hoặc ngân sách, **Then** AI trả về lời tư vấn kèm tối đa 5 thẻ món/combo **đang còn bán** với đầy đủ tên, đơn giá chuẩn, lý do gợi ý ngắn và nút `+ Thêm` *(hoặc `Chọn tùy chọn`)*; các món hết hàng hoặc không có trong thực đơn thực tế bị loại bỏ hoàn toàn. *(Happy Path & Catalog Validation)*
  2. **Given** trong danh sách thẻ gợi ý có món **không có tùy chọn** *(hoặc Combo)* và món **có tùy chọn kích cỡ/topping**, **When** tôi bấm `+ Thêm` cho món đơn / Combo, **Then** món được đưa thẳng vào giỏ hàng dùng chung và hiển thị ngay số lượng đang có trong giỏ *(kèm nút mở nhanh giỏ hàng hoặc thêm nhanh toàn bộ các món không tùy chọn nếu có ≥ 2 món)*; khi tôi bấm nút ở món có tùy chọn, hộp thoại tùy chọn mở lên *(tạm ẩn khung chat)* để tôi chọn kích cỡ/topping trước khi vào giỏ. *(Cart & Options Routing)*
  3. **Given** bàn đang bị khóa vì đang thanh toán hoặc quán đang tạm ngưng nhận đơn, hoặc phản hồi AI gặp lỗi cấu trúc/gián đoạn, **When** tôi nhận câu trả lời trong khung chat, **Then** nút đặt món từ thẻ gợi ý bị vô hiệu hóa kèm lý do rõ ràng *(nếu khóa đặt món)*, hoặc hệ thống tự động hạ cấp về câu trả lời văn bản thuần túy an toàn mà không làm treo khung chat hay hỏng trải nghiệm `Light`/`Dark`, `VI`/`EN`. *(Safety & Fallback)*

---

## Stage 3 (`STG-03`): Tiếp nhận Bếp (KDS) & Tự động Giữ/Trừ Tồn kho

### `US-009`: Nhận phiếu món tức thì trên KDS & cập nhật trạng thái Kanban
- **Stage:** `STG-03` | **Persona:** `PER-03 (Chef)` | **Priority:** `P0`
- **Traceability:** PO `F2.1`, `F2.4` · Design `SCR-12` · Rules `BR-09`, `BR-10`
- **Story:** Là một **Đầu bếp**, tôi muốn nhận đơn mới tức thì trên bảng Kanban `SCR-12` kèm âm thanh và thao tác 1 chạm để chuyển *Chờ chế biến → Đang nấu → Hoàn tất*, để giữ nhịp ra món ổn định.
- **Acceptance Criteria:**
  1. **Given** tôi đang mở `SCR-12`, **When** có đơn mới từ bàn hoặc quầy mang về, **Then** thẻ món xuất hiện ngay (< 1 giây) ở cột *Chờ chế biến* với đủ số bàn, số lượng, topping/ghi chú và phát âm thanh nhắc (nếu đang bật).
  2. **Given** món đang ở cột *Chờ chế biến*, **When** tôi bấm bắt đầu nấu rồi hoàn tất, **Then** món tiến tuần tự *Đang nấu → Hoàn tất*, lưu lịch sử thay đổi trạng thái và thông báo ra sàn phục vụ.
  3. **Given** nhân sự có vai trò `PER-02 (Staff)` hoặc `PER-04 (Cashier)` cố truy cập `SCR-12`, **When** mở màn hình, **Then** bị chặn quyền và chuyển về `SCR-06`.

### `US-010`: Chế độ Nấu gom theo mẻ, cảnh báo quá 20 phút & báo hết món
- **Stage:** `STG-03` | **Persona:** `PER-03 (Chef)` | **Priority:** `P1`
- **Traceability:** PO `F2.2`, `F2.3`, `F2.4`, `F2.5` · Design `SCR-12` · Rules `BR-10`, `BR-12`
- **Story:** Là một **Đầu bếp**, tôi muốn gom nhiều bàn đặt cùng món để nấu chung một mẻ, nhận cảnh báo nổi bật khi món chờ quá 20 phút, và bấm hủy món kèm lý do hết hàng khi cần, để không để khách chờ oan.
- **Acceptance Criteria:**
  1. **Given** nhiều bàn cùng gọi một món giống nhau, **When** tôi chuyển `SCR-12` sang góc nhìn *Nấu gom theo mẻ*, **Then** hệ thống hiển thị tổng số lượng cần nấu kèm chi tiết ghi chú riêng từng bàn.
  2. **Given** một món chờ vượt quá 20 phút chưa hoàn tất, **When** đồng hồ chạy qua ngưỡng, **Then** thẻ món đổi sang màu cảnh báo `Danger` nổi bật (đạt chuẩn Light/Dark) kèm âm thanh nhắc khẩn.
  3. **Given** món không thể chế biến do hết nguyên liệu, **When** tôi bấm hủy món và nhập lý do trên `SCR-12`, **Then** món chuyển trạng thái *Đã hủy* và thông báo ngay tới phục vụ và thực khách.

### `US-011`: Tự động giữ/trừ tồn kho theo công thức món (BOM) & cảnh báo kho thấp
- **Stage:** `STG-03` | **Persona:** `PER-03 (Chef)`, `PER-05 (Manager)` | **Priority:** `P0`
- **Traceability:** PO `F5.2`, `F5.3`, `F5.5` · Design `SCR-12`, `SCR-18` · Rules `BR-11`, `BR-12`
- **Story:** Là một **Quản lý / Đầu bếp**, tôi muốn hệ thống tự động giữ và trừ nguyên liệu theo công thức món khi có đơn/hoàn tất món và hoàn kho nếu hủy món, để kiểm soát chính xác lượng nguyên liệu tiêu hao.
- **Acceptance Criteria:**
  1. **Given** món đã được thiết lập công thức định lượng (BOM), **When** đơn được tiếp nhận và hoàn tất trên `SCR-12`, **Then** lượng nguyên liệu tương ứng được giữ/trừ chính xác và ghi lịch sử biến động trên `SCR-18`.
  2. **Given** một món bị hủy trước khi hoàn tất, **When** xác nhận hủy, **Then** lượng nguyên liệu đã giữ cho món đó được hoàn trả lại kho.
  3. **Given** tồn kho một nguyên liệu giảm xuống bằng hoặc thấp hơn ngưỡng tối thiểu, **When** biến động được ghi nhận, **Then** `SCR-18` lập tức hiển thị nhãn cảnh báo `Warning`/`Danger`.

### `US-012`: Bếp nhập kho & điều chỉnh tồn kho thực tế tại `SCR-18`
- **Stage:** `STG-03` | **Persona:** `PER-03 (Chef)` | **Priority:** `P1`
- **Traceability:** PO `F5.1`, `F5.4` · Design `SCR-18` · Rules `BR-11`, `BR-21`
- **Story:** Là một **Đầu bếp**, tôi muốn tạo phiếu nhập hàng đầu ca và điều chỉnh lượng hao hụt nguyên liệu ngay trên `SCR-18`, để số liệu tồn kho luôn khớp thực tế bếp.
- **Acceptance Criteria:**
  1. **Given** tôi đăng nhập bằng `PER-03 (Chef)` trên `SCR-18`, **When** tôi thực hiện nhập kho hoặc điều chỉnh tồn kèm lý do hợp lệ, **Then** số lượng tồn cập nhật tức thì và lưu đầy đủ dấu vết người thực hiện + thời gian.
  2. **Given** tôi là `PER-03 (Chef)`, **When** xem giao diện kho và thực đơn, **Then** tôi không có quyền xóa nguyên liệu (thuộc `PER-06`) và không được sửa công thức định lượng BOM (thuộc `PER-05`/`PER-06`).

---

## Stage 4 (`STG-04`): Phục vụ tại bàn, Gộp bàn & Đơn Mang về

### `US-013`: Theo dõi sơ đồ bàn 4 trạng thái & đặt món hộ khách
- **Stage:** `STG-04` | **Persona:** `PER-02 (Staff)` | **Priority:** `P0`
- **Traceability:** PO `F4.1`, `F4.2` · Design `SCR-11` · Rules `BR-13`, `BR-15`
- **Story:** Là một **Nhân viên phục vụ**, tôi muốn quan sát sơ đồ bàn 4 màu trạng thái cập nhật trực tiếp trên `SCR-11` và đặt món hộ cho khách không dùng điện thoại, để phục vụ kịp thời mọi đối tượng khách.
- **Acceptance Criteria:**
  1. **Given** tôi đang mở `SCR-11`, **When** bàn có khách đặt món hoặc vừa thanh toán xong, **Then** màu trạng thái bàn (*Trống / Đang có khách / Chờ thanh toán / Tạm ngưng*) tự cập nhật tức thì.
  2. **Given** bàn không ở trạng thái *Tạm ngưng*, **When** tôi mở hộp thoại đặt món hộ và gửi đơn, **Then** đơn đi thẳng xuống bếp `SCR-12` giống như đơn khách tự đặt.
  3. **Given** tôi đăng nhập bằng `PER-02 (Staff)`, **When** xem chi tiết bàn trên `SCR-11`, **Then** nút chốt thanh toán không hiển thị và tôi không thể thực hiện thu tiền.

### `US-014`: Gộp nhiều bàn cho nhóm đông & hủy gộp khi cần
- **Stage:** `STG-04` | **Persona:** `PER-02 (Staff)`, `PER-04 (Cashier)` | **Priority:** `P0`
- **Traceability:** PO `F4.3` · Design `SCR-11` · Rules `BR-13`, `BR-14`
- **Story:** Là một **Nhân viên phục vụ / Thu ngân**, tôi muốn gộp 2 hoặc nhiều bàn đang hoạt động vào chung một phiên hóa đơn cho nhóm khách đông và có thể hủy gộp nếu ghép nhầm, để tránh nhầm bill khi thanh toán.
- **Acceptance Criteria:**
  1. **Given** bàn T02 và T03 đều không ở trạng thái *Tạm ngưng*, **When** tôi chọn gộp hai bàn trên `SCR-11`, **Then** cả hai bàn dùng chung một phiên phục vụ, bảo toàn toàn bộ món đã gọi trước đó và hiển thị nhãn nhóm gộp rõ ràng trên sơ đồ.
  2. **Given** một bàn đang ở trạng thái *Tạm ngưng*, **When** tôi mở hộp thoại gộp bàn, **Then** bàn đó không cho phép chọn gộp.
  3. **Given** nhóm bàn đã gộp chưa thanh toán, **When** tôi thực hiện hủy gộp, **Then** các bàn tách lại trạng thái độc lập nhất quán.

### `US-015`: Tạo và theo dõi đơn mua mang về (Takeaway)
- **Stage:** `STG-04` | **Persona:** `PER-02 (Staff)`, `PER-04 (Cashier)` | **Priority:** `P1`
- **Traceability:** PO `F4.4` · Design `SCR-13`, `SCR-12` · Rules `BR-15`
- **Story:** Là một **Nhân viên phục vụ / Thu ngân**, tôi muốn tạo và theo dõi riêng đơn khách mua mang về trên `SCR-13`, để đẩy xuống bếp nhanh mà không chiếm dụng sơ đồ bàn ăn tại chỗ.
- **Acceptance Criteria:**
  1. **Given** tôi đang ở `SCR-13`, **When** tôi tạo đơn mang về với thông tin khách và danh sách món, **Then** đơn hiển thị trong hàng đợi mang về gắn nhãn `Info (Blue)`, đẩy sang `SCR-12` và không làm thay đổi trạng thái bất kỳ bàn ăn nào trên `SCR-11`.

### `US-026`: Theo dõi hàng đợi yêu cầu bàn realtime & nhận xử lý tránh trùng người
- **Stage:** `STG-04` | **Persona:** `PER-02 (Staff)`, `PER-04 (Cashier)`, `PER-05 (Manager)` | **Priority:** `P0`
- **Traceability:** PO `F4.5` · Design `SCR-27`, `SCR-11`, `SCR-06` · Rules `BR-04`, `BR-24`, `BR-27`, `BR-28`
- **Story:** Là một **Nhân viên phục vụ**, tôi muốn nhìn thấy mọi yêu cầu hỗ trợ từ bàn trên hàng đợi thời gian thực `SCR-27` và bấm *Nhận xử lý* với cơ chế khóa duy nhất, để ra hỗ trợ khách đúng lúc mà không bị 2 nhân viên cùng chạy ra 1 bàn.
- **Acceptance Criteria:**
  1. **Given** tôi đang mở `SCR-27` hoặc `SCR-11` bằng tài khoản `PER-02 (Staff)` *(hoặc `PER-04`/`PER-05`/`PER-06`)*, **When** thực khách tại bàn gửi yêu cầu hỗ trợ mới, **Then** trong vòng < 1 giây badge yêu cầu trên thẻ bàn `SCR-11` tăng lên, hàng đợi `SCR-27` phát âm thanh thông báo (nếu bật) và hiển thị thẻ yêu cầu sắp xếp bàn chờ lâu nhất lên đầu kèm đồng hồ đếm thời gian chờ (tự đổi sang màu `Danger` nếu chờ quá 5 phút); khi bấm **"Nhận xử lý"**, yêu cầu chuyển sang trạng thái *Đang xử lý* (`Info`) gắn tên của tôi. *(Happy Path & SLA Alert)*
  2. **Given** hai nhân viên phục vụ cùng nhìn thấy một yêu cầu đang *Chờ tiếp nhận* trên `SCR-27` và bấm **"Nhận xử lý"** gần như đồng thời, **When** hệ thống xử lý lượt bấm, **Then** chỉ đúng 1 nhân viên nhận xử lý thành công, người còn lại nhận thông báo xung đột nêu rõ tên đồng nghiệp đã nhận *(ví dụ: "Yêu cầu này đã được nhân viên [Tên] tiếp nhận")* và thẻ tự cập nhật sang trạng thái *Đang xử lý* không bị nhận đúp. *(Concurrency & Single-Claim Rule)*
  3. **Given** tôi đăng nhập bằng `PER-03 (Chef)` và cố truy cập `SCR-27`, **When** hệ thống kiểm tra phân quyền, **Then** chuyển hướng sang `SCR-06` (Không có quyền truy cập); đồng thời với vai trò hợp lệ, `SCR-27` hỗ trợ lọc theo trạng thái (*Đang mở / Chờ tiếp nhận / Đang xử lý / Đã hoàn tất*), công tắc *"Chỉ xem việc của tôi"*, hiển thị `Empty State` thân thiện khi chưa có yêu cầu, `Loading` skeleton khi tải, cảnh báo mất kết nối khi offline và hỗ trợ đầy đủ Light/Dark + `VI`/`EN`. *(RBAC & 5 UI States)*

### `US-027`: Xác nhận hoàn tất yêu cầu bàn & tự khép yêu cầu khi đóng phiên bàn
- **Stage:** `STG-04` | **Persona:** `PER-02 (Staff)`, `PER-05 (Manager)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F4.5` · Design `SCR-27`, `SCR-11`, `SCR-05` · Rules `BR-27`, `BR-28`
- **Story:** Là một **Nhân viên phục vụ / Quản lý**, tôi muốn xác nhận *Hoàn tất* yêu cầu sau khi hỗ trợ xong cho khách và đảm bảo các yêu cầu còn mở tự khép khi bàn đóng phiên, để hàng đợi luôn gọn sạch và phản ánh đúng tình trạng thực tế.
- **Acceptance Criteria:**
  1. **Given** tôi là nhân viên đang phụ trách một yêu cầu ở trạng thái *Đang xử lý* trên `SCR-27`, **When** tôi bấm **"Hoàn tất"**, **Then** yêu cầu chuyển sang *Đã hoàn tất* (`Success`), đồng bộ tức thì giảm số badge yêu cầu đang mở trên `SCR-11` và cập nhật trạng thái *"Đã hoàn tất"* trên màn hình khách `SCR-05`. *(Happy Path)*
  2. **Given** yêu cầu bàn đang thuộc quyền xử lý của nhân viên A, **When** nhân viên B (`PER-02` khác) cố bấm hoàn tất thay, **Then** hệ thống từ chối thao tác hoàn tất trên `SCR-27`; chỉ nhân viên A hoặc `PER-05 (Manager)` / `PER-06 (Admin)` mới được quyền hoàn tất hoặc hủy can thiệp để điều phối ca. *(Permission Rule)*
  3. **Given** một bàn vẫn còn yêu cầu hỗ trợ đang mở (*Chờ tiếp nhận* hoặc *Đang xử lý*) nhưng khách đã thanh toán xong và phiên bàn được đóng lại, **When** phiên phục vụ đóng, **Then** toàn bộ yêu cầu đang mở của phiên đó tự động chuyển sang trạng thái kết thúc và biến mất khỏi danh sách *Đang mở* trên `SCR-27` và badge `SCR-11`. *(Session Lifecycle Edge Case)*

---

## Stage 5 (`STG-05`): Thanh toán Tiền mặt / VietQR & Đóng phiên bàn

### `US-016`: Kiểm tra & áp dụng mã giảm giá (Voucher) và khuyến mãi tự động
- **Stage:** `STG-05` | **Persona:** `PER-04 (Cashier)`, `PER-01 (Customer)` | **Priority:** `P0`
- **Traceability:** PO `F3.4` · Design `SCR-11`, `SCR-13`, `SCR-05` · Rules `BR-16`, `BR-17`
- **Story:** Là một **Thu ngân / Thực khách**, tôi muốn áp dụng mã giảm giá hợp lệ cùng khuyến mãi tự động ngay trên màn hình thanh toán, để biết chính xác số tiền cần trả sau ưu đãi.
- **Acceptance Criteria:**
  1. **Given** đơn đủ điều kiện tối thiểu và mã voucher còn hạn, còn lượt dùng, **When** tôi nhập mã và áp dụng, **Then** hệ thống hiển thị rõ mức giảm giá và tổng phải thu (`≥ 0`) bằng chữ số thẳng hàng (`tabular/mono`).
  2. **Given** mã voucher sai, hết hạn, chưa đến thời gian áp dụng hoặc đã hết lượt dùng, **When** tôi bấm áp dụng, **Then** hệ thống từ chối kèm thông báo lý do cụ thể và giữ nguyên tổng tiền cũ.

### `US-017`: Thanh toán tiền mặt có kiểm tra tiền khách đưa & tính tiền thối
- **Stage:** `STG-05` | **Persona:** `PER-04 (Cashier)` | **Priority:** `P0`
- **Traceability:** PO `F3.1`, `F3.3`, `F3.5` · Design `SCR-11`, `SCR-13` · Rules `BR-16`, `BR-19`
- **Story:** Là một **Thu ngân**, tôi muốn nhập số tiền mặt khách đưa để hệ thống tự tính tiền thối, chốt hóa đơn, giải phóng bàn và in hóa đơn, để tránh nhầm lẫn tiền mặt giờ cao điểm.
- **Acceptance Criteria:**
  1. **Given** tổng hóa đơn là `250.000đ` và tôi nhập tiền khách đưa `300.000đ`, **When** xác nhận thanh toán, **Then** hệ thống hiển thị tiền thối `50.000đ`, chốt đơn *Đã thanh toán*, giải phóng toàn bộ bàn trong phiên (gồm bàn gộp) về *Trống* và mở tùy chọn in hóa đơn.
  2. **Given** tôi nhập số tiền khách đưa nhỏ hơn `250.000đ`, **When** bấm xác nhận, **Then** hệ thống chặn chốt đơn và báo lỗi ngay tại trường nhập.
  3. **Given** đơn vẫn còn món chưa hoàn tất tại bếp, **When** tôi mở thanh toán, **Then** hệ thống hiển thị cảnh báo rõ ràng theo chính sách chốt đơn của nhà hàng.

### `US-018`: Thanh toán chuyển khoản VietQR tự động đối soát (Quầy & Tự thanh toán tại bàn)
- **Stage:** `STG-05` | **Persona:** `PER-04 (Cashier)`, `PER-01 (Customer)` | **Priority:** `P0`
- **Traceability:** PO `F3.2`, `F3.3`, `F6.5` · Design `SCR-11`, `SCR-13`, `SCR-05` · Rules `BR-18`, `BR-19`
- **Story:** Là một **Thu ngân / Thực khách**, tôi muốn khởi tạo mã VietQR động đúng số tiền hóa đơn để khi chuyển khoản xong hệ thống tự chốt đơn và giải phóng bàn, không phải kiểm tra ảnh chụp màn hình.
- **Acceptance Criteria:**
  1. **Given** tôi chọn phương thức chuyển khoản VietQR tại quầy (hoặc khách chọn tự thanh toán trên `SCR-05` khi Chủ quán đã bật tính năng tại `SCR-26`), **When** khởi tạo giao dịch, **Then** màn hình hiển thị mã VietQR đúng số tiền cuối cùng kèm đếm ngược thời hạn.
  2. **Given** giao dịch chuyển khoản được cổng thanh toán xác nhận hợp lệ, **When** hệ thống tiếp nhận tín hiệu, **Then** đơn tự động chốt *Đã thanh toán*, giải phóng toàn bộ bàn về *Trống* và đóng hộp thoại QR tức thì.
  3. **Given** cổng thanh toán gửi trùng thông báo xác nhận cho cùng một giao dịch, **When** hệ thống nhận lần 2, **Then** bỏ qua an toàn không cộng trùng doanh thu; nếu khách đổi ý muốn trả tiền mặt thì cho phép hủy mã QR đang chờ trước khi thu tiền mặt.

### `US-019`: Tra cứu lịch sử hóa đơn & đối soát cuối ca
- **Stage:** `STG-05` | **Persona:** `PER-04 (Cashier)`, `PER-05 (Manager)` | **Priority:** `P1`
- **Traceability:** PO `F3.5`, `F6.3` · Design `SCR-14` · Rules `BR-19`
- **Story:** Là một **Thu ngân**, tôi muốn xem, lọc và in lại lịch sử hóa đơn theo ngày, trạng thái và phương thức thanh toán trên `SCR-14`, để đối soát tiền mặt và chuyển khoản cuối ca chính xác.
- **Acceptance Criteria:**
  1. **Given** tôi đăng nhập bằng `PER-04 (Cashier)`, **When** tôi mở `SCR-14`, **Then** hệ thống tải thành công danh sách hóa đơn phân trang kèm bộ lọc ngày, trạng thái và phương thức thanh toán.
  2. **Given** tôi chọn một hóa đơn bất kỳ trên `SCR-14`, **When** mở chi tiết, **Then** hiển thị đầy đủ danh sách món, ưu đãi/voucher, phương thức thanh toán, tiền khách đưa/tiền thối và nút in lại hóa đơn.

---

## Stage 6 (`STG-06`): Quản trị Thực đơn, Kho, Nhân sự, Thống kê & Dự báo AI

### `US-020`: Quản trị Thực đơn, Danh mục, Combo, Công thức định lượng (BOM) & Mô tả AI
- **Stage:** `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-06 (Admin)` | **Priority:** `P0`
- **Traceability:** PO `F1.2`, `F1.3`, `F5.2`, `F6.1` · Design `SCR-15`, `SCR-16`, `SCR-17` · Rules `BR-20`, `BR-21`, `BR-23`
- **Story:** Là một **Quản lý**, tôi muốn quản trị danh mục, món ăn, combo, nhóm tùy chọn/topping, công thức định lượng (BOM) và dùng AI soạn nhanh mô tả món trên `SCR-15..17`, để thực đơn luôn chính xác và thu hút.
- **Acceptance Criteria:**
  1. **Given** tôi thêm/sửa món hoặc tắt bán một món trên `SCR-15`, **When** lưu thành công, **Then** màn hình thực khách `SCR-05` tự động cập nhật ngay; nếu thay đổi giá món thì không làm đổi giá của các món đã đặt trước đó.
  2. **Given** tôi bấm nút "Gợi ý mô tả bằng AI" trên `SCR-15` hoặc `SCR-17`, **When** AI trả kết quả, **Then** nội dung điền vào ô mô tả để tôi chỉnh sửa trước khi lưu; nếu AI gián đoạn thì tôi vẫn tự nhập mô tả và lưu bình thường.

### `US-021`: Quản lý Voucher, Chương trình Khuyến mãi & Tài khoản Nhân sự
- **Stage:** `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F3.4`, `F6.4` · Design `SCR-19`, `SCR-20`, `SCR-21` · Rules `BR-04`, `BR-17`, `BR-20`
- **Story:** Là một **Quản lý**, tôi muốn tạo mã voucher, cấu hình khuyến mãi theo khung giờ/ngày và cấp tài khoản nhân sự đúng vai trò trên `SCR-19..21`, để vận hành chiến dịch bán hàng và phân quyền minh bạch.
- **Acceptance Criteria:**
  1. **Given** tôi tạo khuyến mãi theo khung giờ/ngày trong tuần trên `SCR-20` hoặc voucher trên `SCR-19`, **When** lưu thành công, **Then** ưu đãi tự đồng bộ sang màn hình đặt món & thanh toán theo đúng điều kiện cấu hình.
  2. **Given** tôi tạo tài khoản nhân sự mới trên `SCR-21`, **When** gán 1 trong 5 vai trò nội bộ và lưu, **Then** tài khoản có đúng quyền của vai trò đó và áp dụng chính sách đổi mật khẩu lần đầu khi cần.

### `US-022`: Xem Dashboard tổng quan, Top món bán chạy & Hiệu suất nhân sự
- **Stage:** `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F6.3` · Design `SCR-10`, `SCR-23`, `SCR-24` · Rules `BR-23`, `BR-24`
- **Story:** Là một **Quản lý**, tôi muốn xem nhanh các chỉ số KPI tổng quan, bảng xếp hạng món bán chạy và hiệu suất xử lý đơn của nhân sự trên `SCR-10`, `SCR-23`, `SCR-24`, để điều chỉnh thực đơn và lịch ca kịp thời.
- **Acceptance Criteria:**
  1. **Given** tôi chọn khoảng thời gian trên `SCR-10`, `SCR-23` hoặc `SCR-24`, **When** dữ liệu tải xong, **Then** hiển thị chính xác thẻ KPI (`tabular/mono`), biểu đồ trực quan và bảng xếp hạng Top 3 có phân trang.
  2. **Given** khoảng ngày được lọc không có giao dịch nào, **When** tải dữ liệu, **Then** hiển thị `Empty State` rõ ràng, không phát sinh lỗi giao diện.

### `US-023`: Xem Báo cáo Doanh thu chuyên sâu & Dự báo AI (Độc quyền Chủ quán)
- **Stage:** `STG-06` | **Persona:** `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F6.2`, `F6.3` · Design `SCR-22`, `SCR-25` · Rules `BR-22`, `BR-23`
- **Story:** Là một **Chủ nhà hàng**, tôi muốn xem báo cáo doanh thu chi tiết theo kỳ và biểu đồ dự báo nhu cầu món/nguyên liệu bằng AI trên `SCR-22` và `SCR-25`, để hoạch định tài chính và nhập hàng chủ động.
- **Acceptance Criteria:**
  1. **Given** tôi đăng nhập bằng `PER-06 (Admin)`, **When** mở `SCR-22` và `SCR-25`, **Then** hiển thị đầy đủ dữ liệu doanh thu, so sánh kỳ trước và khuyến nghị dự báo nhu cầu từ AI.
  2. **Given** bất kỳ vai trò nào thấp hơn (`PER-05 Manager` trở xuống) cố mở `SCR-22` hoặc `SCR-25`, **When** hệ thống kiểm tra quyền, **Then** bị từ chối truy cập và chuyển sang `SCR-06`.

### `US-024`: Tùy chọn cá nhân (VI/EN, Sáng/Tối) & Cấu hình Hệ thống 8 tab
- **Stage:** `STG-06` | **Persona:** `PER-02..06` (Cá nhân) · `PER-06 (Admin)` (Hệ thống) | **Priority:** `P1`
- **Traceability:** PO `F6.4`, `F6.5` · Design `SCR-26` · Rules `BR-19`, `BR-20`, `BR-22`, `BR-24`
- **Story:** Là một **Nhân sự / Chủ nhà hàng**, tôi muốn tự tùy chỉnh ngôn ngữ `VI/EN` và chế độ Sáng/Tối theo sở thích cá nhân, đồng thời Chủ quán quản trị trọn vẹn 7 nhóm cấu hình nhà hàng (bao gồm bật/tắt tự thanh toán tại bàn) tại `SCR-26`.
- **Acceptance Criteria:**
  1. **Given** bất kỳ nhân sự nào (`PER-02..06`) mở tab Tùy chọn cá nhân trên `SCR-26`, **When** chuyển đổi `Tiếng Việt ↔ English` hoặc `Light ↔ Dark Mode`, **Then** toàn bộ giao diện cập nhật tức thì đạt chuẩn tương phản `DESIGN_SYSTEM.md` và ghi nhớ cho lần truy cập sau.
  2. **Given** tôi là `PER-06 (Admin)`, **When** tôi bật/tắt tùy chọn *Cho phép khách tự thanh toán tại bàn* hoặc cập nhật thông tin quán/logo/hóa đơn ở 7 tab hệ thống trên `SCR-26`, **Then** cấu hình được lưu và đồng bộ tức thời sang màn hình khách & nhân sự; nếu vai trò khác cố sửa cấu hình hệ thống sẽ bị chặn quyền.

### `US-029`: Xem AI Tóm tắt ca & Phân tích điều hành thông minh trên Dashboard
- **Stage:** `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F6.6` · Design `SCR-10` · Rules `BR-04`, `BR-24`, `BR-31`
- **Story:** Là một **Quản lý hoặc Chủ nhà hàng**, tôi muốn bấm *"Phân tích nhanh bằng AI"* ngay trên Dashboard `SCR-10` cho khung thời gian đã chọn (*Hôm nay / 7 ngày / 30 ngày*), để nắm ngay tình hình doanh thu so với kỳ trước, điểm nghẽn bếp/phục vụ/kho và 2–3 việc cần xử lý ngay trong 10 giây.
- **Acceptance Criteria:**
  1. **Given** tôi đăng nhập bằng `PER-05 (Manager)` hoặc `PER-06 (Admin)` và mở `SCR-10`, **When** tôi chọn khung thời gian hợp lệ (≤ 90 ngày) và bấm *"Phân tích nhanh bằng AI"*, **Then** hệ thống hiển thị trạng thái đang tải (skeleton) rồi trả về dải chỉ số thực tế *(doanh thu + % so kỳ trước, đơn hoàn tất/hủy, món bếp quá hạn > 20p, yêu cầu bàn quá SLA > 5p, nguyên liệu chạm ngưỡng thấp)*, tiêu đề tóm tắt, danh sách điểm nhấn, cảnh báo bất thường theo mức độ (`Danger`/`Warning`/`Info`) và 2–3 khuyến nghị cụ thể bằng đúng ngôn ngữ `VI`/`EN` đang chọn. *(Happy Path & Real Grounding)*
  2. **Given** dịch vụ AI đang tắt, quá tải hoặc trả về dữ liệu sai định dạng, **When** tôi bấm phân tích trên `SCR-10`, **Then** hệ thống vẫn trả về bản phân tích điều hành tổng hợp tự động từ số liệu thực tế kèm nhãn nhận diện rõ nguồn dự phòng, không hiển thị lỗi trống; nếu khoảng ngày không hợp lệ *(ngày bắt đầu > ngày kết thúc hoặc > 90 ngày)* hoặc gặp lỗi kết nối/hết hạn mức lượt gọi, giao diện hiển thị thông báo lỗi rõ ràng kèm nút thử lại. *(Fallback & Error/Validation States)*
  3. **Given** nhân sự có vai trò `PER-02 (Staff)`, `PER-03 (Chef)` hoặc `PER-04 (Cashier)` cố gửi yêu cầu phân tích điều hành AI, **When** hệ thống kiểm tra quyền, **Then** bị từ chối truy cập theo phân cấp quyền (`BR-04`, `BR-31`). *(RBAC)*

### `US-030`: AI Gợi ý nhập kho thông minh từ BOM + Dự báo bán trên `SCR-18`
- **Stage:** `STG-03`, `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-03 (Chef)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F5.6` · Design `SCR-18` · Rules `BR-04`, `BR-11`, `BR-24`, `BR-32`
- **Story:** Là một **Quản lý hoặc Đầu bếp**, tôi muốn xem danh sách đề xuất nhập bù nguyên liệu từ định mức BOM nhân với tốc độ bán dự kiến trong 3/7/14 ngày tới và bấm *"Nhập nhanh"* 1 chạm, để nhập hàng kịp trước khi hết món bán.
- **Acceptance Criteria:**
  1. **Given** tôi đăng nhập bằng `PER-03`, `PER-05` hoặc `PER-06` và mở `SCR-18`, **When** tôi chọn khung dự báo `3 / 7 / 14 ngày` và bấm *"Phân tích nhập kho AI"*, **Then** hệ thống hiển thị danh sách nguyên liệu kèm tồn khả dụng, nhu cầu dự kiến, số ngày còn đủ dùng, badge mức khẩn cấp (`CRITICAL`/`HIGH`/`MEDIUM`), chi phí dự kiến và lý do món tiêu thụ chính.
  2. **Given** một nguyên liệu đang thiếu hoặc sắp hết trên thẻ đề xuất, **When** tôi bấm **"Nhập nhanh"**, **Then** hộp thoại nhập kho mở lên với số lượng gợi ý và ghi chú điền sẵn để tôi kiểm tra và xác nhận nhập kho.
  3. **Given** dịch vụ AI đang tắt/lỗi hoặc tôi là `PER-02`/`PER-04` cố gọi tính năng này, **Then** vai trò hợp lệ vẫn nhận được tính toán dự phòng xác định đầy đủ từ BOM + sức bán (`aiGenerated=false`); vai trò `PER-02`/`PER-04` bị chặn quyền (`BR-32`).

### `US-031`: AI Đề xuất Combo bán kèm & Khung giờ Khuyến mãi trên `SCR-17` / `SCR-20`
- **Stage:** `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F6.7` · Design `SCR-17`, `SCR-20` · Rules `BR-04`, `BR-17`, `BR-20`, `BR-24`, `BR-33`
- **Story:** Là một **Quản lý**, tôi muốn nhận gợi ý tạo Combo từ các cặp món khách hay gọi chung trên `SCR-17` và gợi ý khung giờ Khuyến mãi cho giờ vắng khách trên `SCR-20`, kèm nút tạo bản nháp 1 chạm trước khi lưu.
- **Acceptance Criteria:**
  1. **Given** tôi là `PER-05` hoặc `PER-06`, **When** tôi bấm *"AI Đề xuất Combo"* trên `SCR-17` (hoặc *"AI Đề xuất Khung giờ"* trên `SCR-20`), **Then** hệ thống trả về các gói combo ≥ 2 món còn bán có giá ưu đãi hợp lý (hoặc khung giờ thấp điểm giảm 5%–40% kèm ngày áp dụng).
  2. **Given** tôi chọn *"Tạo Combo từ gợi ý"* trên `SCR-17` hoặc *"Tạo Khuyến mãi từ gợi ý"* trên `SCR-20`, **When** form tạo mới mở ra, **Then** tên, thành phần món/giá ưu đãi hoặc khung giờ/ngày/tỷ lệ giảm đã được điền sẵn dưới dạng bản nháp để tôi chỉnh sửa và bấm Lưu tạo mới (`BR-33`).
  3. **Given** nhân sự có vai trò `PER-02`, `PER-03` hoặc `PER-04` cố gọi chức năng đề xuất này, **Then** bị từ chối quyền truy cập (`BR-04`, `BR-33`).

### `US-032`: AI Cảnh báo dị ứng & Ưu tiên ghi chú tự do cho Bếp & Phục vụ
- **Stage:** `STG-03`, `STG-04` | **Persona:** `PER-03 (Chef)`, `PER-02 (Staff)`, `PER-05 (Manager)` | **Priority:** `P0`
- **Traceability:** PO `F2.6` · Design `SCR-12`, `SCR-27`, `SCR-11` · Rules `BR-04`, `BR-12`, `BR-24`, `BR-34`
- **Story:** Là một **Đầu bếp hoặc Nhân viên phục vụ**, tôi muốn ghi chú có từ khóa dị ứng hoặc yêu cầu khẩn từ khách phải bật cảnh báo đỏ tức thì trên thẻ món KDS `SCR-12`, chi tiết đơn `SCR-11` và hàng đợi `SCR-27`, đồng thời dùng AI phân tích toàn bộ ghi chú đang mở để biết chính xác việc cần làm.
- **Acceptance Criteria:**
  1. **Given** có món đang chờ/nấu hoặc yêu cầu bàn mang ghi chú *"dị ứng đậu phộng, không hải sản"* hoặc *"bé nhỏ làm không cay gấp"*, **When** thẻ hiển thị trên `SCR-12`, `SCR-27` hoặc `SCR-11`, **Then** badge cảnh báo dị ứng (`Danger`) / khẩn (`Warning`) hiển thị ngay lập tức và `SCR-12` hỗ trợ nút lọc *"Dị ứng / Ưu tiên"*.
  2. **Given** tôi là `PER-02`, `PER-03`, `PER-04`, `PER-05` hoặc `PER-06`, **When** tôi bấm *"Phân tích ghi chú bằng AI"* trên `SCR-12` hoặc `SCR-27`, **Then** danh sách cảnh báo được xếp theo thứ tự `CRITICAL → HIGH → NORMAL` kèm hướng dẫn xử lý ngắn gọn, và không bao giờ hạ mức cảnh báo `CRITICAL` đã phát hiện xác định (`BR-34`).
  3. **Given** không có ghi chú nào đang mở hoặc người dùng là `PER-01` (Khách) gọi endpoint phân tích nội bộ, **Then** UI hiển thị `Empty State` an toàn còn khách bị chặn quyền `401`/`403`.

### `US-033`: AI Gợi ý công thức định lượng (BOM) & Kiểm tra giá vốn món (`SCR-18`)
- **Stage:** `STG-03`, `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-03 (Chef)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F5.7` · Design `SCR-18` · Rules `BR-21`, `BR-24`, `BR-35`
- **Story:** Là một **Quản lý hoặc Đầu bếp**, tôi muốn thiết lập đơn giá vốn nguyên liệu, nhìn thấy ngay tỷ lệ Giá vốn / Giá bán (Food Cost %) khi soạn định mức BOM và nhờ AI gợi ý định mức kèm cảnh báo biên lợi nhuận.
- **Acceptance Criteria:**
  1. **Given** tôi mở hộp thoại Thêm/Sửa nguyên liệu trên `SCR-18`, **When** tôi nhập `Giá vốn / đơn vị ≥ 0` và lưu, **Then** giá vốn được lưu chính xác; khi mở hộp thoại Định mức món, thanh **Kiểm tra giá vốn** cập nhật trực tiếp tổng giá vốn, tỷ lệ Food Cost % và mức cảnh báo (`HEALTHY` / `WARNING` / `CRITICAL` / `MISSING_COST`).
  2. **Given** tôi bấm *"AI Gợi ý định mức & Kiểm tra giá vốn"* trong hộp thoại Định mức, **When** AI trả về kết quả, **Then** danh sách nguyên liệu gợi ý chỉ chứa nguyên liệu hợp lệ từ kho, đi kèm giá vốn dự kiến và khuyến nghị giá bán; riêng `PER-05`/`PER-06` thấy nút **"Áp dụng công thức AI"** điền nhanh vào form và bấm Lưu, còn `PER-03` chỉ xem đề xuất và không được lưu BOM (`BR-21`, `BR-35`).
  3. **Given** một nguyên liệu đang nhập giá vốn âm `< 0` hoặc khoảng AI bị gián đoạn, **Then** hệ thống báo lỗi validation tại trường nhập (nếu âm) hoặc dùng phân tích giá vốn xác định theo quy tắc (`aiGenerated=false`).

### `US-034`: Nhập kho hàng loạt bằng chụp ảnh hóa đơn (Gemini Vision) trên `SCR-18`
- **Stage:** `STG-03`, `STG-06` | **Persona:** `PER-05 (Manager)`, `PER-03 (Chef)`, `PER-06 (Admin)` | **Priority:** `P1`
- **Traceability:** PO `F5.8` · Design `SCR-18` · Rules `BR-04`, `BR-11`, `BR-24`, `BR-36`
- **Story:** Là một **Quản lý hoặc Đầu bếp**, tôi muốn chụp ảnh hoặc tải lên ảnh hóa đơn nhập hàng để hệ thống tự động đọc danh sách nguyên liệu, quy đổi đơn vị phù hợp và cho tôi rà soát trước khi xác nhận nhập kho hàng loạt, để tiết kiệm thời gian nhập liệu thủ công mà vẫn chính xác tồn kho.
- **Acceptance Criteria:**
  1. **Given** tôi đăng nhập bằng `PER-03`, `PER-05` hoặc `PER-06` tại `SCR-18` và mở hộp thoại *"Nhập từ hóa đơn AI"*, **When** tôi chụp hoặc chọn ảnh hóa đơn hợp lệ (`JPEG`/`PNG`/`WebP` ≤ 5MB) và bấm quét, **Then** hệ thống hiển thị thông tin nhà cung cấp, bảng các dòng hàng trích xuất đã tự khớp nguyên liệu kho kèm quy đổi đơn vị tự động *(ví dụ: `2 kg` → `2000 g`)* và cảnh báo các dòng chưa khớp hoặc lệch đơn vị để tôi chỉnh sửa trước khi xác nhận. *(Happy Path & Unit Conversion)*
  2. **Given** kết quả trích xuất đã hiển thị trên bảng rà soát, **When** tôi là `PER-05`/`PER-06` chỉnh lại nguyên liệu/số lượng, tích chọn cập nhật đơn giá vốn và bấm *"Xác nhận nhập kho"*, **Then** toàn bộ các dòng được chọn được cộng tồn kho nguyên tử trong cùng một giao dịch, ghi đầy đủ lịch sử biến động kèm mã hóa đơn và cập nhật giá vốn mới; riêng với `PER-03 (Chef)`, hệ thống chỉ cho phép nhập số lượng tồn kho và chặn cập nhật giá vốn (`BR-36`). *(Review, Atomic Batch & RBAC)*
  3. **Given** tệp tải lên sai định dạng/vượt quá 5MB, dịch vụ AI gián đoạn không đọc được hóa đơn, hoặc nhân sự `PER-02`/`PER-04` cố gọi chức năng quét/nhập kho hàng loạt, **Then** giao diện báo lỗi thân thiện không làm mất dữ liệu (cho phép thêm dòng nhập tay nếu AI gián đoạn), còn `PER-02`/`PER-04` bị chặn quyền truy cập (`BR-04`, `BR-36`). *(Validation, Fallback & Security)*
