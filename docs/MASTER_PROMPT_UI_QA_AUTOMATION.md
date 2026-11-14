# MASTER PROMPT: AUTOMATED UI/UX DISCOVERY, PLAYWRIGHT QA AUDIT & RESPONSIVE SCREENSHOT COLLECTION
**Hệ thống: QROS — Nền tảng Đặt món qua mã QR & Quản lý Nhà hàng Đa vai trò**

---

## 1. MỤC TIÊU VÀ TƯ CÁCH THỰC THI (MISSION & IDENTITY)

Bạn đang hành động với tư cách là **Senior QA Automation Lead, Frontend Architect, UI/UX Auditor và Full-Stack Engineer**.

Nhiệm vụ của bạn là **khảo sát, vận hành, kiểm thử tự động toàn diện và thu thập thư viện ảnh chụp màn hình (screenshot library) của toàn bộ ứng dụng QROS** trên môi trường thực tế, không dùng dữ liệu giả lập tĩnh hay trang mock.

### Nguyên tắc thực thi cốt lõi (Core Principles)
1. **Dựa trên mã nguồn thực (Source Code Grounded):** Mọi route, component, modal, form và quyền hạn phải được bóc tách trực tiếp từ `frontend/src/app/router.jsx`, `backend/.../UserRole.java`, và các component thực tế.
2. **Thực thi phân pha có kiểm soát (Phased Execution):** Không chạy dồn tất cả các bước vào một phiên đơn lẻ để tránh tràn context window hay timeout trình duyệt. Tiến hành tuần tự qua 5 Pha (Phase 1 → Phase 5).
3. **Chiến lược Viewport thông minh (Smart Viewport Strategy):** Thu thập toàn diện trên 3 Viewport chuẩn đại diện (Desktop, Tablet, Mobile) và mở rộng kích thước biên khi phát hiện lỗi responsive.
4. **Bảo tồn dữ liệu & Kiểm thử luồng động (Stateful E2E Flow):** Các màn hình đặc thù (Bếp KDS, Thu ngân, Khách đặt món) phải có dữ liệu active thực tế (Order/Session bàn) thay vì chỉ chụp màn hình rỗng (`EmptyState`).

---

## 2. MA TRẬN 6 VAI TRÒ NGƯỜI DÙNG (6 USER ROLES & PERMISSIONS)

Khảo sát và kiểm thử độc lập cho toàn bộ **6 vai trò** theo đúng kiến trúc phân quyền RBAC của dự án:

| Role | Định danh hệ thống | Cơ chế xác thực | Phạm vi truy cập & Tính năng chính |
|---|---|---|---|
| **1. Customer** | `CUSTOMER` (Guest) | Quét QR bàn (`/:tableCode/menu`), Public Menu (`/menu`), Session Storage | Xem thực đơn, lọc danh mục, chọn món & tùy chọn topping/ghi chú, giỏ hàng, đặt món, theo dõi trạng thái đơn hàng. |
| **2. Staff** | `STAFF` | JWT Form Login (`/login`) | Quản lý sơ đồ bàn (`/admin/tables`), gọi món tại bàn, gộp/tách bàn, đơn mang về (`/admin/takeaway`), hồ sơ cá nhân (`/admin/profile`). |
| **3. Chef** | `CHEF` | JWT Form Login (`/login`) | Bếp / KDS (`/admin/kitchen`) realtime WebSocket/STOMP, cập nhật trạng thái món (Chờ nấu → Đang nấu → Hoàn thành), kiểm tra kho nguyên liệu (`/admin/inventory`). |
| **4. Cashier** | `CASHIER` | JWT Form Login (`/login`) | Sơ đồ bàn (`/admin/tables`), thanh toán hóa đơn (Tiền mặt / VietQR PayOS), in hóa đơn, áp dụng voucher, lịch sử hóa đơn (`/admin/history`), đơn mang về (`/admin/takeaway`). |
| **5. Manager** | `MANAGER` | JWT Form Login (`/login`) | Dashboard tổng quan (`/admin/dashboard`), quản lý Thực đơn / Món ăn (`/admin/menu`), Danh mục (`/admin/categories`), Combo (`/admin/combo`), Khuyến mãi/Voucher (`/admin/voucher`), Nhân sự (`/admin/staffs`), Báo cáo món bán chạy (`/admin/statistics/top-dishes`), Báo cáo hiệu suất nhân viên (`/admin/statistics/staff`), Toàn quyền Bàn & Bếp. |
| **6. Admin** | `ADMIN` | JWT Form Login (`/login`) | Toàn bộ quyền của Manager + Báo cáo Doanh thu chi tiết (`/admin/statistics/revenue`), Quản trị người dùng & phân quyền hệ thống cấp cao nhất, Cấu hình hệ thống (`/admin/settings`). |

