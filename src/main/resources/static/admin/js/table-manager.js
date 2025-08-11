// /admin/js/table-manager.js

if (role === "MANAGER") {
    document.getElementById("adminActions").style.display = "block";
}
const BASE_URL = window.APP_BASE_URL || location.origin;

// ===== state =====
let _isFirstLoad = true;                 // nháy khi reload lần đầu
const _prevOrders = {};                  // { [tableId]: { totalAmount, itemCount } }
let _stomp = null;                       // websocket client
let _renderToken = 0;                    // chặn render chồng

// ===== helpers =====
const $id = (s) => document.getElementById(s);
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

// Parse JSON an toàn (body rỗng => null)
async function jsonOrNull(res) {
  const raw = await res.text();
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

// highlight tiện dụng (đã có CSS .highlight trong style.css)
function highlightCard(cardEl) {
  if (!cardEl) return;
  cardEl.classList.add('highlight');
  setTimeout(() => cardEl.classList.remove('highlight'), 5000);
}

// ===== render =====
window.loadTables = async function () {
  const myToken = ++_renderToken;

  const filter = $id('tableFilter')?.value || 'ALL';
  const container = $id('tableContainer');
  container.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Đang tải...</div>`;

  try {
    const res = await $fetch(`${BASE_URL}/api/tables`);
    if (!res.ok) throw new Error(await $readErr(res));
    const tables = await res.json();
    if (myToken !== _renderToken) return;

    container.innerHTML = '';

    const list = tables
      .filter(t => filter === 'ALL' || t.status === filter)
      .sort((a, b) => Number(a.tableNumber) - Number(b.tableNumber));

    if (list.length === 0) {
      container.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Không có bàn phù hợp.</div>`;
      _isFirstLoad = false;
      return;
    }

    for (const t of list) {
      if (myToken !== _renderToken) return;

      const card = document.createElement('div');
      card.className = 'table-card';
      card.dataset.tableId = t.id;
      card.innerHTML = `
        <h3>Bàn ${t.tableNumber}</h3>
        <p class="status">Trạng thái: <strong>${t.status}</strong></p>
        <p class="total">Tổng tiền: <span class="total-amount">0 VND</span></p>
        <div class="actions">
          <button class="btn btn-detail" onclick="showDetails(${t.id})">Chi tiết</button>
          <span class="pay-slot"></span>
          ${window.role === 'MANAGER' ? `
            <div style="display:inline-flex;gap:8px;flex-wrap:wrap;margin-left:8px;">
              <button class="btn" onclick="showEditTable(${t.id})">✏️</button>
              <button class="btn" onclick="deleteTable(${t.id})">🗑️</button>
            </div>` : ''
          }
        </div>
      `;
      container.appendChild(card);

      try {
        const r = await $fetch(`${BASE_URL}/api/orders/table/${t.id}/current`);

        if (r.status === 404 || r.status === 204) {
          if (_isFirstLoad && t.status === 'Đang phục vụ') highlightCard(card);
          _prevOrders[t.id] = { totalAmount: 0, itemCount: 0 };
          continue;
        }

        if (r.ok) {
          const order = await jsonOrNull(r);
          if (!order) {
            if (_isFirstLoad && t.status === 'Đang phục vụ') highlightCard(card);
            _prevOrders[t.id] = { totalAmount: 0, itemCount: 0 };
            continue;
          }

          const total = order.totalAmount ?? 0;
          const count = Array.isArray(order.orderItems) ? order.orderItems.length : 0;

          card.querySelector('.total-amount').textContent =
            `${Number(total).toLocaleString('vi-VN')} VND`;

          const paySlot = card.querySelector('.pay-slot');
          if (paySlot) {
            paySlot.innerHTML =
              `<button class="btn btn-pay" onclick="pay(${order.id}, ${t.id})">Thanh toán</button>`;
          }

          const prev = _prevOrders[t.id];
          const changed = !prev || prev.totalAmount !== total || prev.itemCount !== count;
          if (_isFirstLoad && (t.status === 'Đang phục vụ' || total > 0 || count > 0)) highlightCard(card);
          if (!_isFirstLoad && changed) highlightCard(card);

          _prevOrders[t.id] = { totalAmount: total, itemCount: count };
        } else {
          _prevOrders[t.id] = { totalAmount: 0, itemCount: 0 };
        }
      } catch {
        // bỏ qua lỗi lẻ của từng bàn
      }
    }

    _isFirstLoad = false;
  } catch (e) {
    if (myToken !== _renderToken) return;
    container.innerHTML = `<div style="text-align:center;color:#ef4444;padding:16px;">Không thể tải danh sách bàn</div>`;
    console.error(e);
  }
};

