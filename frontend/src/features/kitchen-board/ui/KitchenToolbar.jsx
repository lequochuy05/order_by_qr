import { Ban, Layers, LayoutGrid, RefreshCw, Sun, Volume2, VolumeX } from 'lucide-react';
import { useTranslation } from '@shared/lib/i18n';

const KitchenToolbar = ({
  wakeLock,
  soundEnabled,
  onToggleSound,
  refreshing,
  onRefresh,
  onOpenQuick86,
  viewMode = 'kanban',
  onViewModeChange,
  batchCount = 0,
}) => {
  const { isEn } = useTranslation('kitchen');

  return (
  <div className="flex flex-wrap items-center gap-2">
    {onViewModeChange && (
      <div className="inline-flex rounded-2xl border border-slate-200/80 bg-slate-100/80 p-1 dark:border-slate-800 dark:bg-slate-800/60">
        <button
          type="button"
          onClick={() => onViewModeChange('kanban')}
          className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-bold transition-all ${
            viewMode === 'kanban'
              ? 'bg-white text-indigo-600 shadow-sm dark:bg-slate-900 dark:text-indigo-400'
              : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
          }`}
        >
          <LayoutGrid size={15} />
          {isEn ? 'By Progress' : 'Theo tiến độ'}
        </button>
        <button
          type="button"
          onClick={() => onViewModeChange('batch')}
          className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-bold transition-all ${
            viewMode === 'batch'
              ? 'bg-white text-indigo-600 shadow-sm dark:bg-slate-900 dark:text-indigo-400'
              : 'text-slate-500 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
          }`}
        >
          <Layers size={15} />
          {isEn ? 'Batch Cooking' : 'Gom món nấu'}
          {batchCount > 0 && (
            <span className="rounded-full bg-gradient-to-r from-indigo-600 to-violet-600 px-1.5 py-0.5 text-[10px] font-black font-mono text-white shadow-sm">
              {batchCount}
            </span>
          )}
        </button>
      </div>
    )}

    <button
      type="button"
      onClick={wakeLock.requestWakeLock}
      disabled={!wakeLock.isSupported}
      className={`inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold transition ${
        wakeLock.isActive
          ? 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300'
          : wakeLock.isSupported
            ? 'border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-300'
            : 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400 dark:border-slate-700 dark:bg-slate-800'
      }`}
      title={
        wakeLock.isSupported
          ? (isEn ? 'Keep kitchen screen awake' : 'Giữ màn hình bếp không tự tắt')
          : (isEn ? 'Screen Wake Lock not supported' : 'Thiết bị này không hỗ trợ Screen Wake Lock')
      }
    >
      <Sun size={17} className={wakeLock.isActive ? 'animate-pulse' : ''} />
      {wakeLock.isActive
        ? ''
        : wakeLock.isSupported
          ? (isEn ? 'Keep awake' : 'Bật giữ màn hình')
          : (isEn ? 'No Wake Lock' : 'Không hỗ trợ Wake Lock')}
    </button>
    <button
      type="button"
      onClick={onToggleSound}
      className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100 px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-200 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
      title={isEn ? 'Toggle sound notification' : 'Bật hoặc tắt âm thanh thông báo'}
    >
      {soundEnabled ? <Volume2 size={17} /> : <VolumeX size={17} />}
    </button>
    {onOpenQuick86 && (
      <button
        type="button"
        onClick={onOpenQuick86}
        className="inline-flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2 text-sm font-bold text-rose-700 transition hover:bg-rose-100 active:scale-95 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300 dark:hover:bg-rose-500/25"
        title={isEn ? 'Quickly toggle out-of-stock dishes and combos' : 'Khóa nhanh các món hoặc combo đã hết hàng'}
      >
        <Ban size={16} />
        {isEn ? 'Out of Stock' : 'Báo hết món'}
      </button>
    )}
    <button
      type="button"
      onClick={onRefresh}
      disabled={refreshing}
      className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2 text-sm font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:cursor-not-allowed disabled:opacity-70"
    >
      <RefreshCw size={17} className={refreshing ? 'animate-spin' : ''} />
      {isEn ? 'Refresh' : 'Làm mới'}
    </button>
  </div>
);
};

export default KitchenToolbar;
