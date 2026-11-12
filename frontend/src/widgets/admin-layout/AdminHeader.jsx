import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '@features/auth';
import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { Bell, Menu, User, LogOut, Wifi, WifiOff, Maximize2, Minimize2 } from 'lucide-react';
import { fmtRole } from '@shared/lib/formatters.js';
import { adminWsService } from '@shared/lib/websocket.js';

const AdminHeader = ({ toggleSidebar, isCompactViewport = false }) => {
  const { user, logout } = useAuth();
  const [preferences] = useAdminPreferences();
  const location = useLocation();
  const [isFullscreen, setIsFullscreen] = useState(Boolean(document.fullscreenElement));

  useEffect(() => {
    const handleFullscreenChange = () => setIsFullscreen(Boolean(document.fullscreenElement));
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  // WebSocket status badge
  const [wsConnected, setWsConnected] = useState(() => adminWsService.isConnected());

  useEffect(() => {
    return adminWsService.addConnectListener((connected) => {
      setWsConnected(connected);
    });
  }, []);

  const getPageTitle = (path) => {
    const titles = preferences.language === 'en' ? englishPageTitles : vietnamesePageTitles;
    return titles[path] || titles['/admin/dashboard'];
  };

  const toggleFullscreen = async () => {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
    } else {
      await document.documentElement.requestFullscreen();
    }
  };

  return (
    <header
      className={`sticky top-0 z-30 flex min-w-0 shrink-0 items-center justify-between border-b border-[#E8DCD1] bg-white shadow-sm transition-colors dark:border-[#352C26] dark:bg-[#1F1A17] ${
        isCompactViewport ? 'h-16 gap-2 px-3' : 'h-20 gap-4 px-6'
      }`}
    >
      <div className={`flex min-w-0 items-center ${isCompactViewport ? 'gap-2' : 'gap-4'}`}>
        <button
          onClick={toggleSidebar}
          className="flex shrink-0 p-2 hover:bg-[#F7F0E7] rounded-xl text-[#56372B] transition-colors dark:text-[#D8CCC4] dark:hover:bg-[#2A221E]"
          aria-label="Chuyển đổi menu thanh bên"
        >
          <Menu size={22} />
        </button>

        <h1
          className={`truncate font-extrabold tracking-tight text-[#2D1D16] dark:text-[#F7F0E7] ${
            isCompactViewport ? 'text-base' : 'text-xl'
          }`}
        >
          {getPageTitle(location.pathname)}
        </h1>
      </div>

      {/* User Actions */}
      <div
        className={`flex shrink-0 items-center ${isCompactViewport ? 'gap-1' : 'gap-3 lg:gap-5'}`}
      >
        {/* WebSocket Status Badge */}
        {!isCompactViewport && (
          <div
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-[10px] font-black tracking-wider transition-all ${
              wsConnected
                ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-400'
                : 'bg-rose-50 text-rose-700 dark:bg-rose-500/15 dark:text-rose-400'
            }`}
            title={wsConnected ? 'WebSocket connected' : 'WebSocket disconnected'}
          >
            {wsConnected ? <Wifi size={12} /> : <WifiOff size={12} />}
            {wsConnected ? 'Trực tuyến' : 'Ngoại tuyến'}
          </div>
        )}

        {!isCompactViewport && (
          <button
            className="relative p-2.5 text-[#6B5D55] rounded-xl transition-colors hover:bg-[#F7F0E7] dark:text-[#BDB0A4] dark:hover:bg-[#2A221E]"
            title="Thông báo hệ thống"
          >
            <Bell size={19} />
            <span className="absolute top-2 right-2 w-2 h-2 bg-[#C17C4A] rounded-full border-2 border-white dark:border-[#1F1A17]"></span>
          </button>
        )}

        <div
          className={`flex items-center border-l border-[#E8DCD1] dark:border-[#352C26] ${
            isCompactViewport ? 'gap-1 pl-2' : 'gap-3 pl-4'
          }`}
        >
          {!isCompactViewport && (
            <div className="max-w-36 text-right">
              <p className="truncate text-sm font-bold leading-tight text-[#2D1D16] dark:text-[#F7F0E7]">
                {user?.fullName || 'Người dùng'}
              </p>
              <p className="text-[10px] font-extrabold text-[#C17C4A] uppercase tracking-wider">
                {preferences.language === 'en'
                  ? formatEnglishRole(user?.role)
                  : fmtRole(user?.role)}
              </p>
            </div>
          )}

          {/* Avatar */}
          <div
            className={`flex shrink-0 items-center justify-center overflow-hidden rounded-xl border border-[#E8DCD1] bg-[#F7F0E7] dark:border-[#352C26] dark:bg-[#2A221E] ${
              isCompactViewport ? 'h-9 w-9' : 'h-10 w-10'
            }`}
          >
            {user?.avatarUrl ? (
              <img src={user?.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
            ) : (
              <User className="text-[#C17C4A]" size={20} />
            )}
          </div>

          <button
            onClick={logout}
            className="p-2 text-[#9C8E85] hover:text-rose-600 transition-colors dark:text-[#82756B] dark:hover:text-rose-400"
            title={preferences.language === 'en' ? 'Sign out' : 'Đăng xuất'}
          >
            <LogOut size={19} />
          </button>
          {!isCompactViewport && (
            <button
              type="button"
              onClick={toggleFullscreen}
              className="p-2 text-[#9C8E85] transition-colors hover:text-[#56372B] dark:text-[#82756B] dark:hover:text-[#D8CCC4]"
              title={
                preferences.language === 'en'
                  ? isFullscreen
                    ? 'Minimize'
                    : 'Fullscreen'
                  : isFullscreen
                    ? 'Thu nhỏ'
                    : 'Toàn màn hình'
              }
            >
              {isFullscreen ? <Minimize2 size={19} /> : <Maximize2 size={19} />}
            </button>
          )}
        </div>
      </div>
    </header>
  );
};

const vietnamesePageTitles = {
  '/admin/dashboard': 'Bảng điều khiển',
  '/admin/categories': 'Quản lý danh mục',
  '/admin/menu': 'Quản lý món ăn',
  '/admin/orders': 'Quản lý đơn hàng',
  '/admin/tables': 'Quản lý bàn',
  '/admin/staffs': 'Quản lý nhân viên',
  '/admin/statistics': 'Thống kê',
  '/admin/combo': 'Quản lý combo',
  '/admin/inventory': 'Quản lý kho',
  '/admin/voucher': 'Quản lý voucher',
  '/admin/promotions': 'Quản lý khuyến mãi',
  '/admin/statistics/revenue': 'Doanh thu',
  '/admin/statistics/top-dishes': 'Món ăn bán chạy',
  '/admin/statistics/staff': 'Hiệu suất nhân viên',
  '/admin/history': 'Lịch sử đơn hàng',
  '/admin/kitchen': 'Nhà bếp',
  '/admin/profile': 'Cập nhật thông tin cá nhân',
  '/admin/settings': 'Cài đặt',
};

const englishPageTitles = {
  '/admin/dashboard': 'Dashboard',
  '/admin/categories': 'Categories',
  '/admin/menu': 'Menu items',
  '/admin/orders': 'Orders',
  '/admin/tables': 'Tables',
  '/admin/staffs': 'Staff',
  '/admin/statistics': 'Statistics',
  '/admin/combo': 'Combos',
  '/admin/inventory': 'Inventory',
  '/admin/voucher': 'Vouchers',
  '/admin/promotions': 'Promotions',
  '/admin/statistics/revenue': 'Revenue',
  '/admin/statistics/top-dishes': 'Top dishes',
  '/admin/statistics/staff': 'Staff performance',
  '/admin/history': 'Order history',
  '/admin/kitchen': 'Kitchen',
  '/admin/profile': 'Profile',
  '/admin/settings': 'Settings',
};

const formatEnglishRole = (role) => {
  const roles = {
    ADMIN: 'Admin',
    MANAGER: 'Manager',
    CASHIER: 'Cashier',
    STAFF: 'Staff',
    CHEF: 'Chef',
  };
  return roles[role] || role;
};

export default AdminHeader;
