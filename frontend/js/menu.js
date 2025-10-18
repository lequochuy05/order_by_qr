// static/js/menu.js — FULL VERSION (tableCode edition)

let tableCode = null;
let cart = {};                 // { [menuItemId]: { qty, note, name, price } }
let selectedCombos = {};       // { comboId: { qty, note } }
let combosCache = [];          // danh sách combo

// ===== Helpers =====
function safeText(s = "") {
  return String(s).replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[m]));
}
function money(v){ return Number(v||0).toLocaleString('vi-VN'); }
function toImgUrl(path){ return path?.startsWith("http") ? path : (BASE_URL + "/images/" + path); }

// ===== Categories =====
async function loadCategories(selected) {
  try {
    const res = await fetch(`${BASE_URL}/api/categories`);
    const categories = res.ok ? await res.json() : [];
    const select = document.getElementById('categoryFilter');
    select.innerHTML = `<option value="all">Tất cả</option>`;
    categories.forEach(cat => {
      const opt = document.createElement('option');
      opt.value = cat.id;
      opt.textContent = cat.name;
      select.appendChild(opt);
    });
    select.value = selected || 'all';
    await loadMenu(select.value);
  } catch {
    alert("Không tải được danh mục!");
  }
}

// ===== Filter & Sort =====
function filterByName(q){
  q = (q||'').toLowerCase().trim();
  const items = document.querySelectorAll('#menuItems .menu-item');
  items.forEach(it=>{
    const name = it.querySelector('.details .name')?.textContent?.toLowerCase() || '';
    it.style.display = name.includes(q) ? '' : 'none';
  });
}
function reloadWithSort(){
  const sel = document.getElementById('categoryFilter');
  loadMenu(sel.value);
}
window.filterByName = filterByName;
window.reloadWithSort = reloadWithSort;

// ===== Menu list =====
async function loadMenu(categoryId){
  const container = document.getElementById("menuItems");
  container.innerHTML = "Đang tải...";
  const url = categoryId==="all"
    ? `${BASE_URL}/api/menu`
    : `${BASE_URL}/api/menu/category/${encodeURIComponent(categoryId)}`;
  try{
    const res = await fetch(url);
    if(!res.ok) throw new Error(await res.text());
    const items = await res.json();
    container.innerHTML="";
    if(!Array.isArray(items)||!items.length){
      container.innerHTML=`<div style="text-align:center;color:#666;">Không có món.</div>`; return;
    }
    const bust = Date.now();
    items.forEach(it=>{
      const id=String(it.id);
      const name=it.name||'';
      const price=Number(it.price||0);
      const img=it.img?toImgUrl(it.img)+`?v=${bust}`:'';
      if(!cart[id]) cart[id]={qty:0,note:"",name,price};
      const {qty,note}=cart[id];
      const div=document.createElement('div');
      div.className='menu-item';
      div.innerHTML=`
        <img src="${img}" alt="${safeText(name)}">
        <div class="details"><div class="name">${safeText(name)}</div><div class="price">${money(price)} VND</div></div>
        <div class="actions">
          <div class="quantity">
            <button onclick="updateQuantity('${id}',-1)">-</button>
            <span id="qty-${id}">${qty}</span>
            <button onclick="updateQuantity('${id}',1)">+</button>
          </div>
          <button class="note-toggle-btn" onclick="toggleNote('${id}')">Thêm ghi chú</button>
          <div id="note-box-${id}" class="note" style="display:${note?'block':'none'}">
            <input type="text" id="note-${id}" value="${safeText(note)}" oninput="updateNote('${id}',this.value)">
          </div>
        </div>`;
      container.appendChild(div);
    });
  }catch(e){
    console.error(e);
    container.innerHTML=`<div style="color:#e11d48;">Không tải được thực đơn.</div>`;
  }
}

