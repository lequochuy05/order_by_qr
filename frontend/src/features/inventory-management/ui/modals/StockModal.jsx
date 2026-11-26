import { ArrowDownToLine, ClipboardCheck, Package } from 'lucide-react';
import { fmtQty } from '@features/inventory-management/lib/inventoryFormat.js';
import { ModalActions, ModalHeader, SharedModal, TextField } from '@shared/ui';

const StockModal = ({ action, form, setForm, onClose, onSubmit, isSubmitting = false }) => {
  const isStockIn = action.type === 'stock-in';
  const currentQty = Number(action.item.quantityOnHand || 0);
  const delta = isStockIn
    ? Number(form.quantity || 0)
    : form.quantityOnHand === ''
      ? 0
      : Number(form.quantityOnHand) - currentQty;
  const nextQty = isStockIn
    ? currentQty + Math.max(0, Number(form.quantity || 0))
    : Number(form.quantityOnHand || 0);

  return (
    <SharedModal
      isOpen
      onClose={onClose}
      className="max-w-md !p-0"
      ariaLabel={isStockIn ? 'Nhập kho nguyên liệu' : 'Kiểm kê tồn kho'}
    >
      <ModalHeader
        title={isStockIn ? 'Nhập kho nguyên liệu' : 'Kiểm kê tồn kho'}
        subtitle="Cập nhật số lượng tồn kho thực tế"
        icon={isStockIn ? ArrowDownToLine : ClipboardCheck}
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="stockAdjustForm"
        onSubmit={onSubmit}
        className="custom-scrollbar flex-1 space-y-5 overflow-y-auto p-6 sm:p-8"
      >
        <div className="rounded-2xl border border-indigo-200/70 bg-gradient-to-br from-indigo-50/90 to-violet-50/70 p-4 dark:border-indigo-500/25 dark:from-indigo-950/50 dark:to-violet-950/30">
          <div className="flex items-start justify-between gap-3">
            <div className="flex min-w-0 items-center gap-2.5">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white text-indigo-600 shadow-xs ring-1 ring-indigo-500/15 dark:bg-slate-900 dark:text-indigo-400">
                <Package size={17} />
              </div>
              <div className="min-w-0">
                <p className="text-[10px] font-bold uppercase tracking-wider text-indigo-600/80 dark:text-indigo-400">
                  Nguyên liệu
                </p>
                <p className="truncate text-sm font-black text-slate-900 dark:text-white">
                  {action.item.name}
                </p>
              </div>
            </div>
            <div className="text-right">
              <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Hiện có
              </p>
              <p className="font-mono text-sm font-black text-indigo-600 dark:text-indigo-400">
                {fmtQty(currentQty)} {action.item.unit}
              </p>
            </div>
          </div>

          <div className="mt-3 flex items-center justify-between border-t border-indigo-200/60 pt-2.5 text-xs dark:border-indigo-500/20">
            <span className="font-semibold text-slate-600 dark:text-slate-300">
              {isStockIn ? 'Dự kiến sau nhập' : 'Chênh lệch kiểm kê'}
            </span>
            <span
              className={`font-mono font-black ${
                !isStockIn && delta < 0
                  ? 'text-rose-600 dark:text-rose-400'
                  : 'text-emerald-600 dark:text-emerald-400'
              }`}
            >
              {isStockIn
                ? `${fmtQty(nextQty)} ${action.item.unit}`
                : `${delta > 0 ? '+' : ''}${fmtQty(delta)} ${action.item.unit}`}
            </span>
          </div>
        </div>

        {isStockIn ? (
          <TextField
            type="number"
            min="0.001"
            step="0.001"
            label="Số lượng nhập thêm"
            suffix={action.item.unit}
            value={form.quantity}
            onChange={(value) => setForm({ ...form, quantity: value })}
            required
          />
        ) : (
          <TextField
            type="number"
            min="0"
            step="0.001"
            label="Tồn thực tế sau kiểm kê"
            suffix={action.item.unit}
            value={form.quantityOnHand}
            onChange={(value) => setForm({ ...form, quantityOnHand: value })}
            required
          />
        )}

        <TextField
          label="Ghi chú"
          placeholder="Lý do nhập / điều chỉnh tồn kho..."
          value={form.note}
          onChange={(value) => setForm({ ...form, note: value })}
        />
      </form>

      <ModalActions
        formId="stockAdjustForm"
        onClose={onClose}
        submitLabel={isStockIn ? 'Xác nhận nhập' : 'Lưu kiểm kê'}
        isSubmitting={isSubmitting}
      />
    </SharedModal>
  );
};

export default StockModal;
