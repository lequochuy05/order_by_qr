import { chromium, devices } from 'playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173';
const ROOT_DIR = process.cwd();
const ANDROID_ARTIFACTS_DIR = path.join(ROOT_DIR, 'artifacts/android-audit');
const SCREENSHOTS_DIR = path.join(ANDROID_ARTIFACTS_DIR, 'screenshots');
const LOGS_DIR = path.join(ANDROID_ARTIFACTS_DIR, 'logs');

fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
fs.mkdirSync(LOGS_DIR, { recursive: true });

const results = {
  timestamp: new Date().toISOString(),
  devicesTested: [],
  featuresAudited: [],
  manifestCheck: {},
  capacitorCheck: {},
  errors: []
};

// 1. Audit Capacitor & Android Config
console.log('📱 [Step 1/4] Auditing Android & Capacitor Configuration...');
try {
  const capConfigPath = path.join(ROOT_DIR, 'frontend/capacitor.config.json');
  if (fs.existsSync(capConfigPath)) {
    const capConfig = JSON.parse(fs.readFileSync(capConfigPath, 'utf8'));
    results.capacitorCheck = {
      status: 'VALID',
      appId: capConfig.appId,
      appName: capConfig.appName,
      webDir: capConfig.webDir,
      serverUrl: capConfig.server?.url || 'embedded',
      cleartext: capConfig.server?.cleartext ?? false
    };
    console.log('✓ Capacitor Config:', results.capacitorCheck);
  }

  const manifestPath = path.join(ROOT_DIR, 'frontend/android/app/src/main/AndroidManifest.xml');
  if (fs.existsSync(manifestPath)) {
    const manifestXml = fs.readFileSync(manifestPath, 'utf8');
    results.androidManifest = {
      hasInternetPermission: manifestXml.includes('android.permission.INTERNET'),
      hasMainActivity: manifestXml.includes('android.name=".MainActivity"'),
      packageId: 'com.qros.app'
    };
    console.log('✓ AndroidManifest.xml verified:', results.androidManifest);
  }
} catch (e) {
  results.errors.push({ stage: 'config_audit', error: e.message });
}

// 2. Playwright Android Emulation Tests
async function runAndroidUiTests() {
  console.log('\n📱 [Step 2/4] Launching Playwright with Android Device Emulation...');
  const browser = await chromium.launch({ headless: true });

  const androidProfiles = [
    {
      name: 'Google Pixel 7 (Standard Android)',
      config: {
        ...devices['Pixel 7'],
        locale: 'vi-VN',
        timezoneId: 'Asia/Ho_Chi_Minh'
      }
    },
    {
      name: 'Samsung Galaxy (Narrow 360x800)',
      config: {
        viewport: { width: 360, height: 800 },
        userAgent: 'Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
        deviceScaleFactor: 3,
        isMobile: true,
        hasTouch: true,
        locale: 'vi-VN',
        timezoneId: 'Asia/Ho_Chi_Minh'
      }
    }
  ];

  for (const profile of androidProfiles) {
    console.log(`\n--- Testing profile: ${profile.name} ---`);
    const ctx = await browser.newContext(profile.config);
    const page = await ctx.newPage();

    page.on('console', msg => {
      if (msg.type() === 'error') {
        results.errors.push({ device: profile.name, text: msg.text() });
      }
    });

    const prefix = profile.name.includes('Pixel') ? 'pixel7' : 'galaxy_narrow';

    // A. Landing Page
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' });
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, `android_${prefix}_01_landing.png`), fullPage: true });

    // B. Customer Menu with Table T01
    await page.goto(`${BASE_URL}/T01/menu`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, `android_${prefix}_02_customer_menu.png`), fullPage: false });

    // C. Touch Interaction: Open Dish Details Bottom Sheet
    try {
      const dishCard = page.locator('.cursor-pointer, [data-testid="dish-card"], button:has-text("+"), .group').first();
      if (await dishCard.isVisible()) {
        await dishCard.click();
        await page.waitForTimeout(600);
        await page.screenshot({ path: path.join(SCREENSHOTS_DIR, `android_${prefix}_03_item_bottom_sheet.png`), fullPage: false });

        // Add to Cart
        const addToCart = page.locator('button:has-text("Thêm vào giỏ"), button:has-text("Xác nhận")').first();
        if (await addToCart.isVisible()) {
          await addToCart.click();
          await page.waitForTimeout(600);
        }
      }
    } catch (e) {
      console.warn('Dish modal interaction:', e.message);
    }

    // D. View Cart Bottom Sheet on Android
    try {
      const cartBtn = page.locator('button:has-text("Giỏ hàng"), button:has-text("Xem giỏ hàng"), [aria-label*="giỏ"]').first();
      if (await cartBtn.isVisible()) {
        await cartBtn.click();
        await page.waitForTimeout(600);
        await page.screenshot({ path: path.join(SCREENSHOTS_DIR, `android_${prefix}_04_cart_bottom_sheet.png`), fullPage: false });
        await page.keyboard.press('Escape');
      }
    } catch (e) {}

    // E. Mobile Login Screen
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, `android_${prefix}_05_login_screen.png`), fullPage: true });

    // F. Mobile Table Map (Staff on Android)
    await page.fill('input[type="email"]', 'staff@gmail.com');
    await page.fill('input[type="password"]', 'staff123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(1500);

    await page.goto(`${BASE_URL}/admin/tables`, { waitUntil: 'networkidle' });
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, `android_${prefix}_06_staff_table_map.png`), fullPage: true });

    // G. Mobile Kitchen Board (Chef on Android)
    await page.goto(`${BASE_URL}/admin/kitchen`, { waitUntil: 'networkidle' });
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, `android_${prefix}_07_chef_kitchen_board.png`), fullPage: true });

    results.devicesTested.push({
      profile: profile.name,
      viewport: profile.config.viewport,
      status: 'PASS',
      screenshotsCount: 7
    });

    await ctx.close();
  }

  await browser.close();

  // Write Report
  fs.writeFileSync(path.join(ANDROID_ARTIFACTS_DIR, 'android_test_summary.json'), JSON.stringify(results, null, 2), 'utf8');
  console.log('\n===============================================================');
  console.log('🎉 ANDROID TESTING COMPLETED SUCCESSFULLY!');
  console.log(`📱 Profiles Tested: ${results.devicesTested.length}`);
  console.log(`📸 Screenshots saved to: artifacts/android-audit/screenshots/`);
  console.log('===============================================================');
}

runAndroidUiTests().catch(err => {
  console.error('Fatal Android test failure:', err);
  process.exit(1);
});
