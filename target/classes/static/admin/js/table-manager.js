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

const fmtVND = n => Number(n || 0).toLocaleString('vi-VN') + ' VND';

// Lưu snapshot đơn trước khi thanh toán để in hóa đơn
let _payContext = {
  order: null,      // dữ liệu order snapshot
  table: null,      // dữ liệu table (số bàn)
  paidBy: null,     // tên người thu (localStorage.fullname)
  paidAt: null      // thời điểm thanh toán
};


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
            ? `<span class="status-prepared">Đã phục vụ</span>`
            : `
              <button class="btn-prepared" onclick="markPrepared(${it.id})">Đã xong</button>
              <button class="btn-cancel-item" onclick="cancelItem(${it.id})">Hủy món</button>
              <button class="btn-edit-item" onclick="openEditItem(${it.id}, ${it.quantity}, '${it.notes || ''}')">Sửa đơn</button>
            `}
        </div>
      </div>
    `).join('');
  } catch (e) {
    body.innerHTML = `<div style="text-align:center;color:#ef4444;padding:16px;">${e.message || 'Lỗi kết nối'}</div>`;
  }
};
window.closeModal = function () { $id('modal').style.display = 'none'; };

// ====== Sửa món ======
window.openEditItem = function (itemId, qty, notes) {
  $id('editModal').classList.remove('hidden');
  $id('editQuantity').value = qty;
  $id('editNotes').value = notes || '';
  window._currentEditItemId = itemId;
};

window.closeEditModal = function () {
  $id('editModal').classList.add('hidden');
  window._currentEditItemId = null;
};

window.saveEdit = async function () {
  const itemId = window._currentEditItemId;
  const qty = Number($id('editQuantity').value);
  const notes = $id('editNotes').value;

  if (!qty || qty <= 0) {
    alert("Số lượng phải > 0");
    return;
  }

  try {
    const res = await $fetch(`${BASE_URL}/api/orders/items/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity: qty, notes })
    });
    if (!res.ok) throw new Error(await $readErr(res));

    closeEditModal();

    // Reload lại modal chi tiết
    if (typeof window.currentOpenTableId === 'number') {
      showDetails(window.currentOpenTableId);
    }
  } catch (e) {
    alert(e.message || 'Sửa món thất bại');
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
// ===== Thanh toán: mở modal xác nhận =====
window.pay = async function (orderId, tableId) {
  const uid = localStorage.getItem('userId');
  if (!uid) { showError?.('Không xác định được người dùng', 'Lỗi'); return; }

  // Reset UI modal
  _payContext = { order: null, table: null, paidBy: localStorage.getItem('fullname') || '—', paidAt: null };
  const payModal = $id('payModal');
  const infoEl   = $id('payInfo');
  const itemsEl  = $id('payItems');
  const sumEl    = $id('paySummary');
  const errEl    = $id('payError');
  const btnOK    = $id('btnConfirmPay');
  const btnInv   = $id('btnViewInvoice');

  itemsEl.innerHTML = '';
  sumEl.textContent = '';
  errEl.style.display = 'none';
  btnOK.style.display = 'inline-block';
  btnInv.style.display = 'none';

  payModal.style.display = 'flex';
  infoEl.innerHTML = `<div style="color:#6b7280;">Đang tải thông tin đơn hàng...</div>`;

  try {
    // Lấy thông tin bàn (để hiển thị số bàn)
    const resTable = await $fetch(`${BASE_URL}/api/tables/${tableId}`);
    const table = resTable.ok ? await resTable.json() : { tableNumber: tableId };
    _payContext.table = table;

    // Lấy order hiện tại của bàn
    const res = await $fetch(`${BASE_URL}/api/orders/table/${tableId}/current`);
    if (res.status === 404 || res.status === 204) {
      infoEl.innerHTML = `<span style="color:#ef4444;">Bàn ${table.tableNumber}: không có đơn hiện tại.</span>`;
      btnOK.style.display = 'none';
      return;
    }
    if (!res.ok) {
      const m = await $readErr(res);
      infoEl.innerHTML = `<span style="color:#ef4444;">${m || 'Không tải được đơn hiện tại'}</span>`;
      btnOK.style.display = 'none';
      return;
    }

    const order = await jsonOrNull(res);
    _payContext.order = order; // lưu snapshot để in

    // Render phần đầu
    infoEl.innerHTML = `
      <div><strong>Bàn:</strong> ${table.tableNumber}</div>
      <div><strong>Mã đơn:</strong> ${order?.id ?? '—'}</div>
    `;

    // Render danh sách món
    const rows = (order?.orderItems || []).map(it => {
      const name = it?.menuItem?.name ?? '(Món)';
      const price = it?.menuItem?.price ?? 0;
      const qty = it?.quantity ?? 0;
      const notes = it?.notes ? `<div style="font-size:12px;color:#6b7280;">Ghi chú: ${it.notes}</div>` : '';
      const line = price * qty;
      return `
        <tr>
          <td style="padding:10px;border-bottom:1px solid #eee;">
            ${name} ${notes}
          </td>
          <td style="text-align:center;padding:10px;border-bottom:1px solid #eee;">${qty}</td>
          <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(price)}</td>
          <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(line)}</td>
        </tr>
      `;
    }).join('');

    itemsEl.innerHTML = rows || `
      <tr><td colspan="4" style="padding:12px;text-align:center;color:#6b7280;">Không có món</td></tr>
    `;

    const total = order?.totalAmount ?? (order?.orderItems || []).reduce((s,it)=>s+(it?.menuItem?.price||0)*(it?.quantity||0),0);
    sumEl.innerHTML = `Tổng thanh toán: <span style="font-size:18px;">${fmtVND(total)}</span>`;

    // Lưu id để confirm
    _payContext.orderId = orderId;
    _payContext.tableId = tableId;
  } catch (e) {
    infoEl.innerHTML = `<span style="color:#ef4444;">${e.message || 'Lỗi kết nối'}</span>`;
    $id('btnConfirmPay').style.display = 'none';
  }
};

window.closePayModal = function () { $id('payModal').style.display = 'none'; };


// ===== Xác nhận thanh toán =====
window.confirmPay = async function () {
  const uid = localStorage.getItem('userId');
  if (!uid) { showError?.('Không xác định được người dùng', 'Lỗi'); return; }

  const errEl  = $id('payError');
  const btnOK  = $id('btnConfirmPay');
  const btnInv = $id('btnViewInvoice');

  errEl.style.display = 'none';
  btnOK.disabled = true; btnOK.textContent = 'Đang xử lý...';

  try {
    const res = await $fetch(`${BASE_URL}/api/orders/${_payContext.orderId}/pay?userId=${uid}`, { method:'PUT' });
    if (!res.ok) throw new Error(await $readErr(res));

    _payContext.paidAt = new Date();

    // Hiển thị nút xem/in hóa đơn
    btnOK.style.display = 'none';
    btnInv.style.display = 'inline-block';
    showSuccess?.('Thanh toán thành công!', 'Thành công');

    // Đợi WS cập nhật, hoặc chủ động reload
    await sleep(200);
    loadTables();
  } catch (e) {
    errEl.textContent = e.message || 'Thanh toán thất bại';
    errEl.style.display = 'block';
  } finally {
    btnOK.disabled = false; btnOK.textContent = 'Xác nhận thanh toán';
  }
};

// ===== Xem / In hóa đơn =====
window.viewInvoice = function () {
  if (!_payContext?.order) return;

  const html = buildInvoiceHTML({
    order: _payContext.order,
    table: _payContext.table,
    paidBy: _payContext.paidBy,
    paidAt: _payContext.paidAt || new Date()
  });

  const win = window.open('', 'INVOICE', 'width=720,height=880');
  win.document.write(html);
  win.document.close();
  win.focus();
};

// Tạo HTML hóa đơn (in đẹp, A5/A4 đều ok)
function buildInvoiceHTML({ order, table, paidBy, paidAt }) {
  const items = order?.orderItems || [];
  const rows = items.map(it => {
    const name = it?.menuItem?.name ?? '';
    const price = it?.menuItem?.price ?? 0;
    const qty = it?.quantity ?? 0;
    const notes = it?.notes ? ` <em style="color:#6b7280;font-style:italic;">(${it.notes})</em>` : '';
    const line = price * qty;
    return `
      <tr>
        <td>${name}${notes}</td>
        <td style="text-align:center;">${qty}</td>
        <td style="text-align:right;">${fmtVND(price)}</td>
        <td style="text-align:right;">${fmtVND(line)}</td>
      </tr>
    `;
  }).join('');

  const total = order?.totalAmount ?? items.reduce((s,it)=>s+(it?.menuItem?.price||0)*(it?.quantity||0),0);
  const timeStr = new Date(paidAt).toLocaleString('vi-VN');

  return `
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>Hóa đơn - Bàn ${table?.tableNumber ?? ''}</title>
  <style>
    *{box-sizing:border-box} body{font:14px/1.5 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,"Noto Sans","Liberation Sans",sans-serif; color:#111827; padding:24px}
    .bill{max-width:720px;margin:0 auto}
    .brand{font-weight:800;font-size:20px}
    .muted{color:#6b7280}
    table{width:100%;border-collapse:collapse;margin-top:10px}
    th,td{padding:8px;border-bottom:1px solid #eee}
    th{background:#f8fafc;text-align:left}
    .tot{font-weight:700;font-size:16px}
    .right{text-align:right}
    @media print{ .no-print{display:none} body{padding:0} .bill{margin:0} }
  </style>
</head>
<body>
  <div class="bill">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div>
        <div class="brand">Sắc màu quán</div>
        <div class="muted">V328+9QR, Đại Minh, Đại Lộc, Quảng Nam</div>
        <div class="muted">Số điện thoại: 0706163387</div>
      </div>
      <div style="text-align:right">
        <div><strong>HÓA ĐƠN THANH TOÁN</strong></div>
        <div class="muted">Mã đơn: ${order?.id ?? ''}</div>
        <div class="muted">Bàn: ${table?.tableNumber ?? ''}</div>
        <div class="muted">Thời gian: ${timeStr}</div>
      </div>
    </div>

    <table>
      <thead>
        <tr>
          <th style="width:50%;">Món</th>
          <th style="width:10%;text-align:center;">SL</th>
          <th style="width:20%;text-align:right;">Đơn giá</th>
          <th style="width:20%;text-align:right;">Thành tiền</th>
        </tr>
      </thead>
      <tbody>
        ${rows || `<tr><td colspan="4" class="muted">Không có món</td></tr>`}
      </tbody>
      <tfoot>
        <tr>
          <td colspan="3" class="right tot">Tổng cộng</td>
          <td class="tot right">${fmtVND(total)}</td>
        </tr>
      </tfoot>
    </table>

    <div style="display:flex;justify-content:space-between;margin-top:18px">
      <div class="muted">Thu ngân: ${paidBy || '—'}</div>
      <div class="muted">Xin cảm ơn & hẹn gặp lại!</div>
    </div>

    <div style="margin-top:16px" class="no-print">
      <button style="padding: 8px 12px; background-color: #4CAF50; color: white; border: none; border-radius: 4px;" onclick="window.print()">In hóa đơn</button>
    </div>
  </div>
</body>
</html>
  `;
}


// CRUD BÀN (Add / Edit / Delete)

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
