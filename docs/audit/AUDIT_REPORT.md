# QROS FRONTEND UI/UX AUDIT REPORT
**Target Branch:** `refactor/architecture-improvements`  
**Date:** November 2026  
**Auditor:** Lead Product Designer & Frontend Architect

---

## 1. Executive Summary

QROS is an enterprise-grade restaurant ordering and operational platform serving six core roles: **CUSTOMER, STAFF, CHEF, MANAGER, ADMIN, and CASHIER**.

This comprehensive frontend audit evaluates:
- Complete route inventory and authorization guards
- Verified backend role hierarchy and permissions
- Page-to-feature architecture
- State management, API layers, and WebSocket real-time contracts
- Existing visual language, design inconsistencies, and technical debt

The audit confirms that the backend and business logic are mature and robust. However, the existing frontend suffers from fragmented design patterns, hardcoded color styles (`#74ebd5` gradients, arbitrary `orange-500` / `slate-900` usage), inconsistent dark mode coverage, and lack of a cohesive "Warm Hospitality" Modern POS design system.

---

## 2. Route Inventory & Authorization Matrix

### 2.1 Public & Customer Routes

| Route | Component | Access / Guard | Description |
|---|---|---|---|
| `/` | `LandingPage.jsx` | Public | Welcome screen with QR scan instructions and staff login button |
| `/:tableCode/menu` | `OrderingPage.jsx` | Public (Table Scoped) | Customer ordering menu with table session verification (`sig`) |
| `/menu` | `OrderingPage.jsx` | Public (Unscoped) | Fallback customer menu |
| `/login` | `LoginPage.jsx` | Public / Guest | Staff & Admin login with 2FA OTP verification for ADMIN |
| `/forgot-password` | `ForgotPasswordPage.jsx` | Public / Guest | Request password reset token via email |
| `/reset-password` | `ResetPasswordPage.jsx` | Public / Guest | Reset password with token from email |
| `/unauthorized` | `UnauthorizedPage.jsx` | Public | 403 Forbidden feedback |
| `/not-found` | `NotFoundPage.jsx` | Public | 404 Not Found feedback |
| `/offline` | `SystemStatusPage.jsx` | Public | Network disconnected state |
| `/error/:statusCode` | `SystemStatusPage.jsx` | Public | Generic HTTP error handler |

### 2.2 Protected Admin & Operations Routes

All operations routes are wrapped in `ProtectedRoute` and nested under `AdminLayout.jsx`.

