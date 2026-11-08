import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@features/auth';
import { authService } from '@shared/api/authService.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { showSuccessToast } from '@shared/lib/toast.js';
import { Lock, Mail, Loader2, AlertCircle, ShieldCheck, ArrowLeft, RefreshCw, KeyRound } from 'lucide-react';
import { FaGoogle, FaFacebook, FaTwitter } from 'react-icons/fa';

const LoginPage = () => {
  const [credentials, setCredentials] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 2FA State for ADMIN
  const [twoFactorData, setTwoFactorData] = useState(null); // { preAuthToken, email }
  const [otp, setOtp] = useState('');
  const [resendCooldown, setResendCooldown] = useState(60);
  const [isResending, setIsResending] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  // Cooldown countdown for resending 2FA OTP
  useEffect(() => {
    let timer;
    if (twoFactorData && resendCooldown > 0) {
      timer = setInterval(() => {
        setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [twoFactorData, resendCooldown]);

  const handleLoginSuccess = (data) => {
    login(data);
    if (data?.mustChangePassword) {
      navigate('/admin/profile?forceChangePassword=true');
    } else if (data?.role === 'CHEF') {
      navigate('/admin/kitchen');
    } else if (data?.role === 'STAFF' || data?.role === 'CASHIER') {
      navigate('/admin/tables');
    } else {
      navigate('/admin/dashboard');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    if (!credentials.email || !credentials.password) {
      setError('Vui lòng nhập đầy đủ Email và Mật khẩu');
      setIsSubmitting(false);
      return;
    }

    try {
      const data = await authService.login(credentials.email, credentials.password);

      // Nếu tài khoản ADMIN yêu cầu xác thực 2 bước
      if (data?.requires2fa) {
        setTwoFactorData({
          preAuthToken: data.preAuthToken,
          email: data.email,
        });
        setOtp('');
        setResendCooldown(60);
        return;
      }

      handleLoginSuccess(data);
    } catch (err) {
      const msg = buildErrorMessage(err, { includeDetails: false });
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerify2fa = async (e) => {
    e.preventDefault();
    setError('');

    const trimmedOtp = otp.trim();
    if (!trimmedOtp || trimmedOtp.length !== 6) {
      setError('Vui lòng nhập đủ 6 chữ số mã OTP');
      return;
    }

    setIsSubmitting(true);
    try {
      const data = await authService.verify2fa(twoFactorData.preAuthToken, trimmedOtp);
      showSuccessToast('Xác thực bảo mật 2FA thành công!');
      handleLoginSuccess(data);
    } catch (err) {
      const msg = buildErrorMessage(err, { includeDetails: false });
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResendOtp = async () => {
    if (resendCooldown > 0 || isResending) return;
    setIsResending(true);
    setError('');

    try {
      await authService.resend2fa(twoFactorData.preAuthToken);
      setResendCooldown(60);
      showSuccessToast('Mã OTP mới đã được gửi về email của bạn');
    } catch (err) {
      const msg = buildErrorMessage(err, { includeDetails: false });
      setError(msg);
    } finally {
      setIsResending(false);
    }
  };

  const handleBackToLogin = () => {
    setTwoFactorData(null);
    setOtp('');
    setError('');
    setResendCooldown(60);
  };

  return (
    // Background
    <div className="min-h-screen bg-gradient-to-br from-[#74ebd5] to-[#ACB6E5] flex items-center justify-center p-4">
      {/* Container chính */}
      <div className="bg-white w-full max-w-md rounded-[2rem] shadow-2xl p-8 md:p-12 animate-in fade-in zoom-in duration-300">
        {/* ================= GIAO DIỆN XÁC THỰC 2FA (ADMIN) ================= */}
        {twoFactorData ? (
          <div>
            <button
              type="button"
              onClick={handleBackToLogin}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-gray-500 hover:text-gray-800 transition-colors mb-4"
            >
              <ArrowLeft size={16} /> Quay lại đăng nhập
            </button>

            <div className="text-center mb-6">
              <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-orange-100 text-orange-600 mb-3 shadow-inner">
                <ShieldCheck size={32} />
              </div>
              <h1 className="text-2xl font-bold text-gray-800 tracking-tight">Xác Thực 2 Bước (2FA)</h1>
              <p className="mt-2 text-xs text-gray-500 leading-relaxed">
                Tài khoản Quản trị viên yêu cầu xác thực bảo mật. Hệ thống đã gửi mã OTP 6 chữ số tới:
                <br />
                <span className="font-semibold text-gray-800 text-sm mt-0.5 inline-block">
                  {twoFactorData.email || 'email quản trị'}
                </span>
              </p>
            </div>

            {/* Hiển thị lỗi */}
            {error && (
              <div className="mb-5 p-3.5 bg-red-50 border-l-4 border-red-500 text-red-700 text-xs flex items-center gap-2.5 rounded-r-xl">
                <AlertCircle size={18} className="shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleVerify2fa} className="space-y-5">
              <div className="space-y-2">
                <label className="text-xs font-bold text-gray-600 uppercase tracking-wider block text-center">
                  Nhập mã OTP (6 chữ số)
                </label>
                <div className="relative">
                  <KeyRound className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                  <input
                    type="text"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    pattern="[0-9]*"
                    autoFocus
                    value={otp}
                    onChange={(e) => {
                      const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                      setOtp(val);
                      if (error) setError('');
                    }}
                    className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-100 outline-none transition-all text-center text-2xl font-mono font-bold tracking-[0.35em]"
                    placeholder="••••••"
                  />
                </div>
              </div>

              {/* Countdown / Resend OTP */}
              <div className="flex items-center justify-between text-xs text-gray-500 px-1">
                <span>Hết hạn trong 5 phút</span>
                {resendCooldown > 0 ? (
                  <span className="font-medium text-gray-400">Gửi lại mã ({resendCooldown}s)</span>
                ) : (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={isResending}
                    className="font-bold text-orange-600 hover:text-orange-700 inline-flex items-center gap-1 hover:underline"
                  >
                    {isResending ? (
                      <Loader2 size={13} className="animate-spin" />
                    ) : (
                      <RefreshCw size={13} />
                    )}
                    Gửi lại mã OTP
                  </button>
                )}
              </div>

              {/* Nút xác nhận 2FA */}
              <button
                type="submit"
                disabled={isSubmitting || otp.trim().length !== 6}
                className="w-full bg-orange-500 hover:bg-orange-600 disabled:opacity-50 text-white py-3.5 rounded-xl font-bold text-base shadow-lg shadow-orange-200 hover:-translate-y-0.5 active:scale-95 transition-all flex items-center justify-center gap-2"
              >
                {isSubmitting ? <Loader2 className="animate-spin" size={20} /> : 'XÁC NHẬN ĐĂNG NHẬP'}
              </button>
            </form>
          </div>
        ) : (
          /* ================= GIAO DIỆN ĐĂNG NHẬP CHÍNH ================= */
          <div>
            {/* Tiêu đề */}
            <div className="text-center mb-8">
              <h1 className="text-3xl font-bold text-[#e74c3c] tracking-tight">Đăng Nhập</h1>
              <p className="mt-1 text-xs text-gray-400">Hệ thống gọi món QR & Quản lý nhà hàng</p>
            </div>

            {/* Hiển thị lỗi */}
            {error && (
              <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm flex items-center gap-3 rounded-r-xl">
                <AlertCircle size={20} className="shrink-0" />
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Email Input */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-gray-600 ml-1">Email</label>
                <div className="relative">
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                  <input
                    type="email"
                    className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-[#e74c3c] focus:bg-white focus:ring-4 focus:ring-red-100 outline-none transition-all"
                    placeholder="abc@xyz.com"
                    value={credentials.email}
                    onChange={(e) => setCredentials({ ...credentials, email: e.target.value })}
                  />
                </div>
              </div>

              {/* Password Input */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label className="text-sm font-semibold text-gray-600 ml-1">Mật khẩu</label>
                </div>
                <div className="relative">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
                  <input
                    type="password"
                    className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-[#e74c3c] focus:bg-white focus:ring-4 focus:ring-red-100 outline-none transition-all"
                    placeholder="••••••••"
                    value={credentials.password}
                    onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
                  />
                </div>
                <div className="flex justify-end">
                  <Link to="/forgot-password" className="text-xs text-emerald-600 hover:underline">
                    Quên mật khẩu?
                  </Link>
                </div>
              </div>

              {/* Nút Đăng nhập */}
              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full bg-[#3498db] hover:bg-[#2980b9] text-white py-4 rounded-xl font-bold text-lg shadow-lg shadow-blue-200 hover:-translate-y-1 active:scale-95 transition-all flex items-center justify-center gap-3 mt-4"
              >
                {isSubmitting ? <Loader2 className="animate-spin" /> : 'ĐĂNG NHẬP NGAY'}
              </button>
            </form>

            {/* Social Login */}
            <div className="mt-8">
              <div className="relative flex py-3 items-center">
                <div className="flex-grow border-t border-gray-200"></div>
                <span className="flex-shrink mx-4 text-gray-400 text-sm italic">
                  Hoặc đăng nhập bằng
                </span>
                <div className="flex-grow border-t border-gray-200"></div>
              </div>

              <div className="flex justify-center gap-6 mt-4">
                <button className="p-3 bg-gray-50 rounded-xl hover:scale-110 transition-transform text-[#DB4437]">
                  <FaGoogle size={24} />
                </button>

                <button className="p-3 bg-gray-50 rounded-xl hover:scale-110 transition-transform text-[#4267B2]">
                  <FaFacebook size={24} />
                </button>

                <button className="p-3 bg-gray-50 rounded-xl hover:scale-110 transition-transform text-[#1DA1F2]">
                  <FaTwitter size={24} />
                </button>
              </div>
            </div>

            <p className="text-center mt-8 text-sm text-gray-500">
              Chưa có tài khoản?{' '}
              <a href="#" className="text-emerald-600 font-bold hover:underline">
                Đăng ký ngay
              </a>
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default LoginPage;
