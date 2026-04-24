import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import {
  LayoutDashboard,
  Layers,
  UtensilsCrossed,
  ClipboardList,
  Table,
  Users,
  BarChart3,
  Package,
  TicketPercent,
  TicketIcon,
  ChevronDown, // Import thêm icon mũi tên
  ChevronRight,
  Circle // Dùng làm icon cho menu con
} from 'lucide-react';

const menuItems = [
  {
    title: 'Bảng điều khiển',
    path: '/admin/dashboard',
    icon: <LayoutDashboard size={22} />,
    roles: ['MANAGER', 'STAFF', 'CHEF']
  },
  {
    title: 'Quản lý bàn',
    path: '/admin/tables',
    icon: <Table size={22} />,
    roles: ['MANAGER', 'STAFF']
  },
  {
    title: 'Nhà bếp',
    path: '/admin/kitchen',
    icon: <UtensilsCrossed size={22} />,
    roles: ['MANAGER', 'CHEF']
  },
  {
    title: 'Quản lý danh mục',
    path: '/admin/categories',
    icon: <Layers size={22} />,
    roles: ['MANAGER']
  },
  {
    title: 'Quản lý món ăn',
    path: '/admin/menu',
    icon: <UtensilsCrossed size={22} />,
    roles: ['MANAGER']
  },
  {
    title: 'Quản lý combo',
    path: '/admin/combo',
    icon: <Package size={22} />,
    roles: ['MANAGER']
  },
  {
    title: 'Quản lý voucher',
    path: '/admin/voucher',
    icon: <TicketIcon size={22} />,
    roles: ['MANAGER']
  },
  // {
  //   title: 'Quản lý khuyến mãi',
  //   path: '/admin/promotions',
  //   icon: <TicketPercent size={22} />,
  //   roles: ['MANAGER']
  // },
  {
    title: 'Quản lý nhân viên',
    path: '/admin/staffs',
    icon: <Users size={22} />,
    roles: ['MANAGER']
  },
  {
    title: 'Lịch sử đơn hàng',
    path: '/admin/history',
    icon: <ClipboardList size={22} />,
    roles: ['MANAGER', 'STAFF']
  },

  {
    title: 'Thống kê',
    icon: <BarChart3 size={22} />,
    roles: ['MANAGER'],
    children: [
      { title: 'Doanh thu', path: '/admin/statistics/revenue' },
      { title: 'Món ăn bán chạy', path: '/admin/statistics/top-dishes' },
      { title: 'Nhân viên', path: '/admin/statistics/staff' }
    ]
  },
];

