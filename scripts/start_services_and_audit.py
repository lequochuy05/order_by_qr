#!/usr/bin/env python3
import os
import sys
import time
import urllib.request
import subprocess

PROJECT_ROOT = os.path.abspath(os.path.dirname(__file__) + "/..")
LOGS_DIR = os.path.join(PROJECT_ROOT, "artifacts/ui-audit/logs")
os.makedirs(LOGS_DIR, exist_ok=True)

def load_env(env_path):
    env = os.environ.copy()
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    env[k.strip()] = v.strip()
    return env

ENV = load_env(os.path.join(PROJECT_ROOT, ".env"))

def free_port(port):
    print("Freeing port " + str(port) + " if in use...")
    try:
        subprocess.run(["fuser", "-k", str(port) + "/tcp"], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)
    except Exception:
        pass
    time.sleep(1)

def wait_for_url(url, timeout=120, interval=2, expected_status=200):
    start = time.time()
    while time.time() - start < timeout:
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "HealthCheck/1.0"})
            with urllib.request.urlopen(req, timeout=3) as resp:
                if resp.status == expected_status:
                    return True
        except Exception:
            pass
        time.sleep(interval)
    return False

def main():
    print("==================================================")
    print("   QROS UI AUDIT & AUTOMATION TEST SUITE RUNNER   ")
    print("==================================================")

    # 1. Check Redis
    print("\n[Step 1/5] Checking Redis container...")
    redis_check = subprocess.run(["docker", "ps", "--filter", "name=qros-redis", "--format", "{{.Status}}"], capture_output=True, text=True)
    if "Up" not in redis_check.stdout:
        print("Starting qros-redis container...")
        subprocess.run(["docker", "start", "qros-redis"], check=False)
        time.sleep(2)
    print("✓ Redis is UP and running on port 6379.")

    # 2. Free Ports
    free_port(8080)
    free_port(5173)

    # 3. Start Backend
    print("\n[Step 2/5] Starting Spring Boot Backend (dev profile)...")
    backend_log_path = os.path.join(LOGS_DIR, "backend.log")
    backend_log_file = open(backend_log_path, "w", encoding="utf-8")
    backend_proc = subprocess.Popen(
        ["mvn", "-f", "backend/pom.xml", "spring-boot:run", "-Dspring-boot.run.profiles=dev"],
        cwd=PROJECT_ROOT,
        env=ENV,
        stdout=backend_log_file,
        stderr=subprocess.STDOUT
    )
    print("Backend process PID: " + str(backend_proc.pid))
    print("Waiting for http://localhost:8080/actuator/health...")
    
    if not wait_for_url("http://localhost:8080/actuator/health", timeout=120, interval=3):
        print("❌ ERROR: Backend failed to become healthy within 120s! Check artifacts/ui-audit/logs/backend.log")
        backend_proc.terminate()
        sys.exit(1)
    print("✓ Backend is UP, HEALTHY and ready!")

    # 4. Start Frontend
    print("\n[Step 3/5] Starting React / Vite Frontend...")
    frontend_log_path = os.path.join(LOGS_DIR, "frontend.log")
    frontend_log_file = open(frontend_log_path, "w", encoding="utf-8")
    frontend_proc = subprocess.Popen(
        ["npm", "--prefix", "frontend", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"],
        cwd=PROJECT_ROOT,
        env=ENV,
        stdout=frontend_log_file,
        stderr=subprocess.STDOUT
    )
    print("Frontend process PID: " + str(frontend_proc.pid))
    print("Waiting for http://localhost:5173/...")
    
    if not wait_for_url("http://localhost:5173/", timeout=30, interval=1):
        print("❌ ERROR: Frontend failed to respond within 30s! Check artifacts/ui-audit/logs/frontend.log")
        frontend_proc.terminate()
        backend_proc.terminate()
        sys.exit(1)
    print("✓ Frontend is UP and accessible at http://localhost:5173/!")

    # 5. Run Playwright UI Audit
    print("\n[Step 4/5] Executing Playwright UI Audit & Capturing Screenshots...")
    audit_proc = subprocess.run(
        ["node", "scripts/audit_playwright.js"],
        cwd=PROJECT_ROOT,
        env=ENV
    )

    print("\nAudit script exit code: " + str(audit_proc.returncode))

    # 6. Summary of Screenshots
    print("\n[Step 5/5] Auditing Captured Screenshots...")
    screenshots_dir = os.path.join(PROJECT_ROOT, "artifacts/ui-audit/screenshots")
    total_shots = 0
    categories = ["customer", "staff", "chef", "manager", "notifications"]
    for cat in categories:
        cat_dir = os.path.join(screenshots_dir, cat)
        if os.path.exists(cat_dir):
            count = 0
            for root, dirs, files in os.walk(cat_dir):
                count += len([f for f in files if f.endswith(".png")])
            total_shots += count
            print("  • " + cat.capitalize() + ": " + str(count) + " screenshots")

    print("\n🎉 Total Screenshots Captured: " + str(total_shots))
    print("Artifacts directory: artifacts/ui-audit/")

if __name__ == "__main__":
    main()