// ===== Note & Quantity =====
function toggleNote(id){
  const box=document.getElementById(`note-box-${id}`);
  box.style.display=box.style.display==='block'?'none':'block';
}
function updateNote(id,val){ if(!cart[id]) cart[id]={qty:0,note:""}; cart[id].note=val; persistCart(); }
function updateQuantity(id,chg){
  if(!cart[id]) cart[id]={qty:0,note:""};
  cart[id].qty=Math.max(0,(cart[id].qty||0)+chg);
  const el=document.getElementById(`qty-${id}`); if(el) el.textContent=cart[id].qty;
  persistCart();
}

// ===== Combo Section =====
async function loadCombos(){
  const wrap=document.getElementById('comboItems');
  if(!wrap) return;
  wrap.innerHTML="Đang tải combo...";
  combosCache=[];
  try{
    const res=await fetch(`${BASE_URL}/api/combos`);
    const combos=res.ok?await res.json():[];
    combosCache=combos;
    if(!combos.length){wrap.innerHTML='<div style="color:#666;">Chưa có combo.</div>';return;}
    wrap.innerHTML='';
    combos.forEach(c=>{
      const qty=selectedCombos[c.id]?.qty||0;
      const note=selectedCombos[c.id]?.note||"";
      const div=document.createElement('div');
      div.className='combo-item';
      div.innerHTML=`
        <div class="details"><div class="name">Combo ${safeText(c.name)}</div><div class="price">${money(c.price)} VND</div></div>
        <div class="actions">
          <div class="quantity">
            <button onclick="updateCombo(${c.id},-1)">-</button>
            <span id="combo-qty-${c.id}">${qty}</span>
            <button onclick="updateCombo(${c.id},1)">+</button>
          </div>
          <button class="note-toggle-btn" onclick="toggleNote('combo-${c.id}')">Thêm ghi chú</button>
          <div id="note-box-combo-${c.id}" class="note" style="display:${note?'block':'none'}">
            <input type="text" id="note-combo-${c.id}" placeholder="..." value="${safeText(note)}"
              oninput="updateComboNote(${c.id},this.value)">
          </div>
        </div>`;
      wrap.appendChild(div);
    });
  }catch(e){
    console.error(e);
    wrap.innerHTML='<div style="color:#e11d48;">Không tải được combo.</div>';
  }
}
function updateCombo(id,chg){
  if(!selectedCombos[id]) selectedCombos[id]={qty:0,note:""};
  selectedCombos[id].qty=Math.max(0,(selectedCombos[id].qty||0)+chg);
  const el=document.getElementById(`combo-qty-${id}`);
  if(el) el.textContent=selectedCombos[id].qty;
}
function updateComboNote(id,val){
  if(!selectedCombos[id]) selectedCombos[id]={qty:0,note:""};
  selectedCombos[id].note=val;
}

// ===== Modal Confirm =====
async function openConfirm(){
  const items=Object.entries(cart).filter(([_,v])=>(v.qty||0)>0);
  const combosSel=Object.entries(selectedCombos).filter(([_,v])=>(v.qty||0)>0);
  if(!items.length && !combosSel.length){alert("Chọn ít nhất 1 món hoặc combo!");return;}
  const box=document.getElementById('confirmList'); box.innerHTML='';
  items.forEach(([id,v])=>{
    const div=document.createElement('div');
    const line=(v.qty||0)*(v.price||0);
    div.className='confirm-row';
    div.innerHTML=`
      <div class="left">
        <div class="name">${safeText(v.name)} × ${v.qty}</div>
        ${v.note?`<div class="note">Ghi chú: ${safeText(v.note)}</div>`:''}
      </div>
      <div class="right">
        <div>${money(v.price)} VND</div>
        <div><b>${money(line)} VND</b></div>
      </div>`;
    box.appendChild(div);
  });
  combosSel.forEach(([cid,v])=>{
    const c=combosCache.find(x=>x.id==cid); if(!c) return;
    const line=(v.qty||0)*(c.price||0);
    const div=document.createElement('div');
    div.className='confirm-row';
    div.innerHTML=`
      <div class="left">
        <div class="name">Combo ${safeText(c.name)} × ${v.qty}</div>
        ${v.note?`<div class="note">Ghi chú: ${safeText(v.note)}</div>`:''}
      </div>
      <div class="right">
        <div>${money(c.price)} VND</div>
        <div><b>${money(line)} VND</b></div>
      </div>`;
    box.appendChild(div);
  });

  // 🧮 Tính tổng
  const subItems=items.reduce((s,[_,v])=>s+(v.qty||0)*(v.price||0),0);
  const subCombos=combosSel.reduce((s,[cid,v])=>{
    const c=combosCache.find(x=>x.id==cid);
    return s+(c?.price||0)*(v.qty||0);
  },0);
  const total=subItems+subCombos;

  document.getElementById('subtotalItems').textContent=money(subItems);
  document.getElementById('subtotalCombos').textContent=money(subCombos);
  document.getElementById('confirmSubtotal').textContent=money(total);
  document.getElementById('confirmTotal').textContent=money(total);

  document.getElementById('confirmModal').classList.remove('hidden');
  document.getElementById('confirmModal').classList.add('show');
}
function closeConfirm(){
  document.getElementById('confirmModal').classList.add('hidden');
  document.getElementById('confirmModal').classList.remove('show');
}

