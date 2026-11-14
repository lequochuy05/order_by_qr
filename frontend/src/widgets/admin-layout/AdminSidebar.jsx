import React, { useState, useEffect, useMemo } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '@features/auth';
import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { useSettingsQuery } from '@shared/api/settingsQueries.js';
import {
  LayoutDashboard,
  Layers,
  UtensilsCrossed,
  ClipboardList,
  Table,
  Users,
  BarChart3,
  Boxes,
  Package,
  TicketIcon,
  ChevronDown,
  Settings,
  ChevronRight,
  Circle,
  ShoppingBag,
  KeyRound,
  ShieldAlert,
} from 'lucide-react';

const menuItems = [
  {
    title: 'Bảng điều khiển',
    path: '/admin/dashboard',
    icon: <LayoutDashboard size={20} />,
    roles: ['ADMIN', 'MANAGER'],
  },
  {
    title: 'Bán mang về',
    path: '/admin/takeaway',
    icon: <ShoppingBag size={20} />,
    roles: ['ADMIN', 'MANAGER', 'CASHIER', 'STAFF'],
  },
  {
    title: 'Quản lý bàn',
    path: '/admin/tables',
    icon: <Table size={20} />,
    roles: ['ADMIN', 'MANAGER', 'CASHIER', 'STAFF'],
  },
  {
    title: 'Nhà bếp',
    path: '/admin/kitchen',
    icon: <UtensilsCrossed size={20} />,
    roles: ['ADMIN', 'MANAGER', 'CHEF'],
  },
  {
    title: 'Quản lý danh mục',
    path: '/admin/categories',
    icon: <Layers size={20} />,
    roles: ['ADMIN', 'MANAGER'],
  },
  {
    title: 'Quản lý món ăn',
    path: '/admin/menu',
    icon: <UtensilsCrossed size={20} />,
    roles: ['ADMIN', 'MANAGER'],
  },
  {
    title: 'Quản lý combo',
    path: '/admin/combo',
    icon: <Package size={20} />,
    roles: ['ADMIN', 'MANAGER'],
  },
  {
    title: 'Quản lý kho',
    path: '/admin/inventory',
    icon: <Boxes size={20} />,
    roles: ['ADMIN', 'MANAGER', 'CHEF'],
  },
  {
    title: 'Quản lý voucher',
    path: '/admin/voucher',
    icon: <TicketIcon size={20} />,
    roles: ['ADMIN', 'MANAGER'],
  },
  {
    title: 'Quản lý nhân viên',
    path: '/admin/staffs',
    icon: <Users size={20} />,
    roles: ['ADMIN', 'MANAGER'],
  },
  {
    title: 'Lịch sử đơn hàng',
    path: '/admin/history',
    icon: <ClipboardList size={20} />,
    roles: ['ADMIN', 'MANAGER', 'CASHIER'],
  },
  {
    title: 'Thống kê',
    icon: <BarChart3 size={20} />,
    roles: ['ADMIN', 'MANAGER'],
    children: [
      { title: 'Doanh thu', path: '/admin/statistics/revenue', roles: ['ADMIN'] },
      { title: 'Món ăn bán chạy', path: '/admin/statistics/top-dishes' },
      { title: 'Nhân viên', path: '/admin/statistics/staff' },
    ],
  },
  {
    title: 'Cài đặt',
    icon: <Settings size={20} />,
    roles: ['ADMIN', 'MANAGER', 'CASHIER', 'STAFF', 'CHEF'],
    children: [
      { title: 'Thông tin cá nhân', path: '/admin/profile' },
      { title: 'Cài đặt nhà hàng', path: '/admin/settings', roles: ['ADMIN'] },
      { title: 'Tùy chọn cá nhân', path: '/admin/settings', roles: ['MANAGER', 'CASHIER', 'STAFF', 'CHEF'] },
    ],
  },
];

