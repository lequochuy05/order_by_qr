# QROS Master Design System — Evondev Signature Edition (`02-design`)

> **Tầng Knowledge:** `02-design` (Design System & Visual Language)  
> **Kế thừa bắt buộc từ (`01-po`):** [personas.md](../01-po/personas.md) · [vision.md](../01-po/vision.md) · [journey-features.md](../01-po/journey-features.md)  
> **Đồng bộ tài nguyên thiết kế:** [figma-tokens.json](./figma-tokens.json) · [PROTOTYPE_SCREEN_MAP.md](./PROTOTYPE_SCREEN_MAP.md) · [figma/](./figma/)  
> **Style:** Modern SaaS & Restaurant POS — Sleek, Clean & High Aesthetic (`v3.0.0`)  
> **Thiết bị mục tiêu:** Mobile Order (Khách tại bàn), Tablet POS (Phục vụ & Thu ngân), Màn hình Bếp (KDS), Desktop Quản trị

---

## 1. Nguyên tắc Thiết kế Sản phẩm (Grounded in `01-po`)

1. **Khoảng thở & Phân cấp thị giác (Clarity, Air & Breathing Room):**  
   Mọi thành phần đều có khoảng trắng hợp lý. Mật độ thông tin cao của nghiệp vụ nhà hàng đạt được nhờ tương phản màu, thang cỡ chữ rõ ràng và hệ thống nhãn trạng thái (status badges) thay vì nhồi nhét chi tiết.

2. **Thẩm mỹ SaaS hiện đại & Chống "AI Slop":**  
   Sử dụng nền sạch (`#F8FAFC` ở Light Mode, `#0B0F17` ở Dark Mode) làm bệ đỡ cho ảnh món ăn thực tế và các điểm nhấn thương hiệu Electric Indigo → Royal Violet. Mọi khoảng cách, cỡ chữ, bo góc và độ tương phản đều được khóa bằng thông số cụ thể thay vì trang trí cảm tính.

3. **Thích ứng theo 6 vai trò vận hành (Role-Adaptive Experience):**
   - **`PER-01` (Thực khách — Mobile-First):** Thẻ món giàu hình ảnh, thao tác 1 tay vừa ngón cái, ngăn kéo giỏ hàng nổi ở cạnh dưới, nhãn theo dõi tiến độ món trực tiếp.
   - **`PER-02` (Phục vụ) & `PER-04` (Thu ngân — Tablet/Desktop POS):** Sơ đồ bàn 4 trạng thái nhận diện tức thì, modal gộp bàn & thanh toán ít bước, số tiền lớn dễ kiểm tra.
   - **`PER-03` (Đầu bếp — KDS):** Bảng Kanban 3 cột & chế độ Nấu gom tương phản cao, nhìn rõ từ xa, đồng hồ đếm giờ nổi bật kèm cảnh báo quá hạn 20 phút.
   - **`PER-05` (Quản lý) & `PER-06` (Chủ quán — Desktop Dashboard):** Thẻ KPI trực quan, biểu đồ doanh thu/tỷ trọng gọn mắt, bảng dữ liệu phân trang và hỗ trợ chuyển đổi song ngữ Việt – Anh mượt mà.

4. **Đồng nhất Light Mode & Dark Mode (WCAG 2.1 AA):**  
   Dark Mode được thiết kế theo tầng độ sâu *Deep Midnight* (`#0B0F17` nền → `#131B2E` thẻ → `#1E293B` hover) giúp nhân viên làm ca tối và bếp đỡ mỏi mắt, đảm bảo toàn bộ văn bản đạt chuẩn tương phản **WCAG 2.1 AA**.

---

## 2. Hệ thống Màu sắc & Design Tokens

Toàn bộ token được chuẩn hóa đồng bộ với [figma-tokens.json](./figma-tokens.json) theo **kiến trúc 2 tầng**:
- **Tầng Token Thương hiệu (Static Brand Tokens — `--color-brand-*`):** Bảng màu chuẩn cốt lõi dùng chung trong bộ UI Kit.
- **Tầng Token Ngữ nghĩa (Semantic Theme Tokens — `:root` / `.dark`):** Biến đổi linh hoạt giữa Light Mode và Dark Mode ở thời điểm hiển thị mà không phải thiết kế trùng lặp component.

