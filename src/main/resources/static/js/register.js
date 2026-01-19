// resources/static/js/register.js

document.querySelector('.register-form').addEventListener('submit', async (e) => {
  e.preventDefault();

  const fullName = document.getElementById('fullName').value.trim();
  const phone = document.getElementById('phone').value.trim();
  const email = document.getElementById('email').value.trim();
  const role = document.getElementById('role').value; // "" | STAFF | MANAGER
  const password = document.getElementById('password').value;
  const confirmPassword = document.getElementById('confirmPassword').value;

  // ===== Client-side validation =====
  if (!fullName || !email || !role || !password || !confirmPassword) {
    showError('Vui lòng điền đầy đủ thông tin.', 'Thiếu thông tin');
    return;
  }
  if (!/^\S+@\S+\.\S+$/.test(email)) {
    showError('Email không hợp lệ. (Ví dụ: abcz@gmail.com)', 'Sai định dạng');
    return;
  }
  if (password.length < 6) {
    showError('Mật khẩu tối thiểu 6 ký tự.', 'Mật khẩu yếu');
    return;
  }
  if (password !== confirmPassword) {
    showError('Mật khẩu xác nhận không trùng khớp!', 'Xác nhận mật khẩu');
    return;
  }

  const payload = { fullName, phone, email, password, role: role.toUpperCase(), status: 'ACTIVE' };

  // UX: khóa nút khi submit
  const btn = document.querySelector('.login-btn');
  const oldText = btn.textContent;
  btn.disabled = true;
  btn.textContent = 'Đang tạo...';

  try {
    const res = await fetch(`${BASE_URL}/api/users/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const msg = await window.readErr(res); // dùng hàm global trong common.js
      // Gợi ý thông điệp theo mã lỗi
      let hint = '';
      if (res.status === 409) hint = '<br>• Email đã tồn tại.';
      else if (res.status >= 500) hint = '<br>• Máy chủ đang gặp sự cố, thử lại sau.';

      showError((msg || 'Đăng ký thất bại.') + hint, 'Đăng ký thất bại');
      return;
    }

    // Thành công: có thể show modal rồi chuyển trang
    showSuccess('Đăng ký thành công!', 'Thành công');
    setTimeout(() => { window.location.href = '/login.html'; }, 400);
  } catch (err) {
    showError('Lỗi kết nối server: ' + err.message, 'Kết nối thất bại');
  } finally {
    btn.disabled = false;
    btn.textContent = oldText;
  }
});
