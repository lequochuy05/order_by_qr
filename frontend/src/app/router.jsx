import { lazy, Suspense } from 'react';
import { Route, Routes, useLocation } from 'react-router-dom';

import { ProtectedRoute } from '@features/auth';
import { AdminLayout } from '@widgets/admin-layout';
import { ErrorBoundary } from '@shared/ui';
import { useTranslation } from '@shared/lib/i18n';

const LoginPage = lazy(() => import('@pages/auth/LoginPage.jsx'));
const ForgotPasswordPage = lazy(() => import('@pages/auth/ForgotPasswordPage.jsx'));
const ResetPasswordPage = lazy(() => import('@pages/auth/ResetPasswordPage.jsx'));
const OrderingPage = lazy(() => import('@pages/customer/OrderingPage.jsx'));
const DashboardPage = lazy(() => import('@pages/admin/DashboardPage.jsx'));
const ProfilePage = lazy(() => import('@pages/admin/ProfilePage.jsx'));
const SettingsPage = lazy(() => import('@pages/admin/SettingsPage.jsx'));
const CategoryPage = lazy(() => import('@pages/admin/CategoryPage.jsx'));
const MenuPage = lazy(() => import('@pages/admin/MenuPage.jsx'));
const ComboPage = lazy(() => import('@pages/admin/ComboPage.jsx'));
const InventoryPage = lazy(() => import('@pages/admin/InventoryPage.jsx'));
const VoucherPage = lazy(() => import('@pages/admin/VoucherPage.jsx'));
const PromotionPage = lazy(() => import('@pages/admin/PromotionPage.jsx'));
const UserPage = lazy(() => import('@pages/admin/UserPage.jsx'));
const RevenueStatsPage = lazy(() => import('@pages/admin/RevenueStatsPage.jsx'));
const TopDishesStatsPage = lazy(() => import('@pages/admin/TopDishesStatsPage.jsx'));
const StaffStatsPage = lazy(() => import('@pages/admin/StaffStatsPage.jsx'));
const TableMapPage = lazy(() => import('@pages/admin/TableMapPage.jsx'));
const ServiceRequestsPage = lazy(() => import('@pages/admin/ServiceRequestsPage.jsx'));
const TakeawayPage = lazy(() => import('@pages/admin/TakeawayPage.jsx'));
const OrderHistoryPage = lazy(() => import('@pages/admin/OrderHistoryPage.jsx'));
const KitchenPage = lazy(() => import('@pages/admin/KitchenPage.jsx'));
const LandingPage = lazy(() => import('@pages/landing/LandingPage.jsx'));
const UnauthorizedPage = lazy(() => import('@pages/system/UnauthorizedPage.jsx'));
const NotFoundPage = lazy(() => import('@pages/system/NotFoundPage.jsx'));
const SystemStatusPage = lazy(() => import('@pages/system/SystemStatusPage.jsx'));

const LoadingScreen = () => {
  const { isEn } = useTranslation();
  return (
    <div className="flex min-h-screen items-center justify-center">
      {isEn ? 'Loading...' : 'Đang tải...'}
    </div>
  );
};

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

const AppRouter = () => {
  const location = useLocation();

  return (
    // resetKeys=[location.pathname] giúp ErrorBoundary tự reset khi user navigate sang URL khác
    // tránh bị kẹt màn hình lỗi sau khi Back/Forward trình duyệt
    <ErrorBoundary fullScreen resetKeys={[location.pathname]}>
      <Suspense fallback={<LoadingScreen />}>
        <Routes>
          <Route path="/:tableCode/menu" element={<CustomerOrderingRoute />} />
          <Route path="/menu" element={<CustomerOrderingRoute />} />
          <Route path="/login" element={<AuthRoute />} />
          <Route path="/forgot-password" element={
            <ErrorBoundary fullScreen>
              <ForgotPasswordPage />
            </ErrorBoundary>
          } />
          <Route path="/reset-password" element={
            <ErrorBoundary fullScreen>
              <ResetPasswordPage />
            </ErrorBoundary>
          } />

        <Route
          element={
            <ErrorBoundary fullScreen>
              <ProtectedRoute allowedRoles={['ADMIN', 'MANAGER', 'CASHIER', 'STAFF', 'CHEF']} />
            </ErrorBoundary>
          }
        >
          <Route element={<AdminLayout />}>
            <Route path="/admin/profile" element={<ProfilePage />} />
            <Route path="/admin/settings" element={<SettingsPage />} />

            <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'MANAGER']} />}>
              <Route path="/admin/dashboard" element={<DashboardPage />} />
              <Route path="/admin/statistics/revenue" element={<RevenueStatsPage />} />
              <Route path="/admin/categories" element={<CategoryPage />} />
              <Route path="/admin/menu" element={<MenuPage />} />
              <Route path="/admin/combo" element={<ComboPage />} />
              <Route path="/admin/voucher" element={<VoucherPage />} />
              <Route path="/admin/promotions" element={<PromotionPage />} />
              <Route path="/admin/staffs" element={<UserPage />} />
              <Route path="/admin/statistics/top-dishes" element={<TopDishesStatsPage />} />
              <Route path="/admin/statistics/staff" element={<StaffStatsPage />} />
            </Route>

            <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'MANAGER', 'CASHIER']} />}>
              <Route path="/admin/history" element={<OrderHistoryPage />} />
            </Route>

            <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'MANAGER', 'CASHIER', 'STAFF']} />}>
              <Route path="/admin/tables" element={<TableMapPage />} />
              <Route path="/admin/service-requests" element={<ServiceRequestsPage />} />
              <Route path="/admin/takeaway" element={<TakeawayPage />} />
            </Route>

            <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'MANAGER', 'CHEF']} />}>
              <Route path="/admin/kitchen" element={<KitchenPage />} />
              <Route path="/admin/inventory" element={<InventoryPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="/" element={<LandingPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
        <Route path="/not-found" element={<NotFoundPage />} />
        <Route path="/offline" element={<SystemStatusPage statusCode="offline" />} />
        <Route path="/error/:statusCode" element={<SystemStatusPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  </ErrorBoundary>
  );
};

export default AppRouter;
