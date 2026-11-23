import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AlertTriangle,
  Camera,
  CheckCircle2,
  ImagePlus,
  Loader2,
  Lock,
  Plus,
  Sparkles,
  Trash2,
  Upload,
} from 'lucide-react';

import { useBatchStockInMutation } from '@entities/inventory';
import { aiInventoryService } from '@shared/api/aiService.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { fmtVND } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import {
  buildBatchStockInPayload,
  createEmptyReviewRow,
  mapOcrResponseToReviewRows,
  recalculateRowForTargetItem,
  validateInvoiceImageFile,
} from '@features/inventory-management/lib/invoiceOcrHelpers.js';
import { ModalActions, ModalHeader, SharedModal, TextField } from '@shared/ui';

const MATCH_BADGE_MAP = {
  MATCHED: {
    vi: 'Đã khớp',
    en: 'Matched',
    cls: 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/15 dark:text-emerald-300',
  },
  UNIT_MISMATCH: {
    vi: 'Khác đơn vị',
    en: 'Unit differs',
    cls: 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-500/30 dark:bg-amber-500/15 dark:text-amber-300',
  },
  UNMATCHED: {
    vi: 'Chưa khớp',
    en: 'Unmatched',
    cls: 'border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300',
  },
};

const formatFileSizeKb = (size) => {
  const kb = Math.round(Number(size || 0) / 1024);
  if (kb >= 1024) return `${(kb / 1024).toFixed(2)} MB`;
  return `${kb} KB`;
};

