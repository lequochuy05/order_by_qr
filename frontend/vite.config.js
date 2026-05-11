import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  define: {
    global: 'window',
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