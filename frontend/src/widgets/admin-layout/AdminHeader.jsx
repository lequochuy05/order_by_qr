import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '@features/auth';
import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { Bell, Menu, User, LogOut, Wifi, WifiOff, Maximize2, Minimize2 } from 'lucide-react';
import { fmtRole } from '@shared/lib/formatters.js';
import wsService from '@shared/lib/websocket.js';

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
  const [wsConnected, setWsConnected] = useState(() => wsService.isConnected());

  useEffect(() => {
    return wsService.addConnectListener((connected) => {
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
      className={`sticky top-0 z-30 flex min-w-0 items-center justify-between border-b border-gray-100 bg-white shadow-sm transition-colors dark:border-slate-800 dark:bg-slate-950 ${
        isCompactViewport ? 'h-16 gap-2 px-3' : 'h-20 gap-4 px-6'
      }`}
    >
      <div className={`flex min-w-0 items-center ${isCompactViewport ? 'gap-2' : 'gap-4'}`}>
        <button
          onClick={toggleSidebar}
          className="flex shrink-0 p-2 hover:bg-gray-100 rounded-lg text-gray-600 transition-colors dark:text-slate-300 dark:hover:bg-slate-800"
        >
          <Menu size={24} />
        </button>

        <h1
          className={`truncate font-bold tracking-tight text-gray-800 dark:text-slate-100 ${
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
            className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-[10px] font-black uppercase tracking-wider transition-all ${
              wsConnected
                ? 'bg-green-50 text-green-600 dark:bg-green-500/10 dark:text-green-400'
                : 'bg-red-50 text-red-500 dark:bg-red-500/10 dark:text-red-400'
            }`}
            title={wsConnected ? 'WebSocket connected' : 'WebSocket disconnected'}
          >
            {wsConnected ? <Wifi size={12} /> : <WifiOff size={12} />}
            {wsConnected ? 'LIVE' : 'OFFLINE'}
          </div>
        )}

        {!isCompactViewport && (
          <button className="relative p-2 text-gray-500 transition-colors hover:bg-gray-50 dark:text-slate-400 dark:hover:bg-slate-800">
            <Bell size={20} />
            <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full border-2 border-white dark:border-slate-950"></span>
          </button>
        )}

        <div
          className={`flex items-center border-l border-gray-100 dark:border-slate-800 ${
            isCompactViewport ? 'gap-1 pl-2' : 'gap-3 pl-4'
          }`}
        >
          {!isCompactViewport && (
            <div className="max-w-32 text-right">
              <p className="truncate text-sm font-bold leading-tight text-gray-800 dark:text-slate-100">
                {user?.fullName || 'NaN'}
              </p>
              <p className="text-[10px] font-bold text-orange-500 uppercase tracking-wider">
                {preferences.language === 'en' ? formatEnglishRole(user?.role) : fmtRole(user?.role)}
              </p>
            </div>
          )}

          {/* Avatar xử lý từ UserDto */}
          <div
            className={`flex shrink-0 items-center justify-center overflow-hidden rounded-xl border border-orange-100 bg-orange-50 dark:border-orange-500/30 dark:bg-slate-900 ${
              isCompactViewport ? 'h-9 w-9' : 'h-10 w-10'
            }`}
          >
            {user?.avatarUrl ? (
              <img src={user?.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
            ) : (
              <User className="text-orange-500" size={20} />
            )}
          </div>

          <button
            onClick={logout}
            className="p-2 text-gray-400 hover:text-red-500 transition-colors dark:text-slate-500 dark:hover:text-red-400"
            title={preferences.language === 'en' ? 'Sign out' : 'Đăng xuất'}
          >
            <LogOut size={20} />
          </button>
          {!isCompactViewport && (
            <button
              type="button"
              onClick={toggleFullscreen}
              className="p-2 text-gray-400 transition-colors hover:text-blue-500 dark:text-slate-500 dark:hover:text-blue-400"
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
              {isFullscreen ? <Minimize2 size={20} /> : <Maximize2 size={20} />}
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
    MANAGER: 'Manager',
    STAFF: 'Staff',
    CHEF: 'Chef',
  };
  return roles[role] || role;
};

export default AdminHeader;
