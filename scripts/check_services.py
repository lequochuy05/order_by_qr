import urllib.request
import json
import socket
import sys

def check_port(host, port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(2)
    try:
        s.connect((host, port))
        s.close()
        return True
    except Exception:
        return False

def check_http(url):
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "HealthCheck/1.0"})
        with urllib.request.urlopen(req, timeout=3) as resp:
            return resp.status, resp.read().decode('utf-8')[:200]
    except Exception as e:
        return None, str(e)

print("=== CHECKING SERVICES FOR PHASE 1 ===")

# 1. Redis
redis_ok = check_port("localhost", 6379)
print(f"Redis (port 6379): {'UP' if redis_ok else 'DOWN'}")

# 2. Backend
backend_port_ok = check_port("localhost", 8080)
print(f"Backend (port 8080): {'UP' if backend_port_ok else 'DOWN'}")
status, body = check_http("http://localhost:8080/actuator/health")
print(f"Backend Actuator Health: HTTP {status} -> {body}")

# 3. Frontend
frontend_port_ok = check_port("localhost", 5173)
print(f"Frontend (port 5173): {'UP' if frontend_port_ok else 'DOWN'}")
status, body = check_http("http://localhost:5173/")
print(f"Frontend HTTP: HTTP {status}")

# 4. Playwright check
try:
    import subprocess
    res = subprocess.run(["node", "-e", "console.log(require('playwright') ? 'OK' : 'ERR')"], capture_output=True, text=True)
    print(f"Playwright: {res.stdout.strip()}")
except Exception as e:
    print(f"Playwright check err: {e}")
