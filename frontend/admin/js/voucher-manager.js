const BASE_URL = window.APP_BASE_URL || location.origin;
let vouchers = [];
let vPage = 1, pageSize = 10;
let vSort = { key: 'code', asc: true };
let stomp = null;

// === Utils ===
const fmt = n => (Number(n || 0)).toLocaleString('vi-VN');
const dmy = s => s ? new Date(s).toLocaleDateString('vi-VN') : '—';
const pick = (o,...k)=>{for(const x of k){if(o?.[x]!=null)return o[x]}return null};

// === Load vouchers ===
async function loadVouchers() {
  try {
    const res = await authFetch(`${BASE_URL}/api/vouchers`);
    vouchers = res.ok ? await res.json() : [];
  } catch { vouchers = []; }
  renderVouchers();
}

// === Helpers ===
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

// === Render ===
function renderVouchers(){
  const q=$('#vSearch').value.trim().toLowerCase();
  const st=$('#vStatus').value;
  let list=vouchers.filter(v=>!q||(pick(v,'code')||'').toLowerCase().includes(q));
  if(st)list=list.filter(v=>voucherStatus(v)===st);

  // thống kê
  $('#vCount').textContent=`Tổng: ${vouchers.length}`;
  $('#vActive').textContent=`Đang bật: ${vouchers.filter(v=>voucherStatus(v)==='ACTIVE').length}`;
  $('#vExpired').textContent=`Hết hạn/đủ lượt: ${vouchers.filter(v=>voucherStatus(v)==='EXPIRED').length}`;

  // sắp xếp
  list.sort((a,b)=>{
    const ka=(pick(a,vSort.key)||'').toString();
    const kb=(pick(b,vSort.key)||'').toString();
    return vSort.asc?ka.localeCompare(kb,'vi',{numeric:true}):kb.localeCompare(ka,'vi',{numeric:true});
  });

  // phân trang
  const totalPages=Math.max(1,Math.ceil(list.length/pageSize));
  vPage=Math.min(vPage,totalPages);
  const items=list.slice((vPage-1)*pageSize,vPage*pageSize);

  $('#vTbody').innerHTML=items.map(v=>{
    const code=pick(v,'code')||'';
    const pct=pick(v,'discount_percent','discountPercent');
    const amt=pick(v,'discount_amount','discountAmount');
    const disc=pct?`${pct}%`:amt?fmt(amt):'—';
    const st=voucherStatus(v);
    const color=st==='ACTIVE'?'ok':st==='EXPIRED'?'danger':'warn';
    return `
      <tr>
        <td><code class="code">${code}</code></td>
        <td>${disc}</td>
        <td><span class="badge ${color}">${st}</span></td>
        <td>${fmt(pick(v,'usage_limit','usageLimit')||0) || '∞'}</td>
        <td>${fmt(pick(v,'used_count','usedCount')||0)}</td>
        <td>${remain(v)}</td>
        <td>${dmy(pick(v,'valid_from','validFrom'))} → ${dmy(pick(v,'valid_to','validTo'))}</td>
      </tr>`;
  }).join('');

  $('#vPage').textContent=`${vPage}/${totalPages}`;
  $('#vPrev').disabled=vPage<=1;
  $('#vNext').disabled=vPage>=totalPages;
}

// === Export CSV ===
function exportCSV(){
  const rows=[['Code','Discount','Status','Limit','Used','Remain','From','To']];
  vouchers.forEach(v=>{
    rows.push([
      pick(v,'code'),
      pick(v,'discount_percent','discountPercent') ? `${pick(v,'discount_percent','discountPercent')}%`
      : fmt(pick(v,'discount_amount','discountAmount')||0),
      voucherStatus(v),
      pick(v,'usage_limit','usageLimit')||'∞',
      pick(v,'used_count','usedCount')||0,
      remain(v),
      dmy(pick(v,'valid_from','validFrom')),
      dmy(pick(v,'valid_to','validTo'))
    ]);
  });
  const blob=new Blob([rows.map(r=>r.join(',')).join('\n')],{type:'text/csv'});
  const a=document.createElement('a');
  a.href=URL.createObjectURL(blob);a.download='vouchers.csv';a.click();
}

// === Realtime WebSocket ===
function connectVoucherSocket(){
  try{
    const sock=new SockJS(`${BASE_URL}/ws`);
    stomp=Stomp.over(sock);
    stomp.debug=()=>{};
    stomp.connect({},()=>{
      stomp.subscribe('/topic/vouchers', msg=>{
        console.log('[WS] update',msg.body);
        if(msg.body==='reload') {
          loadVouchers();
          toast('Cập nhật mới từ hệ thống');
        }
      });
    });
  }catch(e){console.warn('WS error',e);}
}

// === Toast ===
function toast(msg){
  const t=$('#toast');t.textContent=msg;t.style.display='block';
  setTimeout(()=>t.style.display='none',2000);
}

// === Helper shorthand ===
const $=sel=>document.querySelector(sel);

// === Init ===
window.addEventListener('DOMContentLoaded',()=>{
  $('#vSearch').addEventListener('input',()=>{vPage=1;renderVouchers();});
  $('#vStatus').addEventListener('change',()=>{vPage=1;renderVouchers();});
  $('#vPrev').addEventListener('click',()=>{if(vPage>1){vPage--;renderVouchers();}});
  $('#vNext').addEventListener('click',()=>{vPage++;renderVouchers();});
  $('#vExport').addEventListener('click',exportCSV);

  document.querySelectorAll('#vTable th.sortable').forEach(th=>{
    th.addEventListener('click',()=>{
      const k=th.dataset.key;
      if(vSort.key===k) vSort.asc=!vSort.asc; else {vSort.key=k;vSort.asc=true;}
      renderVouchers();
    });
  });

  loadVouchers();
  connectVoucherSocket(); // ✅ realtime
});
