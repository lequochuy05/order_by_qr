// /admin/js/table-manager.js

// Hiện nút admin nếu là quản lý
if (typeof role !== 'undefined' && role === "MANAGER") {
  const el = document.getElementById("adminActions");
  if (el) el.style.display = "block";
}

// ===== state =====
let _isFirstLoad = true;
const _prevOrders = {};
let _stomp = null;
let _renderToken = 0;
let _reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 10;

// Lưu snapshot đơn trước khi thanh toán để in hóa đơn
let _payContext = {
  order: null,
  table: null,
  paidBy: null,
  paidAt: null,
  orderId: null,
  tableId: null,
  voucherCode: null
};

// ===== helpers =====
const $id = (s) => document.getElementById(s);
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function jsonOrNull(res) {
  const raw = await res.text();
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

function highlightCard(cardEl) {
  if (!cardEl) return;
  cardEl.classList.add('highlight');
  setTimeout(() => cardEl.classList.remove('highlight'), 5000);
}

function updateTableCard(tableId, patch = {}) {
  const card = document.querySelector(`.table-card[data-table-id="${tableId}"]`);
  if (!card) return;

  // Chặn hiển thị sai: Nếu bàn Trống thì Tiền phải là 0
  if (patch.status === 'Trống') {
      patch.totalAmount = 0;
      patch.orderId = null; // Xóa ID đơn để nút thanh toán biến mất
  }

  // 1. Cập nhật Trạng thái
  if (patch.status) {
    const statusEl = card.querySelector(".status strong");
    if (statusEl) statusEl.textContent = patch.status;
  }

  // 2. Cập nhật Tổng tiền
  if (patch.totalAmount !== undefined) {
    const totalEl = card.querySelector(".total-amount");
    if (totalEl) totalEl.textContent = `${Number(patch.totalAmount).toLocaleString('vi-VN')} VND`;
  }

  // 3. Cập nhật nút Thanh toán (Quan trọng: Xóa nút nếu bàn trống hoặc tiền = 0)
  const paySlot = card.querySelector('.pay-slot');
  if (paySlot) {
    // Nếu có orderId VÀ tiền > 0 VÀ bàn không phải Trống -> Hiện nút
    if (patch.orderId && patch.totalAmount > 0 && patch.status !== 'Trống') {
        paySlot.innerHTML = `<button class="btn btn-pay" onclick="pay(${patch.orderId}, ${tableId})">Thanh toán</button>`;
    } else {
        // Ngược lại -> Xóa nút
        paySlot.innerHTML = "";
    }
  }

  if (patch.highlight) highlightCard(card);
}

// ===== WebSocket với Auto-Reconnect =====
function connectWebSocket() {
  // Ngắt kết nối cũ nếu có
  if (_stomp && _stomp.connected) {
    try {
      _stomp.disconnect();
    } catch (e) {
      console.warn('Error disconnecting old WebSocket:', e);
    }
  }

  try {
    console.log('🔌 Connecting to WebSocket...');
    const socket = new SockJS(`${BASE_URL}/ws`);
    _stomp = Stomp.over(socket);
    
    // Tắt debug logs
    _stomp.debug = null;
    
    // Kết nối
    _stomp.connect(
      {},
      // Success callback
      function(frame) {
        console.log('✅ WebSocket connected successfully');
        _reconnectAttempts = 0; // Reset reconnect counter
        
        // Subscribe to table updates
        _stomp.subscribe('/topic/tables', function(message) {
          try {
            const data = JSON.parse(message.body);
            console.log('📢 Table update received:', data);
            updateSingleTableFast(data);
          } catch (e) {
            console.error('Error processing table update:', e);
          }
        });
      },
      // Error callback
      function(error) {
        console.error('❌ WebSocket error:', error);
        attemptReconnect();
      }
    );
    
    // Handle connection close
    socket.onclose = function() {
      console.warn('🔌 WebSocket connection closed');
      attemptReconnect();
    };
    
  } catch (e) {
    console.error('❌ WebSocket connect error:', e);
    attemptReconnect();
  }
}

function attemptReconnect() {
  if (_reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
    console.error('❌ Max reconnect attempts reached. Please refresh the page.');
    return;
  }
  
  _reconnectAttempts++;
  const delay = Math.min(30000, 1000 * Math.pow(2, _reconnectAttempts));
  
  console.log(`🔄 Reconnecting in ${delay/1000}s (attempt ${_reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})...`);
  
  setTimeout(() => {
    connectWebSocket();
  }, delay);
}

function updateSingleTableFast(data) {
  const { tableId, status, totalAmount, orderId } = data;

  const card = document.querySelector(`.table-card[data-table-id="${tableId}"]`);
  if (!card) {
    console.log(`Table card not found for ID: ${tableId}, reloading...`);
    loadTables(); // Reload nếu không tìm thấy card
    return;
  }

  // Update status
  const statusEl = card.querySelector(".status strong");
  if (statusEl) statusEl.textContent = status;

  // Update tiền
  const totalEl = card.querySelector(".total-amount");
  if (totalEl) {
    totalEl.textContent = `${Number(totalAmount || 0).toLocaleString('vi-VN')} VND`;
  }

  // Update nút thanh toán
  const paySlot = card.querySelector('.pay-slot');
  if (paySlot) {
    if (orderId && totalAmount > 0) {
      paySlot.innerHTML = `<button class="btn btn-pay" onclick="pay(${orderId}, ${tableId})">Thanh toán</button>`;
    } else {
      paySlot.innerHTML = "";
    }
  }

  highlightCard(card);
  
  // Update prev order tracking
  _prevOrders[tableId] = { 
    totalAmount: totalAmount || 0, 
    itemCount: 0 // Will be updated on next full load if needed
  };
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
          <button class="btn" onclick="openAddItemModal(${t.id})">➕ Thêm món</button>
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
          if (paySlot && total > 0) {
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

    const comboMap = {};
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

    Object.values(comboMap).forEach(c => {
      html += `
        <div class="order-item combo-block ${c.prepared ? 'prepared' : ''}">
          <strong>Combo ${c.combo.name} × ${c.qty}</strong>
          ${c.notes ? `<div class="order-note">Ghi chú: ${c.notes}</div>` : ''}
          <div style="display:flex;gap:8px;margin-top:6px;">
            ${c.prepared ? `<span class="status-prepared">Đã phục vụ</span>`
              : `
                <button class="btn-prepared" onclick="markPrepared(${c.orderItemId}, this)">Đã xong</button>
                <button class="btn-cancel-item" onclick="cancelItem(${c.orderItemId}, this)">Hủy món</button>
              `}
          </div>
        </div>`;
    });

    html += normalItems.map(it => `
      <div class="order-item ${it.prepared ? 'prepared' : ''}">
        <strong>${it.menuItem?.name || ''} × ${it.quantity}</strong>
        ${it.notes ? `<div class="order-note">Ghi chú: ${it.notes}</div>` : ''}
        <div style="display:flex; gap:8px;">
          ${it.prepared ? `<span class="status-prepared">Đã phục vụ</span>`
            : `
              <button class="btn-prepared" onclick="markPrepared(${it.id}, this)">Đã xong</button>
              <button class="btn-cancel-item" onclick="cancelItem(${it.id}, this)">Hủy món</button>
              <button class="btn-edit-item" onclick="openEditItem(${it.id}, ${it.quantity}, '${(it.notes || '').replace(/'/g, "\\'")}')">Sửa đơn</button>
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

    if (typeof window.currentOpenTableId === 'number') {
      showDetails(window.currentOpenTableId);
      updateTableCard(window.currentOpenTableId);
    }
  } catch (e) {
    alert(e.message || 'Sửa món thất bại');
  }
};

window.cancelItem = async function(itemId, btn) {
    if (!confirm('Hủy món này?')) return;
    try {
        const res = await $fetch(`${BASE_URL}/api/orders/items/${itemId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(await $readErr(res));

        // Tìm container của món ăn và xóa nó khỏi giao diện
        const itemEl = btn.closest('.order-item');
        if (itemEl) {
            itemEl.style.transition = "all 0.4s ease";
            itemEl.style.opacity = "0";
            itemEl.style.transform = "translateX(20px)";
            
            setTimeout(() => {
                itemEl.remove();
                // Kiểm tra nếu không còn món nào thì hiện thông báo trống
                const body = document.getElementById('modalBody');
                if (body && body.querySelectorAll('.order-item').length === 0) {
                    body.innerHTML = `<div style="text-align:center;color:#6b7280;padding:16px;">Không có món trong đơn</div>`;
                }
            }, 400);
        }
    } catch (e) {
        alert(e.message || 'Hủy món thất bại');
    }
};
window.markPrepared = async function(itemId, btn) {
    try {
        const res = await $fetch(`${BASE_URL}/api/orders/items/${itemId}/prepared`, { method: 'PUT' });
        
        if (res.ok) {
            // Tìm container của món ăn vừa nhấn
            const itemEl = btn.closest('.order-item');
            if (itemEl) {
                // Thêm class 'prepared' để đổi màu nền (giống như lúc render ban đầu)
                itemEl.classList.add('prepared');
                
                // Tìm div chứa các nút bấm để thay thế nội dung
                const actionDiv = itemEl.querySelector('div[style*="display:flex"]');
                if (actionDiv) {
                    actionDiv.innerHTML = `<span class="status-prepared">Đã phục vụ</span>`;
                }
            }
        } else {
            alert("Lỗi khi cập nhật trạng thái");
        }
    } catch (e) { 
        console.error(e);
    }
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
const itemMap = {};    //  gộp món lẻ theo tên + giá + notes

(order.orderItems || []).forEach(it => {
  if (it.combo) {
    const key = it.combo.id;
    if (!comboMap[key]) {
      comboMap[key] = { name: it.combo.name, qty: 0, price: it.combo.price || 0 };
    }
    comboMap[key].qty += (it.quantity || 1);
  } else {
    // key: tên món + notes + giá
    const key = (it.menuItem?.name || "") + "::" + (it.notes || "") + "::" + (it.unitPrice || it.menuItem?.price || 0);
    if (!itemMap[key]) {
      itemMap[key] = {
        name: it.menuItem?.name || "",
        notes: it.notes || "",
        qty: 0,
        price: it.unitPrice ?? it.menuItem?.price ?? 0
      };
    }
    itemMap[key].qty += it.quantity || 1;
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

// Build rows cho món lẻ (đã gộp)
rows += Object.values(itemMap).map(it => `
  <tr>
    <td style="padding:10px;border-bottom:1px solid #eee;">
      ${it.name}${it.notes ? `<br><small style="color:#6b7280;">(${it.notes})</small>` : ""}
    </td>
    <td style="text-align:center;padding:10px;border-bottom:1px solid #eee;">${it.qty}</td>
    <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(it.price)}</td>
    <td style="text-align:right;padding:10px;border-bottom:1px solid #eee;">${fmtVND(it.price * it.qty)}</td>
  </tr>
`).join('');


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

// Áp dụng voucher trong modal thanh toán 
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

// Tạo HTML hóa đơn
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
      _stomp.subscribe('/topic/tables', (msg) => {
          const data = JSON.parse(msg.body);
          updateSingleTableFast(data);
      });
    });
  } catch (e) {
    console.warn('WS connect error:', e);
  }
}

function updateSingleTableFast(data) {
    const { tableId, status, totalAmount, orderId } = data;

    const card = document.querySelector(`.table-card[data-table-id="${tableId}"]`);
    if (!card) return;

    // update status
    card.querySelector(".status strong").textContent = status;

    // update tiền
    card.querySelector(".total-amount").textContent =
        `${Number(totalAmount).toLocaleString('vi-VN')} VND`;

    const paySlot = card.querySelector('.pay-slot');
    if (orderId) {
        paySlot.innerHTML = `<button class="btn btn-pay" onclick="pay(${orderId}, ${tableId})">Thanh toán</button>`;
    } else {
        paySlot.innerHTML = "";
    }

    highlightCard(card);
}

// ====== THÊM MÓN VÀO BÀN ======
let _currentAddTableId = null;
let _menuItems = [];
let _menuCombos = [];
let _selectedCart = [];
let _currentTab = 'items';

// Mở modal
window.openAddItemModal = async function (tableId) {
  _currentAddTableId = tableId;
  $id("openAddItemModal").style.display = "flex";
  $id("selectedItemsList").innerHTML = `<p style="text-align:center; color:var(--muted); padding:20px;">Đang tải menu...</p>`;
  $id("cartCount").textContent = "0";
  $id("tempTotal").textContent = "0 VND";

  try {
    // Lấy thông tin bàn
    const res = await $fetch(`${BASE_URL}/api/tables/${tableId}`);
    if (!res.ok) throw new Error("Không tìm thấy bàn");
    const table = await res.json();
    $id("addItemTableNumber").textContent = table.tableNumber || "";

    // Gọi API lấy danh mục + menu + combo
    const [resMenu, resCombo, resCate] = await Promise.all([
      $fetch(`${BASE_URL}/api/menu`),
      $fetch(`${BASE_URL}/api/combos`),
      $fetch(`${BASE_URL}/api/categories`)
    ]);

    _menuItems = resMenu.ok ? await resMenu.json() : [];
    _menuCombos = resCombo.ok ? await resCombo.json() : [];
    const categories = resCate.ok ? await resCate.json() : [];

    // Render filter category
    const catEl = $id("categoryFilter");
    catEl.innerHTML = `<button class="btn small" onclick="renderMenuItems()" data-cat="ALL">Tất cả</button>` +
      categories.map(c => `<button class="btn small" onclick="renderMenuItems('${c.name}')">${c.name}</button>`).join("");

    renderMenuItems();
    renderCart();

  } catch (e) {
    $id("selectedItemsList").innerHTML = `<p style="text-align:center;color:#ef4444;">${e.message}</p>`;
  }
};

// Đóng modal
window.closeAddItemModal = function () {
  _currentAddTableId = null;
  _selectedCart = [];
  _menuItems = [];
  _menuCombos = [];
  $id("openAddItemModal").style.display = "none";
};

// Chuyển tab
window.switchTab = function (tab) {
  _currentTab = tab;
  document.getElementById("tabItems").classList.toggle("active", tab === "items");
  document.getElementById("tabCombos").classList.toggle("active", tab === "combos");
  document.getElementById("categoryFilter").style.display = tab === "items" ? "flex" : "none";
  renderMenuItems();
};

// Render danh sách món / combo
window.renderMenuItems = function (category = "ALL") {
  const grid = $id("menuItemsGrid");
  const list = _currentTab === "items"
    ? _menuItems.filter(i => category === "ALL" || i.category?.name === category)
    : _menuCombos;

  if (!list.length) {
    grid.innerHTML = `<p style="text-align:center;color:#6b7280;padding:16px;">Không có dữ liệu</p>`;
    return;
  }

  grid.innerHTML = list.map(it => `
    <div class="menu-card" onclick="addToCart('${_currentTab}', ${it.id})">
      <div class="menu-info">
        <strong>${it.name}</strong>
        <div>${fmtVND(it.price)}</div>
      </div>
    </div>
  `).join("");
};

// Thêm món vào giỏ hàng
window.addToCart = function (type, id) {
  const src = type === "items"
    ? _menuItems.find(i => i.id === id)
    : _menuCombos.find(c => c.id === id);
  if (!src) return;

  const key = `${type}-${id}`;
  const exist = _selectedCart.find(it => it.key === key);
  if (exist) {
    exist.qty++;
  } else {
    _selectedCart.push({
      key,
      type,
      id,
      name: src.name,
      price: src.price,
      qty: 1,
      notes: ""
    });
  }
  renderCart();
};

// Cập nhật ghi chú
window.updateCartNote = function (key, val) {
  const it = _selectedCart.find(i => i.key === key);
  if (it) it.notes = val;
};

// Cập nhật số lượng
window.updateCartQty = function (key, val) {
  const it = _selectedCart.find(i => i.key === key);
  if (it) it.qty = Math.max(1, Number(val) || 1);
  renderCart();
};

// Xóa khỏi giỏ
window.removeCartItem = function (key) {
  _selectedCart = _selectedCart.filter(i => i.key !== key);
  renderCart();
};

// Hiển thị giỏ hàng
function renderCart() {
  const listEl = $id("selectedItemsList");
  if (!_selectedCart.length) {
    listEl.innerHTML = `<p style="text-align:center; color:var(--muted); padding:20px;">Chưa chọn món nào</p>`;
    $id("cartCount").textContent = "0";
    $id("tempTotal").textContent = "0 VND";
    return;
  }

  let total = 0;
  listEl.innerHTML = _selectedCart.map(it => {
    const line = (it.price || 0) * (it.qty || 1);
    total += line;
    return `
      <div class="cart-row">
        <div><strong>${it.name}</strong> - ${fmtVND(it.price)}</div>
        <div style="display:flex;gap:8px;align-items:center;margin-top:4px;">
          <input type="number" min="1" value="${it.qty}" style="width:60px" onchange="updateCartQty('${it.key}', this.value)">
          <input type="text" placeholder="Ghi chú..." value="${it.notes}" style="flex:1" onchange="updateCartNote('${it.key}', this.value)">
          <button class="btn small danger" onclick="removeCartItem('${it.key}')">✖</button>
        </div>
      </div>
    `;
  }).join("");
  $id("cartCount").textContent = _selectedCart.length;
  $id("tempTotal").textContent = fmtVND(total);
}

// Gửi API thêm món vào bàn
window.submitAddItemsToTable = async function () {
  if (!_selectedCart.length) {
    $id("addItemError").textContent = "Vui lòng chọn ít nhất 1 món.";
    return;
  }

  try {
    const resT = await $fetch(`${BASE_URL}/api/tables/${_currentAddTableId}`);
    if (!resT.ok) throw new Error("Không tìm thấy bàn");
    const table = await resT.json();

    const req = {
      tableId: table.id,
      tableCode: table.tableCode,
      items: _selectedCart
        .filter(i => i.type === "items")
        .map(i => ({
          menuItemId: i.id,
          quantity: i.qty,
          notes: i.notes || null
        })),
      combos: _selectedCart
        .filter(i => i.type === "combos")
        .map(i => ({
          comboId: i.id,
          quantity: i.qty,
          notes: i.notes || null
        }))
    };

    const res = await $fetch(`${BASE_URL}/api/orders`, {
      method: "POST",
      body: JSON.stringify(req)
    });
    if (!res.ok) throw new Error(await $readErr(res));

    closeAddItemModal();
    showSuccess?.("Đã thêm món vào bàn!", "Thành công");
  } catch (e) {
    $id("addItemError").textContent = e.message || "Không thể thêm món";
  }
};


// ===== boot =====
window.addEventListener('DOMContentLoaded', () => {
  console.log('🚀 Initializing table manager...');
  loadTables();
  connectWebSocket();
  
  // Ping WebSocket every 30s to keep alive
  setInterval(() => {
    if (_stomp && _stomp.connected) {
      console.log('💓 WebSocket heartbeat');
    } else {
      console.warn('💔 WebSocket disconnected, attempting reconnect...');
      connectWebSocket();
    }
  }, 30000);
});

// Reconnect on page visibility change
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && (!_stomp || !_stomp.connected)) {
    console.log('👁️ Page visible, checking WebSocket connection...');
    connectWebSocket();
  }
});