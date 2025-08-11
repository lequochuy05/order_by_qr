// src/main/resources/static/admin/js/items.js
if (role === "MANAGER") {
    document.getElementById("adminActions").style.display = "block";
}

const BASE_URL = window.APP_BASE_URL || location.origin;

let allCategories = [];
let editingItemId = null;

// -------- helpers ----------
function byId(id) { return document.getElementById(id); }
function showError(id, msg) {
  const el = byId(id);
  if (!el) return;
  el.textContent = msg || '';
  el.style.display = msg ? 'block' : 'none';
}
function escapeHtml(s='') {
  return s.replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]));
}
function safeUrl(u='') {
  try {
    const url = new URL(u, location.origin);
    return (url.protocol === 'http:' || url.protocol === 'https:') ? url.href : '';
  } catch { return ''; }
}


// ---------- bootstrap ----------
window.addEventListener('DOMContentLoaded', async () => {
  await fetchCategories();   // fill filter + modal selects
  await loadMenuItems();     // render list
});

// ---------- categories (for filters/selects) ----------
async function fetchCategories() {
  const res = await fetch(`${BASE_URL}/api/categories`);
  const data = res.ok ? await res.json() : [];
  allCategories = Array.isArray(data) ? data : [];

  // Filter select
  const filter = byId('categoryFilter');
  if (filter) {
    filter.innerHTML = `<option value="ALL">Tất cả</option>`;
    allCategories.forEach(c => {
      const opt = document.createElement('option');
      opt.value = String(c.id);
      opt.textContent = c.name;
      filter.appendChild(opt);
    });
  }

  // Add/Edit modal selects
  const addSel  = byId('addCategory');
  const editSel = byId('editCategory');
  [addSel, editSel].forEach(sel => {
    if (!sel) return;
    sel.innerHTML = '';
    allCategories.forEach(c => {
      const opt = document.createElement('option');
      opt.value = String(c.id);
      opt.textContent = c.name;
      sel.appendChild(opt);
    });
  });
}

