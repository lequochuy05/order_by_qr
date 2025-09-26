// static/js/menu.js
const BASE_URL = window.APP_BASE_URL || location.origin;

let tableId = null;
let cart = {};                 // { [menuItemId]: { qty, note, name, price } }
let selectedCombos = {};       // { comboId: qty }
let combosCache = [];          // [{id, name, price, items:[{menuItemId, quantity, name}]}]

// ===== Helpers =====
function toImgUrl(u = "") {
  try {
    if (!u) return "";
    if (u.startsWith("http://") || u.startsWith("https://")) return u;
    if (u.startsWith("/")) return new URL(u, BASE_URL).href;
    return new URL("/" + u, BASE_URL).href;
  } catch {
    return "";
  }
}
function safeText(s = "") {
  return String(s).replace(/[&<>"']/g, m => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[m]));
}
function money(v){ return Number(v||0).toLocaleString('vi-VN'); }

// ===== Boot =====
document.addEventListener('DOMContentLoaded', async () => {
  const urlParams = new URLSearchParams(window.location.search);
  tableId = urlParams.get('tableId');
  const categoryId = urlParams.get('categoryId') || 'all';
  if (!tableId) { alert("Không tìm thấy tableId trong URL!"); return; }

  try {
    const res = await fetch(`${BASE_URL}/api/tables/${tableId}`);
    const table = res.ok ? await res.json() : null;
    document.getElementById('tableNumber').textContent = table?.tableNumber ?? tableId;
  } catch {
    document.getElementById('tableNumber').textContent = tableId;
  }

  // clear giỏ phiên trước mỗi lần vào menu của bàn này
  sessionStorage.removeItem(`qr-cart:table-${tableId}`);
  cart = {};

  await loadCategories(categoryId);
  await loadCombos();            // nạp danh sách combo
  connectWS();
});

// ===== Categories =====
async function loadCategories(selected) {
  try {
    const res = await fetch(`${BASE_URL}/api/categories`);
    const categories = res.ok ? await res.json() : [];
    const select = document.getElementById('categoryFilter');
    select.innerHTML = `<option value="all">Tất cả</option>`;
    categories.forEach(cat => {
      const option = document.createElement('option');
      option.value = cat.id;
      option.textContent = cat.name;
      select.appendChild(option);
    });
    select.value = selected || 'all';
    await loadMenu(select.value);
  } catch {
    alert("Không tải được danh mục!");
  }
}

// ===== Filter & Sort =====
function filterByName(q){
  q = (q || '').toLowerCase().trim();
  const items = document.querySelectorAll('#menuItems .menu-item');
  items.forEach(it=>{
    const name = it.querySelector('.details .name')?.textContent?.toLowerCase() || '';
    it.style.display = name.includes(q) ? '' : 'none';
  });
}
window.filterByName = filterByName;

function reloadWithSort(){
  const select = document.getElementById('categoryFilter');
  loadMenu(select.value);
}
window.reloadWithSort = reloadWithSort;

// ===== Menu list =====
async function loadMenu(categoryId) {
  const container = document.getElementById("menuItems");
  container.innerHTML = "Đang tải...";

  const url = categoryId === "all"
    ? `${BASE_URL}/api/menu`
    : `${BASE_URL}/api/menu/category/${encodeURIComponent(categoryId)}`;

  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error(await res.text());
    let items = await res.json();

    // sort theo lựa chọn
    const sort = document.getElementById('sortBy')?.value || 'default';
    if (sort === 'price-asc')  items.sort((a,b) => (a.price||0)-(b.price||0));
    if (sort === 'price-desc') items.sort((a,b) => (b.price||0)-(a.price||0));

    container.innerHTML = "";
    if (!Array.isArray(items) || items.length === 0) {
      container.innerHTML = `<div style="text-align:center;color:#666;padding:16px;">Chưa có món nào.</div>`;
      return;
    }

    const bust = Date.now(); // cache-busting

    items.forEach(item => {
      const id = String(item.id);
      const name = item.name || '';
      const price = Number(item.price || 0);

      const raw = item.img || "";
      const imgFull = toImgUrl(raw);
      const imgSrc = imgFull ? `${imgFull}${imgFull.includes("?") ? "&" : "?"}v=${bust}` : "";

      if (!cart[id]) {
        cart[id] = { qty: 0, note: "", name, price };
      } else {
        cart[id].name  = name;
        cart[id].price = price;
      }

      const { qty, note } = cart[id];

      const wrap = document.createElement('div');
      wrap.className = 'menu-item';
      wrap.innerHTML = `
        <img src="${imgSrc}" alt="${safeText(name)}" loading="lazy">
        <div class="details">
          <div class="name">${safeText(name)}</div>
          <div class="price">${money(price)} VND</div>
        </div>
        <div class="actions">
          <div class="quantity">
            <button onclick="updateQuantity('${id}', -1)">-</button>
            <span id="qty-${id}">${qty}</span>
            <button onclick="updateQuantity('${id}', 1)">+</button>
          </div>
          <button class="note-toggle-btn" onclick="toggleNote('${id}')">Thêm ghi chú</button>
          <div class="note" id="note-box-${id}" style="display:${note ? 'block':'none'};">
            <input type="text" id="note-${id}" placeholder="..." value="${safeText(note)}"
                  oninput="updateNote('${id}', this.value)">
          </div>
        </div>
      `;
      container.appendChild(wrap);
    });

  } catch (e) {
    console.error(e);
    container.innerHTML = `<div style="color:#e11d48;">Không tải được thực đơn.</div>`;
  }
}

// ===== Note & Qty =====
function toggleNote(itemId) {
  const noteBox = document.getElementById(`note-box-${itemId}`);
  noteBox.style.display = (noteBox.style.display === 'block') ? 'none' : 'block';
}
function updateNote(itemId, value){
  if (!cart[itemId]) cart[itemId] = { qty:0, note:"" };
  cart[itemId].note = value;
  persistCart();
}
function updateQuantity(itemId, change) {
  if (!cart[itemId]) cart[itemId] = { qty:0, note:"" };
  let qty = cart[itemId].qty + change;
  if (qty < 0) qty = 0;
  cart[itemId].qty = qty;
  const el = document.getElementById(`qty-${itemId}`);
  if (el) el.textContent = qty;
  persistCart();
}

// ===== Combos UI =====
async function loadCombos(){
  const wrap = document.getElementById('comboItems');
  if (!wrap) return;
  wrap.innerHTML = 'Đang tải combo...';
  combosCache = [];

  try {
    const res = await fetch(`${BASE_URL}/api/combos`);
    const combos = res.ok ? await res.json() : [];
    combosCache = combos;

    if (!Array.isArray(combos) || combos.length === 0) {
      wrap.innerHTML = '<div style="color:#666;">Chưa có combo.</div>';
      return;
    }

    wrap.innerHTML = '';
    combos.forEach(c => {
      const qty  = selectedCombos[c.id]?.qty || 0;
      const note = selectedCombos[c.id]?.note || "";

      const div = document.createElement('div');
      div.className = 'combo-item';

      div.innerHTML = `
        
        <div class="details">
          <div class="name">Combo ${safeText(c.name)}</div>
          <div class="price">${money(c.price)} VND</div>
        </div>
        <div class="actions">
          <div class="quantity">
            <button onclick="updateCombo(${c.id}, -1)">-</button>
            <span id="combo-qty-${c.id}">${qty}</span>
            <button onclick="updateCombo(${c.id}, 1)">+</button>
          </div>
          <button class="note-toggle-btn" onclick="toggleNote('combo-${c.id}')">Thêm ghi chú</button>
          <div class="note" id="note-box-combo-${c.id}" style="display:${note ? 'block':'none'};">
            <input type="text" id="note-combo-${c.id}" placeholder="..." value="${safeText(note)}"
                  oninput="updateComboNote(${c.id}, this.value)">
          </div>
        </div>
      `;
      wrap.appendChild(div);
    });
  } catch (e){
    console.error(e);
    wrap.innerHTML = '<div style="color:#e11d48;">Không tải được combo.</div>';
  }
}

function addCombo(id) {
  if (!selectedCombos[id]) selectedCombos[id] = { qty: 0, note: "" };
  selectedCombos[id].qty++;
  loadCombos();
}

function removeCombo(id) {
  if (!selectedCombos[id]) return;
  selectedCombos[id].qty--;
  if (selectedCombos[id].qty <= 0) delete selectedCombos[id];
  loadCombos();
}

function updateCombo(id, change){
  if (!selectedCombos[id]) selectedCombos[id] = { qty:0, note:"" };
  let qty = selectedCombos[id].qty + change;
  if (qty < 0) qty = 0;
  selectedCombos[id].qty = qty;
  const el = document.getElementById(`combo-qty-${id}`);
  if (el) el.textContent = qty;
}

function updateComboNote(id, value){
  if (!selectedCombos[id]) selectedCombos[id] = { qty:0, note:"" };
  selectedCombos[id].note = value;
}

// ===== Modal confirm =====
async function openConfirm(){
  const items = Object.entries(cart).filter(([_,v]) => (v.qty||0) > 0);
  const combosSelected = Object.entries(selectedCombos).filter(([_,v]) => (v.qty||0) > 0);
  if (items.length === 0 && combosSelected.length === 0) {
    alert("Vui lòng chọn ít nhất 1 món hoặc 1 combo!");
    return;
  }

  const box = document.getElementById('confirmList');
  box.innerHTML = '';

  // render món lẻ
  items.forEach(([id, v]) => {
    const qty   = v.qty || 0;
    const note  = v.note || '';
    const name  = v.name || `#${id}`;
    const price = Number(v.price || 0);
    const line  = qty * price;

    const div = document.createElement('div');
    div.className = 'confirm-row';
    div.innerHTML = `
      <div class="left">
        <div class="name">${safeText(name)} × ${qty}</div>
        ${note ? `<div class="note">Ghi chú: ${safeText(note)}</div>` : ''}
      </div>
      <div class="right">
        <div>${money(price)} VND</div>
        <div><b>${money(line)} VND</b></div>
      </div>
    `;
    box.appendChild(div);
  });

  // render combo
  combosSelected.forEach(([comboId, v]) => {
    const combo = combosCache.find(c => c.id == comboId);
    if (!combo) return;
    const qty = v.qty;
    const note = v.note || "";

    const div = document.createElement('div');
    div.className = 'confirm-row';
    div.innerHTML = `
      <div class="left">
        <div class="name">Combo ${safeText(combo.name)} × ${qty}</div>
        ${note ? `<div class="note">Ghi chú: ${safeText(note)}</div>` : ""}
      </div>
      <div class="right">
        <div>${money(combo.price)} VND</div>
        <div><b>${money(combo.price * qty)} VND</b></div>
      </div>
    `;
    box.appendChild(div);
  });

  // chuẩn bị dữ liệu preview
  const orderItems = items.map(([id,v]) => ({
    menuItemId: parseInt(id,10),
    quantity: v.qty||0,
    notes: v.note||null
  }));

  const comboRequests = combosSelected.map(([id, v]) => ({
    comboId: Number(id),
    quantity: v.qty,
    notes: v.note || null
  }));

  let p = null;
  try {
    const res = await fetch(`${BASE_URL}/api/orders/preview`, {
      method: "POST",
      headers: {"Content-Type":"application/json"},
      body: JSON.stringify({
        tableId: Number(tableId),
        status: "PENDING",
        items: orderItems,
        combos: comboRequests   // ✅ gửi đúng format mới
      })
    });
    if (res.ok) {
      p = await res.json();
    }
  } catch(e){
    console.warn("preview error", e);
  }

  if (p) {
    document.getElementById('subtotalItems').textContent  = money(p.subtotalItems || 0);
    document.getElementById('subtotalCombos').textContent = money(p.subtotalCombos || 0);
    document.getElementById('confirmSubtotal').textContent= money((p.subtotalItems||0)+(p.subtotalCombos||0));
    document.getElementById('confirmTotal').textContent   = money(p.finalTotal || 0);
  } else {
    // fallback tính tay từ cart
    let itemsSubtotal = items.reduce((s, [_,v]) => s + (v.qty||0)*(v.price||0), 0);
    let combosSubtotal = combosSelected.reduce((s, [id,v])=>{
      const c = combosCache.find(x => x.id == id);
      return s + (c?.price||0)*(v.qty||0);
    },0);
    const total = itemsSubtotal + combosSubtotal;

    document.getElementById('subtotalItems').textContent  = money(itemsSubtotal);
    document.getElementById('subtotalCombos').textContent = money(combosSubtotal);
    document.getElementById('confirmSubtotal').textContent= money(total);
    document.getElementById('confirmTotal').textContent   = money(total);
  }

  document.getElementById('confirmModal').classList.remove('hidden');
}

function closeConfirm(){
  document.getElementById('confirmModal').classList.add('hidden');
}

// ===== Submit order =====
async function confirmOrder(){
  const btn = document.getElementById('confirmBtn');
  btn.disabled = true;

  try {
    const orderItems = Object.keys(cart).map(id => ({
      menuItemId: parseInt(id, 10),
      quantity: cart[id].qty || 0,
      notes: cart[id].note || null
    })).filter(i => i.quantity > 0);

    // Đúng format cho backend
    const comboRequests = Object.entries(selectedCombos).map(([id, v]) => ({
      comboId: Number(id),
      quantity: v.qty,
      notes: v.note || null
    }));

    const res = await fetch(`${BASE_URL}/api/orders`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        tableId: Number(tableId),
        status: "PENDING",
        items: orderItems,
        combos: comboRequests   
      })
    });

    if (!res.ok) {
      const msg = await res.text();
      alert(`Đặt món thất bại (${res.status}): ${msg}`);
      return;
    }

    const data = await res.json();
    closeConfirm();
    alert("Đặt món thành công! Mã đơn: " + (data.id ?? ''));

    cart = {};
    selectedCombos = {};
    persistCart();
    window.location.href = `/dashboard.html?tableId=${encodeURIComponent(tableId)}`;
  } catch (err) {
    alert("Lỗi đặt món: " + err.message);
  } finally {
    btn.disabled = false;
  }
}

