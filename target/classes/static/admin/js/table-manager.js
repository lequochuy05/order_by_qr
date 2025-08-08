const BASE_URL = "http://192.168.1.8:8080";

let stompClient = null;
const previousOrders = {};

// load danh sách bàn
function loadTables() {
    const selectedFilter = document.getElementById("tableFilter").value; // lấy trạng thái lọc

    fetch(BASE_URL + "/api/tables")
        .then(res => res.json())
        .then(tables => {
            const container = document.getElementById('tableContainer');
            container.innerHTML = '';

            tables
            .filter(table => selectedFilter === "ALL" || table.status === selectedFilter) // lọc theo trạng thái
            .sort((a, b) => parseInt(a.tableNumber) - parseInt(b.tableNumber))
            .forEach(table => {
                const card = document.createElement('div');
                card.className = 'table-card';

                fetch(`${BASE_URL}/api/orders/table/${table.id}/current`)
                    .then(res => res.ok ? res.json() : null)
                    .then(order => {
                        card.innerHTML = `
                            <h3>Bàn ${table.tableNumber}</h3>
                            <p class="status">Trạng thái: ${table.status}</p>
                            <p>Tổng tiền: ${order ? order.totalAmount.toLocaleString('vi-VN') + ' VND' : '0 VND'}</p>
                            <button class="btn btn-detail" onclick="showDetails(${table.id})">Chi tiết</button>
                            ${order ? `<button class='btn btn-pay' onclick='pay(${order.id}, ${table.id})'>Thanh toán</button>` : ''}
                            ${role === 'MANAGER' ? `
                                <button class="btn" onclick="showEditTable(${table.id})">✏️</button>
                                <button class="btn" onclick="deleteTable(${table.id})">🗑️</button>` : ''}
                        `;
                        container.appendChild(card);

                        if (order) {
                            const prev = previousOrders[table.id];
                            const itemCount = order.orderItems?.length || 0;

                            if (!prev || order.totalAmount !== prev.totalAmount || itemCount !== prev.itemCount) {
                                highlightCard(card);
                            }

                            previousOrders[table.id] = {
                                totalAmount: order.totalAmount,
                                itemCount: itemCount
                            };
                        }
                    });
            });
        })
        .catch(error => {
            console.error("Lỗi khi tải danh sách bàn:", error);
        });
}

// Hiển thị modal thêm bàn
let currentOpenTableId = null;
function showAddTable() {
    document.getElementById("newTableNumber").value = "";
    document.getElementById("addTableModal").style.display = "flex";
}

// Đóng modal thêm bàn
function closeAddTableModal() {
    document.getElementById("addTableModal").style.display = "none";
}

// Thêm bàn mới
async function submitNewTable() {
const number = document.getElementById("newTableNumber").value.trim();
const capacity = document.getElementById("newTableCapacity").value.trim();

if (!number || !capacity) {
    alert("Vui lòng nhập đầy đủ số bàn và sức chứa!");
    return;
}

try {
    const response = await fetch(`${BASE_URL}/api/tables`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ 
            qrCodeUrl: "",
                tableNumber: String(number),
            status: "Trống",
            capacity: capacity
        })
    });

    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Lỗi khi thêm bàn mới!");
    }

    closeAddTableModal();
    loadTables();
} catch (error) {
    console.error(error);
    alert("Đã xảy ra lỗi: " + error.message);
}
}

// Hiển thị modal cập nhật bàn
let currentEditTableId = null;
async function showEditTable(id) {
    try {
        const res = await fetch(`${BASE_URL}/api/tables/${id}`);
        if (!res.ok) throw new Error("Không lấy được thông tin bàn");
        const table = await res.json();

        currentEditTableId = id;

        document.getElementById("editTableNumber").value = table.tableNumber;
        document.getElementById("editQrCodeUrl").value = table.qrCodeUrl || "";
        document.getElementById("editStatus").value = table.status;
        document.getElementById("editCapacity").value = table.capacity;

        document.getElementById("editTableModal").style.display = "flex";
    } catch (error) {
        alert(error.message);
    }
}

