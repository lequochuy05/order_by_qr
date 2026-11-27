# Spec `008`: Hệ thống Nâng cấp Gói Pro & Paywall (Free vs Pro + PayOS VietQR)

- **Type:** `New Feature`
- **Branch / Track:** `Nhánh A (Full SDD)` *(Tích hợp module `subscription`, migration `V1.27`, định tuyến webhook PayOS nâng cấp gói không cycle, bộ kiểm tra quyền gói `@RequiresPlanFeature` 2 lớp và màn hình `SCR-28`)*
- **Status:** `Implemented (Pending Sync-Back Review)` *(Draft → In Progress → Implemented (Pending Sync-Back Review) → Archived)*
- **Source of Truth:**
  - PO: [journey-features.md](../../01-po/journey-features.md) (`F6.8`) · [personas.md](../../01-po/personas.md) (`PER-01`–`PER-06`)
  - Design: [PROTOTYPE_SCREEN_MAP.md](../../02-design/PROTOTYPE_SCREEN_MAP.md) (`SCR-28`, `SCR-05`, `SCR-10`, `SCR-12`, `SCR-17`, `SCR-18`, `SCR-20`, `SCR-27`) · [DESIGN_SYSTEM.md](../../02-design/DESIGN_SYSTEM.md)
  - BA PRD: [PRD_BY_USER_STAGE.md](../../03-ba/PRD_BY_USER_STAGE.md) (`STG-06`, `BR-04`, `BR-24`, `BR-37`, `BR-38`)
  - BA User Stories: [USER_STORIES.md](../../03-ba/USER_STORIES.md) (`US-035`, `US-036`)
  - Tech: [SYSTEM_DESIGN.md](../../04-tech/SYSTEM_DESIGN.md)

---

## 1. Phạm vi & Nguyên tắc an toàn
- **Phân tầng gói (`F6.8`, `BR-37`, `US-036`):**
  1. Gói `FREE` giữ nguyên luồng bán hàng cơ bản (QR Menu, đặt món, KDS, sơ đồ bàn, mang về, thanh toán tại quầy) với hạn mức `10` bàn hoạt động và `5` tài khoản nhân sự hoạt động.
  2. Gói `PRO` (`TRIALING` hoặc `ACTIVE` còn hạn) mở khóa `AI_OPERATIONS_SUITE`, `AI_CUSTOMER_ASSISTANT`, `INVENTORY_BOM` (thao tác ghi/nhập kho/BOM) và `UNLIMITED_RESOURCES`.
  3. Bảo vệ 2 lớp: Soft Paywall (`ProBadge` + `UpgradeProModal`) trên Frontend và `SubscriptionFeatureInterceptor` (`@RequiresPlanFeature`, trả HTTP `402 PRO_PLAN_REQUIRED`) trên Backend.
  4. Hạ gói an toàn (Graceful Downgrade): Khi `PRO` hết hạn, giữ nguyên toàn bộ dữ liệu kho/BOM/bàn/nhân sự cũ, chỉ khóa thao tác tạo mới/ghi kho và gọi AI.
- **Dùng thử & Nâng cấp tự động (`BR-38`, `US-035`):**
  1. Chỉ `ADMIN` (`PER-06`) được kích hoạt dùng thử Pro 14 ngày (1 lần duy nhất), khởi tạo giao dịch VietQR PayOS (giá cấu hình qua `application.yml`, mặc định demo `2.000đ`/tháng & `5.000đ`/năm), đồng bộ `/sync` và hủy giao dịch `PENDING`.
  2. Webhook PayOS xác thực chữ ký HMAC, chống xử lý trùng (`findByGatewayOrderCodeForUpdate`), cộng dồn `30`/`365` ngày nối tiếp mốc `expiresAt` nếu đang còn hạn Pro, evict cache và phát WebSocket `/topic/settings` để đồng bộ realtime.