// Order detail
window.showDetails = async function (tableId) {
  window.currentOpenTableId = tableId;
  const modal = $id('modal');
  const body  = $id('modalBody');
 

  body.innerHTML = `<div style="text-align:center;color:#6b7280;padding:12px;">Đang tải...</div>`;
  modal.style.display = 'flex';

  try {
    const res = await $fetch(`${BASE_URL}/api/orders/table/${tableId}/current`);

    if (res.status === 404 || res.status === 204) {
      body.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Không có đơn hiện tại</div>`;
      return;
    }

    if (!res.ok) {
      const m = await $readErr(res);
      body.innerHTML = `<div style="text-align:center;color:#ef4444;padding:16px;">${m || 'Lỗi tải chi tiết đơn'}</div>`;
      return;
    }

    const order = await jsonOrNull(res);
    const items = Array.isArray(order?.orderItems) ? order.orderItems : [];

    if (items.length === 0) {
      body.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Không có món trong đơn</div>`;
      return;
    }

    body.innerHTML = items.map(it => `
      <div class="order-item ${it.prepared ? 'prepared' : ''}">
        <strong>${it.menuItem?.name || ''} x${it.quantity}</strong>
        ${it.notes ? `<div class="order-note">Ghi chú: ${it.notes}</div>` : ''}

        <div style="display:flex; gap:8px; align-items:center;">
          ${it.prepared
            ? `<span class="status-prepared">Đã làm</span>`
            : `<button class="btn-prepared" onclick="markPrepared(${it.id})">Đã xong</button>
              <button class="btn-cancel-item" onclick="cancelItem(${it.id})">Hủy món</button>`}
        </div>
      </div>
    `).join('');
  } catch (e) {
    body.innerHTML = `<div style="text-align:center;color:#ef4444;padding:16px;">${e.message || 'Lỗi kết nối'}</div>`;
  }
};
window.closeModal = function () { $id('modal').style.display = 'none'; };

// Hủy món
window.cancelItem = async function(itemId){
  if (!confirm('Hủy món này?')) return;
  try{
    const res = await $fetch(`${BASE_URL}/api/orders/items/${itemId}`, { method:'DELETE' });
    if (!res.ok) throw new Error(await $readErr(res));

    // Reload lại modal & danh sách (WS cũng sẽ bắn)
    if (typeof window.currentOpenTableId === 'number') {
      showDetails(window.currentOpenTableId);
    } else {
      loadTables();
    }
  }catch(e){
    alert(e.message || 'Hủy món thất bại');
  }
};

// Mark item as prepared
window.markPrepared = async function(itemId){
    try{
      await $fetch(`${BASE_URL}/api/orders/items/${itemId}/prepared`, { method:'PUT' });
      // Làm tươi modal nếu đang mở
      const opened = document.getElementById('modal').style.display === 'flex';
      if (opened && typeof window.currentOpenTableId === 'number') {
        showDetails(window.currentOpenTableId); // re-render chi tiết
      }
      // Bảng trạng thái/tiền sẽ tự cập nhật do WS "/topic/tables"
    }catch(e){ /* ignore */ }
};

// Thanh toán đơn hàng
window.pay = async function (orderId, tableId) {
  const uid = localStorage.getItem('userId');
  if (!uid) { alert('Không xác định được người dùng'); return; }
  try {
    const res = await $fetch(`${BASE_URL}/api/orders/${orderId}/pay?userId=${uid}`, { method: 'PUT' });
    if (!res.ok) throw new Error(await $readErr(res));
    alert('Thanh toán thành công!');
    // WS sẽ bắn sự kiện và loadTables() sẽ chạy lại
  } catch (e) {
    alert(e.message || 'Thanh toán thất bại');
  }
};

// =========================
// CRUD BÀN (Add / Edit / Delete)
// =========================

// --- Thêm bàn ---
window.showAddTable = function () {
  if (window.role && window.role !== 'MANAGER') return;
  $id('newTableNumber').value = '';
  $id('newTableCapacity').value = '';
  const err = $id('addTableError');
  err.textContent = '';
  err.style.display = 'none';
  $id('addTableModal').style.display = 'flex';
};

