import { lazy, Suspense } from 'react';
import { Link, Route, Routes } from 'react-router-dom';
import { LogIn, QrCode, ScanLine, Utensils } from 'lucide-react';

import { ProtectedRoute } from '@features/auth';
import AdminLayout from '@widgets/admin-layout/AdminLayout.jsx';
import { ErrorBoundary } from '@shared/ui';

const LoginPage = lazy(() => import('@pages/auth/LoginPage.jsx'));
const OrderingPage = lazy(() => import('@pages/customer/OrderingPage.jsx'));
const DashboardPage = lazy(() => import('@pages/admin/DashboardPage.jsx'));
const ProfilePage = lazy(() => import('@pages/admin/ProfilePage.jsx'));
const SettingsPage = lazy(() => import('@pages/admin/SettingsPage.jsx'));
const CategoryPage = lazy(() => import('@pages/admin/CategoryPage.jsx'));
const MenuPage = lazy(() => import('@pages/admin/MenuPage.jsx'));
const ComboPage = lazy(() => import('@pages/admin/ComboPage.jsx'));
const InventoryPage = lazy(() => import('@pages/admin/InventoryPage.jsx'));
const VoucherPage = lazy(() => import('@pages/admin/VoucherPage.jsx'));
const UserPage = lazy(() => import('@pages/admin/UserPage.jsx'));
const RevenueStatsPage = lazy(() => import('@pages/admin/RevenueStatsPage.jsx'));
const TopDishesStatsPage = lazy(() => import('@pages/admin/TopDishesStatsPage.jsx'));
const StaffStatsPage = lazy(() => import('@pages/admin/StaffStatsPage.jsx'));
const TableMapPage = lazy(() => import('@pages/admin/TableMapPage.jsx'));
const OrderHistoryPage = lazy(() => import('@pages/admin/OrderHistoryPage.jsx'));
const KitchenPage = lazy(() => import('@pages/admin/KitchenPage.jsx'));

const LoadingScreen = () => (
  <div className="flex min-h-screen items-center justify-center">Đang tải...</div>
);

const UnauthorizedPage = () => (
  <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 text-slate-800">
    <h1 className="mb-2 text-4xl font-extrabold text-red-500">403</h1>
    <p className="text-lg">Bạn không có quyền truy cập trang này!</p>
    <a
      href="/admin/dashboard"
      className="mt-4 rounded-xl bg-orange-500 px-4 py-2 font-bold text-white transition-colors hover:bg-orange-600"
    >
      Về trang chủ
    </a>
  </div>
);

const LandingPage = () => (
  <div className="min-h-screen bg-neutral-950 text-white">
    <div className="mx-auto flex min-h-screen w-full max-w-md flex-col px-6 py-8">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-orange-500">
            <Utensils size={20} />
          </div>
          <span className="text-lg font-black">QROS</span>
        </div>
        <Link
          to="/login"
          className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10 text-white transition-colors hover:bg-white/15"
          aria-label="Đăng nhập"
          title="Đăng nhập"
        >
          <LogIn size={18} />
        </Link>
      </div>

      <div className="flex flex-1 flex-col items-center justify-center text-center">
        <div className="relative mb-8 flex h-36 w-36 items-center justify-center rounded-[2rem] border border-white/10 bg-white">
          <QrCode className="text-neutral-950" size={92} strokeWidth={1.8} />
          <div className="absolute -right-3 -top-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-orange-500 shadow-lg shadow-orange-500/30">
            <ScanLine size={24} />
          </div>
        </div>

        <h1 className="text-3xl font-black leading-tight">Quét mã QR trên bàn</h1>
        <p className="mt-3 max-w-xs text-sm font-medium leading-6 text-neutral-300">
          Mã QR sẽ mở đúng thực đơn và phiên gọi món của bàn bạn đang ngồi.
        </p>
      </div>

      <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-center text-xs font-semibold leading-5 text-neutral-300">
        Nếu bạn là nhân viên, hãy đăng nhập để vào khu vực vận hành.
      </div>
    </div>
  </div>
);

const AuthRoute = () => (
  <ErrorBoundary fullScreen>
    <LoginPage />
  </ErrorBoundary>
);

const CustomerOrderingRoute = () => (
  <ErrorBoundary fullScreen>
    <OrderingPage />
  </ErrorBoundary>
);

const AppRouter = () => (
  <ErrorBoundary fullScreen>
    <Suspense fallback={<LoadingScreen />}>
      <Routes>
        <Route path="/:tableCode/menu" element={<CustomerOrderingRoute />} />
        <Route path="/menu" element={<CustomerOrderingRoute />} />
        <Route path="/login" element={<AuthRoute />} />

        <Route
          element={
            <ErrorBoundary fullScreen>
              <ProtectedRoute allowedRoles={['MANAGER', 'STAFF', 'CHEF']} />
            </ErrorBoundary>
          }
        >
          <Route element={<AdminLayout />}>
            <Route path="/admin/dashboard" element={<DashboardPage />} />
            <Route path="/admin/profile" element={<ProfilePage />} />
            <Route path="/admin/settings" element={<SettingsPage />} />

            <Route element={<ProtectedRoute allowedRoles={['MANAGER']} />}>
              <Route path="/admin/categories" element={<CategoryPage />} />
              <Route path="/admin/menu" element={<MenuPage />} />
              <Route path="/admin/combo" element={<ComboPage />} />
              <Route path="/admin/inventory" element={<InventoryPage />} />
              <Route path="/admin/voucher" element={<VoucherPage />} />
              <Route path="/admin/staffs" element={<UserPage />} />
              <Route path="/admin/statistics/revenue" element={<RevenueStatsPage />} />
              <Route path="/admin/statistics/top-dishes" element={<TopDishesStatsPage />} />
              <Route path="/admin/statistics/staff" element={<StaffStatsPage />} />
            </Route>

            <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'STAFF']} />}>
              <Route path="/admin/tables" element={<TableMapPage />} />
              <Route path="/admin/history" element={<OrderHistoryPage />} />
            </Route>

            <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'CHEF']} />}>
              <Route path="/admin/kitchen" element={<KitchenPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="/" element={<LandingPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<div>Trang không tồn tại</div>} />
      </Routes>
    </Suspense>
  </ErrorBoundary>
);

export default AppRouter;
