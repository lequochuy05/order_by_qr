// /admin/js/table-manager.js

// Hiện nút admin nếu là quản lý
if (typeof role !== 'undefined' && role === "MANAGER") {
  const el = document.getElementById("adminActions");
  if (el) el.style.display = "block";
}

// ===== state =====
let _isFirstLoad = true;                 // nháy khi reload lần đầu
const _prevOrders = {};                  // { [tableId]: { totalAmount, itemCount } }
let _stomp = null;                       // websocket client
let _renderToken = 0;                    // chặn render chồng

// Lưu snapshot đơn trước khi thanh toán để in hóa đơn
let _payContext = {
  order: null,      // dữ liệu order snapshot
  table: null,      // dữ liệu table (số bàn)
  paidBy: null,     // tên người thu (localStorage.fullname)
  paidAt: null,     // thời điểm thanh toán
  orderId: null,
  tableId: null,
  voucherCode: null // mã áp dụng tạm thời khi preview thanh toán
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

// highlight card
function highlightCard(cardEl) {
  if (!cardEl) return;
  cardEl.classList.add('highlight');
  setTimeout(() => cardEl.classList.remove('highlight'), 5000);
}

// Cập nhật thông tin 1 card bàn
function updateTableCard(tableId, patch = {}) {
  const card = document.querySelector(`.table-card[data-table-id="${tableId}"]`);
  if (!card) return;

  if (patch.status) {
    const statusEl = card.querySelector(".status strong");
    if (statusEl) statusEl.textContent = patch.status;
  }
  if (patch.totalAmount !== undefined) {
    const totalEl = card.querySelector(".total-amount");
    if (totalEl) totalEl.textContent = `${Number(patch.totalAmount).toLocaleString('vi-VN')} VND`;
  }
  if (patch.highlight) highlightCard(card);
}


// ===== render danh sách bàn =====
window.loadTables = async function () {
  const myToken = ++_renderToken;

  const filter = $id('tableFilter')?.value || 'ALL';
  const container = $id('tableContainer');
  if (container) {
    container.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Đang tải...</div>`;
  }

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

// ===== chi tiết đơn của 1 bàn =====
window.showDetails = async function (tableId) {
  window.currentOpenTableId = tableId;
  const modal = $id('modal');
  const body  = $id('modalBody');

  if (!modal || !body) return;
  body.innerHTML = `<div style="text-align:center;color:#6b7280;padding:12px;">Đang tải...</div>`;
  modal.style.display = 'flex';

  try {
    const res = await $fetch(`${BASE_URL}/api/orders/table/${tableId}/current`);
    if (!res.ok) {
      body.innerHTML = `<div style="text-align:center;color:#ef4444;padding:16px;">Không tải được đơn</div>`;
      return;
    }

    const order = await jsonOrNull(res);
    if (!order || !order.orderItems?.length) {
      body.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Không có món trong đơn</div>`;
      return;
    }

    // Gom combo theo ID
    const comboMap = {};   // { comboId: { combo, qty, orderItemId, prepared } }
    const normalItems = [];

    order.orderItems.forEach(it => {
      if (it.combo) {
      const key = it.combo.id + '::' + (it.notes || '');
      if (!comboMap[key]) {
        comboMap[key] = {
          combo: it.combo,
          qty: 0,
          notes: it.notes || '',
          orderItemId: it.id,
          prepared: it.prepared
        };
      }
      comboMap[key].qty += it.quantity || 1;
      if (!it.prepared) comboMap[key].prepared = false;

        } else {
          normalItems.push(it);
        }
    });

    let html = "";

    // Render combo block
    Object.values(comboMap).forEach(c => {
    html += `
      <div class="order-item combo-block ${c.prepared ? 'prepared' : ''}">
        <strong>Combo ${c.combo.name} × ${c.qty}</strong>
        ${c.notes ? `<div class="order-note">Ghi chú: ${c.notes}</div>` : ''}
        <div style="display:flex;gap:8px;margin-top:6px;">
          ${c.prepared ? `<span class="status-prepared">Đã phục vụ</span>`
            : 
            `
              <button class="btn-prepared" onclick="markPrepared(${c.orderItemId})">Đã xong</button>
              <button class="btn-cancel-item" onclick="cancelItem(${c.orderItemId})">Hủy</button>
            `}
        </div>
      </div>`;
  });

    // Render món lẻ
    html += normalItems.map(it => `
      <div class="order-item ${it.prepared ? 'prepared' : ''}">
        <strong>${it.menuItem?.name || ''} × ${it.quantity}</strong>
        ${it.notes ? `<div class="order-note">Ghi chú: ${it.notes}</div>` : ''}
        <div style="display:flex; gap:8px;">
          ${it.prepared ? `<span class="status-prepared">Đã phục vụ</span>`
            : 
            `
              <button class="btn-prepared" onclick="markPrepared(${it.id})">Đã xong</button>
              <button class="btn-cancel-item" onclick="cancelItem(${it.id})">Hủy món</button>
              <button class="btn-edit-item" onclick="openEditItem(${it.id}, ${it.quantity}, '${it.notes || ''}')">Sửa đơn</button>
            `}
        </div>
      </div>
    `).join('');

    body.innerHTML = html;

  } catch (e) {
    body.innerHTML = `<div style="text-align:center;color:#ef4444;padding:16px;">${e.message}</div>`;
  }
};


