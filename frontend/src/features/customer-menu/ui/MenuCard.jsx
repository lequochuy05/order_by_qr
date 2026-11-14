import { useRef } from 'react';
import { Plus, Minus } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const MenuCard = ({ item, onAddToCart, quantity = 0, onFly }) => {
  const imageUrl = item.img;
  const available = item.available !== false;
  const cardRef = useRef(null);

  const handlePlus = () => {
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
      className={`bg-white dark:bg-slate-900/90 rounded-3xl border border-slate-200/80 dark:border-slate-800/80 shadow-sm overflow-hidden flex flex-col h-full transition-all duration-300 hover:shadow-xl hover:-translate-y-1 active:scale-[0.98] group ${available ? '' : 'opacity-70'}`}
    >
      <div className="relative w-full pt-[100%] bg-slate-100 dark:bg-slate-800/60 overflow-hidden">
        <img
          src={imageUrl}
          className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          alt={item.name}
          onError={(e) => {
            e.target.onerror = null;
          }}
        />
        {/* Price overlay trên ảnh */}
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-slate-950/80 via-slate-950/30 to-transparent px-3 pb-2 pt-8">
          <span className="text-white font-mono font-black text-sm sm:text-base drop-shadow-md">
            {fmtVND(item.price)}
          </span>
        </div>
        {!available && (
          <div className="absolute inset-0 bg-slate-950/60 flex items-center justify-center">
            <span className="px-3 py-1 rounded-full bg-white text-rose-600 text-xs font-bold uppercase shadow-md">
              Hết hàng
            </span>
          </div>
        )}
      </div>

      <div className="p-3.5 flex flex-col flex-1 justify-between transition-colors">
        <div>
          <h3 className="font-bold text-slate-900 dark:text-white text-sm line-clamp-1 leading-tight transition-colors">
            {item.name}
          </h3>
          <p className="text-xs text-slate-400 dark:text-slate-500 mt-1 line-clamp-2 transition-colors min-h-[2rem] leading-relaxed">
            {item.description?.trim() || 'Chưa có mô tả món.'}
          </p>
        </div>

        <div className="mt-3 flex flex-col gap-2">
          <div
            className={`flex items-center rounded-full p-1 bg-slate-100 dark:bg-slate-800/80 border border-slate-200/60 dark:border-slate-700/60 transition-colors ${
              item.itemOptions?.length > 0 ? 'justify-center w-[7.5rem] ml-auto' : 'justify-between'
            }`}
          >
            {item.itemOptions && item.itemOptions.length > 0 ? (
              <button
                onClick={() => onAddToCart(item, quantity + 1, true)}
                disabled={!available}
                className="w-full h-8 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 rounded-full shadow-sm shadow-indigo-600/25 flex items-center justify-center text-white font-bold text-xs uppercase tracking-wider px-3 transition-all active:scale-95 disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:text-slate-500"
              >
                {available ? 'Tùy chọn' : 'Hết hàng'}
              </button>
            ) : (
              <>
                <button
                  onClick={() => onAddToCart(item, quantity - 1)}
                  className={`w-7 h-7 rounded-full flex items-center justify-center font-bold transition-all active:scale-95 ${
                    quantity > 0
                      ? 'bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-200 shadow-sm'
                      : 'text-transparent pointer-events-none'
                  }`}
                  disabled={quantity === 0}
                >
                  <Minus size={13} />
                </button>

                <span
                  className={`text-xs font-mono font-bold text-indigo-600 dark:text-indigo-400 min-w-[1.25rem] text-center transition-colors ${
                    quantity === 0 ? 'opacity-0' : ''
                  }`}
                >
                  {quantity}
                </span>

                <button
                  onClick={handlePlus}
                  disabled={!available}
                  className="w-7 h-7 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 rounded-full shadow-sm shadow-indigo-600/25 flex items-center justify-center text-white font-bold active:scale-110 transition-all disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:text-slate-500"
                >
                  <Plus size={13} />
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
