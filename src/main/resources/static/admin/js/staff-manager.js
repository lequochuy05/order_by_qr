// admin/js/staff-manager.js

// ===== helpers =====
const $ = s => document.querySelector(s);
const $$ = s => Array.from(document.querySelectorAll(s));
const $id = s => document.getElementById(s);
const fmtDate = d => d ? new Date(d).toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'}) : '—';

// ===== state =====
let allUsers = [];
let view = { q:'', role:'', status:'', sortKey:'createdAt', sortDir:'desc', page:1, pageSize:5 };

// ===== data load =====
async function loadUsers(){
  const tbody = $id('tbody');
  tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:#6b7280">Đang tải...</td></tr>`;
  try{
    const res = await $fetch(`${BASE_URL}/api/users`);
    if (!res.ok) throw new Error(await $readErr(res));
    allUsers = await res.json();
    render();
  }catch(e){
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:#ef4444">${e.message||'Lỗi tải danh sách'}</td></tr>`;
  }
}

// ===== render =====
function render(){
  // filter
  const q = ($id('searchInput').value||'').toLowerCase().trim();
  const role = $id('roleFilter').value;
  const status = $id('statusFilter').value;

  let data = allUsers.filter(u=>{
    const hay = `${u.fullName||''} ${u.email||''} ${u.phone||''}`.toLowerCase();
    const okQ = !q || hay.includes(q);
    const okRole = !role || u.role === role;
    // nếu backend chưa có status, coi tất cả là ACTIVE
    const uStatus = (u.status || 'ACTIVE');
    const okStatus = !status || uStatus === status;
    return okQ && okRole && okStatus;
  });

  // sort
  data.sort((a,b)=>{
    const k = view.sortKey; const d = view.sortDir==='asc'? 1 : -1;
    let va=a[k], vb=b[k];
    if (k==='createdAt') return (new Date(va) - new Date(vb)) * d;
    return String(va||'').localeCompare(String(vb||''),'vi') * d;
  });

  // stats
  $('#statTotal').textContent   = `Tổng: ${data.length} |`;
  $('#statManager').textContent = `Quản lý: ${data.filter(x=>x.role==='MANAGER').length} |`;
  $('#statStaff').textContent   = `Nhân viên: ${data.filter(x=>x.role==='STAFF').length} |`;
  $('#statActive').textContent  = `Đang hoạt động: ${data.filter(x=> (x.status||'ACTIVE')==='ACTIVE').length}`;

  // pagination
  const totalPages = Math.max(1, Math.ceil(data.length / view.pageSize));
  view.page = Math.min(view.page, totalPages);
  const start = (view.page-1)*view.pageSize;
  const pageData = data.slice(start, start+view.pageSize);
  $('#pageInfo').textContent = `${view.page} / ${totalPages}`;
  $('#prevPage').disabled = view.page<=1;
  $('#nextPage').disabled = view.page>=totalPages;

  // body
  const tbody = $id('tbody'); tbody.innerHTML = '';
  if (pageData.length===0){
    const tr = document.createElement('tr');
    const td = document.createElement('td'); td.colSpan = 8; td.style.textAlign='center'; td.textContent='Không có dữ liệu phù hợp';
    tr.appendChild(td); tbody.appendChild(tr);
  } else {
    for(const u of pageData){
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${u.fullName||''}</td>
        <td>${u.email||''}</td>
        <td>${u.phone||''}</td>
        <td><span class="chip">${u.role==='MANAGER'?'Quản lý':'Nhân viên'}</span></td>
        <td>
          <span class="status ${(u.status||'ACTIVE')==='ACTIVE'?'active':'inactive'}">
            <span style="width:8px;height:8px;border-radius:999px;background:currentColor;display:inline-block"></span>
            ${(u.status||'ACTIVE')==='ACTIVE'?'Đang hoạt động':'Ngừng'}
          </span>
        </td>
        <td>${fmtDate(u.createdAt)}</td>
        <td>
            <div class="row-actions">
              <button class="btn secondary" onclick='openEdit(${u.id})'>Sửa</button>
              <button class="btn danger" onclick='removeUser(${u.id})'>Xoá</button>
            </div>
        </td>
      `;
      tbody.appendChild(tr);
    }
  }
}

// ===== sort header =====
$$('#employeeTable thead th.sortable').forEach(th=>{
  th.addEventListener('click', ()=>{
    const key = th.dataset.key;
    if (view.sortKey===key) view.sortDir = view.sortDir==='asc' ? 'desc' : 'asc';
    else { view.sortKey = key; view.sortDir='asc'; }
    render();
  });
});

// ===== filters & pagination =====
$id('searchInput').addEventListener('input', ()=>{ view.page=1; render(); });
$id('roleFilter').addEventListener('change', ()=>{ view.page=1; render(); });
$id('statusFilter').addEventListener('change', ()=>{ view.page=1; render(); });
$id('prevPage').addEventListener('click', ()=>{ if (view.page>1){ view.page--; render(); } });
$id('nextPage').addEventListener('click', ()=>{ view.page++; render(); });

// ===== export =====
$id('exportBtn').addEventListener('click', ()=>{
  if (window.role !== 'MANAGER') return alert('Chỉ quản lý mới có quyền sửa nhân viên');
  const headers = ['Họ tên','Email','SĐT','Vai trò','Trạng thái', 'Ngày tạo'];
  const rows = allUsers.map(u=>[
    u.fullName||'', u.email||'', u.phone||'',
    u.role||'', (u.status||'ACTIVE'), fmtDate(u.createdAt)
  ]);
  const csv = [headers, ...rows].map(r=> r.map(v=>`"${String(v).replaceAll('"','""')}"`).join(',')).join('\n');
  const blob = new Blob(["\uFEFF"+csv], {type:'text/csv;charset=utf-8;'});
  const url = URL.createObjectURL(blob); const a=document.createElement('a');
  a.href=url; a.download='employees.csv'; a.click(); URL.revokeObjectURL(url);
});

// ===== modal (add/edit) =====
function openCreate(){
  if (window.role !== 'MANAGER') return alert('Chỉ quản lý mới có quyền thêm nhân viên');
  $('#modalTitle').textContent = 'Thêm nhân viên';
  resetForm(); $('#formModal').showModal();
}
function openEdit(id){
  if (window.role !== 'MANAGER') return alert('Chỉ quản lý mới có quyền sửa nhân viên');
  const u = allUsers.find(x=>x.id===id); if(!u) return;
  $('#modalTitle').textContent = 'Cập nhật nhân viên';
  $('#id').value = u.id;
  $('#fullName').value = u.fullName||'';
  $('#email').value = u.email||'';
  $('#phone').value = u.phone||'';
  $('#role').value = u.role||'STAFF';
  $('#status').value = (u.status||'ACTIVE');
  $('#formModal').showModal();
}
function closeModal(){ $('#formModal').close(); }
function resetForm(){
  ['id','fullName','email','phone'].forEach(id=> $id(id).value='');
  $('#role').value='STAFF'; $('#status').value='ACTIVE';
}

// mở modal từ toolbar
document.getElementById('addBtnTop')?.addEventListener('click', openCreate);

// submit form
$('#employeeForm').addEventListener('submit', async (ev)=>{
  ev.preventDefault();
  const id = $('#id').value.trim();

  // build payload
  const base = {
    fullName: $('#fullName').value.trim(),
    email: $('#email').value.trim(),
    phone: $('#phone').value.trim(),
    role: $('#role').value,
    status: $('#status').value
  };
  if (!base.fullName) { alert('Vui lòng nhập Họ tên'); return; }

  try{
    if (id){ // update
      const res = await $fetch(`${BASE_URL}/api/users/${id}`, {
        method:'PUT', 
        body: JSON.stringify({ 
          fullName: base.fullName, 
          phone: base.phone, 
          role: base.role,
          status: base.status
        })
      });
      if (!res.ok) throw new Error(await $readErr(res));
    } else { // create
      const password = prompt('Nhập mật khẩu ban đầu cho nhân viên (mặc định 123456):','') || '123456';
      const res = await $fetch(`${BASE_URL}/api/users`, {
        method:'POST', 
        body: JSON.stringify({ ...base, password })
      });
      if (!res.ok) throw new Error(await $readErr(res));
    }
    closeModal(); await loadUsers();
  }catch(e){ alert(e.message || 'Lỗi lưu nhân viên'); }
});

// delete
async function removeUser(id){
  if (window.role !== 'MANAGER') return alert('Chỉ quản lý mới có quyền xoá nhân viên');
  if (!confirm('Xoá tài khoản này?')) return;
  try{
    const res = await $fetch(`${BASE_URL}/api/users/${id}`, { method:'DELETE' });
    if (!res.ok) throw new Error(await $readErr(res));
    await loadUsers();
  }catch(e){ alert(e.message || 'Không thể xóa'); }
}
window.removeUser = removeUser;

// ===== boot =====
window.addEventListener('DOMContentLoaded', loadUsers);
