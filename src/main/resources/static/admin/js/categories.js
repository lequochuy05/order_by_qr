// src/main/resources/static/admin/js/categories.js
// KHÔNG khai báo lại 'role' ở đây (đã có trong common.js)

if (role === "MANAGER") {
    document.getElementById("adminActions").style.display = "block";
}

const BASE_URL = window.APP_BASE_URL || location.origin;
let editingCategoryId = null;
let q = '';
let page = 0;
let size = 12;
let sort = 'name,asc';

// ===== Load & render =====
async function loadCategories() {
  const container = document.getElementById('categoryContainer');
  if (!container) return;
  container.innerHTML = `<div style="text-align:center; color:#6b7280; padding:16px;">Đang tải...</div>`;

  const url = q
    ? `${BASE_URL}/api/categories/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`
    : `${BASE_URL}/api/categories`;

  try {
    const res = await fetch(url); // GET public
    if (!res.ok) throw new Error('Lỗi tải danh mục');

    const data = q ? await res.json() : { content: await res.json(), totalPages: 1 };

    container.innerHTML = '';
    (data.content || []).forEach(cat => {
      const card = document.createElement('div');
      card.className = 'table-card';
      card.innerHTML = `
        ${cat.img ? `<img src="${safeUrl(cat.img)}" alt="${escapeHtml(cat.name)}" style="width:100%; height:150px; object-fit:cover; border-radius:10px;">` : ''}
        <h3 style="text-align:center;">${escapeHtml(cat.name)}</h3>
        ${window.role === 'MANAGER' ? `
          <div style="text-align:center;">
            <button class="btn" onclick="showEditCategory(${Number(cat.id)}, '${escapeHtml(cat.name)}', '${escapeHtml(cat.img || '')}')">✏️</button>
            <button class="btn" onclick="deleteCategory(${Number(cat.id)})">🗑️</button>
          </div>` : ''
        }
      `;
      container.appendChild(card);
    });

    renderPagination(data.totalPages || 1);
  } catch (e) {
    container.innerHTML = `<div style="text-align:center; color:#ef4444; padding:16px;">${e.message || 'Không thể tải danh mục'}</div>`;
  }
}

function renderPagination(totalPages) {
  const container = document.getElementById('categoryContainer');
  if (!container) return;

  let bar = document.getElementById('cate-pagination');
  if (!bar) {
    bar = document.createElement('div');
    bar.id = 'cate-pagination';
    bar.style = 'display:flex;gap:8px;justify-content:center;margin:12px 0';
    container.parentNode.insertBefore(bar, container.nextSibling);
  }
  bar.innerHTML = '';

  if (totalPages <= 1) return;
  for (let i = 0; i < totalPages; i++) {
    const b = document.createElement('button');
    b.className = 'btn';
    b.textContent = (i + 1);
    if (i === page) { b.style.background = '#3498db'; b.style.color = '#fff'; }
    b.onclick = () => { page = i; loadCategories(); };
    bar.appendChild(b);
  }
}

// ===== Modal - export ra window để HTML gọi được =====
window.showAddCategory = function () {
  byId('newCategoryName').value = '';
  byId('newCategoryImg').value = '';
  showError('addCategoryError', '');
  byId('addCategoryModal').style.display = 'flex';
};
window.closeAddCategoryModal = function () {
  byId('addCategoryModal').style.display = 'none';
};

window.submitNewCategory = async function () {
  const name = byId('newCategoryName').value.trim();
  const img  = byId('newCategoryImg').value.trim();
  const errId = 'addCategoryError';
  showError(errId, '');

  if (!name) { showError(errId, 'Vui lòng nhập tên danh mục'); return; }

  try {
    const res = await $fetch(`${BASE_URL}/api/categories`, {
      method: 'POST',
      body: JSON.stringify({ name, img })
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'Tạo danh mục thất bại');
    }
    window.closeAddCategoryModal();
    page = 0;
    loadCategories();
  } catch (e) {
    showError(errId, e.message || 'Có lỗi khi tạo danh mục');
  }
};

window.showEditCategory = function (id, name, img) {
  editingCategoryId = Number(id);
  byId('editCategoryName').value = name || '';
  byId('editCategoryImg').value = img || '';
  showError('editCategoryError', '');
  byId('editCategoryModal').style.display = 'flex';
};
window.closeEditCategoryModal = function () {
  byId('editCategoryModal').style.display = 'none';
};

window.submitEditCategory = async function () {
  const name = byId('editCategoryName').value.trim();
  const img  = byId('editCategoryImg').value.trim();
  const errId = 'editCategoryError';
  showError(errId, '');

  if (!name) { showError(errId, 'Tên danh mục không được rỗng'); return; }

  try {
    const res = await $fetch(`${BASE_URL}/api/categories/${editingCategoryId}`, {
      method: 'PUT',
      body: JSON.stringify({ name, img })
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'Cập nhật thất bại');
    }
    window.closeEditCategoryModal();
    loadCategories();
  } catch (e) {
    showError(errId, e.message || 'Có lỗi khi cập nhật danh mục');
  }
};

window.deleteCategory = async function (id) {
  if (!confirm('Bạn có chắc muốn xóa danh mục này?')) return;
  try {
    const res = await $fetch(`${BASE_URL}/api/categories/${Number(id)}`, { method: 'DELETE' });
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || 'Xóa thất bại');
    }
    loadCategories();
  } catch (e) {
    alert(e.message || 'Có lỗi khi xóa danh mục');
  }
};

// ===== Utils =====
function byId(id) { return document.getElementById(id); }

function showError(id, msg) {
  const el = byId(id);
  if (!el) return;
  el.textContent = msg || '';
  el.style.display = msg ? 'block' : 'none';
}

function debounce(fn, delay = 300) {
  let t; return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), delay); };
}
const onSearchInput = debounce(val => { q = (val || '').trim(); page = 0; loadCategories(); }, 350);

function escapeHtml(s = '') {
  return s.replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
}
function safeUrl(u = '') {
  try {
    const url = new URL(u, location.origin);
    return (url.protocol === 'http:' || url.protocol === 'https:') ? url.href : '';
  } catch { return ''; }
}

// ===== Boot =====
window.addEventListener('DOMContentLoaded', () => {
  const input = document.getElementById('cateSearch');
  if (input) input.addEventListener('input', e => onSearchInput(e.target.value));
  loadCategories();
});
