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

  // Kiểm tra an toàn để tránh lỗi nếu staff bị null/undefined
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
        return 'bg-purple-100 text-purple-600 dark:bg-purple-500/10 dark:text-purple-300';
      case 'MANAGER':
        return 'bg-emerald-100 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300';
      case 'CASHIER':
        return 'bg-teal-100 text-teal-600 dark:bg-teal-500/10 dark:text-teal-300';
      case 'CHEF':
        return 'bg-orange-100 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300';
      default:
        return 'bg-blue-100 text-blue-600 dark:bg-blue-500/10 dark:text-blue-300';
    }
  };

  return (
    <div className="group flex h-full flex-col rounded-3xl border border-[#E8DCD1] bg-white p-5 shadow-sm transition-all hover:shadow-md dark:border-[#352C26] dark:bg-[#1F1A17]">
      {/* Header: Role & Status */}
      <div className="flex justify-between items-start mb-4">
        <div className={`p-2.5 rounded-2xl ${getRoleColor()}`}>{getRoleIcon()}</div>
        <div className="flex items-center gap-1.5 flex-wrap justify-end">
          {staff.mustChangePassword && (
            <span
              className="px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wide bg-amber-100 text-amber-700 dark:bg-amber-500/10 dark:text-amber-300"
              title="Nhân viên chưa đổi mật khẩu khởi tạo"
            >
              Mật khẩu tạm
            </span>
          )}
          <span
            className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wide ${status.color}`}
          >
            {status.label}
          </span>
        </div>
      </div>

      {/* Avatar & Name */}
      <div className="flex flex-col items-center mb-6">
        <div className="mb-3 h-20 w-20 rounded-full border-2 border-dashed border-[#E8DCD1] p-1 transition-colors group-hover:border-[#C17C4A] dark:border-[#352C26]">
          <img
            src={
              staff.avatarUrl ||
              `https://ui-avatars.com/api/?name=${encodeURIComponent(staff.fullName)}&background=random`
            }
            alt={staff.fullName}
            className="w-full h-full rounded-full object-cover"
          />
        </div>
        <h3 className="line-clamp-1 text-center text-lg font-bold text-[#2D1D16] dark:text-[#F7F0E7]">
          {staff.fullName}
        </h3>
        <span className="text-xs font-semibold text-[#6B5D55] dark:text-[#BDB0A4]">
          {fmtRole(staff.role)}
        </span>
      </div>

      {/* Info Details */}
      <div className="mb-6 flex-1 space-y-3 text-sm text-[#6B5D55] dark:text-[#BDB0A4]">
        <div className="flex items-center gap-3 rounded-xl bg-[#F7F0E7]/60 p-2.5 dark:bg-[#2A221E]">
          <Mail size={16} className="flex-shrink-0 text-slate-400 dark:text-slate-500" />
          <span className="truncate" title={staff.email}>
            {staff.email}
          </span>
        </div>
        <div className="flex items-center gap-3 rounded-xl bg-slate-50 p-2.5 dark:bg-slate-950/60">
          <Phone size={16} className="flex-shrink-0 text-slate-400 dark:text-slate-500" />
          <span>{staff.phone || 'Chưa cập nhật'}</span>
        </div>
      </div>

      {canManage ? (
        <EditDeleteActions onEdit={onEdit} onDelete={onDelete} />
      ) : (
        <div className="flex items-center justify-center py-2.5 rounded-xl bg-slate-50 text-xs font-medium text-slate-400 dark:bg-slate-800/40 dark:text-slate-500">
          Chỉ Quản trị viên mới có quyền thao tác
        </div>
      )}
    </div>
  );
};

export default UserCard;