window.closeModal = function () { const m = $id('modal'); if (m) m.style.display = 'none'; };

// ====== Sửa món ======
window.openEditItem = function (itemId, qty, notes) {
  $id('editModal')?.classList.remove('hidden');
  const q = $id('editQuantity'); if (q) q.value = qty;
  const n = $id('editNotes'); if (n) n.value = notes || '';
  window._currentEditItemId = itemId;
};

window.closeEditModal = function () {
  $id('editModal')?.classList.add('hidden');
  window._currentEditItemId = null;
};

window.saveEdit = async function () {
  const itemId = window._currentEditItemId;
  const qty = Number($id('editQuantity')?.value);
  const notes = $id('editNotes')?.value || '';

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
      updateTableCard(window.currentOpenTableId);
    }
  } catch (e) {
    alert(e.message || 'Sửa món thất bại');
  }
};

// ===== Hủy món =====
window.cancelItem = async function(itemId){
  if (!confirm('Hủy món này?')) return;
  try{
    const res = await $fetch(`${BASE_URL}/api/orders/items/${itemId}`, { method:'DELETE' });
    if (!res.ok) throw new Error(await $readErr(res));

    // Tải lại trạng thái bàn
    if (typeof window.currentOpenTableId === 'number') {
      await showDetails(window.currentOpenTableId);
      loadTables();
    }

  }catch(e){
    alert(e.message || 'Hủy món thất bại');
  }
};

// ===== Mark item as prepared =====
window.markPrepared = async function(itemId){
  try{
    await $fetch(`${BASE_URL}/api/orders/items/${itemId}/prepared`, { method:'PUT' });
    // Làm tươi modal nếu đang mở
    const opened = $id('modal')?.style.display === 'flex';
    if (opened && typeof window.currentOpenTableId === 'number') {
      showDetails(window.currentOpenTableId); // re-render chi tiết
    }
    // Bảng trạng thái/tiền sẽ tự cập nhật do WS "/topic/tables"
  }catch(e){ /* ignore */ }
};

