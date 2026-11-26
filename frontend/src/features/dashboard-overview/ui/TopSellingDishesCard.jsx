import React from 'react';
import { UtensilsCrossed } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const RANK_STYLES = [
  'bg-gradient-to-tr from-amber-400 to-amber-600 text-white shadow-amber-500/25',
  'bg-gradient-to-tr from-slate-400 to-slate-600 text-white shadow-slate-500/25',
  'bg-gradient-to-tr from-amber-700 to-amber-900 text-white shadow-amber-800/25',
];

const TopSellingDishesCard = ({ topDishes, isEn }) => {
  const maxTopDishRevenue = topDishes[0]?.totalRevenue || 1;

  return (
    <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300">
      <div className="flex items-center gap-2 mb-1">
        <span className="p-2 rounded-xl bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400">
          <UtensilsCrossed size={20} />
        </span>
        <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
          {isEn ? 'Top selling dishes' : 'Top món bán chạy'}
        </h3>
      </div>
      <p className="text-xs text-slate-500 dark:text-slate-400 mb-6">
        {isEn
          ? 'Top 5 by quantity and revenue in the last 7 days'
          : '5 món có số lượng và doanh thu cao nhất 7 ngày qua'}
      </p>

      <div className="space-y-3.5">
        {topDishes.map((dish, idx) => {
          const rankClass =
            RANK_STYLES[idx] ||
            'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300';
          const barPercent = Math.min(
            100,
            Math.round((dish.totalRevenue / maxTopDishRevenue) * 100),
          );

          return (
            <div
              key={idx}
              className="rounded-2xl border border-[#1E293B]/20 dark:border-[#1E293B]/60 bg-slate-50/70 dark:bg-slate-800/30 p-3.5 hover:bg-slate-100/70 dark:hover:bg-slate-800 transition-colors"
            >
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-black shadow-sm shrink-0 ${rankClass}`}
                  >
                    {idx + 1}
                  </div>
                  <div className="min-w-0">
                    <p
                      className="font-bold text-sm text-slate-900 dark:text-white truncate"
                      title={dish.name}
                    >
                      {dish.name}
                    </p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {isEn ? 'Sold:' : 'Đã bán:'}{' '}
                      <span className="font-extrabold text-slate-800 dark:text-slate-200">
                        {dish.totalQty}
                      </span>{' '}
                      {isEn ? 'servings' : 'phần'}
                    </p>
                  </div>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-sm font-black text-indigo-600 dark:text-indigo-400 font-mono">
                    {fmtVND(dish.totalRevenue)}
                  </p>
                </div>
              </div>

              {/* Volume Ratio Bar */}
              <div className="mt-2.5 h-1 w-full rounded-full bg-slate-200/60 dark:bg-slate-700/60 overflow-hidden">
                <div
                  className="h-full rounded-full bg-indigo-500/80"
                  style={{ width: `${barPercent}%` }}
                />
              </div>
            </div>
          );
        })}

        {topDishes.length === 0 && (
          <div className="py-8 text-center text-slate-400 text-xs">
            {isEn ? 'No dishes recorded yet.' : 'Chưa có món nào được ghi nhận bán ra.'}
          </div>
        )}
      </div>
    </div>
  );
};

export default TopSellingDishesCard;
