// static/js/menu.js

let tableCode = null;
let cart = {};
let selectedCombos = {};
let combosCache = [];
let menuCache = [];

// ===== Helpers =====
function safeText(s = "") {
  return String(s).replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[m]));
}

function money(v) {
  return Number(v || 0).toLocaleString('vi-VN') + 'đ';
}

function toImgUrl(path) {
  if (!path) return 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="200" height="200"%3E%3Crect fill="%23f0f0f0" width="200" height="200"/%3E%3Ctext x="50%25" y="50%25" text-anchor="middle" dy=".3em" fill="%23999" font-size="20"%3ENo Image%3C/text%3E%3C/svg%3E';
  // Nếu path đã là link full (http...) thì giữ nguyên, ngược lại nối với BASE_URL
  return path.startsWith("http") ? path : (BASE_URL + "/images/" + path);
}

// ===== Categories =====
async function loadCategories(selected) {
  try {
    const res = await fetch(`${BASE_URL}/api/categories`);
    const categories = res.ok ? await res.json() : [];
    const select = document.getElementById('categoryFilter');
    
    // Reset select box
    select.innerHTML = `<option value="all">Tất cả</option>`;
    
    categories.forEach(cat => {
      const opt = document.createElement('option');
      opt.value = cat.id;
      opt.textContent = cat.name;
      select.appendChild(opt);
    });
    
    select.value = selected || 'all';
    await loadMenu(select.value);
  } catch (e) {
    console.error(e);
    if(window.showError) window.showError("Không tải được danh mục!");
  }
}

// ===== Load Menu =====
async function loadMenu(categoryId) {
  const container = document.getElementById("menuItems");
  container.innerHTML = "<p style='text-align:center;color:#666;'>Đang tải...</p>";
  
  const url = categoryId === "all"
    ? `${BASE_URL}/api/menu`
    : `${BASE_URL}/api/menu/category/${encodeURIComponent(categoryId)}`;
  
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error(await res.text());
    const items = await res.json();
    menuCache = items;
    
    container.innerHTML = "";
    if (!Array.isArray(items) || !items.length) {
      container.innerHTML = `<div style="grid-column:1/-1;text-align:center;color:#666;padding:20px;">Không có món</div>`;
      return;
    }
    
    const bust = Date.now(); // Cache busting cho ảnh
    items.forEach((item, index) => {
      // --- QUAN TRỌNG: Lọc trạng thái Active/Inactive ---
      // Nếu API trả về active=false hoặc status=false thì ẩn đi
      if (item.active === false || item.status === false) return; 
      // -------------------------------------------------

      const div = document.createElement('div');
      div.className = 'menu-card';
      div.style.animationDelay = `${index * 0.05}s`;
      div.onclick = () => addToCart(item.id, item.name, item.price);
      
      div.innerHTML = `
        <img class="card-image" src="${toImgUrl(item.img)}?v=${bust}" alt="${safeText(item.name)}">
        <div class="card-info">
          <div class="card-name">${safeText(item.name)}</div>
          <div class="card-price">${money(item.price)}</div>
        </div>
      `;
      container.appendChild(div);
    });
  } catch (e) {
    console.error(e);
    container.innerHTML = `<div style="grid-column:1/-1;color:#e11d48;text-align:center;">Không tải được thực đơn</div>`;
  }
}

// ===== Load Combos =====
async function loadCombos() {
  const wrap = document.getElementById('comboItems');
  if (!wrap) return;
  
  wrap.innerHTML = "<p style='text-align:center;color:#666;'>Đang tải...</p>";
  combosCache = [];
  
  try {
    const res = await fetch(`${BASE_URL}/api/combos`);
    const combos = res.ok ? await res.json() : [];
    combosCache = combos;
    
    if (!combos.length) {
      document.getElementById('comboSection').style.display = 'none';
      return;
    }
    
    document.getElementById('comboSection').style.display = 'block';
    wrap.innerHTML = '';
    
    combos.forEach((combo, index) => {
      // --- QUAN TRỌNG: Lọc trạng thái Combo ---
      if (combo.active === false || combo.status === false) return;
      // ----------------------------------------

      const div = document.createElement('div');
      div.className = 'combo-item';
      div.style.animationDelay = `${index * 0.05}s`;
      div.onclick = () => addComboToCart(combo.id, combo.name, combo.price);
      
      div.innerHTML = `
        <div class="combo-details">
          <div class="combo-name">${safeText(combo.name)}</div>
          <div class="combo-price">${money(combo.price)}</div>
        </div>
        <button class="combo-add-btn" onclick="event.stopPropagation(); addComboToCart(${combo.id}, '${safeText(combo.name)}', ${combo.price})">+</button>
      `;
      wrap.appendChild(div);
    });
  } catch (e) {
    console.error(e);
    wrap.innerHTML = '<p style="color:#e11d48;text-align:center;">Không tải được combo</p>';
  }
}

