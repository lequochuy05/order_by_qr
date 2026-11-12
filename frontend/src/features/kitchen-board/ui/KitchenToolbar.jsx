import { Layers, LayoutGrid, RefreshCw, Sun, Volume2, VolumeX } from 'lucide-react';

const KitchenToolbar = ({
  wakeLock,
  soundEnabled,
  onToggleSound,
  refreshing,
  onRefresh,
  viewMode = 'kanban',
  onViewModeChange,
  batchCount = 0,
}) => (
  <div className="flex flex-wrap items-center gap-2">
    {onViewModeChange && (
      <div className="inline-flex rounded-xl bg-slate-100 p-1 dark:bg-slate-800">
        <button
          type="button"
          onClick={() => onViewModeChange('kanban')}
          className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-bold transition ${
            viewMode === 'kanban'
              ? 'bg-[#56372B] text-white shadow-sm dark:bg-[#C17C4A]'
              : 'text-[#6B5D55] hover:text-[#2D1D16] dark:text-[#BDB0A4] dark:hover:text-white'
          }`}
        >
          <LayoutGrid size={15} />
          Theo tiến độ
        </button>
        <button
          type="button"
          onClick={() => onViewModeChange('batch')}
          className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-bold transition ${
            viewMode === 'batch'
              ? 'bg-[#56372B] text-white shadow-sm dark:bg-[#C17C4A]'
              : 'text-[#6B5D55] hover:text-[#2D1D16] dark:text-[#BDB0A4] dark:hover:text-white'
          }`}
        >
          <Layers size={15} />
          Gom món nấu
          {batchCount > 0 && (
            <span className="rounded-full bg-[#C17C4A] px-1.5 py-0.5 text-[10px] font-black text-white">
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
          ? 'Giữ màn hình bếp không tự tắt'
          : 'Thiết bị này không hỗ trợ Screen Wake Lock'
      }
    >
      <Sun size={17} className={wakeLock.isActive ? 'animate-pulse' : ''} />
      {wakeLock.isActive
        ? ''
        : wakeLock.isSupported
          ? 'Bật giữ màn hình'
          : 'Không hỗ trợ Wake Lock'}
    </button>
    <button
      type="button"
      onClick={onToggleSound}
      className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100 px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-200 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
      title="Bật hoặc tắt âm thanh thông báo"
    >
      {soundEnabled ? <Volume2 size={17} /> : <VolumeX size={17} />}
    </button>
    <button
      type="button"
      onClick={onRefresh}
      disabled={refreshing}
      className="inline-flex items-center gap-2 rounded-xl bg-[#56372B] hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537] px-4 py-2 text-sm font-bold text-white shadow-sm transition disabled:cursor-not-allowed disabled:opacity-70"
    >
      <RefreshCw size={17} className={refreshing ? 'animate-spin' : ''} />
      Làm mới
    </button>
  </div>
);

export default KitchenToolbar;
