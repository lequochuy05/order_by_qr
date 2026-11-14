import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'http://localhost:5173';
const BACKEND_URL = 'http://localhost:8080';
const ARTIFACTS_DIR = path.resolve('artifacts/ui-audit');
const SCREENSHOTS_DIR = path.join(ARTIFACTS_DIR, 'screenshots');
const LOGS_DIR = path.join(ARTIFACTS_DIR, 'logs');

// Ensure dirs
fs.mkdirSync(LOGS_DIR, { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'customer/desktop'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'customer/mobile'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'customer/interactions'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'staff/desktop'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'staff/mobile'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'staff/interactions'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'chef/desktop'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'chef/mobile'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'chef/interactions'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'manager/desktop'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'manager/mobile'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'manager/interactions'), { recursive: true });
fs.mkdirSync(path.join(SCREENSHOTS_DIR, 'notifications'), { recursive: true });

const browserErrorsStream = fs.createWriteStream(path.join(LOGS_DIR, 'browser-errors.log'), { flags: 'a' });
const networkErrorsStream = fs.createWriteStream(path.join(LOGS_DIR, 'network-errors.log'), { flags: 'a' });

function attachLoggers(page) {
  page.on('console', msg => {
    if (msg.type() === 'error') {
      const logLine = `[BROWSER ERROR] [${new Date().toISOString()}] ${msg.text()}\n`;
      browserErrorsStream.write(logLine);
    }
  });

  page.on('pageerror', err => {
    browserErrorsStream.write(`[PAGE CRASH] [${new Date().toISOString()}] ${err.stack || err.message}\n`);
  });

  page.on('requestfailed', request => {
    const failure = request.failure();
    const logLine = `[NETWORK FAILED] [${new Date().toISOString()}] ${request.method()} ${request.url()} - ${failure?.errorText}\n`;
    networkErrorsStream.write(logLine);
  });

  page.on('response', response => {
    if (response.status() >= 400 && !response.url().includes('/api/v1/auth/login')) {
      networkErrorsStream.write(`[HTTP ${response.status()}] [${new Date().toISOString()}] ${response.request().method()} ${response.url()}\n`);
    }
  });
}

async function capture(page, filePath, options = {}) {
  await page.waitForTimeout(600); // let animations settle
  await page.screenshot({ path: filePath, fullPage: options.fullPage !== false });
  console.log(`📸 Saved: ${path.relative(process.cwd(), filePath)}`);
}

