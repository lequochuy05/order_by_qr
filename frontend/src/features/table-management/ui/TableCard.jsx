import React from 'react';
import { Users, FileText, PlusCircle, CreditCard, Edit, Trash2, Lock } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import { getTableStatusMeta } from '@shared/lib/tableStatus.js';
import { getOrderFinalAmount } from '@shared/lib/orderMoney.js';

const TableCard = ({ table, order, onDetail, onAddItems, onPay, onEdit, onDelete, userRole }) => {
  const finalAmount = getOrderFinalAmount(order);
  const hasOrder = order && finalAmount > 0;
  const isManager = userRole === 'MANAGER'; // Kiểm tra quyền

  const status = getTableStatusMeta(table.status);

  return (
    <div
      className={`group relative min-w-0 rounded-2xl border-2 p-4 transition-all hover:shadow-lg ${status.classes}`}
    >
      {/* Header và Nội dung chính giữ nguyên */}
      <div className="mb-2 flex min-w-0 items-start justify-between gap-2">
        <h3 className="min-w-0 text-xl font-bold text-gray-800">Bàn {table.tableNumber}</h3>
        <span className="shrink-0 whitespace-nowrap rounded border bg-white/50 px-2 py-1 text-[10px] font-bold uppercase sm:text-xs">
          {status.label}
        </span>
      </div>

      <div className="flex items-center gap-2 text-gray-500 text-sm mb-4">
        <Users size={16} /> <span>{table.capacity} khách</span>
      </div>

      <div className="mb-4">
        <p className="text-sm text-gray-500">Tạm tính:</p>
        <p className="text-2xl font-black text-orange-600">
          {hasOrder ? fmtVND(finalAmount) : fmtVND(0)}
        </p>
      </div>

      <div className="grid min-w-0 grid-cols-1 gap-2 min-[360px]:grid-cols-2">
        <button
          onClick={onDetail}
          className="btn-secondary flex min-w-0 items-center justify-center gap-1 rounded-lg border bg-white px-2 py-2 hover:bg-gray-50"
        >
          <FileText className="shrink-0" size={16} /> <span className="truncate">Chi tiết</span>
        </button>
        <button
          onClick={onAddItems}
          className="btn-primary flex min-w-0 items-center justify-center gap-1 rounded-lg bg-orange-100 px-2 py-2 text-orange-600 hover:bg-orange-200"
        >
          <PlusCircle className="shrink-0" size={16} /> <span className="truncate">Thêm món</span>
        </button>

        {hasOrder && (
          <button
            onClick={onPay}
            className="btn-pay flex items-center justify-center gap-1 rounded-lg bg-green-500 px-2 py-2 text-white shadow-md hover:bg-green-600 min-[360px]:col-span-2"
          >
            <CreditCard size={16} /> Thanh toán
          </button>
        )}
      </div>

      {/* */}
      <div className="absolute top-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          onClick={onEdit}
          className="p-1.5 bg-white rounded-lg shadow-sm text-blue-500 hover:bg-blue-50 border border-gray-100"
          title="Sửa bàn"
        >
          {isManager ? <Edit size={14} /> : <Lock size={14} className="text-gray-400" />}
        </button>
        <button
          onClick={onDelete}
          className="p-1.5 bg-white rounded-lg shadow-sm text-red-500 hover:bg-red-50 border border-gray-100"
          title="Xóa bàn"
        >
          {isManager ? <Trash2 size={14} /> : <Lock size={14} className="text-gray-400" />}
        </button>
      </div>
    </div>
  );
};

export default TableCard;
