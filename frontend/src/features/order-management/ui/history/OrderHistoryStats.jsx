import { createElement } from 'react';
import { DollarSign, ShoppingBag, TrendingUp } from 'lucide-react';

import { fmtVND } from '@shared/lib/formatters.js';

const StatCard = ({ icon, iconClass, iconBackground, label, value }) => (
  <div className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5 shadow-sm">
    <div className={`rounded-xl p-3 ${iconBackground}`}>
      {createElement(icon, { size: 22, className: iconClass })}
    </div>
    <div>
      <p className="text-xs font-medium uppercase tracking-wider text-gray-500">{label}</p>
      <p className="text-2xl font-bold text-gray-800">{value}</p>
    </div>
  </div>
);

const OrderHistoryStats = ({ analytics, averageOrder }) => (
  <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
    <StatCard
      icon={ShoppingBag}
      iconBackground="bg-blue-50"
      iconClass="text-blue-500"
      label="Tổng đơn hàng"
      value={analytics.totalOrders?.toLocaleString() || 0}
    />
    <StatCard
      icon={DollarSign}
      iconBackground="bg-green-50"
      iconClass="text-green-500"
      label="Tổng doanh thu"
      value={fmtVND(analytics.totalRevenue || 0)}
    />
    <StatCard
      icon={TrendingUp}
      iconBackground="bg-orange-50"
      iconClass="text-orange-500"
      label="TB/Đơn"
      value={fmtVND(averageOrder)}
    />
  </div>
);

export default OrderHistoryStats;
