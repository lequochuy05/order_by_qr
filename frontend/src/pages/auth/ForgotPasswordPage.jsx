import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '@shared/api/authService.js';
import { Mail, ArrowLeft, Loader2, AlertCircle, CheckCircle, KeyRound } from 'lucide-react';

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.trim()) {
      setError('Vui lòng nhập email');
      return;
    }

    setIsSubmitting(true);
    try {
      await authService.forgotPassword(email.trim());
      setSent(true);
    } catch (err) {
      if (err.data?.details?.retryAfterSeconds) {
        setError(`Vui lòng thử lại sau ${err.data.details.retryAfterSeconds} giây`);
      } else if (err.status === 429) {
        setError('Gửi yêu cầu quá nhiều lần. Vui lòng thử lại sau.');
      } else {
        setError(err.message || 'Gửi yêu cầu thất bại');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-slate-50 p-4 transition-colors dark:bg-[#0B0F17]">
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-32 -left-32 h-96 w-96 rounded-full bg-indigo-500/15 blur-3xl dark:bg-indigo-500/10" />
        <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-violet-500/15 blur-3xl dark:bg-violet-500/10" />
      </div>

      <div className="relative w-full max-w-md rounded-3xl border border-slate-200/80 bg-white p-8 shadow-2xl transition-all animate-in fade-in zoom-in duration-300 dark:border-slate-800/80 dark:bg-slate-900/90 md:p-10">
        {sent ? (
          <div className="text-center">
            <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-tr from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/25">
              <CheckCircle size={32} />
            </div>
            <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-white">
              Đã Gửi Hướng Dẫn
            </h1>
            <p className="mt-2 text-xs leading-relaxed text-slate-600 dark:text-slate-400">
              Chúng tôi đã gửi link đặt lại mật khẩu tới email{' '}
              <span className="font-mono font-bold text-slate-900 dark:text-slate-100">{email}</span>. Vui
              lòng kiểm tra hộp thư của bạn trong vài phút.
            </p>
            <div className="mt-6 flex flex-col gap-3">
              <button
                type="button"
                onClick={() => {
                  setSent(false);
                  setError('');
                }}
                className="text-xs font-bold text-indigo-600 hover:underline dark:text-indigo-400"
              >
                Gửi lại email khác
              </button>
              <Link
                to="/login"
                className="inline-flex items-center justify-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-indigo-600 dark:text-slate-400 dark:hover:text-indigo-400"
              >
                <ArrowLeft size={14} /> Quay lại đăng nhập
              </Link>
            </div>
          </div>
        ) : (
          <div>
            <div className="mb-8 text-center">
              <div className="mb-3 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
                <KeyRound size={24} />
              </div>
              <h1 className="text-2xl font-black tracking-tight text-slate-900 dark:text-white">
                Quên Mật Khẩu
              </h1>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
                Nhập email đăng ký để nhận liên kết đặt lại mật khẩu
              </p>
            </div>

            {error && (
              <div className="mb-6 flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50/90 p-3.5 text-xs font-semibold text-rose-700 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-300">
                <AlertCircle size={18} className="shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
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
                    value={email}
                    className="w-full rounded-2xl border border-slate-200/80 bg-slate-50/50 py-3 pl-11 pr-4 text-sm font-semibold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:ring-4 focus:ring-indigo-500/15 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-100"
                    placeholder="wuchuy.dev@gmail.com"
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 py-3.5 text-sm font-bold text-white shadow-lg shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:opacity-50"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="animate-spin text-white" size={18} />
                    <span className="text-white font-bold">Đang gửi...</span>
                  </>
                ) : (
                  <span className="text-white font-bold tracking-wide">GỬI YÊU CẦU</span>
                )}
              </button>
            </form>

            <div className="mt-8 border-t border-slate-200/80 pt-4 text-center dark:border-slate-800">
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 transition-colors hover:text-indigo-600 dark:text-slate-400 dark:hover:text-indigo-400"
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

export default ForgotPasswordPage;