// ===== Persist cart =====
function persistCart(){
  try { sessionStorage.setItem(`qr-cart:table-${tableId}`, JSON.stringify(cart)); } catch {}
}
function restoreCart(){
  try{
    const raw = sessionStorage.getItem(`qr-cart:table-${tableId}`);
    if (raw) cart = JSON.parse(raw) || {};
  } catch {}
}

// ===== WebSocket =====
function connectWS() {
  try {
    const socket = new SockJS(`${BASE_URL}/ws`);
    const stomp  = Stomp.over(socket);
    stomp.debug = () => {};

    let retry = 0;
    const reconnect = () =>
      setTimeout(connectWS, Math.min(30000, 1000 * Math.pow(2, Math.min(6, ++retry))));

    stomp.connect({}, () => {
      retry = 0;

      stomp.subscribe('/topic/categories', () => {
        const sel = document.getElementById('categoryFilter');
        const current = sel?.value || 'all';
        setTimeout(() => loadCategories(current), 250);
      });

      stomp.subscribe('/topic/menu', () => {
        const sel = document.getElementById('categoryFilter');
        setTimeout(() => loadMenu(sel?.value || 'all'), 250);
      });

      stomp.subscribe('/topic/combos', () => {
        setTimeout(loadCombos, 250);
      });
    }, reconnect);
  } catch (e) {
    console.warn('WS connect error:', e);
  }
}

// Expose
window.updateQuantity = updateQuantity;
window.updateNote = updateNote;
window.toggleNote = toggleNote;
window.openConfirm = openConfirm;
window.closeConfirm = closeConfirm;
window.confirmOrder = confirmOrder;
window.reloadWithSort = reloadWithSort;
window.filterByName = filterByName;
window.addCombo = addCombo;
window.removeCombo = removeCombo;
