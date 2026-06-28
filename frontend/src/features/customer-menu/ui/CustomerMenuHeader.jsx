import { Moon, Sun, Wifi, WifiOff, SunDim, CloudSun, MoonStar } from 'lucide-react';

const GREETING_ICON = {
  Sáng: <SunDim size={14} />,
  Trưa: <CloudSun size={14} />,
  Chiều: <CloudSun size={14} />,
  Tối: <MoonStar size={14} />,
};

const CustomerMenuHeader = ({
  restaurantSettings,
  tableNumber,
  isDarkMode,
  onToggleDarkMode,
  wsConnected,
  timeContext,
  weatherLine,
  loadingWeather,
}) => (
  <div
    className="rounded-b-3xl bg-orange-500 px-5 pb-4 pt-3 text-white shadow-lg"
    style={{ paddingTop: 'calc(0.75rem + var(--safe-area-inset-top))' }}
  >
    <div className="flex items-center justify-between">
      <div className="flex min-w-0 items-center gap-2.5">
        {restaurantSettings.logoUrl && (
          <img
            src={restaurantSettings.logoUrl}
            alt={restaurantSettings.restaurantName}
            className="h-9 w-9 shrink-0 rounded-xl border border-white/30 bg-white/20 object-cover"
          />
        )}

        <div className="min-w-0">
          <h1 className="truncate text-lg font-extrabold uppercase tracking-tight">
            {restaurantSettings.restaurantName || 'QROS'}
          </h1>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onToggleDarkMode}
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-white/30 bg-white/20 text-white shadow-sm transition-all duration-300 hover:bg-white/30"
          aria-label="Chuyển chế độ sáng tối"
        >
          {isDarkMode ? <Sun size={14} /> : <Moon size={14} />}
        </button>
        <div
          className={`flex items-center gap-1 rounded-full border px-2.5 py-1 text-[10px] font-bold transition-all duration-500 ${
            wsConnected ? 'border-green-400 bg-green-500/20' : 'border-red-400 bg-red-500/20'
          }`}
          style={{ fontSize: '10px' }}
        >
          {wsConnected ? <Wifi size={11} className="animate-pulse" /> : <WifiOff size={11} />}
          {wsConnected ? 'Trực tuyến' : 'Ngoại tuyến'}
        </div>
      </div>
    </div>

    {/* Greeting + table number row */}
    <div className="mt-2 flex items-center gap-2 text-sm text-orange-100">
      <span className="inline-flex items-center gap-1 rounded-full bg-white/15 px-2.5 py-0.5 text-xs font-semibold backdrop-blur-sm">
        Bàn {tableNumber || '?'}
      </span>
      {loadingWeather ? (
        <span className="inline-flex min-h-[22px] items-center gap-1 rounded-full bg-white/15 px-2.5 py-0.5 text-xs font-semibold backdrop-blur-sm">
          <span className="inline-block h-3 w-8 animate-pulse rounded-full bg-white/30" />
        </span>
      ) : weatherLine ? (
        <span className="inline-flex min-h-[22px] items-center gap-1 rounded-full bg-white/15 px-2.5 py-0.5 text-xs font-semibold backdrop-blur-sm">
          {weatherLine}
        </span>
      ) : (
        <span className="inline-flex min-h-[22px] items-center gap-1 rounded-full bg-white/15 px-2.5 py-0.5 text-xs font-semibold backdrop-blur-sm">
          Hôm nay ăn gì ngon?
        </span>
      )}
    </div>
  </div>
);

export default CustomerMenuHeader;
