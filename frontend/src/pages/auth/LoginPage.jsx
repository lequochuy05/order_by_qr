import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { authService } from '../../services/authService';
import { Lock, Mail, Loader2, AlertCircle} from 'lucide-react';
import { FaGoogle, FaFacebook, FaTwitter } from 'react-icons/fa';

const LoginPage = () => {
  const [credentials, setCredentials] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    // Kiểm tra nhập liệu 
    if (!credentials.email || !credentials.password) {
      setError('Vui lòng nhập đầy đủ Email và Mật khẩu');
      setIsSubmitting(false);
      return;
    }

    try {
      // Gọi API login từ UserController
      const data = await authService.login(credentials.email, credentials.password);
      
      // Lưu vào Context và LocalStorage (userId, fullName, role, accessToken)
      login(data); 
      
      // Điều hướng
      navigate('/admin/dashboard'); 
    } catch (err) {
      // Xử lý lỗi 401/403 
      const msg = err.response?.status === 401 ? 'Sai email hoặc mật khẩu' : 'Đăng nhập thất bại';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    // Background
    <div className="min-h-screen bg-gradient-to-br from-[#74ebd5] to-[#ACB6E5] flex items-center justify-center p-4">
      
      {/* Container chính */}
      <div className="bg-white w-full max-w-md rounded-[2rem] shadow-2xl p-8 md:p-12 animate-in fade-in zoom-in duration-300">
        
        {/* Tiêu đề */}
        <div className="text-center mb-10">
          <h1 className="text-4xl font-bold text-[#e74c3c] tracking-tight">Đăng Nhập</h1>
        </div>

        {/* Hiển thị lỗi */}
        {error && (
          <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 text-sm flex items-center gap-3 rounded-r-xl animate-bounce">
            <AlertCircle size={20} />
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
                required
                className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-[#e74c3c] focus:bg-white focus:ring-4 focus:ring-red-100 outline-none transition-all"
                placeholder="abc@xyz.com"
                onChange={(e) => setCredentials({...credentials, email: e.target.value})}
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
                required
                className="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-xl focus:border-[#e74c3c] focus:bg-white focus:ring-4 focus:ring-red-100 outline-none transition-all"
                placeholder="••••••••"
                onChange={(e) => setCredentials({...credentials, password: e.target.value})}
              />
            </div>
                <div className="flex justify-end">
                    <a href="#" className="text-xs text-purple-600 hover:underline">
                    Quên mật khẩu?
                    </a>
                </div>
          </div>

          {/* Nút Đăng nhập */}
          <button 
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-[#3498db] hover:bg-[#2980b9] text-white py-4 rounded-xl font-bold text-lg shadow-lg shadow-blue-200 hover:-translate-y-1 active:scale-95 transition-all flex items-center justify-center gap-3 mt-4"
          >
            {isSubmitting ? <Loader2 className="animate-spin" /> : "ĐĂNG NHẬP NGAY"}
          </button>
        </form>

        {/* Social Login */}
        <div className="mt-8">
          <div className="relative flex py-3 items-center">
            <div className="flex-grow border-t border-gray-200"></div>
            <span className="flex-shrink mx-4 text-gray-400 text-sm italic">Hoặc đăng nhập bằng</span>
            <div className="flex-grow border-t border-gray-200"></div>
          </div>
          
          <div className="flex justify-center gap-6 mt-4">
                <button className="p-3 bg-gray-50 rounded-xl hover:scale-110 transition-transform text-[#DB4437]">
                    <FaGoogle size={24} /> {/* Gọi trực tiếp như một Component */}
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
          Chưa có tài khoản? <a href="#" className="text-purple-600 font-bold hover:underline">Đăng ký ngay</a>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;