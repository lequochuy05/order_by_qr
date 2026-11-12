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
          <h3 className="text-lg font-black flex items-center gap-2 text-[#2D1D16] dark:text-[#F7F0E7] transition-colors">
            <ShoppingBasket className="text-[#C17C4A]" /> Giỏ hàng của bạn
          </h3>
          <button
            onClick={onClose}
            className="p-2 bg-[#F7F0E7] dark:bg-[#2A221E] rounded-full hover:bg-[#EAE0D5] dark:hover:bg-[#352C26] text-[#6B5D55] dark:text-[#BDB0A4] transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Danh sách món ăn */}
        <div className="flex-1 overflow-y-auto pr-2 space-y-4">
          {/* Render món lẻ trong giỏ */}
          {Object.entries(cart.items || {}).map(([id, item], index) => (
            <div
              key={id}
              className="border-b border-[#E8DCD1] dark:border-[#352C26] pb-4 transition-colors animate-stagger-fade"
              style={{ animationDelay: `${index * 30}ms` }}
            >
              <div className="flex justify-between items-start font-bold text-sm text-[#2D1D16] dark:text-[#F7F0E7] transition-colors">
                <div className="flex flex-col flex-1">
                  <span>
                    {item.name} <span className="text-[#C17C4A] ml-1">x{item.qty}</span>
                  </span>
                  {item.selectedOptionObjs?.length > 0 && (
                    <span className="text-xs text-[#9C8E85] dark:text-[#82756B] font-medium mt-0.5 leading-tight pr-4 transition-colors">
                      (Tùy chọn: {item.selectedOptionObjs.map((o) => o.valueName).join(', ')})
                    </span>
                  )}
                </div>
                <div className="flex flex-col items-end shrink-0">
                  <span className="font-extrabold text-[#56372B] dark:text-[#D8986A]">{fmtVND(item.qty * item.price)}</span>
                  <div className="flex items-center gap-3 mt-1.5 bg-[#F7F0E7] dark:bg-[#2A221E] rounded-full px-2 py-0.5 border border-[#E8DCD1] dark:border-[#352C26] transition-colors">
                    <button
                      onClick={() => handleUpdateCartItemQty(id, item.qty - 1)}
                      className="text-[#9C8E85] dark:text-[#82756B] hover:text-[#C17C4A] dark:hover:text-[#D8986A] font-black px-1 transition-colors"
                    >
                      -
                    </button>
                    <span className="text-[11px] font-bold text-[#2D1D16] dark:text-[#F7F0E7] w-3 text-center transition-colors">
                      {item.qty}
                    </span>
                    <button
                      onClick={() => handleUpdateCartItemQty(id, item.qty + 1)}
                      className="text-[#9C8E85] dark:text-[#82756B] hover:text-[#C17C4A] dark:hover:text-[#D8986A] font-black px-1 transition-colors"
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
                className="mt-2 w-full p-3 bg-[#F7F0E7]/60 dark:bg-[#2A221E] dark:text-white dark:placeholder-[#82756B] rounded-xl text-xs outline-none focus:ring-2 ring-[#C17C4A]/30 border border-[#E8DCD1]/50 dark:border-[#352C26] transition-colors"
              />
            </div>
          ))}

          {/* Render Combo trong giỏ */}
          {Object.entries(cart.combos || {}).map(([id, combo]) => (
            <div
              key={id}
              className="mb-2 border-b border-[#E8DCD1] dark:border-[#352C26] pb-4 bg-[#F7F0E7] dark:bg-[#2A221E]/60 p-3 rounded-2xl transition-colors"
            >
              <div className="flex justify-between items-center font-bold text-sm text-[#56372B] dark:text-[#D8986A] transition-colors">
                <span>
                  Combo {combo.name}{' '}
                  <span className="text-[#C17C4A] ml-1">x{combo.qty}</span>
                </span>
                <span className="font-extrabold">{fmtVND(combo.qty * combo.price)}</span>
              </div>
              <input
                type="text"
                placeholder="Ghi chú cho combo..."
                value={combo.note || ''}
                onChange={(e) => handleUpdateNote(id, e.target.value, true)}
                className="mt-2 w-full p-3 bg-white dark:bg-[#1F1A17] dark:text-white dark:placeholder-[#82756B] rounded-xl text-xs outline-none border border-[#E8DCD1] dark:border-[#352C26] shadow-sm transition-colors"
              />
            </div>
          ))}
        </div>

        {/* Gợi ý mua thêm (Cross-sell) */}
        {crossSellItems?.length > 0 && (
          <div className="mt-6 pt-4 border-t border-[#E8DCD1] dark:border-[#352C26] transition-colors">
            <div className="flex justify-between items-center mb-3">
              <h4 className="text-[11px] font-black text-[#8B4F31] dark:text-[#D8986A] uppercase tracking-widest flex items-center gap-2 transition-colors">
                <Sparkles size={14} className="fill-[#C17C4A] text-[#C17C4A]" /> Thêm vào cho đủ vị?
              </h4>
              <button
                onClick={() => setCrossSellItems([])}
                className="text-[#9C8E85] dark:text-[#82756B] hover:text-[#56372B] dark:hover:text-[#F7F0E7] transition-colors"
              >
                <X size={16} />
              </button>
            </div>
            <div className="flex gap-3 overflow-x-auto no-scrollbar pb-2">
              {crossSellItems.map((item) => {
                const hasOptions = item.itemOptions?.length > 0;
                const available = item.available !== false;

                return (
                  <div
                    key={item.id}
                    className="min-w-[130px] bg-[#F7F0E7]/80 dark:bg-[#2A221E]/80 p-3 rounded-2xl border border-[#E8DCD1] dark:border-[#352C26] hover:border-[#C17C4A] transition-all flex flex-col justify-between"
                  >
                    <div>
                      <p className="text-[11px] font-bold text-[#2D1D16] dark:text-[#F7F0E7] leading-tight mb-2 line-clamp-2 h-8 transition-colors">
                        {item.name}
                      </p>
                      <p className="mb-2 text-[11px] font-black text-[#C17C4A]">
                        {fmtVND(item.price)}
                      </p>
                    </div>
                    <button
                      disabled={!available}
                      onClick={() =>
                        handleAddToCart(item, getCartItemQty(item) + 1, false, hasOptions)
                      }
                      className="w-full py-2 bg-[#56372B] hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537] text-white text-xs font-black rounded-xl shadow-sm active:scale-95 transition-all disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:text-slate-500 disabled:shadow-none"
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
        <div className="safe-bottom mt-6 border-t border-[#E8DCD1] pt-4 transition-colors dark:border-[#352C26]">
          <div className="flex justify-between items-center mb-4">
            <span className="font-bold text-[#6B5D55] dark:text-[#BDB0A4] uppercase text-xs tracking-wider transition-colors">
              Tổng cộng
            </span>
            <span className="text-2xl font-black text-[#56372B] dark:text-[#D8986A] transition-colors">
              {fmtVND(calculateTotal())}
            </span>
          </div>
          <button
            disabled={isSubmitting || orderingBlocked}
            onClick={handleSubmitOrder}
            className={`w-full py-4 rounded-2xl font-black shadow-lg transition-all duration-300 flex items-center justify-center gap-2 tracking-wide
              ${isSubmitting || orderingBlocked ? 'bg-slate-300 dark:bg-slate-800 cursor-not-allowed text-slate-500' : 'bg-[#56372B] hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537] shadow-[#56372B]/20 dark:shadow-[#C17C4A]/25 active:scale-98 text-white'}`}
          >
            {isSubmitting && <Loader2 className="animate-spin" size={20} />}
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
