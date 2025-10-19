// resources/static/admin/js/categories.js

if (role === "MANAGER") {
  document.getElementById("adminActions").style.display = "block";
}

let editingCategoryId = null;
let q = '';
let page = 0;
let size = 12;
let sort = 'name,asc';

// ===== Load & render =====
async function loadCategories() {
  const container = document.getElementById('categoryContainer');
  if (!container) return;
  container.innerHTML = `<div style="text-align:center; color:#6b7280; padding:40px; grid-column: 1/-1;">⏳ Đang tải...</div>`;

  const url = q
    ? `${BASE_URL}/api/categories/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`
    : `${BASE_URL}/api/categories`;

  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error('Lỗi tải danh mục');

    const data = q ? await res.json() : { content: await res.json(), totalPages: 1 };

    container.innerHTML = '';
    
    if (!data.content || data.content.length === 0) {
      container.innerHTML = `<div style="text-align:center; color:#6b7280; padding:60px; grid-column: 1/-1;">
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
        ${cat.img ? `<img src="${safeUrl(cat.img)}?v=${Date.now()}" alt="${escapeHtml(cat.name)}" onerror="this.style.display='none'">` : ''}
        <h3>${escapeHtml(cat.name)}</h3>
        ${window.role === 'MANAGER' ? `
          <div style="margin-top:auto;">
            <button class="btn" onclick="showEditCategory(${Number(cat.id)}, '${escapeHtml(cat.name).replace(/'/g, "\\'")}', '${escapeHtml(cat.img || '').replace(/'/g, "\\'")}')">✏️</button>
            <button class="btn red" onclick="deleteCategory(${Number(cat.id)})">🗑️</button>
          </div>` : ''
        }
      `;
      container.appendChild(card);
    });

    renderPagination(data.totalPages || 1);
  } catch (e) {
    container.innerHTML = `<div style="text-align:center; color:#ef4444; padding:40px; grid-column: 1/-1;">
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

// ===== Modal =====
window.showAddCategory = function () {
  byId('newCategoryName').value = '';
  const file = byId('newCategoryImgFile'); 
  file.value = '';
  const prev = byId('newCategoryPreview'); 
  prev.style.display = 'none'; 
  prev.src = '';
  showError('addCategoryError', '');
  byId('addCategoryModal').style.display = 'flex';
  setTimeout(() => byId('newCategoryName').focus(), 100);
};

window.closeAddCategoryModal = function () {
  byId('addCategoryModal').style.display = 'none';
};

window.submitNewCategory = async function () {
  const name = byId('newCategoryName').value.trim();
  const file = byId('newCategoryImgFile').files[0] || null;
  const errId = 'addCategoryError';
  showError(errId, '');

  if (!name) { 
    showError(errId, 'Vui lòng nhập tên danh mục'); 
    byId('newCategoryName').focus();
    return; 
  }

  try {
    // 1) Tạo category trước (chưa có ảnh)
    const resCreate = await $fetch(`${BASE_URL}/api/categories`, {
      method: 'POST',
      body: JSON.stringify({ name })
    });
    if (!resCreate.ok) {
      const errMsg = await $readErr(resCreate);
      throw new Error(errMsg || 'Tạo danh mục thất bại');
    }
    const created = await resCreate.json();

    // 2) Nếu có file ảnh -> upload
    if (file) {
      const fd = new FormData();
      fd.append('file', file);
      const resUp = await $fetch(`${BASE_URL}/api/categories/${created.id}/image`, {
        method: 'POST',
        body: fd
      });
      if (!resUp.ok) {
        const errMsg = await $readErr(resUp);
        throw new Error(errMsg || 'Upload ảnh thất bại');
      }
    }

    window.closeAddCategoryModal();
    showAppSuccess(`Đã thêm danh mục "${name}" thành công!`);
    page = 0;
    setTimeout(() => loadCategories(), 500);
  } catch (e) {
    showError(errId, e.message || 'Có lỗi khi tạo danh mục');
  }
};

window.showEditCategory = function (id, name, img) {
  editingCategoryId = Number(id);
  byId('editCategoryName').value = name || '';

  // reset file & preview
  const file = byId('editCategoryImgFile'); 
  file.value = '';
  const prev = byId('editCategoryPreview');
  if (img) { 
    prev.src = safeUrl(img) + '?v=' + Date.now(); 
    prev.style.display = 'block'; 
  } else { 
    prev.src = ''; 
    prev.style.display = 'none'; 
  }

  showError('editCategoryError', '');
  byId('editCategoryModal').style.display = 'flex';
  setTimeout(() => byId('editCategoryName').focus(), 100);
};

window.closeEditCategoryModal = function () {
  byId('editCategoryModal').style.display = 'none';
};

window.submitEditCategory = async function () {
  const name = byId('editCategoryName').value.trim();
  const file = byId('editCategoryImgFile').files[0] || null;
  const errId = 'editCategoryError';
  showError(errId, '');

  if (!name) { 
    showError(errId, 'Tên danh mục không được rỗng'); 
    byId('editCategoryName').focus();
    return; 
  }

  try {
    // 1) Cập nhật name
    const resUpdate = await $fetch(`${BASE_URL}/api/categories/${editingCategoryId}`, {
      method: 'PUT',
      body: JSON.stringify({ name })
    });
    if (!resUpdate.ok) {
      const errMsg = await $readErr(resUpdate);
      throw new Error(errMsg || 'Cập nhật thất bại');
    }

    // 2) Nếu có file -> upload ảnh
    if (file) {
      const fd = new FormData();
      fd.append('file', file);
      const resUp = await $fetch(`${BASE_URL}/api/categories/${editingCategoryId}/image`, {
        method: 'POST',
        body: fd
      });
      if (!resUp.ok) {
        const errMsg = await $readErr(resUp);
        throw new Error(errMsg || 'Upload ảnh thất bại');
      }
    }

    window.closeEditCategoryModal();
    showAppSuccess(`Đã cập nhật danh mục "${name}" thành công!`);
    setTimeout(() => loadCategories(), 500);
  } catch (e) {
    showError(errId, e.message || 'Có lỗi khi cập nhật danh mục');
  }
};

window.deleteCategory = async function (id) {
  if (!confirm('Bạn có chắc muốn xóa danh mục này?\n\nLưu ý: Các món ăn thuộc danh mục này có thể bị ảnh hưởng.')) return;
  
  try {
    const res = await $fetch(`${BASE_URL}/api/categories/${Number(id)}`, { method: 'DELETE' });
    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Xóa thất bại');
    }
    showAppSuccess('Đã xóa danh mục thành công!');
    setTimeout(() => loadCategories(), 500);
  } catch (e) {
    showAppError(e.message || 'Có lỗi khi xóa danh mục');
  }
};

// ===== Preview ảnh khi chọn file =====
(function bindFilePreviews(){
  const addFile = document.getElementById('newCategoryImgFile');
  const addPrev = document.getElementById('newCategoryPreview');
  if (addFile && addPrev) {
    addFile.addEventListener('change', e => {
      const f = e.target.files[0];
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
        const url = URL.createObjectURL(f);
        addPrev.src = url; 
        addPrev.style.display = 'block';
      } else {
        addPrev.src = ''; 
        addPrev.style.display = 'none';
      }
    });
  }

  const editFile = document.getElementById('editCategoryImgFile');
  const editPrev = document.getElementById('editCategoryPreview');
  if (editFile && editPrev) {
    editFile.addEventListener('change', e => {
      const f = e.target.files[0];
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
        const url = URL.createObjectURL(f);
        editPrev.src = url; 
        editPrev.style.display = 'block';
      }
    });
  }
})();

// ===== Utils =====
function debounce(fn, delay = 300) {
  let t; 
  return (...args) => { 
    clearTimeout(t); 
    t = setTimeout(() => fn(...args), delay); 
  };
}

const onSearchInput = debounce(val => { 
  q = (val || '').trim(); 
  page = 0; 
  loadCategories(); 
}, 400);

// ===== Close modal on Escape =====
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    const addModal = byId('addCategoryModal');
    const editModal = byId('editCategoryModal');
    if (addModal && addModal.style.display === 'flex') closeAddCategoryModal();
    if (editModal && editModal.style.display === 'flex') closeEditCategoryModal();
  }
});

// ===== Boot =====
window.addEventListener('DOMContentLoaded', () => {
  const input = document.getElementById('cateSearch');
  if (input) {
    input.addEventListener('input', e => onSearchInput(e.target.value));
    // Clear search on button (optional)
    input.addEventListener('keydown', e => {
      if (e.key === 'Escape') {
        input.value = '';
        onSearchInput('');
      }
    });
  }
  loadCategories();
});