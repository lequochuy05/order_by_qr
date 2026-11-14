import React, { useState } from 'react';
import { Loader2, QrCode, RefreshCw } from 'lucide-react';
import {
  FormLabel,
  ModalActions,
  ModalHeader,
  SelectField,
  SharedModal,
  TextField,
} from '@shared/ui';
import { TABLE_STATUS, TABLE_STATUSES } from '@entities/table/lib/tableStatus.js';

const TABLE_STATUS_OPTIONS = Object.entries(TABLE_STATUS).map(([value, meta]) => ({
  value,
  label: meta.label,
}));

const TableFormModal = ({
  isOpen,
  onClose,
  initialData,
  onSubmit,
  isSubmitting,
  serverErrors = {},
  onClearServerError,
  onRegenerateQr,
  isRegeneratingQr,
}) => {
  const [formData, setFormData] = useState(
    initialData || {
      id: null,
      tableNumber: '',
      capacity: 4,
      status: TABLE_STATUSES.AVAILABLE,
      initialStatus: TABLE_STATUSES.AVAILABLE,
      qrCodeUrl: '',
    },
  );
  const [errors, setErrors] = useState({});
  const initialStatus = initialData?.status ?? TABLE_STATUSES.AVAILABLE;
  const [isQrPreviewOpen, setIsQrPreviewOpen] = useState(false);
  const fieldErrors = {
    tableNumber: errors.tableNumber || serverErrors.tableNumber,
    capacity: errors.capacity || serverErrors.capacity,
  };

  const isChanged = React.useMemo(() => {
    if (!initialData) return true;
    if (formData.tableNumber !== initialData.tableNumber) return true;
    if (formData.capacity !== initialData.capacity) return true;
    if (formData.status !== initialData.status) return true;
    return false;
  }, [formData, initialData]);

  const validateForm = () => {
    const newErrors = {};
    if (!formData.tableNumber || !formData.tableNumber.trim()) {
      newErrors.tableNumber = 'Số bàn không được để trống';
    }
    if (formData.capacity <= 0) {
      newErrors.capacity = 'Sức chứa phải lớn hơn 0';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = (e) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit({ ...formData, initialStatus });
    }
  };

  const handleRegenerateQr = async () => {
    if (!formData.id || !onRegenerateQr) return;
    const updatedTable = await onRegenerateQr(formData.id);
    if (updatedTable) {
      setFormData(updatedTable);
    }
  };

  if (!isOpen) return null;

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      closeOnBackdrop={false}
      className="max-w-md !p-0"
      ariaLabel={formData.id ? 'Cập nhật bàn' : 'Thêm bàn'}
    >
      <ModalHeader
        title={formData.id ? 'Cập Nhật Bàn' : 'Thêm Bàn Mới'}
        subtitle="Thiết lập khu vực phục vụ"
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="tableForm"
        onSubmit={handleSave}
        className="custom-scrollbar space-y-6 overflow-y-auto p-6 sm:p-8"
      >
        <TextField
          label="Số bàn"
          required
          value={formData.tableNumber}
          onChange={(value) => {
            setFormData({ ...formData, tableNumber: value });
            if (errors.tableNumber) setErrors({ ...errors, tableNumber: null });
            onClearServerError?.('tableNumber');
          }}
          error={fieldErrors.tableNumber}
          placeholder="Ví dụ: 101, Bàn 01..."
          disabled={Boolean(formData.id)}
        />

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
          <TextField
            label="Sức chứa"
            required
            type="number"
            min="1"
            inputClassName="text-indigo-600 dark:text-indigo-400 font-mono"
            value={formData.capacity}
            onChange={(value) => {
              setFormData({ ...formData, capacity: parseInt(value) || 0 });
              if (errors.capacity) setErrors({ ...errors, capacity: null });
            }}
            error={fieldErrors.capacity}
          />

          {formData.id && (
            <SelectField
              label="Trạng thái"
              value={formData.status}
              onChange={(value) => setFormData({ ...formData, status: value })}
              options={TABLE_STATUS_OPTIONS}
            />
          )}
        </div>

        {formData.qrCodeUrl && (
          <div className="relative group mt-2">
            <FormLabel>QR Code của bàn</FormLabel>
            <div className="flex flex-col items-center justify-center space-y-3 rounded-3xl border-2 border-dashed border-slate-200/80 bg-slate-50/50 p-6 transition-all group-hover:border-indigo-200 group-hover:bg-indigo-50/20 dark:border-slate-800 dark:bg-slate-950/50 dark:group-hover:border-indigo-500/40 dark:group-hover:bg-indigo-500/10">
              <button
                type="button"
                onClick={() => setIsQrPreviewOpen(true)}
                className="cursor-zoom-in rounded-2xl border border-slate-100 bg-white p-3 shadow-sm transition-transform duration-300 ease-out hover:z-10 hover:scale-125 hover:shadow-xl dark:border-slate-700 dark:bg-slate-900"
              >
                <img src={formData.qrCodeUrl} alt="QR" className="w-32 h-32 object-contain" />
              </button>
              <div className="flex items-center gap-2 rounded-full bg-white px-4 py-1.5 text-[10px] font-black uppercase tracking-tighter text-slate-400 shadow-sm dark:bg-slate-900 dark:text-slate-500">
                <QrCode size={14} className="text-indigo-600 dark:text-indigo-400" />
                <span>Quét để đặt món</span>
              </div>
              {formData.id && (
                <button
                  type="button"
                  onClick={handleRegenerateQr}
                  disabled={isRegeneratingQr}
                  className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2 text-[11px] font-black uppercase tracking-[0.12em] text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:bg-slate-300 disabled:text-slate-500 disabled:shadow-none dark:disabled:bg-slate-700"
                >
                  {isRegeneratingQr ? (
                    <Loader2 size={14} className="animate-spin" />
                  ) : (
                    <RefreshCw size={14} />
                  )}
                  <span>{isRegeneratingQr ? 'Đang tạo...' : 'Tạo lại QR'}</span>
                </button>
              )}
            </div>
          </div>
        )}
      </form>

      <ModalActions
        onClose={onClose}
        formId="tableForm"
        submitLabel={formData.id ? 'Lưu thay đổi' : 'Thêm bàn'}
        isSubmitting={isSubmitting}
        disabled={!isChanged}
      />
      {formData.qrCodeUrl && (
        <SharedModal
          isOpen={isQrPreviewOpen}
          onClose={() => setIsQrPreviewOpen(false)}
          closeOnBackdrop={true}
          className="max-w-lg !p-0 !bg-transparent !shadow-none"
          backdropClassName="bg-black/75 backdrop-blur-sm"
        >
          <img
            src={formData.qrCodeUrl}
            alt="QR"
            className="max-h-[82vh] w-full rounded-2xl bg-white p-5 object-contain shadow-2xl dark:bg-slate-900"
          />
        </SharedModal>
      )}
    </SharedModal>
  );
};

export default TableFormModal;