// ===== Thanh toán =====
window.pay = async function (orderId, tableId) {
  const uid = localStorage.getItem('userId');
  if (!uid) { window.showError?.('Không xác định được người dùng', 'Lỗi'); return; }

  // Reset UI modal
  _payContext = {
    order: null,
    table: null,
    paidBy: localStorage.getItem('fullname') || '—',
    paidAt: null,
    orderId,
    tableId,
    voucherCode: null
  };

  const payModal = $id('payModal');
  const infoEl   = $id('payInfo');
  const itemsEl  = $id('payItems');
  const sumEl    = $id('paySummary');
  const errEl    = $id('payError');
  const btnOK    = $id('btnConfirmPay');
  const btnInv   = $id('btnViewInvoice');
  const msgEl    = $id('payVoucherMsg');

  if (!payModal) return;

  if (itemsEl) itemsEl.innerHTML = '';
  if (sumEl) sumEl.textContent = '';
  if (errEl) errEl.style.display = 'none';
  if (btnOK) { btnOK.style.display = 'inline-block'; btnOK.disabled = false; btnOK.textContent = 'Xác nhận thanh toán'; }
  if (btnInv) btnInv.style.display = 'none';
  if (msgEl) { msgEl.textContent = ''; msgEl.style.color = '#6b7280'; }
  payModal.style.display = 'flex';
  if (infoEl) infoEl.innerHTML = `<div style="color:#6b7280;">Đang tải thông tin đơn hàng...</div>`;

  try {
    // Lấy thông tin bàn
    const resTable = await $fetch(`${BASE_URL}/api/tables/${tableId}`);
    const table = resTable.ok ? await resTable.json() : { tableNumber: tableId };
    _payContext.table = table;

    // Lấy order hiện tại
    const res = await $fetch(`${BASE_URL}/api/orders/table/${tableId}/current`);
    if (res.status === 404 || res.status === 204) {
      if (infoEl) infoEl.innerHTML = `<span style="color:#ef4444;">Bàn ${table.tableNumber}: không có đơn hiện tại.</span>`;
      if (btnOK) btnOK.style.display = 'none';
      return;
    }
    if (!res.ok) {
      const m = await $readErr(res);
      if (infoEl) infoEl.innerHTML = `<span style="color:#ef4444;">${m || 'Không tải được đơn hiện tại'}</span>`;
      if (btnOK) btnOK.style.display = 'none';
      return;
    }

    const order = await jsonOrNull(res);
    _payContext.order = order; // snapshot để in

    // Header
    if (infoEl) {
      infoEl.innerHTML = `
        <div><strong>Bàn:</strong> ${table.tableNumber}</div>
        <div><strong>Mã đơn:</strong> ${order?.id ?? '—'}</div>
      `;
    }

// Gom combo và món lẻ
const comboMap = {};   // { comboId: { name, qty, price } }
const normalItems = [];

(order.orderItems || []).forEach(it => {
  if (it.combo) {
    const key = it.combo.id;
    if (!comboMap[key]) {
      comboMap[key] = { name: it.combo.name, qty: 0, price: it.combo.price || 0 };
    }
    comboMap[key].qty += (it.quantity || 1);
  } else if (it.notes && it.notes.startsWith("[COMBO]")) {
    const key = it.notes;
    if (!comboMap[key]) {
      comboMap[key] = { name: it.notes.replace("[COMBO]","").trim(), qty: 0, price: it.unitPrice || 0 };
    }
    comboMap[key].qty += (it.quantity || 1);
  } else {
    normalItems.push(it);
  }
});

  // Build rows cho combo
  let rows = Object.values(comboMap).map(c => `
    <tr>
      <td style="padding:10px;border-bottom:1px solid #eee;">
        Combo ${c.name}
      </td>
      <td style="text-align:center;padding:10px;border-bottom:1px solid #eee;">${c.qty}</td>
      <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(c.price)}</td>
      <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(c.price * c.qty)}</td>
    </tr>
  `).join('');

  // Build rows cho món lẻ
  rows += normalItems.map(it => {
    const name  = it.menuItem?.name ?? '(Món)';
    const price = it.unitPrice ?? it.menuItem?.price ?? 0;
    const qty   = it.quantity ?? 0;
    const line  = price * qty;
    return `
      <tr>
        <td style="padding:10px;border-bottom:1px solid #eee;">
          ${name}
        </td>
        <td style="text-align:center;padding:10px;border-bottom:1px solid #eee;">${qty}</td>
        <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(price)}</td>
        <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(line)}</td>
      </tr>
    `;
  }).join('');

  // Gắn vào bảng
  if (itemsEl) {
    itemsEl.innerHTML = rows || `
      <tr><td colspan="4" style="padding:12px;text-align:center;color:#6b7280;">Không có món</td></tr>
    `;
  }

  // Tổng tiền
  let total;
  if (typeof order?.totalAmount === 'number') {
    total = order.totalAmount;
  } else {
      const totalCombos = Object.values(comboMap)
        .reduce((s, c) => s + (c.price || 0) * (c.qty || 0), 0);
      const totalNormals = normalItems
        .reduce((s, it) => s + (it.quantity || 0) * (it.unitPrice ?? it.menuItem?.price ?? 0), 0);
      total = totalCombos + totalNormals;
    }
    if (sumEl) sumEl.innerHTML = `Tổng thanh toán: <span style="font-size:18px;  color: #de4b4bff">${fmtVND(total)}</span>`;

  } catch (e) {
    if (infoEl) infoEl.innerHTML = `<span style="color:#ef4444;">${e.message || 'Lỗi kết nối'}</span>`;
    if ($id('btnConfirmPay')) $id('btnConfirmPay').style.display = 'none';
  }
};

