import React, { useEffect, useState } from 'react';
import { Outlet, Link } from 'react-router-dom';
import { useAuth } from '@features/auth';
import { AlertTriangle, ChevronRight } from 'lucide-react';
import AdminSidebar from './AdminSidebar';
import AdminHeader from './AdminHeader';
import AdminFooter from './AdminFooter';
import { ErrorBoundary, SpotlightGrid } from '@shared/ui';

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
      className={`relative flex h-dvh min-h-dvh w-full max-w-full overflow-hidden transition-colors ${
        isCompactViewport ? 'safe-top safe-right safe-bottom safe-left' : ''
      }`}
      style={
        isCompactViewport
          ? { width: `${viewportWidth}px`, maxWidth: `${viewportWidth}px` }
          : undefined
      }
    >
      {/* 0. Full-Screen Interactive Spotlight Dot Grid Background */}
      <SpotlightGrid />

      {/* 1. Backdrop for Mobile Overlay */}
      {isCompactViewport && isSidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/60 backdrop-blur-sm transition-opacity duration-300"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* 2. Sidebar component */}
      <AdminSidebar isOpen={isSidebarOpen} isCompactViewport={isCompactViewport} />

      {/* 3. Main Container */}
      <div
        className={`relative z-10 flex h-full min-h-0 min-w-0 max-w-full flex-1 flex-col transition-[padding] duration-300 ${
          isCompactViewport ? 'pl-0' : isSidebarOpen ? 'pl-64' : 'pl-20'
        }`}
      >
        <AdminHeader toggleSidebar={toggleSidebar} isCompactViewport={isCompactViewport} />

        <div className="flex min-h-0 flex-1 flex-col overflow-y-auto overflow-x-hidden">
          {user?.mustChangePassword && (
            <div className="border-b border-amber-200/80 bg-gradient-to-r from-amber-500/10 via-amber-500/5 to-transparent px-4 py-2.5 text-amber-900 dark:border-amber-900/60 dark:text-amber-200 text-xs sm:text-sm flex items-center justify-between gap-3 shrink-0 shadow-sm backdrop-blur-sm">
              <div className="flex items-center gap-2.5 min-w-0">
                <span className="p-1 rounded-lg bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-400 shrink-0">
                  <AlertTriangle className="h-4 w-4" />
                </span>
                <span className="truncate font-semibold">
                  Tài khoản của bạn đang dùng mật khẩu tạm thời. Vui lòng đổi mật khẩu để bảo vệ hệ thống.
                </span>
              </div>
              <Link
                to="/admin/profile?forceChangePassword=true"
                className="inline-flex items-center gap-1 shrink-0 font-bold text-amber-800 hover:text-amber-950 dark:text-amber-300 dark:hover:text-amber-100 text-xs underline underline-offset-2 hover:no-underline"
              >
                Đổi mật khẩu ngay
                <ChevronRight className="h-3.5 w-3.5" />
              </Link>
            </div>
          )}

          <main
            className={`min-w-0 flex-1 text-slate-900 transition-colors dark:text-slate-100 ${
              isCompactViewport ? 'p-3' : 'p-4 sm:p-6 lg:p-8'
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
