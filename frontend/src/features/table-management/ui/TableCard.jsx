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
  const isManager = userRole === 'MANAGER'; // Kiểm tra quyền
  const tableTitle = table.displayName || `Bàn ${table.tableNumber}`;
  const memberNumbers = table.memberTableNumbers || [];
  const totalCapacity = table.totalCapacity || table.capacity;

  const status = getTableStatusMeta(table.status);
  const isWaitingPayment = table.status === TABLE_STATUSES.WAITING_FOR_PAYMENT;

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
              className="rounded-lg border border-slate-100 bg-white p-1.5 text-orange-500 shadow-sm transition hover:bg-orange-50 active:bg-orange-100 dark:border-slate-700 dark:bg-slate-900 dark:text-orange-300 dark:hover:bg-orange-500/10 dark:active:bg-orange-500/20"
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
                  className="rounded-lg border border-slate-100 bg-white p-1.5 text-red-500 shadow-sm transition hover:bg-red-50 active:bg-red-100 dark:border-slate-700 dark:bg-slate-900 dark:text-red-300 dark:hover:bg-red-500/10 dark:active:bg-red-500/20"
                  title="Xóa bàn"
                >
                  <Trash2 size={14} />
                </button>
              </>
            )}
          </div>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-slate-500 dark:text-slate-400">
        <div className="flex items-center gap-1">
          <Users size={14} />
          <span>
            {table.guestCount ? `${table.guestCount} khách / ` : ''}
            {totalCapacity} chỗ
          </span>
        </div>
        {table.merged && (
          <div className="flex items-center gap-1 rounded bg-orange-50 px-1.5 font-medium text-orange-600 dark:bg-orange-500/10 dark:text-orange-300">
            <Link2 size={14} /> <span>{memberNumbers.join(' + ')}</span>
          </div>
        )}
        {timeElapsed && (
          <div className="flex items-center gap-1 rounded bg-amber-50 px-1.5 font-medium text-amber-600 dark:bg-amber-500/10 dark:text-amber-300">
            <Clock size={14} /> <span>{timeElapsed}</span>
          </div>
        )}
      </div>

      <div className="mb-4">
        <p className="text-sm text-slate-500 dark:text-slate-400">Tạm tính:</p>
        <p className="text-2xl font-black text-orange-600 dark:text-orange-300">
          {hasOrder ? fmtVND(finalAmount) : fmtVND(0)}
        </p>
      </div>

      <div className="grid min-w-0 grid-cols-1 gap-2 min-[360px]:grid-cols-2">
        <button
          onClick={onDetail}
          className="btn-secondary flex min-w-0 items-center justify-center gap-1 rounded-lg border border-slate-200 bg-white px-2 py-2 text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
        >
          <FileText className="shrink-0" size={16} /> <span className="truncate">Chi tiết</span>
        </button>
        <button
          onClick={onAddItems}
          className="btn-primary flex min-w-0 items-center justify-center gap-1 rounded-lg bg-orange-100 px-2 py-2 text-orange-600 transition hover:bg-orange-200 dark:bg-orange-500/10 dark:text-orange-300 dark:hover:bg-orange-500/20"
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
    </div>
  );
};

export default TableCard;
