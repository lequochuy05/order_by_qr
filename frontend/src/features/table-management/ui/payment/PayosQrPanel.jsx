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
          className="flex w-full flex-col items-center justify-center gap-3 rounded-2xl bg-blue-600 py-8 font-bold text-white shadow-lg shadow-blue-200 transition-all hover:bg-blue-700"
        >
          {loading ? <Loader2 size={32} className="animate-spin" /> : <QrCode size={32} />}
          <span className="text-lg">Tạo mã QR PayOS</span>
        </button>
        {error && <p className="text-center text-xs font-medium text-red-500">{error}</p>}
      </>
    )}

    {(status === 'waiting' || status === 'expired') && data && (
      <div className="relative space-y-4 overflow-hidden rounded-2xl border-2 border-dashed border-blue-200 bg-gray-50 p-6 text-center">
        {status === 'expired' && (
          <div className="absolute inset-0 z-10 flex flex-col items-center justify-center bg-white/80 p-4 text-center backdrop-blur-[2px]">
            <div className="mb-2 rounded-full bg-red-50 p-3 text-red-600">
              <XCircle size={32} />
            </div>
            <p className="font-bold text-red-600">Mã thanh toán đã hết hạn</p>
            <p className="mb-4 text-xs text-gray-500">Vui lòng tạo lại mã mới để tiếp tục</p>
            <button
              type="button"
              onClick={onCreate}
              className="mx-auto flex items-center gap-2 rounded-xl bg-blue-600 px-6 py-2 font-bold text-white shadow-lg shadow-blue-200 transition-all hover:bg-blue-700"
            >
              <RefreshCw size={18} /> Tạo mã mới
            </button>
          </div>
        )}
        <div className="flex justify-center">
          <div className="rounded-xl border bg-white p-3 shadow-sm">
            <QRCodeSVG value={data.qrCode} size={220} level="H" includeMargin />
          </div>
        </div>
        <div className="space-y-1">
          <div className="mb-1 flex items-center justify-center gap-2">
            <div
              className={`flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold ${
                timeLeft < 60
                  ? 'animate-pulse bg-red-100 text-red-600'
                  : 'bg-blue-100 text-blue-600'
              }`}
            >
              <Loader2 size={12} className={status === 'waiting' ? 'animate-spin' : ''} />
              Hết hạn sau: {formatTime(timeLeft)}
            </div>
          </div>
          <p className="flex items-center justify-center gap-2 font-bold text-gray-700">
            Đang chờ thanh toán...
          </p>
          <p className="text-xs text-gray-500">Quét mã bằng ứng dụng Ngân hàng để thanh toán</p>
        </div>
        <button
          type="button"
          onClick={onCancel}
          className="mx-auto flex items-center justify-center gap-1 text-sm font-semibold text-red-500 hover:underline"
        >
          <XCircle size={14} /> Hủy giao dịch này
        </button>
      </div>
    )}

    {status === 'success' && (
      <div className="space-y-3 py-8 text-center animate-in zoom-in duration-500">
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-green-100 text-green-600">
          <CheckCircle2 size={48} />
        </div>
        <h4 className="text-xl font-bold text-green-700">Thanh toán thành công!</h4>
        <p className="text-gray-500">Hệ thống đang chuẩn bị in hóa đơn...</p>
      </div>
    )}
  </div>
);

export default PayosQrPanel;
