import React from 'react';
import { ChefHat } from 'lucide-react';
import KitchenCard from './KitchenCard';

const toneClasses = {
  orange: {
    column: 'border-orange-100 bg-orange-50/50 dark:border-orange-500/10 dark:bg-orange-500/5',
    icon: 'bg-orange-100 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300',
    count: 'bg-orange-100 text-orange-700 dark:bg-orange-500/10 dark:text-orange-300',
  },
  blue: {
    column: 'border-blue-100 bg-blue-50/50 dark:border-blue-500/10 dark:bg-blue-500/5',
    icon: 'bg-blue-100 text-blue-600 dark:bg-blue-500/10 dark:text-blue-300',
    count: 'bg-blue-100 text-blue-700 dark:bg-blue-500/10 dark:text-blue-300',
  },
  green: {
    column: 'border-emerald-100 bg-emerald-50/50 dark:border-emerald-500/10 dark:bg-emerald-500/5',
    icon: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300',
    count: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300',
  },
};

const KitchenColumn = ({
  title,
  subtitle,
  status,
  items,
  icon,
  tone,
  onUpdateStatus,
  processingItems,
}) => {
  const classes = toneClasses[tone];
  const overdueCount = items.filter((item) => item.isOverdue).length;

  return (
    <section
      className={`flex min-h-[520px] flex-col overflow-hidden rounded-2xl border ${classes.column}`}
    >
      <header className="sticky top-0 z-10 border-b border-white/80 bg-white/90 px-4 py-4 backdrop-blur dark:border-slate-800 dark:bg-slate-900/90">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <span className={`rounded-xl p-2.5 ${classes.icon}`}>
              {React.createElement(icon, { size: 20 })}
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-black text-slate-800 dark:text-slate-100">{title}</h2>
                <span className={`rounded-full px-2 py-0.5 text-xs font-black ${classes.count}`}>
                  {items.length}
                </span>
              </div>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{subtitle}</p>
            </div>
          </div>

          {overdueCount > 0 && (
            <span className="rounded-full bg-red-100 px-2 py-1 text-[11px] font-black text-red-600 dark:bg-red-500/10 dark:text-red-300">
              {overdueCount} trễ
            </span>
          )}
        </div>
      </header>

      <div className="flex-1 space-y-3 overflow-y-auto p-3">
        {items.length === 0 ? (
          <div className="flex min-h-64 flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white/50 px-4 text-center dark:border-slate-700 dark:bg-slate-900/40">
            <span className={`mb-3 rounded-2xl p-3 ${classes.icon}`}>
              <ChefHat size={24} />
            </span>
            <p className="text-sm font-bold text-slate-600 dark:text-slate-300">
              Không có món trong cột này
            </p>
            <p className="mt-1 text-xs text-slate-400">Danh sách sẽ tự cập nhật khi có thay đổi.</p>
          </div>
        ) : (
          items.map((item) => (
            <KitchenCard
              key={item.id}
              item={item}
              status={status}
              onUpdateStatus={onUpdateStatus}
              isProcessing={processingItems.has(item.id)}
            />
          ))
        )}
      </div>
    </section>
  );
};

export default KitchenColumn;
