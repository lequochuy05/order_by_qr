import { useRef } from 'react';
import { Plus, Minus } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const MenuCard = ({ item, onAddToCart, quantity = 0, onFly }) => {
  const imageUrl = item.img;
  const available = item.available !== false;
  const cardRef = useRef(null);

  const handlePlus = (e) => {
    const img = cardRef.current?.querySelector('img');
    if (onFly && img) {
      const rect = img.getBoundingClientRect();
      onFly(rect);
    }
    onAddToCart(item, quantity + 1);
  };

  return (
    <div
      ref={cardRef}
      className={`bg-surface-warm dark:bg-slate-900 rounded-3xl border border-stone-200/60 dark:border-slate-800/50 shadow-[0_8px_30px_rgb(0,0,0,0.04)] dark:shadow-[0_8px_30px_rgb(0,0,0,0.2)] overflow-hidden flex flex-col h-full transition-all duration-300 hover:shadow-[0_8px_30px_rgb(0,0,0,0.08)] dark:hover:shadow-[0_8px_30px_rgb(0,0,0,0.3)] active:scale-[0.98] group ${available ? '' : 'opacity-70'}`}
    >
      <div className="relative w-full pt-[100%] bg-stone-100 dark:bg-slate-800 overflow-hidden">
        <img
          src={imageUrl}
          className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          alt={item.name}
          onError={(e) => {
            e.target.onerror = null;
          }}
        />
        {/* Price overlay trên ảnh */}
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/60 via-black/20 to-transparent px-3 pb-2 pt-8">
          <span className="text-white font-black text-base drop-shadow-sm">
            {fmtVND(item.price)}
          </span>
        </div>
        {!available && (
          <div className="absolute inset-0 bg-black/45 flex items-center justify-center">
            <span className="px-3 py-1 rounded-full bg-white text-red-600 text-xs font-black uppercase">
              Hết hàng
            </span>
          </div>
        )}
      </div>

      <div className="p-3 flex flex-col flex-1 justify-between transition-colors duration-500">
        <div>
          <h3 className="font-semibold text-gray-800 dark:text-white text-[15px] line-clamp-1 leading-tight transition-colors">
            {item.name}
          </h3>
          <p className="text-xs text-stone-400 dark:text-gray-500 mt-1 line-clamp-2 transition-colors min-h-[2rem]">
            {item.description?.trim() || 'Chưa có mô tả món.'}
          </p>
        </div>

        <div className="mt-3 flex flex-col gap-2">
          <div
            className={`flex items-center rounded-full p-1 bg-orange-50 dark:bg-orange-500/10 transition-colors duration-500 ${item.itemOptions?.length > 0 ? 'justify-center w-[7rem] ml-auto' : 'justify-between'}`}
          >
            {item.itemOptions && item.itemOptions.length > 0 ? (
              <button
                onClick={() => onAddToCart(item, quantity + 1, true)}
                disabled={!available}
                className="w-full h-9 bg-orange-500 hover:bg-orange-600 rounded-full shadow-sm flex items-center justify-center text-white font-black text-xs uppercase tracking-wider px-3 transition-colors disabled:bg-gray-300 disabled:text-gray-500"
              >
                {available ? 'Tùy chọn' : 'Hết hàng'}
              </button>
            ) : (
              <>
                <button
                  onClick={() => onAddToCart(item, quantity - 1)}
                  className={`w-10 h-10 rounded-full flex items-center justify-center font-bold transition-colors ${
                    quantity > 0
                      ? 'bg-white dark:bg-slate-800 text-orange-600 dark:text-orange-400 shadow-sm'
                      : 'text-transparent pointer-events-none'
                  }`}
                  disabled={quantity === 0}
                >
                  <Minus size={16} />
                </button>

                <span
                  className={`text-sm font-bold text-orange-700 dark:text-orange-400 min-w-[1.5rem] text-center transition-colors ${quantity === 0 ? 'opacity-0' : ''}`}
                >
                  {quantity}
                </span>

                <button
                  onClick={handlePlus}
                  disabled={!available}
                  className="w-10 h-10 bg-orange-500 rounded-full shadow-sm flex items-center justify-center text-white font-bold hover:bg-orange-600 active:scale-110 transition-all disabled:bg-gray-300 disabled:text-gray-500 duration-150"
                >
                  <Plus size={16} />
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default MenuCard;
