const role = localStorage.getItem("role");
const fullname = localStorage.getItem("fullname");

if (fullname) {
    document.getElementById("userInfo").innerHTML = `
        <p style="color: white;">Xin chào, ${fullname}</p>
        <a href="#" style="color: #00f;" onclick="logout()">Đăng xuất</a>
    `;
}
if (role === "MANAGER") {
    document.getElementById("adminActions").style.display = "block";
}

function toggleSidebar() {
    const sidebar = document.getElementById("sidebar");
    sidebar.classList.toggle("active");
}

function logout() {
    localStorage.clear();
    window.location.href = "/login.html";
}
