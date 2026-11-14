import { CheckCircle2, Loader2, QrCode, RefreshCw, XCircle } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

const formatTime = (seconds) => {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
};

const PayosQrPanel = ({ status, data, loading, timeLeft, error, onCreate, onCancel }) => (
  <div className="space-y-4 animate-in slide-in-from-bottom-2 duration-300">
    {(status === 'idle' || status === 'error') && (
      <>
        <button
          type="button"
          onClick={onCreate}
          disabled={loading}
          className="flex w-full flex-col items-center justify-center gap-3 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 py-8 font-black text-white shadow-lg shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:opacity-50"
        >
          {loading ? <Loader2 size={32} className="animate-spin" /> : <QrCode size={32} />}
          <span className="text-base sm:text-lg">Tạo mã QR PayOS</span>
        </button>
        {error && <p className="text-center text-xs font-semibold text-rose-500">{error}</p>}
      </>
    )}

    {(status === 'waiting' || status === 'expired') && data && (
      <div className="relative space-y-4 overflow-hidden rounded-2xl border-2 border-dashed border-indigo-200/80 bg-slate-50/60 p-6 text-center dark:border-indigo-500/30 dark:bg-slate-950/60">
        {status === 'expired' && (
          <div className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/85 p-4 text-center backdrop-blur-[2px] dark:bg-slate-950/85">
            <div className="mb-2 rounded-2xl bg-rose-50 p-3 text-rose-600 dark:bg-rose-500/10 dark:text-rose-300">
              <XCircle size={32} />
            </div>
            <p className="font-bold text-rose-600">Mã thanh toán đã hết hạn</p>
            <p className="mb-4 text-xs text-slate-500 dark:text-slate-400">
              Vui lòng tạo lại mã mới để tiếp tục
            </p>
            <button
              type="button"
              onClick={onCreate}
              className="mx-auto flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-6 py-2.5 text-xs font-bold text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95"
            >
              <RefreshCw size={16} /> Tạo mã mới
            </button>
          </div>
        )}
        <div className="flex justify-center">
          <div className="rounded-2xl border border-slate-200/80 bg-white p-3.5 shadow-sm dark:border-slate-700 dark:bg-slate-900">
            <QRCodeSVG value={data.qrCode} size={220} level="H" includeMargin />
          </div>
        </div>
        <div className="space-y-1">
          <div className="mb-1 flex items-center justify-center gap-2">
            <div
              className={`flex items-center gap-1.5 rounded-full px-3 py-1 font-mono text-xs font-bold ${
                timeLeft < 60
                  ? 'animate-pulse bg-rose-100 text-rose-600 dark:bg-rose-500/20 dark:text-rose-400'
                  : 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300'
              }`}
            >
              <Loader2 size={12} className={status === 'waiting' ? 'animate-spin' : ''} />
              Hết hạn sau: {formatTime(timeLeft)}
            </div>
          </div>
          <p className="flex items-center justify-center gap-2 text-sm font-bold text-slate-700 dark:text-slate-200">
            Đang chờ thanh toán...
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Quét mã bằng ứng dụng Ngân hàng để thanh toán
          </p>
        </div>
        <button
          type="button"
          onClick={onCancel}
          className="mx-auto flex items-center justify-center gap-1 text-xs font-bold text-rose-500 transition hover:text-rose-700"
        >
          <XCircle size={14} /> Hủy giao dịch này
        </button>
      </div>
    )}

    {status === 'success' && (
      <div className="space-y-3 py-8 text-center animate-in zoom-in duration-500">
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400">
          <CheckCircle2 size={44} />
        </div>
        <h4 className="text-xl font-black text-emerald-700 dark:text-emerald-300">
          Thanh toán thành công!
        </h4>
        <p className="text-xs text-slate-500 dark:text-slate-400">Hệ thống đang chuẩn bị in hóa đơn...</p>
      </div>
    )}
  </div>
);

export default PayosQrPanel;