window.closePayModal = function () { const m = $id('payModal'); if (m) m.style.display = 'none'; };

// ===== Xác nhận thanh toán =====
window.confirmPay = async function () {
  const uid = localStorage.getItem('userId');
  if (!uid) { window.showError?.('Không xác định được người dùng', 'Lỗi'); return; }

  const errEl  = $id('payError');
  const btnOK  = $id('btnConfirmPay');
  const btnInv = $id('btnViewInvoice');

  if (errEl) errEl.style.display = 'none';
  if (btnOK) { btnOK.disabled = true; btnOK.textContent = 'Đang xử lý...'; }

  try {
    // gửi voucherCode nếu có
    const voucherQuery = _payContext.voucherCode
      ? `&voucherCode=${encodeURIComponent(_payContext.voucherCode)}`
      : '';

    const res = await $fetch(
      `${BASE_URL}/api/orders/${_payContext.orderId}/pay?userId=${uid}${voucherQuery}`,
      { method:'PUT' }
    );
    if (!res.ok) throw new Error(await $readErr(res));

    _payContext.paidAt = new Date();

    // Hiển thị nút xem/in hóa đơn
    if (btnOK) btnOK.style.display = 'none';
    if (btnInv) btnInv.style.display = 'inline-block';
    window.showSuccess?.('Thanh toán thành công!', 'Thành công');

    await sleep(200);
    loadTables();

  } catch (e) {
    if (errEl) { errEl.textContent = e.message || 'Thanh toán thất bại'; errEl.style.display = 'block'; }
  } finally {
    if (btnOK) { btnOK.disabled = false; btnOK.textContent = 'Xác nhận thanh toán'; }
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


// Áp dụng voucher trong modal thanh toán (preview)
window.applyVoucher = async function () {
  const code = document.getElementById('payVoucher').value.trim();
  if (!code) {
    document.getElementById('payVoucherMsg').textContent = "Vui lòng nhập mã voucher";
    return;
  }

  try {
    const items = [];
    const combos = [];

    (_payContext.order.orderItems || []).forEach(it => {
      if (it.menuItem) {
        items.push({
          menuItemId: it.menuItem.id,
          quantity: it.quantity,
          notes: it.notes || null
        });
      } else if (it.combo) {
        combos.push({
          comboId: it.combo.id,
          quantity: it.quantity,
          notes: it.notes || null
        });
      }
    });

    const req = {
      tableId: _payContext.tableId,
      items: items,
      combos: combos,     // đúng format backend
      voucherCode: code
    };


    const res = await $fetch(`${BASE_URL}/api/orders/preview`, {
      method: 'POST',
      body: JSON.stringify(req)
    });
    if (!res.ok) throw new Error(await $readErr(res));
    const data = await res.json();

    document.getElementById('paySummary').innerHTML = `
      <div>Tạm tính: ${fmtVND((data.subtotalItems || 0) + (data.subtotalCombos || 0))}</div>
      <br>
      <div>Voucher: - ${fmtVND(data.discountVoucher || 0)}</div>
      <div style="border-top:1px dashed #ddd;margin:12px 0;"></div>
      <div style="font-weight:600; color: #de4b4bff;">Tổng thanh toán: ${fmtVND(data.finalTotal || 0)}</div>
    `;
    
    const msg = document.getElementById('payVoucherMsg');
    msg.textContent = data.voucherMessage || (data.voucherValid ? "Voucher hợp lệ" : "Voucher không hợp lệ");
    msg.style.color = data.voucherValid ? "#16a34a" : "#ef4444";

    _payContext.order.totalAmount = data.finalTotal;
    _payContext.voucherCode = code;
    _payContext.discountVoucher = data.discountVoucher || 0;
    _payContext.originalTotal = (data.subtotalItems || 0) + (data.subtotalCombos || 0);

    //  đồng bộ vào order snapshot để in hóa đơn chính xác
    if (_payContext.order) {
      _payContext.order.originalTotal = _payContext.originalTotal;
      _payContext.order.discountVoucher = _payContext.discountVoucher;
      _payContext.order.totalAmount = data.finalTotal;
    }

  } catch (e) {
    document.getElementById('payVoucherMsg').textContent = e.message || "Lỗi áp dụng voucher";
    document.getElementById('payVoucherMsg').style.color = "#ef4444";
  }
};


// Tạo HTML hóa đơn (in đẹp, A5/A4 đều ok)
function buildInvoiceHTML({ order, table, paidBy, paidAt }) {
  const items = order?.orderItems || [];

  // Gom combo
  const comboMap = {};   // { [comboId]: { name, qty, price } }
  const normalItems = [];

  items.forEach(it => {
    if (it.combo) {
      const key = it.combo.id;
      if (!comboMap[key]) {
        comboMap[key] = {
          name: it.combo.name,
          qty: 0,
          price: it.combo.price || it.unitPrice || 0
        };
      }
      comboMap[key].qty += 1;
    } else {
      normalItems.push(it);
    }
  });

  // Render combo
  const rowsCombo = Object.values(comboMap).map(c => {
    const line = (c.price || 0) * (c.qty || 0);
    return `
      <tr>
        <td>${c.name}</td>
        <td style="text-align:center;">${c.qty}</td>
        <td style="text-align:right;">${fmtVND(c.price)}</td>
        <td style="text-align:right;">${fmtVND(line)}</td>
      </tr>
    `;
  }).join('');

  // Render món lẻ
  const rowsNormal = normalItems.map(it => {
    const name = it?.menuItem?.name ?? '';
    const price = it.unitPrice ?? it.menuItem?.price ?? 0;
    const qty = it.quantity ?? 0;
    const notes = it.notes
      ? ` <em style="color:#6b7280;font-style:italic;">(${it.notes})</em>` : '';
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

  const rows = rowsCombo + rowsNormal;

  // Tổng tiền
  let total;
  if (typeof order?.totalAmount === 'number') {
    total = order.totalAmount;
  } else {
    const totalCombos = Object.values(comboMap)
      .reduce((s, c) => s + (c.price || 0) * (c.qty || 0), 0);
    const totalNormals = normalItems
      .reduce((s, it) => s + (it.quantity || 0) * (it.unitPrice ?? it.menuItem?.price ?? 0), 0);
    total = totalCombos + totalNormals;
  }

  const timeStr = new Date(paidAt).toLocaleString('vi-VN');
  const original = order.originalTotal ?? total;
  const discount = order.discountVoucher ?? 0;
  const finalPay = order.totalAmount ?? total;

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
          <td colspan="3" class="right">Tạm tính</td>
          <td class="right">${fmtVND(original)}</td>
        </tr>
        <tr>
          <td colspan="3" class="right">Voucher</td>
          <td class="right">-${fmtVND(discount)}</td>
        </tr>
        <tr>
          <td colspan="3" class="right tot">Thanh toán</td>
          <td class="tot right">${fmtVND(finalPay)}</td>
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


// ===== CRUD BÀN =====
// --- Thêm bàn ---
window.showAddTable = function () {
  if (window.role && window.role !== 'MANAGER') return;
  $id('newTableNumber').value = '';
  $id('newTableCapacity').value = '';
  const err = $id('addTableError');
  if (err) { err.textContent = ''; err.style.display = 'none'; }
  $id('addTableModal').style.display = 'flex';
};

window.closeAddTableModal = function () { $id('addTableModal').style.display = 'none'; };

window.submitNewTable = async function () {
  if (window.role && window.role !== 'MANAGER') return;

  const number = ($id('newTableNumber').value || '').trim();
  const capacity = Number($id('newTableCapacity').value || 0);
  const err = $id('addTableError');

  if (err) { err.textContent = ''; err.style.display = 'none'; }

  // validate đơn giản
  if (!number || isNaN(Number(number)) || capacity <= 0) {
    if (err) { err.textContent = 'Vui lòng nhập số bàn hợp lệ và sức chứa > 0'; err.style.display = 'block'; }
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
    if (err) { err.textContent = e.message || 'Đã xảy ra lỗi'; err.style.display = 'block'; }
  }
};

// --- Sửa bàn ---
let _currentEditTableId = null;

window.showEditTable = async function (id) {
  if (window.role && window.role !== 'MANAGER') return;

  const err = $id('editTableError');
  if (err) { err.textContent = ''; err.style.display = 'none'; }

  try {
    const res = await $fetch(`${BASE_URL}/api/tables/${id}`);
    if (!res.ok) throw new Error(await $readErr(res));
    const t = await res.json();

    _currentEditTableId = id;
    $id('editTableNumber').value = t.tableNumber ?? '';
    $id('editStatus').value     = t.status ?? 'Trống';
    $id('editCapacity').value   = t.capacity ?? 1;

    const img = $id('editQrCodeImg');
    if (t.qrCodeUrl) {
      img.src = t.qrCodeUrl;
      img.style.display = 'inline-block';
    } else {
      img.style.display = 'none';
    }

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

  if (err) { err.textContent = ''; err.style.display = 'none'; }

  if (!status || capacity <= 0) {
    if (err) { err.textContent = 'Vui lòng nhập trạng thái và sức chứa hợp lệ'; err.style.display = 'block'; }
    return;
  }

  try {
    const res = await $fetch(`${BASE_URL}/api/tables/${_currentEditTableId}`, {
      method: 'PUT',
      body: JSON.stringify({ status, capacity })
    });

    if (!res.ok) throw new Error(await $readErr(res));

    closeEditTableModal();
    updateTableCard(_currentEditTableId, { status, capacity, highlight: true });

  } catch (e) {
    if (err) { err.textContent = e.message || 'Lỗi cập nhật'; err.style.display = 'block'; }
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

window.openQrModal = function (url) {
  if (!url) return;
  $id('qrPreviewImg').src = url;
  $id('qrPreviewModal').style.display = 'flex';
};

window.closeQrModal = function () {
  $id('qrPreviewModal').style.display = 'none';
};

// ===== WS =====
function connectWebSocket() {
  try {
    const socket = new SockJS(`${BASE_URL}/ws`);
    _stomp = Stomp.over(socket);
    _stomp.debug = () => {};
    _stomp.connect({}, () => {
      _stomp.subscribe('/topic/tables', async (msg) => {
        if(msg.body == "UPDATED"){
          await sleep(200);  
          loadTables();
        }
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
