// resource/static/admin/js/voucher-manager.js
let vouchers = [];
let promotions = [];
let vPage = 1, pPage = 1, pageSize = 10;
let vSort = { key: 'code', asc: true };
let pSort = { key: 'name', asc: true };
let stomp = null;
let editingVoucherId = null;
let editingPromotionId = null;

// === Utils ===
const fmt = n => (Number(n || 0)).toLocaleString('vi-VN');
const dmy = s => s ? new Date(s).toLocaleDateString('vi-VN') : '—';
const pick = (o,...k)=>{for(const x of k){if(o?.[x]!=null)return o[x]}return null};
const $ = sel => document.querySelector(sel);
const $$ = sel => document.querySelectorAll(sel);

// ========== VOUCHERS ==========

// === Load vouchers ===
async function loadVouchers() {
  try {
    const res = await $fetch(`${BASE_URL}/api/vouchers`);
    vouchers = res.ok ? await res.json() : [];
  } catch { vouchers = []; }
  renderVouchers();
}

// === Voucher Status ===
function voucherStatus(v){
  const active=!!pick(v,'active','isActive');
  const used=+pick(v,'used_count','usedCount')||0;
  const limit=+pick(v,'usage_limit','usageLimit')||0;
  const from=pick(v,'valid_from','validFrom');
  const to=pick(v,'valid_to','validTo');
  const now=new Date();
  const okDate=(!from||new Date(from)<=now)&&(!to||new Date(to)>=now);
  if(!active)return'INACTIVE';
  if(okDate&&(limit===0||used<limit))return'ACTIVE';
  return'EXPIRED';
}

function remain(v){
  const used=+pick(v,'used_count','usedCount')||0;
  const limit=+pick(v,'usage_limit','usageLimit')||0;
  return limit===0?'∞':Math.max(0,limit-used);
}

// === Render Vouchers ===
function renderVouchers(){
  const q=$('#vSearch').value.trim().toLowerCase();
  const st=$('#vStatus').value;
  let list=vouchers.filter(v=>!q||(pick(v,'code')||'').toLowerCase().includes(q));
  if(st)list=list.filter(v=>voucherStatus(v)===st);

  // Stats
  $('#vCount').textContent=`Tổng: ${vouchers.length}`;
  $('#vActive').textContent=`Đang bật: ${vouchers.filter(v=>voucherStatus(v)==='ACTIVE').length}`;
  $('#vExpired').textContent=`Hết hạn/đủ lượt: ${vouchers.filter(v=>voucherStatus(v)==='EXPIRED').length}`;

  // Sort
  list.sort((a,b)=>{
    const ka=(pick(a,vSort.key)||'').toString();
    const kb=(pick(b,vSort.key)||'').toString();
    return vSort.asc?ka.localeCompare(kb,'vi',{numeric:true}):kb.localeCompare(ka,'vi',{numeric:true});
  });

  // Pagination
  const totalPages=Math.max(1,Math.ceil(list.length/pageSize));
  vPage=Math.min(vPage,totalPages);
  const items=list.slice((vPage-1)*pageSize,vPage*pageSize);

  const tbody = $('#vTbody');
  if (items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty-state"><p>Không có voucher nào</p></td></tr>`;
  } else {
    tbody.innerHTML=items.map(v=>{
      const id=pick(v,'id');
      const code=pick(v,'code')||'';
      const pct=pick(v,'discount_percent','discountPercent');
      const amt=pick(v,'discount_amount','discountAmount');
      const disc=pct?`${pct}%`:amt?fmt(amt)+' VND':'—';
      const st=voucherStatus(v);
      const color=st==='ACTIVE'?'ok':st==='EXPIRED'?'danger':'warn';
      const stText=st==='ACTIVE'?'Đang dùng':st==='EXPIRED'?'Hết hạn':'Ngừng';
      
      return `
        <tr>
          <td><code class="code">${escapeHtml(code)}</code></td>
          <td>${disc}</td>
          <td><span class="badge ${color}">${stText}</span></td>
          <td>${fmt(pick(v,'usage_limit','usageLimit')||0) || '∞'}</td>
          <td>${fmt(pick(v,'used_count','usedCount')||0)}</td>
          <td>${remain(v)}</td>
          <td class="nowrap">${dmy(pick(v,'valid_from','validFrom'))} → ${dmy(pick(v,'valid_to','validTo'))}</td>
          <td class="nowrap">
            ${window.role === 'MANAGER' ? `
              <button class="btn-icon" onclick="showEditVoucher(${id})" title="Sửa">✏️</button>
              <button class="btn-icon red" onclick="deleteVoucher(${id})" title="Xóa">🗑️</button>
            ` : ''}
          </td>
        </tr>`;
    }).join('');
  }

  $('#vPage').textContent=`${vPage} / ${totalPages}`;
  $('#vPrev').disabled=vPage<=1;
  $('#vNext').disabled=vPage>=totalPages;
}

