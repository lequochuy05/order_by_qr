import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";

const ProtectedRoute = ({ allowedRoles }) => {
    const { user } = useAuth();

    if (!user) return <Navigate to="/login" replace />;

    // Kiểm tra role
    const hasAccess = allowedRoles.includes(user.role);

    return hasAccess ? <Outlet /> : <Navigate to="/unauthorized" replace />;
};
export default ProtectedRoute;