### 2.1 Bảng màu Thương hiệu & Bề mặt (Light / Dark)

| Semantic Token (`:root` / `.dark`) | Brand Token (`@theme`) | Light Mode | Dark Mode | Mục đích sử dụng |
|---|---|---|---|---|
| `--brand-primary` | `--color-brand-primary` | `#6366F1` (Indigo 500) | `#818CF8` (Indigo 400) | Màu nhận diện chính, nút CTA chính, mục menu đang chọn, đường biểu đồ |
| `--brand-primary-hover` | `--color-brand-primary-hover` | `#4F46E5` (Indigo 600) | `#A5B4FC` (Indigo 300) | Trạng thái di chuột (hover) nút chính |
| `--brand-secondary` | `--color-brand-secondary` | `#8B5CF6` (Violet 500) | `#A78BFA` (Violet 400) | Điểm nhấn phụ, nổi bật danh mục, gradient phụ |
| `--brand-secondary-hover` | `--color-brand-secondary-hover` | `#7C3AED` (Violet 600) | `#C4B5FD` (Violet 300) | Trạng thái hover nút phụ |
| `--bg-app` / `--brand-surface` | `--color-brand-surface` | `#F8FAFC` (Slate 50) | `#0B0F17` (Deep Midnight) | Nền tổng thể toàn ứng dụng |
| `--bg-card` | `--color-brand-surface-card` | `#FFFFFF` (Pure White) | `#131B2E` (Deep Slate) | Nền thẻ nổi, modal, khung bảng dữ liệu |
| `--bg-card-hover` | `--color-brand-surface-card-hover` | `#F1F5F9` (Slate 100) | `#1E293B` (Slate 800) | Nền khi di chuột vào thẻ hoặc dòng bảng |
| `--border-color` | `--color-brand-border` | `#E2E8F0` (Slate 200) | `#1E293B` (Slate 800) | Đường kẻ phân cách, viền card và viền ô nhập |

> *Ghi chú mở rộng:* Bộ token còn cung cấp 2 màu nhấn nhạt cho nền badge (`--color-brand-primary-light: #EEF2FF`, `--color-brand-secondary-light: #F5F3FF`).

### 2.2 Bảng màu Chữ (Typography Colors)

| Semantic Token | Cấp độ | Light Mode | Dark Mode | Ứng dụng |
|---|---|---|---|---|
| `--text-primary` | **Chữ chính** | `#0F172A` (`slate-900`) | `#F8FAFC` (`slate-50`) | Tiêu đề trang, số KPI lớn, tên món, tên bàn |
| `--text-secondary` | **Chữ phụ** | `#475569` (`slate-600`) | `#94A3B8` (`slate-400`) | Mô tả món ăn, nội dung ô bảng, phụ đề |
| `--text-muted` | **Chữ mờ** | `#94A3B8` (`slate-400`) | `#64748B` (`slate-500`) | Mốc thời gian, placeholder, trạng thái rỗng |

### 2.3 Hệ màu Trạng thái Ngữ nghĩa (5 Trạng thái Vận hành)

