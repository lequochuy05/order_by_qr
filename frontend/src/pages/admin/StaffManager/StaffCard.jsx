import { Shield, User as UserIcon, UtensilsCrossed, Mail, Phone, Pencil, Trash2 } from 'lucide-react';
import { fmtRole, fmtStatus } from '../../../utils/formatters';

const StaffCard = ({ staff, onEdit, onDelete }) => {
  // Kiểm tra an toàn để tránh lỗi nếu staff bị null/undefined
  if (!staff) return null;

  const status = fmtStatus('staff', staff.status);

  const getRoleIcon = () => {
    switch (staff.role) {
      case 'MANAGER': return <Shield size={20} />;
      case 'CHEF': return <UtensilsCrossed size={20} />;
      default: return <UserIcon size={20} />;
    }
  };

  const getRoleColor = () => {
    switch (staff.role) {
      case 'MANAGER': return 'bg-purple-100 text-purple-600';
      case 'CHEF': return 'bg-orange-100 text-orange-600';
      default: return 'bg-blue-100 text-blue-600';
    }
  };

  return (
    <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 hover:shadow-md transition-all flex flex-col h-full group">
      {/* Header: Role & Status */}
      <div className="flex justify-between items-start mb-4">
        <div className={`p-2.5 rounded-2xl ${getRoleColor()}`}>
          {getRoleIcon()}
        </div>
        <span className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wide ${status.color}`}>
          {status.label}
        </span>
      </div>

      {/* Avatar & Name */}
      <div className="flex flex-col items-center mb-6">
        <div className="w-20 h-20 rounded-full p-1 border-2 border-dashed border-gray-200 mb-3 group-hover:border-orange-400 transition-colors">
          <img
            src={staff.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(staff.fullName)}&background=random`}
            alt={staff.fullName}
            className="w-full h-full rounded-full object-cover"
          />
        </div>
        <h3 className="text-lg font-bold text-gray-800 text-center line-clamp-1">{staff.fullName}</h3>
        <span className="text-xs text-gray-500 font-medium">{fmtRole(staff.role)}</span>
      </div>

      {/* Info Details */}
      <div className="space-y-3 text-sm text-gray-600 mb-6 flex-1">
        <div className="flex items-center gap-3 bg-gray-50 p-2.5 rounded-xl">
          <Mail size={16} className="text-gray-400 flex-shrink-0" />
          <span className="truncate" title={staff.email}>{staff.email}</span>
        </div>
        <div className="flex items-center gap-3 bg-gray-50 p-2.5 rounded-xl">
          <Phone size={16} className="text-gray-400 flex-shrink-0" />
          <span>{staff.phone || 'Chưa cập nhật'}</span>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-2 border-t border-gray-100 pt-4 mt-auto">
        <button
          onClick={onEdit}
          className="flex-1 py-2.5 bg-blue-50 text-blue-600 rounded-xl hover:bg-blue-600 hover:text-white transition-all flex items-center justify-center gap-2 font-medium text-sm"
        >
          <Pencil size={16} /> Sửa
        </button>
        <button
          onClick={onDelete}
          className="w-12 py-2.5 bg-red-50 text-red-600 rounded-xl hover:bg-red-600 hover:text-white transition-all flex items-center justify-center"
        >
          <Trash2 size={18} />
        </button>
      </div>
    </div>
  );
};

export default StaffCard;