// src/components/admin/layout/AdminLayout.jsx

import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import AdminSidebar from './AdminSidebar';
import AdminHeader from './AdminHeader';
import AdminFooter from './AdminFooter';

const AdminLayout = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  const toggleSidebar = () => setIsSidebarOpen(!isSidebarOpen);

  return (
    <div className="flex min-h-screen bg-slate-50">
      {/* 1. Sidebar đã có class 'fixed' bên trong component */}
      <AdminSidebar isOpen={isSidebarOpen} />

      {/* 2. Container chính: thêm margin-left (ml) tương ứng với độ rộng Sidebar */}
      <div className={`flex-1 flex flex-col transition-all duration-300 min-h-screen
        ${isSidebarOpen ? 'lg:ml-64' : 'lg:ml-20'}`}>
        
        {/* Header cũng nên cố định hoặc chạy theo */}
        <AdminHeader toggleSidebar={toggleSidebar} />

        <main className="flex-1 p-8 overflow-x-hidden">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
        
        <AdminFooter />
      </div>
    </div>
  );
};

export default AdminLayout;