// Đóng modal cập nhật bàn
function closeEditTableModal() {
    document.getElementById("editTableModal").style.display = "none";
}

// Cập nhật thông tin bàn
async function submitEditTable() {
    const status = document.getElementById("editStatus").value;
    const capacity = document.getElementById("editCapacity").value;

    if (!status || !capacity) {
        alert("Vui lòng nhập đầy đủ thông tin!");
        return;
    }

    try {
        const res = await fetch(`${BASE_URL}/api/tables/${currentEditTableId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status, capacity })

        });
        alert("Cập nhật bàn thành công!");
        if (!res.ok) throw new Error("Cập nhật bàn thất bại");

        closeEditTableModal();
        loadTables();
    } catch (error) {
        alert(error.message);
    }
}

// Xóa bàn
async function deleteTable(id) {
    if (!confirm("Bạn có chắc muốn xóa bàn này không?")) return;

    try {
        const res = await fetch(`${BASE_URL}/api/tables/${id}`, {
            method: "DELETE"
        });

        if (!res.ok) throw new Error("Không thể xóa bàn.");

        loadTables();
    } catch (error) {
        console.error(error);
        alert("Lỗi khi xóa bàn: " + error.message);
    }
}

// Hiển thị chi tiết đơn hàng
function showDetails(tableId) {
    currentOpenTableId = tableId;
    fetch(`${BASE_URL}/api/orders/table/${tableId}/current`)
        .then(res => res.json())
        .then(order => {
            const modalBody = document.getElementById("modalBody");
            modalBody.innerHTML = order.orderItems.map(item => `
                <div class="order-item">
                    <strong>${item.menuItem.name} x${item.quantity}</strong>
                    ${item.notes ? `<div class="order-note">Ghi chú: ${item.notes}</div>` : ''}
                    <div>
                        ${item.prepared 
                            ? `<span class="status-prepared">Đã làm</span>` 
                            : `<button class="btn-prepared" onclick="markPrepared(${item.id})">Đã xong</button>`}
                    </div>
                </div>
            `).join('');
            document.getElementById("modal").style.display = "flex";
        });
}

// Đóng modal chi tiết đơn hàng
function closeModal() {
    currentOpenTableId = null;
    document.getElementById("modal").style.display = "none";
}

// Đánh dấu món đã làm
function markPrepared(itemId) {
    fetch(`${BASE_URL}/api/orders/items/${itemId}/prepared`, { method: 'PUT' })
        .then(() => {
            if (currentOpenTableId) showDetails(currentOpenTableId);
        });
}

// Thanh toán đơn hàng
function pay(orderId, tableId) {
    const userId = localStorage.getItem("userId");
    if (!userId) {
        alert("Không xác định được người dùng!");
        return;
    }

    fetch(`${BASE_URL}/api/orders/${orderId}/pay?userId=${userId}`, { method: 'PUT' })
        .then(response => {
            if (!response.ok) throw new Error("Lỗi khi thanh toán");
            alert("Thanh toán thành công!");
            loadTables(); 
        })
        .catch(error => {
            console.error("Lỗi:", error);
            alert("Thanh toán thất bại: " + error.message);
        });
}

// Kết nối WebSocket
function connectWebSocket() {
    const socket = new SockJS(BASE_URL + "/ws");
    stompClient = Stomp.over(socket);
    stompClient.connect({}, () => {
        stompClient.subscribe("/topic/tables", () => {
            //console.log("Nhận cập nhật WebSocket từ server");
            loadTables();
        });
    });
}

loadTables();
connectWebSocket();


// Hàm highlight card
    function highlightCard(cardElement) {
    cardElement.classList.add('highlight');
    setTimeout(() => {
        cardElement.classList.remove('highlight');
    }, 5000);
}



