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
  // Preview thêm - với validation
  const addFile = byId('addImgFile'), addPrev = byId('addImgPreview');
  if (addFile && addPrev) {
    addFile.addEventListener('change', e => {
      const f = e.target.files?.[0];
      if (f) {
        // Validate file type
        if (!f.type.startsWith('image/')) {
          alert('Vui lòng chọn file ảnh!');
          e.target.value = '';
          addPrev.src = '';
          addPrev.style.display = 'none';
          return;
        }
        // Validate file size (max 5MB)
        if (f.size > 5 * 1024 * 1024) {
          alert('Ảnh quá lớn! Vui lòng chọn ảnh dưới 5MB.');
          e.target.value = '';
          addPrev.src = '';
          addPrev.style.display = 'none';
          return;
        }
        addPrev.src = URL.createObjectURL(f);
        addPrev.style.display = 'block';
      } else {
        addPrev.src = '';
        addPrev.style.display = 'none';
      }
    });
  }

  // Preview sửa - với validation
  const editFile = byId('editImgFile'), editPrev = byId('editImgPreview');
  if (editFile && editPrev) {
    editFile.addEventListener('change', e => {
      const f = e.target.files?.[0];
      if (f) {
        // Validate file type
        if (!f.type.startsWith('image/')) {
          alert('Vui lòng chọn file ảnh!');
          e.target.value = '';
          return;
        }
        // Validate file size (max 5MB)
        if (f.size > 5 * 1024 * 1024) {
          alert('Ảnh quá lớn! Vui lòng chọn ảnh dưới 5MB.');
          e.target.value = '';
          return;
        }
        editPrev.src = URL.createObjectURL(f);
        editPrev.style.display = 'block';
      }
    });
  }

  await fetchCategories();
  await loadMenuItems();
});

// ===== Categories (filter + selects) =====
async function fetchCategories() {
  try {
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
    const addSel = byId('addCategory');
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
  } catch (e) {
    console.error('Lỗi tải danh mục:', e);
  }
}

// ===== Load items =====
window.loadMenuItems = async function () {
  const container = byId('menuItemContainer');
  if (!container) return;
  container.innerHTML = `<div style="text-align:center; color:#6b7280; padding:40px; grid-column: 1/-1;">⏳ Đang tải...</div>`;

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
      container.innerHTML = `<div style="text-align:center; color:#6b7280; padding:60px; grid-column: 1/-1;">
        <div style="font-size:4rem; margin-bottom:16px;">🍽️</div>
        <p style="font-size:1.1rem; margin:0;">Chưa có món ăn nào</p>
      </div>`;
      return;
    }

    items.forEach(it => {
      const card = document.createElement('div');
      card.className = 'menu-item-card';
      
      // Lấy tên danh mục
      const categoryName = it.category?.name || 'Chưa phân loại';
      
      card.innerHTML = `
        ${it.img ? `<img src="${safeUrl(it.img)}?v=${Date.now()}" alt="${escapeHtml(it.name)}" onerror="this.style.display='none'">` : ''}
        <h3 class="menu-item-name">${escapeHtml(it.name ?? '')}</h3>
        <p class="menu-item-price">${(it.price ?? 0).toLocaleString('vi-VN')}đ</p>
        <span class="menu-item-category">${escapeHtml(categoryName)}</span>
        ${(window.role === 'MANAGER') ? `
          <div style="margin-top:auto; display:flex; gap:8px; justify-content:center;">
            <button class="btn" onclick="showEditItem(${Number(it.id)})">✏️</button>
            <button class="btn red" onclick="deleteItem(${Number(it.id)})">🗑️</button>
          </div>` : ''
        }
      `;
      container.appendChild(card);
    });
  } catch (e) {
    container.innerHTML = `<div style="text-align:center; color:#ef4444; padding:40px; grid-column: 1/-1;">
      <div style="font-size:3rem; margin-bottom:12px;">⚠️</div>
      <p style="margin:0;">${e.message || 'Không thể tải món'}</p>
    </div>`;
  }
};

