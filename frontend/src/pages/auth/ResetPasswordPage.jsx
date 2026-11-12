import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { authService } from '@shared/api/authService.js';
import { Lock, Loader2, AlertCircle, CheckCircle, Eye, EyeOff, ShieldCheck, ArrowLeft } from 'lucide-react';

const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!token) {
      setError('Token không hợp lệ. Vui lòng sử dụng link trong email.');
      return;
    }

    if (!password.trim() || !confirmPassword.trim()) {
      setError('Vui lòng điền đầy đủ thông tin');
      return;
    }

    if (password.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự');
      return;
    }

    if (!/[A-Z]/.test(password)) {
      setError('Mật khẩu phải chứa ít nhất 1 chữ hoa');
      return;
    }

    if (!/[0-9]/.test(password)) {
      setError('Mật khẩu phải chứa ít nhất 1 số');
      return;
    }

    if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      setError('Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt');
      return;
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      return;
    }

    setIsSubmitting(true);
    try {
      await authService.resetPassword(token, password);
      setSuccess(true);
      setTimeout(() => navigate('/login'), 3000);
    } catch (err) {
      if (err.data?.details?.retryAfterSeconds) {
        setError(`Vui lòng thử lại sau ${err.data.details.retryAfterSeconds} giây`);
      } else if (err.status === 429) {
        setError('Yêu cầu quá nhiều lần. Vui lòng thử lại sau.');
      } else if (err.message?.includes('expired') || err.message?.includes('invalid')) {
        setError('Link đặt lại mật khẩu đã hết hạn hoặc không hợp lệ.');
      } else {
        setError(err.message || 'Đặt lại mật khẩu thất bại');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="relative flex min-h-screen items-center justify-center bg-brand-surface bg-[#F7F0E7] p-4 transition-colors dark:bg-[#14100E]"
      style={{ backgroundColor: 'var(--brand-surface, #F7F0E7)' }}
    >
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-32 -left-32 h-96 w-96 rounded-full bg-[#C17C4A]/10 blur-3xl" />
        <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-[#56372B]/10 blur-3xl dark:bg-[#C17C4A]/5" />
      </div>

      <div
        className="relative w-full max-w-md rounded-3xl border border-brand-border border-[#E8DCD1] bg-white p-8 shadow-xl transition-all animate-in fade-in zoom-in duration-300 dark:border-[#352C26] dark:bg-[#1F1A17] md:p-10"
        style={{ borderColor: 'var(--border-color, #E8DCD1)' }}
      >
        {!token ? (
          <div className="text-center">
            <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-50 text-rose-600 shadow-inner dark:bg-rose-500/15 dark:text-rose-400">
              <AlertCircle size={32} />
            </div>
            <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-slate-100">
              Link Không Hợp Lệ
            </h1>
            <p className="mt-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
              Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn sử dụng.
            </p>
            <div className="mt-6">
              <Link
                to="/forgot-password"
                className="inline-flex items-center gap-1.5 text-xs font-bold text-[#C17C4A] hover:underline"
              >
                Yêu cầu gửi lại link mới
              </Link>
            </div>
          </div>
        ) : success ? (
          <div className="text-center">
            <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 shadow-inner dark:bg-emerald-500/15 dark:text-emerald-400">
              <CheckCircle size={32} />
            </div>
            <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-slate-100">
              Đặt Lại Thành Công!
            </h1>
            <p className="mt-2 text-xs leading-relaxed text-slate-500 dark:text-slate-400">
              Mật khẩu mới đã được cập nhật. Bạn đang được chuyển hướng về trang đăng nhập...
            </p>
            <div className="mt-6">
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 rounded-xl bg-brand-primary bg-[#56372B] px-5 py-2.5 text-xs font-bold text-white shadow-md hover:bg-[#432A21] dark:bg-brand-secondary dark:bg-[#C17C4A] dark:hover:bg-[#A86537]"
                style={{ backgroundColor: '#56372B', color: '#FFFFFF' }}
              >
                Đăng nhập ngay
              </Link>
            </div>
          </div>
        ) : (
          <div>
            <div className="mb-8 text-center">
              <div
                className="mb-3 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-primary bg-[#56372B] text-[#F7F0E7] shadow-md dark:bg-brand-secondary dark:bg-[#C17C4A]"
                style={{ backgroundColor: '#56372B', color: '#F7F0E7' }}
              >
                <ShieldCheck size={24} />
              </div>
              <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-slate-100">
                Đặt Lại Mật Khẩu
              </h1>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Nhập mật khẩu mới cho tài khoản quản trị của bạn
              </p>
            </div>

            {error && (
              <div className="mb-6 flex items-center gap-3 rounded-xl border-l-4 border-rose-500 bg-rose-50 p-3.5 text-xs font-semibold text-rose-700 dark:bg-rose-500/10 dark:text-rose-300">
                <AlertCircle size={18} className="shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="space-y-1.5">
                <label className="ml-1 text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Mật khẩu mới
                </label>
                <div className="relative">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-11 text-sm font-semibold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-[#C17C4A] focus:bg-white focus:ring-4 focus:ring-[#C17C4A]/15 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    placeholder="••••••••"
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 transition-colors hover:text-slate-600 dark:hover:text-slate-200"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="ml-1 text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Xác nhận mật khẩu
                </label>
                <div className="relative">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                  <input
                    type="password"
                    value={confirmPassword}
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-4 text-sm font-semibold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-[#C17C4A] focus:bg-white focus:ring-4 focus:ring-[#C17C4A]/15 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                    placeholder="••••••••"
                    onChange={(e) => setConfirmPassword(e.target.value)}
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-brand-primary bg-[#56372B] py-3.5 text-sm font-bold text-white shadow-md transition-all hover:bg-[#432A21] active:scale-[0.98] disabled:opacity-50 dark:bg-brand-secondary dark:bg-[#C17C4A] dark:hover:bg-[#A86537]"
                style={{ backgroundColor: '#56372B', color: '#FFFFFF' }}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="animate-spin text-white" size={18} />
                    <span className="text-white font-bold">Đang cập nhật...</span>
                  </>
                ) : (
                  <span className="text-white font-bold tracking-wide">ĐẶT LẠI MẬT KHẨU</span>
                )}
              </button>
            </form>

            <div
              className="mt-8 border-t border-brand-border border-[#E8DCD1] pt-4 text-center dark:border-[#352C26]"
              style={{ borderColor: 'var(--border-color, #E8DCD1)' }}
            >
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 transition-colors hover:text-[#56372B] dark:text-slate-400 dark:hover:text-[#D8986A]"
              >
                <ArrowLeft size={14} /> Quay lại đăng nhập
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ResetPasswordPage;
