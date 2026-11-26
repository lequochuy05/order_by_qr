import React from 'react';
import { Clock, Receipt } from 'lucide-react';
import { getOrderFinalAmount, getOrderStatusMeta } from '@entities/order';
import { fmtTime, fmtVND } from '@shared/lib/formatters.js';

const RecentOrdersCard = ({ recentOrders, isEn, language }) => (
  <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300">
    <div className="flex items-center gap-2 mb-1">
      <span className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400">
        <Clock size={20} />
      </span>
      <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
        {isEn ? 'Recent orders' : 'Đơn hàng mới nhất'}
      </h3>
    </div>
    <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
      {isEn ? 'Latest transactions today' : 'Nhật ký giao dịch phát sinh gần nhất trong ngày'}
    </p>

    <div className="space-y-3">
      {recentOrders.map((order, idx) => {
        const tableNumber = order.table?.tableNumber || order.tableNumber;
        const orderAmount = order.finalAmount ?? getOrderFinalAmount(order);
        const statusMeta = getOrderStatusMeta(order.status, language);

        return (
          <div
            key={order.id || idx}
            className="flex items-center justify-between p-3 rounded-2xl border border-[#1E293B]/20 dark:border-[#1E293B]/60 bg-slate-50/70 dark:bg-slate-800/30 hover:bg-slate-100/70 dark:hover:bg-slate-800 transition-colors"
          >
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 flex items-center justify-center shrink-0">
                <Receipt size={18} />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-slate-900 dark:text-white">
                    #{order.id}
                  </span>
                  <span className="px-1.5 py-0.5 rounded-md text-[10px] font-semibold bg-slate-200/60 dark:bg-slate-700 text-slate-600 dark:text-slate-300">
                    {tableNumber
                      ? isEn
                        ? `Table ${tableNumber}`
                        : `Bàn ${tableNumber}`
                      : isEn
                        ? 'Takeaway'
                        : 'Mang đi'}
                  </span>
                </div>
                <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-0.5">
                  {fmtTime(order.paymentTime || order.createdAt)}
                </p>
              </div>
            </div>

            <div className="text-right shrink-0 flex flex-col items-end gap-1">
              <span
                className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${statusMeta.classes}`}
              >
                {statusMeta.label}
              </span>
              <span className="text-xs font-black text-slate-900 dark:text-white font-mono">
                {fmtVND(orderAmount)}
              </span>
            </div>
          </div>
        );
      })}

      {recentOrders.length === 0 && (
        <div className="py-8 text-center text-slate-400 text-xs">
          {isEn ? 'No orders today yet.' : 'Chưa phát sinh đơn hàng nào hôm nay.'}
        </div>
      )}
    </div>
  </div>
);

export default RecentOrdersCard;
