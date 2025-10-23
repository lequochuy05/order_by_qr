// resource/static/admin/js/profile.js

const userId = localStorage.getItem('userId');
let currentUser = null;

// ===== Load User Profile =====
async function loadProfile() {
  if (!userId) {
    showAppError('Không tìm thấy thông tin người dùng');
    setTimeout(() => window.location.href = '/login.html', 2000);
    return;
  }

  try {
    const res = await authFetch(`${BASE_URL}/api/users/${userId}`);
    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Không thể tải thông tin');
    }

    currentUser = await res.json();
    displayProfile(currentUser);
  } catch (e) {
    showAppError(e.message || 'Lỗi tải thông tin người dùng');
  }
}

// ===== Display Profile Data =====
function displayProfile(user) {
  byId('fullName').value = user.fullName || user.full_name || '';
  byId('email').value = user.email || '';
  byId('phone').value = user.phone || '';
  byId('role').value = user.role === 'MANAGER' ? 'Quản lý' : 'Nhân viên';
  
  // Format created date
  const createdAt = user.createdAt || user.created_at;
  if (createdAt) {
    const date = new Date(createdAt);
    byId('createdAt').value = date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // Avatar
  const avatarUrl = user.avatarUrl || user.avatar_url;
  if (avatarUrl) {
    byId('avatarImg').src = avatarUrl + '?v=' + Date.now();
  }
}

// ===== Update Profile =====
async function updateProfile() {
  const fullName = byId('fullName').value.trim();
  const phone = byId('phone').value.trim();

  showError('profileError', '');

  if (!fullName) {
    showError('profileError', 'Vui lòng nhập họ tên');
    byId('fullName').focus();
    return;
  }

  // Validate phone if provided
  if (phone && !/^[0-9]{10,11}$/.test(phone)) {
    showError('profileError', 'Số điện thoại không hợp lệ (10-11 số)');
    byId('phone').focus();
    return;
  }

  try {
    const payload = {
      full_name: fullName,
      phone: phone || null,
      email: currentUser.email,
      role: currentUser.role
    };

    const res = await $fetch(`${BASE_URL}/api/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Cập nhật thất bại');
    }

    // Update localStorage
    localStorage.setItem('fullname', fullName);
    
    // Update display
    if (window.userInfo) {
      window.userInfo.innerHTML = `
        <p style="color: white;">Xin chào, ${fullName}</p>
        <a href="#" style="color: #00f;" onclick="logout()">Đăng xuất</a>
      `;
    }

    // Success animation
    const card = document.querySelector('.info-card');
    card.classList.add('success');
    setTimeout(() => card.classList.remove('success'), 600);

    showAppSuccess('Đã cập nhật thông tin thành công!');
    setTimeout(() => loadProfile(), 500);
  } catch (e) {
    showError('profileError', e.message || 'Có lỗi xảy ra');
  }
}

// ===== Change Password =====
async function changePassword() {
  const currentPassword = byId('currentPassword').value;
  const newPassword = byId('newPassword').value;
  const confirmPassword = byId('confirmPassword').value;

  showError('passwordError', '');

  if (!currentPassword || !newPassword || !confirmPassword) {
    showError('passwordError', 'Vui lòng điền đầy đủ thông tin');
    return;
  }

  if (newPassword.length < 6) {
    showError('passwordError', 'Mật khẩu mới phải có ít nhất 6 ký tự');
    byId('newPassword').focus();
    return;
  }

  if (newPassword !== confirmPassword) {
    showError('passwordError', 'Mật khẩu xác nhận không khớp');
    byId('confirmPassword').focus();
    return;
  }

  if (currentPassword === newPassword) {
    showError('passwordError', 'Mật khẩu mới phải khác mật khẩu hiện tại');
    byId('newPassword').focus();
    return;
  }

  try {
    // First verify current password by trying to login
    const verifyRes = await $fetch(`${BASE_URL}/api/users/login`, {
      method: 'POST',
      body: JSON.stringify({
        email: currentUser.email,
        password: currentPassword
      })
    });

    if (!verifyRes.ok) {
      throw new Error('Mật khẩu hiện tại không đúng');
    }

    // Now update password
    const res = await $fetch(`${BASE_URL}/api/users/${userId}/reset-password`, {
      method: 'PUT',
      body: JSON.stringify({
        password: newPassword
      })
    });

    if (!res.ok) {
      const errMsg = await $readErr(res);
      throw new Error(errMsg || 'Đổi mật khẩu thất bại');
    }

    // Success animation
    const card = document.querySelector('.password-card');
    card.classList.add('success');
    setTimeout(() => card.classList.remove('success'), 600);

    showAppSuccess('Đã đổi mật khẩu thành công!');
    
    // Reset form
    byId('passwordForm').reset();
  } catch (e) {
    showError('passwordError', e.message || 'Có lỗi xảy ra');
  }
}

// ===== Avatar Upload =====
function setupAvatarUpload() {
  const fileInput = byId('avatarFile');
  const uploadBtn = byId('btnUploadAvatar');
  const avatarImg = byId('avatarImg');

  fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) {
      uploadBtn.style.display = 'none';
      return;
    }

    // Validate file type
    if (!file.type.startsWith('image/')) {
      showAppError('Vui lòng chọn file ảnh!');
      fileInput.value = '';
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      showAppError('Ảnh quá lớn! Vui lòng chọn ảnh dưới 5MB.');
      fileInput.value = '';
      return;
    }

    // Preview
    const reader = new FileReader();
    reader.onload = (e) => {
      avatarImg.src = e.target.result;
      uploadBtn.style.display = 'inline-block';
    };
    reader.readAsDataURL(file);
  });

  uploadBtn.addEventListener('click', async () => {
    const file = fileInput.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    try {
      uploadBtn.disabled = true;
      uploadBtn.textContent = '⏳ Đang tải...';

      const res = await authFetch(`${BASE_URL}/api/users/${userId}/avatar`, {
        method: 'POST',
        body: formData
      });

      if (!res.ok) {
        const errMsg = await $readErr(res);
        throw new Error(errMsg || 'Upload thất bại');
      }

      const data = await res.json();
      
      // Success animation
      const card = document.querySelector('.avatar-card');
      card.classList.add('success');
      setTimeout(() => card.classList.remove('success'), 600);

      showAppSuccess('Đã cập nhật ảnh đại diện!');
      
      uploadBtn.style.display = 'none';
      fileInput.value = '';

    localStorage.setItem('avatarUrl', data.avatarUrl);
    document.getElementById('sidebarAvatar').src = data.avatarUrl;

      
      // Reload to get new avatar
      setTimeout(() => loadProfile(), 500);
    } catch (e) {
      showAppError(e.message || 'Không thể tải ảnh lên');
    } finally {
      uploadBtn.disabled = false;
      uploadBtn.textContent = '⬆️ Tải lên';
    }
  });
}

// ===== Toggle Password Visibility =====
window.togglePassword = function(inputId) {
  const input = byId(inputId);
  const btn = input.nextElementSibling;
  
  if (input.type === 'password') {
    input.type = 'text';
    btn.textContent = '🙈';
  } else {
    input.type = 'password';
    btn.textContent = '👁️';
  }
};

// ===== Keyboard Shortcuts =====
document.addEventListener('keydown', (e) => {
  // Ctrl/Cmd + S to save profile
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault();
    const activeForm = document.activeElement.closest('form');
    if (activeForm && activeForm.id === 'profileForm') {
      updateProfile();
    } else if (activeForm && activeForm.id === 'passwordForm') {
      changePassword();
    }
  }
});

// ===== Init =====
window.addEventListener('DOMContentLoaded', () => {
  loadProfile();
  setupAvatarUpload();
  
  // Add tooltips
  byId('email').title = 'Email không thể thay đổi';
  byId('role').title = 'Vai trò được cấp bởi quản lý';
  byId('createdAt').title = 'Ngày tạo tài khoản';
});