> **Lưu ý nghiệp vụ:**
> - Tài khoản nhân sự mẫu lấy từ cơ sở dữ liệu (`sample_data_...sql`):
>   - Admin: `admin@gmail.com`
>   - Manager: `manager@gmail.com`
>   - Staff: `staff@gmail.com`
>   - Cashier: `cashier@gmail.com`
>   - Chef: `chef@gmail.com`
> - Khách hàng (Customer) kiểm thử bằng bàn mẫu `T01` tại URL: `http://localhost:5173/T01/menu`.

---

## 3. CHIẾN LƯỢC VIEWPORT & THIẾT BỊ (RESPONSIVE VIEWPORT STRATEGY)

Để đảm bảo hiệu năng, chống quá tải bộ nhớ và vẫn bao phủ 100% các điểm ngắt giao diện (Breakpoints), chia làm 2 cấp độ:

### 3.1 Cấp độ 1: 3 Viewports chuẩn đại diện (Bắt buộc chụp 100% màn hình & modal)
- **Desktop (Tiêu chuẩn):** `1440 × 900` px (Tỷ lệ 16:10, chuẩn laptop phổ biến)
- **Tablet (Máy tính bảng):** `768 × 1024` px (iPad Mini / iPad Air dọc, cảm ứng)
- **Mobile (Điện thoại):** `390 × 844` px (iPhone 13/14/15 chuẩn, viewport di động phổ biến)

### 3.2 Cấp độ 2: Viewports biên kiểm toán lỗi (Boundary Audit Viewports)
Chỉ kích hoạt chụp khi kiểm tra Navigation Drawer, Table ngang, Modal tràn màn hình hoặc khi phát hiện lỗi vỡ layout:
- **Mobile nhỏ (Màn hình hẹp):** `360 × 800` px (Kiểm tra tràn ngang văn bản, nút bấm chồng lấn)
- **Desktop lớn (Màn hình siêu rộng):** `1920 × 1080` px (Kiểm tra căn giữa layout, co giãn biểu đồ thống kê)

---

## 4. QUY TRÌNH THỰC THI 5 PHA (PHASED EXECUTION WORKFLOW)

Thực thi tuần tự 5 pha, hoàn thành mục tiêu và nghiệm thu từng pha trước khi chuyển tiếp:

```
[Phase 1: Environment & Pre-flight] ──► [Phase 2: Discovery & Matrix]
                                                  │
                                                  ▼
[Phase 4: Role Exploration & Shots] ◄── [Phase 3: Automation Suite]
                │
                ▼
[Phase 5: UI/UX Audit & Final Reports]
```

---

### GIAI ĐOẠN 1: THIẾT LẬP MÔI TRƯỜNG & KHỞI ĐỘNG HẠ TẦNG (PRE-FLIGHT)

1. **Khởi động dịch vụ phụ trợ:**
   - Kiểm tra và kích hoạt container Redis: `docker start qros-redis` (port `6379`).
   - Kiểm tra kết nối cơ sở dữ liệu PostgreSQL (Neon Cloud / Local DB) qua cấu hình `.env`.