// === Add Voucher ===
window.showAddVoucher = function() {
  byId('voucherForm').reset();
  byId('voucherModalTitle').textContent = 'Thêm Voucher mới';
  editingVoucherId = null;
  showError('voucherError', '');
  byId('voucherModal').style.display = 'flex';
  setTimeout(() => byId('voucherCode').focus(), 100);
};

window.closeVoucherModal = function() {
  byId('voucherModal').style.display = 'none';
};

window.submitVoucher = async function() {
  const code = byId('voucherCode').value.trim().toUpperCase();
  const discountPercent = parseFloat(byId('voucherPercent').value) || null;
  const discountAmount  = parseFloat(byId('voucherAmount').value) || null;
  const usageLimit = parseInt(byId('voucherLimit').value) || 0;
  const validFrom = byId('voucherFrom').value;
  const validTo = byId('voucherTo').value;
  const isActive = byId('voucherActive').checked;

  showError('voucherError', '');

  if (!code) {
    showError('voucherError', 'Vui lòng nhập mã voucher');
    return;
  }

  if (!discountPercent && !discountAmount) {
    showError('voucherError', 'Vui lòng nhập % giảm hoặc số tiền giảm');
    return;
  }

  if (discountPercent && discountAmount) {
    showError('voucherError', 'Chỉ chọn 1 loại: % giảm HOẶC số tiền giảm');
    return;
  }

  const payload = {
    code: code,
    discountPercent: discountPercent,
    discountAmount: discountAmount,
    usageLimit: usageLimit,
    validFrom: validFrom ? `${validFrom}T00:00:00` : null,
    validTo: validTo ? `${validTo}T23:59:59` : null,
    active: isActive
  };

  try {
    const url = editingVoucherId
      ? `${BASE_URL}/api/vouchers/${editingVoucherId}`
      : `${BASE_URL}/api/vouchers`;
    const method = editingVoucherId ? 'PUT' : 'POST';

    const res = await $fetch(url, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Lỗi lưu voucher');
    }

    closeVoucherModal();
    showAppSuccess(editingVoucherId ? 'Đã cập nhật voucher!' : 'Đã thêm voucher mới!');
    loadVouchers();

  } catch (e) {
    showError('voucherError', e.message || 'Có lỗi xảy ra');
  }
};


// === Edit Voucher ===
window.showEditVoucher = function(id) {
  const v = vouchers.find(x => pick(x, 'id') === id);
  if (!v) return;

  editingVoucherId = id;
  byId('voucherModalTitle').textContent = 'Chỉnh sửa Voucher';
  
  byId('voucherCode').value = pick(v, 'code') || '';
  byId('voucherPercent').value = pick(v, 'discount_percent', 'discountPercent') || '';
  byId('voucherAmount').value = pick(v, 'discount_amount', 'discountAmount') || '';
  byId('voucherLimit').value = pick(v, 'usage_limit', 'usageLimit') || 0;
  byId('voucherFrom').value = pick(v, 'valid_from', 'validFrom') ? pick(v, 'valid_from', 'validFrom').split('T')[0] : '';
  byId('voucherTo').value = pick(v, 'valid_to', 'validTo') ? pick(v, 'valid_to', 'validTo').split('T')[0] : '';
  byId('voucherActive').checked = !!pick(v, 'active', 'isActive', 'is_active');

  showError('voucherError', '');
  byId('voucherModal').style.display = 'flex';
};

