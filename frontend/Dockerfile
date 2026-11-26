# Stage 1: Build frontend
FROM node:22-alpine AS builder
WORKDIR /app

# Cache dependencies (dùng npm ci để cài đặt nhanh và chính xác hơn)
COPY package*.json ./
RUN npm ci --quiet

# Nhận các biến môi trường build-time cho Vite
ARG VITE_API_URL
ARG VITE_WS_URL
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL

# Copy source và build
COPY . .
RUN npm run build

# Stage 2: Serve với Nginx unprivileged (Runtime — non-root port 8080)
FROM nginxinc/nginx-unprivileged:stable-alpine

# Copy file cấu hình Nginx tối ưu cho SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy kết quả build từ stage trước
COPY --from=builder /app/dist /usr/share/nginx/html

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