const AdminSidebar = ({ isOpen }) => {
  const location = useLocation();
  const { user } = useAuth();

  // State để quản lý menu nào đang mở (lưu theo title)
  const [expandedMenu, setExpandedMenu] = useState(null);

  // Hàm toggle menu con
  const toggleSubMenu = (title) => {
    if (expandedMenu === title) {
      setExpandedMenu(null); // Đóng nếu đang mở
    } else {
      setExpandedMenu(title); // Mở menu mới
    }
  };

  // Tự động mở menu cha nếu đang ở trang con (UX tốt hơn)
  useEffect(() => {
    menuItems.forEach(item => {
      if (item.children) {
        const isChildActive = item.children.some(child => child.path === location.pathname);
        if (isChildActive) {
          setExpandedMenu(item.title);
        }
      }
    });
  }, [location.pathname]);

  return (
    <aside
      className={`fixed inset-y-0 left-0 z-40 bg-slate-900 text-slate-300 transition-all duration-300 ease-in-out shadow-2xl flex flex-col
        ${isOpen
          ? 'translate-x-0 w-64'
          : '-translate-x-full lg:translate-x-0 w-0 lg:w-20 overflow-hidden lg:overflow-visible'}`}
    >
      {/* Header Sidebar */}
      <div className="h-20 flex items-center px-6 border-b border-slate-800 flex-shrink-0">
        <div className="flex items-center gap-3 overflow-hidden">
          <div className="w-8 h-8 bg-orange-500 rounded-lg flex-shrink-0 flex items-center justify-center text-white font-bold">
            S
          </div>
          {isOpen && (
            <span className="text-xl font-black text-white italic tracking-tighter whitespace-nowrap">
              SẮC MÀU <span className="text-orange-500">QUÁN</span>
            </span>
          )}
        </div>
      </div>

      {/* Menu Area */}
      <nav className="flex-1 p-4 space-y-2 mt-4 overflow-y-auto scrollbar-thin scrollbar-thumb-slate-700">
        {menuItems.map((item, index) => {
          if (user?.role && !item.roles.includes(user.role)) return null;

          // Logic kiểm tra Active
          const hasChildren = !!item.children;
          const isParentExpanded = expandedMenu === item.title;
          // Item được coi là active nếu đường dẫn trùng khớp HOẶC là cha của menu con đang active
          const isActive = !hasChildren
            ? location.pathname === item.path
            : item.children.some(child => child.path === location.pathname);

          return (
            <div key={index}>
              {/* === RENDER ITEM MENU === */}
              {hasChildren ? (
                // --- TRƯỜNG HỢP CÓ MENU CON (Dạng Button Toggle) ---
                <div
                  onClick={() => isOpen && toggleSubMenu(item.title)}
                  className={`flex items-center gap-4 px-3 py-3 rounded-xl transition-all group relative cursor-pointer select-none
                    ${isActive && !isParentExpanded ? 'bg-slate-800 text-orange-500' : 'hover:bg-slate-800 hover:text-white'}
                    ${isParentExpanded ? 'bg-slate-800 text-white' : ''}
                  `}
                >
                  <div className={`${isActive ? 'text-orange-500' : 'text-slate-400 group-hover:text-orange-400'}`}>
                    {item.icon}
                  </div>

                  {isOpen && (
                    <>
                      <span className="font-medium whitespace-nowrap flex-1 text-sm">
                        {item.title}
                      </span>
                      {/* Icon mũi tên xoay */}
                      <span className="text-slate-500">
                        {isParentExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                      </span>
                    </>
                  )}

                  {/* Tooltip khi đóng sidebar */}
                  {!isOpen && (
                    <div className="absolute left-16 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity whitespace-nowrap z-50 shadow-xl border border-slate-700">
                      {item.title}
                    </div>
                  )}
                </div>
              ) : (
                // --- TRƯỜNG HỢP MENU THƯỜNG (Dạng Link) ---
                <Link
                  to={item.path}
                  className={`flex items-center gap-4 px-3 py-3 rounded-xl transition-all group relative
                    ${isActive
                      ? 'bg-orange-500 text-white shadow-lg shadow-orange-500/20'
                      : 'hover:bg-slate-800 hover:text-white'}`}
                >
                  <div className={`${isActive ? 'text-white' : 'text-slate-400 group-hover:text-orange-400'}`}>
                    {item.icon}
                  </div>
                  {isOpen && (
                    <span className="font-medium whitespace-nowrap animate-in fade-in slide-in-from-left-2 text-sm">
                      {item.title}
                    </span>
                  )}
                  {!isOpen && (
                    <div className="absolute left-16 bg-slate-800 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity whitespace-nowrap z-50 shadow-xl border border-slate-700">
                      {item.title}
                    </div>
                  )}
                </Link>
              )}

              {/* === RENDER MENU CON (SUB-MENU) === */}
              {hasChildren && isOpen && isParentExpanded && (
                <div className="mt-1 space-y-1 pl-11 overflow-hidden animate-in slide-in-from-top-2 duration-200">
                  {item.children.map((child, childIndex) => {
                    const isChildActive = location.pathname === child.path;
                    return (
                      <Link
                        key={childIndex}
                        to={child.path}
                        className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors
                          ${isChildActive
                            ? 'text-orange-500 bg-slate-800 font-medium'
                            : 'text-slate-400 hover:text-white hover:bg-slate-800/50'}`}
                      >
                        {/* Dấu chấm tròn nhỏ để trang trí */}
                        <Circle size={8} className={isChildActive ? 'fill-orange-500' : 'fill-transparent'} />
                        <span className="whitespace-nowrap">{child.title}</span>
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

export default AdminSidebar;