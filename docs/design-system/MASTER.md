# QROS MASTER DESIGN SYSTEM — EVONDEV SIGNATURE EDITION
**Style:** Modern SaaS & Restaurant POS — Sleek, Clean & High Aesthetic  
**Typeface:** Be Vietnam Pro (Primary), Inter (Secondary)  
**Primary Brand Tokens:** `#6366F1` (Electric Indigo), `#8B5CF6` (Royal Violet), `#F8FAFC` (Slate 50 Surface)  
**Version:** 3.0.0  
**Target:** Web, Tablet POS, Mobile Order, Kitchen Display Systems (KDS)

---

## 1. Product Design Principles (Evondev Signature UI/UX)

1. **Clarity, Air & Breathing Room (Khoảng thở & Phân cấp thị giác):**  
   Every component possesses adequate whitespace. Information density is achieved through clear hierarchy (contrast, optical font sizing, badge tags) rather than cramped elements.

2. **Modern Sophisticated Aesthetics (Hiện đại & Tinh tế):**  
   Clean canvas foundations (`#F8FAFC` in light mode, `#0B0F17` in dark mode) paired with vibrant gradient accents (`from-indigo-600 to-violet-500`, `from-blue-600 to-cyan-500`, `from-emerald-600 to-teal-500`). Cards feature soft layered drop shadows and smooth hover lifts (`hover:-translate-y-1 hover:shadow-xl transition-all duration-300`).

3. **Role-Adaptive Operational Experience:**  
   - **Customer:** Visual, mouth-watering dish cards, thumb-friendly mobile floating cart drawer, live status pill.
   - **Cashier & Staff:** Rapid table occupancy board, high-contrast modal forms, instant settlement panel.
   - **Kitchen (KDS):** High-contrast Kanban & Batch views, color-coded overdue thresholds, sound alerts.
   - **Manager & Admin:** Sleek analytics dashboards, Recharts with gradient areas, AI forecast insights, structured data tables with interactive pagination.

4. **Universal Light & Dark Mode Harmony:**  
   Dark mode is designed as a deep luxury midnight workspace (`#0B0F17` canvas with `#131B2E` / `#1E293B` cards), not an accidental color inversion. All text passes WCAG 2.1 AA contrast standards.

---

## 2. Color System & Tokens

### 2.1 Brand Tokens (Evondev Modern Palette)

| Token | Light Value | Dark Value | Purpose |
|---|---|---|---|
| `--color-brand-primary` | `#6366F1` (Indigo 500) | `#818CF8` (Indigo 400) | Main brand identity, primary CTA, active navigation, key charts |
| `--color-brand-primary-hover` | `#4F46E5` (Indigo 600) | `#A5B4FC` (Indigo 300) | Primary button hover state |
| `--color-brand-secondary` | `#8B5CF6` (Violet 500) | `#A78BFA` (Violet 400) | Secondary accent, category highlights, badge accents |
| `--color-brand-surface` | `#F8FAFC` (Slate 50) | `#0B0F17` (Deep Midnight) | App surface canvas foundation |
| `--color-brand-surface-card` | `#FFFFFF` (Pure White) | `#131B2E` / `#1E293B` | Elevation card background |
| `--color-brand-surface-card-hover` | `#F1F5F9` (Slate 100) | `#1E293B` (Slate 800) | Card interactive hover state |
| `--color-brand-border` | `#E2E8F0` (Slate 200) | `#1E293B` (Slate 800) | Divider lines, card borders, table row borders |

### 2.2 Typography Colors

| Level | Light Mode | Dark Mode | Usage |
|---|---|---|---|
| **Text Primary** | `#0F172A` (`slate-900`) | `#F8FAFC` (`slate-50`) | Headings, large KPI numbers, active titles |
| **Text Secondary** | `#475569` (`slate-600`) | `#94A3B8` (`slate-400`) | Body descriptions, table content, subtitle |
| **Text Muted** | `#94A3B8` (`slate-400`) | `#64748B` (`slate-500`) | Timestamps, empty state placeholders, borders |

### 2.3 Semantic Status System

| Semantic State | Light Background | Light Text / Icon | Dark Background | Dark Text / Icon | Border |
|---|---|---|---|---|---|
| **Success** (Completed, Paid, Available) | `#ECFDF5` (`emerald-50`) | `#047857` (`emerald-700`) | `rgba(16, 185, 129, 0.12)` | `#34D399` (`emerald-400`) | `emerald-200/60` |
| **Warning / Overdue** (Pending, Low Stock) | `#FFFBEB` (`amber-50`) | `#B45309` (`amber-700`) | `rgba(245, 158, 11, 0.12)` | `#FBBF24` (`amber-400`) | `amber-200/60` |
| **Danger / Cancelled** (Urgent, Error) | `#FEF2F2` (`rose-50`) | `#B91C1C` (`rose-700`) | `rgba(239, 68, 68, 0.12)` | `#F87171` (`rose-400`) | `rose-200/60` |
| **Info / Tech** (Takeaway, Online, QR) | `#EFF6FF` (`blue-50`) | `#1D4ED8` (`blue-700`) | `rgba(59, 130, 246, 0.12)` | `#60A5FA` (`blue-400`) | `blue-200/60` |
| **Indigo / Brand** (Features, AI Badges) | `#EEF2FF` (`indigo-50`) | `#4338CA` (`indigo-700`) | `rgba(99, 102, 241, 0.12)` | `#818CF8` (`indigo-400`) | `indigo-200/60` |

