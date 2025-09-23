// resource/static/js/common.js
async function readErr(res) {
  const ct = (res.headers.get('content-type') || '').toLowerCase();
  const raw = await res.text();
  if (!raw) return `${res.status} ${res.statusText}`;

  if (ct.includes('application/json') || ct.includes('application/problem+json')) {
    try {
      const j = JSON.parse(raw);
      // Ưu tiên các field chuẩn của Spring ProblemDetail
      return j.detail || j.message || j.error || j.title || raw;
    } catch {
      return raw;
    }
  }
  return raw;
}

// ===== Modal helpers (reusable) =====
(function initAppModal() {
  // 1) TRÁNH TRÙNG MODAL:
  // Nếu bạn đã fetch('/common.html') và nó đã có #app-modal trong DOM,
  // KHÔNG auto-inject nữa.
  let modal = document.getElementById('app-modal');
  if (!modal) {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = `
      <div id="app-modal" class="modal" role="dialog" aria-modal="true" aria-labelledby="app-modal-title" aria-hidden="true">
        <div class="modal__backdrop"></div>
        <div class="modal__dialog">
          <div class="modal__header">
            <h3 id="app-modal-title" class="modal__title">Thông báo</h3>
            <button type="button" class="modal__close" aria-label="Đóng">&times;</button>
          </div>
          <div id="app-modal-body" class="modal__body"></div>
          <div class="modal__footer">
            <button type="button" class="btn btn--primary" id="app-modal-ok">OK</button>
          </div>
        </div>
      </div>
    `.trim();
    document.body.appendChild(wrapper.firstElementChild);
    modal = document.getElementById('app-modal');
  }

  const btnClose = modal.querySelector('.modal__close');
  const btnOk = modal.querySelector('#app-modal-ok');
  const backdrop = modal.querySelector('.modal__backdrop');

  const hide = () => {
    modal.classList.remove('show', 'modal--error', 'modal--info', 'modal--success');
    modal.setAttribute('aria-hidden', 'true');
  };

  btnClose.addEventListener('click', hide);
  btnOk.addEventListener('click', hide);
  backdrop.addEventListener('click', hide);
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.classList.contains('show')) hide();
  });

  // === HÀM RENDER CHẮC ĂN ===
  function safeHtmlFromText(txt) {
    const s = String(txt ?? ''); // null/undefined -> ''
    // escape &, <, >
    const escaped = s.replace(/[&<>]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
    // chuyển \n thành <br>
    return escaped.replace(/\n/g, '<br>');
  }

  window.showModal = function({ title = 'Thông báo', message = '', type = 'info', okText = 'OK' } = {}) {
    const titleEl = modal.querySelector('#app-modal-title');
    const bodyEl  = modal.querySelector('#app-modal-body');
    const okEl    = modal.querySelector('#app-modal-ok');

    modal.classList.remove('modal--error', 'modal--info', 'modal--success');
    if (type) modal.classList.add(`modal--${type}`);

    titleEl.textContent = title;

    // Luôn hiển thị nội dung; nếu rỗng -> dùng dấu gạch ngang để dễ thấy
    const html = safeHtmlFromText(message);
    bodyEl.innerHTML = html && html.trim() ? html : '—';

    okEl.textContent = okText || 'OK';

    modal.classList.add('show');
    modal.setAttribute('aria-hidden', 'false');
    okEl.focus();
  };

  window.showError   = (msg, title='Có lỗi xảy ra') => window.showModal({ title, message: msg, type: 'error',   okText: 'Đã hiểu' });
  window.showInfo    = (msg, title='Thông báo')     => window.showModal({ title, message: msg, type: 'info',    okText: 'OK'      });
  window.showSuccess = (msg, title='Thành công')    => window.showModal({ title, message: msg, type: 'success', okText: 'OK'      });
})();
