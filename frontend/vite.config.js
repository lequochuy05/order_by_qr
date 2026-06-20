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
        navigateFallbackDenylist: [/^\/api\//, /^\/ws\//],
        runtimeCaching: [
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
        name: 'Order by QR - Hệ thống gọi món thông minh',
        short_name: 'Order by QR',
        description: 'Hệ thống gọi món bằng mã QR cho nhà hàng',
        theme_color: '#EA580C',
        background_color: '#FAFAFA',
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
      '@modules': path.resolve(__dirname, './src/features'),
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
          if (id.includes('/lucide-react/') || id.includes('/react-icons/')) return 'vendor-icons';
          if (id.includes('/qrcode.react/')) return 'vendor-qrcode';
          if (id.includes('/axios/') || id.includes('/zustand/')) return 'vendor-data';
          return undefined;
        },
      },
    },
  },
  server: {
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