// ===== Cart Functions =====
function addToCart(id, name, price) {
  if (!cart[id]) {
    cart[id] = { qty: 0, note: "", name, price };
  }
  cart[id].qty++;
  updateCartUI();
  showAddedFeedback();
}

function addComboToCart(id, name, price) {
  if (!selectedCombos[id]) {
    selectedCombos[id] = { qty: 0, note: "", name, price };
  }
  selectedCombos[id].qty++;
  updateCartUI();
  showAddedFeedback();
}

function showAddedFeedback() {
  const badge = document.getElementById('cartBadge');
  badge.classList.add('animate');
  setTimeout(() => badge.classList.remove('animate'), 300);
}

function updateCartUI() {
  const badge = document.getElementById('cartBadge');
  const items = Object.values(cart).filter(v => v.qty > 0);
  const combos = Object.values(selectedCombos).filter(v => v.qty > 0);
  const totalItems = items.reduce((sum, v) => sum + v.qty, 0) + 
                    combos.reduce((sum, v) => sum + v.qty, 0);
  
  if (totalItems > 0) {
    badge.textContent = totalItems;
    badge.classList.add('show');
  } else {
    badge.classList.remove('show');
  }
  
  renderCartItems();
  updateCartTotal();
}

function renderCartItems() {
  const container = document.getElementById('cartItemsList');
  const items = Object.entries(cart).filter(([_, v]) => v.qty > 0);
  const combos = Object.entries(selectedCombos).filter(([_, v]) => v.qty > 0);
  
  if (items.length === 0 && combos.length === 0) {
    container.innerHTML = `
      <div class="cart-empty">
        <div class="cart-empty-icon">🛒</div>
        <div>Giỏ hàng trống</div>
        <div style="font-size: 13px; margin-top: 8px; color: #999;">Nhấn vào món để thêm</div>
      </div>
    `;
    return;
  }
  
  container.innerHTML = '';
  
  // Render menu items
  items.forEach(([id, v]) => {
    const lineTotal = v.qty * v.price;
    const div = document.createElement('div');
    div.className = 'cart-item';
    div.innerHTML = `
      <div class="cart-item-header">
        <div class="cart-item-name">${safeText(v.name)}</div>
        <button class="cart-item-delete" onclick="removeFromCart('${id}')">×</button>
      </div>
      <div class="cart-item-controls">
        <div class="cart-item-qty">
          <button onclick="updateCartQty('${id}', -1)">-</button>
          <span>${v.qty}</span>
          <button onclick="updateCartQty('${id}', 1)">+</button>
        </div>
        <div class="cart-item-price">${money(v.price)} × ${v.qty}</div>
        <div class="cart-item-total">${money(lineTotal)}</div>
      </div>
      <div class="cart-item-note">
        <input type="text" placeholder="Ghi chú (không bắt buộc)..." 
               value="${safeText(v.note)}" 
               oninput="updateCartNote('${id}', this.value)">
      </div>
    `;
    container.appendChild(div);
  });
  
  // Render combos
  combos.forEach(([cid, v]) => {
    const lineTotal = v.qty * v.price;
    const div = document.createElement('div');
    div.className = 'cart-item';
    div.innerHTML = `
      <div class="cart-item-header">
        <div class="cart-item-name">🎁 ${safeText(v.name)}</div>
        <button class="cart-item-delete" onclick="removeComboFromCart('${cid}')">×</button>
      </div>
      <div class="cart-item-controls">
        <div class="cart-item-qty">
          <button onclick="updateComboQty('${cid}', -1)">-</button>
          <span>${v.qty}</span>
          <button onclick="updateComboQty('${cid}', 1)">+</button>
        </div>
        <div class="cart-item-price">${money(v.price)} × ${v.qty}</div>
        <div class="cart-item-total">${money(lineTotal)}</div>
      </div>
      <div class="cart-item-note">
        <input type="text" placeholder="Ghi chú (không bắt buộc)..." 
               value="${safeText(v.note)}" 
               oninput="updateComboNote('${cid}', this.value)">
      </div>
    `;
    container.appendChild(div);
  });
}

