import { useEffect, useMemo, useRef, useState } from 'react';
import { AlertTriangle } from 'lucide-react';

import { useBatchStockInMutation } from '@entities/inventory';
import { aiInventoryService } from '@entities/ai';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { useTranslation } from '@shared/lib/i18n';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import {
  buildBatchStockInPayload,
  createEmptyReviewRow,
  mapOcrResponseToReviewRows,
  recalculateRowForTargetItem,
  validateInvoiceImageFile,
} from '@features/inventory-management/lib/invoiceOcrHelpers.js';
import { ModalActions, ModalHeader, SharedModal } from '@shared/ui';
import InvoiceOcrReviewList from './InvoiceOcrReviewList.jsx';
import InvoiceOcrUploadSection from './InvoiceOcrUploadSection.jsx';

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

  const selectedCount = useMemo(() => rows.filter((row) => row.selected).length, [rows]);

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

  const handleHeaderChange = (patch) => {
    setHeader((prev) => ({ ...prev, ...patch }));
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

        <InvoiceOcrUploadSection
          isEn={isEn}
          busy={busy}
          scanning={scanning}
          selectedFile={selectedFile}
          previewUrl={previewUrl}
          scanError={scanError}
          ocrResult={ocrResult}
          rowsCount={rows.length}
          warnings={warnings}
          header={header}
          onHeaderChange={handleHeaderChange}
          onPickUpload={() => uploadInputRef.current?.click()}
          onPickCamera={() => cameraInputRef.current?.click()}
          onScanInvoice={handleScanInvoice}
        />

        <InvoiceOcrReviewList
          isEn={isEn}
          isManager={isManager}
          busy={busy}
          rows={rows}
          inventoryItems={inventoryItems}
          inventoryItemsLoading={inventoryItemsLoading}
          selectedCount={selectedCount}
          estimatedTotal={estimatedTotal}
          onAddRow={handleAddRow}
          onRemoveRow={handleRemoveRow}
          onUpdateRow={updateRow}
          onSelectCatalogItem={handleSelectCatalogItem}
          onToggleAllUnitCosts={handleToggleAllUnitCosts}
        />

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
