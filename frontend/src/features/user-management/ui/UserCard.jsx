import {
  CreditCard,
  Mail,
  Phone,
  Shield,
  ShieldCheck,
  User as UserIcon,
  UtensilsCrossed,
} from 'lucide-react';
import { useAuth } from '@features/auth';
import { fmtRole, fmtStatus } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const UserCard = ({ staff, onEdit, onDelete }) => {
  const { user: currentUser } = useAuth();

  if (!staff) return null;

  const isManager = currentUser?.role === 'MANAGER';
  const isPrivilegedTarget = staff.role === 'ADMIN' || staff.role === 'MANAGER';
  const canManage = !(isManager && isPrivilegedTarget);

  const status = fmtStatus('staff', staff.status);

  const getRoleIcon = () => {
    switch (staff.role) {
      case 'ADMIN':
        return <ShieldCheck size={20} />;
      case 'MANAGER':
        return <Shield size={20} />;
      case 'CASHIER':
        return <CreditCard size={20} />;
      case 'CHEF':
        return <UtensilsCrossed size={20} />;
      default:
        return <UserIcon size={20} />;
    }
  };

  const getRoleColor = () => {
    switch (staff.role) {
      case 'ADMIN':
        return 'bg-gradient-to-tr from-purple-600 to-indigo-600 text-white shadow-purple-500/25';
      case 'MANAGER':
        return 'bg-gradient-to-tr from-indigo-600 to-blue-600 text-white shadow-indigo-500/25';
      case 'CASHIER':
        return 'bg-gradient-to-tr from-teal-600 to-emerald-600 text-white shadow-teal-500/25';
      case 'CHEF':
        return 'bg-gradient-to-tr from-amber-500 to-orange-500 text-white shadow-amber-500/25';
      default:
        return 'bg-gradient-to-tr from-blue-600 to-cyan-600 text-white shadow-blue-500/25';
    }
  };

  return (
    <div className="group flex h-full flex-col rounded-3xl border border-[#1E293B] bg-white/90 p-6 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:border-indigo-500 hover:shadow-xl dark:border-[#1E293B] dark:bg-[#0F172A]/90">
      {/* Header: Role & Status */}
      <div className="flex justify-between items-start mb-4">
        <div className={`p-2.5 rounded-2xl shadow-md ${getRoleColor()}`}>{getRoleIcon()}</div>
        <div className="flex items-center gap-1.5 flex-wrap justify-end">
          {staff.mustChangePassword && (
            <span
              className="px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wide bg-amber-50 text-amber-700 border border-amber-200/60 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800/40"
              title="Nhân viên chưa đổi mật khẩu khởi tạo"
            >
              Mật khẩu tạm
            </span>
          )}
          <span
            className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${status.color}`}
          >
            {status.label}
          </span>
        </div>
      </div>

      {/* Avatar & Name */}
      <div className="flex flex-col items-center mb-5">
        <div className="mb-3 h-20 w-20 rounded-full border-2 border-slate-200/80 p-1 transition-all group-hover:border-indigo-400 group-hover:scale-105 duration-300 dark:border-slate-700">
          <img
            src={
              staff.avatarUrl ||
              `https://ui-avatars.com/api/?name=${encodeURIComponent(staff.fullName)}&background=6366f1&color=fff`
            }
            alt={staff.fullName}
            className="w-full h-full rounded-full object-cover shadow-sm"
          />
        </div>
        <h3 className="line-clamp-1 text-center text-lg font-black text-slate-900 dark:text-white">
          {staff.fullName}
        </h3>
        <span className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 mt-0.5">
          {fmtRole(staff.role)}
        </span>
      </div>

      {/* Info Details */}
      <div className="mb-5 flex-1 space-y-2.5 text-xs">
        <div className="flex items-center gap-2.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 p-3 border border-slate-100 dark:border-slate-800/60 text-slate-600 dark:text-slate-300">
          <Mail size={15} className="shrink-0 text-slate-400 dark:text-slate-500" />
          <span className="truncate font-medium" title={staff.email}>
            {staff.email}
          </span>
        </div>
        <div className="flex items-center gap-2.5 rounded-2xl bg-slate-50 dark:bg-slate-800/50 p-3 border border-slate-100 dark:border-slate-800/60 text-slate-600 dark:text-slate-300">
          <Phone size={15} className="shrink-0 text-slate-400 dark:text-slate-500" />
          <span className="font-medium">{staff.phone || 'Chưa cập nhật'}</span>
        </div>
      </div>

      {canManage ? (
        <EditDeleteActions onEdit={onEdit} onDelete={onDelete} className="pt-3" />
      ) : (
        <div className="flex items-center justify-center py-2.5 rounded-2xl bg-slate-50 text-xs font-semibold text-slate-400 dark:bg-slate-800/40 dark:text-slate-500">
          Chỉ Quản trị viên mới có quyền thao tác
        </div>
      )}
    </div>
  );
};

export default UserCard;