// === Delete Voucher ===
window.deleteVoucher = async function(id) {
  const v = vouchers.find(x => pick(x, 'id') === id);
  if (!v) return;
  
  const code = pick(v, 'code');
  if (!confirm(`Xóa voucher "${code}"?\n\nThao tác này không thể hoàn tác!`)) return;

  try {
    const res = await $fetch(`${BASE_URL}/api/vouchers/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Xóa thất bại');
    }
    showAppSuccess('Đã xóa voucher!');
    setTimeout(() => loadVouchers(), 500);
  } catch (e) {
    showAppError(e.message || 'Không thể xóa voucher');
  }
};

// ========== PROMOTIONS ==========

// === Load Promotions ===
// async function loadPromotions() {
//   try {
//     const res = await $fetch(`${BASE_URL}/api/promotions`);
//     promotions = res.ok ? await res.json() : [];
//   } catch { promotions = []; }
//   renderPromotions();
// }

// === Promotion Status ===
// function promotionStatus(p) {
//   const active = !!pick(p, 'active', 'isActive', 'is_active');
//   if (!active) return 'INACTIVE';
  
//   const days = pick(p, 'applicable_days', 'applicableDays') || [];
//   const from = pick(p, 'start_time', 'startTime');
//   const to = pick(p, 'end_time', 'endTime');
  
//   const now = new Date();
//   const dayOfWeek = now.getDay(); // 0=Sunday, 1=Monday...
//   const todayApplies = days.length === 0 || days.includes(dayOfWeek);
  
//   if (!todayApplies) return 'INACTIVE';
  
//   if (from && to) {
//     const currentTime = now.toTimeString().slice(0, 5); // HH:MM
//     if (currentTime >= from && currentTime <= to) return 'NOW';
//   }
  
//   return 'ACTIVE';
// }

// === Render Promotions ===
// function renderPromotions(){
//   const q = $('#pSearch').value.trim().toLowerCase();
//   const st = $('#pStatus').value;
//   let list = promotions.filter(p => !q || (pick(p, 'name') || '').toLowerCase().includes(q));
  
//   if (st === 'TODAY') {
//     const today = new Date().getDay();
//     list = list.filter(p => {
//       const days = pick(p, 'applicable_days', 'applicableDays') || [];
//       return days.length === 0 || days.includes(today);
//     });
//   } else if (st === 'NOW') {
//     list = list.filter(p => promotionStatus(p) === 'NOW');
//   } else if (st) {
//     list = list.filter(p => promotionStatus(p) === st || (st === 'ACTIVE' && promotionStatus(p) === 'NOW'));
//   }

//   // Stats
//   const today = new Date().getDay();
//   $('#pCount').textContent = `Tổng: ${promotions.length}`;
//   $('#pActive').textContent = `Đang bật: ${promotions.filter(p => !!pick(p, 'active', 'isActive', 'is_active')).length}`;
//   $('#pToday').textContent = `Hôm nay: ${promotions.filter(p => {
//     const days = pick(p, 'applicable_days', 'applicableDays') || [];
//     return days.length === 0 || days.includes(today);
//   }).length}`;

//   // Sort
//   list.sort((a, b) => {
//     const ka = (pick(a, pSort.key) || '').toString();
//     const kb = (pick(b, pSort.key) || '').toString();
//     return pSort.asc ? ka.localeCompare(kb, 'vi', { numeric: true }) : kb.localeCompare(ka, 'vi', { numeric: true });
//   });

//   // Pagination
//   const totalPages = Math.max(1, Math.ceil(list.length / pageSize));
//   pPage = Math.min(pPage, totalPages);
//   const items = list.slice((pPage - 1) * pageSize, pPage * pageSize);

//   const tbody = $('#pTbody');
//   if (items.length === 0) {
//     tbody.innerHTML = `<tr><td colspan="6" class="empty-state"><p>Không có khuyến mãi nào</p></td></tr>`;
//   } else {
//     tbody.innerHTML = items.map(p => {
//       const id = pick(p, 'id');
//       const name = pick(p, 'name') || '';
//       const pct = pick(p, 'discount_percent', 'discountPercent') || 0;
//       const days = pick(p, 'applicable_days', 'applicableDays') || [];
//       const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
//       const daysText = days.length === 0 ? 'Tất cả' : days.map(d => dayNames[d]).join(', ');
//       const from = pick(p, 'start_time', 'startTime') || '';
//       const to = pick(p, 'end_time', 'endTime') || '';
//       const time = (from && to) ? `${from} - ${to}` : 'Cả ngày';
//       const st = promotionStatus(p);
//       const color = st === 'NOW' ? 'ok' : st === 'ACTIVE' ? 'info' : 'warn';
//       const stText = st === 'NOW' ? 'Đang chạy' : st === 'ACTIVE' ? 'Đang bật' : 'Ngừng';

//       return `
//         <tr>
//           <td>${escapeHtml(name)}</td>
//           <td>${pct}%</td>
//           <td class="nowrap">${daysText}</td>
//           <td class="nowrap">${time}</td>
//           <td><span class="badge ${color}">${stText}</span></td>
//           <td class="nowrap">
//             ${window.role === 'MANAGER' ? `
//               <button class="btn-icon" onclick="showEditPromotion(${id})" title="Sửa">✏️</button>
//               <button class="btn-icon red" onclick="deletePromotion(${id})" title="Xóa">🗑️</button>
//             ` : ''}
//           </td>
//         </tr>`;
//     }).join('');
//   }

//   $('#pPage').textContent = `${pPage} / ${totalPages}`;
//   $('#pPrev').disabled = pPage <= 1;
//   $('#pNext').disabled = pPage >= totalPages;
// }

// === Toast ===
function toast(msg, type = 'success') {
  const t = $('#toast');
  t.textContent = msg;
  t.className = 'toast ' + type;
  t.style.display = 'block';
  setTimeout(() => t.style.display = 'none', 3000);
}

// === WebSocket ===
function connectSocket() {
  try {
    const sock = new SockJS(`${BASE_URL}/ws`);
    stomp = Stomp.over(sock);
    stomp.debug = () => {};
    stomp.connect({}, () => {
      stomp.subscribe('/topic/vouchers', msg => {
        if (msg.body === 'reload') {
          loadVouchers();
          toast('Cập nhật voucher mới');
        }
      });
      stomp.subscribe('/topic/promotions', msg => {
        if (msg.body === 'reload') {
          loadPromotions();
          toast('Cập nhật khuyến mãi mới');
        }
      });
    });
  } catch (e) {
    console.warn('WebSocket error:', e);
  }
}

// === Init ===
window.addEventListener('DOMContentLoaded', () => {
  // Show admin actions
  if (window.role === 'MANAGER') {
    const actions = $$('.admin-actions');
    actions.forEach(el => el.style.display = 'block');
  }

  // Voucher events
  $('#vSearch').addEventListener('input', () => { vPage = 1; renderVouchers(); });
  $('#vStatus').addEventListener('change', () => { vPage = 1; renderVouchers(); });
  $('#vPrev').addEventListener('click', () => { if (vPage > 1) { vPage--; renderVouchers(); } });
  $('#vNext').addEventListener('click', () => { vPage++; renderVouchers(); });


  // // Promotion events
  // $('#pSearch').addEventListener('input', () => { pPage = 1; renderPromotions(); });
  // $('#pStatus').addEventListener('change', () => { pPage = 1; renderPromotions(); });
  // $('#pPrev').addEventListener('click', () => { if (pPage > 1) { pPage--; renderPromotions(); } });
  // $('#pNext').addEventListener('click', () => { pPage++; renderPromotions(); });

  // Sort columns
  $$('#vTable th.sortable').forEach(th => {
    th.addEventListener('click', () => {
      const k = th.dataset.key;
      if (vSort.key === k) vSort.asc = !vSort.asc;
      else { vSort.key = k; vSort.asc = true; }
      renderVouchers();
    });
  });

  // $$('#pTable th.sortable').forEach(th => {
  //   th.addEventListener('click', () => {
  //     const k = th.dataset.key;
  //     if (pSort.key === k) pSort.asc = !pSort.asc;
  //     else { pSort.key = k; pSort.asc = true; }
  //     renderPromotions();
  //   });
  // });

  // Load data
  loadVouchers();
  // loadPromotions();
  connectSocket();
});