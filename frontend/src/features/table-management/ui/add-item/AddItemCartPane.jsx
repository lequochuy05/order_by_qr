import { X } from 'lucide-react';

const AddItemCartPane = ({
  cart,
  total,
  onClose,
  onUpdateItem,
  onRemoveItem,
  onConfirm,
  isSubmitting,
}) => (
  <div className="flex max-h-[42%] w-full min-w-0 shrink-0 flex-col bg-white md:max-h-none md:w-[350px]">
    <div className="flex items-center justify-between border-b bg-orange-50 p-4">
      <h3 className="font-bold text-orange-800">Món đã chọn ({cart.length})</h3>
      <button
        type="button"
        onClick={onClose}
        className="rounded-lg p-1 transition-colors hover:bg-orange-100"
        aria-label="Đóng"
      >
        <X size={20} className="text-orange-600" />
      </button>
    </div>

    <div className="flex-1 space-y-3 overflow-y-auto p-4">
      {cart.length === 0 && (
        <p className="mt-10 text-center text-sm text-gray-400">Chưa chọn món nào</p>
      )}
      {cart.map((item, index) => (
        <div key={item.cartId} className="flex flex-col gap-2 border-b pb-3 last:border-0">
          <div className="flex items-start justify-between">
            <div className="text-sm font-bold">{item.name}</div>
            <div className="text-sm font-medium">{item.price.toLocaleString()}</div>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="number"
              min="1"
              className="w-12 rounded border p-1 text-center text-sm"
              value={item.qty}
              onChange={(event) => onUpdateItem(index, { qty: parseInt(event.target.value) || 1 })}
            />
            <input
              type="text"
              className="flex-1 rounded border p-1 text-sm"
              placeholder="Ghi chú..."
              value={item.notes}
              onChange={(event) => onUpdateItem(index, { notes: event.target.value })}
            />
            <button
              type="button"
              onClick={() => onRemoveItem(index)}
              className="shrink-0 rounded p-1 text-red-500 transition-colors hover:bg-red-50"
              title="Xóa khỏi giỏ"
            >
              <X size={16} />
            </button>
          </div>
        </div>
      ))}
    </div>

    <div className="border-t bg-gray-50 p-4">
      <div className="mb-4 flex justify-between text-lg font-bold">
        <span>Tổng tạm tính:</span>
        <span className="text-orange-600">{total.toLocaleString()}đ</span>
      </div>
      <button
        type="button"
        onClick={onConfirm}
        disabled={isSubmitting}
        className={`w-full rounded-xl py-3 font-bold text-white shadow-lg shadow-orange-200 ${
          isSubmitting ? 'cursor-not-allowed bg-orange-300' : 'bg-orange-500 hover:bg-orange-600'
        }`}
      >
        {isSubmitting ? 'Đang xử lý...' : 'Xác nhận thêm món'}
      </button>
    </div>
  </div>
);

export default AddItemCartPane;
