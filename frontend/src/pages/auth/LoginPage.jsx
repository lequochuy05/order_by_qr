import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@features/auth';
import { authService } from '@shared/api/authService.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { showSuccessToast } from '@shared/lib/toast.js';
import {
  Lock,
  Mail,
  Loader2,
  AlertCircle,
  ShieldCheck,
  ArrowLeft,
  RefreshCw,
  KeyRound,
  UtensilsCrossed,
} from 'lucide-react';

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
    <div
      className="relative flex min-h-screen items-center justify-center bg-brand-surface bg-[#F7F0E7] p-4 transition-colors dark:bg-[#14100E]"
      style={{ backgroundColor: 'var(--brand-surface, #F7F0E7)' }}
    >
      {/* Decorative Warm Ambient Glow */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-32 -left-32 h-96 w-96 rounded-full bg-[#C17C4A]/10 blur-3xl" />
        <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-[#56372B]/10 blur-3xl dark:bg-[#C17C4A]/5" />
      </div>

      {/* Main Card */}
      <div
        className="relative w-full max-w-md rounded-3xl border border-brand-border border-[#E8DCD1] bg-white p-8 shadow-xl transition-all animate-in fade-in zoom-in duration-300 dark:border-[#352C26] dark:bg-[#1F1A17] md:p-10"
        style={{ borderColor: 'var(--border-color, #E8DCD1)' }}
      >
        {/* ================= GIAO DIỆN XÁC THỰC 2FA (ADMIN) ================= */}
        {twoFactorData ? (
          <div>
            <button
              type="button"
              onClick={handleBackToLogin}
              className="mb-4 inline-flex items-center gap-1.5 text-xs font-bold text-slate-500 transition-colors hover:text-[#56372B] dark:text-slate-400 dark:hover:text-[#D8986A]"
            >
              <ArrowLeft size={16} /> Quay lại đăng nhập
            </button>

            <div className="mb-6 text-center">
              <div className="mb-3 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-700 shadow-inner dark:bg-amber-500/15 dark:text-amber-400">
                <ShieldCheck size={32} />
              </div>
              <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-slate-100">
                Xác Thực 2 Bước (2FA)
              </h1>
              <p className="mt-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
                Tài khoản Quản trị viên yêu cầu xác thực bảo mật. Hệ thống đã gửi mã OTP 6 chữ số
                tới:
                <br />
                <span className="mt-1 inline-block font-bold text-slate-800 dark:text-slate-200">
                  {twoFactorData.email || 'email quản trị'}
                </span>
              </p>
            </div>

            {/* Hiển thị lỗi */}
            {error && (
              <div className="mb-5 flex items-center gap-2.5 rounded-xl border-l-4 border-rose-500 bg-rose-50 p-3.5 text-xs text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">
                <AlertCircle size={18} className="shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleVerify2fa} className="space-y-5">
              <div className="space-y-2">
                <label className="block text-center text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Nhập mã OTP (6 chữ số)
                </label>
                <div className="relative">
                  <KeyRound
                    className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                    size={20}
                  />
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
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-3.5 pl-12 pr-4 text-center font-mono text-2xl font-bold tracking-[0.35em] text-slate-900 outline-none transition-all placeholder:text-slate-300 focus:border-[#C17C4A] focus:bg-white focus:ring-4 focus:ring-[#C17C4A]/15 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    placeholder="••••••"
                  />
                </div>
              </div>

              {/* Countdown / Resend OTP */}
              <div className="flex items-center justify-between px-1 text-xs text-slate-500 dark:text-slate-400">
                <span>Hết hạn trong 5 phút</span>
                {resendCooldown > 0 ? (
                  <span className="font-medium text-slate-400 dark:text-slate-500">
                    Gửi lại mã ({resendCooldown}s)
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={isResending}
                    className="inline-flex items-center gap-1 font-bold text-[#C17C4A] hover:underline"
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
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-primary bg-[#56372B] py-3.5 text-base font-bold text-white shadow-md transition-all hover:bg-[#432A21] active:scale-[0.98] disabled:opacity-50 dark:bg-brand-secondary dark:bg-[#C17C4A] dark:hover:bg-[#A86537]"
                style={{ backgroundColor: '#56372B', color: '#FFFFFF' }}
              >
                {isSubmitting ? (
                  <Loader2 className="animate-spin text-white" size={20} />
                ) : (
                  <span className="text-white font-bold">XÁC NHẬN ĐĂNG NHẬP</span>
                )}
              </button>
            </form>
          </div>
        ) : (
          /* ================= GIAO DIỆN ĐĂNG NHẬP CHÍNH ================= */
          <div>
            {/* Logo & Header */}
            <div className="mb-8 text-center">
              <div
                className="mb-3 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-primary bg-[#56372B] text-[#F7F0E7] shadow-md dark:bg-brand-secondary dark:bg-[#C17C4A]"
                style={{ backgroundColor: '#56372B', color: '#F7F0E7' }}
              >
                <UtensilsCrossed size={28} />
              </div>
              <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-slate-100">
                Đăng Nhập
              </h1>
              <p className="mt-1 text-xs font-medium text-slate-500 dark:text-slate-400">
                QROS - Hệ thống gọi món bằng mã QR
              </p>
            </div>

            {/* Hiển thị lỗi */}
            {error && (
              <div className="mb-6 flex items-center gap-3 rounded-xl border-l-4 border-rose-500 bg-rose-50 p-3.5 text-xs font-semibold text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">
                <AlertCircle size={18} className="shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              {/* Email Input */}
              <div className="space-y-1.5">
                <label className="ml-1 text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Email
                </label>
                <div className="relative">
                  <Mail
                    className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                    size={18}
                  />
                  <input
                    type="email"
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-4 text-sm font-semibold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-[#C17C4A] focus:bg-white focus:ring-4 focus:ring-[#C17C4A]/15 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    placeholder="wuchuy.dev@gmail.com"
                    value={credentials.email}
                    onChange={(e) => setCredentials({ ...credentials, email: e.target.value })}
                  />
                </div>
              </div>

              {/* Password Input */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="ml-1 text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                    Mật khẩu
                  </label>
                  <Link
                    to="/forgot-password"
                    className="text-xs font-semibold text-[#C17C4A] transition-colors hover:underline"
                  >
                    Quên mật khẩu?
                  </Link>
                </div>
                <div className="relative">
                  <Lock
                    className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                    size={18}
                  />
                  <input
                    type="password"
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-4 text-sm font-semibold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-[#C17C4A] focus:bg-white focus:ring-4 focus:ring-[#C17C4A]/15 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    placeholder="••••••••"
                    value={credentials.password}
                    onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
                  />
                </div>
              </div>

              {/* Nút Đăng nhập */}
              <button
                type="submit"
                disabled={isSubmitting}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-brand-primary bg-[#56372B] py-3.5 text-sm font-bold text-white shadow-md transition-all hover:bg-[#432A21] active:scale-[0.98] disabled:opacity-50 dark:bg-brand-secondary dark:bg-[#C17C4A] dark:hover:bg-[#A86537]"
                style={{ backgroundColor: '#56372B', color: '#FFFFFF' }}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="animate-spin text-white" size={18} />
                    <span className="text-white font-bold">Đang xác thực...</span>
                  </>
                ) : (
                  <span className="text-white font-bold tracking-wide">ĐĂNG NHẬP</span>
                )}
              </button>
            </form>

            <div
              className="mt-8 border-t border-brand-border border-[#E8DCD1] pt-4 text-center dark:border-[#352C26]"
              style={{ borderColor: 'var(--border-color, #E8DCD1)' }}
            >
              <Link
                to="/"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 transition-colors hover:text-[#56372B] dark:text-slate-400 dark:hover:text-[#D8986A]"
              >
                <ArrowLeft size={14} /> Về trang quét mã QR
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default LoginPage;
