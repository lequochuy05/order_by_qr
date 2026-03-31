import { Plus, Minus } from 'lucide-react';
import { fmtVND } from '../../../utils/formatters.js';
const MenuCard = ({ item, onAddToCart, quantity = 0 }) => {

  const imageUrl = item.img;

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden flex flex-col h-full transition-transform active:scale-95">
      <div className="relative w-full pt-[100%] bg-gray-50">
        <img
          src={imageUrl}
          className="absolute inset-0 w-full h-full object-cover"
          alt={item.name}
          onError={(e) => {
            e.target.onerror = null;
          }}
        />
      </div>

      <div className="p-3 flex flex-col flex-1 justify-between">
        <div>
          <h3 className="font-bold text-gray-800 text-sm line-clamp-1 leading-tight">
            {item.name}
          </h3>
          <p className="text-[10px] text-gray-400 mt-1 line-clamp-1">
            {"Thơm ngon nồng hổi..."}
          </p>
        </div>

        <div className="mt-3 flex flex-col gap-2">
          <span className="text-orange-600 font-bold text-sm">
            {fmtVND(item.price)}
          </span>

          {/* Bộ điều khiển số lượng */}
          <div className={`flex items-center rounded-full p-1 bg-orange-50 ${item.itemOptions?.length > 0 ? 'justify-center w-[5.5rem] ml-auto' : 'justify-between'}`}>
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
                  className={`w-6 h-6 rounded-full flex items-center justify-center font-bold transition-colors ${quantity > 0 ? 'bg-white text-orange-600 shadow-sm' : 'text-transparent'
                    }`}
                  disabled={quantity === 0}
                >
                  <Minus size={14} />
                </button>

                <span className={`text-xs font-bold text-orange-700 ${quantity === 0 ? 'opacity-0' : ''}`}>
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