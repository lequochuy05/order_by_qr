// admin/js/items.js

if (role === "MANAGER") {
  document.getElementById("adminActions").style.display = "block";
}

// ================== GLOBAL ==================
let allCategories = [];
let editingItemId = null;  // null = thêm mới, khác null = sửa

const $ = id => document.getElementById(id);

// ================== Khởi động ==================
window.addEventListener("DOMContentLoaded", async () => {
    await fetchCategories();
    await loadMenuItems();

    // Preview ảnh
    const file = $("itemImgFile");
    const prev = $("itemImgPreview");

    if (file && prev) {
        file.addEventListener("change", e => {
            const f = e.target.files?.[0];
            if (!f) {
                prev.style.display = "none";
                prev.src = "";
                return;
            }
            if (!f.type.startsWith("image/")) {
                alert("Vui lòng chọn file ảnh!");
                file.value = "";
                return;
            }
            prev.src = URL.createObjectURL(f);
            prev.style.display = "block";
        });
    }
});

// ================== TẢI DANH MỤC ==================
async function fetchCategories() {
    try {
        const res = await fetch(`${BASE_URL}/api/categories`);
        const data = res.ok ? await res.json() : [];
        allCategories = data;

        const sel = $("itemCategory");
        sel.innerHTML = "";
        data.forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.name;
            sel.appendChild(opt);
        });

        // filter danh mục
        const filter = $("categoryFilter");
        filter.innerHTML = `<option value="ALL">Tất cả</option>`;
        data.forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.name;
            filter.appendChild(opt);
        });

    } catch (e) {
        console.error("Lỗi tải danh mục:", e);
    }
}

// ================== LOAD MENU ITEMS ==================
window.loadMenuItems = async function () {
    const container = $("menuItemContainer");
    const cate = $("categoryFilter").value;

    container.innerHTML =
        `<div style="text-align:center;padding:30px;color:#777;">Đang tải...</div>`;

    const url = cate === "ALL"
        ? `${BASE_URL}/api/menu`
        : `${BASE_URL}/api/menu/category/${cate}`;

    try {
        const res = await fetch(url);
        if (!res.ok) throw new Error("Không tải được món ăn");
        const items = await res.json();

        if (!items.length) {
            container.innerHTML = `<div style="padding:40px;color:#777;text-align:center;">Không có món</div>`;
            return;
        }

        container.innerHTML = items.map(it => `
            <div class="menu-item-card">
                <img src="${it.img || '/img/noimg.png'}" alt="" class="menu-item-image">
                <h3>${it.name}</h3>
                <p>${it.price.toLocaleString('vi-VN')}đ</p>
                <span>${it.category?.name || ""}</span>

                ${role === "MANAGER" ? `
                <div class="item-actions">
                    <button class="btn" onclick="showEditItem(${it.id})">✏️</button>
                    <button class="btn red" onclick="deleteItem(${it.id})">🗑️</button>
                </div>
                ` : ""}
            </div>
        `).join("");

    } catch (e) {
        container.innerHTML = `<div style="padding:40px;color:red;text-align:center;">${e.message}</div>`;
    }
};

// ================== MỞ MODAL THÊM ==================
window.showAddItem = function () {
    editingItemId = null;

    $("itemModalTitle").textContent = "Thêm món ăn";
    $("itemName").value = "";
    $("itemPrice").value = "";
    $("itemImgFile").value = "";
    $("itemImgPreview").style.display = "none";
    $("itemError").textContent = "";

    $("itemModal").style.display = "flex";
};

// ================== MỞ MODAL SỬA ==================
window.showEditItem = async function (id) {
    editingItemId = id;
    $("itemModalTitle").textContent = "Sửa món ăn";

    try {
        const res = await fetch(`${BASE_URL}/api/menu/${id}`);
        if (!res.ok) throw new Error("Không tìm thấy món ăn");

        const it = await res.json();

        $("itemName").value = it.name;
        $("itemPrice").value = it.price;
        $("itemCategory").value = it.category?.id || allCategories[0]?.id;

        if (it.img) {
            $("itemImgPreview").src = it.img;
            $("itemImgPreview").style.display = "block";
        } else {
            $("itemImgPreview").style.display = "none";
        }

        $("itemError").textContent = "";
        $("itemModal").style.display = "flex";

    } catch (e) {
        alert(e.message);
    }
};

// ================== ĐÓNG MODAL ==================
window.closeItemModal = function () {
    $("itemModal").style.display = "none";
};

// ================== LƯU (THÊM + SỬA) ==================
window.submitItem = async function () {
    const name = $("itemName").value.trim();
    const price = Number($("itemPrice").value);
    const cateId = Number($("itemCategory").value);
    const file = $("itemImgFile").files[0];

    if (!name) return $("itemError").textContent = "Tên món không được để trống";
    if (price < 0 || isNaN(price)) return $("itemError").textContent = "Giá không hợp lệ";

    try {
        let res;

        // === 1) Nếu đang sửa ===
        if (editingItemId) {
            res = await $fetch(`${BASE_URL}/api/menu/${editingItemId}`, {
                method: "PUT",
                body: JSON.stringify({ name, price, category: { id: cateId } })
            });
            if (!res.ok) throw new Error(await $readErr(res));

        } else {
            // === 2) Thêm món mới ===
            res = await $fetch(`${BASE_URL}/api/menu`, {
                method: "POST",
                body: JSON.stringify({ name, price, category: { id: cateId } })
            });
            if (!res.ok) throw new Error(await $readErr(res));

            editingItemId = (await res.json()).id;
        }

        // === 3) Upload ảnh nếu có ===
        if (file) {
            const fd = new FormData();
            fd.append("file", file);

            const up = await $fetch(`${BASE_URL}/api/menu/${editingItemId}/image`, {
                method: "POST",
                body: fd
            });
            if (!up.ok) throw new Error(await $readErr(up));
        }

        closeItemModal();
        loadMenuItems();

    } catch (e) {
        $("itemError").textContent = e.message || "Có lỗi xảy ra";
    }
};

// ================== XÓA ==================
window.deleteItem = async function (id) {
    if (!confirm("Xóa món ăn này?")) return;

    try {
        const res = await $fetch(`${BASE_URL}/api/menu/${id}`, { method: "DELETE" });
        if (!res.ok) throw new Error(await $readErr(res));

        loadMenuItems();
    } catch (e) {
        alert(e.message);
    }
};
