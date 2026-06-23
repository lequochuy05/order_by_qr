import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '@features/auth';
import { Mail, ArrowLeft, Loader2, AlertCircle, CheckCircle } from 'lucide-react';

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
        setError(
          err.message === 'Email not found'
            ? 'Email không tồn tại trong hệ thống'
            : err.message || 'Gửi yêu cầu thất bại',
        );
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (sent) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#74ebd5] to-[#ACB6E5] flex items-center justify-center p-4">
        <div className="bg-white w-full max-w-md rounded-[2rem] shadow-2xl p-8 md:p-12 animate-in fade-in zoom-in duration-300 text-center">
          <div className="flex justify-center mb-6">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
              <CheckCircle size={36} className="text-green-500" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-4">Đã gửi email!</h1>
          <p className="text-gray-600 mb-6">
            Bạn sẽ nhận được hướng dẫn đặt lại mật khẩu trong vài phút.
          </p>
          <p className="text-sm text-gray-500 mb-8">
            <button
              onClick={() => {
                setSent(false);
                setError('');
              }}
              className="text-emerald-600 hover:underline font-medium"
            >
              Gửi lại
            </button>
          </p>
          <Link
            to="/login"
            className="inline-flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm"
          >
            <ArrowLeft size={16} />
            Quay lại đăng nhập
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#74ebd5] to-[#ACB6E5] flex items-center justify-center p-4">
      <div className="bg-white w-full max-w-md rounded-[2rem] shadow-2xl p-8 md:p-12 animate-in fade-in zoom-in duration-300">
        <div className="text-center mb-10">
          <h1 className="text-4xl font-bold text-[#e74c3c] tracking-tight">Quên Mật Khẩu</h1>
          <p className="text-gray-500 mt-2 text-sm">
            Nhập email đã đăng ký, chúng tôi sẽ gửi link đặt lại mật khẩu cho bạn.
          </p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm flex items-center gap-3 rounded-r-xl animate-bounce">
            <AlertCircle size={20} />
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <label className="text-sm font-semibold text-gray-600 ml-1">Email</label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="email"
                value={email}
                className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-[#e74c3c] focus:bg-white focus:ring-4 focus:ring-red-100 outline-none transition-all"
                placeholder="abc@xyz.com"
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-[#e74c3c] hover:bg-[#c0392b] text-white py-4 rounded-xl font-bold text-lg shadow-lg shadow-red-200 hover:-translate-y-1 active:scale-95 transition-all flex items-center justify-center gap-3"
          >
            {isSubmitting ? <Loader2 className="animate-spin" /> : 'GỬI YÊU CẦU'}
          </button>
        </form>

        <div className="text-center mt-8">
          <Link
            to="/login"
            className="inline-flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm"
          >
            <ArrowLeft size={16} />
            Quay lại đăng nhập
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
