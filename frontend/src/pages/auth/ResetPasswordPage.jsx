import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { authService } from '@features/auth';
import { Lock, Loader2, AlertCircle, CheckCircle, Eye, EyeOff } from 'lucide-react';

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

  if (!token) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#74ebd5] to-[#ACB6E5] flex items-center justify-center p-4">
        <div className="bg-white w-full max-w-md rounded-[2rem] shadow-2xl p-8 md:p-12 text-center">
          <AlertCircle size={48} className="mx-auto mb-4 text-red-500" />
          <h1 className="text-2xl font-bold text-gray-800 mb-4">Link không hợp lệ</h1>
          <p className="text-gray-600 mb-6">Link đặt lại mật khẩu không đúng hoặc đã hết hạn.</p>
          <Link to="/forgot-password" className="text-emerald-600 hover:underline font-medium">
            Yêu cầu link mới
          </Link>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-[#74ebd5] to-[#ACB6E5] flex items-center justify-center p-4">
        <div className="bg-white w-full max-w-md rounded-[2rem] shadow-2xl p-8 md:p-12 text-center">
          <div className="flex justify-center mb-6">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
              <CheckCircle size={36} className="text-green-500" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-gray-800 mb-4">Đặt lại mật khẩu thành công!</h1>
          <p className="text-gray-600 mb-6">Bạn sẽ được chuyển hướng đến trang đăng nhập...</p>
          <Link to="/login" className="text-emerald-600 hover:underline font-medium">
            Đăng nhập ngay
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#74ebd5] to-[#ACB6E5] flex items-center justify-center p-4">
      <div className="bg-white w-full max-w-md rounded-[2rem] shadow-2xl p-8 md:p-12 animate-in fade-in zoom-in duration-300">
        <div className="text-center mb-10">
          <h1 className="text-4xl font-bold text-[#e74c3c] tracking-tight">Đặt Lại Mật Khẩu</h1>
          <p className="text-gray-500 mt-2 text-sm">Nhập mật khẩu mới cho tài khoản của bạn.</p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm flex items-center gap-3 rounded-r-xl animate-bounce">
            <AlertCircle size={20} />
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <label className="text-sm font-semibold text-gray-600 ml-1">Mật khẩu mới</label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                className="w-full pl-12 pr-12 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-[#e74c3c] focus:bg-white focus:ring-4 focus:ring-red-100 outline-none transition-all"
                placeholder="••••••••"
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-gray-600 ml-1">Xác nhận mật khẩu</label>
            <div className="relative">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="password"
                value={confirmPassword}
                className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-[#e74c3c] focus:bg-white focus:ring-4 focus:ring-red-100 outline-none transition-all"
                placeholder="••••••••"
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-[#e74c3c] hover:bg-[#c0392b] text-white py-4 rounded-xl font-bold text-lg shadow-lg shadow-red-200 hover:-translate-y-1 active:scale-95 transition-all flex items-center justify-center gap-3"
          >
            {isSubmitting ? <Loader2 className="animate-spin" /> : 'ĐẶT LẠI MẬT KHẨU'}
          </button>
        </form>

        <div className="text-center mt-8">
          <Link
            to="/login"
            className="inline-flex items-center gap-2 text-gray-500 hover:text-gray-700 text-sm"
          >
            Quay lại đăng nhập
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ResetPasswordPage;
