import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'

import ProtectedRoute from '@modules/auth/ui/ProtectedRoute.jsx'
import AdminLayout from '@widgets/admin-layout/AdminLayout.jsx'

const LoginPage = lazy(() => import('@pages/auth/LoginPage.jsx'))
const OrderingPage = lazy(() => import('@pages/customer/OrderingPage.jsx'))
const DashboardPage = lazy(() => import('@pages/admin/DashboardPage.jsx'))
const ProfilePage = lazy(() => import('@pages/admin/ProfilePage.jsx'))
const SettingsPage = lazy(() => import('@pages/admin/SettingsPage.jsx'))
const CategoryPage = lazy(() => import('@pages/admin/CategoryPage.jsx'))
const MenuPage = lazy(() => import('@pages/admin/MenuPage.jsx'))
const ComboPage = lazy(() => import('@pages/admin/ComboPage.jsx'))
const VoucherPage = lazy(() => import('@pages/admin/VoucherPage.jsx'))
const StaffPage = lazy(() => import('@pages/admin/StaffPage.jsx'))
const RevenueStatsPage = lazy(() => import('@pages/admin/RevenueStatsPage.jsx'))
const TopDishesStatsPage = lazy(() => import('@pages/admin/TopDishesStatsPage.jsx'))
const StaffStatsPage = lazy(() => import('@pages/admin/StaffStatsPage.jsx'))
const TableMapPage = lazy(() => import('@pages/admin/TableMapPage.jsx'))
const OrderHistoryPage = lazy(() => import('@pages/admin/OrderHistoryPage.jsx'))
const KitchenPage = lazy(() => import('@pages/admin/KitchenPage.jsx'))

const LoadingScreen = () => (
  <div className="flex min-h-screen items-center justify-center">Đang tải...</div>
)

const UnauthorizedPage = () => (
  <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 text-slate-800">
    <h1 className="mb-2 text-4xl font-extrabold text-red-500">403</h1>
    <p className="text-lg">Bạn không có quyền truy cập trang này!</p>
    <a href="/admin/dashboard" className="mt-4 rounded-xl bg-orange-500 px-4 py-2 font-bold text-white transition-colors hover:bg-orange-600">
      Về trang chủ
    </a>
  </div>
)

const AppRouter = () => (
  <Suspense fallback={<LoadingScreen />}>
    <Routes>
      <Route path="/menu" element={<OrderingPage />} />
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'STAFF', 'CHEF']} />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin/dashboard" element={<DashboardPage />} />
          <Route path="/admin/profile" element={<ProfilePage />} />
          <Route path="/admin/settings" element={<SettingsPage />} />

          <Route element={<ProtectedRoute allowedRoles={['MANAGER']} />}>
            <Route path="/admin/categories" element={<CategoryPage />} />
            <Route path="/admin/menu" element={<MenuPage />} />
            <Route path="/admin/combo" element={<ComboPage />} />
            <Route path="/admin/voucher" element={<VoucherPage />} />
            <Route path="/admin/staffs" element={<StaffPage />} />
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

      <Route path="/" element={<Navigate to="/menu?tableCode=5c8006237e33" replace />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route path="*" element={<div>Trang không tồn tại</div>} />
    </Routes>
  </Suspense>
)

export default AppRouter
