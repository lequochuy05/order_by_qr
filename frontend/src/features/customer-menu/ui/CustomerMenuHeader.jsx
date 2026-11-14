import { Moon, Sun, Wifi, WifiOff } from 'lucide-react';

const CustomerMenuHeader = ({
  restaurantSettings,
  tableNumber,
  isDarkMode,
  onToggleDarkMode,
  wsConnected,
  weatherLine,
  loadingWeather,
}) => (
  <div
    className="rounded-b-3xl bg-slate-900 px-5 pb-5 pt-3 text-white shadow-xl border-b border-slate-800/80"
    style={{ paddingTop: 'calc(0.75rem + var(--safe-area-inset-top))' }}
  >
    <div className="flex items-center justify-between">
      <div className="flex min-w-0 items-center gap-2.5">
        {restaurantSettings.logoUrl && (
          <img
            src={restaurantSettings.logoUrl}
            alt={restaurantSettings.restaurantName}
            className="h-10 w-10 shrink-0 rounded-2xl border border-white/20 bg-white/10 object-cover shadow-sm"
          />
        )}

        <div className="min-w-0">
          <h1 className="truncate text-base font-black uppercase tracking-tight text-white">
            {restaurantSettings.restaurantName || 'QROS'}
          </h1>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onToggleDarkMode}
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-white/20 bg-white/10 text-white shadow-sm transition-all duration-300 hover:bg-white/20 active:scale-95"
          aria-label="Chuyển chế độ sáng tối"
        >
          {isDarkMode ? <Sun size={14} /> : <Moon size={14} />}
        </button>
        <div
          className={`flex items-center gap-1 rounded-full border px-2.5 py-1 text-[10px] font-bold tracking-wider transition-all duration-500 ${
            wsConnected
              ? 'border-emerald-500/40 bg-emerald-500/20 text-emerald-300'
              : 'border-rose-500/40 bg-rose-500/20 text-rose-300'
          }`}
        >
          {wsConnected ? <Wifi size={11} className="animate-pulse" /> : <WifiOff size={11} />}
          {wsConnected ? 'Trực tuyến' : 'Ngoại tuyến'}
        </div>
      </div>
    </div>

    {/* Greeting + table number row */}
    <div className="mt-3 flex items-center gap-2 text-sm text-slate-300">
      <span className="inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-indigo-600 to-violet-600 px-3 py-0.5 text-xs font-bold text-white shadow-md shadow-indigo-600/25">
        Bàn {tableNumber || '?'}
      </span>
      {loadingWeather ? (
        <span className="inline-flex min-h-[22px] items-center gap-1 rounded-full bg-slate-800/80 border border-slate-700/60 px-2.5 py-0.5 text-xs font-semibold backdrop-blur-sm">
          <span className="inline-block h-3 w-8 animate-pulse rounded-full bg-white/30" />
        </span>
      ) : weatherLine ? (
        <span className="inline-flex min-h-[22px] items-center gap-1 rounded-full bg-slate-800/80 border border-slate-700/60 px-2.5 py-0.5 text-xs font-semibold text-slate-300 backdrop-blur-sm">
          {weatherLine}
        </span>
      ) : (
        <span className="inline-flex min-h-[22px] items-center gap-1 rounded-full bg-slate-800/80 border border-slate-700/60 px-2.5 py-0.5 text-xs font-semibold text-slate-300 backdrop-blur-sm">
          Hôm nay bạn muốn thưởng thức món gì?
        </span>
      )}
    </div>
  </div>
);

export default CustomerMenuHeader;
