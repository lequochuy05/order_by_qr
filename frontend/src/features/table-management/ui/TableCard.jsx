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
  const isManager = userRole === 'ADMIN' || userRole === 'MANAGER';
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

  const statusTheme = (() => {
    if (isWaitingPayment) {
      return {
        card: 'border-2 border-amber-500 bg-amber-50/50 dark:bg-amber-950/20 shadow-amber-500/10 ring-2 ring-amber-400/40 animate-pulse',
        badge: 'border border-amber-300/80 bg-amber-100/90 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200 dark:border-amber-700',
      };
    }
    switch (table.status) {
      case TABLE_STATUSES.OCCUPIED:
        return {
          card: 'border border-indigo-400/70 bg-indigo-50/40 dark:bg-indigo-950/25 dark:border-indigo-500/50 shadow-sm shadow-indigo-500/10',
          badge: 'border border-indigo-200/80 bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 dark:border-indigo-800/80',
        };
      case TABLE_STATUSES.INACTIVE:
        return {
          card: 'border border-rose-300/60 bg-rose-50/20 opacity-75 dark:bg-rose-950/20 dark:border-rose-900/40',
          badge: 'border border-rose-200 bg-rose-50 text-rose-700 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
        };
      case TABLE_STATUSES.AVAILABLE:
      default:
        return {
          card: 'border border-[#1E293B] bg-white/95 dark:bg-[#0F172A]/95',
          badge: 'border border-slate-200/80 bg-slate-100 text-slate-600 dark:border-slate-700/80 dark:bg-slate-800 dark:text-slate-300',
        };
    }
  })();

  return (
    <div
      className={`group relative min-w-0 rounded-3xl p-5 backdrop-blur-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-xl ${statusTheme.card}`}
    >
      <div className="mb-3 flex min-w-0 items-start justify-between gap-2">
        <h3 className="min-w-0 text-xl font-black text-slate-900 dark:text-white tracking-tight">
          {tableTitle}
        </h3>
        <div className="flex shrink-0 flex-col items-end gap-1.5">
          <span className={`whitespace-nowrap rounded-lg px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider sm:text-xs ${statusTheme.badge}`}>
            {status.label}
          </span>
          <div className="flex gap-1.5 mt-0.5">
            <button
              type="button"
              onClick={onMerge}
              aria-label={table.merged ? 'Gộp thêm bàn' : 'Gộp bàn'}
              title={table.merged ? 'Gộp thêm bàn' : 'Gộp bàn'}
              className="rounded-xl border border-slate-200/80 bg-white/90 p-1.5 text-indigo-600 shadow-sm transition hover:bg-indigo-50 active:scale-95 dark:border-slate-700 dark:bg-slate-800 dark:text-indigo-400 dark:hover:bg-indigo-950/40"
            >
              <Link2 size={14} />
            </button>
            {isManager && (
              <>
                <button
                  type="button"
                  onClick={onEdit}
                  aria-label="Sửa bàn"
                  className="rounded-xl border border-slate-200/80 bg-white/90 p-1.5 text-slate-600 shadow-sm transition hover:bg-indigo-50 hover:text-indigo-600 active:scale-95 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-indigo-950/40 dark:hover:text-indigo-400"
                  title="Sửa bàn"
                >
                  <Edit size={14} />
                </button>
                <button
                  type="button"
                  onClick={onDelete}
                  aria-label="Xóa bàn"
                  className="rounded-xl border border-slate-200/80 bg-white/90 p-1.5 text-rose-600 shadow-sm transition hover:bg-rose-50 active:scale-95 dark:border-slate-700 dark:bg-slate-800 dark:text-rose-400 dark:hover:bg-rose-950/40"
                  title="Xóa bàn"
                >
                  <Trash2 size={14} />
                </button>
              </>
            )}
          </div>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-xs text-slate-500 dark:text-slate-400">
        <div className="flex items-center gap-1 font-medium">
          <Users size={14} className="text-slate-400" />
          <span>
            {table.guestCount ? `${table.guestCount} khách / ` : ''}
            {totalCapacity} chỗ
          </span>
        </div>
        {table.merged && (
          <div className="flex items-center gap-1 rounded-lg bg-indigo-50 px-2 py-0.5 font-bold text-indigo-700 border border-indigo-200/60 dark:bg-indigo-950/40 dark:text-indigo-400 dark:border-indigo-800/40">
            <Link2 size={13} /> <span>{memberNumbers.join(' + ')}</span>
          </div>
        )}
        {timeElapsed && (
          <div className="flex items-center gap-1 rounded-lg bg-amber-50 px-2 py-0.5 font-bold text-amber-700 border border-amber-200/60 dark:bg-amber-950/40 dark:text-amber-400 dark:border-amber-800/40">
            <Clock size={13} /> <span>{timeElapsed}</span>
          </div>
        )}
      </div>

      <div className="mb-5 rounded-2xl bg-slate-50/80 dark:bg-slate-800/50 p-3 border border-slate-100 dark:border-slate-800/60">
        <p className="text-[10px] font-black uppercase tracking-wider text-slate-400 dark:text-slate-500">Tạm tính</p>
        <p className="text-2xl font-black font-mono tracking-tight text-slate-900 dark:text-white mt-0.5">
          {hasOrder ? fmtVND(finalAmount) : fmtVND(0)}
        </p>
      </div>

      <div className="grid min-w-0 grid-cols-1 gap-2 min-[360px]:grid-cols-2">
        <button
          onClick={onDetail}
          className="flex min-w-0 items-center justify-center gap-1.5 rounded-xl border border-slate-200/80 bg-white px-3 py-2.5 text-xs font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 active:scale-95 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
        >
          <FileText className="shrink-0 text-slate-400" size={15} /> <span className="truncate">Chi tiết</span>
        </button>
        <button
          onClick={onAddItems}
          className="flex min-w-0 items-center justify-center gap-1.5 rounded-xl bg-indigo-50 px-3 py-2.5 text-xs font-bold text-indigo-600 transition hover:bg-indigo-100 active:scale-95 dark:bg-indigo-950/40 dark:text-indigo-400 dark:hover:bg-indigo-900/40"
        >
          <PlusCircle className="shrink-0" size={15} /> <span className="truncate">Thêm món</span>
        </button>
        {hasOrder && (
          <button
            onClick={onPay}
            className="flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 px-3 py-2.5 text-xs font-black uppercase tracking-wider text-white shadow-md shadow-emerald-600/25 hover:from-emerald-500 hover:to-teal-500 active:scale-95 min-[360px]:col-span-2 transition-all"
          >
            <CreditCard size={15} /> Thanh toán
          </button>
        )}
      </div>
    </div>
  );
};

export default TableCard;
