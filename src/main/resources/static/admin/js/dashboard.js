// /static/admin/js/dashboard.js
const BASE = window.APP_BASE_URL || location.origin;
const $    = s => document.querySelector(s);
const fmtN = n => (Number(n)||0).toLocaleString('vi-VN');

function fmtDDMMYYYY(s){
  const d = (s instanceof Date) ? s : new Date(s);
  return d.toLocaleDateString('vi-VN', { day:'2-digit', month:'2-digit', year:'numeric' });
}
function ymd(d){
  const x = new Date(d || Date.now()); x.setHours(0,0,0,0);
  return x.toISOString().slice(0,10);
}

async function readErr(res){
  const ct = res.headers.get('content-type') || '';
  const raw = await res.text();
  if (!raw) return `${res.status} ${res.statusText}`;
  if (ct.includes('application/json')) {
    try { const j = JSON.parse(raw); return j.message || j.error || raw; } catch { return raw; }
  }
  return raw;
}
async function $fetch(url){
  const f = (typeof window.authFetch === 'function') ? window.authFetch : fetch;
  const res = await f(url, { headers: { 'Accept': 'application/json' } });
  if (!res.ok) throw new Error(await readErr(res));
  return res.json();
}

// CHỈ LẤY THEO NGÀY
async function fetchStats(fromDate, toDate){
  const q = `from=${encodeURIComponent(ymd(fromDate))}&to=${encodeURIComponent(ymd(toDate))}`;
  const rev = await $fetch(`${BASE}/api/stats/revenue?${q}`);            // backend trả theo ngày
  const emp = await $fetch(`${BASE}/api/stats/employees?${q}`);
  const ods = await $fetch(`${BASE}/api/stats/orders?${q}`);
  if (!Array.isArray(rev) || !Array.isArray(emp) || !Array.isArray(ods)) {
    throw new Error('API stats trả về sai dạng (không phải mảng).');
  }
  return { rev, emp, ods };
}

function niceMax(v){
  if (v <= 0) return 1;
  const unit = Math.pow(10, Math.floor(Math.log10(v)));
  const m = Math.ceil(v / unit);
  return (m<=1?1:m<=2?2:m<=5?5:10) * unit;
}

// Vẽ line chart
function drawLineChart(canvas, points, labels){
  const ctx = canvas.getContext('2d');
  const W = canvas.width  = canvas.clientWidth;
  const H = canvas.height = canvas.height || 260;

  ctx.clearRect(0,0,W,H);
  if (!points || points.length===0) return;

  const pad = {l:56,r:18,t:10,b:32};
  const YMAX = niceMax(Math.max(...points, 1));
  const n = points.length;
  const ix = i => pad.l + (W-pad.l-pad.r) * (n<=1 ? 0.5 : i/(n-1));
  const iy = v => pad.t + (H-pad.t-pad.b) * (1 - (v / YMAX));

  // grid + y ticks
  ctx.strokeStyle = '#e5e7eb'; ctx.lineWidth=1;
  ctx.fillStyle = '#6b7280'; ctx.font = '12px system-ui'; ctx.textAlign = 'right';
  for (let g=0; g<=4; g++){
    const val = (YMAX * g) / 4;
    const y = pad.t + (H-pad.t-pad.b) * (1 - g/4);
    ctx.beginPath(); ctx.moveTo(pad.l, y); ctx.lineTo(W-pad.r, y); ctx.stroke();
    ctx.fillText(fmtN(val), pad.l-8, y+4);
  }

  // area fill
  const grad = ctx.createLinearGradient(0, pad.t, 0, H-pad.b);
  grad.addColorStop(0,'rgba(14,165,233,.25)'); grad.addColorStop(1,'rgba(14,165,233,0)');
  ctx.fillStyle = grad;
  ctx.beginPath();
  points.forEach((v,i)=>{ const x=ix(i), y=iy(v); i? ctx.lineTo(x,y) : ctx.moveTo(x,y); });
  ctx.lineTo(W-pad.r, H-pad.b); ctx.lineTo(pad.l, H-pad.b); ctx.closePath(); ctx.fill();

  // line
  ctx.strokeStyle='#0ea5e9'; ctx.lineWidth=2; ctx.beginPath();
  points.forEach((v,i)=>{ const x=ix(i), y=iy(v); i? ctx.lineTo(x,y) : ctx.moveTo(x,y); });
  ctx.stroke();

  // points
  ctx.fillStyle = '#0ea5e9';
  points.forEach((v,i)=>{ const x=ix(i), y=iy(v); ctx.beginPath(); ctx.arc(x,y,3,0,Math.PI*2); ctx.fill(); });

  // x labels (đều là dd/MM/yyyy)
  ctx.fillStyle='#6b7280'; ctx.font='12px system-ui'; ctx.textAlign='center';
  const step = Math.max(1, Math.ceil(labels.length/6));
  labels.forEach((lb,i)=>{ if(i%step && i!==labels.length-1) return; ctx.fillText(lb, ix(i), H-8); });
}

