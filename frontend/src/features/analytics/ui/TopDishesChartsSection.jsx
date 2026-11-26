import React from 'react';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { BarChart3, TrendingUp, UtensilsCrossed } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const CustomBarTooltip = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="rounded-2xl border border-slate-200/80 bg-white/95 p-3.5 shadow-xl backdrop-blur-md dark:border-slate-700/80 dark:bg-slate-900/95">
        <p className="text-xs font-bold text-slate-900 dark:text-white mb-2">{data.fullName}</p>
        <div className="space-y-1 text-xs">
          <div className="flex items-center justify-between gap-4">
            <span className="text-slate-500 dark:text-slate-400">Số lượng bán:</span>
            <span className="font-extrabold text-indigo-600 dark:text-indigo-400 font-mono">
              {data.quantity.toLocaleString()} suất
            </span>
          </div>
          <div className="flex items-center justify-between gap-4">
            <span className="text-slate-500 dark:text-slate-400">Doanh thu:</span>
            <span className="font-extrabold text-slate-900 dark:text-slate-100 font-mono">
              {fmtVND(data.revenue)}
            </span>
          </div>
        </div>
      </div>
    );
  }
  return null;
};

const CustomTrendTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    const data = payload[0];
    return (
      <div className="rounded-2xl border border-slate-200/80 bg-white/95 px-3.5 py-2.5 shadow-xl backdrop-blur-md dark:border-slate-700/80 dark:bg-slate-900/95">
        <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 mb-1">
          {data.payload?.date || label}
        </p>
        <div className="flex items-center gap-2 text-xs">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
          <span className="text-slate-600 dark:text-slate-300 font-medium">Tổng bán ra:</span>
          <span className="font-bold text-slate-900 dark:text-white font-mono">
            {Number(data.value).toLocaleString()} suất
          </span>
        </div>
      </div>
    );
  }
  return null;
};

const EmptyChartState = () => (
  <div className="h-full flex flex-col items-center justify-center text-slate-400 dark:text-slate-500 py-12">
    <UtensilsCrossed size={48} className="opacity-20 mb-3" />
    <p className="text-sm font-semibold">Chưa có dữ liệu thống kê món ăn</p>
  </div>
);

const TopDishesChartsSection = ({ barData, trendData }) => (
  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
    {/* Horizontal BarChart - Top 10 Best Sellers */}
    <div className="min-w-0 rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm">
      <div className="flex items-center gap-2 mb-1">
        <span className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400">
          <BarChart3 size={20} />
        </span>
        <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
          Top 10 món bán chạy nhất
        </h3>
      </div>
      <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
        Xếp hạng 10 món ăn có sản lượng phục vụ cao nhất trong kỳ
      </p>

      <div className="h-[380px] w-full">
        {barData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={barData} layout="vertical" margin={{ left: 5, right: 20 }}>
              <CartesianGrid
                strokeDasharray="3 3"
                horizontal={false}
                stroke="#E2E8F0"
                opacity={0.5}
              />
              <XAxis
                type="number"
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#64748B', fontSize: 11 }}
              />
              <YAxis
                type="category"
                dataKey="name"
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#334155', fontSize: 12, fontWeight: 600 }}
                width={125}
              />
              <RechartsTooltip
                content={<CustomBarTooltip />}
                cursor={{ fill: 'rgba(99, 102, 241, 0.05)' }}
              />
              <Bar dataKey="quantity" fill="#6366F1" radius={[0, 8, 8, 0]} barSize={20} />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <EmptyChartState />
        )}
      </div>
    </div>

    {/* AreaChart - Daily Trend */}
    <div className="min-w-0 rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm">
      <div className="flex items-center gap-2 mb-1">
        <span className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400">
          <TrendingUp size={20} />
        </span>
        <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
          Xu hướng gọi món theo ngày
        </h3>
      </div>
      <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
        Tổng số suất ăn phục vụ qua từng mốc thời gian
      </p>

      <div className="h-[380px] w-full">
        {trendData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={trendData} margin={{ top: 10, right: 10, bottom: 5, left: 0 }}>
              <defs>
                <linearGradient id="trendGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10B981" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid
                strokeDasharray="3 3"
                vertical={false}
                stroke="#E2E8F0"
                opacity={0.5}
              />
              <XAxis
                dataKey="date"
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#64748B', fontSize: 11 }}
                dy={6}
              />
              <YAxis
                axisLine={false}
                tickLine={false}
                tick={{ fill: '#64748B', fontSize: 11 }}
                allowDecimals={false}
                width={40}
              />
              <RechartsTooltip content={<CustomTrendTooltip />} />
              <Area
                type="monotone"
                dataKey="quantity"
                stroke="#10B981"
                fill="url(#trendGrad)"
                strokeWidth={3}
                activeDot={{ r: 6, fill: '#10B981', stroke: '#fff', strokeWidth: 2 }}
              />
            </AreaChart>
          </ResponsiveContainer>
        ) : (
          <EmptyChartState />
        )}
      </div>
    </div>
  </div>
);

export default TopDishesChartsSection;
