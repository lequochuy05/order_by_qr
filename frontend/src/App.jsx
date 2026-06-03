// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/admin/auth/ProtectedRoute';
import { Toaster } from 'react-hot-toast';
import AdminLayout from './components/admin/layout/AdminLayout';
import CategoryManager from './pages/admin/CategoryManager';
import MenuManager from './pages/admin/MenuManager';
import ComboManager from './pages/admin/ComboManager';
import VoucherManager from './pages/admin/VoucherManager';
import StaffManager from './pages/admin/StaffManager';
import TableManager from './pages/admin/TableManager';
import RevenueStats from './pages/admin/statistics/RevenueStats.jsx';
import TopDishesStats from './pages/admin/statistics/TopDishesStats.jsx';
import StaffStats from './pages/admin/statistics/StaffStats.jsx';
import KitchenManager from './pages/admin/KitchenManager';
import Dashboard from './pages/admin/Dashboard';
import OrderHistoryPage from './pages/admin/OrderHistoryPage';
import ProfilePage from './pages/admin/ProfilePage';
import SettingsPage from './pages/admin/SettingsPage';
import { lazy, Suspense } from 'react';


// Import các trang của bạn
const MenuPage = lazy(() => import('./pages/customer/MenuPage'));
const LoginPage = lazy(() => import('./pages/auth/LoginPage'));

function App() {
  return (
    <AuthProvider> {/* Quản lý trạng thái đăng nhập cho Admin */}
      <Toaster position="top-right" reverseOrder={false} />
      <BrowserRouter>
        <Suspense fallback={<div className="flex items-center justify-center min-h-screen">Đang tải...</div>}>
          <Routes>
            <Route path="/menu" element={<MenuPage />} /> {/* Trang menu cho khách hàng */}

            <Route path="/login" element={<LoginPage />} /> {/* Trang đăng nhập quản lý */}
            <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'STAFF', 'CHEF']} />}>
              <Route element={<AdminLayout />}>
                <Route path="/admin/dashboard" element={<Dashboard />} />
                <Route path="/admin/profile" element={<ProfilePage />} />
                <Route path="/admin/settings" element={<SettingsPage />} />

                {/* Hạng mục chỉ dành cho Quản lý (MANAGER) */}
                <Route element={<ProtectedRoute allowedRoles={['MANAGER']} />}>
                  <Route path="/admin/categories" element={<CategoryManager />} />
                  <Route path="/admin/menu" element={<MenuManager />} />
                  <Route path="/admin/combo" element={<ComboManager />} />
                  <Route path="/admin/voucher" element={<VoucherManager />} />
                  <Route path="/admin/staffs" element={<StaffManager />} />
                  <Route path="/admin/statistics/revenue" element={<RevenueStats />} />
                  <Route path="/admin/statistics/top-dishes" element={<TopDishesStats />} />
                  <Route path="/admin/statistics/staff" element={<StaffStats />} />
                </Route>

                {/* Hạng mục dành cho Quản lý & Nhân viên phục vụ (MANAGER, STAFF) */}
                <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'STAFF']} />}>
                  <Route path="/admin/tables" element={<TableManager />} />
                  <Route path="/admin/history" element={<OrderHistoryPage />} />
                </Route>

                {/* Hạng mục dành cho Quản lý & Bếp (MANAGER, CHEF) */}
                <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'CHEF']} />}>
                  <Route path="/admin/kitchen" element={<KitchenManager />} />
                </Route>
              </Route>
            </Route>

            {/* Điều hướng mặc định khi khách vào trang chủ */}
            <Route path="/" element={<Navigate to="/menu?tableCode=c21edc13d371" replace />} />

            {/* Trang báo lỗi phân quyền */}
            <Route path="/unauthorized" element={<div className="flex flex-col items-center justify-center min-h-screen bg-slate-50 text-slate-800"><h1 className="text-4xl font-extrabold mb-2 text-red-500">403</h1><p className="text-lg">Bạn không có quyền truy cập trang này!</p><a href="/admin/dashboard" className="mt-4 px-4 py-2 bg-orange-500 text-white font-bold rounded-xl hover:bg-orange-600 transition-colors">Về trang chủ</a></div>} />

            {/* Trang lỗi 404 */}
            <Route path="*" element={<div>Trang không tồn tại</div>} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
