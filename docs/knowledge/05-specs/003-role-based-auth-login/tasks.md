# Execution Tasks `003` (Baseline — Dựng ngược từ code): Role-Based Authentication & Login Flow

- **Type:** `Baseline Verification Checklist` *(dựng ngược từ code có sẵn để demo luồng đăng nhập theo 6 vai trò; mục Sync-Back giữ mở chờ người dùng duyệt)*
- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD (khớp CI):** `mvn -f backend/pom.xml --batch-mode verify` · `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Database & Redis State (Đã có trong code — đã đối chiếu)
- [x] Bảng `users` (5 vai trò `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF`, cột `must_change_password` — `V1`, `V1.19`, `V1.20`) và `password_reset_tokens` (`V1`, `V1.13`)
- [x] Lưu trữ phân tán trên Redis cho 2FA ticket (`qros:2fa:ticket:*`), Refresh Token Rotation, Token Blacklist, Lockout và Rate Limit

## Nhóm 2 — Backend Auth, 2FA & RBAC + Tests (Đã có trong code — đã đối chiếu)
- [x] `AuthController` + `AuthService` (chống timing-attack bằng `dummyPasswordHash`, chặn `ACCOUNT_INACTIVE`, nhánh 2FA cho `ADMIN`)
- [x] `Admin2faService` (OTP 6 số gửi qua Brevo HTTPS / SMTP, `verify-2fa`, `resend-2fa`), `AccountLockoutService` (HTTP 423), `AuthRateLimitService` (HTTP 429)
- [x] `RefreshTokenService` + HttpOnly Cookie (`Path=/api/v1`) + `CookieRequestOriginValidator` + `TokenBlacklistService`
- [x] `SecurityConfig` (`RoleHierarchy`: `ADMIN > MANAGER > CASHIER > STAFF`, `MANAGER > CHEF`)
- [x] 11 bộ test có sẵn: `AuthControllerTest`, `AuthServiceTest`, `Admin2faServiceTest`, `AccountLockoutServiceTest`, `AuthRateLimitServiceTest`, `PasswordResetServiceTest`, `RefreshTokenServiceTest`, `JwtAuthFilterTest`, `TokenBlacklistServiceTest`, `CoreApiSecurityIntegrationTest`, `SecurityConfigRegressionTest`

## Nhóm 3 — Frontend Login, Role Redirect & Route Guard + Tests (Đã có trong code — đã đối chiếu)
- [x] `LoginPage.jsx` (form đăng nhập 1 bước cho Staff/Chef/Cashier/Manager; bước 2FA OTP + cooldown 60s cho Admin; xử lý lỗi 423/429; điều hướng mặc định đúng từng vai trò)
- [x] `ProtectedRoute.jsx` + `router.jsx` (chặn chưa đăng nhập về `/login`, cưỡng chế `mustChangePassword` về `/admin/profile?forceChangePassword=true`, phân quyền route theo từng vai trò và đẩy trái quyền về `/unauthorized`)
- [x] `ForgotPasswordPage.jsx`, `ResetPasswordPage.jsx`, `AuthContext.jsx`, `httpClient.js` (tự refresh token khi 401)
- [x] Bộ test có sẵn: `frontendHardening.test.js`, `errorMessages.test.js`, `systemErrors.test.js`

## Nhóm 4 — Verification & Sync-Back Proposal (Đang mở — Chờ người dùng duyệt)
- [x] Đối chiếu code thật với `02-design/`, `03-ba/`, `04-tech/`, xuất bảng **Sync-Back Proposal (`SB-08` → `SB-10`)** trong [spec.md](./spec.md)
- [ ] **Chờ người dùng duyệt** từng dòng `SB-08` → `SB-10`
- [ ] *(Chỉ làm sau khi duyệt)* Áp dụng diff vào `PROTOTYPE_SCREEN_MAP.md`, `PRD_BY_USER_STAGE.md`, `USER_STORIES.md`, `SYSTEM_DESIGN.md`
- [ ] *(Chỉ làm sau khi duyệt)* Ghi 1 dòng vào `Sync Changelog` trong `KNOWLEDGE_HUB.md` và đổi `spec.md` sang `Status: Archived`
