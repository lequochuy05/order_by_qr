# Stage 1: Build frontend
FROM node:20-alpine AS builder
WORKDIR /app

# Cache dependencies (dùng npm ci để cài đặt nhanh và chính xác hơn)
COPY package*.json ./
RUN npm ci --quiet

# Nhận các biến môi trường build-time cho Vite
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

# Copy source và build
COPY . .
RUN npm run build

# Stage 2: Serve với Nginx (Runtime)
FROM nginx:stable-alpine

# Copy file cấu hình Nginx tối ưu cho SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy kết quả build từ stage trước
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