// presets
function setPreset(days){
  const to = new Date(); to.setHours(0,0,0,0);
  const from = new Date(to); from.setDate(to.getDate()-(days-1));
  $('#from').valueAsDate = from; $('#to').valueAsDate = to;
  apply();
}

// MAIN
async function apply(){
  try{
    const from = $('#from').valueAsDate || new Date();
    const to   = $('#to').valueAsDate   || new Date();

    $('#rangeLabel').textContent = `${fmtDDMMYYYY(from)} → ${fmtDDMMYYYY(to)}`;

    const { rev, emp, ods } = await fetchStats(from, to);

    // KPIs
    const totalRevenue = rev.reduce((s, p) => s + (p.revenue || 0), 0);
    const totalOrders  = ods.length;
    const avg = totalOrders ? Math.round(totalRevenue / totalOrders) : 0;
    $('#kpiRevenue').textContent = fmtN(totalRevenue) + ' VND';
    $('#kpiOrders').textContent  = fmtN(totalOrders);
    $('#kpiAvg').textContent     = `Giá trị TB: ${fmtN(avg)} VND`;
    $('#kpiTopEmp').textContent  = emp[0]?.fullName || '—';
    $('#kpiTopEmpRev').textContent = emp[0] ? `${fmtN(emp[0].revenue)} VND` : '—';
    $('#kpiActive').textContent  = `${emp.length} NV có đơn`;

    // Chart (labels dd/MM/yyyy)
    const values = rev.map(p => p.revenue || 0);
    const labels = rev.map(p => fmtDDMMYYYY(p.bucket));
    drawLineChart($('#revenueChart'), values, labels);

    // Bảng nhân viên
    const tb = $('#empTable tbody'); tb.innerHTML = '';
    const denom = emp[0]?.revenue || 1;
    emp.forEach(e => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${e.fullName || '—'}</td>
        <td><div class="bar"><i style="width:${Math.round((e.revenue||0)/denom*100)}%"></i></div></td>
        <td align="right">${fmtN(e.orders||0)}</td>`;
      tb.appendChild(tr);
    });

    // Bảng đơn
    const ob = $('#orderTable tbody'); ob.innerHTML = '';
    ods.forEach(o => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${fmtDDMMYYYY(o.paymentTime)}</td>
        <td>${o.id}</td>
        <td>${o.employeeName || '—'}</td>
        <td align="right">${fmtN(o.totalAmount||0)}</td>`;
      ob.appendChild(tr);
    });

  } catch(err){
    console.error(err);
    alert('Không tải được thống kê: ' + err.message);
  }
}

// boot
window.addEventListener('DOMContentLoaded', () => {
  const to = new Date(); to.setHours(0,0,0,0);
  const from = new Date(to); from.setDate(to.getDate()-6); // mặc định 7 ngày
  $('#from').valueAsDate = from; $('#to').valueAsDate = to;

  // 7 ngày & 30 ngày
  document.querySelectorAll('.toolbar [data-preset]').forEach(b=>{
    b.addEventListener('click', ()=> setPreset(parseInt(b.dataset.preset)));
  });
  $('#applyBtn').addEventListener('click', apply);

  apply();
});