---

## 3. Typography Scale (Be Vietnam Pro)

Primary Typeface: **Be Vietnam Pro** (Google Fonts: 400 Regular, 500 Medium, 600 SemiBold, 700 Bold, 800 ExtraBold, 900 Black).  
Secondary Typeface: **Inter** (Data numbers, code, labels).

| Level | Size | Weight | Tracking | Usage |
|---|---|---|---|---|
| **Display Hero** | `2.25rem` (36px) | 900 Black | `-0.03em` | Hero landing titles, welcome greetings |
| **Page Title** | `1.75rem` - `1.875rem` (28-30px) | 800 ExtraBold | `-0.025em` | Admin page headers, main dashboard title |
| **Section Heading** | `1.25rem` (20px) | 700 Bold | `-0.015em` | Card section titles, modal headers |
| **Card Heading** | `1rem` (16px) | 700 Bold | `0em` | Table numbers, dish names, user card names |
| **Body Standard** | `0.875rem` (14px) | 500 Medium | `0em` | Form labels, table cells, descriptions |
| **Body Small** | `0.75rem` (12px) | 500 Medium | `0em` | Subtitles, help text, list item details |
| **Micro Caption / Badge** | `0.6875rem` (11px) | 800 ExtraBold | `0.05em` | Status tags, table header columns, pills |
| **Financial / Metric** | `1.5rem` - `2rem` (24-32px) | 800-900 ExtraBold | `font-mono` / tabular | Price tags, revenue totals, cash calculation |

---

## 4. Spacing, Shape & Elevation

### 4.1 Spacing Scale
- `xs` (8px): Micro icon gaps, button padding-y
- `sm` (12px): List item padding, card internal gaps
- `md` (16px): Standard card padding, modal gutters
- `lg` (24px): Page container gutters, section spacing
- `xl` (32px): Dashboard grid gaps

### 4.2 Border Radius System (Evondev Smooth Curves)
- `rounded-lg` (8px): Mini badges, micro buttons
- `rounded-xl` (12px): Standard form inputs, dropdown selectors, secondary buttons
- `rounded-2xl` (16px): List item containers, dish cards, modal content boxes
- `rounded-3xl` (24px): Primary dashboard cards, charts, panels, tables container
- `rounded-full` (9999px): Status pills, avatar circles, live indicator dots

### 4.3 Elevation & Drop Shadows
- **Card Default:** `shadow-sm border border-slate-200/80 dark:border-slate-800/80`
- **Card Interactive Hover:** `hover:shadow-xl hover:-translate-y-1 transition-all duration-300`
- **Floating Accent / CTAs:** `shadow-lg shadow-indigo-500/25` (Electric Indigo glow), `shadow-lg shadow-emerald-500/25` (Emerald glow)
- **Glassmorphic Top Bar:** `bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800/80`

---

## 5. Component Patterns & Guidelines

### 5.1 KPI Metric Cards
```jsx
<div className="group rounded-3xl bg-white dark:bg-slate-900/90 border border-slate-200/80 dark:border-slate-800/80 p-6 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300">
  <div className="flex items-start justify-between">
    <div>
      <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">Tiêu đề</p>
      <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">{value}</h3>
    </div>
    <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white flex items-center justify-center shadow-lg shadow-indigo-500/25 group-hover:scale-110 transition-transform duration-300">
      {icon}
    </div>
  </div>
  <div className="mt-4 flex items-center gap-2 pt-3 border-t border-slate-100 dark:border-slate-800/60 text-xs text-slate-500">
    <span className="font-semibold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">Badge</span>
    <span>Mô tả phụ</span>
  </div>
</div>
```

### 5.2 Status Pills & Badges
```jsx
// Success
<span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-emerald-200/60 dark:border-emerald-800/40">
  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
  Hoàn thành
</span>
```

### 5.3 Modern Charts (Area & Donut)
- **AreaChart:** Single or dual curve with `<linearGradient>` under the stroke, smooth `strokeWidth={3}`, clean horizontal grid line opacity `0.5`, custom glassmorphism Tooltip (`backdrop-blur-md rounded-2xl shadow-xl`).
- **Donut Chart:** `innerRadius={62}`, `outerRadius={82}`, `paddingAngle={4}`, `cornerRadius={6}`, with center total metric label and custom tag breakdown below.

### 5.4 Data Tables
- Header: `bg-slate-50/75 dark:bg-slate-950/50 text-slate-500 dark:text-slate-400 text-xs uppercase font-bold tracking-wider`.
- Row hover: `hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors`.
- Rank pills: Gold / Silver / Bronze gradient pills for Top 3, rounded number badges for others.
- Progress bar: Gradient fills (`from-indigo-500 to-purple-500`) with proportional percentage.
- Pagination: Sleek chevron buttons with clean state styling.
