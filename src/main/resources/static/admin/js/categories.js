// admin/js/categories.js
if (role === "MANAGER") {
  document.getElementById("adminActions").style.display = "block";
}

// ================== GLOBAL VARS ==================
let editingCategoryId = null;
let q = '';
let page = 0;
let size = 12;
let sort = 'name,asc';

// ================== LOAD & RENDER ==================
async function loadCategories() {
  const container = document.getElementById('categoryContainer');
  if (!container) return;

  container.innerHTML = `
    <div style="text-align:center; color:#6b7280; padding:40px; grid-column: 1/-1;">
      ⏳ Đang tải...
    </div>`;

  const url = q
    ? `${BASE_URL}/api/categories/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`
    : `${BASE_URL}/api/categories`;

  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error('Lỗi tải danh mục');
    const data = q ? await res.json() : { content: await res.json(), totalPages: 1 };

    container.innerHTML = '';

    if (!data.content || data.content.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; color:#6b7280; padding:60px; grid-column: 1/-1;">
          <div style="font-size:4rem; margin-bottom:16px;">📂</div>
          <p style="font-size:1.1rem; margin:0;">Chưa có danh mục nào</p>
        </div>`;
      renderPagination(0);
      return;
    }

    data.content.forEach(cat => {
      const card = document.createElement('div');
      card.className = 'category-card';
      card.innerHTML = `
        ${cat.img ? `<img src="${safeUrl(cat.img)}?v=${Date.now()}" alt="${escapeHtml(cat.name)}"
            onerror="this.style.display='none'">` : ''}
        <h3>${escapeHtml(cat.name)}</h3>
        ${window.role === 'MANAGER' ? `
          <div style="margin-top:auto;">
            <button class="btn" onclick="showEditCategory(${Number(cat.id)}, '${escapeHtml(cat.name).replace(/'/g, "\\'")}', '${escapeHtml(cat.img || '').replace(/'/g, "\\'")}')">✏️</button>
            <button class="btn red" onclick="deleteCategory(${Number(cat.id)})">🗑️</button>
          </div>` : ''
        }`;
      container.appendChild(card);
    });

    renderPagination(data.totalPages || 1);
  } catch (e) {
    container.innerHTML = `
      <div style="text-align:center; color:#ef4444; padding:40px; grid-column: 1/-1;">
        <div style="font-size:3rem; margin-bottom:12px;">⚠️</div>
        <p style="margin:0;">${e.message || 'Không thể tải danh mục'}</p>
      </div>`;
    renderPagination(0);
  }
}

function renderPagination(totalPages) {
  const container = document.getElementById('categoryContainer');
  if (!container) return;

  let bar = document.getElementById('cate-pagination');
  if (!bar) {
    bar = document.createElement('div');
    bar.id = 'cate-pagination';
    container.parentNode.insertBefore(bar, container.nextSibling);
  }
  bar.innerHTML = '';

  if (totalPages <= 1) return;

  for (let i = 0; i < totalPages; i++) {
    const b = document.createElement('button');
    b.className = 'btn';
    b.textContent = (i + 1);
    if (i === page) {
      b.style.background = '#3498db';
      b.style.color = '#fff';
    }
    b.onclick = () => {
      page = i;
      loadCategories();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    };
    bar.appendChild(b);
  }
}

// ================== MODAL ==================
window.showAddCategory = function () {
  editingCategoryId = null;
  byId('categoryModalTitle').textContent = 'Thêm danh mục';
  byId('categoryName').value = '';
  byId('categoryImgFile').value = '';
  const prev = byId('categoryPreview');
  prev.src = '';
  prev.style.display = 'none';
  showError('categoryError', '');
  byId('categoryModal').style.display = 'flex';
  setTimeout(() => byId('categoryName').focus(), 100);
};

window.showEditCategory = function (id, name, img) {
  editingCategoryId = Number(id);
  byId('categoryModalTitle').textContent = 'Cập nhật danh mục';
  byId('categoryName').value = name || '';
  byId('categoryImgFile').value = '';

  const prev = byId('categoryPreview');
  if (img) {
    prev.src = safeUrl(img) + '?v=' + Date.now();
    prev.style.display = 'block';
  } else {
    prev.src = '';
    prev.style.display = 'none';
  }

  showError('categoryError', '');
  byId('categoryModal').style.display = 'flex';
  setTimeout(() => byId('categoryName').focus(), 100);
};

window.closeCategoryModal = function () {
  byId('categoryModal').style.display = 'none';
};

// ================== SUBMIT (THÊM / SỬA) ==================
window.submitCategory = async function () {
  const name = byId('categoryName').value.trim();
  const file = byId('categoryImgFile').files[0] || null;
  const errId = 'categoryError';
  showError(errId, '');

  if (!name) {
    showError(errId, 'Vui lòng nhập tên danh mục');
    byId('categoryName').focus();
    return;
  }
  if (!file) {
    showError(errId, 'Vui lòng nhập ảnh danh mục');
    byId('categoryImgFile').focus();
    return;
  }
  

  try {
    const url = editingCategoryId
      ? `${BASE_URL}/api/categories/${editingCategoryId}`
      : `${BASE_URL}/api/categories`;
    const method = editingCategoryId ? 'PUT' : 'POST';

    const res = await $fetch(url, { method, body: JSON.stringify({ name }) });
    if (!res.ok) throw new Error(await $readErr(res));
    const created = await res.json();

    // Nếu có file ảnh thì upload
    if (file) {
      if (!file.type.startsWith('image/')) throw new Error('Chỉ chọn file ảnh!');
      if (file.size > 5 * 1024 * 1024) throw new Error('Ảnh quá lớn! Tối đa 5MB.');

      const fd = new FormData();
      fd.append('file', file);
      const resUp = await $fetch(`${BASE_URL}/api/categories/${created.id}/image`, {
        method: 'POST',
        body: fd
      });
      if (!resUp.ok) throw new Error(await $readErr(resUp));
    }

    closeCategoryModal();
    showAppSuccess(editingCategoryId ? 'Đã cập nhật danh mục!' : 'Đã thêm danh mục mới!');
    setTimeout(loadCategories, 500);
  } catch (e) {
    showError(errId, e.message || 'Có lỗi khi lưu danh mục');
  }
};

// ================== DELETE ==================
window.deleteCategory = async function (id) {
  if (!confirm('Bạn có chắc muốn xóa danh mục này?\n\nCác món ăn trong danh mục sẽ bị ảnh hưởng.')) return;
  try {
    const res = await $fetch(`${BASE_URL}/api/categories/${Number(id)}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(await $readErr(res));
    showAppSuccess('Đã xóa danh mục thành công!');
    setTimeout(loadCategories, 500);
  } catch (e) {
    showAppError(e.message || 'Có lỗi khi xóa danh mục');
  }
};

