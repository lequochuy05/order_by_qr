import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { VitePWA } from 'vite-plugin-pwa';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const CACHEABLE_PUBLIC_API_PATHS = new Set([
  '/api/v1/public/catalog',
  '/api/v1/public/settings',
]);

const isCacheablePublicApiGetRequest = ({ request, url }) =>
  request.method === 'GET' &&
  (CACHEABLE_PUBLIC_API_PATHS.has(url.pathname) ||
    url.pathname.startsWith('/api/v1/recommendations/')) &&
  !url.searchParams.has('sessionToken');

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'pwa-*.png'],
      workbox: {
        cleanupOutdatedCaches: true,
        clientsClaim: true,
        skipWaiting: true,
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        globIgnores: [
          '**/vendor-charts-*.js',
          '**/AdminLayout-*.js',
          '**/*Management-*.js',
          '**/CategoryPage-*.js',
          '**/ComboPage-*.js',
          '**/DashboardPage-*.js',
          '**/InventoryPage-*.js',
          '**/KitchenPage-*.js',
          '**/MenuPage-*.js',
          '**/OrderHistoryPage-*.js',
          '**/ProfilePage-*.js',
          '**/PromotionPage-*.js',
          '**/RevenueStatsPage-*.js',
          '**/ServiceRequestsPage-*.js',
          '**/SettingsPage-*.js',
          '**/StaffStatsPage-*.js',
          '**/TableMapPage-*.js',
          '**/TakeawayPage-*.js',
          '**/TopDishesStatsPage-*.js',
          '**/UserPage-*.js',
          '**/VoucherPage-*.js',
          '**/StaffAiAssistant-*.js',
          '**/AiInvoiceOcrModal-*.js',
        ],
        navigateFallbackDenylist: [/^\/api\//, /^\/ws\//],
        runtimeCaching: [
          {
            urlPattern: ({ url }) =>
              url.pathname.startsWith('/assets/') && /\.(?:js|css)$/i.test(url.pathname),
            handler: 'CacheFirst',
            options: {
              cacheName: 'lazy-assets-cache',
              cacheableResponse: {
                statuses: [0, 200],
              },
              expiration: {
                maxEntries: 120,
                maxAgeSeconds: 60 * 60 * 24 * 30,
              },
            },
          },
          {
            urlPattern: isCacheablePublicApiGetRequest,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'public-catalog-cache-v2',
              networkTimeoutSeconds: 10,
              cacheableResponse: {
                statuses: [0, 200],
              },
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60,
              },
            },
          },
          {
            urlPattern: /^https:\/\/res\.cloudinary\.com\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'cloudinary-images',
              cacheableResponse: {
                statuses: [0, 200],
              },
              expiration: {
                maxEntries: 200,
                maxAgeSeconds: 60 * 60 * 24 * 30,
              },
            },
          },
          {
            urlPattern: /^https:\/\/ui-avatars\.com\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'ui-avatars',
              cacheableResponse: {
                statuses: [0, 200],
              },
              expiration: {
                maxEntries: 50,
                maxAgeSeconds: 60 * 60 * 24 * 30,
              },
            },
          },
        ],
      },
      manifest: {
        name: 'QROS — Smart QR Order & Restaurant Operating System',
        short_name: 'QROS',
        description: 'Hệ thống đặt món qua mã QR và quản lý vận hành nhà hàng thông minh',
        theme_color: '#6366F1',
        background_color: '#F8FAFC',
        display: 'standalone',
        orientation: 'portrait-primary',
        scope: '/',
        start_url: '/',
        lang: 'vi',
        icons: [
          {
            src: '/pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: '/pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
          },
          {
            src: '/pwa-512x512-maskable.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@app': path.resolve(__dirname, './src/app'),
      '@pages': path.resolve(__dirname, './src/pages'),
      '@widgets': path.resolve(__dirname, './src/widgets'),
      '@features': path.resolve(__dirname, './src/features'),
      '@entities': path.resolve(__dirname, './src/entities'),
      '@shared': path.resolve(__dirname, './src/shared'),
    },
  },
  define: {
    global: 'window',
  },
  build: {
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined;
          if (
            id.includes('/react/') ||
            id.includes('/react-dom/') ||
            id.includes('/react-router/') ||
            id.includes('/react-router-dom/') ||
            id.includes('/scheduler/') ||
            id.includes('/use-sync-external-store/')
          )
            return 'vendor-react';
          if (id.includes('/@tanstack/')) return 'vendor-query';
          if (id.includes('/@stomp/') || id.includes('/sockjs-client/')) return 'vendor-realtime';
          if (
            id.includes('/recharts/') ||
            id.includes('/d3-') ||
            id.includes('/victory-vendor/') ||
            id.includes('/react-smooth/')
          )
            return 'vendor-charts';
          if (id.includes('/lucide-react/') || id.includes('/react-icons/')) return 'vendor-icons';
          if (id.includes('/qrcode.react/')) return 'vendor-qrcode';
          if (id.includes('/axios/') || id.includes('/zustand/')) return 'vendor-data';
          return undefined;
        },
      },
    },
  },
  server: {
    headers: {
      'Permissions-Policy': 'fullscreen=(self), unload=(self)',
    },
    proxy: {
      // Bắt các request API đẩy sang Spring Boot
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Bắt các request WebSocket đẩy sang Spring Boot
      '/ws': {
        target: 'http://localhost:8080',
        ws: true, // Quan trọng: Bật chế độ proxy cho WebSocket
      },
    },
  },
});