| Trạng thái ngữ nghĩa | Nền (Light) | Chữ / Icon (Light) | Nền (Dark) | Chữ / Icon (Dark) | Ngữ cảnh sử dụng trên Prototype |
|---|---|---|---|---|---|
| **Success** (Thành công / Sẵn sàng) | `#ECFDF5` | `#047857` | `rgba(16, 185, 129, 0.12)` | `#34D399` | Bàn trống, món hoàn tất, yêu cầu hỗ trợ bàn đã hoàn tất, đã thanh toán |
| **Warning** (Cảnh báo / Đang chờ) | `#FFFBEB` | `#B45309` | `rgba(245, 158, 11, 0.12)` | `#FBBF24` | Bàn chờ thanh toán, món đang chờ/nấu, yêu cầu bàn đang chờ tiếp nhận, tồn kho thấp |
| **Danger** (Khẩn cấp / Hủy / Lỗi) | `#FEF2F2` | `#B91C1C` | `rgba(239, 68, 68, 0.12)` | `#F87171` | Món KDS quá 20 phút, yêu cầu bàn chờ quá 5 phút (vượt SLA), hủy món, hết hàng, lỗi nhập liệu |
| **Info** (Thông tin / Trực tuyến) | `#EFF6FF` | `#1D4ED8` | `rgba(59, 130, 246, 0.12)` | `#60A5FA` | Đơn mang về (Takeaway), yêu cầu bàn đang có nhân viên xử lý, giao dịch VietQR trực tuyến |
| **Brand / AI** (Điểm nhấn / AI) | `#EEF2FF` | `#4338CA` | `rgba(99, 102, 241, 0.12)` | `#818CF8` | Gợi ý món AI, sinh mô tả AI, dự báo nhu cầu |

---

## 3. Hệ thống Chữ viết (Typography Scale)

- **Font chủ đạo (`--font-display`, `--font-body`):** **Be Vietnam Pro** (trọng lượng `400` → `900`) — tối ưu dấu tiếng Việt rõ nét ở cả kích thước nhỏ trên điện thoại và khoảng cách xa trên màn hình bếp.
- **Font hỗ trợ số liệu:** **Inter** / kiểu chữ số thẳng cột (`tabular-nums` / `font-mono`) cho giá món, tổng tiền, tiền thối và số KPI để tránh nhảy cột khi cập nhật trực tiếp.

| Cấp độ chữ | Kích thước | Độ dày | Giãn chữ | Ứng dụng |
|---|---|---|---|---|
| **Display Hero** | `36px` (`2.25rem`) | `900 Black` | `-0.03em` | Tiêu đề trang chào, Landing |
| **Page Title** | `28–30px` | `800 ExtraBold` | `-0.025em` | Tiêu đề màn hình quản trị & bảng điều khiển |
| **Section Heading** | `20px` (`1.25rem`) | `700 Bold` | `-0.015em` | Tiêu đề khối nội dung, tiêu đề hộp thoại (Modal) |
| **Card Heading** | `16px` (`1rem`) | `700 Bold` | `0em` | Số bàn, tên món ăn, tên tài khoản |
| **Body Standard** | `14px` (`0.875rem`) | `500 Medium` | `0em` | Nhãn trường nhập, nội dung bảng, mô tả |
| **Body Small** | `12px` (`0.75rem`) | `500 Medium` | `0em` | Phụ đề, ghi chú hướng dẫn, chi tiết topping |
| **Micro Caption / Badge** | `11px` (`0.6875rem`) | `800 ExtraBold` | `+0.05em` | Nhãn trạng thái, tiêu đề cột bảng, pill nhỏ |
| **Financial / KPI Metric** | `24–32px` | `800–900` | `tabular / mono` | Giá bán, tổng hóa đơn, tiền thối, doanh thu |

---

## 4. Khoảng cách, Hình khối, Độ sâu & Chuyển động

### 4.1 Thang Khoảng cách (4px Grid System)
- `xs` (`8px`): Khoảng cách giữa icon và chữ, padding dọc nút nhỏ
- `sm` (`12px`): Khoảng cách giữa các dòng danh sách, khoảng thở nội bộ thẻ
- `md` (`16px`): Padding chuẩn của thẻ, lề bên trong modal
- `lg` (`24px`): Lề khung trang, khoảng cách giữa các khu vực nội dung
- `xl` (`32px`): Khoảng cách lưới lớn trên Dashboard

### 4.2 Hệ Bo góc (Evondev Smooth Curves)
- `8px` (`rounded-lg`): Nhãn nhỏ, nút thao tác mini
- `12px` (`rounded-xl`): Ô nhập liệu, hộp chọn (dropdown), nút phụ
- `16px` (`rounded-2xl`): Thẻ món ăn, khung hộp thoại (modal), danh sách con
- `24px` (`rounded-3xl`): Thẻ KPI, khung biểu đồ, khung bảng dữ liệu chính
- `9999px` (`rounded-full`): Status pills, ảnh đại diện tròn, chấm trạng thái trực tiếp

