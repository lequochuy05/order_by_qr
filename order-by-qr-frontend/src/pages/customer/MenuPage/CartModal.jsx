import React from 'react';
import { ShoppingBasket, X, Sparkles, Loader2 } from 'lucide-react';

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
    calculateTotal,
    isSubmitting,
    handleSubmitOrder
}) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/60 flex items-end z-50 animate-in fade-in duration-300">
            <div className="bg-white w-full max-w-md mx-auto rounded-t-[2rem] p-6 max-h-[85vh] flex flex-col shadow-2xl animate-in slide-in-from-bottom duration-500">

                {/* Header Modal */}
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-lg font-bold flex items-center gap-2 text-gray-800">
                        <ShoppingBasket className="text-orange-500" /> Giỏ hàng của bạn
                    </h3>
                    <button
                        onClick={onClose}
                        className="p-2 bg-gray-100 rounded-full hover:bg-gray-200 transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Danh sách món ăn */}
                <div className="flex-1 overflow-y-auto pr-2 space-y-4">

                    {/* Render món lẻ trong giỏ */}
                    {Object.entries(cart.items || {}).map(([id, item]) => (
                        <div key={id} className="border-b border-gray-100 pb-4">
                            <div className="flex justify-between items-start font-bold text-sm text-gray-800">
                                <div className="flex flex-col flex-1">
                                    <span>
                                        {item.name} <span className="text-orange-500 ml-1">x{item.qty}</span>
                                    </span>
                                    {item.selectedOptionObjs?.length > 0 && (
                                        <span className="text-[10px] text-gray-500 font-medium mt-0.5 leading-tight pr-4">
                                            (Size: {item.selectedOptionObjs.map(o => o.valueName).join(', ')})
                                        </span>
                                    )}
                                </div>
                                <div className="flex flex-col items-end shrink-0">
                                    <span>{fmtVND(item.qty * item.price)}</span>
                                    <div className="flex items-center gap-3 mt-1.5 bg-gray-50 rounded-full px-2 py-0.5 border border-gray-100">
                                        <button
                                            onClick={() => handleUpdateCartItemQty(id, item.qty - 1)}
                                            className="text-gray-400 hover:text-orange-500 font-black px-1"
                                        >
                                            -
                                        </button>
                                        <span className="text-[11px] font-bold text-gray-600 w-3 text-center">{item.qty}</span>
                                        <button
                                            onClick={() => handleUpdateCartItemQty(id, item.qty + 1)}
                                            className="text-gray-400 hover:text-orange-500 font-black px-1"
                                        >
                                            +
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <input
                                type="text"
                                placeholder="Ghi chú thêm (cay, ít đá...)"
                                value={item.note || ""}
                                onChange={(e) => handleUpdateNote(id, e.target.value, false)}
                                className="mt-2 w-full p-3 bg-gray-50 rounded-xl text-xs outline-none focus:ring-1 ring-orange-400 border-none"
                            />
                        </div>
                    ))}

                    {/* Render Combo trong giỏ */}
                    {Object.entries(cart.combos || {}).map(([id, combo]) => (
                        <div key={id} className="mb-2 border-b border-gray-100 pb-4 bg-orange-50/50 p-3 rounded-2xl">
                            <div className="flex justify-between items-center font-bold text-sm text-orange-800">
                                <span>
                                    Combo {combo.name} <span className="text-orange-600 ml-1">x{combo.qty}</span>
                                </span>
                                <span>{fmtVND(combo.qty * combo.price)}</span>
                            </div>
                            <input
                                type="text"
                                placeholder="Ghi chú cho combo..."
                                value={combo.note || ""}
                                onChange={(e) => handleUpdateNote(id, e.target.value, true)}
                                className="mt-2 w-full p-3 bg-white rounded-xl text-xs outline-none border-none shadow-sm"
                            />
                        </div>
                    ))}
                </div>

                {/* Gợi ý mua thêm (Cross-sell) */}
                {crossSellItems?.length > 0 && (
                    <div className="mt-6 pt-4 border-t border-gray-100">
                        <div className="flex justify-between items-center mb-3">
                            <h4 className="text-[11px] font-bold text-orange-800 uppercase tracking-widest flex items-center gap-2">
                                <Sparkles size={14} className="fill-orange-500 text-orange-500" /> Thêm vào cho đủ vị?
                            </h4>
                            <button
                                onClick={() => setCrossSellItems([])}
                                className="text-gray-400 hover:text-gray-600 transition-colors"
                            >
                                <X size={16} />
                            </button>
                        </div>
                        <div className="flex gap-3 overflow-x-auto no-scrollbar pb-2">
                            {crossSellItems.map(item => (
                                <div key={item.id} className="min-w-[130px] bg-gray-50 p-3 rounded-2xl border border-gray-100 hover:border-orange-200 transition-all flex flex-col justify-between">
                                    <div>
                                        <p className="text-[11px] font-semibold text-gray-700 leading-tight mb-2 line-clamp-2 h-8">
                                            {item.name}
                                        </p>
                                    </div>
                                    <button
                                        onClick={() => handleAddToCart(item, (cart.items[item.id]?.qty || 0) + 1, false, item.itemOptions?.length > 0)}
                                        className="w-full py-2 bg-orange-500 text-white text-[10px] font-extrabold rounded-xl shadow-md shadow-orange-100 active:scale-95 transition-all"
                                    >
                                        + {fmtVND(item.price)}
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Tổng tiền và Submit */}
                <div className="mt-6 pt-4 border-t border-gray-200">
                    <div className="flex justify-between items-center mb-4">
                        <span className="font-bold text-gray-500 uppercase text-xs tracking-wider">Tổng cộng</span>
                        <span className="text-2xl font-black text-orange-600">
                            {fmtVND(calculateTotal())}
                        </span>
                    </div>
                    <button
                        disabled={isSubmitting}
                        onClick={handleSubmitOrder}
                        className={`w-full py-4 rounded-2xl font-bold shadow-lg transition-all duration-300 flex items-center justify-center gap-2
              ${isSubmitting ? 'bg-gray-400 cursor-not-allowed' : 'bg-orange-500 shadow-orange-200 active:scale-95 text-white'}`}
                    >
                        {isSubmitting && <Loader2 className="animate-spin" size={20} />}
                        {isSubmitting ? 'ĐANG XỬ LÝ...' : 'XÁC NHẬN ĐẶT MÓN'}
                    </button>
                </div>

            </div>
        </div>
    );
};

export default CartModal;