// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/admin/auth/ProtectedRoute';
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

// Import các trang của bạn
import MenuPage from './pages/customer/MenuPage';
import LoginPage from './pages/auth/LoginPage';

function App() {
  return (
    <AuthProvider> {/* Quản lý trạng thái đăng nhập cho Admin */}
      <BrowserRouter>
        <Routes>
          <Route path="/menu" element={<MenuPage />} /> {/* Trang menu cho khách hàng */}

          <Route path="/login" element={<LoginPage />} /> {/* Trang đăng nhập quản lý */}
          <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'STAFF', 'CHEF']} />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin/dashboard" element={<Dashboard />} />
              <Route path="/admin/history" element={<OrderHistoryPage />} />
              <Route path="/admin/kitchen" element={<KitchenManager />} />
              <Route path="/admin/tables" element={<TableManager />} />
              <Route path="/admin/categories" element={<CategoryManager />} />
              <Route path="/admin/menu" element={<MenuManager />} />
              <Route path="/admin/combo" element={<ComboManager />} />
              <Route path="/admin/voucher" element={<VoucherManager />} />
              {/* <Route path="/admin/promotions" element={<div>Quản lý khuyến mãi</div>} /> */}
              <Route path="/admin/staffs" element={<StaffManager />} />
              <Route path="/admin/statistics" element={<div>Thống kê</div>} />
              <Route path="/admin/statistics/revenue" element={<RevenueStats />} />
              <Route path="/admin/statistics/top-dishes" element={<TopDishesStats />} />
              <Route path="/admin/statistics/staff" element={<StaffStats />} />
            </Route>
          </Route>

          {/* Điều hướng mặc định khi khách vào trang chủ */}
          <Route path="/" element={<Navigate to="/menu?tableCode=c21edc13d371" replace />} />

          {/* Trang lỗi 404 */}
          <Route path="*" element={<div>Trang không tồn tại</div>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;