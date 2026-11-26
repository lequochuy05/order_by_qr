import {
  AlertTriangle,
  Camera,
  ImagePlus,
  Loader2,
  Sparkles,
  Upload,
} from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';
import { TextField } from '@shared/ui';

const formatFileSizeKb = (size) => {
  const kb = Math.round(Number(size || 0) / 1024);
  if (kb >= 1024) return `${(kb / 1024).toFixed(2)} MB`;
  return `${kb} KB`;
};

const InvoiceOcrUploadSection = ({
  isEn,
  busy,
  scanning,
  selectedFile,
  previewUrl,
  scanError,
  ocrResult,
  rowsCount,
  warnings,
  header,
  onHeaderChange,
  onPickUpload,
  onPickCamera,
  onScanInvoice,
}) => (
  <>
    <div className="grid grid-cols-1 gap-4 rounded-2xl border border-slate-200/80 bg-slate-50/80 p-4 dark:border-slate-800 dark:bg-slate-900/60 lg:grid-cols-12">
      <div className="flex flex-col justify-between gap-3 lg:col-span-7">
        <div className="flex items-start gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-indigo-600 text-white shadow-sm">
            <Sparkles size={18} />
          </div>
          <div>
            <p className="text-sm font-bold text-slate-900 dark:text-white">
              {isEn ? '1. Select or Capture Receipt Image' : '1. Chọn hoặc chụp ảnh hóa đơn'}
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              {isEn
                ? 'Supports JPEG, PNG, WebP up to 5 MB. Clear lighting improves quantity & price accuracy.'
                : 'Hỗ trợ JPEG, PNG, WebP tối đa 5 MB. Ảnh rõ chữ giúp nhận diện số lượng & đơn giá chính xác hơn.'}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={onPickUpload}
            disabled={busy}
            className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
          >
            <Upload size={15} />
            {isEn ? 'Upload Photo' : 'Tải ảnh lên'}
          </button>

          <button
            type="button"
            onClick={onPickCamera}
            disabled={busy}
            className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
          >
            <Camera size={15} />
            {isEn ? 'Take Photo' : 'Chụp ảnh'}
          </button>

          <button
            type="button"
            onClick={onScanInvoice}
            disabled={!selectedFile || busy}
            className="inline-flex h-10 items-center gap-2 rounded-xl bg-indigo-600 px-4 text-xs font-bold text-white shadow-sm transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {scanning ? <Loader2 size={15} className="animate-spin" /> : <Sparkles size={15} />}
            {scanning
              ? isEn
                ? 'Scanning Invoice...'
                : 'Đang đọc hóa đơn...'
              : isEn
                ? 'Scan with AI'
                : 'Quét hóa đơn AI'}
          </button>
        </div>

        {selectedFile && (
          <p className="truncate text-xs font-medium text-slate-600 dark:text-slate-300">
            {selectedFile.name} ({formatFileSizeKb(selectedFile.size)})
          </p>
        )}
      </div>

      <div className="flex items-center justify-center lg:col-span-5">
        {previewUrl ? (
          <img
            src={previewUrl}
            alt={isEn ? 'Invoice preview' : 'Xem trước hóa đơn'}
            className="max-h-40 w-full rounded-xl border border-slate-200 object-contain bg-white dark:border-slate-700 dark:bg-slate-950"
          />
        ) : (
          <div className="flex h-32 w-full flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-slate-300/80 text-slate-400 dark:border-slate-700 dark:text-slate-500">
            <ImagePlus size={24} />
            <span className="text-xs font-medium">
              {isEn ? 'No invoice photo selected' : 'Chưa có ảnh hóa đơn'}
            </span>
          </div>
        )}
      </div>
    </div>

    {scanError && (
      <div
        role="alert"
        className="flex items-start gap-2.5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700 dark:border-rose-500/30 dark:bg-rose-950/40 dark:text-rose-300"
      >
        <AlertTriangle size={16} className="mt-0.5 shrink-0" />
        <span>{scanError}</span>
      </div>
    )}

    {ocrResult && (
      <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-indigo-200/80 bg-indigo-50/70 px-4 py-3 text-xs dark:border-indigo-500/25 dark:bg-indigo-950/35">
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1 rounded-full bg-indigo-600 px-2.5 py-0.5 text-[11px] font-bold text-white">
            {ocrResult.aiGenerated ? 'Gemini Vision' : isEn ? 'Local Fallback' : 'Dự phòng'}
          </span>
          {ocrResult.invoiceDate && (
            <span className="font-semibold text-slate-700 dark:text-slate-200">
              {isEn ? 'Date:' : 'Ngày:'} {ocrResult.invoiceDate}
            </span>
          )}
          {ocrResult.totalAmount != null && Number(ocrResult.totalAmount) > 0 && (
            <span className="font-semibold text-slate-700 dark:text-slate-200">
              {isEn ? 'Invoice Total:' : 'Tổng HĐ:'} {fmtVND(ocrResult.totalAmount)}
            </span>
          )}
        </div>
        <span className="font-semibold text-indigo-700 dark:text-indigo-300">
          {rowsCount} {isEn ? 'line(s)' : 'dòng hàng'}
        </span>
      </div>
    )}

    {warnings.length > 0 && (
      <div className="space-y-1 rounded-2xl border border-amber-200 bg-amber-50/80 px-4 py-3 text-xs text-amber-800 dark:border-amber-500/30 dark:bg-amber-950/30 dark:text-amber-300">
        {warnings.map((warning, idx) => (
          <div key={idx} className="flex items-start gap-2">
            <AlertTriangle size={14} className="mt-0.5 shrink-0" />
            <span>{warning}</span>
          </div>
        ))}
      </div>
    )}

    <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
      <TextField
        label={isEn ? 'Supplier' : 'Nhà cung cấp'}
        value={header.supplierName}
        onChange={(value) => onHeaderChange({ supplierName: value })}
        placeholder={isEn ? 'Optional supplier name' : 'Tên nhà cung cấp (tùy chọn)'}
      />
      <TextField
        label={isEn ? 'Invoice Ref' : 'Số hóa đơn'}
        value={header.invoiceRef}
        onChange={(value) => onHeaderChange({ invoiceRef: value })}
        placeholder={isEn ? 'e.g. HD-0012' : 'VD: HD-0012'}
      />
      <TextField
        label={isEn ? 'Batch Note' : 'Ghi chú phiếu nhập'}
        value={header.note}
        onChange={(value) => onHeaderChange({ note: value })}
        placeholder={isEn ? 'Optional note' : 'Ghi chú chung...'}
      />
    </div>
  </>
);

export default InvoiceOcrUploadSection;
