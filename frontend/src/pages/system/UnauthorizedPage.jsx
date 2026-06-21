import { Link } from 'react-router-dom';

const UnauthorizedPage = () => (
  <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 text-slate-800">
    <h1 className="mb-2 text-4xl font-extrabold text-red-500">403</h1>
    <p className="text-lg">Bạn không có quyền truy cập trang này!</p>
    <Link
      to="/admin/dashboard"
      className="mt-4 rounded-xl bg-orange-500 px-4 py-2 font-bold text-white transition-colors hover:bg-orange-600"
    >
      Về trang chủ
    </Link>
  </div>
);

export default UnauthorizedPage;