function updateCartTotal() {
  const items = Object.values(cart).filter(v => v.qty > 0);
  const combos = Object.values(selectedCombos).filter(v => v.qty > 0);
  
  const itemTotal = items.reduce((sum, v) => sum + (v.qty * v.price), 0);
  const comboTotal = combos.reduce((sum, v) => sum + (v.qty * v.price), 0);
  const total = itemTotal + comboTotal;
  
  document.getElementById('cartTotalValue').textContent = money(total);
}

function updateCartQty(id, change) {
  if (cart[id]) {
    cart[id].qty = Math.max(0, cart[id].qty + change);
    if (cart[id].qty === 0) delete cart[id];
    updateCartUI();
  }
}

function updateComboQty(id, change) {
  if (selectedCombos[id]) {
    selectedCombos[id].qty = Math.max(0, selectedCombos[id].qty + change);
    if (selectedCombos[id].qty === 0) delete selectedCombos[id];
    updateCartUI();
  }
}

function updateCartNote(id, note) {
  if (cart[id]) cart[id].note = note;
}

function updateComboNote(id, note) {
  if (selectedCombos[id]) selectedCombos[id].note = note;
}

function removeFromCart(id) {
  delete cart[id];
  updateCartUI();
}

function removeComboFromCart(id) {
  delete selectedCombos[id];
  updateCartUI();
}

// ===== Cart Panel Controls =====
function toggleCart() {
  const panel = document.getElementById('cartPanel');
  const backdrop = document.getElementById('cartBackdrop');
  
  if (panel.classList.contains('open')) {
    closeCart();
  } else {
    panel.classList.add('open');
    backdrop.classList.add('show');
    updateCartUI();
  }
}

function closeCart() {
  document.getElementById('cartPanel').classList.remove('open');
  document.getElementById('cartBackdrop').classList.remove('show');
}

// ===== Checkout =====
function checkoutFromCart() {
  const items = Object.values(cart).filter(v => v.qty > 0);
  const combos = Object.values(selectedCombos).filter(v => v.qty > 0);
  
  if (items.length === 0 && combos.length === 0) {
    if(window.showInfo) window.showInfo("Giỏ hàng trống!");
    else alert("Giỏ hàng trống!");
    return;
  }
  
  closeCart();
  openConfirm();
}

function openConfirm() {
  const items = Object.entries(cart).filter(([_, v]) => v.qty > 0);
  const combos = Object.entries(selectedCombos).filter(([_, v]) => v.qty > 0);
  
  const box = document.getElementById('confirmList');
  box.innerHTML = '';
  
  items.forEach(([id, v]) => {
    const line = v.qty * v.price;
    const div = document.createElement('div');
    div.className = 'confirm-row';
    div.innerHTML = `
      <div class="confirm-row-header">
        <span class="confirm-row-name">${safeText(v.name)} × ${v.qty}</span>
        <span class="confirm-row-line">${money(line)}</span>
      </div>
      ${v.note ? `<div class="confirm-row-details">Ghi chú: ${safeText(v.note)}</div>` : ''}
    `;
    box.appendChild(div);
  });
  
  combos.forEach(([cid, v]) => {
    const line = v.qty * v.price;
    const div = document.createElement('div');
    div.className = 'confirm-row';
    div.innerHTML = `
      <div class="confirm-row-header">
        <span class="confirm-row-name">🎁 ${safeText(v.name)} × ${v.qty}</span>
        <span class="confirm-row-line">${money(line)}</span>
      </div>
      ${v.note ? `<div class="confirm-row-details">Ghi chú: ${safeText(v.note)}</div>` : ''}
    `;
    box.appendChild(div);
  });
  
  const subItems = items.reduce((s, [_, v]) => s + (v.qty * v.price), 0);
  const subCombos = combos.reduce((s, [_, v]) => s + (v.qty * v.price), 0);
  const total = subItems + subCombos;
  
  document.getElementById('subtotalItems').textContent = money(subItems);
  document.getElementById('subtotalCombos').textContent = money(subCombos);
  document.getElementById('confirmTotal').textContent = money(total);
  
  document.getElementById('confirmModal').classList.add('show');
}