// ---------- load items ----------
window.loadMenuItems = async function () {
  const container = byId('menuItemContainer');
  if (!container) return;
  container.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Đang tải...</div>`;

  const sel = byId('categoryFilter');
  const cateId = sel ? sel.value : 'ALL';

  const url = (cateId && cateId !== 'ALL')
    ? `${BASE_URL}/api/menu/category/${encodeURIComponent(cateId)}`
    : `${BASE_URL}/api/menu`;

  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error('Lỗi tải danh sách món');
    const items = await res.json();

    container.innerHTML = '';
    if (!Array.isArray(items) || items.length === 0) {
      container.innerHTML = `<div style="text-align:center;color:#6b7280;padding:20px;">Chưa có món nào.</div>`;
      return;
    }

    items.forEach(it => {
      const card = document.createElement('div');
      card.className = 'table-card';
      card.innerHTML = `
        <div style="text-align:center;">
          ${it.img ? `<img src="${safeUrl(it.img)}" alt="${escapeHtml(it.name)}" style="width:50%;height:150px;object-fit:cover;border-radius:10px;">` : ''}
          <h3>${escapeHtml(it.name ?? '')}</h3>
          <p><strong>Giá:</strong> ${(it.price ?? 0).toLocaleString('vi-VN')}đ</p>
          ${(window.role === 'MANAGER') ? `
            <div style="display:flex;gap:8px;justify-content:center;">
              <button class="btn" onclick="showEditItem(${Number(it.id)})">Sửa</button>
              <button class="btn red" onclick="deleteItem(${Number(it.id)})">Xóa</button>
            </div>` : ''
          }
        </div>
      `;
      container.appendChild(card);
    });
  } catch (e) {
    container.innerHTML = `<div style="text-align:center;color:#ef4444;padding:16px;">${e.message || 'Không thể tải món'}</div>`;
  }
};

// ---------- ADD ----------
window.showAddItem = function () {
  if (window.role !== 'MANAGER') return;
  byId('addName').value = '';
  byId('addPrice').value = '';
  byId('addImg').value = '';
  if (allCategories.length > 0) byId('addCategory').value = String(allCategories[0].id);
  showError('addError', '');
  byId('addItemModal').style.display = 'flex';
};
window.closeAddItemModal = function () {
  byId('addItemModal').style.display = 'none';
};


window.submitNewItem = async function () {
  if (window.role !== 'MANAGER') return;

  const name = byId('addName').value.trim();
  const priceVal = Number(byId('addPrice').value || 0);
  const img = byId('addImg').value.trim();
  const cateId = Number(byId('addCategory').value);

  

  if (!name || isNaN(priceVal) || priceVal < 0 || !cateId) {
    showError('addError', 'Vui lòng kiểm tra lại các trường nhập.');
    return;
  }

  if (allCategories.length > 0) {
      // Lấy toàn bộ items trước khi tạo để check trùng
      const resItems = await fetch(`${BASE_URL}/api/menu`);
      const existingItems = resItems.ok ? await resItems.json() : [];
      if (existingItems.some(it => it.name.trim().toLowerCase() === name.toLowerCase())) {
          showError('addError', 'Tên món đã tồn tại, vui lòng chọn tên khác.');
          return;
      }
  }

  try {
    const res = await $fetch(`${BASE_URL}/api/menu`, {
      method: 'POST',
      body: JSON.stringify({
        name, price: priceVal, img,
        category: { id: cateId }
      })
    });
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || 'Tạo món thất bại');
    }
    window.closeAddItemModal();
    await loadMenuItems();
  } catch (e) {
    showError('addError', e.message || 'Có lỗi khi tạo món');
  }
};

// ---------- EDIT ----------
window.showEditItem = async function (id) {
  if (window.role !== 'MANAGER') return;
  showError('editError', '');

  try {
    const res = await fetch(`${BASE_URL}/api/menu/${Number(id)}`);
    if (!res.ok) throw new Error('Không tìm thấy món');
    const it = await res.json();

    editingItemId = Number(it.id);
    byId('editName').value = it.name ?? '';
    byId('editPrice').value = it.price ?? 0;
    byId('editImg').value = it.img ?? '';
    const currentCateId = it.category?.id;
    if (currentCateId) byId('editCategory').value = String(currentCateId);
    else if (allCategories.length > 0) byId('editCategory').value = String(allCategories[0].id);

    byId('editItemModal').style.display = 'flex';
  } catch (e) {
    alert(e.message || 'Lỗi tải chi tiết món');
  }
};
window.closeEditItemModal = function () {
  byId('editItemModal').style.display = 'none';
  editingItemId = null;
};
window.submitEditItem = async function () {
  if (window.role !== 'MANAGER' || !editingItemId) return;

  const name = byId('editName').value.trim();
  const priceVal = Number(byId('editPrice').value || 0);
  const img = byId('editImg').value.trim();
  const cateId = Number(byId('editCategory').value);

  if (!name || isNaN(priceVal) || priceVal < 0 || !cateId) {
    showError('editError', 'Vui lòng kiểm tra lại các trường nhập.');
    return;
  }

  try {
    const res = await $fetch(`${BASE_URL}/api/menu/${editingItemId}`, {
      method: 'PUT',
      body: JSON.stringify({
        name, price: priceVal, img,
        category: { id: cateId }
      })
    });
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || 'Cập nhật thất bại');
    }
    window.closeEditItemModal();
    await loadMenuItems();
  } catch (e) {
    showError('editError', e.message || 'Có lỗi khi cập nhật');
  }
};

// ---------- DELETE ----------
window.deleteItem = async function (id) {
  if (window.role !== 'MANAGER') return;
  if (!confirm('Xác nhận xóa món này?')) return;

  try {
    const res = await $fetch(`${BASE_URL}/api/menu/${Number(id)}`, { method: 'DELETE' });
    if (!res.ok) {
      const t = await res.text();
      throw new Error(t || 'Xóa thất bại');
    }
    await loadMenuItems();
  } catch (e) {
    alert(e.message || 'Có lỗi khi xóa');
  }
};

async function readErr(res) {
  try {
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
      const j = await res.json();
      return j.message || j.error || JSON.stringify(j);
    }
    return await res.text();
  } catch {
    return 'Có lỗi xảy ra';
  }
}
