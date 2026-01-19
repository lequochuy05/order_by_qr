import React, { use } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { Bell, Menu, User, LogOut } from 'lucide-react';

const AdminHeader = ({ toggleSidebar }) => {
  const { user, logout } = useAuth(); // Lấy data từ AuthResponse
  const location = useLocation();

  const getPageTitle = (path) => {
    switch (path) {
        case '/admin/dashboard':
            return 'Bảng điều khiển';
        case '/admin/categories':
            return 'Quản lý danh mục';
        case '/admin/menu':
            return 'Quản lý món ăn';
        case '/admin/orders':
            return 'Quản lý đơn hàng';
        case '/admin/tables':
            return 'Quản lý bàn';
        case '/admin/staffs':
            return 'Quản lý nhân viên';
        case '/admin/statistics':
            return 'Thống kê';
        case '/admin/settings':
            return 'Cài đặt';
        case '/admin/combo':
            return 'Quản lý combo';
        case '/admin/voucher':
            return 'Quản lý voucher';
        case '/admin/promotions':
            return 'Quản lý khuyến mãi';
        
        default:
            return 'Bảng điều khiển';
    }

  };

  return (
    <header className="h-20 bg-white border-b border-gray-100 px-6 flex items-center justify-between sticky top-0 z-30 shadow-sm">
        {/*  */}
        <div className="flex items-center gap-4">
            {/* NÚT MENU: Click vào đây để mở rộng/thu hẹp Sidebar */}
            <button 
            onClick={toggleSidebar}
            className="p-2 hover:bg-gray-100 rounded-lg text-gray-600 transition-colors"
            >
            <Menu size={24} />
            </button>
            
            <h1 className="text-xl font-bold text-gray-800 tracking-tight">
            {getPageTitle(location.pathname)}
            </h1>
        </div>
     
        {/* User Actions */}
        <div className="flex items-center gap-5">
            <button className="p-2 text-gray-500 hover:bg-gray-50 rounded-lg relative">
            <Bell size={20} />
            <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
            </button>

            <div className="flex items-center gap-3 pl-4 border-l border-gray-100">
            <div className="text-right">
                <p className="text-sm font-bold text-gray-800 leading-tight">
                {user?.fullName || 'Admin'}
                </p>
                <p className="text-[10px] font-bold text-orange-500 uppercase tracking-wider">
                {user?.role} {/* MANAGER hoặc STAFF */}
                </p>
            </div>
            
            {/* Avatar xử lý từ UserDto */}
            <div className="w-10 h-10 bg-orange-50 rounded-xl flex items-center justify-center border border-orange-100 overflow-hidden">
                {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                ) : (
                <User className="text-orange-500" size={20} />
                )}
            </div>

            <button 
                onClick={logout}
                className="p-2 text-gray-400 hover:text-red-500 transition-colors"
                title="Đăng xuất"
            >
                <LogOut size={20} />
            </button>
            </div>
        </div>
    </header>
  );
};

export default AdminHeader;