let combos = [];
let editingComboId = null;
let allMenuItems = [];

if (role === "MANAGER") {
  document.getElementById("adminActions").style.display = "block";
}

const $id = (s) => document.getElementById(s);
const fmtVND = (n) => Number(n || 0).toLocaleString("vi-VN") + " VND";

window.addEventListener("DOMContentLoaded", () => {
  loadCombos();
  connectComboSocket();
  const s = $id("comboSearch");
  if (s) s.addEventListener("input", () => renderCombos());
});

// ===== LOAD COMBOS =====
async function loadCombos() {
  const container = $id("comboContainer");
  container.innerHTML = `<div class="loading">⏳ Đang tải...</div>`;
  try {
    const res = await $fetch(`${BASE_URL}/api/combos`);
    if (!res.ok) throw new Error(await $readErr(res));
    combos = await res.json();
    renderCombos();
  } catch (e) {
    container.innerHTML = `<div class="error">${e.message}</div>`;
  }
}

function renderCombos() {
  const container = $id("comboContainer");
  const query = ($id("comboSearch")?.value || "").trim().toLowerCase();
  const list = combos.filter(c => !query || c.name.toLowerCase().includes(query));

  if (list.length === 0) {
    container.innerHTML = `<div class="empty">Không có combo nào</div>`;
    return;
  }

  container.innerHTML = "";
  list.forEach(c => {
    const div = document.createElement("div");
    div.className = "combo-card";
    div.innerHTML = `
      <h3>${escapeHtml(c.name)}</h3>
      <div class="price">${fmtVND(c.price)}</div>
      <span class="status ${c.active ? "active" : "inactive"}">
        ${c.active ? "Đang bán" : "Ngừng bán"}
      </span>
      <div class="combo-actions">
        <button class="btn secondary" onclick="showEditCombo(${c.id})">✏️</button>
        <button class="btn ${c.active ? "red" : "primary"}" onclick="toggleComboActive(${c.id})">
          ${c.active ? "❌" : "✅"}
        </button>
        <button class="btn red" onclick="deleteCombo(${c.id})">🗑️</button>
      </div>
    `;
    container.appendChild(div);
  });
}

// ===== MODALS =====
window.showAddCombo = async function() {
  editingComboId = null;
  $id("comboModalTitle").textContent = "Thêm Combo";
  $id("comboForm").reset();
  $id("comboActive").checked = true;
  $id("comboPrice").value = "";
  $id("comboTotal").textContent = "Tổng: 0 VND";
  showError("comboError", "");
  $id("comboModal").style.display = "flex";
  await loadMenuItems();
};

// ====== SỬA COMBO ======
window.showEditCombo = async function(id) {
  editingComboId = id;
  $id("comboModalTitle").textContent = "Sửa Combo";
  $id("comboForm").reset();
  showError("comboError", "");
  $id("comboModal").style.display = "flex";

  try {
    // 1. Lấy thông tin combo + item của nó
    const res = await $fetch(`${BASE_URL}/api/combos/${id}`);
    if (!res.ok) throw new Error(await $readErr(res));
    const combo = await res.json();

    // 2. Gán giá trị cơ bản
    $id("comboName").value = combo.name;
    $id("comboPrice").value = combo.price;
    $id("comboActive").checked = combo.active;

    // 3. Nạp danh sách menu (toàn bộ)
    const resMenu = await $fetch(`${BASE_URL}/api/menu`);
    if (!resMenu.ok) throw new Error(await $readErr(resMenu));
    const menuItems = await resMenu.json();

    // 4. Gán danh sách món + đánh dấu món trong combo
    const container = $id("menuList");
    container.innerHTML = menuItems.map(m => {
      const found = combo.items?.find(ci => ci.menuItem.id === m.id);
      const checked = found ? "checked" : "";
      const qty = found ? found.quantity : 1;
      return `
        <div class="menu-item">
          <div style="display:flex;align-items:center;gap:6px;">
            <input type="checkbox" class="menu-checkbox" data-id="${m.id}" data-price="${m.price}" ${checked} onchange="calculateComboPrice()">
            <div>
              <div>${escapeHtml(m.name)}</div>
              <small>(${fmtVND(m.price)})</small>
            </div>
          </div>
          <input type="number" class="menu-qty" min="1" value="${qty}" onchange="calculateComboPrice()">
        </div>
      `;
    }).join("");

    calculateComboPrice();
  } catch (e) {
    showError("comboError", e.message);
  }
};