const AdminSidebar = ({ isOpen, isCompactViewport = false }) => {
  const location = useLocation();
  const { user } = useAuth();
  const [preferences] = useAdminPreferences();
  const { data: settings } = useSettingsQuery({ enabled: !!user, stateTime: 5 * 60 * 1000 });
  const restaurantName = settings?.restaurantName?.trim() || 'QROS';
  const initial = restaurantName.charAt(0).toUpperCase();
  const label = (text) => getMenuLabel(text, preferences.language);

  // State để quản lý menu nào đang mở (lưu theo title)
  const [expandedMenu, setExpandedMenu] = useState(null);

  // Hàm toggle menu con
  const toggleSubMenu = (title) => {
    if (expandedMenu === title) {
      setExpandedMenu(null);
    } else {
      setExpandedMenu(title);
    }
  };

  const effectiveMenuItems = useMemo(
    () =>
      user?.mustChangePassword
        ? [
            {
              title: 'Đổi mật khẩu',
              path: '/admin/profile',
              icon: <KeyRound size={20} />,
              roles: ['ADMIN', 'MANAGER', 'CASHIER', 'STAFF', 'CHEF'],
            },
          ]
        : menuItems,
    [user?.mustChangePassword],
  );

  // Tự động mở menu cha nếu đang ở trang con
  useEffect(() => {
    effectiveMenuItems.forEach((item) => {
      if (item.children) {
        const isChildActive = item.children.some((child) => child.path === location.pathname);
        if (isChildActive) {
          setExpandedMenu(item.title);
        }
      }
    });
  }, [location.pathname, effectiveMenuItems]);

  return (
    <aside
      className={`fixed inset-y-0 left-0 z-40 flex flex-col bg-[#0F172A] text-slate-300 shadow-2xl transition-all duration-300 ease-in-out dark:bg-[#090D16] border-r border-slate-800/80 ${
        isCompactViewport
          ? isOpen
            ? 'w-64 translate-x-0'
            : 'w-64 -translate-x-full overflow-hidden'
          : isOpen
            ? 'w-64 translate-x-0'
            : 'w-20 translate-x-0 overflow-visible'
      }`}
    >
      {/* Header Sidebar: Brand logo & name */}
      <div className="h-20 flex items-center px-5 border-b border-slate-800/80 flex-shrink-0">
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="w-10 h-10 bg-gradient-to-tr from-indigo-600 to-violet-500 rounded-2xl flex-shrink-0 flex items-center justify-center text-white font-black shadow-lg shadow-indigo-500/25">
            {initial}
          </div>
          {isOpen && (
            <div className="min-w-0">
              <span className="text-base font-black text-white tracking-tight truncate block">
                {restaurantName}
              </span>
              <span className="text-[10px] font-bold text-indigo-400 tracking-wider uppercase block">
                POS Management
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Menu Area */}
      <nav className="flex-1 p-3.5 space-y-1.5 mt-2 overflow-y-auto scrollbar-thin scrollbar-thumb-slate-800">
        {user?.mustChangePassword && isOpen && (
          <div className="mb-4 rounded-2xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs text-amber-300">
            <div className="flex items-center gap-1.5 font-bold mb-1">
              <ShieldAlert size={14} className="text-amber-400" />
              <span>Yêu cầu bảo mật</span>
            </div>
            <p className="text-slate-400 text-[11px] leading-relaxed">
              Bạn cần đổi mật khẩu khởi tạo để kích hoạt đầy đủ tính năng.
            </p>
          </div>
        )}

        {effectiveMenuItems.map((item, index) => {
          if (user?.role && !item.roles.includes(user.role)) return null;

          const hasChildren = !!item.children;
          const isParentExpanded = expandedMenu === item.title;
          const isActive = !hasChildren
            ? location.pathname === item.path
            : item.children.some((child) => child.path === location.pathname);

          return (
            <div key={index}>
              {/* === RENDER PARENT OR SINGLE ITEM === */}
              {hasChildren ? (
                <div
                  onClick={() => isOpen && toggleSubMenu(item.title)}
                  className={`flex items-center gap-3.5 px-3 py-2.5 rounded-2xl transition-all group relative cursor-pointer select-none
                    ${isActive && !isParentExpanded ? 'bg-indigo-950/40 text-indigo-400 font-bold' : 'hover:bg-slate-800/60 hover:text-white text-slate-400'}
                    ${isParentExpanded ? 'bg-slate-800/80 text-white font-bold' : ''}
                  `}
                >
                  <div
                    className={`${
                      isActive
                        ? 'text-indigo-400'
                        : 'text-slate-400 group-hover:text-white group-hover:scale-105 transition-all'
                    }`}
                  >
                    {item.icon}
                  </div>

                  {isOpen && (
                    <>
                      <span className="whitespace-nowrap flex-1 text-xs font-semibold">
                        {label(item.title)}
                      </span>
                      <span className="text-slate-500 group-hover:text-slate-300 transition-colors">
                        {isParentExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                      </span>
                    </>
                  )}

                  {/* Tooltip on collapsed sidebar */}
                  {!isOpen && (
                    <div className="absolute left-16 bg-slate-900 text-white text-xs font-medium px-3 py-1.5 rounded-xl opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity whitespace-nowrap z-50 shadow-2xl border border-slate-700/80">
                      {label(item.title)}
                    </div>
                  )}
                </div>
              ) : (
                <Link
                  to={item.path}
                  className={`flex items-center gap-3.5 px-3 py-2.5 rounded-2xl transition-all group relative
                    ${
                      isActive
                        ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25 font-bold'
                        : 'hover:bg-slate-800/60 hover:text-white text-slate-400 font-medium'
                    }`}
                >
                  <div
                    className={`${
                      isActive
                        ? 'text-white'
                        : 'text-slate-400 group-hover:text-white group-hover:scale-105 transition-all'
                    }`}
                  >
                    {item.icon}
                  </div>

                  {isOpen && (
                    <span className="whitespace-nowrap animate-in fade-in slide-in-from-left-2 text-xs font-semibold">
                      {label(item.title)}
                    </span>
                  )}

                  {!isOpen && (
                    <div className="absolute left-16 bg-slate-900 text-white text-xs font-medium px-3 py-1.5 rounded-xl opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity whitespace-nowrap z-50 shadow-2xl border border-slate-700/80">
                      {label(item.title)}
                    </div>
                  )}
                </Link>
              )}

              {/* === RENDER SUB-MENU ITEMS === */}
              {hasChildren && isOpen && isParentExpanded && (
                <div className="mt-1 space-y-1 pl-10 overflow-hidden animate-in slide-in-from-top-2 duration-200">
                  {item.children.map((child, childIndex) => {
                    if (child.roles && user?.role && !child.roles.includes(user.role)) return null;
                    const isChildActive = location.pathname === child.path;
                    return (
                      <Link
                        key={childIndex}
                        to={child.path}
                        className={`flex items-center gap-2.5 px-3 py-2 rounded-xl text-xs transition-colors
                          ${
                            isChildActive
                              ? 'text-indigo-400 bg-indigo-950/40 font-bold'
                              : 'text-slate-400 hover:text-white hover:bg-slate-800/40 font-medium'
                          }`}
                      >
                        <Circle
                          size={6}
                          className={isChildActive ? 'fill-indigo-400 text-indigo-400' : 'fill-transparent text-slate-600'}
                        />
                        <span className="whitespace-nowrap">{label(child.title)}</span>
                      </Link>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </nav>
    </aside>
  );
};

const englishMenuLabels = {
  'Bảng điều khiển': 'Dashboard',
  'Quản lý bàn': 'Tables',
  'Nhà bếp': 'Kitchen',
  'Quản lý danh mục': 'Categories',
  'Quản lý món ăn': 'Menu items',
  'Quản lý combo': 'Combos',
  'Quản lý voucher': 'Vouchers',
  'Quản lý nhân viên': 'Staff',
  'Lịch sử đơn hàng': 'Order history',
  'Thống kê': 'Statistics',
  'Doanh thu': 'Revenue',
  'Món ăn bán chạy': 'Top dishes',
  'Nhân viên': 'Staff',
  'Bán mang về': 'Takeaway',
  'Cài đặt': 'Settings',
  'Thông tin cá nhân': 'Profile',
  'Cài đặt nhà hàng': 'Restaurant settings',
  'Tùy chọn cá nhân': 'Preferences',
};

const getMenuLabel = (text, language) => {
  if (language !== 'en') return text;
  return englishMenuLabels[text] || text;
};

export default AdminSidebar;
