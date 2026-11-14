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
      className={`relative sticky top-0 z-30 flex min-w-0 shrink-0 items-center justify-between border-b border-[#1E293B]/20 bg-white/90 backdrop-blur-md shadow-sm transition-colors dark:border-[#1E293B] dark:bg-[#0F172A]/90 ${
        isCompactViewport ? 'h-16 gap-2 px-3' : 'h-20 gap-4 px-6'
      }`}
    >
      {/* Decorative diagonal rain animation */}
      <div
        className="pointer-events-none absolute inset-0 overflow-hidden"
        aria-hidden="true"
      >
        {RAIN_STREAKS.map((streak) => (
          <span
            key={streak.id}
            className="admin-rain-drop"
            style={{
              left: streak.left,
              width: `${streak.width}px`,
              height: `${streak.height}px`,
              animationDuration: streak.duration,
              animationDelay: streak.delay,
              '--rain-opacity': streak.opacity,
            }}
          />
        ))}
      </div>

      {/* Left: Sidebar Toggle & Page Title */}
      <div className={`relative z-10 flex min-w-0 items-center ${isCompactViewport ? 'gap-2' : 'gap-4'}`}>
        <button
          onClick={toggleSidebar}
          className="flex shrink-0 p-2.5 hover:bg-slate-100 rounded-xl text-slate-600 transition-colors dark:text-slate-300 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white"
          aria-label="Chuyển đổi menu thanh bên"
        >
          <Menu size={22} />
        </button>

        <h1
          className={`truncate font-extrabold tracking-tight text-slate-900 dark:text-white ${
            isCompactViewport ? 'text-base' : 'text-xl'
          }`}
        >
          {getPageTitle(location.pathname)}
        </h1>
      </div>

      {/* Right: Actions, WebSocket, Notifications & Profile */}
      <div
        className={`relative z-10 flex shrink-0 items-center ${isCompactViewport ? 'gap-1.5' : 'gap-3 lg:gap-5'}`}
      >
        {/* WebSocket Status Badge */}
        {!isCompactViewport && (
          <div
            className={`flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-bold transition-all border ${
              wsConnected
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200/70 dark:bg-emerald-950/40 dark:text-emerald-400 dark:border-emerald-800/50'
                : 'bg-rose-50 text-rose-700 border-rose-200/70 dark:bg-rose-950/40 dark:text-rose-400 dark:border-rose-800/50'
            }`}
            title={wsConnected ? 'WebSocket connected' : 'WebSocket disconnected'}
          >
            {wsConnected ? (
              <>
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                <span>Trực tuyến</span>
              </>
            ) : (
              <>
                <span className="w-2 h-2 rounded-full bg-rose-500" />
                <span>Ngoại tuyến</span>
              </>
            )}
          </div>
        )}

        {/* Notifications Icon Button */}
        {!isCompactViewport && (
          <button
            className="relative p-2.5 text-slate-500 rounded-xl transition-colors hover:bg-slate-100 hover:text-slate-800 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200"
            title="Thông báo hệ thống"
          >
            <Bell size={19} />
            <span className="absolute top-2 right-2 w-2 h-2 bg-indigo-600 rounded-full ring-2 ring-white dark:ring-slate-900" />
          </button>
        )}

        {/* User Card & Profile */}
        <div
          className={`flex items-center border-l border-slate-200/80 dark:border-slate-800 ${
            isCompactViewport ? 'gap-1.5 pl-2' : 'gap-3 pl-4'
          }`}
        >
          {!isCompactViewport && (
            <div className="max-w-36 text-right">
              <p className="truncate text-sm font-extrabold leading-tight text-slate-900 dark:text-white">
                {user?.fullName || 'Người dùng'}
              </p>
              <span className="inline-block text-[10px] font-black uppercase tracking-wider text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/60 px-2 py-0.5 rounded-md mt-0.5">
                {preferences.language === 'en'
                  ? formatEnglishRole(user?.role)
                  : fmtRole(user?.role)}
              </span>
            </div>
          )}

          {/* User Avatar */}
          <div
            className={`flex shrink-0 items-center justify-center overflow-hidden rounded-xl border border-slate-200/80 bg-slate-100 dark:border-slate-700/80 dark:bg-slate-800 shadow-sm ${
              isCompactViewport ? 'h-9 w-9' : 'h-10 w-10'
            }`}
          >
            {user?.avatarUrl ? (
              <img src={user?.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
            ) : (
              <User className="text-indigo-600 dark:text-indigo-400" size={20} />
            )}
          </div>

          {/* Fullscreen Button */}
          {!isCompactViewport && (
            <button
              type="button"
              onClick={toggleFullscreen}
              className="p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 rounded-xl dark:text-slate-500 dark:hover:bg-slate-800 dark:hover:text-slate-300"
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
              {isFullscreen ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
            </button>
          )}

          {/* Logout Button */}
          <button
            onClick={logout}
            className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-colors dark:text-slate-500 dark:hover:text-rose-400 dark:hover:bg-rose-950/30"
            title={preferences.language === 'en' ? 'Sign out' : 'Đăng xuất'}
          >
            <LogOut size={19} />
          </button>
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

const RAIN_STREAKS = [
  { id: 1, left: '-2%', width: 1.5, height: 28, duration: '1.2s', delay: '-0.3s', opacity: 0.5 },
  { id: 2, left: '3%', width: 1, height: 22, duration: '1.5s', delay: '-1.1s', opacity: 0.35 },
  { id: 3, left: '7%', width: 2, height: 38, duration: '0.95s', delay: '-0.6s', opacity: 0.65 },
  { id: 4, left: '12%', width: 1.5, height: 30, duration: '1.3s', delay: '-1.4s', opacity: 0.45 },
  { id: 5, left: '16%', width: 1, height: 24, duration: '1.6s', delay: '-0.2s', opacity: 0.4 },
  { id: 6, left: '21%', width: 2, height: 42, duration: '1.05s', delay: '-0.9s', opacity: 0.7 },
  { id: 7, left: '26%', width: 1.5, height: 32, duration: '1.25s', delay: '-1.7s', opacity: 0.5 },
  { id: 8, left: '30%', width: 1, height: 26, duration: '1.45s', delay: '-0.5s', opacity: 0.35 },
  { id: 9, left: '35%', width: 2, height: 40, duration: '0.9s', delay: '-1.2s', opacity: 0.65 },
  { id: 10, left: '40%', width: 1.5, height: 28, duration: '1.35s', delay: '-0.8s', opacity: 0.5 },
  { id: 11, left: '45%', width: 1, height: 22, duration: '1.55s', delay: '-0.1s', opacity: 0.4 },
  { id: 12, left: '50%', width: 2, height: 36, duration: '1.0s', delay: '-1.5s', opacity: 0.7 },
  { id: 13, left: '54%', width: 1.5, height: 30, duration: '1.4s', delay: '-0.4s', opacity: 0.45 },
  { id: 14, left: '59%', width: 1, height: 25, duration: '1.65s', delay: '-1.0s', opacity: 0.35 },
  { id: 15, left: '64%', width: 2, height: 44, duration: '0.95s', delay: '-0.7s', opacity: 0.65 },
  { id: 16, left: '68%', width: 1.5, height: 28, duration: '1.15s', delay: '-1.3s', opacity: 0.5 },
  { id: 17, left: '73%', width: 1, height: 24, duration: '1.5s', delay: '-0.3s', opacity: 0.4 },
  { id: 18, left: '78%', width: 2, height: 38, duration: '1.05s', delay: '-1.6s', opacity: 0.65 },
  { id: 19, left: '82%', width: 1.5, height: 32, duration: '1.25s', delay: '-0.6s', opacity: 0.5 },
  { id: 20, left: '87%', width: 1, height: 26, duration: '1.45s', delay: '-1.2s', opacity: 0.35 },
  { id: 21, left: '92%', width: 2, height: 40, duration: '1.0s', delay: '-0.5s', opacity: 0.7 },
  { id: 22, left: '96%', width: 1.5, height: 30, duration: '1.3s', delay: '-1.5s', opacity: 0.45 },
  { id: 23, left: '100%', width: 1, height: 24, duration: '1.6s', delay: '-0.8s', opacity: 0.35 },
  { id: 24, left: '103%', width: 2, height: 36, duration: '1.1s', delay: '-0.2s', opacity: 0.6 },
];

export default AdminHeader;