window.closeAddTableModal = function () {
  $id('addTableModal').style.display = 'none';
};

window.submitNewTable = async function () {
  if (window.role && window.role !== 'MANAGER') return;

  const number = ($id('newTableNumber').value || '').trim();
  const capacity = Number($id('newTableCapacity').value || 0);
  const err = $id('addTableError');

  err.textContent = '';
  err.style.display = 'none';

  // validate đơn giản
  if (!number || isNaN(Number(number)) || capacity <= 0) {
    err.textContent = 'Vui lòng nhập số bàn hợp lệ và sức chứa > 0';
    err.style.display = 'block';
    return;
  }

  try {
    const res = await $fetch(`${BASE_URL}/api/tables`, {
      method: 'POST',
      body: JSON.stringify({
        tableNumber: String(number),
        capacity: capacity,
        status: 'Trống',
        qrCodeUrl: '' // cho phép rỗng
      })
    });

    if (!res.ok) {
      const m = await $readErr(res);
      throw new Error(m || 'Lỗi khi thêm bàn');
    }

    closeAddTableModal();
    loadTables();
  } catch (e) {
    err.textContent = e.message || 'Đã xảy ra lỗi';
    err.style.display = 'block';
  }
};

// --- Sửa bàn ---
let _currentEditTableId = null;

window.showEditTable = async function (id) {
  if (window.role && window.role !== 'MANAGER') return;

  const err = $id('editTableError');
  err.textContent = '';
  err.style.display = 'none';

  try {
    const res = await $fetch(`${BASE_URL}/api/tables/${id}`);
    if (!res.ok) throw new Error(await $readErr(res));
    const t = await res.json();

    _currentEditTableId = id;
    $id('editTableNumber').value = t.tableNumber ?? '';
    $id('editQrCodeUrl').value  = t.qrCodeUrl ?? '';
    $id('editStatus').value     = t.status ?? 'Trống';
    $id('editCapacity').value   = t.capacity ?? 1;

    $id('editTableModal').style.display = 'flex';
  } catch (e) {
    alert(e.message || 'Không lấy được thông tin bàn');
  }
};

window.closeEditTableModal = function () {
  _currentEditTableId = null;
  $id('editTableModal').style.display = 'none';
};

window.submitEditTable = async function () {
  if (window.role && window.role !== 'MANAGER') return;
  if (!_currentEditTableId) return;

  const status = $id('editStatus').value;
  const capacity = Number($id('editCapacity').value || 0);
  const err = $id('editTableError');

  err.textContent = '';
  err.style.display = 'none';

  if (!status || capacity <= 0) {
    err.textContent = 'Vui lòng nhập trạng thái và sức chứa hợp lệ';
    err.style.display = 'block';
    return;
  }

  try {
    const res = await $fetch(`${BASE_URL}/api/tables/${_currentEditTableId}`, {
      method: 'PUT',
      body: JSON.stringify({ status, capacity })
    });

    if (!res.ok) throw new Error(await $readErr(res));

    closeEditTableModal();
    loadTables();
  } catch (e) {
    err.textContent = e.message || 'Lỗi cập nhật';
    err.style.display = 'block';
  }
};

// --- Xóa bàn ---
window.deleteTable = async function (id) {
  if (window.role && window.role !== 'MANAGER') return;
  if (!confirm('Bạn có chắc muốn xóa bàn này?')) return;

  try {
    const res = await $fetch(`${BASE_URL}/api/tables/${id}`, { method: 'DELETE' });
    if (!res.ok) throw new Error(await $readErr(res));
    loadTables();
  } catch (e) {
    alert(e.message || 'Không thể xóa bàn');
  }
};


// ===== WS =====
function connectWebSocket() {
  try {
    const socket = new SockJS(`${BASE_URL}/ws`);
    _stomp = Stomp.over(socket);
    _stomp.debug = () => {};
    _stomp.connect({}, () => {
      _stomp.subscribe('/topic/tables', async () => {
        await sleep(200);  // chờ backend commit
        loadTables();
      });
    });
  } catch (e) {
    console.warn('WS connect error:', e);
  }
}

// ===== boot =====
window.addEventListener('DOMContentLoaded', () => {
  loadTables();
  connectWebSocket();
});
