import {
  Shield,
  User as UserIcon,
  UtensilsCrossed,
  Mail,
  Phone,
} from 'lucide-react';
import { fmtRole, fmtStatus } from '@shared/lib/formatters.js';
import { EditDeleteActions } from '@shared/ui';

const UserCard = ({ staff, onEdit, onDelete }) => {
  // Kiểm tra an toàn để tránh lỗi nếu staff bị null/undefined
  if (!staff) return null;

  const status = fmtStatus('staff', staff.status);

  const getRoleIcon = () => {
    switch (staff.role) {
      case 'MANAGER':
        return <Shield size={20} />;
      case 'CHEF':
        return <UtensilsCrossed size={20} />;
      default:
        return <UserIcon size={20} />;
    }
  };

  const getRoleColor = () => {
    switch (staff.role) {
      case 'MANAGER':
        return 'bg-emerald-100 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300';
      case 'CHEF':
        return 'bg-orange-100 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300';
      default:
        return 'bg-blue-100 text-blue-600 dark:bg-blue-500/10 dark:text-blue-300';
    }
  };

  return (
    <div className="group flex h-full flex-col rounded-3xl border border-slate-100 bg-white p-5 shadow-sm transition-all hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
      {/* Header: Role & Status */}
      <div className="flex justify-between items-start mb-4">
        <div className={`p-2.5 rounded-2xl ${getRoleColor()}`}>{getRoleIcon()}</div>
        <span
          className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wide ${status.color}`}
        >
          {status.label}
        </span>
      </div>

      {/* Avatar & Name */}
      <div className="flex flex-col items-center mb-6">
        <div className="mb-3 h-20 w-20 rounded-full border-2 border-dashed border-slate-200 p-1 transition-colors group-hover:border-orange-400 dark:border-slate-700">
          <img
            src={
              staff.avatarUrl ||
              `https://ui-avatars.com/api/?name=${encodeURIComponent(staff.fullName)}&background=random`
            }
            alt={staff.fullName}
            className="w-full h-full rounded-full object-cover"
          />
        </div>
        <h3 className="line-clamp-1 text-center text-lg font-bold text-slate-800 dark:text-slate-100">
          {staff.fullName}
        </h3>
        <span className="text-xs font-medium text-slate-500 dark:text-slate-400">
          {fmtRole(staff.role)}
        </span>
      </div>

      {/* Info Details */}
      <div className="mb-6 flex-1 space-y-3 text-sm text-slate-600 dark:text-slate-300">
        <div className="flex items-center gap-3 rounded-xl bg-slate-50 p-2.5 dark:bg-slate-950/60">
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

      <EditDeleteActions onEdit={onEdit} onDelete={onDelete} />
    </div>
  );
};

export default UserCard;
