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
      className={`bg-white dark:bg-[#1F1A17] rounded-3xl border border-[#E8DCD1] dark:border-[#352C26] shadow-sm overflow-hidden flex flex-col h-full transition-all duration-300 hover:shadow-md dark:shadow-black/20 active:scale-[0.98] group ${available ? '' : 'opacity-70'}`}
    >
      <div className="relative w-full pt-[100%] bg-[#F7F0E7] dark:bg-[#2A221E] overflow-hidden">
        <img
          src={imageUrl}
          className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
          alt={item.name}
          onError={(e) => {
            e.target.onerror = null;
          }}
        />
        {/* Price overlay trên ảnh */}
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent px-3 pb-2 pt-8">
          <span className="text-white font-black text-base drop-shadow-sm">
            {fmtVND(item.price)}
          </span>
        </div>
        {!available && (
          <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
            <span className="px-3 py-1 rounded-full bg-white text-rose-600 text-xs font-black uppercase shadow-md">
              Hết hàng
            </span>
          </div>
        )}
      </div>

      <div className="p-3.5 flex flex-col flex-1 justify-between transition-colors duration-500">
        <div>
          <h3 className="font-bold text-[#2D1D16] dark:text-[#F7F0E7] text-[15px] line-clamp-1 leading-tight transition-colors">
            {item.name}
          </h3>
          <p className="text-xs text-[#9C8E85] dark:text-[#82756B] mt-1 line-clamp-2 transition-colors min-h-[2rem]">
            {item.description?.trim() || 'Chưa có mô tả món.'}
          </p>
        </div>

        <div className="mt-3 flex flex-col gap-2">
          <div
            className={`flex items-center rounded-full p-1 bg-[#F7F0E7] dark:bg-[#2A221E] transition-colors duration-500 ${item.itemOptions?.length > 0 ? 'justify-center w-[7.5rem] ml-auto' : 'justify-between'}`}
          >
            {item.itemOptions && item.itemOptions.length > 0 ? (
              <button
                onClick={() => onAddToCart(item, quantity + 1, true)}
                disabled={!available}
                className="w-full h-9 bg-[#56372B] hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537] rounded-full shadow-sm flex items-center justify-center text-white font-black text-xs uppercase tracking-wider px-3 transition-colors disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:text-slate-500"
              >
                {available ? 'Tùy chọn' : 'Hết hàng'}
              </button>
            ) : (
              <>
                <button
                  onClick={() => onAddToCart(item, quantity - 1)}
                  className={`w-9 h-9 rounded-full flex items-center justify-center font-bold transition-colors ${
                    quantity > 0
                      ? 'bg-white dark:bg-[#1F1A17] text-[#56372B] dark:text-[#C17C4A] shadow-sm'
                      : 'text-transparent pointer-events-none'
                  }`}
                  disabled={quantity === 0}
                >
                  <Minus size={15} />
                </button>

                <span
                  className={`text-sm font-bold text-[#56372B] dark:text-[#D8986A] min-w-[1.5rem] text-center transition-colors ${quantity === 0 ? 'opacity-0' : ''}`}
                >
                  {quantity}
                </span>

                <button
                  onClick={handlePlus}
                  disabled={!available}
                  className="w-9 h-9 bg-[#56372B] hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537] rounded-full shadow-sm flex items-center justify-center text-white font-bold active:scale-110 transition-all disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:text-slate-500 duration-150"
                >
                  <Plus size={15} />
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
