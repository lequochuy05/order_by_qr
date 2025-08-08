const BASE_URL = "http://192.168.1.8:8080";

let allCategories = [];
let editingItemId = null;

// Load danh mục và món ăn khi trang tải
window.onload = function () {
    fetchCategories();
    loadMenuItems();
};

// Lọc món theo danh mục
function fetchCategories() {
    fetch(`${BASE_URL}/api/categories`)
        .then(res => res.json())
        .then(data => {
            allCategories = data;
            const select = document.getElementById("categoryFilter");
            data.forEach(cate => {
                const option = document.createElement("option");
                option.value = cate.id;
                option.text = cate.name;
                select.appendChild(option);
            });
        });
}

// Hiển thị danh sách món ăn
function loadMenuItems() {
    const container = document.getElementById("menuItemContainer");
    container.innerHTML = "";

    const categoryId = document.getElementById("categoryFilter").value;
    let url = `${BASE_URL}/api/menu`;
    if (categoryId !== "ALL") {
        url = `${BASE_URL}/api/menu/category/${categoryId}`;
    }

    fetch(url)
        .then(res => res.json())
        .then(data => {
            data.forEach(item => {
                const div = document.createElement("div");
                div.className = "table-card";
                div.innerHTML = `
                    <div class="table-card-content" style="text-align: center;">
                        ${item.img ? `<img src="${item.img}" alt="${item.name}" style="width:50%; height:150px; object-fit:cover; border-radius: 10px;">` : ''}
                        <h3>${item.name}</h3>
                        <p><strong>Giá:</strong> ${item.price.toLocaleString()}đ</p>
                        ${role === 'MANAGER' ? `
                            <div class="card-actions">
                                <button class="btn" onclick="editItem(${item.id})">Sửa</button>
                                <button class="btn red" onclick="deleteItem(${item.id})">Xóa</button>
                            </div>
                        ` : ''}
                    </div>
                `;
                container.appendChild(div);
            });
        });
}

// Modal thêm món
function showAddItemModal() {}

function closeAddItemModal() {
    document.getElementById("addItemModal").style.display = "none";
}

function submitNewItem() {}

// Modal cập nhật món
function showEditItemModal(id){}

function closeEditItemModal() {
    document.getElementById("editItemModal").style.display = "none";
}

function submitEditItem() {}

// Modal xóa món
function deleteItem(id) {}