// ===== Submit Order =====
async function confirmOrder(){
  const btn=document.getElementById('confirmBtn'); btn.disabled=true;
  try{
    const items=Object.entries(cart)
      .filter(([_,v])=>(v.qty||0)>0)
      .map(([id,v])=>({menuItemId:Number(id),quantity:v.qty,notes:v.note||null}));
    const combos=Object.entries(selectedCombos)
      .filter(([_,v])=>(v.qty||0)>0)
      .map(([id,v])=>({comboId:Number(id),quantity:v.qty,notes:v.note||null}));

    const res=await fetch(`${BASE_URL}/api/orders`,{
      method:"POST",
      headers:{"Content-Type":"application/json"},
      body:JSON.stringify({tableCode,status:"PENDING",items,combos})
    });
    if(!res.ok){alert("Đặt món thất bại!");return;}
    const data=await res.json();
    alert("Đặt món thành công! Mã đơn: "+(data.id||""));
    cart={}; selectedCombos={}; persistCart();
    window.location.href=`/dashboard.html?tableCode=${encodeURIComponent(tableCode)}`;
  }catch(e){alert("Lỗi: "+e.message);}
  finally{btn.disabled=false;}
}

// ===== Persist Cart =====
function persistCart(){
  try{sessionStorage.setItem(`qr-cart:${tableCode}`,JSON.stringify(cart));}catch{}
}
function restoreCart(){
  try{const raw=sessionStorage.getItem(`qr-cart:${tableCode}`); if(raw) cart=JSON.parse(raw)||{};}catch{}
}

// ===== Boot =====
document.addEventListener('DOMContentLoaded',async()=>{
  const params=new URLSearchParams(location.search);
  tableCode=params.get('tableCode');
  const category=params.get('categoryId')||'all';
  if(!tableCode){alert("Không tìm thấy mã bàn!");return;}

  try{
    const res=await fetch(`${BASE_URL}/api/tables/code/${tableCode}`);
    const t=res.ok?await res.json():null;
    document.getElementById('tableNumber').textContent=t?.tableNumber||"—";
  }catch{document.getElementById('tableNumber').textContent="—";}

  sessionStorage.removeItem(`qr-cart:${tableCode}`);
  cart={};
  await loadCategories(category);
  await loadCombos();
  connectWS({
    "/topic/categories":()=>loadCategories(document.getElementById('categoryFilter')?.value||'all'),
    "/topic/menu":()=>loadMenu(document.getElementById('categoryFilter')?.value||'all'),
    "/topic/combos":loadCombos
  });
});

// Expose
window.updateQuantity=updateQuantity;
window.updateNote=updateNote;
window.toggleNote=toggleNote;
window.openConfirm=openConfirm;
window.closeConfirm=closeConfirm;
window.confirmOrder=confirmOrder;
window.reloadWithSort=reloadWithSort;
window.filterByName=filterByName;