const InvoiceOcrModal = ({
  inventoryItems = [],
  inventoryItemsLoading = false,
  isManager = false,
  onClose,
  onSuccess,
}) => {
  const { language, isEn } = useTranslation('inventory');
  const uploadInputRef = useRef(null);
  const cameraInputRef = useRef(null);

  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [scanning, setScanning] = useState(false);
  const [scanError, setScanError] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [ocrResult, setOcrResult] = useState(null);
  const [header, setHeader] = useState({
    supplierName: '',
    invoiceRef: '',
    note: '',
  });
  const [rows, setRows] = useState([]);

  const { mutateAsync: batchStockIn, isPending: isBatchSubmitting } = useBatchStockInMutation();

  useEffect(() => {
    if (!selectedFile) {
      setPreviewUrl('');
      return undefined;
    }
    const url = URL.createObjectURL(selectedFile);
    setPreviewUrl(url);
    return () => {
      URL.revokeObjectURL(url);
    };
  }, [selectedFile]);

  const busy = scanning || isBatchSubmitting;

  const selectedCount = useMemo(
    () => rows.filter((row) => row.selected).length,
    [rows],
  );

  const estimatedTotal = useMemo(
    () =>
      rows.reduce((sum, row) => {
        if (!row.selected) return sum;
        const qty = Number(row.quantity || 0);
        const cost = isManager ? Number(row.unitCost || 0) : Number(row.currentUnitCost || 0);
        if (!Number.isFinite(qty) || !Number.isFinite(cost) || qty <= 0 || cost <= 0) return sum;
        return sum + qty * cost;
      }, 0),
    [isManager, rows],
  );

  const handleSafeClose = () => {
    if (!busy) onClose?.();
  };

  const handleFilePicked = (event) => {
    const file = event.target.files?.[0] || null;
    event.target.value = '';
    if (!file) return;

    const validation = validateInvoiceImageFile(file, language);
    if (!validation.valid) {
      setScanError(validation.error);
      return;
    }

    setScanError('');
    setSubmitError('');
    setSelectedFile(file);
  };

  const handleScanInvoice = async () => {
    if (!selectedFile || busy) return;

    setScanning(true);
    setScanError('');
    setSubmitError('');
    try {
      const response = await aiInventoryService.scanInvoice(selectedFile, { language });
      setOcrResult(response);
      setHeader({
        supplierName: response?.supplierName || '',
        invoiceRef: response?.invoiceNumber || '',
        note: response?.invoiceDate
          ? isEn
            ? `Invoice date: ${response.invoiceDate}`
            : `Ngày hóa đơn: ${response.invoiceDate}`
          : '',
      });
      setRows(mapOcrResponseToReviewRows(response, inventoryItems));
    } catch (error) {
      setScanError(buildErrorMessage(error, { includeDetails: false }));
    } finally {
      setScanning(false);
    }
  };

  const updateRow = (rowKey, patch) => {
    setSubmitError('');
    setRows((current) =>
      current.map((row) => (row._key === rowKey ? { ...row, ...patch } : row)),
    );
  };

  const handleSelectCatalogItem = (rowKey, targetItemId) => {
    setSubmitError('');
    setRows((current) =>
      current.map((row) =>
        row._key === rowKey
          ? recalculateRowForTargetItem(row, targetItemId, inventoryItems, language)
          : row,
      ),
    );
  };

  const handleAddRow = () => {
    setSubmitError('');
    setRows((current) => [...current, createEmptyReviewRow()]);
  };

  const handleRemoveRow = (rowKey) => {
    setSubmitError('');
    setRows((current) => current.filter((row) => row._key !== rowKey));
  };

  const handleToggleAllUnitCosts = (checked) => {
    if (!isManager) return;
    setRows((current) =>
      current.map((row) => ({
        ...row,
        updateUnitCost: row.unitCost !== '' ? checked : false,
      })),
    );
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (busy) return;

    const prepared = buildBatchStockInPayload(header, rows, isManager, language);
    if (!prepared.valid) {
      setSubmitError(prepared.error);
      return;
    }

    setSubmitError('');
    try {
      const response = await batchStockIn(prepared.payload);
      const count = response?.importedCount ?? prepared.payload.items.length;
      showSuccessToast(
        isEn
          ? `Imported ${count} inventory item(s) from invoice`
          : `Đã nhập kho ${count} dòng nguyên liệu từ hóa đơn`,
      );
      onSuccess?.(response);
      onClose?.();
    } catch (error) {
      const message = buildErrorMessage(error, { includeDetails: false });
      setSubmitError(message);
      showErrorToast(error);
    }
  };

  const warnings = ocrResult?.warnings || [];

  return (
    <SharedModal
      isOpen
      onClose={handleSafeClose}
      className="max-w-5xl !p-0"
      ariaLabel={isEn ? 'Scan invoice with Gemini Vision' : 'Nhập kho từ ảnh hóa đơn AI'}
    >
      <ModalHeader
        title={isEn ? 'Stock-in from Invoice Photo (AI)' : 'Nhập kho từ ảnh hóa đơn (Gemini Vision)'}
        subtitle={
          isEn
            ? 'Upload or take a receipt photo, review matched ingredients & unit conversions, then confirm.'
            : 'Chụp hoặc tải ảnh hóa đơn, kiểm tra đối chiếu nguyên liệu & quy đổi đơn vị trước khi xác nhận.'
        }
        onClose={handleSafeClose}
        disabled={busy}
      />

      <form
        id="invoiceOcrForm"
        onSubmit={handleSubmit}
        className="custom-scrollbar flex-1 space-y-5 overflow-y-auto p-5 sm:p-7"
      >
        <input
          ref={uploadInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className="hidden"
          onChange={handleFilePicked}
        />
        <input
          ref={cameraInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          capture="environment"
          className="hidden"
          onChange={handleFilePicked}
        />

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
                onClick={() => uploadInputRef.current?.click()}
                disabled={busy}
                className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
              >
                <Upload size={15} />
                {isEn ? 'Upload Photo' : 'Tải ảnh lên'}
              </button>

              <button
                type="button"
                onClick={() => cameraInputRef.current?.click()}
                disabled={busy}
                className="inline-flex h-10 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
              >
                <Camera size={15} />
                {isEn ? 'Take Photo' : 'Chụp ảnh'}
              </button>

              <button
                type="button"
                onClick={handleScanInvoice}
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
              {rows.length} {isEn ? 'line(s)' : 'dòng hàng'}
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
            onChange={(value) => setHeader((prev) => ({ ...prev, supplierName: value }))}
            placeholder={isEn ? 'Optional supplier name' : 'Tên nhà cung cấp (tùy chọn)'}
          />
          <TextField
            label={isEn ? 'Invoice Ref' : 'Số hóa đơn'}
            value={header.invoiceRef}
            onChange={(value) => setHeader((prev) => ({ ...prev, invoiceRef: value }))}
            placeholder={isEn ? 'e.g. HD-0012' : 'VD: HD-0012'}
          />
          <TextField
            label={isEn ? 'Batch Note' : 'Ghi chú phiếu nhập'}
            value={header.note}
            onChange={(value) => setHeader((prev) => ({ ...prev, note: value }))}
            placeholder={isEn ? 'Optional note' : 'Ghi chú chung...'}
          />
        </div>

        <div className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <h3 className="text-sm font-black text-slate-900 dark:text-white">
                {isEn ? '2. Review Matched Inventory Lines' : '2. Đối chiếu dòng nguyên liệu trước khi nhập'}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {isEn
                  ? 'Nothing is added to stock until you click Confirm Stock-In.'
                  : 'Hệ thống chỉ cộng tồn kho sau khi bạn kiểm tra và bấm Xác nhận nhập kho.'}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {isManager && rows.some((row) => row.unitCost !== '') && (
                <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200">
                  <input
                    type="checkbox"
                    checked={
                      rows.filter((row) => row.unitCost !== '').length > 0 &&
                      rows
                        .filter((row) => row.unitCost !== '')
                        .every((row) => row.updateUnitCost)
                    }
                    onChange={(e) => handleToggleAllUnitCosts(e.target.checked)}
                    className="h-3.5 w-3.5 rounded accent-indigo-600"
                  />
                  <span>{isEn ? 'Update all unit costs' : 'Cập nhật tất cả giá vốn'}</span>
                </label>
              )}

              <button
                type="button"
                onClick={handleAddRow}
                disabled={busy}
                className="inline-flex h-9 items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3 text-xs font-bold text-indigo-700 transition hover:bg-indigo-600 hover:text-white disabled:cursor-not-allowed disabled:opacity-50 dark:border-indigo-700/60 dark:bg-indigo-950/40 dark:text-indigo-300"
              >
                <Plus size={14} />
                {isEn ? 'Add Line' : 'Thêm dòng'}
              </button>
            </div>
          </div>

          {!isManager && (
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
              <Lock size={14} className="shrink-0 text-slate-400" />
              <span>
                {isEn
                  ? 'Chef role can batch stock-in quantities. Only Managers can update ingredient unit costs.'
                  : 'Vai trò Bếp được xác nhận nhập số lượng vào kho. Chỉ Quản lý mới được phép cập nhật giá vốn nguyên liệu.'}
              </span>
            </div>
          )}

          {inventoryItemsLoading && (
            <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
              <Loader2 size={14} className="animate-spin" />
              <span>{isEn ? 'Loading inventory catalog...' : 'Đang tải danh mục nguyên liệu...'}</span>
            </div>
          )}

          {rows.length === 0 ? (
            <div className="rounded-2xl border-2 border-dashed border-slate-200 py-8 text-center text-xs font-medium text-slate-500 dark:border-slate-800 dark:text-slate-400">
              {isEn
                ? 'Scan an invoice image above or click "Add Line" to enter stock-in items manually.'
                : 'Chụp/tải ảnh hóa đơn ở trên để AI trích xuất, hoặc bấm "Thêm dòng" để nhập thủ công.'}
            </div>
          ) : (
            <div className="space-y-3">
              {rows.map((row) => {
                const badge = MATCH_BADGE_MAP[row.matchStatus] || MATCH_BADGE_MAP.UNMATCHED;
                return (
                  <div
                    key={row._key}
                    className={`rounded-2xl border p-3.5 transition ${
                      row.selected
                        ? 'border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900'
                        : 'border-slate-200/60 bg-slate-50/70 opacity-70 dark:border-slate-800 dark:bg-slate-950/60'
                    }`}
                  >
                    <div className="mb-2.5 flex flex-wrap items-center justify-between gap-2">
                      <div className="flex min-w-0 items-center gap-2.5">
                        <input
                          type="checkbox"
                          aria-label={isEn ? 'Select line' : 'Chọn dòng nhập'}
                          checked={Boolean(row.selected)}
                          onChange={(e) => updateRow(row._key, { selected: e.target.checked })}
                          className="h-4 w-4 rounded accent-indigo-600"
                        />
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="truncate text-sm font-bold text-slate-900 dark:text-white">
                              {row.rawName || (isEn ? 'Manual item' : 'Dòng nhập thủ công')}
                            </span>
                            <span
                              className={`rounded-full border px-2 py-0.5 text-[10px] font-bold ${badge.cls}`}
                            >
                              {isEn ? badge.en : badge.vi}
                            </span>
                          </div>
                          {(row.invoiceQuantity || row.invoiceUnit || row.invoiceUnitPrice) && (
                            <p className="text-[11px] text-slate-500 dark:text-slate-400">
                              {isEn ? 'Receipt:' : 'Hóa đơn:'} {row.invoiceQuantity || '?'}{' '}
                              {row.invoiceUnit}
                              {row.invoiceUnitPrice !== '' && Number(row.invoiceUnitPrice) > 0
                                ? ` · ${fmtVND(row.invoiceUnitPrice)}`
                                : ''}
                            </p>
                          )}
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemoveRow(row._key)}
                        title={isEn ? 'Remove line' : 'Xóa dòng'}
                        aria-label={isEn ? 'Remove line' : 'Xóa dòng'}
                        className="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-rose-200/80 bg-rose-50 text-rose-600 transition hover:bg-rose-600 hover:text-white dark:border-rose-800/40 dark:bg-rose-950/40 dark:text-rose-400"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>

                    {row.conversionNote && (
                      <p className="mb-2.5 rounded-xl bg-amber-50 px-3 py-1.5 text-[11px] font-medium text-amber-800 dark:bg-amber-950/35 dark:text-amber-300">
                        {row.conversionNote}
                      </p>
                    )}

                    <div
                      className={`grid grid-cols-1 gap-3 ${
                        isManager ? 'md:grid-cols-12' : 'md:grid-cols-8'
                      }`}
                    >
                      <div className="md:col-span-4">
                        <label className="block">
                          <span className="mb-1 block text-[11px] font-bold text-slate-600 dark:text-slate-300">
                            {isEn ? 'Inventory Item' : 'Nguyên liệu kho'}
                          </span>
                          <select
                            value={row.inventoryItemId}
                            onChange={(e) => handleSelectCatalogItem(row._key, e.target.value)}
                            className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-800 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                          >
                            <option value="">
                              {isEn ? '-- Select ingredient --' : '-- Chọn nguyên liệu --'}
                            </option>
                            {inventoryItems.map((item) => (
                              <option key={item.id} value={String(item.id)}>
                                {item.name} ({item.unit})
                              </option>
                            ))}
                          </select>
                        </label>
                      </div>

                      <div className="md:col-span-2">
                        <label className="block">
                          <span className="mb-1 block text-[11px] font-bold text-slate-600 dark:text-slate-300">
                            {isEn ? 'Stock Qty' : 'SL nhập'}{' '}
                            {row.unit ? `(${row.unit})` : ''}
                          </span>
                          <input
                            type="number"
                            min="0.001"
                            step="0.001"
                            value={row.quantity}
                            onChange={(e) => updateRow(row._key, { quantity: e.target.value })}
                            className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-900 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white"
                          />
                        </label>
                      </div>

                      {isManager && (
                        <div className="md:col-span-4">
                          <label className="block">
                            <span className="mb-1 flex items-center justify-between text-[11px] font-bold text-slate-600 dark:text-slate-300">
                              <span>
                                {isEn ? 'Unit Cost' : 'Đơn giá'} ({row.unit || 'đv'})
                              </span>
                              <span className="font-normal text-slate-400">
                                {isEn ? 'Cur:' : 'Hiện tại:'} {fmtVND(row.currentUnitCost || 0)}
                              </span>
                            </span>
                            <input
                              type="number"
                              min="0"
                              step="100"
                              value={row.unitCost}
                              onChange={(e) => updateRow(row._key, { unitCost: e.target.value })}
                              placeholder="0"
                              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-900 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white"
                            />
                          </label>
                          <label className="mt-1.5 inline-flex cursor-pointer items-center gap-1.5 text-[11px] font-semibold text-slate-600 dark:text-slate-300">
                            <input
                              type="checkbox"
                              checked={Boolean(row.updateUnitCost)}
                              disabled={row.unitCost === ''}
                              onChange={(e) =>
                                updateRow(row._key, { updateUnitCost: e.target.checked })
                              }
                              className="h-3.5 w-3.5 rounded accent-indigo-600"
                            />
                            <span>{isEn ? 'Update item unit cost' : 'Cập nhật giá vốn'}</span>
                          </label>
                        </div>
                      )}

                      <div className="md:col-span-2">
                        <label className="block">
                          <span className="mb-1 block text-[11px] font-bold text-slate-600 dark:text-slate-300">
                            {isEn ? 'Line Note' : 'Ghi chú dòng'}
                          </span>
                          <input
                            type="text"
                            maxLength={300}
                            value={row.note}
                            onChange={(e) => updateRow(row._key, { note: e.target.value })}
                            placeholder="..."
                            className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs text-slate-800 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                          />
                        </label>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {rows.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-emerald-200/80 bg-emerald-50/70 px-4 py-3 text-xs font-semibold text-emerald-900 dark:border-emerald-500/25 dark:bg-emerald-950/30 dark:text-emerald-200">
            <span className="inline-flex items-center gap-1.5">
              <CheckCircle2 size={15} className="text-emerald-600 dark:text-emerald-400" />
              {isEn
                ? `${selectedCount} item(s) selected for batch stock-in`
                : `Đã chọn ${selectedCount} dòng nguyên liệu để nhập kho`}
            </span>
            {estimatedTotal > 0 && (
              <span>
                {isEn ? 'Estimated value:' : 'Giá trị ước tính:'} {fmtVND(estimatedTotal)}
              </span>
            )}
          </div>
        )}

        {submitError && (
          <div
            role="alert"
            className="flex items-start gap-2.5 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-medium text-rose-700 dark:border-rose-500/30 dark:bg-rose-950/40 dark:text-rose-300"
          >
            <AlertTriangle size={16} className="mt-0.5 shrink-0" />
            <span>{submitError}</span>
          </div>
        )}
      </form>

      <ModalActions
        formId="invoiceOcrForm"
        onClose={handleSafeClose}
        submitLabel={
          isEn
            ? `Confirm Stock-In (${selectedCount})`
            : `Xác nhận nhập kho (${selectedCount})`
        }
        isSubmitting={isBatchSubmitting}
        disabled={scanning || selectedCount === 0}
      />
    </SharedModal>
  );
};

export default InvoiceOcrModal;
