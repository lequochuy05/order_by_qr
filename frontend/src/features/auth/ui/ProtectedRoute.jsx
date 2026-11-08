import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../model/AuthContext.jsx';

const ProtectedRoute = ({ allowedRoles }) => {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) return <Navigate to="/login" replace />;

  // Cưỡng chế đổi mật khẩu bắt buộc: nếu tài khoản chưa đổi mật khẩu khởi tạo,
  // chỉ được phép truy cập /admin/profile để cập nhật mật khẩu mới.
  if (user.mustChangePassword && location.pathname !== '/admin/profile') {
    return <Navigate to="/admin/profile?forceChangePassword=true" replace />;
  }

  // Kiểm tra role
  const hasAccess = allowedRoles.includes(user.role);

  return hasAccess ? <Outlet /> : <Navigate to="/unauthorized" replace />;
};
export default ProtectedRoute;
