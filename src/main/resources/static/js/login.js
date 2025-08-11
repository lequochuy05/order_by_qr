// resource/static/js/login.js
const BASE_URL = window.APP_BASE_URL || location.origin;

document.querySelector('.login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    if (!email || !password) {
    alert('Vui lòng nhập Email và Mật khẩu');
    return;
    }

    try {
    const res = await fetch(`${BASE_URL}/api/users/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ email, password })
    });

    if (!res.ok) {
        let msg = await readErr(res);
        if (!msg || msg === `${res.status} ${res.statusText}`) {
            if (res.status === 401) msg = 'Sai email hoặc mật khẩu';
            if (res.status === 403) msg = 'Bạn không có quyền';
        }
        alert('Đăng nhập thất bại: ' + msg);
        return;
    }

    const data = await res.json();

    // Lưu localStorage để dùng lại ở toàn hệ thống
    localStorage.setItem('userId', String(data.userId));
    localStorage.setItem('fullname', data.fullName || '');
    localStorage.setItem('role', data.role || '');
    localStorage.setItem('accessToken', data.accessToken || '');

    // Điều hướng sau đăng nhập
    window.location.href = '/admin/table-manager.html';
    } catch (err) {
    alert('Lỗi khi kết nối server: ' + err.message);
    }
});
