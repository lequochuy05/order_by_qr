import React from 'react';
import { ShoppingBasket, X, Sparkles, Loader2 } from 'lucide-react';
import SharedModal from '@shared/ui/SharedModal.jsx';

const CartModal = ({
  isOpen,
  onClose,
  cart,
  fmtVND,
  handleUpdateCartItemQty,
  handleUpdateNote,
  crossSellItems,
  setCrossSellItems,
  handleAddToCart,
  getCartItemQty,
  calculateTotal,
  isSubmitting,
  orderingBlocked = false,
  orderingBlockMessage = '',
  handleSubmitOrder,
}) => {
  if (!isOpen) return null;

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      placement="bottom"
      closeOnBackdrop
      ariaLabel="Giỏ hàng của bạn"
      className="!max-h-[85vh] !max-w-md !p-6"
    >
      {/* Header Modal */}
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-base font-black flex items-center gap-2.5 text-slate-900 dark:text-white transition-colors">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-md shadow-indigo-500/25">
            <ShoppingBasket size={18} />
          </div>
          Giỏ hàng của bạn
        </h3>
        <button
          onClick={onClose}
          className="p-2 bg-slate-100 dark:bg-slate-800 rounded-full hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-500 dark:text-slate-400 transition-colors active:scale-95"
        >
          <X size={18} />
        </button>
      </div>

      {/* Danh sách món ăn */}
      <div className="flex-1 overflow-y-auto pr-2 space-y-4 custom-scrollbar">
        {/* Render món lẻ trong giỏ */}
        {Object.entries(cart.items || {}).map(([id, item], index) => (
          <div
            key={id}
            className="border-b border-slate-100 dark:border-slate-800/80 pb-4 transition-colors animate-stagger-fade"
            style={{ animationDelay: `${index * 30}ms` }}
          >
            <div className="flex justify-between items-start text-sm text-slate-900 dark:text-white transition-colors">
              <div className="flex flex-col flex-1 pr-3">
                <span className="font-bold">
                  {item.name} <span className="text-indigo-600 dark:text-indigo-400 font-mono ml-1">x{item.qty}</span>
                </span>
                {item.selectedOptionObjs?.length > 0 && (
                  <span className="text-xs text-slate-400 dark:text-slate-500 font-medium mt-0.5 leading-tight pr-4">
                    ({item.selectedOptionObjs.map((o) => o.valueName).join(', ')})
                  </span>
                )}
              </div>
              <div className="flex flex-col items-end shrink-0">
                <span className="font-black font-mono text-slate-900 dark:text-white">
                  {fmtVND(item.qty * item.price)}
                </span>
                <div className="flex items-center gap-2.5 mt-2 bg-slate-100 dark:bg-slate-800 rounded-full px-2 py-0.5 border border-slate-200/60 dark:border-slate-700/60 transition-colors">
                  <button
                    onClick={() => handleUpdateCartItemQty(id, item.qty - 1)}
                    className="text-slate-500 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 font-bold px-1 transition-colors"
                  >
                    -
                  </button>
                  <span className="text-xs font-bold font-mono text-slate-900 dark:text-white w-3 text-center">
                    {item.qty}
                  </span>
                  <button
                    onClick={() => handleUpdateCartItemQty(id, item.qty + 1)}
                    className="text-slate-500 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 font-bold px-1 transition-colors"
                  >
                    +
                  </button>
                </div>
              </div>
            </div>
            <input
              type="text"
              placeholder="Ghi chú thêm (cay, ít đá...)"
              value={item.note || ''}
              onChange={(e) => handleUpdateNote(id, e.target.value, false)}
              className="mt-2 w-full p-2.5 bg-slate-50 dark:bg-slate-800/60 dark:text-white dark:placeholder-slate-500 rounded-xl text-xs outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 border border-slate-200/80 dark:border-slate-700 transition-colors"
            />
          </div>
        ))}

        {/* Render Combo trong giỏ */}
        {Object.entries(cart.combos || {}).map(([id, combo]) => (
          <div
            key={id}
            className="mb-2 border border-indigo-200/60 dark:border-indigo-800/40 pb-3 bg-indigo-50/40 dark:bg-indigo-950/20 p-3 rounded-2xl transition-colors"
          >
            <div className="flex justify-between items-center font-bold text-sm text-indigo-950 dark:text-indigo-200 transition-colors">
              <span>
                Combo {combo.name}{' '}
                <span className="text-indigo-600 dark:text-indigo-400 font-mono ml-1">x{combo.qty}</span>
              </span>
              <span className="font-black font-mono">{fmtVND(combo.qty * combo.price)}</span>
            </div>
            <input
              type="text"
              placeholder="Ghi chú cho combo..."
              value={combo.note || ''}
              onChange={(e) => handleUpdateNote(id, e.target.value, true)}
              className="mt-2 w-full p-2.5 bg-white dark:bg-slate-900 dark:text-white dark:placeholder-slate-500 rounded-xl text-xs outline-none border border-slate-200/80 dark:border-slate-700 shadow-sm transition-colors focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>
        ))}
      </div>

      {/* Gợi ý mua thêm (Cross-sell) */}
      {crossSellItems?.length > 0 && (
        <div className="mt-5 pt-4 border-t border-slate-200/80 dark:border-slate-800/80 transition-colors">
          <div className="flex justify-between items-center mb-3">
            <h4 className="text-[11px] font-bold text-indigo-600 dark:text-indigo-400 uppercase tracking-wider flex items-center gap-1.5 transition-colors">
              <Sparkles size={14} className="fill-indigo-600 text-indigo-600 dark:fill-indigo-400 dark:text-indigo-400" /> Thêm vào cho đủ vị?
            </h4>
            <button
              onClick={() => setCrossSellItems([])}
              className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors"
            >
              <X size={15} />
            </button>
          </div>
          <div className="flex gap-2.5 overflow-x-auto no-scrollbar pb-2">
            {crossSellItems.map((item) => {
              const hasOptions = item.itemOptions?.length > 0;
              const available = item.available !== false;

              return (
                <div
                  key={item.id}
                  className="min-w-[130px] bg-slate-50/80 dark:bg-slate-800/60 p-3 rounded-2xl border border-slate-200/80 dark:border-slate-700/80 hover:border-indigo-300 transition-all flex flex-col justify-between"
                >
                  <div>
                    <p className="text-[11px] font-bold text-slate-800 dark:text-slate-200 leading-tight mb-1.5 line-clamp-2 h-8">
                      {item.name}
                    </p>
                    <p className="mb-2 text-[11px] font-mono font-bold text-indigo-600 dark:text-indigo-400">
                      {fmtVND(item.price)}
                    </p>
                  </div>
                  <button
                    disabled={!available}
                    onClick={() =>
                      handleAddToCart(item, getCartItemQty(item) + 1, false, hasOptions)
                    }
                    className="w-full py-1.5 bg-gradient-to-r from-indigo-600 to-violet-600 text-white text-xs font-bold rounded-xl shadow-sm active:scale-95 transition-all disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:text-slate-500"
                  >
                    {!available ? 'Hết hàng' : hasOptions ? 'Tùy chọn' : '+ Thêm'}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Tổng tiền và Submit */}
      <div className="safe-bottom mt-5 border-t border-slate-200/80 pt-4 transition-colors dark:border-slate-800/80">
        <div className="flex justify-between items-center mb-4">
          <span className="font-bold text-slate-400 dark:text-slate-500 uppercase text-xs tracking-wider">
            Tổng cộng
          </span>
          <span className="text-2xl font-black font-mono tracking-tight text-indigo-600 dark:text-indigo-400">
            {fmtVND(calculateTotal())}
          </span>
        </div>
        <button
          disabled={isSubmitting || orderingBlocked}
          onClick={handleSubmitOrder}
          className={`w-full py-4 rounded-2xl font-bold shadow-lg transition-all duration-300 flex items-center justify-center gap-2 tracking-wide active:scale-[0.98]
            ${
              isSubmitting || orderingBlocked
                ? 'bg-slate-300 dark:bg-slate-800 cursor-not-allowed text-slate-500 shadow-none'
                : 'bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white shadow-indigo-600/25'
            }`}
        >
          {isSubmitting && <Loader2 className="animate-spin" size={18} />}
          {orderingBlocked
            ? 'BÀN ĐANG THANH TOÁN'
            : isSubmitting
              ? 'ĐANG XỬ LÝ...'
              : 'XÁC NHẬN ĐẶT MÓN'}
        </button>
        {orderingBlocked && orderingBlockMessage && (
          <p className="mt-2 text-center text-xs font-semibold text-rose-600">
            {orderingBlockMessage}
          </p>
        )}
      </div>
    </SharedModal>
  );
};

export default CartModal;