2. **Khởi động Backend (Spring Boot 3.5.x - Java 21):**
   - Chạy nền profile `dev`: `mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev`
   - Chờ kiểm tra sức khỏe tại: `http://localhost:8080/actuator/health` (HTTP 200 OK).
3. **Khởi động Frontend (React 19 - Vite 7):**
   - Chạy nền: `npm --prefix frontend run dev -- --host 0.0.0.0 --port 5173`
   - Chờ kiểm tra phản hồi tại: `http://localhost:5173/` (HTTP 200 OK).
4. **Kiểm tra công cụ Automation:**
   - Xác nhận thư viện `playwright` (`1.65.0`) sẵn sàng trong `node_modules`.

---

### GIAI ĐOẠN 2: KHẢO SÁT MÃ NGUỒN & XÂY DỰNG MA TRẬN PHÂN QUYỀN (DISCOVERY)

1. Quét toàn bộ cây thư mục `frontend/src/` và `backend/src/`.
2. Lập danh mục toàn bộ các route công khai và bảo vệ từ `frontend/src/app/router.jsx`.
3. Bóc tách danh sách toàn bộ Modals, Slide-overs, Dropdowns, Cards, Tables, Forms.
4. **Xuất báo cáo 1:** `reports/PROJECT_DISCOVERY.md`
   - Tổng quan kiến trúc frontend, backend, state management (Zustand/React Query), STOMP realtime.
   - Danh sách toàn bộ route và linh kiện UI tương ứng.
5. **Xuất báo cáo 2:** `reports/ROLE_FEATURE_MATRIX.md`
   - Bảng phân quyền chi tiết cho 6 vai trò: `Customer`, `Staff`, `Chef`, `Cashier`, `Manager`, `Admin`.

---

### GIAI ĐOẠN 3: XÂY DỰNG BỘ CÔNG CỤ AUTOMATION PLAYWRIGHT

Xây dựng/nâng cấp bộ kịch bản tự động hóa (`scripts/audit_playwright.js` hoặc runner chuyên dụng):
1. **Helper modules:**
   - `loginAs(role)`: Xử lý đăng nhập tự động theo từng vai trò, quản lý token/cookie.
   - `setupCustomerSession(tableCode)`: Thiết lập session bàn cho khách hàng.
   - `captureShot(page, role, device, category, name, options)`: Tự động chụp và lưu theo đúng cây thư mục chuẩn.
   - `attachLoggers(page)`: Lắng nghe Console errors, Page crashes, Network HTTP 4xx/5xx failures.
2. **Stateful Data Seed Helper:**
   - Tạo sẵn 1 đơn hàng mẫu đang hoạt động (Active Order) để các màn hình Bếp (`/admin/kitchen`) và Thu ngân (`/admin/tables`) hiển thị đầy đủ dữ liệu món ăn, nút đổi trạng thái và nút thanh toán thay vì rơi vào Empty State.

---

### GIAI ĐOẠN 4: THỰC THI KIỂM THỬ TỪNG VAI TRÒ & THU THẬP SCREENSHOTS

Thực hiện độc lập theo từng vai trò trên 3 viewports (`desktop`, `tablet`, `mobile`):

#### 4.1 Khách hàng (Customer Role)
- **Trang & Luồng chính:**
  - Landing Page (`/`), Trang lỗi (`/unauthorized`, `/not-found`, `/offline`, `/error/500`).
  - Thực đơn công khai (`/menu`) và Thực đơn theo bàn (`/T01/menu`).
- **Tương tác & Modals:**
  - Click món ăn → Mở modal tùy chọn món (Options/Topping/Ghi chú).
  - Tăng giảm số lượng, thêm món vào giỏ hàng.
  - Mở giỏ hàng (Cart drawer/modal) → Chụp trạng thái giỏ có món.
  - Form xác nhận đặt món & Màn hình theo dõi trạng thái đơn hàng (Order Tracking).

