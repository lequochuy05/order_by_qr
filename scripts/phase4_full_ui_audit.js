import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';
const ROOT_DIR = process.cwd();
const SCREENSHOTS_ROOT = path.join(ROOT_DIR, 'screenshots');
const ARTIFACTS_DIR = path.join(ROOT_DIR, 'artifacts/ui-audit');
const LOGS_DIR = path.join(ARTIFACTS_DIR, 'logs');

// Viewport specifications
const VIEWPORTS = {
  desktop: { width: 1440, height: 900, isMobile: false, hasTouch: false },
  tablet: { width: 768, height: 1024, isMobile: true, hasTouch: true },
  mobile: { width: 390, height: 844, isMobile: true, hasTouch: true }
};

// Ensure directories
const ROLES = ['customer', 'staff', 'chef', 'cashier', 'manager', 'admin', 'shared'];
const DEVICES = ['desktop', 'tablet', 'mobile'];
const SUB_DIRS = ['pages', 'modals', 'forms', 'widgets', 'features'];

fs.mkdirSync(LOGS_DIR, { recursive: true });
fs.mkdirSync(SCREENSHOTS_ROOT, { recursive: true });

for (const role of ROLES) {
  for (const device of DEVICES) {
    for (const sub of SUB_DIRS) {
      fs.mkdirSync(path.join(SCREENSHOTS_ROOT, role, device, sub), { recursive: true });
    }
  }
}

const browserErrorsStream = fs.createWriteStream(path.join(LOGS_DIR, 'browser-errors.log'), { flags: 'a' });
const networkErrorsStream = fs.createWriteStream(path.join(LOGS_DIR, 'network-errors.log'), { flags: 'a' });

const auditMetrics = {
  totalScreenshots: 0,
  byRole: {},
  byDevice: { desktop: 0, tablet: 0, mobile: 0 },
  capturedItems: [],
  browserErrors: [],
  networkErrors: []
};

ROLES.forEach(r => { auditMetrics.byRole[r] = 0; });

function attachLoggers(page, roleContext = 'unknown') {
  page.on('console', msg => {
    if (msg.type() === 'error') {
      const errText = `[CONSOLE_ERROR] [${new Date().toISOString()}] [${roleContext}] ${msg.text()}`;
      browserErrorsStream.write(errText + '\n');
      auditMetrics.browserErrors.push({ timestamp: new Date().toISOString(), role: roleContext, text: msg.text() });
    }
  });

  page.on('pageerror', err => {
    const errText = `[PAGE_CRASH] [${new Date().toISOString()}] [${roleContext}] ${err.stack || err.message}`;
    browserErrorsStream.write(errText + '\n');
    auditMetrics.browserErrors.push({ timestamp: new Date().toISOString(), role: roleContext, text: err.message, stack: err.stack });
  });

  page.on('requestfailed', request => {
    const failure = request.failure();
    const errText = `[REQ_FAILED] [${new Date().toISOString()}] [${roleContext}] ${request.method()} ${request.url()} - ${failure?.errorText}`;
    networkErrorsStream.write(errText + '\n');
    auditMetrics.networkErrors.push({ timestamp: new Date().toISOString(), role: roleContext, url: request.url(), method: request.method(), error: failure?.errorText });
  });

  page.on('response', response => {
    if (response.status() >= 400 && !response.url().includes('/api/v1/auth/login')) {
      const errText = `[HTTP_${response.status()}] [${new Date().toISOString()}] [${roleContext}] ${response.request().method()} ${response.url()}`;
      networkErrorsStream.write(errText + '\n');
      auditMetrics.networkErrors.push({ timestamp: new Date().toISOString(), role: roleContext, url: response.url(), status: response.status() });
    }
  });
}

