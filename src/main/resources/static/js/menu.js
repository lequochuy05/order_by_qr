// static/js/menu.js
const BASE_URL = window.APP_BASE_URL || location.origin;

let tableId = null;
let cart = {};            // { [id]: { qty, note, name, price } }
let priceMap = {};       

// ===== Helpers =====
/** Chuẩn hoá đường dẫn ảnh (hỗ trợ /uploads, uploads, http(s)://) */
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
  priceMap = {};

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

    const bust = Date.now(); // cache-busting giống dashboard

    items.forEach(item => {
      const id = String(item.id);
      const name = item.name || '';
      const price = Number(item.price || 0);

      // build ảnh với cache-busting
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
      const priceText = price.toLocaleString('vi-VN');

      const wrap = document.createElement('div');
      wrap.className = 'menu-item';
      wrap.innerHTML = `
        <img src="${imgSrc}" alt="${safeText(name)}" loading="lazy">
        <div class="details">
          <div class="name">${safeText(name)}</div>
          <div class="price">${priceText} VND</div>
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

  } catch {
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

// ===== Modal confirm =====
function openConfirm(){
  const items = Object.entries(cart).filter(([_,v]) => (v.qty||0) > 0);
  if (items.length === 0) { alert("Vui lòng chọn ít nhất 1 món!"); return; }

  let total = 0;
  const box = document.getElementById('confirmList');
  box.innerHTML = '';

  items.forEach(([id, v]) => {
    const qty   = v.qty || 0;
    const note  = v.note || '';
    const name  = v.name || `#${id}`;
    const price = Number(v.price || 0);
    const line  = qty * price;
    total += line;

    const div = document.createElement('div');
    div.className = 'confirm-row';
    div.innerHTML = `
      <div class="left">
        <div class="name">${safeText(name)} × ${qty}</div>
        ${note ? `<div class="note">Ghi chú: ${safeText(note)}</div>` : ''}
      </div>
      <div class="right">
        <div>${price.toLocaleString('vi-VN')} VND</div>
        <div><b>${line.toLocaleString('vi-VN')} VND</b></div>
      </div>
    `;
    box.appendChild(div);
  });

  document.getElementById('confirmTotal').textContent = total.toLocaleString('vi-VN');
  document.getElementById('confirmModal').classList.remove('hidden');
}
function closeConfirm(){
  document.getElementById('confirmModal').classList.add('hidden');
}

async function confirmOrder(){
  const btn = document.getElementById('confirmBtn');
  btn.disabled = true;

  try {
    const orderItems = Object.keys(cart).map(id => ({
      menuItemId: parseInt(id, 10),
      quantity: cart[id].qty || 0,
      notes: cart[id].note || null
    })).filter(i => i.quantity > 0);

    const res = await fetch(`${BASE_URL}/api/orders`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        tableId: Number(tableId),
        status: "PENDING",
        items: orderItems
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

// ===== WebSocket (giống dashboard: delay 250ms rồi reload) =====
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

      // categories thay đổi -> refetch categories (giữ lựa chọn hiện tại)
      stomp.subscribe('/topic/categories', () => {
        const sel = document.getElementById('categoryFilter');
        const current = sel?.value || 'all';
        setTimeout(() => loadCategories(current), 250);
      });

      // menu thay đổi -> reload danh sách món theo bộ lọc hiện tại
      stomp.subscribe('/topic/menu', () => {
        const sel = document.getElementById('categoryFilter');
        setTimeout(() => loadMenu(sel?.value || 'all'), 250);
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
