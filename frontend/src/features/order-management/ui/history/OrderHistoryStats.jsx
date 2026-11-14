import { createElement } from 'react';
import { DollarSign, ShoppingBag, TrendingUp } from 'lucide-react';

import { fmtVND } from '@shared/lib/formatters.js';

const StatCard = ({ icon, badgeClass, label, value }) => (
  <div className="flex items-center gap-4 rounded-3xl border border-[#1E293B] bg-white/90 p-5 shadow-sm backdrop-blur-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md dark:border-[#1E293B] dark:bg-[#0F172A]/90">
    <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl ${badgeClass}`}>
      {createElement(icon, { size: 22 })}
    </div>
    <div className="min-w-0 flex-1">
      <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">{label}</p>
      <p className="text-2xl sm:text-3xl font-black font-mono tracking-tight text-slate-900 dark:text-white truncate">
        {value}
      </p>
    </div>
  </div>
);

const OrderHistoryStats = ({ analytics, averageOrder }) => (
  <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
    <StatCard
      icon={ShoppingBag}
      badgeClass="bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25"
      label="Tổng đơn hàng"
      value={analytics.totalOrders?.toLocaleString() || 0}
    />
    <StatCard
      icon={DollarSign}
      badgeClass="bg-gradient-to-tr from-emerald-500 to-teal-500 text-white shadow-lg shadow-emerald-500/25"
      label="Tổng doanh thu"
      value={fmtVND(analytics.totalRevenue || 0)}
    />
    <StatCard
      icon={TrendingUp}
      badgeClass="bg-gradient-to-tr from-amber-500 to-orange-500 text-white shadow-lg shadow-amber-500/25"
      label="TB / Đơn hàng"
      value={fmtVND(averageOrder)}
    />
  </div>
);

export default OrderHistoryStats;
