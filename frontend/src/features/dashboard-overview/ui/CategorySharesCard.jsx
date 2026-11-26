import React from 'react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { Layers } from 'lucide-react';
import { fmtCompactVND, fmtVND } from '@shared/lib/formatters.js';

const PIE_COLORS = [
  '#6366F1', // Indigo
  '#3B82F6', // Blue
  '#10B981', // Emerald
  '#F59E0B', // Amber
  '#EC4899', // Pink
  '#8B5CF6', // Purple
  '#14B8A6', // Teal
  '#F97316', // Orange
];

const CustomPieTooltip = ({ active, payload }) => {
  if (active && payload && payload.length) {
    const data = payload[0];
    return (
      <div className="rounded-2xl border border-[#1E293B] bg-white/95 px-3.5 py-2.5 shadow-xl backdrop-blur-md dark:border-[#1E293B] dark:bg-slate-900/95">
        <p className="text-xs font-bold text-slate-800 dark:text-slate-200 mb-1">{data.name}</p>
        <p className="text-sm font-extrabold text-indigo-600 dark:text-indigo-400 font-mono">
          {fmtVND(data.value)}
        </p>
      </div>
    );
  }
  return null;
};

const CategorySharesCard = ({ categoryShares, isEn }) => {
  const totalCategoryRevenue = categoryShares.reduce((acc, item) => acc + (item.value || 0), 0);

  return (
    <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300 flex flex-col justify-between">
      <div>
        <div className="flex items-center gap-2 mb-1">
          <span className="p-2 rounded-xl bg-purple-50 dark:bg-purple-950/50 text-purple-600 dark:text-purple-400">
            <Layers size={20} />
          </span>
          <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
            {isEn ? 'Category breakdown' : 'Tỉ trọng danh mục'}
          </h3>
        </div>
        <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
          {isEn
            ? 'Revenue by food category over 7 days'
            : 'Doanh thu phân bổ theo các nhóm món ăn trong 7 ngày'}
        </p>

        <div className="h-56 relative flex items-center justify-center">
          {categoryShares.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={categoryShares}
                  cx="50%"
                  cy="50%"
                  innerRadius={62}
                  outerRadius={82}
                  paddingAngle={4}
                  cornerRadius={6}
                  dataKey="value"
                >
                  {categoryShares.map((entry, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={PIE_COLORS[index % PIE_COLORS.length]}
                      stroke="transparent"
                    />
                  ))}
                </Pie>
                <Tooltip content={<CustomPieTooltip />} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="text-center text-slate-400 text-xs">
              {isEn ? 'No sales data yet' : 'Chưa có thông tin bán hàng'}
            </div>
          )}

          {/* Center Donut Label */}
          {categoryShares.length > 0 && (
            <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
              <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                {isEn ? 'Total' : 'Tổng cộng'}
              </span>
              <span className="text-xs font-black text-slate-800 dark:text-slate-200 font-mono">
                {fmtCompactVND(totalCategoryRevenue)}
              </span>
            </div>
          )}
        </div>

        {/* Custom Category Legend Badges */}
        <div className="mt-4 flex flex-wrap gap-2">
          {categoryShares.map((entry, index) => {
            const percent =
              totalCategoryRevenue > 0
                ? Math.round((entry.value / totalCategoryRevenue) * 100)
                : 0;
            const color = PIE_COLORS[index % PIE_COLORS.length];
            return (
              <div
                key={`legend-${index}`}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-50/80 dark:bg-slate-800/60 border border-[#1E293B]/20 dark:border-[#1E293B] text-xs text-slate-700 dark:text-slate-300"
              >
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: color }}
                />
                <span className="font-medium truncate max-w-[90px]">{entry.name}</span>
                <span className="font-bold text-slate-900 dark:text-white font-mono">
                  {percent}%
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default CategorySharesCard;
