import { ShoppingBag, X } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const AddItemCartPane = ({
  cart,
  total,
  onClose,
  onUpdateItem,
  onRemoveItem,
  onConfirm,
  isSubmitting,
}) => (
  <div className="flex max-h-[45%] w-full min-w-0 shrink-0 flex-col bg-white transition-colors dark:bg-slate-900 md:max-h-none md:w-[360px]">
    <div className="flex items-center justify-between border-b border-slate-200/80 bg-slate-50/80 px-5 py-4 dark:border-slate-800/80 dark:bg-slate-950/80">
      <div className="flex items-center gap-2.5">
        <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400">
          <ShoppingBag size={16} />
        </span>
        <h3 className="text-sm font-black text-slate-900 dark:text-white">
          Món đã chọn{' '}
          <span className="ml-1 rounded-full bg-indigo-100 px-2 py-0.5 font-mono text-xs font-black text-indigo-800 dark:bg-indigo-950/60 dark:text-indigo-300">
            {cart.length}
          </span>
        </h3>
      </div>
      <button
        type="button"
        onClick={onClose}
        className="rounded-xl p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-800 dark:hover:text-slate-200"
        aria-label="Đóng"
      >
        <X size={18} />
      </button>
    </div>

    <div className="flex-1 space-y-3 overflow-y-auto p-4 custom-scrollbar">
      {cart.length === 0 && (
        <div className="mt-12 text-center text-sm font-semibold text-slate-400 dark:text-slate-500">
          Chưa chọn món nào
        </div>
      )}
      {cart.map((item, index) => (
        <div
          key={item.cartId}
          className="flex flex-col gap-2 rounded-2xl border border-slate-100 bg-slate-50/50 p-3 transition-colors last:border-b-0 dark:border-slate-800/80 dark:bg-slate-800/40"
        >
          <div className="flex items-start justify-between gap-2">
            <div className="text-sm font-bold text-slate-900 dark:text-slate-100">{item.name}</div>
            <div className="font-mono text-xs font-black text-indigo-600 dark:text-indigo-400">
              {fmtVND(item.price)}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="number"
              min="1"
              className="w-14 rounded-xl border border-slate-200/80 bg-white p-1.5 text-center font-mono text-xs font-black text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950 dark:text-slate-100"
              value={item.qty}
              onChange={(event) => onUpdateItem(index, { qty: parseInt(event.target.value) || 1 })}
            />
            <input
              type="text"
              className="flex-1 rounded-xl border border-slate-200/80 bg-white px-3 py-1.5 text-xs text-slate-900 outline-none placeholder:text-slate-400 transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950 dark:text-slate-100 dark:placeholder:text-slate-600"
              placeholder="Ghi chú món..."
              value={item.notes}
              onChange={(event) => onUpdateItem(index, { notes: event.target.value })}
            />
            <button
              type="button"
              onClick={() => onRemoveItem(index)}
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl text-rose-500 transition-colors hover:bg-rose-50 active:scale-95 dark:text-rose-400 dark:hover:bg-rose-950/40"
              title="Xóa khỏi giỏ"
            >
              <X size={15} />
            </button>
          </div>
        </div>
      ))}
    </div>

    <div className="border-t border-slate-100 bg-slate-50/80 p-5 transition-colors dark:border-slate-800 dark:bg-slate-950/60">
      <div className="mb-4 flex items-center justify-between text-base font-bold text-slate-900 dark:text-slate-100">
        <span>Tổng tạm tính:</span>
        <span className="font-mono text-lg font-black text-indigo-600 dark:text-indigo-400">
          {fmtVND(total)}
        </span>
      </div>
      <button
        type="button"
        onClick={onConfirm}
        disabled={isSubmitting || cart.length === 0}
        className={`w-full rounded-2xl py-3 text-sm font-black text-white shadow-lg transition-all active:scale-[0.98] ${
          isSubmitting || cart.length === 0
            ? 'cursor-not-allowed bg-slate-300 shadow-none dark:bg-slate-800 dark:text-slate-500'
            : 'bg-gradient-to-r from-indigo-600 to-violet-600 shadow-indigo-600/25 hover:from-indigo-500 hover:to-violet-500'
        }`}
      >
        {isSubmitting ? 'Đang xử lý...' : 'Xác nhận thêm món'}
      </button>
    </div>
  </div>
);

export default AddItemCartPane;
