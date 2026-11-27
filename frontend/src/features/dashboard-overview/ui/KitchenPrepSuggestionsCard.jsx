import React from 'react';
import { ChefHat } from 'lucide-react';
import { getDemandLevel } from '../lib/forecastUtils.js';

const RANK_COLORS = [
  'bg-gradient-to-r from-amber-400 to-amber-600 text-white shadow-amber-500/20',
  'bg-gradient-to-r from-slate-400 to-slate-600 text-white shadow-slate-500/20',
  'bg-gradient-to-r from-amber-700 to-amber-900 text-white shadow-amber-800/20',
];

const KitchenPrepSuggestionsCard = ({ popularDishesForecast, isEn, language }) => (
  <div className="min-w-0 relative rounded-3xl bg-white/90 dark:bg-[#0F172A]/90 backdrop-blur-sm border border-[#1E293B] p-6 sm:p-7 shadow-sm hover:shadow-[0_12px_32px_rgba(30,41,59,0.08)] transition-all duration-300 flex flex-col justify-between">
    <div>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <span className="p-2 rounded-xl bg-amber-50 dark:bg-amber-950/50 text-amber-600 dark:text-amber-400">
            <ChefHat size={20} />
          </span>
          <h3 className="text-lg font-black text-slate-900 dark:text-white tracking-tight">
            {isEn ? 'Prep suggestions' : 'Gợi ý chuẩn bị món'}
          </h3>
        </div>
        <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
          {isEn ? 'Last 30 days' : '30 Ngày qua'}
        </span>
      </div>
      <p className="text-xs text-slate-500 dark:text-slate-400 mb-5">
        {isEn
          ? 'Estimated servings to prep ahead to optimize cook time.'
          : 'Dự kiến số lượng suất ăn cần chuẩn bị trước để tối ưu thời gian ra món.'}
      </p>

      <div className="space-y-3.5">
        {popularDishesForecast.map((dish, idx) => {
          const demand = getDemandLevel(dish.estimatedQty, language);
          const badgeColor =
            RANK_COLORS[idx] ||
            'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300';

          return (
            <div
              key={dish.id || idx}
              className="group rounded-2xl border border-[#1E293B]/20 dark:border-[#1E293B]/60 bg-slate-50/70 dark:bg-slate-800/40 p-3.5 hover:bg-slate-100/70 dark:hover:bg-slate-800 transition-colors"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className={`w-7 h-7 rounded-xl flex items-center justify-center text-xs font-black shadow-sm shrink-0 ${badgeColor}`}
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
                      {isEn ? 'Est. sold:' : 'Dự kiến bán:'}{' '}
                      <span className="font-extrabold text-indigo-600 dark:text-indigo-400">
                        {dish.estimatedQty}
                      </span>{' '}
                      {isEn ? 'servings' : 'suất'}
                    </p>
                  </div>
                </div>

                <span
                  className={`shrink-0 rounded-full px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wider ${demand.className}`}
                >
                  {demand.label}
                </span>
              </div>

              {/* Progress Bar */}
              <div className="mt-3 h-1.5 w-full rounded-full bg-slate-200/70 dark:bg-slate-700 overflow-hidden">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-500"
                  style={{
                    width: `${Math.min(100, (dish.estimatedQty / 30) * 100)}%`,
                  }}
                />
              </div>
            </div>
          );
        })}

        {popularDishesForecast.length === 0 && (
          <div className="py-8 text-center text-slate-400 text-xs">
            {isEn ? 'No dish forecast data for today.' : 'Chưa có dữ liệu dự báo món ăn hôm nay.'}
          </div>
        )}
      </div>
    </div>
  </div>
);

export default KitchenPrepSuggestionsCard;
