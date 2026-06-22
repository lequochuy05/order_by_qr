import { Moon, Sun, Wifi, WifiOff } from 'lucide-react';

const CustomerMenuHeader = ({
  restaurantSettings,
  tableNumber,
  isDarkMode,
  onToggleDarkMode,
  wsConnected,
}) => (
  <div
    className="rounded-b-[2.5rem] bg-orange-500 p-5 text-white shadow-lg"
    style={{ paddingTop: 'calc(1.25rem + var(--safe-area-inset-top))' }}
  >
    <div className="flex items-start justify-between">
      <div className="flex min-w-0 items-start gap-3">
        {restaurantSettings.logoUrl && (
          <img
            src={restaurantSettings.logoUrl}
            alt={restaurantSettings.restaurantName}
            className="h-10 w-10 rounded-xl border border-white/30 bg-white/20 object-cover"
          />
        )}
        <div className="min-w-0">
          <h1 className="truncate text-xl font-black uppercase tracking-tighter">
            {restaurantSettings.restaurantName || 'Sắc Màu Quán'}
          </h1>
          <div className="mt-1 flex items-center gap-2">
            <span className="rounded-full border border-white/30 bg-white/20 px-3 py-0.5 text-[10px] font-bold uppercase">
              Bàn số: {tableNumber || 'NaN'}
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onToggleDarkMode}
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full border border-white/30 bg-white/20 text-white shadow-sm transition-all duration-300 hover:bg-white/30"
          aria-label="Chuyển chế độ sáng tối"
        >
          {isDarkMode ? <Sun size={14} className="animate-spin-slow" /> : <Moon size={14} />}
        </button>
        <div
          className={`flex items-center gap-1 rounded-full border px-3 py-1 text-[10px] font-bold transition-all duration-500 ${
            wsConnected ? 'border-green-400 bg-green-500/20' : 'border-red-400 bg-red-500/20'
          }`}
        >
          {wsConnected ? <Wifi size={12} className="animate-pulse" /> : <WifiOff size={12} />}
          {wsConnected ? 'LIVE' : 'OFFLINE'}
        </div>
      </div>
    </div>
  </div>
);

export default CustomerMenuHeader;
