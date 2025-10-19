//resource/static/admin/js/common.js
function byId(id){ return document.getElementById(id); }
function escapeHtml(s){
  return String(s || '').replace(/[&<>"']/g, m => (
    { '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' }[m]
  ));
}
function safeUrl(u){
  try {
    const url = new URL(u, location.origin);
    return ['http:','https:'].includes(url.protocol) ? url.href : u;
  } catch { return u; }
}
function showError(id, msg){
  const el = byId(id);
  if (!el) return;
  el.textContent = msg || '';
  el.style.display = msg ? 'block' : 'none';
}

// Gắn ra window nếu cần dùng toàn cục
window.byId = byId;
window.escapeHtml = escapeHtml;
window.safeUrl = safeUrl;
window.showError = showError;

// ===== Biến chung / auth =====
window.BASE_URL = window.APP_BASE_URL;
window.role = localStorage.getItem("role");
const role = window.role;
const fullname = localStorage.getItem("fullname");

// Nếu chưa đăng nhập -> chuyển về trang login
if (!localStorage.getItem('accessToken')) window.location.href = '/login.html';

// Hiển thị tên người dùng (chờ DOM sẵn sàng để chắc có #userInfo)
document.addEventListener('DOMContentLoaded', () => {
  const box = byId("userInfo");
  if (box && fullname) {
    box.innerHTML = `
      <p>
        <span>👤 ${escapeHtml(fullname)}</span>
        <a href="#" onclick="logout(); return false;">Đăng xuất</a>
      </p>
    `;
  }
});

// Toggle sidebar
function toggleSidebar() {
    const sidebar = document.getElementById("sidebar");
    if (sidebar) sidebar.classList.toggle("active");
}

// Đọc lỗi từ response
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
      } catch {
        return raw;
      }
    }
    return raw;
  } catch {
    return `${res.status} ${res.statusText}`;
  }
}
window.$readErr = $readErr;

// Fetch có kèm header mặc định 
async function $fetch(url, options = {}) {
  if (typeof window.authFetch === 'function') return window.authFetch(url, options);
  const headers = new Headers(options.headers || {});
  
  // CHỈ set application/json khi KHÔNG phải FormData
  const isForm = (options.body && typeof FormData !== 'undefined' && options.body instanceof FormData);
  if (!headers.has('Content-Type') && options.body && !isForm) {
    headers.set('Content-Type', 'application/json');
  }
  
  return fetch(url, { ...options, headers });
}
window.$fetch = $fetch;

// Định dạng số
window.fmtN = n => (Number(n) || 0).toLocaleString('vi-VN');

// Định dạng tiền VND
window.fmtVND = n => window.fmtN(n) + 'đ';

// Khởi động đồng hồ hiển thị giờ
(function initHeaderClock() {
  const el = document.getElementById('headerClock');
  if (!el) return;
  const tick = () => {
    const now = new Date();
    const time = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    const date = now.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    el.textContent = `${time} ${date}`;
  };
  tick();
  setInterval(tick, 1000);
})();

// ===== Modal thông báo dùng chung cho ADMIN =====
(function initAdminMessageModal() {
  // Nếu chưa có modal thông báo -> tự chèn
  if (!document.getElementById('msgModal')) {
    const wrap = document.createElement('div');
    wrap.innerHTML = `
      <div id="msgModal" class="modal" style="display:none;">
        <div class="modal-content modal-content--sm">
          <span class="close" id="msgModalClose" aria-label="Đóng">&times;</span>
          <h3 id="msgModalTitle" style="margin:0 0 16px; font-size:18px; font-weight:700; color:var(--text);">Thông báo</h3>
          <div id="msgModalBody" style="color:var(--text); font-size:15px; line-height:1.6; margin-bottom:20px;"></div>
          <div style="text-align:right;">
            <button class="btn primary" id="msgModalOk" style="min-width:100px;">OK</button>
          </div>
        </div>
      </div>
    `.trim();
    document.body.appendChild(wrap.firstElementChild);
  }

  const modal = document.getElementById('msgModal');
  const btnClose = document.getElementById('msgModalClose');
  const btnOk = document.getElementById('msgModalOk');
  const titleEl = document.getElementById('msgModalTitle');
  const bodyEl = document.getElementById('msgModalBody');

  function hide() {
    modal.style.display = 'none';
  }

  if (btnClose) btnClose.addEventListener('click', hide);
  if (btnOk) btnOk.addEventListener('click', hide);
  
  modal.addEventListener('click', (e) => {
    if (e.target === modal) hide();
  });
  
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.style.display === 'flex') hide();
  });

  function safeHtmlFromText(txt) {
    const s = String(txt ?? '');
    const esc = s.replace(/[&<>]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c]));
    return esc.replace(/\n/g, '<br>');
  }

  // Hàm hiển thị chung
  window.showModal = function ({ title = 'Thông báo', message = '', type = 'info', okText = 'OK' } = {}) {
    titleEl.textContent = title;
    bodyEl.innerHTML = safeHtmlFromText(message) || '—';
    btnOk.textContent = okText || 'OK';
    modal.style.display = 'flex';
    setTimeout(() => btnOk.focus(), 100);
  };

  window.showAppError = (msg, title = '⚠️ Có lỗi xảy ra') => 
    window.showModal({ title, message: msg, type: 'error', okText: 'Đã hiểu' });
  
  window.showAppInfo = (msg, title = 'ℹ️ Thông báo') => 
    window.showModal({ title, message: msg, type: 'info', okText: 'OK' });
  
  window.showAppSuccess = (msg, title = '✅ Thành công') => 
    window.showModal({ title, message: msg, type: 'success', okText: 'OK' });
})();

function logout() {
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
        localStorage.clear();
        window.location.href = "/login.html";
    }
}