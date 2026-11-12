import React, { useEffect, useState } from 'react';
import { Outlet, Link } from 'react-router-dom';
import { useAuth } from '@features/auth';
import { AlertTriangle, ChevronRight } from 'lucide-react';
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
  const { user } = useAuth();
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
      className={`relative flex h-dvh min-h-dvh w-full max-w-full overflow-hidden bg-[#F7F0E7] transition-colors dark:bg-[#14100E] ${
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
        className={`flex h-full min-h-0 min-w-0 max-w-full flex-1 flex-col transition-[padding] duration-300 ${
          isCompactViewport ? 'pl-0' : isSidebarOpen ? 'pl-64' : 'pl-20'
        }`}
      >
        <AdminHeader toggleSidebar={toggleSidebar} isCompactViewport={isCompactViewport} />

        <div className="flex min-h-0 flex-1 flex-col overflow-y-auto overflow-x-hidden">
          {user?.mustChangePassword && (
            <div className="border-b border-amber-200 bg-amber-50 px-4 py-2.5 text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/50 dark:text-amber-200 text-sm flex items-center justify-between gap-3 shrink-0 shadow-sm">
              <div className="flex items-center gap-2 min-w-0">
                <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
                <span className="truncate font-medium">
                  Tài khoản của bạn đang dùng mật khẩu tạm thời. Vui lòng đổi mật khẩu để bảo vệ hệ thống.
                </span>
              </div>
              <Link
                to="/admin/profile?forceChangePassword=true"
                className="inline-flex items-center gap-1 shrink-0 font-semibold text-amber-900 hover:text-amber-700 dark:text-amber-300 dark:hover:text-amber-200 text-xs sm:text-sm underline hover:no-underline"
              >
                Đổi mật khẩu ngay
                <ChevronRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          )}

          <main
            className={`min-w-0 flex-1 text-slate-900 transition-colors dark:text-slate-100 ${
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
    </div>
  );
};

const AdminLayout = () => (
  <ErrorBoundary>
    <AdminLayoutContent />
  </ErrorBoundary>
);

export default AdminLayout;
