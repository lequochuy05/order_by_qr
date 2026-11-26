import assert from 'node:assert/strict';
import { createServer } from 'vite';
import { dirname, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(testDir, '../..');

test('invoice escapes user-controlled values before generating printable HTML', async (t) => {
  const previousWindow = Object.getOwnPropertyDescriptor(globalThis, 'window');
  const previousDocument = Object.getOwnPropertyDescriptor(globalThis, 'document');
  Object.defineProperty(globalThis, 'window', {
    configurable: true,
    value: { location: { href: 'http://localhost/' } },
  });
  Object.defineProperty(globalThis, 'document', { configurable: true, value: {} });
  t.after(() => {
    if (previousWindow) Object.defineProperty(globalThis, 'window', previousWindow);
    else delete globalThis.window;
    if (previousDocument) Object.defineProperty(globalThis, 'document', previousDocument);
    else delete globalThis.document;
  });

  const vite = await createServer({
    configFile: resolve(frontendRoot, 'vite.config.js'),
    root: frontendRoot,
    optimizeDeps: { noDiscovery: true },
    server: { middlewareMode: true },
    appType: 'custom',
  });
  t.after(() => vite.close());

  const { generateInvoice } = await vite.ssrLoadModule('/src/entities/order/lib/invoiceGenerator.js');
  const { useSettingsStore } = await vite.ssrLoadModule('/src/entities/settings/index.js');
  const previousSettings = useSettingsStore.getState().settings;
  t.after(() => useSettingsStore.setState({ settings: previousSettings }));
  useSettingsStore.setState({
    settings: {
      ...previousSettings,
      restaurantName: '<img src=x onerror=alert(1)>',
      wifiName: 'Guest <script>alert(1)</script>',
    },
  });

  const html = generateInvoice({
    order: {
      id: 'ORD-<script>alert(1)</script>',
      orderItems: [
        {
          itemNameSnapshot: '<svg onload=alert(1)>',
          notes: '<img src=x onerror=alert(1)>',
          options: [{ optionName: '<b>', optionValueName: '"><script>alert(1)</script>' }],
          quantity: 1,
          unitPrice: 1000,
          lineTotal: 1000,
        },
      ],
      subtotalAmount: 100000,
      discountAmount: 20000,
      serviceChargePercent: 5,
      serviceChargeAmount: 4000,
      taxPercent: 10,
      taxAmount: 8400,
      finalAmount: 92400,
    },
    table: { tableNumber: '<iframe src=javascript:alert(1)>' },
    paidBy: '<script>alert(1)</script>',
    settings: useSettingsStore.getState().settings,
  });

  assert.match(html, /&lt;img src=x onerror=alert\(1\)&gt;/);
  assert.match(html, /&lt;svg onload=alert\(1\)&gt;/);
  assert.match(html, /&lt;iframe src=javascript:alert\(1\)&gt;/);
  assert.match(html, /&lt;script&gt;alert\(1\)&lt;\/script&gt;/);
  assert.match(html, /Phí dịch vụ \(5%\):/);
  assert.match(html, /Thuế VAT \(10%\):/);
  assert.doesNotMatch(html, /<script>alert\(1\)<\/script>/);
  assert.doesNotMatch(html, /<img src=x onerror=alert\(1\)>/);
});
