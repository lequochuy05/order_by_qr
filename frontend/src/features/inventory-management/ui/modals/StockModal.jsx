import { fmtQty } from '@features/inventory-management/lib/inventoryFormat.js';
import { ModalActions, ModalHeader, SharedModal, TextField } from '@shared/ui';

const StockModal = ({ action, form, setForm, onClose, onSubmit, isSubmitting = false }) => (
  <SharedModal
    isOpen
    onClose={onClose}
    className="max-w-md !p-0"
    ariaLabel={action.type === 'stock-in' ? 'Nhập kho' : 'Kiểm kê kho'}
  >
    <form onSubmit={onSubmit} className="flex min-h-0 w-full flex-col">
      <ModalHeader
        title={action.type === 'stock-in' ? 'Nhập kho' : 'Kiểm kê'}
        onClose={onClose}
        disabled={isSubmitting}
      />
      <div className="space-y-5 p-8">
        <div className="rounded-2xl bg-orange-50 px-4 py-3 text-sm font-bold text-orange-700">
          {action.item.name}: {fmtQty(action.item.quantityOnHand)} {action.item.unit}
        </div>
        {action.type === 'stock-in' ? (
          <TextField
            type="number"
            min="0.001"
            step="0.001"
            label="Số lượng nhập thêm"
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
            value={form.quantityOnHand}
            onChange={(value) => setForm({ ...form, quantityOnHand: value })}
            required
          />
        )}
        <TextField
          label="Ghi chú"
          value={form.note}
          onChange={(value) => setForm({ ...form, note: value })}
        />
      </div>
      <ModalActions onClose={onClose} isSubmitting={isSubmitting} />
    </form>
  </SharedModal>
);

export default StockModal;
