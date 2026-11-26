import { RefreshCw, UserCheck, Wifi, WifiOff, X } from 'lucide-react';

const ServiceRequestFilters = ({
  copy,
  statusFilter,
  onSelectStatus,
  mineOnly,
  onToggleMineOnly,
  tableIdFilter,
  onClearTableFilter,
  wsConnected,
  isFetching,
  onRefresh,
  summary,
}) => {
  const tabs = [
    { id: 'OPEN', label: copy.filterOpen, count: summary?.openTotal },
    { id: 'PENDING', label: copy.filterPending, count: summary?.pending },
    { id: 'CLAIMED', label: copy.filterClaimed, count: summary?.claimed },
    { id: 'RESOLVED', label: copy.filterResolved },
    { id: 'CANCELLED', label: copy.filterCancelled },
  ];

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200/80 bg-white/95 p-3 shadow-sm dark:border-slate-800 dark:bg-slate-900/95 sm:p-4">
      <div className="flex flex-wrap items-center gap-2">
        {tabs.map((tab) => {
          const isActive = statusFilter === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => onSelectStatus(tab.id)}
              className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-bold transition-all active:scale-98 ${
                isActive
                  ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-600/25'
                  : 'border border-slate-200 bg-slate-50 text-slate-600 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800/70 dark:text-slate-300 dark:hover:bg-slate-800'
              }`}
            >
              <span>{tab.label}</span>
              {typeof tab.count === 'number' && (
                <span
                  className={`rounded-full px-1.5 py-0.5 text-[10px] font-black ${
                    isActive
                      ? 'bg-white/20 text-white'
                      : 'bg-slate-200/80 text-slate-700 dark:bg-slate-700 dark:text-slate-200'
                  }`}
                >
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}

        <button
          type="button"
          onClick={() => onToggleMineOnly(!mineOnly)}
          className={`inline-flex items-center gap-1.5 rounded-xl border px-3 py-2 text-xs font-bold transition-all active:scale-98 ${
            mineOnly
              ? 'border-emerald-500 bg-emerald-50 text-emerald-700 dark:border-emerald-500/60 dark:bg-emerald-950/40 dark:text-emerald-300'
              : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800/60 dark:text-slate-300'
          }`}
        >
          <UserCheck size={14} />
          <span>{copy.mineOnly}</span>
        </button>

        {tableIdFilter != null && (
          <button
            type="button"
            onClick={onClearTableFilter}
            className="inline-flex items-center gap-1 rounded-xl border border-indigo-200 bg-indigo-50 px-2.5 py-1.5 text-xs font-bold text-indigo-700 transition hover:bg-indigo-100 dark:border-indigo-700/60 dark:bg-indigo-950/40 dark:text-indigo-300"
          >
            <span>#{tableIdFilter}</span>
            <X size={13} aria-label={copy.clearingTableFilter} />
          </button>
        )}
      </div>

      <div className="flex items-center gap-2">
        <span
          className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-bold ${
            wsConnected
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-950/40 dark:text-emerald-300'
              : 'border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-500/30 dark:bg-rose-950/40 dark:text-rose-300'
          }`}
        >
          {wsConnected ? <Wifi size={12} /> : <WifiOff size={12} />}
          <span>{wsConnected ? copy.liveConnected : copy.liveDisconnected}</span>
        </span>

        <button
          type="button"
          onClick={onRefresh}
          disabled={isFetching}
          aria-label="Làm mới hàng đợi"
          className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 active:scale-95 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
        >
          <RefreshCw size={14} className={isFetching ? 'animate-spin' : ''} />
        </button>
      </div>
    </div>
  );
};

export default ServiceRequestFilters;
