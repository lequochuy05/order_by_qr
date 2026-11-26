# Technical Plan `003` (Baseline — Dựng ngược từ code): Role-Based Authentication & Login Flow

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Kiến trúc & Hợp đồng giao tiếp thực tế trong codebase

### Database / Flyway & Redis
- **PostgreSQL:** `users` (5 vai trò `UserRole` + `status` + `must_change_password` từ `V1.19` + `ADMIN`/`CASHIER` từ `V1.20`), `password_reset_tokens` (`V1`, `V1.13`).
- **Redis:** Lưu ticket 2FA (`qros:2fa:ticket:*`), Refresh Token Rotation (`RedisRefreshTokenStore`), JWT Blacklist (`TokenBlacklistService`), bộ đếm Rate Limit (`AuthRateLimitService`) và Account Lockout (`AccountLockoutService`).

### REST API (`AuthController` — `/api/v1/auth`)
- `POST /api/v1/auth/login` → đăng nhập (1 bước cho `STAFF`/`CHEF`/`CASHIER`/`MANAGER`; trả `requires2fa=true` + `preAuthToken` cho `ADMIN`)
- `POST /api/v1/auth/verify-2fa` → xác minh OTP 6 số cho `ADMIN`, cấp JWT + HttpOnly Refresh Cookie (`Path=/api/v1`)
- `POST /api/v1/auth/resend-2fa` → gửi lại OTP (áp rate limit + cooldown 60s)
- `POST /api/v1/auth/refresh` → xoay vòng Refresh Token (kiểm tra Origin qua `CookieRequestOriginValidator`)
- `POST /api/v1/auth/logout` → xóa Refresh Cookie + đưa Access Token vào blacklist
- `POST /api/v1/auth/forgot-password-email`, `/forgot-password-phone`, `/reset-password-*` → khôi phục mật khẩu trung tính chống enumeration

### Security Hierarchy (`SecurityConfig`)
- `RoleHierarchy`: `ADMIN > MANAGER > CASHIER > STAFF` và `MANAGER > CHEF`.
- Frontend `ProtectedRoute.jsx` kết hợp danh sách `allowedRoles` rõ ràng cho từng cụm route trong `router.jsx` và cưỡng chế `mustChangePassword` về `/admin/profile?forceChangePassword=true`.

---

## 2. Bản đồ File thực tế trong codebase

### Backend (`backend/`)
- Controller & Properties: `AuthController.java`, `AuthLockoutProperties.java`, `AuthRateLimitProperties.java`, `PasswordResetProperties.java`
- Services & Store: `AuthService.java`, `Admin2faService.java`, `AccountLockoutService.java`, `AuthRateLimitService.java`, `RefreshTokenService.java`, `RedisRefreshTokenStore.java`, `PasswordResetService.java`, `CustomUserDetailsService.java`, `TokenBlacklistService.java`
- Security Config: `SecurityConfig.java`, `SecurityRoutes.java`, `CookieRequestOriginValidator.java`, `JwtAuthFilter.java`
- Tests liên quan: `AuthControllerTest.java`, `AuthServiceTest.java`, `Admin2faServiceTest.java`, `AccountLockoutServiceTest.java`, `AuthRateLimitServiceTest.java`, `PasswordResetServiceTest.java`, `RefreshTokenServiceTest.java`, `JwtAuthFilterTest.java`, `TokenBlacklistServiceTest.java`, `CoreApiSecurityIntegrationTest.java`, `SecurityConfigRegressionTest.java`

### Frontend (`frontend/`)
- Auth Feature: `frontend/src/features/auth/model/AuthContext.jsx`, `frontend/src/features/auth/ui/ProtectedRoute.jsx`
- Auth Pages: `frontend/src/pages/auth/{LoginPage.jsx, ForgotPasswordPage.jsx, ResetPasswordPage.jsx}`, `frontend/src/pages/system/UnauthorizedPage.jsx`
- Router & API: `frontend/src/app/router.jsx`, `frontend/src/shared/api/{authService.js, httpClient.js}`
- Tests liên quan: `frontend/tests/unit/{frontendHardening.test.js, errorMessages.test.js, systemErrors.test.js}`

---

## 3. Đối chiếu với `CONSTITUTION.md`
- **Auto-Enforced:** 11 bộ test auth/security backend (`mvn verify` gồm `CoreApiSecurityIntegrationTest`, `SecurityConfigRegressionTest`, `Admin2faServiceTest`, `BackendDependencyRulesTest`, `BackendMessageLanguageTest`, `spotless:check`) + `npm test`, `eslint`, `vite build`.
- **Manual Review:** Xác minh quy tắc không đi Fast Track cho thay đổi auth/RBAC, đối chiếu đầy đủ 6 vai trò trên `ProtectedRoute` và `SecurityConfig`, kiểm tra thông báo lỗi UI thân thiện & hỗ trợ Dark Mode trên `LoginPage`.
