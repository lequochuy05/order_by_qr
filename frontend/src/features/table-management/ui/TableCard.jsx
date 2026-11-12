import React, { useState, useEffect } from 'react';
import { Users, FileText, PlusCircle, CreditCard, Edit, Trash2, Clock, Link2 } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import { getTableStatusMeta, TABLE_STATUSES } from '@entities/table/lib/tableStatus.js';

const TableCard = ({
  table,
  order,
  onDetail,
  onAddItems,
  onPay,
  onEdit,
  onDelete,
  onMerge,
  userRole,
}) => {
  const finalAmount = getOrderFinalAmount(order);
  const hasOrder = order && finalAmount > 0;
  const isManager = userRole === 'ADMIN' || userRole === 'MANAGER'; // Cho phép cả Admin và Manager sửa/xóa bàn
  const tableTitle = table.displayName || `Bàn ${table.tableNumber}`;
  const memberNumbers = table.memberTableNumbers || [];
  const totalCapacity = table.totalCapacity || table.capacity;

  const status = getTableStatusMeta(table.status);
  const isWaitingPayment =
    table.status === TABLE_STATUSES.WAITING_FOR_PAYMENT ||
    order?.status === 'AWAITING_PAYMENT';

  // Tính toán thời gian đã ngồi, tự động cập nhật mỗi 30s
  const calcTime = (createdAt) => {
    if (!createdAt) return null;
    const start = new Date(createdAt);
    const now = new Date();
    const diffMs = now - start;
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 60) return `${diffMins} phút`;
    const hours = Math.floor(diffMins / 60);
    return `${hours}h ${diffMins % 60}p`;
  };
  const [timeElapsed, setTimeElapsed] = useState(() => calcTime(order?.createdAt));
  useEffect(() => {
    const refreshTimer = setTimeout(() => {
      setTimeElapsed(calcTime(order?.createdAt));
    }, 0);
    if (!order?.createdAt) {
      return () => clearTimeout(refreshTimer);
    }
    const interval = setInterval(() => {
      setTimeElapsed(calcTime(order?.createdAt));
    }, 30_000);
    return () => {
      clearTimeout(refreshTimer);
      clearInterval(interval);
    };
  }, [order?.createdAt]);

  return (
    <div
      className={`group relative min-w-0 rounded-2xl border-2 p-4 transition-all hover:shadow-lg dark:bg-slate-900 dark:hover:shadow-slate-900/40
      ${status.classes} 
      ${isWaitingPayment ? 'animate-pulse bg-amber-50/50 ring-2 ring-amber-400 ring-offset-2 dark:bg-amber-500/10 dark:ring-offset-slate-950' : ''}
      `}
      style={isWaitingPayment ? { animationDuration: '2s' } : {}}
    >
      <div className="mb-2 flex min-w-0 items-start justify-between gap-2">
        <h3 className="min-w-0 text-xl font-bold text-slate-800 dark:text-slate-100">
          {tableTitle}
        </h3>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          <span className="whitespace-nowrap rounded border border-white/60 bg-white/50 px-2 py-1 text-[10px] font-bold uppercase dark:border-slate-700 dark:bg-slate-900/60 sm:text-xs">
            {status.label}
          </span>
          <div className="flex gap-1">
            <button
              type="button"
              onClick={onMerge}
              aria-label={table.merged ? 'Gộp thêm bàn' : 'Gộp bàn'}
              title={table.merged ? 'Gộp thêm bàn' : 'Gộp bàn'}
              className="rounded-lg border border-slate-100 bg-white p-1.5 text-[#C17C4A] shadow-sm transition hover:bg-[#F7F0E7] active:bg-[#EAE0D5] dark:border-slate-700 dark:bg-slate-900 dark:text-[#D8986A] dark:hover:bg-[#C17C4A]/10 dark:active:bg-[#C17C4A]/20"
            >
              <Link2 size={14} />
            </button>
            {isManager && (
              <>
                <button
                  type="button"
                  onClick={onEdit}
                  aria-label="Sửa bàn"
                  className="rounded-lg border border-slate-100 bg-white p-1.5 text-blue-500 shadow-sm transition hover:bg-blue-50 active:bg-blue-100 dark:border-slate-700 dark:bg-slate-900 dark:text-blue-300 dark:hover:bg-blue-500/10 dark:active:bg-blue-500/20"
                  title="Sửa bàn"
                >
                  <Edit size={14} />
                </button>
                <button
                  type="button"
                  onClick={onDelete}
                  aria-label="Xóa bàn"
                  className="rounded-lg border border-slate-100 bg-white p-1.5 text-rose-500 shadow-sm transition hover:bg-rose-50 active:bg-rose-100 dark:border-slate-700 dark:bg-slate-900 dark:text-rose-300 dark:hover:bg-rose-500/10 dark:active:bg-rose-500/20"
                  title="Xóa bàn"
                >
                  <Trash2 size={14} />
                </button>
              </>
            )}
          </div>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-[#6B5D55] dark:text-[#BDB0A4]">
        <div className="flex items-center gap-1">
          <Users size={14} />
          <span>
            {table.guestCount ? `${table.guestCount} khách / ` : ''}
            {totalCapacity} chỗ
          </span>
        </div>
        {table.merged && (
          <div className="flex items-center gap-1 rounded bg-[#F7F0E7] px-1.5 font-bold text-[#C17C4A] border border-[#C17C4A]/30 dark:bg-[#2A221E] dark:text-[#D8986A]">
            <Link2 size={14} /> <span>{memberNumbers.join(' + ')}</span>
          </div>
        )}
        {timeElapsed && (
          <div className="flex items-center gap-1 rounded bg-amber-50 px-1.5 font-semibold text-amber-700 border border-amber-200/50 dark:bg-amber-500/10 dark:text-amber-300">
            <Clock size={14} /> <span>{timeElapsed}</span>
          </div>
        )}
      </div>

      <div className="mb-4">
        <p className="text-xs font-bold uppercase tracking-wider text-[#9C8E85] dark:text-[#82756B]">Tạm tính:</p>
        <p className="text-2xl font-black text-[#56372B] dark:text-[#D8986A]">
          {hasOrder ? fmtVND(finalAmount) : fmtVND(0)}
        </p>
      </div>

      <div className="grid min-w-0 grid-cols-1 gap-2 min-[360px]:grid-cols-2">
        <button
          onClick={onDetail}
          className="btn-secondary flex min-w-0 items-center justify-center gap-1 rounded-xl border border-[#E8DCD1] bg-white px-2 py-2 text-xs font-bold text-[#56372B] transition hover:bg-[#F7F0E7] dark:border-[#352C26] dark:bg-[#1F1A17] dark:text-[#F7F0E7] dark:hover:bg-[#2A221E]"
        >
          <FileText className="shrink-0" size={16} /> <span className="truncate">Chi tiết</span>
        </button>
        <button
          onClick={onAddItems}
          className="btn-primary flex min-w-0 items-center justify-center gap-1 rounded-xl bg-[#F7F0E7] px-2 py-2 text-xs font-bold text-[#56372B] transition hover:bg-[#EAE0D5] dark:bg-[#2A221E] dark:text-[#D8986A] dark:hover:bg-[#352C26]"
        >
          <PlusCircle className="shrink-0" size={16} /> <span className="truncate">Thêm món</span>
        </button>
        {hasOrder && (
          <button
            onClick={onPay}
            className="btn-pay flex items-center justify-center gap-1 rounded-xl bg-emerald-600 px-2 py-2.5 text-xs font-black uppercase tracking-wider text-white shadow-md shadow-emerald-600/20 hover:bg-emerald-700 active:scale-98 min-[360px]:col-span-2"
          >
            <CreditCard size={16} /> Thanh toán
          </button>
        )}
      </div>
    </div>
  );
};

export default TableCard;
