import React from 'react';
import { ChefHat } from 'lucide-react';
import KitchenCard from './KitchenCard';

const toneClasses = {
  orange: {
    column: 'border-amber-200/80 bg-amber-50/40 dark:border-amber-900/40 dark:bg-amber-950/20',
    icon: 'bg-gradient-to-tr from-amber-500 to-orange-500 text-white shadow-md shadow-amber-500/25',
    count: 'bg-amber-100 font-mono text-amber-800 dark:bg-amber-950/60 dark:text-amber-300',
  },
  blue: {
    column: 'border-indigo-200/80 bg-indigo-50/40 dark:border-indigo-900/40 dark:bg-indigo-950/20',
    icon: 'bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-md shadow-indigo-500/25',
    count: 'bg-indigo-100 font-mono text-indigo-800 dark:bg-indigo-950/60 dark:text-indigo-300',
  },
  green: {
    column: 'border-emerald-200/80 bg-emerald-50/40 dark:border-emerald-900/40 dark:bg-emerald-950/20',
    icon: 'bg-gradient-to-tr from-emerald-500 to-teal-500 text-white shadow-md shadow-emerald-500/25',
    count: 'bg-emerald-100 font-mono text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300',
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
  onCancelItem,
  processingItems,
}) => {
  const classes = toneClasses[tone];
  const overdueCount = items.filter((item) => item.isOverdue).length;

  return (
    <section
      className={`flex min-h-[520px] flex-col overflow-hidden rounded-3xl border ${classes.column}`}
    >
      <header className="sticky top-0 z-10 border-b border-white/80 bg-white/90 px-5 py-4 backdrop-blur dark:border-slate-800 dark:bg-slate-900/90">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3.5">
            <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl ${classes.icon}`}>
              {React.createElement(icon, { size: 20 })}
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-black text-slate-800 dark:text-slate-100">{title}</h2>
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-black ${classes.count}`}>
                  {items.length}
                </span>
              </div>
              <p className="mt-0.5 text-xs font-medium text-slate-500 dark:text-slate-400">{subtitle}</p>
            </div>
          </div>

          {overdueCount > 0 && (
            <span className="rounded-full bg-rose-100 px-2.5 py-1 text-[11px] font-black text-rose-600 dark:bg-rose-500/10 dark:text-rose-300">
              {overdueCount} trễ
            </span>
          )}
        </div>
      </header>

      <div className="flex-1 space-y-3 overflow-y-auto p-3.5">
        {items.length === 0 ? (
          <div className="flex min-h-64 flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white/50 px-4 text-center dark:border-slate-800 dark:bg-slate-900/40">
            <span className={`mb-3 flex h-12 w-12 items-center justify-center rounded-2xl ${classes.icon}`}>
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
              onCancelItem={onCancelItem}
              isProcessing={processingItems.has(item.id)}
            />
          ))
        )}
      </div>
    </section>
  );
};

export default KitchenColumn;
