#!/bin/bash
set -e

PROJECT_ROOT="$(pwd)"
LOGS_DIR="$PROJECT_ROOT/artifacts/ui-audit/logs"
mkdir -p "$LOGS_DIR"

echo "=== 1. Checking Redis Container ==="
if ! docker ps | grep -q "qros-redis"; then
    echo "Starting redis container..."
    docker start qros-redis 2>/dev/null || docker run -d --name qros-redis -p 6379:6379 redis:7-alpine
fi
echo "Redis is running."

echo "=== 2. Compiling Backend ==="
mvn -f backend/pom.xml compile -DskipTests

echo "=== 3. Starting Backend ==="
# Kill any existing process on 8080
fuser -k 8080/tcp 2>/dev/null || true

nohup mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev > "$LOGS_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "Backend started with PID $BACKEND_PID, waiting for health check..."

HEALTH_READY=0
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
        echo "Backend is UP and HEALTHY!"
        HEALTH_READY=1
        break
    fi
    sleep 2
done

if [ $HEALTH_READY -eq 0 ]; then
    echo "ERROR: Backend failed to start within 120s. Check $LOGS_DIR/backend.log"
    tail -n 50 "$LOGS_DIR/backend.log"
    kill $BACKEND_PID 2>/dev/null || true
    exit 1
fi

echo "=== 4. Starting Frontend ==="
# Kill any existing process on 5173
fuser -k 5173/tcp 2>/dev/null || true

nohup npm --prefix frontend run dev -- --host 0.0.0.0 > "$LOGS_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "Frontend started with PID $FRONTEND_PID, waiting for server..."

FRONTEND_READY=0
for i in {1..30}; do
    if curl -s -I http://localhost:5173 | grep -q "200 OK"; then
        echo "Frontend is UP and ACCESSIBLE!"
        FRONTEND_READY=1
        break
    fi
    sleep 1
done

if [ $FRONTEND_READY -eq 0 ]; then
    echo "ERROR: Frontend failed to start within 30s. Check $LOGS_DIR/frontend.log"
    tail -n 50 "$LOGS_DIR/frontend.log"
    kill $FRONTEND_PID 2>/dev/null || true
    kill $BACKEND_PID 2>/dev/null || true
    exit 1
fi

echo "=== 5. Installing Playwright & Chromium ==="
if [ ! -d "node_modules/playwright" ] && [ ! -d "frontend/node_modules/playwright" ]; then
    echo "Installing playwright in project..."
    npm install --no-save playwright
fi

echo "Installing Playwright Chromium browser..."
npx playwright install chromium

echo "=== 6. Running UI Audit Script ==="
node scripts/audit_playwright.js

echo "=== 7. Audit Completed! ==="
echo "Screenshots and logs generated."