### 4.3 Độ sâu (Elevation) & Chuyển động (Motion)
- **Thẻ mặc định:** Viền mảnh độ mờ 80% + đổ bóng nhẹ (`shadow-sm`).
- **Thẻ tương tác (Hover):** Nâng nhẹ `-4px` kèm bóng lan mềm mượt trong `300ms`.
- **Thanh tiêu đề cố định (Sticky Top Bar):** Hiệu ứng kính mờ nhẹ (`backdrop-blur`, nền mờ 90%) giúp giữ bối cảnh khi cuộn trang.
- **Phản hồi chuyển động có chủ đích:** Phản hồi nảy nhẹ khi thêm món vào giỏ hàng, hiệu ứng xuất hiện mượt cho danh sách/modal, hiệu ứng khung xương (skeleton shimmer) khi đang tải dữ liệu và tôn trọng vùng an toàn màn hình tai thỏ (Safe Area) trên thiết bị di động.

---

## 5. Quy chuẩn Thành phần Giao diện (Component Patterns)

| Nhóm Pattern | Quy chuẩn Thiết kế & Tương tác |
|---|---|
| **KPI Metric Card** | Khung bo `24px`, tiêu đề chữ hoa nhỏ phía trên, số liệu lớn `tabular/mono` nổi bật, khối icon bo `16px` nền gradient thương hiệu bên phải, chân thẻ có badge so sánh xu hướng. |
| **Status Pill (Live)** | Bo tròn `9999px`, dùng 1 trong 5 bộ màu ngữ nghĩa (§2.3), gắn chấm tròn nhịp đập nhẹ cho các trạng thái đang cập nhật trực tiếp (bàn đang mở, món đang nấu). |
| **Form & Input Controls** | Ô nhập bo `12px`, nhãn rõ phía trên, viền focus dùng `--brand-primary`, thông báo lỗi hiển thị ngay dưới trường nhập không làm mất dữ liệu đang gõ. |
| **Modal & Drawer** | Desktop dùng hộp thoại bo `16–24px` canh giữa có nút đóng rõ; Mobile ưu tiên Bottom Sheet vuốt đóng thuận tay cho giỏ hàng, chọn topping và gọi nhân viên hỗ trợ. |
| **Quick-Reason Chip Group & Service Queue Card** | Nhóm chip lý do chọn nhanh bo `12px`/`9999px` hỗ trợ chạm 1 tay (vùng bấm ≥ `44px`), nút gửi hiển thị đếm ngược thời gian chờ chống bấm lặp; Thẻ hàng đợi yêu cầu bàn bo `16px` gồm nhãn trạng thái Live (`Warning`/`Info`/`Danger`), đồng hồ đếm thời gian chờ `tabular/mono`, chip lý do, tên nhân viên đang phụ trách và 1 nút hành động chính nổi bật. |
| **Biểu đồ (Area & Donut)** | Biểu đồ miền dùng đường mượt nét `3px` kèm tô chuyển sắc nhạt dưới đường và tooltip kính mờ; biểu đồ Donut hiển thị con số tổng ngay tâm kèm chú giải gọn phía dưới. |
| **Bảng Dữ liệu (Data Table)** | Header chữ hoa nhỏ tương phản nhẹ, hàng đổi màu mượt khi hover, huy hiệu Vàng/Bạc/Đồng cho Top 3 món bán chạy, thanh tiến độ tỷ trọng và phân trang tối giản. |

---

## 6. Ghi chú Bàn giao Design-to-Dev (Handoff Note)
- Bộ token chuẩn nằm tại [figma-tokens.json](./figma-tokens.json) và tài nguyên dựng khung Figma nằm trong [figma/](./figma/).
- Khi chuyển sang tầng kỹ thuật (`04-tech/` & `frontend/`), các biến màu và font trên được ánh xạ trực tiếp vào bảng biến CSS/Tailwind 2 tầng (không hardcode mã hex trong từng màn hình) để giữ đồng bộ 100% giữa Design và Code.
