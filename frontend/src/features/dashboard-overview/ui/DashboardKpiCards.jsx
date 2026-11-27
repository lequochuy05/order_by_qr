import React from 'react';
import {
  ArrowUpRight,
  Banknote,
  CheckCircle2,
  ShoppingBag,
  TrendingUp,
  UtensilsCrossed,
} from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const DashboardKpiCards = ({ todayRevenue, todayOrders, tablesContext, avgOrderValue, isEn }) => {
  const occupancyPercent =
    tablesContext.total > 0
      ? Math.round((tablesContext.occupied / tablesContext.total) * 100)
      : 0;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
      {/* KPI 1: Doanh thu hôm nay */}
      <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {isEn ? 'Revenue today' : 'Doanh thu hôm nay'}
            </p>
            <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
              {fmtVND(todayRevenue)}
            </h3>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white flex items-center justify-center shadow-lg shadow-indigo-500/25 group-hover:scale-110 transition-transform duration-300">
            <TrendingUp size={22} />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
          <span className="inline-flex items-center gap-1 font-bold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-2 py-0.5 rounded-md">
            <ArrowUpRight size={12} />
            {isEn ? 'By shift' : 'Theo ca bán'}
          </span>
          <span>{isEn ? 'Revenue since midnight' : 'Doanh thu tính từ 00:00'}</span>
        </div>
      </div>

      {/* KPI 2: Đơn hàng hôm nay */}
      <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {isEn ? 'Orders today' : 'Đơn hàng hôm nay'}
            </p>
            <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
              {todayOrders.total}
            </h3>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-cyan-500 text-white flex items-center justify-center shadow-lg shadow-blue-500/25 group-hover:scale-110 transition-transform duration-300">
            <ShoppingBag size={22} />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
          <span className="inline-flex items-center gap-1 font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded-md">
            <CheckCircle2 size={12} />
            {todayOrders.completed} {isEn ? 'completed' : 'hoàn thành'}
          </span>
          <span>
            {todayOrders.total - todayOrders.completed} {isEn ? 'processing' : 'đang xử lý'}
          </span>
        </div>
      </div>

      {/* KPI 3: Bàn đang hoạt động */}
      <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {isEn ? 'Tables serving' : 'Bàn đang phục vụ'}
            </p>
            <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white tracking-tight">
              {tablesContext.occupied}
              <span className="text-lg font-bold text-slate-400 dark:text-slate-500 ml-1">
                / {tablesContext.total}
              </span>
            </h3>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white flex items-center justify-center shadow-lg shadow-emerald-500/25 group-hover:scale-110 transition-transform duration-300">
            <UtensilsCrossed size={22} />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-2 pt-3 border-t border-[#1E293B]/15 dark:border-[#1E293B]/60 text-xs text-slate-500 dark:text-slate-400">
          <span className="inline-flex items-center gap-1 font-bold text-teal-600 dark:text-teal-400 bg-teal-50 dark:bg-teal-950/40 px-2 py-0.5 rounded-md">
            {occupancyPercent}% {isEn ? 'occupancy' : 'công suất'}
          </span>
          <span>{isEn ? 'Currently occupied' : 'Đang có khách ngồi'}</span>
        </div>
      </div>

      {/* KPI 4: Giá trị TB / đơn */}
      <div className="min-w-0 group relative overflow-hidden rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.12)] hover:-translate-y-1 transition-all duration-300">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <p className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              {isEn ? 'Avg order value' : 'Giá trị TB / đơn'}
            </p>
            <h3 className="text-2xl sm:text-3xl font-black text-slate-900 dark:text-white font-mono tracking-tight">
              {fmtVND(avgOrderValue)}
            </h3>
          </div>
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-amber-500 to-orange-500 text-white flex items-center justify-center shadow-lg shadow-amber-500/25 group-hover:scale-110 transition-transform duration-300">
            <Banknote size={22} />
          </div>
        </div>
        <div className="mt-4 flex items-center gap-2 pt-3 border-t border-slate-100 dark:border-slate-800/60 text-xs text-slate-500 dark:text-slate-400">
          <span className="inline-flex items-center gap-1 font-bold text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-2 py-0.5 rounded-md">
            AOV
          </span>
          <span>{isEn ? 'Customer spend efficiency' : 'Hiệu quả chi tiêu khách hàng'}</span>
        </div>
      </div>
    </div>
  );
};

export default DashboardKpiCards;
