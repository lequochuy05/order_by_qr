// src/components/admin/layout/AdminLayout.jsx

import React, { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import AdminSidebar from './AdminSidebar';
import AdminHeader from './AdminHeader';
import AdminFooter from './AdminFooter';
import { ErrorBoundary } from '@shared/ui';

const getViewportWidth = () => {
  if (typeof window === 'undefined') return 1024;

  return Math.min(
    window.innerWidth || Number.POSITIVE_INFINITY,
    window.visualViewport?.width || Number.POSITIVE_INFINITY,
    document.documentElement.clientWidth || Number.POSITIVE_INFINITY,
    window.screen?.width || Number.POSITIVE_INFINITY,
  );
};

const AdminLayoutContent = () => {
  const [viewportWidth, setViewportWidth] = useState(getViewportWidth);
  const isCompactViewport = viewportWidth < 768;
  const [isSidebarOpen, setIsSidebarOpen] = useState(() => getViewportWidth() >= 1024);

  const toggleSidebar = () => setIsSidebarOpen(!isSidebarOpen);

  useEffect(() => {
    const syncViewport = () => {
      const nextViewportWidth = getViewportWidth();
      const compact = nextViewportWidth < 768;
      setViewportWidth(nextViewportWidth);

      if (compact) {
        setIsSidebarOpen(false);
      }
    };

    syncViewport();
    window.addEventListener('resize', syncViewport);
    window.visualViewport?.addEventListener('resize', syncViewport);

    return () => {
      window.removeEventListener('resize', syncViewport);
      window.visualViewport?.removeEventListener('resize', syncViewport);
    };
  }, []);

  return (
    <div
      className={`relative flex min-h-screen w-full max-w-full overflow-x-hidden bg-slate-50 transition-colors dark:bg-slate-950 ${
        isCompactViewport ? 'safe-top safe-right safe-bottom safe-left' : ''
      }`}
      style={
        isCompactViewport
          ? { width: `${viewportWidth}px`, maxWidth: `${viewportWidth}px` }
          : undefined
      }
    >
      {/* 1. Backdrop for Mobile Overlay */}
      {isCompactViewport && isSidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/40 backdrop-blur-[2px] transition-opacity duration-300"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* 2. Sidebar component */}
      <AdminSidebar isOpen={isSidebarOpen} isCompactViewport={isCompactViewport} />

      {/* 3. Container chính */}
      <div
        className={`flex min-h-screen min-w-0 max-w-full flex-1 flex-col transition-[padding] duration-300 ${
          isCompactViewport ? 'pl-0' : isSidebarOpen ? 'pl-64' : 'pl-20'
        }`}
      >
        <AdminHeader toggleSidebar={toggleSidebar} isCompactViewport={isCompactViewport} />

        <main
          className={`min-w-0 flex-1 overflow-x-hidden text-slate-900 transition-colors dark:text-slate-100 ${
            isCompactViewport ? 'p-3' : 'p-5 lg:p-8'
          }`}
        >
          <div className="admin-content mx-auto w-full min-w-0 max-w-7xl">
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
