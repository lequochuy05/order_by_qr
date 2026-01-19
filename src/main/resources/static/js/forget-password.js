document.addEventListener("DOMContentLoaded", () => {
  const recoverForm = document.getElementById("recoverForm");
  const otpForm = document.getElementById("otpForm");
  const resetForm = document.querySelector(".modal-form[data-type='reset']");

  const emailInput = document.getElementById("recoverEmail");
  const phoneInput = document.getElementById("recoverPhone");
  const phonePrefix = document.getElementById("phonePrefix");

  let currentPhone = null; // lưu lại phone khi gửi OTP

  // ====== CASE 1: Quên mật khẩu (email / phone) ======
  if (recoverForm) {
    recoverForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const method = document.querySelector("input[name='recover-option']:checked").value;

      if (method === "email") {
        const email = emailInput.value.trim();
        if (!email) {
          showError("Vui lòng nhập email!");
          return;
        }

        try {
          const response = await fetch(`${BASE_URL}/api/auth/forgot-password`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ email })
          });

          if (response.ok) {
            showSuccess("Đã gửi link đặt lại mật khẩu. Vui lòng kiểm tra email.");
          } else {
            const errMsg = await readErr(response);
            showError(errMsg || "Không thể gửi email.");
          }
        } catch (err) {
          showError("Không thể kết nối server.");
        }
      } else {
        const phone = phoneInput.value.trim();
        if (!phone) {
          showError("Vui lòng nhập số điện thoại!");
          return;
        }

        currentPhone = phonePrefix.value + phone;

        try {
          const response = await fetch(`${BASE_URL}/api/auth/forgot-password-phone`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ phone: currentPhone })
          });

          if (response.ok) {
            showInfo("OTP đã được gửi về số điện thoại của bạn.");
            recoverForm.style.display = "none";
            otpForm.style.display = "flex";
          } else {
            const errMsg = await readErr(response);
            showError(errMsg || "Không thể gửi OTP.");
          }
        } catch (err) {
          showError("Không thể kết nối server.");
        }
      }
    });
  }

  // ====== CASE 2: Xác nhận OTP ======
  if (otpForm) {
    otpForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const otp = document.getElementById("otpCode").value.trim();
      const newPassword = document.getElementById("newPassword").value;
      const confirmPassword = document.getElementById("confirmPassword").value;

      if (newPassword !== confirmPassword) {
        showError("Mật khẩu nhập lại không khớp");
        return;
      }

      try {
        const response = await fetch(`${BASE_URL}/api/auth/reset-password-phone`, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({ phone: currentPhone, otp, newPassword })
        });

        if (response.ok) {
          showSuccess("Đặt lại mật khẩu thành công! Hãy đăng nhập lại.");
          setTimeout(() => {
            window.location.href = "login.html";
          }, 1500);
        } else {
          const errMsg = await readErr(response);
          showError(errMsg || "OTP không hợp lệ hoặc đã hết hạn.");
        }
      } catch (err) {
        showError("Không thể kết nối server.");
      }
    });
  }

  // ====== CASE 3: Đặt lại bằng link email (token) ======
  if (resetForm) {
    // Lấy token từ URL (?token=xxxx)
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get("token");

    if (!token) {
      showError("Token không hợp lệ");
      return;
    }

    resetForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const newPassword = document.getElementById("newPassword").value;
      const confirmPassword = document.getElementById("confirmPassword").value;

      if (newPassword !== confirmPassword) {
        showError("Mật khẩu nhập lại không khớp");
        return;
      }

      try {
        const response = await fetch(`${BASE_URL}/api/auth/reset-password`, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({ token, newPassword })
        });

        if (response.ok) {
          showSuccess("Đặt lại mật khẩu thành công! Hãy đăng nhập lại.");
          setTimeout(() => {
            window.location.href = "login.html";
          }, 1500);
        } else {
          const err = await readErr(response);
          showError("Lỗi: " + err);
        }
      } catch (error) {
        console.error("Error:", error);
        showError("Không thể kết nối đến server.");
      }
    });
  }
});