| Route | Page Component | Allowed Roles | Verified Responsibilities & Features |
|---|---|---|---|
| `/admin/profile` | `ProfilePage.jsx` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF`, `CHEF` | View/edit personal profile, upload avatar, change password (with 8+ char, uppercase, number, symbol policy), force-change temporary password flow. |
| `/admin/settings` | `SettingsPage.jsx` | `ADMIN` (Full tabs), `MANAGER`, `CASHIER`, `STAFF`, `CHEF` (Preferences only) | **ADMIN:** Restaurant info, logo upload, payment configs (cash/PayOS), customer menu options, operation toggles, bill printer template, notification thresholds, WiFi QR.<br>**Others:** Personal language, dark mode, notification audio. |
| `/admin/dashboard` | `DashboardPage.jsx` | `ADMIN`, `MANAGER` | High-level business overview: Daily revenue, order volume, average order value, table occupancy, AI sales forecast, dish popularity breakdown. |
| `/admin/tables` | `TableMapPage.jsx` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | Visual table board, table status filters (AVAILABLE, OCCUPIED, WAITING_FOR_PAYMENT, INACTIVE), open orders, order item additions, table merging (`useMergeTablesMutation`), settlement launch. |
| `/admin/takeaway` | `TakeawayPage.jsx` | `ADMIN`, `MANAGER`, `CASHIER`, `STAFF` | Takeaway order lifecycle: create order, add dishes, monitor status, trigger settlement. |
| `/admin/history` | `OrderHistoryPage.jsx` | `ADMIN`, `MANAGER`, `CASHIER` | Historical orders list with pagination, date range presets (Today, Yesterday, 7 days, 30 days, Custom), order detail modal, receipt printing, order reconciliation. |
| `/admin/kitchen` | `KitchenPage.jsx` | `ADMIN`, `MANAGER`, `CHEF` | Kitchen Display System (KDS): 3-column Kanban (PENDING, COOKING, READY), Batch Cooking view, wait-time timers, overdue audio alerts (>20 min threshold), item cancelation, AI staff query panel. |
| `/admin/inventory` | `InventoryPage.jsx` | `ADMIN`, `MANAGER`, `CHEF` | Real-time stock levels, low-stock warnings, stock-in modal, stock adjustment modal, recipe editor. |
| `/admin/categories` | `CategoryPage.jsx` | `ADMIN`, `MANAGER` | Category CRUD, category image upload, display ordering. |
| `/admin/menu` | `MenuPage.jsx` | `ADMIN`, `MANAGER` | Dish CRUD, pricing, availability toggle, dish option groups (size, sugar, ice, toppings), image upload, AI dish description generation. |
| `/admin/combo` | `ComboPage.jsx` | `ADMIN`, `MANAGER` | Combo meals CRUD, item bundling, combo pricing, image upload. |
| `/admin/voucher` | `VoucherPage.jsx` | `ADMIN`, `MANAGER` | Voucher codes, discount rates (fixed VND or percentage), usage limits, validity date ranges, active toggle. |
| `/admin/staffs` | `UserPage.jsx` | `ADMIN`, `MANAGER` | Employee directory, role assignment, status (ACTIVE, INACTIVE, BANNED), avatar upload, password reset.<br>*Rule:* MANAGER can only manage STAFF, CASHIER, CHEF; ADMIN can manage all including MANAGER and other ADMINs. |
| `/admin/statistics/revenue` | `RevenueStatsPage.jsx` | `ADMIN` (Exclusive) | Detailed financial analytics: Revenue charts (Bar/Area), transaction breakdown, export reports. |
| `/admin/statistics/top-dishes` | `TopDishesStatsPage.jsx` | `ADMIN`, `MANAGER` | Best-selling dishes by volume and revenue, category performance breakdown. |
| `/admin/statistics/staff` | `StaffStatsPage.jsx` | `ADMIN`, `MANAGER` | Staff performance metrics: order processing count, sales contribution. |

---

## 3. Verified Backend Role & Permission Hierarchy

Verified from `SecurityConfig.java` and `UserRole.java`:
```
ROLE_ADMIN > ROLE_MANAGER
ROLE_MANAGER > ROLE_CASHIER
ROLE_MANAGER > ROLE_CHEF
ROLE_CASHIER > ROLE_STAFF
```

### Role Analysis:

1. **ADMIN**:
   - Super-user with system-level configuration authority (`PUT /api/v1/settings`, `POST /api/v1/settings/logo`).
   - Exclusive access to revenue analytics (`/api/v1/analytics/revenue`), forecasting (`/api/v1/forecast/**`), and actuator Prometheus metrics.
   - Destructive operations: Hard delete orders and inventory records.
   - Full user management across all roles.
   - Enforced 2FA OTP verification upon login.

2. **MANAGER**:
   - Operational manager overseeing daily restaurant workflows.
   - Full CRUD over Categories, Menu Items, Combos, Vouchers, Promotions, Tables, and Inventory.
   - Employee management over STAFF, CASHIER, and CHEF.
   - Access to operational analytics (Top dishes, Staff performance).
   - AI assistant access for menu descriptions and combo suggestions.

3. **CASHIER**:
   - Specialized financial operator.
   - Access to Table map, Takeaway orders, and Order history.
   - Authority over `/api/v1/payments/**`:
     - Cash settlement with cash received and change calculations.
     - PayOS QR generation, webhook synchronization, and payment verification.
     - Voucher application and validation.
     - Refund and reconciliation workflows.
   - Restricted from modifying restaurant settings, user accounts, or menu pricing.

4. **STAFF**:
   - Floor service staff.
   - Table map and takeaway order handling: view active tables, create orders, append items, merge tables.
   - Order status transitions: send to kitchen, cancel items with reasons.
   - Staff AI assistant query for operational FAQs.

5. **CHEF**:
   - Kitchen operations specialist.
   - Kitchen Display System: Kanban and Batch cooking views.
   - Real-time status transitions: `PENDING` -> `COOKING` -> `READY` -> `SERVED`.
   - Inventory operations: Record incoming stock (`stock-in`) and report adjustments/waste.

6. **CUSTOMER**:
   - Public unauthenticated or table-session authenticated user.
   - Scan QR -> Start session (`/api/v1/public/tables/*/start-session` with HMAC signature).
   - Browse dishes and combos with live availability.
   - Add customizable options (size, sweet/ice, notes).
   - Submit orders (`POST /api/v1/public/orders`).
   - Track order items live via WebSocket (`/topic/customer/session/*`).
   - Request payment call to staff.
   - Customer AI chat assistant for food recommendations.

---

## 4. UI/UX Architecture & Problems Identified

### 4.1 Visual Identity & Color Inconsistencies
- **Fragmented Color Tokens:** The app currently employs random bright colors (`orange-500`, `emerald-600`, `blue-500`, and arbitrary teal/purple gradients `#74ebd5` to `#ACB6E5` on Auth pages).
- **Missing Warm Hospitality Palette:** The product requires the confirmed Warm Hospitality palette:
  - Primary: `#56372B` (deep roasted espresso brown)
  - Secondary: `#C17C4A` (warm terracotta / copper amber)
  - Surface: `#F7F0E7` (soft linen cream)
- **Status Color Bleed:** In some views, brand orange was used as a generic status badge instead of clear semantic indicators (Success, Warning, Error, Info, Pending, Cooking, Completed, Cancelled).

### 4.2 Typography & Information Density
- **Font Scale:** While `Be Vietnam Pro` is referenced in `index.html`, the CSS imported `Inter` as first fallback in `--font-body`, leading to mixed typography rendering across Vietnamese diacritics.
- **Header vs Content Hierarchy:** Page headers, stat numbers, table values, and card badges lack a standardized typographic scale. Financial amounts (`fmtVND`) need tabular numeric alignment.

### 4.3 Component & Theme Issues
- **Dark Mode Disconnection:** The dark mode class is toggled via `document.documentElement.classList.toggle('dark')` in admin, but customer menu managed a local `isDarkMode` state without synchronized persistence.
- **Form Controls:** Form inputs had hardcoded `rounded-2xl` and `border-transparent` with inconsistent height and focus rings.
- **Navigation & Mobile Responsiveness:**
  - Sidebar collapse works, but the expanded submenus use disparate padding and transition speeds.
  - Notification icon in header is a non-interactive mockup without an operational notification center drawer.
  - Social login buttons on LoginPage are non-functional props.

---

## 5. Strategic Recommendations

1. **Design System Foundation:** Create `docs/design-system/MASTER.md` establishing the complete design token specification (Warm Hospitality palette, Be Vietnam Pro typographic scale, semantic status tokens, elevation, border-radius, density, and accessibility guidelines).
2. **Centralized CSS Variables & Tailwind Theme:** Update `frontend/src/app/styles/index.css` to expose design tokens via CSS variables on `:root` and `.dark`, wired into Tailwind CSS v4 `@theme`.
3. **Refactored Base Components:** Standardize `Button`, `Badge`, `Card`, `Modal`, `Table`, `Input`, `Select`, `Tabs`, and `EmptyState` in `src/shared/ui/`.
4. **Role-Tailored Operational Experiences:**
   - **Customer:** Mobile-first, mouth-watering food cards, warm linen surface, streamlined cart sheet, real-time item tracker.
   - **Staff & Cashier:** High-density table grid, quick payment drawer with cash tendered calculator and PayOS QR, merged table visual indicators.
   - **Chef:** High-contrast KDS with Kanban and Batch views, large countdown timers, audio alert toggles.
   - **Manager & Admin:** Polished executive dashboard with styled Recharts, structured CRUD tables, segmented settings tabs.
5. **Real-time Notification Center:** Enhance `AdminHeader` with an active Notification Drawer displaying real-time events (new orders, payment requests, kitchen overdue notices).
