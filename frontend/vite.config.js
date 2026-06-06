import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@app': path.resolve(__dirname, './src/app'),
      '@pages': path.resolve(__dirname, './src/pages'),
      '@widgets': path.resolve(__dirname, './src/widgets'),
      '@modules': path.resolve(__dirname, './src/modules'),
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
          if (!id.includes('node_modules')) return undefined
          if (id.includes('/@tensorflow/')) return 'vendor-tensorflow'
          if (
            id.includes('/react/') ||
            id.includes('/react-dom/') ||
            id.includes('/react-router/') ||
            id.includes('/react-router-dom/') ||
            id.includes('/scheduler/') ||
            id.includes('/use-sync-external-store/')
          ) return 'vendor-react'
          if (id.includes('/lucide-react/') || id.includes('/react-icons/')) return 'vendor-icons'
          if (id.includes('/qrcode.react/')) return 'vendor-qrcode'
          if (id.includes('/axios/') || id.includes('/zustand/')) return 'vendor-data'
          return undefined
        },
      },
    },
  },
  server: {
    proxy: {
      // Bắt các request API đẩy sang Spring Boot
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Bắt các request WebSocket đẩy sang Spring Boot
      '/ws': {
        target: 'http://localhost:8080',
        ws: true, // Quan trọng: Bật chế độ proxy cho WebSocket
      }
    }
  }
})
