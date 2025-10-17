// resources/static/admin/js/items.js

// Hiển thị nút thêm cho MANAGER
if (role === "MANAGER") {
  const a = document.getElementById("adminActions");
  if (a) a.style.display = "block";
}

let allCategories = [];
let editingItemId = null;

// ===== Bootstrap =====
window.addEventListener('DOMContentLoaded', async () => {
  // Preview thêm
  const addFile = byId('addImgFile'), addPrev = byId('addImgPreview');
  if (addFile && addPrev) {
    addFile.addEventListener('change', e => {
      const f = e.target.files?.[0];
      if (f) { addPrev.src = URL.createObjectURL(f); addPrev.style.display = 'block'; }
      else { addPrev.src = ''; addPrev.style.display = 'none'; }
    });
  }
  // Preview sửa
  const editFile = byId('editImgFile'), editPrev = byId('editImgPreview');
  if (editFile && editPrev) {
    editFile.addEventListener('change', e => {
      const f = e.target.files?.[0];
      if (f) { editPrev.src = URL.createObjectURL(f); editPrev.style.display = 'block'; }
    });
  }

  await fetchCategories();
  await loadMenuItems();
});

// ===== Categories (filter + selects) =====
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

  // Add/Edit selects
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

// ===== Load items =====
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
              <button class="btn" onclick="showEditItem(${Number(it.id)})">✏️</button>
              <button class="btn red" onclick="deleteItem(${Number(it.id)})">🗑️</button>
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

// ===== ADD =====
window.showAddItem = function () {
  if (window.role !== 'MANAGER') return;
  byId('addName').value = '';
  byId('addPrice').value = '';
  const f = byId('addImgFile'); if (f) f.value = '';
  const prev = byId('addImgPreview'); if (prev) { prev.src = ''; prev.style.display = 'none'; }
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
  const file = byId('addImgFile')?.files?.[0] || null;
  const cateId = Number(byId('addCategory').value);

  if (!name || isNaN(priceVal) || priceVal < 0 || !cateId) {
    showError('addError', 'Vui lòng kiểm tra lại các trường nhập.');
    return;
  }

  // Check trùng tên (nhẹ nhàng, không chặn nếu lỗi tải danh sách)
  try {
    const resItems = await fetch(`${BASE_URL}/api/menu`);
    const existingItems = resItems.ok ? await resItems.json() : [];
    if (existingItems.some(it => (it.name||'').trim().toLowerCase() === name.toLowerCase())) {
      showError('addError', 'Tên món đã tồn tại, vui lòng chọn tên khác.');
      return;
    }
  } catch {}

  try {
    // 1) Tạo món trước (chưa có ảnh)
    const res = await $fetch(`${BASE_URL}/api/menu`, {
      method: 'POST',
      body: JSON.stringify({ name, price: priceVal, category: { id: cateId } })
    });
    if (!res.ok) throw new Error(await res.text() || 'Tạo món thất bại');
    const created = await res.json(); // {id,...}

    // 2) Nếu có file -> upload ảnh
    if (file) {
      const fd = new FormData();
      fd.append('file', file);
      const up = await $fetch(`${BASE_URL}/api/menu/${created.id}/image`, {
        method: 'POST',
        body: fd
      });
      if (!up.ok) throw new Error(await up.text() || 'Upload ảnh thất bại');
    }

    window.closeAddItemModal();
    await loadMenuItems();
  } catch (e) {
    showError('addError', e.message || 'Có lỗi khi tạo món');
  }
};

// ===== EDIT =====
window.showEditItem = async function (id) {
  if (window.role !== 'MANAGER') return;
  showError('editError', '');
  const f = byId('editImgFile'); if (f) f.value = '';
  const prev = byId('editImgPreview'); if (prev) { prev.src = ''; prev.style.display = 'none'; }

  try {
    const res = await fetch(`${BASE_URL}/api/menu/${Number(id)}`);
    if (!res.ok) throw new Error('Không tìm thấy món');
    const it = await res.json();

    editingItemId = Number(it.id);
    byId('editName').value = it.name ?? '';
    byId('editPrice').value = it.price ?? 0;
    const currentCateId = it.category?.id;
    if (currentCateId) byId('editCategory').value = String(currentCateId);
    else if (allCategories.length > 0) byId('editCategory').value = String(allCategories[0].id);

    if (it.img && prev) { prev.src = safeUrl(it.img); prev.style.display = 'block'; }

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
  const file = byId('editImgFile')?.files?.[0] || null;
  const cateId = Number(byId('editCategory').value);

  if (!name || isNaN(priceVal) || priceVal < 0 || !cateId) {
    showError('editError', 'Vui lòng kiểm tra lại các trường nhập.');
    return;
  }

  try {
    // 1) Cập nhật thông tin
    const res = await $fetch(`${BASE_URL}/api/menu/${editingItemId}`, {
      method: 'PUT',
      body: JSON.stringify({ name, price: priceVal, category: { id: cateId } })
    });
    if (!res.ok) throw new Error(await res.text() || 'Cập nhật thất bại');

    // 2) Nếu có file -> upload ảnh
    if (file) {
      const fd = new FormData();
      fd.append('file', file);
      const up = await $fetch(`${BASE_URL}/api/menu/${editingItemId}/image`, {
        method: 'POST',
        body: fd
      });
      if (!up.ok) throw new Error(await up.text() || 'Upload ảnh thất bại');
    }

    window.closeEditItemModal();
    await loadMenuItems();
  } catch (e) {
    showError('editError', e.message || 'Có lỗi khi cập nhật');
  }
};

// ===== DELETE =====
window.deleteItem = async function (id) {
  if (window.role !== 'MANAGER') return;
  if (!confirm('Xác nhận xóa món này?')) return;

  try {
    const res = await $fetch(`${BASE_URL}/api/menu/${Number(id)}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(await res.text() || 'Xóa thất bại');
    await loadMenuItems();
  } catch (e) {
    alert(e.message || 'Có lỗi khi xóa');
  }
};
