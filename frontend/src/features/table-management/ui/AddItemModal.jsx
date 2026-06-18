import React, { useState, useEffect } from 'react';
import { toast } from 'react-hot-toast';
import { Loader2, X } from 'lucide-react';
import { tableOrderCatalogService } from '@features/table-management/api/tableOrderCatalogService.js';
import ItemOptionsModal from '@features/customer-ordering/ui/ItemOptionsModal.jsx';
import SharedModal from '@shared/ui/SharedModal.jsx';

const AddItemModal = ({ isOpen, onClose, table, onSubmit, isSubmitting }) => {
  const [activeTab, setActiveTab] = useState('ITEMS'); // ITEMS | COMBOS
  const [menuItems, setMenuItems] = useState([]);
  const [combos, setCombos] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCate, setSelectedCate] = useState('ALL');
  const [cart, setCart] = useState([]); // [{cartId, id, type, name, price, qty, notes, options}]
  const [catalogLoading, setCatalogLoading] = useState(false);

  // State cho Modal chọn Options
  const [selectedItemForOptions, setSelectedItemForOptions] = useState(null);
  const [isOptionsModalOpen, setIsOptionsModalOpen] = useState(false);

  useEffect(() => {
    let isCancelled = false;

    const loadAllData = async () => {
      setCatalogLoading(true);
      try {
        const catalog = await tableOrderCatalogService.getCatalog();
        if (isCancelled) return;

        setMenuItems(Array.isArray(catalog?.menuItems) ? catalog.menuItems : []);
        setCombos(Array.isArray(catalog?.combos) ? catalog.combos : []);
        setCategories(Array.isArray(catalog?.categories) ? catalog.categories : []);
      } catch {
        console.error('Lỗi tải menu/combo');
      } finally {
        if (!isCancelled) setCatalogLoading(false);
      }
    };

    if (isOpen) {
      loadAllData();
    }

    return () => {
      isCancelled = true;
    };
  }, [isOpen]);

  const addToCart = (item, type, options = [], optionObjs = [], totalPrice = null) => {
    const priceToUse = totalPrice !== null ? totalPrice : item.price;
    const optionKey = options.sort().join(',');
    const cartId = `${item.id}-${type}-${optionKey}`;

    const exist = cart.find((x) => x.cartId === cartId);
    if (exist) {
      setCart(cart.map((x) => (x.cartId === cartId ? { ...x, qty: x.qty + 1 } : x)));
    } else {
      const addedName =
        item.name +
        (optionObjs.length > 0 ? ` (${optionObjs.map((o) => o.valueName).join(', ')})` : '');
      setCart([
        ...cart,
        {
          cartId,
          id: item.id,
          type,
          name: addedName,
          price: priceToUse,
          qty: 1,
          notes: '',
          options: options.map((optId) => ({ optionValueId: optId })),
        },
      ]);
    }
  };

  const handleItemClick = (item, type) => {
    if (type === 'ITEM' && item.itemOptions && item.itemOptions.length > 0) {
      setSelectedItemForOptions(item);
      setIsOptionsModalOpen(true);
    } else {
      addToCart(item, type);
    }
  };

  const handleConfirmOptions = (item, selectedValueIds, selectedOptionObjs, totalPrice) => {
    addToCart(item, 'ITEM', selectedValueIds, selectedOptionObjs, totalPrice);
  };

  const removeFromCart = (idx) => {
    setCart(cart.filter((_, i) => i !== idx));
  };

  const handleConfirm = () => {
    if (cart.length === 0) return toast.error('Chưa chọn món nào');
    const payload = {
      tableId: table.id,
      items: cart
        .filter((x) => x.type === 'ITEM')
        .map((x) => ({
          menuItemId: x.id,
          quantity: x.qty,
          notes: x.notes,
          selectedOptionValueIds: (x.options || []).map((opt) => opt.optionValueId),
        })),
      combos: cart
        .filter((x) => x.type === 'COMBO')
        .map((x) => ({ comboId: x.id, quantity: x.qty, notes: x.notes })),
    };
    onSubmit(payload);
  };

  if (!isOpen || !table) return null;

  const displayList =
    activeTab === 'ITEMS'
      ? menuItems.filter((i) => selectedCate === 'ALL' || i.category?.id === selectedCate)
      : combos;

  return (
    <>
      <SharedModal
        isOpen={isOpen}
        onClose={onClose}
        className="max-w-5xl !h-[90vh] !flex-row !p-0 overflow-hidden !rounded-2xl"
      >
        {/* LEFT: Menu Selection */}
        <div className="flex-1 flex flex-col border-r bg-gray-50 min-w-0">
          <div className="p-4 bg-white border-b">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-lg">Thêm món - Bàn {table.tableNumber}</h3>
            </div>
            <div className="flex gap-2 mb-4">
              <button
                onClick={() => setActiveTab('ITEMS')}
                className={`flex-1 py-2 rounded-lg font-bold ${activeTab === 'ITEMS' ? 'bg-orange-500 text-white' : 'bg-gray-200 text-gray-600'}`}
              >
                Món lẻ
              </button>
              <button
                onClick={() => setActiveTab('COMBOS')}
                className={`flex-1 py-2 rounded-lg font-bold ${activeTab === 'COMBOS' ? 'bg-orange-500 text-white' : 'bg-gray-200 text-gray-600'}`}
              >
                Combo
              </button>
            </div>

            {activeTab === 'ITEMS' && (
              <div className="flex gap-2 overflow-x-auto pb-2 custom-scrollbar">
                <button
                  onClick={() => setSelectedCate('ALL')}
                  className={`whitespace-nowrap px-3 py-1 rounded-full text-sm border ${selectedCate === 'ALL' ? 'bg-orange-100 border-orange-500 text-orange-700' : 'bg-white'}`}
                >
                  Tất cả
                </button>
                {categories.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => setSelectedCate(c.id)}
                    className={`whitespace-nowrap px-3 py-1 rounded-full text-sm border ${selectedCate === c.id ? 'bg-orange-100 border-orange-500 text-orange-700' : 'bg-white'}`}
                  >
                    {c.name}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="flex-1 overflow-y-auto p-4 grid grid-cols-2 lg:grid-cols-3 gap-3 content-start">
            {catalogLoading ? (
              <div className="col-span-full flex h-52 items-center justify-center text-gray-400">
                <Loader2 className="mr-2 h-5 w-5 animate-spin text-orange-500" />
                <span className="text-sm font-bold">Đang tải món...</span>
              </div>
            ) : (
              displayList.map((item) => {
                const available = item.available !== false;

                return (
                  <button
                    key={item.id}
                    type="button"
                    disabled={!available}
                    onClick={() =>
                      available && handleItemClick(item, activeTab === 'ITEMS' ? 'ITEM' : 'COMBO')
                    }
                    className={`min-h-[80px] rounded-xl border bg-white p-3 text-left shadow-sm transition-all flex flex-col justify-between ${
                      available
                        ? 'cursor-pointer hover:border-orange-500 hover:shadow-md active:scale-95'
                        : 'cursor-not-allowed opacity-55'
                    }`}
                  >
                    <div className="font-bold text-gray-800 text-sm leading-tight">{item.name}</div>
                    <div className="mt-2 flex items-center justify-between gap-2">
                      <span className="text-orange-600 font-bold text-sm">
                        {item.price.toLocaleString()}đ
                      </span>
                      {!available && (
                        <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-black uppercase text-gray-500">
                          Hết kho
                        </span>
                      )}
                    </div>
                  </button>
                );
              })
            )}
            {!catalogLoading && displayList.length === 0 && (
              <div className="col-span-full flex h-52 items-center justify-center text-sm font-bold text-gray-400">
                Không có món phù hợp
              </div>
            )}
          </div>
        </div>

        {/* RIGHT: Cart */}
        <div className="w-[350px] flex flex-col bg-white shrink-0">
          <div className="p-4 border-b bg-orange-50 flex justify-between items-center">
            <h3 className="font-bold text-orange-800">Món đã chọn ({cart.length})</h3>
            <button
              onClick={onClose}
              className="p-1 hover:bg-orange-100 rounded-lg transition-colors"
            >
              <X size={20} className="text-orange-600" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {cart.length === 0 && (
              <p className="text-center text-gray-400 text-sm mt-10">Chưa chọn món nào</p>
            )}
            {cart.map((item, idx) => (
              <div key={idx} className="flex flex-col gap-2 border-b pb-3 last:border-0">
                <div className="flex justify-between items-start">
                  <div className="font-bold text-sm">{item.name}</div>
                  <div className="text-sm font-medium">{item.price.toLocaleString()}</div>
                </div>
                <div className="flex gap-2 items-center">
                  <input
                    type="number"
                    min="1"
                    className="w-12 p-1 border rounded text-center text-sm"
                    value={item.qty}
                    onChange={(e) => {
                      const newCart = [...cart];
                      newCart[idx].qty = parseInt(e.target.value) || 1;
                      setCart(newCart);
                    }}
                  />
                  <input
                    type="text"
                    className="flex-1 p-1 border rounded text-sm"
                    placeholder="Ghi chú..."
                    value={item.notes}
                    onChange={(e) => {
                      const newCart = [...cart];
                      newCart[idx].notes = e.target.value;
                      setCart(newCart);
                    }}
                  />
                  <button
                    onClick={() => removeFromCart(idx)}
                    className="text-red-500 hover:bg-red-50 p-1 rounded shrink-0 transition-colors"
                    title="Xóa khỏi giỏ"
                  >
                    <X size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="p-4 border-t bg-gray-50">
            <div className="flex justify-between text-lg font-bold mb-4">
              <span>Tổng tạm tính:</span>
              <span className="text-orange-600">
                {cart.reduce((s, i) => s + i.price * i.qty, 0).toLocaleString()}đ
              </span>
            </div>
            <button
              onClick={handleConfirm}
              disabled={isSubmitting}
              className={`w-full py-3 text-white font-bold rounded-xl shadow-lg shadow-orange-200 ${isSubmitting ? 'bg-orange-300 cursor-not-allowed' : 'bg-orange-500 hover:bg-orange-600'}`}
            >
              {isSubmitting ? 'Đang xử lý...' : 'Xác nhận thêm món'}
            </button>
          </div>
        </div>
      </SharedModal>

      {/* Modal Chọn Options của món (dùng chung từ khách hàng) */}
      <ItemOptionsModal
        key={isOptionsModalOpen ? selectedItemForOptions?.id || 'new' : 'closed'}
        item={selectedItemForOptions}
        isOpen={isOptionsModalOpen}
        onClose={() => {
          setIsOptionsModalOpen(false);
          setSelectedItemForOptions(null);
        }}
        onConfirm={handleConfirmOptions}
      />
    </>
  );
};

export default AddItemModal;