// ===== ADD =====
window.showAddItem = function () {
  if (window.role !== 'MANAGER') return;
  byId('addName').value = '';
  byId('addPrice').value = '';
  const f = byId('addImgFile');
  if (f) f.value = '';
  const prev = byId('addImgPreview');
  if (prev) {
    prev.src = '';
    prev.style.display = 'none';
  }
  if (allCategories.length > 0) byId('addCategory').value = String(allCategories[0].id);
  showError('addError', '');
  byId('addItemModal').style.display = 'flex';
  setTimeout(() => byId('addName').focus(), 100);
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
  const errId = 'addError';

  showError(errId, '');

  if (!name) {
    showError(errId, 'Vui lòng nhập tên món');
    byId('addName').focus();
    return;
  }

  if (isNaN(priceVal) || priceVal < 0) {
    showError(errId, 'Giá không hợp lệ');
    byId('addPrice').focus();
    return;
  }

  if (!cateId) {
    showError(errId, 'Vui lòng chọn danh mục');
    return;
  }

  // Check trùng tên (nhẹ nhàng, không chặn nếu lỗi tải danh sách)
  try {
    const resItems = await fetch(`${BASE_URL}/api/menu`);
    const existingItems = resItems.ok ? await resItems.json() : [];
    if (existingItems.some(it => (it.name || '').trim().toLowerCase() === name.toLowerCase())) {
      showError(errId, 'Tên món đã tồn tại, vui lòng chọn tên khác.');
      byId('addName').focus();
      return;
    }
  } catch { }

  try {
    // 1) Tạo món trước (chưa có ảnh)
    const res = await $fetch(`${BASE_URL}/api/menu`, {
      method: 'POST',
      body: JSON.stringify({ name, price: priceVal, category: { id: cateId } })
    });
    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Tạo món thất bại');
    }
    const created = await res.json();

    // 2) Nếu có file -> upload ảnh
    if (file) {
      const fd = new FormData();
      fd.append('file', file);
      const up = await $fetch(`${BASE_URL}/api/menu/${created.id}/image`, {
        method: 'POST',
        body: fd
      });
      if (!up.ok) {
        const errMsg = await $readErr(up);
        throw new Error(errMsg || 'Upload ảnh thất bại');
      }
    }

    window.closeAddItemModal();
    showAppSuccess(`Đã thêm món "${name}" thành công!`);
    setTimeout(() => loadMenuItems(), 500);
  } catch (e) {
    showError(errId, e.message || 'Có lỗi khi tạo món');
  }
};

// ===== EDIT =====
window.showEditItem = async function (id) {
  if (window.role !== 'MANAGER') return;
  showError('editError', '');
  const f = byId('editImgFile');
  if (f) f.value = '';
  const prev = byId('editImgPreview');
  if (prev) {
    prev.src = '';
    prev.style.display = 'none';
  }

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

    if (it.img && prev) {
      prev.src = safeUrl(it.img) + '?v=' + Date.now();
      prev.style.display = 'block';
    }

    byId('editItemModal').style.display = 'flex';
    setTimeout(() => byId('editName').focus(), 100);
  } catch (e) {
    showAppError(e.message || 'Lỗi tải chi tiết món');
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
  const errId = 'editError';

  showError(errId, '');

  if (!name) {
    showError(errId, 'Tên món không được rỗng');
    byId('editName').focus();
    return;
  }

  if (isNaN(priceVal) || priceVal < 0) {
    showError(errId, 'Giá không hợp lệ');
    byId('editPrice').focus();
    return;
  }

  if (!cateId) {
    showError(errId, 'Vui lòng chọn danh mục');
    return;
  }

  try {
    // 1) Cập nhật thông tin
    const res = await $fetch(`${BASE_URL}/api/menu/${editingItemId}`, {
      method: 'PUT',
      body: JSON.stringify({ name, price: priceVal, category: { id: cateId } })
    });
    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Cập nhật thất bại');
    }

    // 2) Nếu có file -> upload ảnh
    if (file) {
      const fd = new FormData();
      fd.append('file', file);
      const up = await $fetch(`${BASE_URL}/api/menu/${editingItemId}/image`, {
        method: 'POST',
        body: fd
      });
      if (!up.ok) {
        const errMsg = await $readErr(up);
        throw new Error(errMsg || 'Upload ảnh thất bại');
      }
    }

    window.closeEditItemModal();
    showAppSuccess(`Đã cập nhật món "${name}" thành công!`);
    setTimeout(() => loadMenuItems(), 500);
  } catch (e) {
    showError(errId, e.message || 'Có lỗi khi cập nhật');
  }
};

// ===== DELETE =====
window.deleteItem = async function (id) {
  if (window.role !== 'MANAGER') return;
  if (!confirm('Bạn có chắc muốn xóa món này?')) return;

  try {
    const res = await $fetch(`${BASE_URL}/api/menu/${Number(id)}`, { method: 'DELETE' });
    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Xóa thất bại');
    }
    showAppSuccess('Đã xóa món thành công!');
    setTimeout(() => loadMenuItems(), 500);
  } catch (e) {
    showAppError(e.message || 'Có lỗi khi xóa');
  }
};

// ===== Close modal on Escape =====
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    const addModal = byId('addItemModal');
    const editModal = byId('editItemModal');
    if (addModal && addModal.style.display === 'flex') closeAddItemModal();
    if (editModal && editModal.style.display === 'flex') closeEditItemModal();
  }
});