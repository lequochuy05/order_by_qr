//resource/static/admin/js/common.js
window.role = localStorage.getItem("role");
const role = window.role;
const fullname = localStorage.getItem("fullname");

if (!localStorage.getItem('accessToken')) window.location.href = '/login.html';
if (fullname) {
    document.getElementById("userInfo").innerHTML = `
        <p style="color: white;">Xin chào, ${fullname}</p>
        <a href="#" style="color: #00f;" onclick="logout()">Đăng xuất</a>
    `;
}

function toggleSidebar() {
    const sidebar = document.getElementById("sidebar");
    sidebar.classList.toggle("active");
}

// Đọc lỗi an toàn (kể cả body rỗng / JSON lỗi)
async function $readErr(res) {
  try {
    const clone = res.clone();
    const ct = clone.headers.get('content-type') || '';
    const raw = await clone.text();
    if (!raw) return `${res.status} ${res.statusText}`;
    if (ct.includes('application/json')) {
      try {
        const j = JSON.parse(raw);
        return j.message || j.error || JSON.stringify(j);
      } catch { return raw; }
    }
    return raw;
  } catch {
    return `${res.status} ${res.statusText}`;
  }
}

// Fetch có kèm header mặc định (hoặc authFetch nếu đã có)
async function $fetch(url, options = {}) {
  if (typeof window.authFetch === 'function') return window.authFetch(url, options);
  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && options.body) headers.set('Content-Type', 'application/json');
  return fetch(url, { ...options, headers });
}

// Khởi động đồng hồ hiển thị giờ

  (function initHeaderClock(){
  const el = document.getElementById('headerClock');
  if (!el) return;
  const tick = ()=> el.textContent = new Date().toLocaleString('vi-VN');
  tick(); setInterval(tick, 1000);
})();

function logout() {
    localStorage.clear();
    window.location.href = "/login.html";
}