async function capture(page, { role, device, category = 'pages', name, state = 'default', fullPage = true }) {
  await page.waitForTimeout(600); // let CSS animations & fonts settle
  const filename = `${role}_${device}_${category}_${name}_${state}.png`.toLowerCase();
  const targetDir = path.join(SCREENSHOTS_ROOT, role, device, category);
  const filePath = path.join(targetDir, filename);

  await page.screenshot({ path: filePath, fullPage });
  auditMetrics.totalScreenshots++;
  auditMetrics.byRole[role] = (auditMetrics.byRole[role] || 0) + 1;
  auditMetrics.byDevice[device] = (auditMetrics.byDevice[device] || 0) + 1;
  auditMetrics.capturedItems.push({
    role,
    device,
    category,
    name,
    state,
    filename,
    relativePath: path.relative(ROOT_DIR, filePath)
  });
  console.log(`📸 [${auditMetrics.totalScreenshots}] Saved: ${path.relative(ROOT_DIR, filePath)}`);
}

async function safeClick(page, selector, timeout = 2500) {
  try {
    const el = page.locator(selector).first();
    await el.waitFor({ state: 'visible', timeout });
    await el.click();
    return true;
  } catch {
    return false;
  }
}

async function runPhase4Audit() {
  console.log('===============================================================');
  console.log('🚀 STARTING PHASE 4: FULL ROLE-BY-ROLE PLAYWRIGHT UI QA AUDIT');
  console.log('===============================================================');

  const browser = await chromium.launch({ headless: true });

  // -------------------------------------------------------------
  // HELPER: Login as role and return page
  // -------------------------------------------------------------
  async function createRoleSession(role, email, password, device = 'desktop') {
    const vp = VIEWPORTS[device];
    const ctx = await browser.newContext({
      viewport: { width: vp.width, height: vp.height },
      isMobile: vp.isMobile,
      hasTouch: vp.hasTouch
    });
    const page = await ctx.newPage();
    attachLoggers(page, role);

    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await page.waitForTimeout(1500);

    return { ctx, page };
  }

  // =============================================================
  // 1. PUBLIC, SHARED & CUSTOMER FLOW
  // =============================================================
  console.log('\n🔵 [ROLE 1/6] Auditing CUSTOMER & Public Flow...');
  for (const device of ['desktop', 'tablet', 'mobile']) {
    const vp = VIEWPORTS[device];
    const ctx = await browser.newContext({
      viewport: { width: vp.width, height: vp.height },
      isMobile: vp.isMobile,
      hasTouch: vp.hasTouch
    });
    const page = await ctx.newPage();
    attachLoggers(page, 'customer');

    // 1.1 Landing page
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
    await capture(page, { role: 'customer', device, category: 'pages', name: 'landing', state: 'default' });

    // 1.2 System status & error pages (Shared)
    if (device === 'desktop') {
      await page.goto(`${BASE_URL}/unauthorized`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'shared', device, category: 'pages', name: 'unauthorized-403', state: 'default' });

      await page.goto(`${BASE_URL}/not-found`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'shared', device, category: 'pages', name: 'not-found-404', state: 'default' });

      await page.goto(`${BASE_URL}/offline`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'shared', device, category: 'pages', name: 'offline-status', state: 'default' });

      await page.goto(`${BASE_URL}/error/500`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'shared', device, category: 'pages', name: 'server-error-500', state: 'default' });
    }

    // 1.3 Auth screens
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await capture(page, { role: 'shared', device, category: 'forms', name: 'login', state: 'empty' });

    if (device === 'desktop') {
      // Test invalid login validation
      await page.click('button[type="submit"]');
      await capture(page, { role: 'shared', device, category: 'widgets', name: 'login-validation', state: 'empty-fields', fullPage: false });

      await page.fill('input[type="email"]', 'wrong@example.com');
      await page.fill('input[type="password"]', 'WrongPassword123!');
      await page.click('button[type="submit"]');
      await page.waitForTimeout(800);
      await capture(page, { role: 'shared', device, category: 'widgets', name: 'login-failed-toast', state: 'invalid-credentials', fullPage: false });
    }

    // Forgot password
    await page.goto(`${BASE_URL}/forgot-password`, { waitUntil: 'networkidle' });
    await capture(page, { role: 'shared', device, category: 'forms', name: 'forgot-password', state: 'default' });

    // Reset password
    await page.goto(`${BASE_URL}/reset-password?token=sample-test-token`, { waitUntil: 'networkidle' });
    await capture(page, { role: 'shared', device, category: 'forms', name: 'reset-password', state: 'default' });

    // 1.4 Customer Menu (Table T01)
    await page.goto(`${BASE_URL}/T01/menu`, { waitUntil: 'networkidle' });
    await capture(page, { role: 'customer', device, category: 'pages', name: 'menu-table-t01', state: 'loaded' });

    // Public Menu without table
    await page.goto(`${BASE_URL}/menu`, { waitUntil: 'networkidle' });
    await capture(page, { role: 'customer', device, category: 'pages', name: 'menu-public-catalog', state: 'loaded' });

    // Return to /T01/menu to test modals
    await page.goto(`${BASE_URL}/T01/menu`, { waitUntil: 'networkidle' });

    // Item Options Modal
    const openedOptions = await safeClick(page, '.cursor-pointer, [data-testid="dish-card"], button:has-text("+"), .group');
    if (openedOptions) {
      await page.waitForTimeout(600);
      await capture(page, { role: 'customer', device, category: 'modals', name: 'item-options', state: 'opened', fullPage: false });

      // Add to cart
      await safeClick(page, 'button:has-text("Thêm vào giỏ"), button:has-text("Xác nhận")');
      await page.waitForTimeout(600);
    }

    // Open Cart Modal
    const openedCart = await safeClick(page, 'button:has-text("Giỏ hàng"), button:has-text("Xem giỏ hàng"), [aria-label*="giỏ"]');
    if (openedCart) {
      await page.waitForTimeout(600);
      await capture(page, { role: 'customer', device, category: 'modals', name: 'cart', state: 'with-items', fullPage: false });
      await page.keyboard.press('Escape');
    }

    // AI Assistant Drawer
    const openedAi = await safeClick(page, 'button:has-text("AI"), [aria-label*="AI"], button:has(.lucide-sparkles), button:has-text("Trợ lý")');
    if (openedAi) {
      await page.waitForTimeout(600);
      await capture(page, { role: 'customer', device, category: 'widgets', name: 'ai-assistant-drawer', state: 'opened', fullPage: false });
      await page.keyboard.press('Escape');
    }

    await ctx.close();
  }

  // =============================================================
  // 2. STAFF ROLE
  // =============================================================
  console.log('\n🟢 [ROLE 2/6] Auditing STAFF Role...');
  for (const device of ['desktop', 'tablet', 'mobile']) {
    try {
      const { ctx, page } = await createRoleSession('staff', 'staff@gmail.com', 'staff123', device);

      // 2.1 Table Map
      await page.goto(`${BASE_URL}/admin/tables`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'staff', device, category: 'pages', name: 'table-map', state: 'default' });

      // Merge table modal
      const openedMerge = await safeClick(page, 'button[title*="Gộp bàn"], button[aria-label*="Gộp bàn"]');
      if (openedMerge) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'staff', device, category: 'modals', name: 'merge-table', state: 'opened', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 2.2 Takeaway
      await page.goto(`${BASE_URL}/admin/takeaway`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'staff', device, category: 'pages', name: 'takeaway-list', state: 'default' });

      const openedTakeaway = await safeClick(page, 'button:has-text("Tạo đơn mới")');
      if (openedTakeaway) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'staff', device, category: 'modals', name: 'takeaway-create', state: 'opened', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 2.3 Profile & Settings
      await page.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'staff', device, category: 'pages', name: 'profile', state: 'default' });

      await page.goto(`${BASE_URL}/admin/settings`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'staff', device, category: 'pages', name: 'settings-preferences', state: 'default' });

      await ctx.close();
    } catch (err) {
      console.error(`Staff audit error on ${device}:`, err.message);
    }
  }

  // =============================================================
  // 3. CHEF ROLE
  // =============================================================
  console.log('\n🟠 [ROLE 3/6] Auditing CHEF Role...');
  for (const device of ['desktop', 'tablet', 'mobile']) {
    try {
      const { ctx, page } = await createRoleSession('chef', 'chef@gmail.com', 'chef123', device);

      // 3.1 Kitchen Board
      await page.goto(`${BASE_URL}/admin/kitchen`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'chef', device, category: 'pages', name: 'kitchen-board-kanban', state: 'default' });

      // Batch cooking view toggle
      const toggledBatch = await safeClick(page, 'button:has-text("Gom món"), button:has-text("Nấu gộp")');
      if (toggledBatch) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'chef', device, category: 'features', name: 'kitchen-batch-cooking', state: 'active' });
      }

      // AI Staff Panel
      const hasAiPanel = await page.locator('.ai-staff-panel, div:has-text("Trợ lý bếp")').first().isVisible().catch(() => false);
      if (hasAiPanel) {
        await capture(page, { role: 'chef', device, category: 'widgets', name: 'ai-staff-panel', state: 'visible', fullPage: false });
      }

      // 3.2 Inventory Management
      await page.goto(`${BASE_URL}/admin/inventory`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'chef', device, category: 'pages', name: 'inventory-stock-list', state: 'default' });

      // Stock adjustment modal
      const openedStock = await safeClick(page, 'button:has-text("Nhập kho"), button:has-text("Điều chỉnh"), button[title*="Nhập"]');
      if (openedStock) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'chef', device, category: 'modals', name: 'stock-adjust', state: 'opened', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // Recipe modal
      const openedRecipe = await safeClick(page, 'button:has-text("Định lượng"), button:has-text("Công thức"), button[title*="công thức"]');
      if (openedRecipe) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'chef', device, category: 'modals', name: 'recipe-spec', state: 'opened', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // Profile
      await page.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'chef', device, category: 'pages', name: 'profile', state: 'default' });

      await ctx.close();
    } catch (err) {
      console.error(`Chef audit error on ${device}:`, err.message);
    }
  }

  // =============================================================
  // 4. CASHIER ROLE
  // =============================================================
  console.log('\n🟣 [ROLE 4/6] Auditing CASHIER Role...');
  for (const device of ['desktop', 'tablet', 'mobile']) {
    try {
      const { ctx, page } = await createRoleSession('cashier', 'cashier@gmail.com', 'cashier123', device);

      // 4.1 Tables Map & Payment
      await page.goto(`${BASE_URL}/admin/tables`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'cashier', device, category: 'pages', name: 'table-map', state: 'default' });

      // Try opening payment modal on a table
      const openedPay = await safeClick(page, 'button:has-text("Thanh toán"), button[title*="Thanh toán"], .bg-amber-500, .bg-emerald-500');
      if (openedPay) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'cashier', device, category: 'modals', name: 'payment-modal', state: 'opened', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 4.2 Order History
      await page.goto(`${BASE_URL}/admin/history`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'cashier', device, category: 'pages', name: 'order-history', state: 'default' });

      // 4.3 Takeaway & Profile
      await page.goto(`${BASE_URL}/admin/takeaway`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'cashier', device, category: 'pages', name: 'takeaway', state: 'default' });

      await page.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'cashier', device, category: 'pages', name: 'profile', state: 'default' });

      await ctx.close();
    } catch (err) {
      console.error(`Cashier audit error on ${device}:`, err.message);
    }
  }

  // =============================================================
  // 5. MANAGER ROLE
  // =============================================================
  console.log('\n🟡 [ROLE 5/6] Auditing MANAGER Role...');
  for (const device of ['desktop', 'tablet', 'mobile']) {
    try {
      const { ctx, page } = await createRoleSession('manager', 'manager@gmail.com', 'manager123', device);

      // 5.1 Dashboard Overview
      await page.goto(`${BASE_URL}/admin/dashboard`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'dashboard-overview', state: 'loaded' });

      // 5.2 Categories Management
      await page.goto(`${BASE_URL}/admin/categories`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'category-management', state: 'list' });

      const openedAddCategory = await safeClick(page, 'button:has-text("Thêm danh mục"), button:has-text("Thêm mới")');
      if (openedAddCategory) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'manager', device, category: 'modals', name: 'category-form', state: 'create-modal', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 5.3 Menu Management
      await page.goto(`${BASE_URL}/admin/menu`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'menu-management', state: 'list' });

      const openedAddMenu = await safeClick(page, 'button:has-text("Thêm món"), button:has-text("Thêm mới")');
      if (openedAddMenu) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'manager', device, category: 'modals', name: 'menu-item-form', state: 'create-modal', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 5.4 Combo Management
      await page.goto(`${BASE_URL}/admin/combo`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'combo-management', state: 'list' });

      const openedAddCombo = await safeClick(page, 'button:has-text("Thêm combo"), button:has-text("Thêm mới")');
      if (openedAddCombo) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'manager', device, category: 'modals', name: 'combo-form', state: 'create-modal', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 5.5 Voucher Management
      await page.goto(`${BASE_URL}/admin/voucher`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'voucher-management', state: 'list' });

      const openedAddVoucher = await safeClick(page, 'button:has-text("Thêm voucher"), button:has-text("Thêm mã"), button:has-text("Tạo mới")');
      if (openedAddVoucher) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'manager', device, category: 'modals', name: 'voucher-form', state: 'create-modal', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 5.6 Staffs / User Management
      await page.goto(`${BASE_URL}/admin/staffs`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'staff-management', state: 'list' });

      const openedAddUser = await safeClick(page, 'button:has-text("Thêm nhân viên"), button:has-text("Thêm mới")');
      if (openedAddUser) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'manager', device, category: 'modals', name: 'user-form', state: 'create-modal', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 5.7 Statistics
      await page.goto(`${BASE_URL}/admin/statistics/top-dishes`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'statistics-top-dishes', state: 'report' });

      await page.goto(`${BASE_URL}/admin/statistics/staff`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'statistics-staff', state: 'report' });

      // 5.8 Table Map & Add Table
      await page.goto(`${BASE_URL}/admin/tables`, { waitUntil: 'networkidle' });
      const openedAddTable = await safeClick(page, 'button:has-text("Thêm bàn"), button:has-text("Thêm mới")');
      if (openedAddTable) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'manager', device, category: 'modals', name: 'table-form', state: 'create-modal', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // 5.9 Order History & Details
      await page.goto(`${BASE_URL}/admin/history`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'order-history', state: 'list' });

      const openedOrderRow = await safeClick(page, 'table tbody tr, .cursor-pointer');
      if (openedOrderRow) {
        await page.waitForTimeout(600);
        await capture(page, { role: 'manager', device, category: 'modals', name: 'order-details', state: 'opened', fullPage: false });
        await page.keyboard.press('Escape');
      }

      // Profile
      await page.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
      await capture(page, { role: 'manager', device, category: 'pages', name: 'profile', state: 'default' });

      await ctx.close();
    } catch (err) {
      console.error(`Manager audit error on ${device}:`, err.message);
    }
  }

  // =============================================================
  // 6. ADMIN ROLE (WITH 2FA OTP & FULL SETTINGS)
  // =============================================================
  console.log('\n🔴 [ROLE 6/6] Auditing ADMIN Role (2FA, Revenue Stats, System Settings)...');
  for (const device of ['desktop', 'tablet', 'mobile']) {
    try {
      const vp = VIEWPORTS[device];
      const ctx = await browser.newContext({
        viewport: { width: vp.width, height: vp.height },
        isMobile: vp.isMobile,
        hasTouch: vp.hasTouch
      });
      const page = await ctx.newPage();
      attachLoggers(page, 'admin');

      await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
      await page.fill('input[type="email"]', 'admin@gmail.com');
      await page.fill('input[type="password"]', 'admin123');
      await page.click('button[type="submit"]');
      await page.waitForTimeout(1500);

      // Capture 2FA OTP prompt modal/screen
      await capture(page, { role: 'admin', device, category: 'modals', name: '2fa-otp-prompt', state: 'opened', fullPage: false });

      // Extract 2FA OTP
      let otp = null;
      try {
        const redisKeys = execSync('docker exec qros-redis redis-cli --raw keys "qros:2fa:ticket:*"', { encoding: 'utf8' }).trim();
        if (redisKeys) {
          const keyList = redisKeys.split('\n');
          for (const k of keyList) {
            const key = k.trim();
            if (key) {
              const rawVal = execSync(`docker exec qros-redis redis-cli --raw get "${key}"`, { encoding: 'utf8' }).trim();
              try {
                const parsed = JSON.parse(rawVal);
                if (parsed?.otp) {
                  otp = parsed.otp;
                  break;
                }
              } catch {}
            }
          }
        }
      } catch (e) {
        console.log('Redis OTP query note:', e.message);
      }

      if (!otp) {
        try {
          const backendLog = fs.readFileSync(path.join(LOGS_DIR, 'backend.log'), 'utf8');
          const matches = backendLog.match(/OTP for admin account .*? is: (\d{6})/g);
          if (matches && matches.length > 0) {
            const lastMatch = matches[matches.length - 1];
            otp = lastMatch.split('is: ')[1];
          }
        } catch {}
      }

      if (otp) {
        console.log(`🔑 Verified Admin OTP: ${otp}`);
        await page.fill('input[pattern="[0-9]*"], input[placeholder="••••••"]', otp);
        await page.click('button:has-text("XÁC NHẬN ĐĂNG NHẬP")');
        await page.waitForTimeout(1500);

        await capture(page, { role: 'admin', device, category: 'widgets', name: '2fa-success-feedback', state: 'authenticated', fullPage: false });

        // 6.1 Revenue Stats (ADMIN ONLY EXCLUSIVE)
        await page.goto(`${BASE_URL}/admin/statistics/revenue`, { waitUntil: 'networkidle' });
        await capture(page, { role: 'admin', device, category: 'pages', name: 'revenue-statistics-analytics', state: 'loaded' });

        // 6.2 Full System Settings (ADMIN ONLY EXCLUSIVE)
        await page.goto(`${BASE_URL}/admin/settings`, { waitUntil: 'networkidle' });
        await capture(page, { role: 'admin', device, category: 'pages', name: 'system-settings-full', state: 'general-tab' });

        if (device === 'desktop') {
          // Tab clicks
          const settingTabs = [
            { id: 'payment', name: 'settings-payment-tab' },
            { id: 'customerMenu', name: 'settings-customer-menu-tab' },
            { id: 'operation', name: 'settings-operation-tab' },
            { id: 'bill', name: 'settings-bill-tab' },
            { id: 'notifications', name: 'settings-notifications-tab' },
            { id: 'wifi', name: 'settings-wifi-tab' },
            { id: 'preferences', name: 'settings-preferences-tab' }
          ];

          for (const tab of settingTabs) {
            const clicked = await safeClick(page, `button[id*="${tab.id}"], button:has-text("${tab.id}"), nav button:has-text("${tab.id}")`);
            if (clicked) {
              await page.waitForTimeout(400);
              await capture(page, { role: 'admin', device, category: 'features', name: tab.name, state: 'active' });
            }
          }
        }

        // 6.3 Profile
        await page.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
        await capture(page, { role: 'admin', device, category: 'pages', name: 'profile', state: 'default' });
      } else {
        console.warn('⚠️ Could not obtain Admin 2FA OTP, skipped authenticated admin exclusive routes.');
      }

      await ctx.close();
    } catch (err) {
      console.error(`Admin audit error on ${device}:`, err.message);
    }
  }

  await browser.close();
  browserErrorsStream.end();
  networkErrorsStream.end();

  // Write summary metrics
  fs.writeFileSync(path.join(ARTIFACTS_DIR, 'audit_summary.json'), JSON.stringify(auditMetrics, null, 2), 'utf8');

  console.log('\n===============================================================');
  console.log('🎉 PHASE 4 COMPLETED SUCCESSFULLY!');
  console.log(`📊 Total Screenshots Captured: ${auditMetrics.totalScreenshots}`);
  console.log(`📱 Devices: Desktop: ${auditMetrics.byDevice.desktop} | Tablet: ${auditMetrics.byDevice.tablet} | Mobile: ${auditMetrics.byDevice.mobile}`);
  console.log(`👥 Roles:`, auditMetrics.byRole);
  console.log(`⚠️ Logged Browser Errors: ${auditMetrics.browserErrors.length}`);
  console.log(`🌐 Logged Network Failures: ${auditMetrics.networkErrors.length}`);
  console.log('===============================================================');
}

runPhase4Audit().catch(err => {
  console.error('Fatal audit failure:', err);
  process.exit(1);
});
