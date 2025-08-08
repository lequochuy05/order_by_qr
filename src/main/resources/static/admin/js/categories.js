
const BASE_URL = "http://192.168.1.8:8080";
let editingCategoryId = null;

function loadCategories() {
    fetch(`${BASE_URL}/api/categories`)
        .then(res => res.json())
        .then(categories => {
            const container = document.getElementById('categoryContainer');
            container.innerHTML = '';
            categories.forEach(cat => {
                const card = document.createElement('div');
                card.className = 'table-card';
                card.innerHTML = `
                    ${cat.img ? `<img src="${cat.img}" alt="${cat.name}" style="width:100%; height:150px; object-fit:cover; border-radius: 10px;">` : ''}
                    <h3 style='text-align: center;'>${cat.name}</h3>
                    ${role === 'MANAGER' ? `
                        <div style='text-align: center;'>
                            <button class="btn" onclick="showEditCategory(${cat.id}, '${cat.name}', '${cat.img || ''}')">✏️</button>
                            <button class="btn" onclick="deleteCategory(${cat.id})">🗑️</button>
                        </div>
                    ` : ''}
                `;
                container.appendChild(card);
            });
        });
}

function showAddCategory() {
    document.getElementById("newCategoryName").value = "";
    document.getElementById("newCategoryImg").value = "";
    document.getElementById("addCategoryModal").style.display = "flex";
}

function closeAddCategoryModal() {
    document.getElementById("addCategoryModal").style.display = "none";
}

async function submitNewCategory() {
    const name = document.getElementById("newCategoryName").value.trim();
    const img = document.getElementById("newCategoryImg").value.trim();
    if (!name) { alert("Vui lòng nhập tên danh mục!"); return; }
    await fetch(`${BASE_URL}/api/categories`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, img })
    });
    closeAddCategoryModal();
    loadCategories();
}

function showEditCategory(id, name, img) {
    editingCategoryId = id;
    document.getElementById("editCategoryName").value = name;
    document.getElementById("editCategoryImg").value = img || "";
    document.getElementById("editCategoryModal").style.display = "flex";
}

function closeEditCategoryModal() {
    document.getElementById("editCategoryModal").style.display = "none";
}

async function submitEditCategory() {
    const name = document.getElementById("editCategoryName").value.trim();
    const img = document.getElementById("editCategoryImg").value.trim();
    if (!name) { alert("Vui lòng nhập tên danh mục!"); return; }
    await fetch(`${BASE_URL}/api/categories/${editingCategoryId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, img })
    });
    closeEditCategoryModal();
    loadCategories();
}

async function deleteCategory(id) {
    if (!confirm("Bạn có chắc muốn xóa danh mục này?")) return;
    await fetch(`${BASE_URL}/api/categories/${id}`, { method: "DELETE" });
    loadCategories();
}
loadCategories();