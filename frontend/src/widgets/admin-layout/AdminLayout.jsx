// src/components/admin/layout/AdminLayout.jsx

import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import AdminSidebar from './AdminSidebar';
import AdminHeader from './AdminHeader';
import AdminFooter from './AdminFooter';
import { ErrorBoundary } from '@shared/ui';

const AdminLayoutContent = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(window.innerWidth >= 1024);

  const toggleSidebar = () => setIsSidebarOpen(!isSidebarOpen);

  return (
    <div className="flex min-h-screen bg-slate-50 dark:bg-slate-950 relative overflow-x-hidden transition-colors">
      {/* 1. Backdrop for Mobile Overlay */}
      {isSidebarOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-black/40 z-30 backdrop-blur-[2px] transition-opacity duration-300"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* 2. Sidebar component */}
      <AdminSidebar isOpen={isSidebarOpen} />

      {/* 3. Container chính */}
      <div
        className={`flex-1 flex flex-col transition-all duration-300 min-h-screen
        ${isSidebarOpen ? 'lg:ml-64' : 'lg:ml-20'}`}
      >
        <AdminHeader toggleSidebar={toggleSidebar} />

        <main className="flex-1 p-8 overflow-x-hidden text-slate-900 transition-colors dark:text-slate-100">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>

        <AdminFooter />
      </div>
    </div>
  );
};

const AdminLayout = () => (
  <ErrorBoundary>
    <AdminLayoutContent />
  </ErrorBoundary>
);

export default AdminLayout;