function closeConfirm() {
  document.getElementById('confirmModal').classList.remove('show');
}

async function confirmOrder() {
  const btn = document.getElementById('confirmBtn');
  btn.disabled = true;
  btn.textContent = 'Đang xử lý...';
  
  try {
    const items = Object.entries(cart)
      .filter(([_, v]) => v.qty > 0)
      .map(([id, v]) => ({
        menuItemId: Number(id),
        quantity: v.qty,
        notes: v.note || null
      }));
    
    const combos = Object.entries(selectedCombos)
      .filter(([_, v]) => v.qty > 0)
      .map(([id, v]) => ({
        comboId: Number(id),
        quantity: v.qty,
        notes: v.note || null
      }));
    
    const res = await fetch(`${BASE_URL}/api/orders`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        tableCode,
        status: "PENDING",
        items,
        combos
      })
    });
    
    if (!res.ok) {
      const error = await res.text();
      throw new Error(error);
    }
    
    const data = await res.json();
    
    // Dùng modal đẹp từ common.js thay vì alert
    if(window.showSuccess) {
      window.showSuccess(`Đặt món thành công! Mã đơn: ${data.id || ''}`);
    } else {
      alert(`✓ Đặt món thành công!\nMã đơn: ${data.id || ''}`);
    }
    
    cart = {};
    selectedCombos = {};
    updateCartUI();
    closeConfirm();
    
  } catch (e) {
    if(window.showError) window.showError("Lỗi: " + e.message);
    else alert("Lỗi: " + e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Xác nhận';
  }
}

// ===== Search & Filter =====
function filterByName(q) {
  q = (q || '').toLowerCase().trim();
  const items = document.querySelectorAll('#menuItems .menu-card');
  items.forEach(card => {
    const name = card.querySelector('.card-name')?.textContent?.toLowerCase() || '';
    card.style.display = name.includes(q) ? '' : 'none';
  });
}

function reloadWithSort() {
  const sel = document.getElementById('categoryFilter');
  loadMenu(sel.value);
}

// ===== Initialize =====
document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(location.search);
  tableCode = params.get('tableCode');
  const category = params.get('categoryId') || 'all';
  
  if (!tableCode) {
    if(window.showError) window.showError("Không tìm thấy mã bàn!");
    else alert("Không tìm thấy mã bàn!");
    return;
  }
  
  // Hiển thị số bàn
  try {
    const res = await fetch(`${BASE_URL}/api/tables/code/${tableCode}`);
    const t = res.ok ? await res.json() : null;
    document.getElementById('tableNumber').textContent = t?.tableNumber || "—";
  } catch {
    document.getElementById('tableNumber').textContent = "—";
  }
  
  await loadCategories(category);
  await loadCombos();
  
  // ===== KẾT NỐI WEBSOCKET =====
  // Sử dụng hàm connectWS từ common.js
  if (typeof window.connectWS === 'function') {
      window.connectWS({
          '/topic/categories': () => {
              console.log("Reload Categories");
              loadCategories(document.getElementById('categoryFilter')?.value);
          },
          '/topic/menu': () => {
              console.log("Reload Menu");
              loadMenu(document.getElementById('categoryFilter')?.value);
          },
          '/topic/combos': () => {
              console.log("Reload Combos");
              loadCombos();
          }
      });
  } else {
      console.warn("⚠️ Không tìm thấy hàm connectWS. Kiểm tra lại việc nhúng file common.js");
  }
});

// ===== Expose Functions =====
window.addToCart = addToCart;
window.addComboToCart = addComboToCart;
window.updateCartQty = updateCartQty;
window.updateComboQty = updateComboQty;
window.updateCartNote = updateCartNote;
window.updateComboNote = updateComboNote;
window.removeFromCart = removeFromCart;
window.removeComboFromCart = removeComboFromCart;
window.toggleCart = toggleCart;
window.closeCart = closeCart;
window.checkoutFromCart = checkoutFromCart;
window.openConfirm = openConfirm;
window.closeConfirm = closeConfirm;
window.confirmOrder = confirmOrder;
window.filterByName = filterByName;
window.reloadWithSort = reloadWithSort;