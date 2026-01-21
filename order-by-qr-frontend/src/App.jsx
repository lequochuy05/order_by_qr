// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/admin/auth/ProtectedRoute';
import AdminLayout from './components/admin/layout/AdminLayout';
import CategoryManager from './pages/admin/CategoryManager';
import MenuManager from './pages/admin/MenuManager';
import ComboManager from './pages/admin/ComboManager';
import VoucherManager from './pages/admin/VoucherManager.jsx';
import StaffManager from './pages/admin/StaffManager.jsx';

// Import các trang của bạn
import MenuPage from './pages/customer/MenuPage';
import LoginPage from './pages/auth/LoginPage';

function App() {
  return (
    <AuthProvider> {/* Quản lý trạng thái đăng nhập cho Admin */}
      <BrowserRouter>
        <Routes>
          {/* 1. LỐI VÀO CHO KHÁCH: Không cần đăng nhập, quét mã là vào */}
          <Route path="/menu" element={<MenuPage />} />
          
          {/* 2. LỐI VÀO CHO ADMIN: Phải đăng nhập */}
          <Route path="/login" element={<LoginPage />} />

          {/* 3. KHU VỰC ADMIN: Được bảo vệ bởi ProtectedRoute */}
          <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'STAFF']} />}>
            <Route element={<AdminLayout />}>
              <Route path="/admin/dashboard" element={<div>Bảng điều khiển</div>} />
              <Route path="/admin/orders" element={<div>Quản lý đơn hàng</div>} />
              <Route path="/admin/tables" element={<div>Quản lý bàn</div>} />
              <Route path="/admin/categories" element={<CategoryManager />} />
              <Route path="/admin/menu" element={<MenuManager />} />
              <Route path="/admin/combo" element={<ComboManager />} />
              <Route path="/admin/voucher" element={<VoucherManager />} />
              <Route path="/admin/promotions" element={<div>Quản lý khuyến mãi</div>} />
              <Route path="/admin/staffs" element={<StaffManager />} />
              <Route path="/admin/statistics" element={<div>Thống kê</div>} />
              <Route path="/admin/settings" element={<div>Cài đặt</div>} />
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