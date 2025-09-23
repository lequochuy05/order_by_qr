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
// ===== Modal thông báo dùng chung cho ADMIN =====
(function initAdminMessageModal(){
  // Nếu chưa có modal thông báo -> tự chèn
  if (!document.getElementById('msgModal')) {
    const wrap = document.createElement('div');
    wrap.innerHTML = `
      <div id="msgModal" class="modal" style="display:none;">
        <div class="modal-content" style="max-width:460px;">
          <span class="close" id="msgModalClose" aria-label="Đóng">&times;</span>
          <h3 id="msgModalTitle" style="margin:6px 0 10px;">Thông báo</h3>
          <div id="msgModalBody" style="color:#374151;"></div>
          <div style="text-align:right; margin-top:14px;">
            <button class="btn primary" id="msgModalOk">OK</button>
          </div>
        </div>
      </div>
    `.trim();
    document.body.appendChild(wrap.firstElementChild);
  }

  const modal = document.getElementById('msgModal');
  const btnClose = document.getElementById('msgModalClose');
  const btnOk    = document.getElementById('msgModalOk');
  const titleEl  = document.getElementById('msgModalTitle');
  const bodyEl   = document.getElementById('msgModalBody');

  function hide(){ modal.style.display = 'none'; }
  btnClose.addEventListener('click', hide);
  btnOk.addEventListener('click', hide);
  modal.addEventListener('click', (e) => { if (e.target === modal) hide(); });
  document.addEventListener('keydown', (e)=>{ if(e.key==='Escape' && modal.style.display==='flex') hide(); });

  function safeHtmlFromText(txt){
    const s = String(txt ?? '');
    const esc = s.replace(/[&<>]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
    return esc.replace(/\n/g,'<br>');
  }

  // Hàm hiển thị chung
  window.showModal = function({ title='Thông báo', message='', type='info', okText='OK' } = {}){
    titleEl.textContent = title;
    bodyEl.innerHTML = safeHtmlFromText(message) || '—';
    btnOk.textContent = okText || 'OK';
    modal.style.display = 'flex';
    btnOk.focus();
  };

  window.showError   = (msg, title='Có lỗi xảy ra') => window.showModal({ title, message: msg, type:'error',   okText:'Đã hiểu' });
  window.showInfo    = (msg, title='Thông báo')     => window.showModal({ title, message: msg, type:'info',    okText:'OK'      });
  window.showSuccess = (msg, title='Thành công')    => window.showModal({ title, message: msg, type:'success', okText:'OK'      });
})();

function logout() {
    localStorage.clear();
    window.location.href = "/login.html";
}