// ================== PREVIEW ẢNH ==================
(function bindPreview() {
  const input = byId('categoryImgFile');
  const preview = byId('categoryPreview');
  if (!input || !preview) return;

  input.addEventListener('change', e => {
    const f = e.target.files[0];
    if (!f) {
      preview.style.display = 'none';
      return;
    }
    if (!f.type.startsWith('image/')) {
      alert('Vui lòng chọn file ảnh!');
      input.value = '';
      preview.style.display = 'none';
      return;
    }
    if (f.size > 5 * 1024 * 1024) {
      alert('Ảnh quá lớn! Vui lòng chọn ảnh dưới 5MB.');
      input.value = '';
      preview.style.display = 'none';
      return;
    }
    const url = URL.createObjectURL(f);
    preview.src = url;
    preview.style.display = 'block';
  });
})();

// ================== SEARCH ==================
function debounce(fn, delay = 300) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), delay); };
}

const onSearchInput = debounce(val => {
  q = (val || '').trim();
  page = 0;
  loadCategories();
}, 400);

// ================== ESCAPE CLOSE ==================
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    const modal = byId('categoryModal');
    if (modal && modal.style.display === 'flex') closeCategoryModal();
  }
});

// ================== INIT ==================
window.addEventListener('DOMContentLoaded', () => {
  const input = document.getElementById('cateSearch');
  if (input) {
    input.addEventListener('input', e => onSearchInput(e.target.value));
    input.addEventListener('keydown', e => {
      if (e.key === 'Escape') { input.value = ''; onSearchInput(''); }
    });
  }
  loadCategories();
});
