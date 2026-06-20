#!/bin/bash
# ============================================================
# Deploy script cho QROS — Order by QR
# Hỗ trợ 3 mode:
#   1. Full Docker (Render) — backend + frontend cùng image
#   2. Hybrid (nhanh nhất) — frontend lên Vercel, backend Docker
#   3. Native (tiết kiệm RAM nhất) — GraalVM native image
# ============================================================
set -euo pipefail

MODE="${1:-hybrid}"
DOCKER_TAG="${2:-latest}"

echo "🚀 Deploy mode: $MODE"

case "$MODE" in
  hybrid)
    echo ""
    echo "═══ Hybrid Deploy ═══"
    echo "  Frontend → Vercel (nhanh, free)"
    echo "  Backend  → Render (Docker)"
    echo ""

    # Backend: Build Docker và push lên Render
    echo "📦 Building backend Docker image..."
    docker build -t order-by-qr-backend:$DOCKER_TAG -f Dockerfile .
    echo "✅ Backend image built: order-by-qr-backend:$DOCKER_TAG"

    echo ""
    echo "📤 Push lên container registry (tuỳ chọn):"
    echo "   docker tag order-by-qr-backend:$DOCKER_TAG ghcr.io/lequochuy05/order-by-qr-backend:$DOCKER_TAG"
    echo "   docker push ghcr.io/lequochuy05/order-by-qr-backend:$DOCKER_TAG"
    echo ""
    echo "🌐 Deploy frontend lên Vercel:"
    echo "   npx vercel --prod"
    echo "   Hoặc: git push (Vercel auto-deploy)"
    ;;

  docker)
    echo ""
    echo "═══ Full Docker Deploy ═══"
    echo "  Backend + Frontend → Docker Compose"
    echo ""

    # Build backend
    echo "📦 Building backend..."
    docker build -t order-by-qr-backend:$DOCKER_TAG -f Dockerfile .
    echo "✅ Backend built"

    # Build frontend
    echo "📦 Building frontend..."
    cd frontend
    docker build \
      -t order-by-qr-frontend:$DOCKER_TAG \
      --build-arg VITE_API_URL="${VITE_API_URL:-}" \
      --build-arg VITE_WS_URL="${VITE_WS_URL:-}" \
      -f Dockerfile .
    cd ..
    echo "✅ Frontend built"

    # Run
    echo ""
    echo "▶️  Chạy với docker compose:"
    echo "   docker compose up -d"
    ;;

  native)
    echo ""
    echo "═══ Native Deploy ═══"
    echo "  GraalVM Native Image — RAM ~30MB, startup < 1s"
    echo "  ⚠️  Lần đầu build mất 5-10 phút"
    echo ""

    # Build native image
    echo "📦 Building native image (mất nhiều thời gian)..."
    cd backend
    mvn -Pnative native:compile -DskipTests -B
    cd ..
    echo "✅ Native binary built: backend/target/qros-backend"

    # Build Docker image
    echo "📦 Building native Docker image..."
    docker build -t order-by-qr-backend-native:$DOCKER_TAG -f backend/Dockerfile.native .
    echo "✅ Native Docker image built: order-by-qr-backend-native:$DOCKER_TAG"

    echo ""
    echo "▶️  Run:"
    echo "   docker run -p 8080:8080 --env-file .env order-by-qr-backend-native:$DOCKER_TAG"
    ;;

  *)
    echo "Usage: $0 {hybrid|docker|native} [tag]"
    echo ""
    echo "  hybrid  (mặc định) Frontend → Vercel, Backend → Docker (khuyên dùng)"
    echo "  docker  Full Docker Compose (truyền thống)"
    echo "  native  GraalVM native image (tiết kiệm RAM nhất)"
    exit 1
    ;;
esac

echo ""
echo "✅ Done!"