async function runAudit() {
  console.log('🚀 Starting Full UI Audit with Playwright...');
  const browser = await chromium.launch({ headless: true });

  const desktopContext = await browser.newContext({
    viewport: { width: 1440, height: 900 }
  });

  const mobileContext = await browser.newContext({
    viewport: { width: 390, height: 844 },
    isMobile: true,
    hasTouch: true
  });

  // ==========================================
  // PHASE 1: PUBLIC, LANDING, AUTH & ERRORS
  // ==========================================
  console.log('\n--- 1. Testing Public & Guest Flow ---');
  const dPage = await desktopContext.newPage();
  attachLoggers(dPage);

  // Landing Page
  await dPage.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/desktop/CUSTOMER_LANDING_DEFAULT_DESKTOP_001.png'));

  const mPage = await mobileContext.newPage();
  attachLoggers(mPage);
  await mPage.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
  await capture(mPage, path.join(SCREENSHOTS_DIR, 'customer/mobile/CUSTOMER_LANDING_DEFAULT_MOBILE_002.png'));

  // Login Page
  await dPage.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/desktop/CUSTOMER_LOGIN_DEFAULT_DESKTOP_001.png'));
  await mPage.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
  await capture(mPage, path.join(SCREENSHOTS_DIR, 'customer/mobile/CUSTOMER_LOGIN_DEFAULT_MOBILE_002.png'));

  // Test empty validation on login
  await dPage.click('button[type="submit"]');
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_AUTH_EMPTY_FIELDS_002.png'), { fullPage: false });

  // Test wrong credentials on login
  await dPage.fill('input[type="email"]', 'wrong@example.com');
  await dPage.fill('input[type="password"]', 'WrongPassword123!');
  await dPage.click('button[type="submit"]');
  await dPage.waitForTimeout(1000);
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_AUTH_LOGIN_FAILED_001.png'), { fullPage: false });

  // Forgot Password
  await dPage.goto(`${BASE_URL}/forgot-password`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/desktop/CUSTOMER_FORGOT_PASSWORD_DEFAULT_DESKTOP_001.png'));
  await mPage.goto(`${BASE_URL}/forgot-password`, { waitUntil: 'networkidle' });
  await capture(mPage, path.join(SCREENSHOTS_DIR, 'customer/mobile/CUSTOMER_FORGOT_PASSWORD_DEFAULT_MOBILE_002.png'));

  // Reset Password Page
  await dPage.goto(`${BASE_URL}/reset-password?token=sample-test-token`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/desktop/CUSTOMER_RESET_PASSWORD_DEFAULT_DESKTOP_001.png'));
  await mPage.goto(`${BASE_URL}/reset-password?token=sample-test-token`, { waitUntil: 'networkidle' });
  await capture(mPage, path.join(SCREENSHOTS_DIR, 'customer/mobile/CUSTOMER_RESET_PASSWORD_DEFAULT_MOBILE_002.png'));

  // System status pages
  await dPage.goto(`${BASE_URL}/unauthorized`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_SYS_403_UNAUTHORIZED_035.png'));

  await dPage.goto(`${BASE_URL}/not-found`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_SYS_404_NOT_FOUND_036.png'));

  await dPage.goto(`${BASE_URL}/offline`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_SYS_OFFLINE_037.png'));

  await dPage.goto(`${BASE_URL}/error/500`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_SYS_500_ERROR_038.png'));

  // ==========================================
  // PHASE 2: CUSTOMER ORDERING FLOW
  // ==========================================
  console.log('\n--- 2. Testing Customer Ordering Flow ---');
  // Customer menu with table code T01
  await dPage.goto(`${BASE_URL}/T01/menu`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/desktop/CUSTOMER_MENU_DEFAULT_DESKTOP_001.png'));

  await mPage.goto(`${BASE_URL}/T01/menu`, { waitUntil: 'networkidle' });
  await capture(mPage, path.join(SCREENSHOTS_DIR, 'customer/mobile/CUSTOMER_MENU_DEFAULT_MOBILE_002.png'));

  // Customer public menu without tableCode
  await dPage.goto(`${BASE_URL}/menu`, { waitUntil: 'networkidle' });
  await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/desktop/CUSTOMER_PUBLIC_MENU_DESKTOP_008.png'));

  // Back to /T01/menu to test interactions: click dish, open options modal, cart
  await dPage.goto(`${BASE_URL}/T01/menu`, { waitUntil: 'networkidle' });
  try {
    const dishCard = dPage.locator('.cursor-pointer, [data-testid="dish-card"], button:has-text("+"), .group').first();
    if (await dishCard.isVisible()) {
      await dishCard.click();
      await dPage.waitForTimeout(600);
      await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/interactions/CUSTOMER_ITEM_OPTIONS_MODAL_DESKTOP_003.png'), { fullPage: false });

      // Try to add to cart
      const addToCartBtn = dPage.locator('button:has-text("Thêm vào giỏ"), button:has-text("Xác nhận")').first();
      if (await addToCartBtn.isVisible()) {
        await addToCartBtn.click();
        await dPage.waitForTimeout(600);
      }
    }
  } catch (e) {
    console.log('Item modal interaction note:', e.message);
  }

  // Open Cart
  try {
    const cartButton = dPage.locator('button:has-text("Giỏ hàng"), button:has-text("Xem giỏ hàng"), [aria-label*="giỏ"]').first();
    if (await cartButton.isVisible()) {
      await cartButton.click();
      await dPage.waitForTimeout(600);
      await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/interactions/CUSTOMER_CART_MODAL_DESKTOP_004.png'), { fullPage: false });
    }
  } catch (e) {
    console.log('Cart modal interaction note:', e.message);
  }

  // Mobile cart interaction
  try {
    const mCartButton = mPage.locator('button:has-text("Giỏ hàng"), button:has-text("Xem giỏ hàng"), [aria-label*="giỏ"]').first();
    if (await mCartButton.isVisible()) {
      await mCartButton.click();
      await mPage.waitForTimeout(600);
      await capture(mPage, path.join(SCREENSHOTS_DIR, 'customer/interactions/CUSTOMER_CART_MODAL_MOBILE_005.png'), { fullPage: false });
    }
  } catch (e) {}

  // AI Chat Assistant
  try {
    const aiBtn = dPage.locator('button:has-text("AI"), [aria-label*="AI"], button:has(.lucide-sparkles), button:has-text("Trợ lý")').first();
    if (await aiBtn.isVisible()) {
      await aiBtn.click();
      await dPage.waitForTimeout(600);
      await capture(dPage, path.join(SCREENSHOTS_DIR, 'customer/interactions/CUSTOMER_AI_ASSISTANT_DRAWER_DESKTOP_007.png'), { fullPage: false });
    }
  } catch (e) {}

  await dPage.close();
  await mPage.close();

  // Helper login function for roles
  async function loginAsRole(email, password) {
    const roleCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await roleCtx.newPage();
    attachLoggers(page);

    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await page.waitForTimeout(1500);

    return { ctx: roleCtx, page };
  }

  // ==========================================
  // PHASE 3: STAFF ROLE
  // ==========================================
  console.log('\n--- 3. Testing STAFF Role ---');
  try {
    const { ctx: staffCtx, page: staffPage } = await loginAsRole('staff@gmail.com', 'staff123');

    // Tables Map
    await staffPage.goto(`${BASE_URL}/admin/tables`, { waitUntil: 'networkidle' });
    await capture(staffPage, path.join(SCREENSHOTS_DIR, 'staff/desktop/STAFF_TABLE_MAP_DEFAULT_DESKTOP_001.png'));

    // Mobile view of table map
    const staffMobile = await staffCtx.newPage();
    attachLoggers(staffMobile);
    await staffMobile.setViewportSize({ width: 390, height: 844 });
    await staffMobile.goto(`${BASE_URL}/admin/tables`, { waitUntil: 'networkidle' });
    await capture(staffMobile, path.join(SCREENSHOTS_DIR, 'staff/mobile/STAFF_TABLE_MAP_DEFAULT_MOBILE_002.png'));
    await staffMobile.close();

    // Table modals
    // Merge table modal
    try {
      const mergeBtn = staffPage.locator('button[title*="Gộp bàn"], button[aria-label*="Gộp bàn"]').first();
      if (await mergeBtn.isVisible()) {
        await mergeBtn.click();
        await staffPage.waitForTimeout(600);
        await capture(staffPage, path.join(SCREENSHOTS_DIR, 'staff/interactions/STAFF_MERGE_TABLE_MODAL_DESKTOP_006.png'), { fullPage: false });
        // Close modal
        await staffPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Takeaway Page
    await staffPage.goto(`${BASE_URL}/admin/takeaway`, { waitUntil: 'networkidle' });
    await capture(staffPage, path.join(SCREENSHOTS_DIR, 'staff/desktop/STAFF_TAKEAWAY_DEFAULT_DESKTOP_007.png'));

    // Takeaway create modal
    try {
      const createOrderBtn = staffPage.locator('button:has-text("Tạo đơn mới")').first();
      if (await createOrderBtn.isVisible()) {
        await createOrderBtn.click();
        await staffPage.waitForTimeout(600);
        await capture(staffPage, path.join(SCREENSHOTS_DIR, 'staff/interactions/STAFF_TAKEAWAY_CREATE_MODAL_DESKTOP_009.png'), { fullPage: false });
        await staffPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Profile Page
    await staffPage.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
    await capture(staffPage, path.join(SCREENSHOTS_DIR, 'staff/desktop/STAFF_PROFILE_DEFAULT_DESKTOP_010.png'));

    // Settings (Staff preferences)
    await staffPage.goto(`${BASE_URL}/admin/settings`, { waitUntil: 'networkidle' });
    await capture(staffPage, path.join(SCREENSHOTS_DIR, 'staff/desktop/STAFF_SETTINGS_PREFERENCES_DESKTOP_011.png'));

    await staffCtx.close();
  } catch (err) {
    console.error('STAFF audit error:', err.message);
  }

  // ==========================================
  // PHASE 4: CHEF ROLE
  // ==========================================
  console.log('\n--- 4. Testing CHEF Role ---');
  try {
    const { ctx: chefCtx, page: chefPage } = await loginAsRole('chef@gmail.com', 'chef123');

    // Kitchen Board
    await chefPage.goto(`${BASE_URL}/admin/kitchen`, { waitUntil: 'networkidle' });
    await capture(chefPage, path.join(SCREENSHOTS_DIR, 'chef/desktop/CHEF_KITCHEN_BOARD_DEFAULT_DESKTOP_001.png'));

    // Mobile Kitchen Board
    const chefMobile = await chefCtx.newPage();
    attachLoggers(chefMobile);
    await chefMobile.setViewportSize({ width: 390, height: 844 });
    await chefMobile.goto(`${BASE_URL}/admin/kitchen`, { waitUntil: 'networkidle' });
    await capture(chefMobile, path.join(SCREENSHOTS_DIR, 'chef/mobile/CHEF_KITCHEN_BOARD_DEFAULT_MOBILE_002.png'));
    await chefMobile.close();

    // AI Staff Panel on Kitchen Board
    try {
      const aiPanel = chefPage.locator('.ai-staff-panel, div:has-text("Trợ lý bếp")').first();
      if (await aiPanel.isVisible()) {
        await capture(chefPage, path.join(SCREENSHOTS_DIR, 'chef/interactions/CHEF_AI_STAFF_PANEL_DESKTOP_003.png'), { fullPage: false });
      }
    } catch (e) {}

    // Inventory Management
    await chefPage.goto(`${BASE_URL}/admin/inventory`, { waitUntil: 'networkidle' });
    await capture(chefPage, path.join(SCREENSHOTS_DIR, 'chef/desktop/CHEF_INVENTORY_DEFAULT_DESKTOP_004.png'));

    // Stock in / Adjust modal
    try {
      const stockBtn = chefPage.locator('button:has-text("Nhập kho"), button:has-text("Điều chỉnh"), button[title*="Nhập"]').first();
      if (await stockBtn.isVisible()) {
        await stockBtn.click();
        await chefPage.waitForTimeout(600);
        await capture(chefPage, path.join(SCREENSHOTS_DIR, 'chef/interactions/CHEF_STOCK_MODAL_DESKTOP_006.png'), { fullPage: false });
        await chefPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Recipe modal
    try {
      const recipeBtn = chefPage.locator('button:has-text("Định lượng"), button:has-text("Công thức"), button[title*="công thức"]').first();
      if (await recipeBtn.isVisible()) {
        await recipeBtn.click();
        await chefPage.waitForTimeout(600);
        await capture(chefPage, path.join(SCREENSHOTS_DIR, 'chef/interactions/CHEF_RECIPE_MODAL_DESKTOP_007.png'), { fullPage: false });
        await chefPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Profile Page
    await chefPage.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
    await capture(chefPage, path.join(SCREENSHOTS_DIR, 'chef/desktop/CHEF_PROFILE_DEFAULT_DESKTOP_008.png'));

    await chefCtx.close();
  } catch (err) {
    console.error('CHEF audit error:', err.message);
  }

  // ==========================================
  // PHASE 5: MANAGER ROLE
  // ==========================================
  console.log('\n--- 5. Testing MANAGER Role ---');
  try {
    const { ctx: mgrCtx, page: mgrPage } = await loginAsRole('manager@gmail.com', 'manager123');

    // Dashboard Overview
    await mgrPage.goto(`${BASE_URL}/admin/dashboard`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_DASHBOARD_DEFAULT_DESKTOP_001.png'));

    // Mobile Dashboard
    const mgrMobile = await mgrCtx.newPage();
    attachLoggers(mgrMobile);
    await mgrMobile.setViewportSize({ width: 390, height: 844 });
    await mgrMobile.goto(`${BASE_URL}/admin/dashboard`, { waitUntil: 'networkidle' });
    await capture(mgrMobile, path.join(SCREENSHOTS_DIR, 'manager/mobile/MANAGER_DASHBOARD_DEFAULT_MOBILE_002.png'));
    await mgrMobile.close();

    // Categories Management
    await mgrPage.goto(`${BASE_URL}/admin/categories`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_CATEGORY_LIST_DEFAULT_DESKTOP_003.png'));

    try {
      const addCateBtn = mgrPage.locator('button:has-text("Thêm danh mục"), button:has-text("Thêm mới")').first();
      if (await addCateBtn.isVisible()) {
        await addCateBtn.click();
        await mgrPage.waitForTimeout(600);
        await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/interactions/MANAGER_CATEGORY_MODAL_DESKTOP_004.png'), { fullPage: false });
        await mgrPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Menu Items Management
    await mgrPage.goto(`${BASE_URL}/admin/menu`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_MENU_LIST_DEFAULT_DESKTOP_005.png'));

    try {
      const addMenuBtn = mgrPage.locator('button:has-text("Thêm món"), button:has-text("Thêm mới")').first();
      if (await addMenuBtn.isVisible()) {
        await addMenuBtn.click();
        await mgrPage.waitForTimeout(600);
        await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/interactions/MANAGER_MENU_MODAL_DESKTOP_006.png'), { fullPage: false });
        await mgrPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Combo Management
    await mgrPage.goto(`${BASE_URL}/admin/combo`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_COMBO_LIST_DEFAULT_DESKTOP_007.png'));

    try {
      const addComboBtn = mgrPage.locator('button:has-text("Thêm combo"), button:has-text("Thêm mới")').first();
      if (await addComboBtn.isVisible()) {
        await addComboBtn.click();
        await mgrPage.waitForTimeout(600);
        await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/interactions/MANAGER_COMBO_MODAL_DESKTOP_008.png'), { fullPage: false });
        await mgrPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Voucher Management
    await mgrPage.goto(`${BASE_URL}/admin/voucher`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_VOUCHER_LIST_DEFAULT_DESKTOP_009.png'));

    try {
      const addVoucherBtn = mgrPage.locator('button:has-text("Thêm voucher"), button:has-text("Thêm mã"), button:has-text("Tạo mới")').first();
      if (await addVoucherBtn.isVisible()) {
        await addVoucherBtn.click();
        await mgrPage.waitForTimeout(600);
        await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/interactions/MANAGER_VOUCHER_MODAL_DESKTOP_010.png'), { fullPage: false });
        await mgrPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Staff / User Management
    await mgrPage.goto(`${BASE_URL}/admin/staffs`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_STAFF_LIST_DEFAULT_DESKTOP_011.png'));

    try {
      const addUserBtn = mgrPage.locator('button:has-text("Thêm nhân viên"), button:has-text("Thêm mới")').first();
      if (await addUserBtn.isVisible()) {
        await addUserBtn.click();
        await mgrPage.waitForTimeout(600);
        await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/interactions/MANAGER_USER_MODAL_DESKTOP_012.png'), { fullPage: false });
        await mgrPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Statistics Top Dishes
    await mgrPage.goto(`${BASE_URL}/admin/statistics/top-dishes`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_TOP_DISHES_STATS_DESKTOP_013.png'));

    // Statistics Staff
    await mgrPage.goto(`${BASE_URL}/admin/statistics/staff`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_STAFF_STATS_DESKTOP_014.png'));

    // Table Map as Manager (with Add Table button)
    await mgrPage.goto(`${BASE_URL}/admin/tables`, { waitUntil: 'networkidle' });
    try {
      const addTableBtn = mgrPage.locator('button:has-text("Thêm bàn"), button:has-text("Thêm mới")').first();
      if (await addTableBtn.isVisible()) {
        await addTableBtn.click();
        await mgrPage.waitForTimeout(600);
        await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/interactions/MANAGER_TABLE_FORM_MODAL_DESKTOP_015.png'), { fullPage: false });
        await mgrPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Order History as Manager
    await mgrPage.goto(`${BASE_URL}/admin/history`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_ORDER_HISTORY_DEFAULT_DESKTOP_017.png'));

    // Order detail modal in history
    try {
      const orderRow = mgrPage.locator('table tbody tr, .cursor-pointer').first();
      if (await orderRow.isVisible()) {
        await orderRow.click();
        await mgrPage.waitForTimeout(600);
        await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/interactions/MANAGER_ORDER_DETAILS_MODAL_DESKTOP_018.png'), { fullPage: false });
        await mgrPage.keyboard.press('Escape');
      }
    } catch (e) {}

    // Profile Page
    await mgrPage.goto(`${BASE_URL}/admin/profile`, { waitUntil: 'networkidle' });
    await capture(mgrPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/MANAGER_PROFILE_DEFAULT_DESKTOP_019.png'));

    await mgrCtx.close();
  } catch (err) {
    console.error('MANAGER audit error:', err.message);
  }

  // ==========================================
  // PHASE 6: ADMIN ROLE (WITH 2FA)
  // ==========================================
  console.log('\n--- 6. Testing ADMIN Role (2FA & Full Settings) ---');
  try {
    const adminCtx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const adminPage = await adminCtx.newPage();
    attachLoggers(adminPage);

    await adminPage.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await adminPage.fill('input[type="email"]', 'admin@gmail.com');
    await adminPage.fill('input[type="password"]', 'admin123');
    await adminPage.click('button[type="submit"]');
    await adminPage.waitForTimeout(1500);

    // Capture 2FA OTP prompt modal/screen
    await capture(adminPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_AUTH_2FA_PROMPT_003.png'), { fullPage: false });

    // Read OTP from Redis first, then backend log
    let otp = null;
    try {
      const { execSync } = await import('child_process');
      const redisKeys = execSync('docker exec qros-redis redis-cli --raw keys "qros:2fa:ticket:*"', { encoding: 'utf8' }).trim();
      if (redisKeys) {
        const keyList = redisKeys.split('\n');
        for (const k of keyList) {
          const key = k.trim();
          if (key) {
            const rawVal = execSync(`docker exec qros-redis redis-cli --raw get "${key}"`, { encoding: 'utf8' }).trim();
            try {
              const parsed = JSON.parse(rawVal);
              if (parsed && parsed.otp) {
                otp = parsed.otp;
                console.log(`🔑 Found Admin 2FA OTP from Redis key ${key}: ${otp}`);
                break;
              }
            } catch (err) {}
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
          console.log(`🔑 Found Admin 2FA OTP from log: ${otp}`);
        }
      } catch (e) {
        console.log('Could not parse backend.log for OTP:', e.message);
      }
    }

    if (otp) {
      await adminPage.fill('input[pattern="[0-9]*"], input[placeholder="••••••"]', otp);
      await adminPage.click('button:has-text("XÁC NHẬN ĐĂNG NHẬP")');
      await adminPage.waitForTimeout(1500);
      await capture(adminPage, path.join(SCREENSHOTS_DIR, 'notifications/NOTIF_AUTH_2FA_SUCCESS_004.png'), { fullPage: false });

      // Revenue Stats (ADMIN ONLY)
      await adminPage.goto(`${BASE_URL}/admin/statistics/revenue`, { waitUntil: 'networkidle' });
      await capture(adminPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/ADMIN_REVENUE_STATS_DEFAULT_DESKTOP_002.png'));

      const adminMobile = await adminCtx.newPage();
      attachLoggers(adminMobile);
      await adminMobile.setViewportSize({ width: 390, height: 844 });
      await adminMobile.goto(`${BASE_URL}/admin/statistics/revenue`, { waitUntil: 'networkidle' });
      await capture(adminMobile, path.join(SCREENSHOTS_DIR, 'manager/mobile/ADMIN_REVENUE_STATS_DEFAULT_MOBILE_003.png'));
      await adminMobile.close();

      // Full Settings (ADMIN ONLY)
      await adminPage.goto(`${BASE_URL}/admin/settings`, { waitUntil: 'networkidle' });
      await capture(adminPage, path.join(SCREENSHOTS_DIR, 'manager/desktop/ADMIN_SETTINGS_FULL_DESKTOP_004.png'));

      // Click each tab in Settings
      const tabs = [
        { id: 'payment', name: 'ADMIN_SETTINGS_PAYMENT_TAB_DESKTOP_005.png' },
        { id: 'customerMenu', name: 'ADMIN_SETTINGS_CUSTOMER_MENU_TAB_DESKTOP_006.png' },
        { id: 'operation', name: 'ADMIN_SETTINGS_OPERATION_TAB_DESKTOP_007.png' },
        { id: 'bill', name: 'ADMIN_SETTINGS_BILL_TAB_DESKTOP_008.png' },
        { id: 'notifications', name: 'ADMIN_SETTINGS_NOTIFICATIONS_TAB_DESKTOP_009.png' },
        { id: 'wifi', name: 'ADMIN_SETTINGS_WIFI_TAB_DESKTOP_010.png' },
        { id: 'preferences', name: 'ADMIN_SETTINGS_PREFERENCES_TAB_DESKTOP_011.png' },
      ];

      for (const tab of tabs) {
        try {
          const tabBtn = adminPage.locator(`button[id*="${tab.id}"], button:has-text("${tab.id}"), nav button`).filter({ hasText: new RegExp(tab.id, 'i') }).first();
          if (await tabBtn.isVisible()) {
            await tabBtn.click();
            await adminPage.waitForTimeout(500);
            await capture(adminPage, path.join(SCREENSHOTS_DIR, `manager/interactions/${tab.name}`));
          }
        } catch (e) {}
      }
    }

    await adminCtx.close();
  } catch (err) {
    console.error('ADMIN audit error:', err.message);
  }

  await browser.close();
  browserErrorsStream.end();
  networkErrorsStream.end();
  console.log('\n🎉 Audit and screenshot capture completed successfully!');
}

runAudit().catch(err => {
  console.error('Fatal audit failure:', err);
  process.exit(1);
});
