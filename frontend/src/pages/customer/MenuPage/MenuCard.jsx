import { Plus, Minus } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';
const MenuCard = ({ item, onAddToCart, quantity = 0 }) => {

  const imageUrl = item.img;

  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-gray-50/50 dark:border-slate-800/50 shadow-[0_8px_30px_rgb(0,0,0,0.04)] dark:shadow-[0_8px_30px_rgb(0,0,0,0.2)] overflow-hidden flex flex-col h-full transition-all duration-300 hover:shadow-[0_8px_30px_rgb(0,0,0,0.08)] dark:hover:shadow-[0_8px_30px_rgb(0,0,0,0.3)] active:scale-[0.98] group">
      <div className="relative w-full pt-[100%] bg-gray-50 dark:bg-slate-800 overflow-hidden">
        <img
          src={imageUrl}
          className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          alt={item.name}
          onError={(e) => {
            e.target.onerror = null;
          }}
        />
      </div>

      <div className="p-3 flex flex-col flex-1 justify-between transition-colors duration-500">
        <div>
          <h3 className="font-bold text-gray-800 dark:text-white text-sm line-clamp-1 leading-tight transition-colors">
            {item.name}
          </h3>
          <p className="text-[10px] text-gray-400 dark:text-gray-500 mt-1 line-clamp-1 transition-colors">
            {"Thơm ngon nồng hổi..."}
          </p>
        </div>

        <div className="mt-3 flex flex-col gap-2">
          <span className="text-orange-600 dark:text-orange-400 font-black text-[15px] tracking-tight transition-colors">
            {fmtVND(item.price)}
          </span>

          {/* Bộ điều khiển số lượng */}
          <div className={`flex items-center rounded-full p-1 bg-orange-50 dark:bg-orange-500/10 transition-colors duration-500 ${item.itemOptions?.length > 0 ? 'justify-center w-[5.5rem] ml-auto' : 'justify-between'}`}>
            {item.itemOptions && item.itemOptions.length > 0 ? (
               <button 
                 onClick={() => onAddToCart(item, quantity + 1, true)}
                 className="w-full h-7 bg-orange-500 hover:bg-orange-600 rounded-full shadow-sm flex items-center justify-center text-white font-black text-[10px] uppercase tracking-wider px-2 transition-colors"
               >
                 Tùy chọn
               </button>
            ) : (
              <>
                <button
                  onClick={() => onAddToCart(item, quantity - 1)}
                  className={`w-6 h-6 rounded-full flex items-center justify-center font-bold transition-colors ${quantity > 0 ? 'bg-white dark:bg-slate-800 text-orange-600 dark:text-orange-400 shadow-sm' : 'text-transparent'
                    }`}
                  disabled={quantity === 0}
                >
                  <Minus size={14} />
                </button>

                <span className={`text-xs font-bold text-orange-700 dark:text-orange-400 transition-colors ${quantity === 0 ? 'opacity-0' : ''}`}>
                  {quantity}
                </span>

                <button
                  onClick={() => onAddToCart(item, quantity + 1)}
                  className="w-6 h-6 bg-orange-500 rounded-full shadow-sm flex items-center justify-center text-white font-bold hover:bg-orange-600 transition-colors"
                >
                  <Plus size={14} />
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