#### 4.2 Nhân viên phục vụ (Staff Role)
- **Đăng nhập:** Form login, kiểm tra validation rỗng, mật khẩu sai, đăng nhập thành công.
- **Sơ đồ bàn (`/admin/tables`):**
  - Trạng thái các bàn (Trống, Đang có khách, Chờ thanh toán).
  - Click mở Modal chi tiết bàn, Modal chuyển bàn / gộp bàn.
- **Đơn mang về (`/admin/takeaway`):** Danh sách đơn mang về, form tạo đơn mới.
- **Trang cá nhân (`/admin/profile`):** Thông tin nhân viên, form đổi mật khẩu.

#### 4.3 Đầu bếp (Chef Role)
- **KDS Bếp (`/admin/kitchen`):**
  - Màn hình nhận đơn realtime.
  - Thẻ món ăn (Order ticket), trạng thái: Đang chờ, Đang chế biến, Đã nấu xong.
  - Nút chuyển trạng thái món ăn & xác nhận hoàn tất.
- **Kho nguyên liệu (`/admin/inventory`):** Bảng danh sách nguyên liệu, chỉ số cảnh báo sắp hết hàng.

#### 4.4 Thu ngân (Cashier Role)
- **Thanh toán bàn (`/admin/tables`):**
  - Mở Modal thanh toán hóa đơn (Bill / Payment Modal).
  - Phương thức thanh toán: Tiền mặt, Chuyển khoản QR (VietQR PayOS).
  - Form áp dụng Voucher / Mã giảm giá.
  - Modal xem trước hóa đơn và in hóa đơn (Invoice Preview).
- **Lịch sử đơn hàng (`/admin/history`):** Bộ lọc ngày, trạng thái thanh toán, phân trang, chi tiết đơn cũ.

#### 4.5 Quản lý (Manager Role)
- **Dashboard (`/admin/dashboard`):** Thẻ KPI doanh thu, biểu đồ doanh số, đơn hàng gần đây.
- **Quản lý Thực đơn & Danh mục:**
  - Món ăn (`/admin/menu`): Bảng danh sách, form tạo món mới, modal sửa món, modal xóa.
  - Danh mục (`/admin/categories`): Danh sách danh mục, modal thêm/sửa danh mục.
  - Combo món (`/admin/combo`): Quản lý gói combo món ăn.
  - Voucher (`/admin/voucher`): Bảng voucher, form tạo mã giảm giá mới.
- **Quản lý Nhân sự (`/admin/staffs`):** Bảng nhân viên, modal thêm nhân viên, đổi trạng thái/phân quyền.
- **Báo cáo thống kê:** Món bán chạy (`/admin/statistics/top-dishes`), Hiệu suất nhân viên (`/admin/statistics/staff`).

#### 4.6 Quản trị viên (Admin Role)
- **Báo cáo Doanh thu độc quyền (`/admin/statistics/revenue`):** Biểu đồ doanh thu theo thời gian, tỷ lệ thanh toán, bộ lọc nâng cao.
- **Quản trị người dùng & Phân quyền nâng cao:** Khóa tài khoản, reset mật khẩu nhân sự.
- **Cài đặt hệ thống (`/admin/settings`):** Cấu hình nhà hàng, thông tin hóa đơn.

---

### GIAI ĐOẠN 5: KIỂM TOÁN CHẤT LƯỢNG UI/UX, BÁO CÁO LỖI & BÀN GIAO

1. **Kiểm toán chất lượng UI/UX:**
   - Kiểm tra tràn ngang (Horizontal overflow), vỡ khung layout trên mobile/tablet.
   - Kiểm tra kích thước nút bấm cảm ứng (Touch targets < 44px).
   - Kiểm tra độ tương phản màu sắc và tính nhất quán của font chữ, icon.
   - Thu thập console errors và network request failures từ log browser.