window.closeComboModal = () => $id("comboModal").style.display = "none";

// ===== LOAD MENU ITEMS =====
async function loadMenuItems() {
  const container = $id("menuList");
  container.innerHTML = `<div style="text-align:center;color:#6b7280;">Đang tải...</div>`;
  try {
    const res = await $fetch(`${BASE_URL}/api/menu`);
    if (!res.ok) throw new Error(await $readErr(res));
    allMenuItems = await res.json();

    container.innerHTML = allMenuItems.map(m => `
      <div class="menu-item">
        <label>
          <input type="checkbox" class="menu-checkbox" 
                 data-id="${m.id}" data-price="${m.price}" 
                 onchange="calculateComboPrice()">
          ${escapeHtml(m.name)} <small>(${fmtVND(m.price)})</small>
        </label>
        <input type="number" class="menu-qty" min="1" value="1" 
               onchange="calculateComboPrice()" />
      </div>
    `).join('');
  } catch (e) {
    container.innerHTML = `<div style="color:red;">${e.message}</div>`;
  }
}

// ===== TÍNH GIÁ COMBO =====
function calculateComboPrice() {
  let total = 0;
  document.querySelectorAll(".menu-checkbox").forEach(cb => {
    if (cb.checked) {
      const price = parseFloat(cb.dataset.price);
      const qtyInput = cb.closest(".menu-item").querySelector(".menu-qty");
      const qty = parseInt(qtyInput.value) || 1;
      total += price * qty;
    }
  });

  const discount = total * 0.1;
  const finalPrice = total - discount;
  $id("comboPrice").value = Math.round(finalPrice);
  $id("comboTotal").textContent = `Tổng: ${fmtVND(total)}`;
}

// ===== CRUD =====
window.submitCombo = async function() {
  const name = $id("comboName").value.trim();
  const price = parseFloat($id("comboPrice").value);
  const active = $id("comboActive").checked;

  const selectedItems = [];
  document.querySelectorAll(".menu-checkbox:checked").forEach(cb => {
    const qtyInput = cb.closest(".menu-item").querySelector(".menu-qty");
    selectedItems.push({
      menuItemId: parseInt(cb.dataset.id),
      quantity: parseInt(qtyInput.value)
    });
  });

  if (!name) return showError("comboError", "Tên combo không được trống");
  if (selectedItems.length === 0) return showError("comboError", "Vui lòng chọn ít nhất 1 món");
  if (!price || price < 1000) return showError("comboError", "Giá combo không hợp lệ");

  const data = { name, price, active, items: selectedItems };
  const url = editingComboId ? `${BASE_URL}/api/combos/${editingComboId}` : `${BASE_URL}/api/combos`;
  const method = editingComboId ? "PUT" : "POST";

  try {
    const res = await $fetch(url, { method, body: JSON.stringify(data) });
    if (!res.ok) throw new Error(await $readErr(res));
    closeComboModal();
    showAppSuccess(editingComboId ? "Đã cập nhật combo" : "Đã thêm combo mới");
    setTimeout(loadCombos, 300);
  } catch (e) {
    showError("comboError", e.message);
  }
};

window.deleteCombo = async function(id) {
  if (!confirm("Xóa combo này?")) return;
  try {
    const res = await $fetch(`${BASE_URL}/api/combos/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error(await $readErr(res));
    showAppSuccess("Đã xóa combo!");
    setTimeout(loadCombos, 300);
  } catch (e) {
    showAppError(e.message);
  }
};

window.toggleComboActive = async function(id) {
  const res = await $fetch(`${BASE_URL}/api/combos/${id}/toggle-active`, { method: "PATCH" });
  if (res.ok) {
    showAppSuccess("Đã cập nhật trạng thái!");
    setTimeout(loadCombos, 300);
  } else {
    showAppError(await $readErr(res));
  }
};

// ===== WEBSOCKET =====
function connectComboSocket() {
  try {
    if (typeof SockJS === "undefined" || typeof Stomp === "undefined") return;
    const sock = new SockJS(`${BASE_URL}/ws`);
    const stomp = Stomp.over(sock);
    stomp.debug = () => {};
    stomp.connect({}, () => {
      stomp.subscribe("/topic/combos", (msg) => {
        if (msg.body === "UPDATED") loadCombos();
      });
    });
  } catch (e) {
    console.warn("WS combo error:", e);
  }
}
