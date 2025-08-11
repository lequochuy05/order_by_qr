// resource/static/js/register.js
const BASE_URL = window.APP_BASE_URL || location.origin;

document.querySelector('.register-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const fullName = document.getElementById('fullName').value.trim();
    const phone = document.getElementById('phone').value.trim();
    const email = document.getElementById('email').value.trim();
    const role = document.getElementById('role').value; // "" | STAFF | MANAGER
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (!fullName || !email || !role) {
    alert('Vui lòng nhập Họ tên, Email và chọn Vai trò.');
    return;
    }
    if (password !== confirmPassword) {
    alert('Mật khẩu xác nhận không trùng khớp!');
    return;
    }

    // status mặc định ACTIVE (backend cũng có thể áp mặc định)
    const payload = { fullName, phone, email, password, role: role.toUpperCase(), status: 'ACTIVE' };

    try {
    const res = await fetch(`${BASE_URL}/api/users/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        const msg = await readErr(res);
        alert('Đăng ký thất bại: ' + msg);
        return;
    }

    alert('Đăng ký thành công!');
    // Chuyển sang trang đăng nhập
    window.location.href = '/login.html';
    } catch (err) {
    alert('Lỗi kết nối server: ' + err.message);
    }
});