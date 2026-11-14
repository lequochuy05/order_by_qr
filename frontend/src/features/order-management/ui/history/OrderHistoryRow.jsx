import { Eye, Printer, RotateCw } from 'lucide-react';

import { getOrderStatusMeta, ORDER_STATUSES } from '@entities/order/lib/orderStatus.js';
import { getOrderFinalAmount } from '@entities/order/lib/orderMoney.js';
import { fmtDateTime, fmtVND } from '@shared/lib/formatters.js';

const OrderHistoryRow = ({ order, onSelect, onPrint, onReconcile }) => (
  <tr
    className="group cursor-pointer border-b border-slate-100 transition-colors hover:bg-indigo-50/30 dark:border-slate-800/60 dark:hover:bg-slate-800/40"
    onClick={() => onSelect(order)}
  >
    <td className="p-5 font-mono font-bold text-slate-900 dark:text-white">
      <span className="mr-1 text-indigo-500">#</span>
      {order.id?.toString().substring(0, 8).toUpperCase()}
    </td>
    <td className="p-5 text-xs text-slate-500 dark:text-slate-400 font-medium">
      {fmtDateTime(order.createdAt)}
    </td>
    <td className="p-5">
      {order.table?.tableNumber || order.table?.name ? (
        <span className="rounded-xl border border-slate-200/80 bg-slate-100 px-3 py-1 text-xs font-bold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200">
          {order.table.tableNumber || order.table.name}
        </span>
      ) : (
        <span className="italic text-xs text-slate-400 dark:text-slate-500">Mang đi</span>
      )}
    </td>
    <td className="p-5 font-mono font-black text-slate-900 dark:text-white">
      {fmtVND(getOrderFinalAmount(order))}
    </td>
    <td className="p-5">
      <span
        className={`rounded-full border px-3 py-1 text-[11px] font-bold ${
          getOrderStatusMeta(order.status).classes
        }`}
      >
        {getOrderStatusMeta(order.status).label}
      </span>
    </td>
    <td className="p-5 text-right">
      <div className="flex justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
        <button
          type="button"
          className="flex h-9 w-9 items-center justify-center rounded-xl text-slate-400 transition-all hover:bg-indigo-50 hover:text-indigo-600 active:scale-95 dark:text-slate-500 dark:hover:bg-indigo-950/40 dark:hover:text-indigo-400"
          title="Tra soát thanh toán"
          onClick={() => onReconcile(order.id)}
        >
          <RotateCw size={16} />
        </button>
        {order.status === ORDER_STATUSES.COMPLETED && (
          <button
            type="button"
            className="flex h-9 w-9 items-center justify-center rounded-xl text-slate-400 transition-all hover:bg-emerald-50 hover:text-emerald-600 active:scale-95 dark:text-slate-500 dark:hover:bg-emerald-950/40 dark:hover:text-emerald-400"
            title="In hóa đơn"
            onClick={() => onPrint(order)}
          >
            <Printer size={16} />
          </button>
        )}
        <button
          type="button"
          className="flex h-9 w-9 items-center justify-center rounded-xl text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-900 active:scale-95 dark:text-slate-500 dark:hover:bg-slate-800 dark:hover:text-white"
          title="Xem chi tiết"
          onClick={() => onSelect(order)}
        >
          <Eye size={16} />
        </button>
      </div>
    </td>
  </tr>
);

export default OrderHistoryRow;
