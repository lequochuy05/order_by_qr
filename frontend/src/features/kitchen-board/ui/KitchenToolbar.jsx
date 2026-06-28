import { RefreshCw, Sun, Volume2, VolumeX } from 'lucide-react';

const KitchenToolbar = ({ wakeLock, soundEnabled, onToggleSound, refreshing, onRefresh }) => (
  <div className="flex flex-wrap items-center gap-2">
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
        ? 'Màn hình luôn bật'
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
      {soundEnabled ? 'Có âm thanh' : 'Tắt âm thanh'}
    </button>
    <button
      type="button"
      onClick={onRefresh}
      disabled={refreshing}
      className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2 text-sm font-bold text-white transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:opacity-70"
    >
      <RefreshCw size={17} className={refreshing ? 'animate-spin' : ''} />
      Làm mới
    </button>
  </div>
);

export default KitchenToolbar;