2. **Tổng hợp 4 báo cáo kiểm thử:**
   - `reports/SCREENSHOT_INVENTORY.md`: Mục lục toàn bộ ảnh chụp màn hình (Role, Viewport, Route, Component, Đường dẫn file).
   - `reports/QA_TEST_RESULTS.md`: Thống kê tổng quan số test pass/fail, số route, modal, widget đã kiểm thử.
   - `reports/RESPONSIVE_AUDIT.md`: Đánh giá chi tiết độ tương thích responsive trên 3 nhóm thiết bị.
   - `reports/BUG_REPORT.md`: Danh sách lỗi UI/UX, mã lỗi, console error, mức độ nghiêm trọng (Severity: Critical/Major/Minor) kèm ảnh minh chứng.

---

## 5. QUY CHUẨN ĐẶT TÊN & LƯU TRỮ SCREENSHOTS

Lưu trữ theo cấu trúc thư mục phân cấp rõ ràng:

```
screenshots/
├── customer/
│   ├── desktop/    (pages/, modals/, forms/, widgets/)
│   ├── tablet/     (pages/, modals/, forms/, widgets/)
│   └── mobile/     (pages/, modals/, forms/, widgets/)
├── staff/
│   ├── desktop/
│   ├── tablet/
│   └── mobile/
├── chef/
├── cashier/
├── manager/
├── admin/
└── shared/         (notifications/, errors/, auth/)
```

### Quy ước đặt tên file (Naming Convention)
`{ROLE}_{DEVICE}_{TYPE}_{NAME}_{STATE}.png`

*Ví dụ:*
- `customer_desktop_pages_menu_default.png`
- `customer_mobile_modals_item-options_open.png`
- `staff_desktop_pages_table-map_active-sessions.png`
- `chef_tablet_pages_kitchen-queue_realtime.png`
- `cashier_desktop_modals_payment-invoice_cash.png`
- `manager_desktop_widgets_kpi-cards_loaded.png`
- `admin_desktop_pages_revenue-analytics_filter-month.png`
- `shared_all_notifications_auth-invalid-credentials.png`

---

## 6. TIÊU CHUẨN NGHIỆM THU (DEFINITION OF DONE - DOD)

Nhiệm vụ chỉ được coi là hoàn tất khi thỏa mãn đầy đủ các điều kiện sau:

- [ ] **Môi trường:** Backend, Frontend, Redis chạy ổn định trong suốt quá trình test; không can thiệp sửa đổi schema hay logic nghiệp vụ cốt lõi.
- [ ] **Bao phủ 6 Role:** Cả 6 vai trò (`Customer`, `Staff`, `Chef`, `Cashier`, `Manager`, `Admin`) đều được kiểm thử đúng quyền hạn thực tế.
- [ ] **Bao phủ Viewport:** 100% các trang và modal chính được chụp trên đủ 3 kích thước chuẩn: Desktop (`1440x900`), Tablet (`768x1024`), Mobile (`390x844`).
- [ ] **Tương tác động:** Bắt được trạng thái mở của tất cả các Modals chính (Options, Cart, Table Detail, Payment, Category/Menu CRUD).
- [ ] **Kho ảnh:** Toàn bộ ảnh chụp được sắp xếp đúng thư mục, định dạng PNG sắc nét, không bị chụp đè khi trang đang tải dở (loading spinner).
- [ ] **Bàn giao đầy đủ 6 Báo cáo:**
  1. `reports/PROJECT_DISCOVERY.md`
  2. `reports/ROLE_FEATURE_MATRIX.md`
  3. `reports/SCREENSHOT_INVENTORY.md`
  4. `reports/QA_TEST_RESULTS.md`
  5. `reports/RESPONSIVE_AUDIT.md`
  6. `reports/BUG_REPORT.md`
- [ ] **Kịch bản tự động:** Bàn giao bộ mã nguồn script Playwright hoàn chỉnh để có thể tái chạy (re-run) bất kỳ lúc nào trong CI/CD.
