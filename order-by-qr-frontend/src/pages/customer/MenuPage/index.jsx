import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useWebSocket } from '../../../hooks/useWebSocket.js';
import { fmtVND } from '../../../utils/formatters.js';

import { menuService } from '../../../services/customer/menuService.js';
import MenuCard from './MenuCard';
import ComboCard from './ComboCard';
import CartModal from './CartModal';
import CategoryFilter from './CategoryFilter';
import ShoppingCartButton from './ShoppingCart';
import ItemOptionsModal from './ItemOptionsModal';
import { Loader2, ShoppingBasket, X, Wifi, WifiOff, Sparkles } from 'lucide-react';

const MenuPage = () => {
  const [searchParams] = useSearchParams();
  const tableCode = searchParams.get('tableCode');

  // Trạng thái dữ liệu
  const [tableInfo, setTableInfo] = useState(null);
  const [categories, setCategories] = useState([]);
  const [menuItems, setMenuItems] = useState([]);
  const [combos, setCombos] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [loading, setLoading] = useState(true);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const [recommendations, setRecommendations] = useState([]);
  const [crossSellItems, setCrossSellItems] = useState([]);

  // Trạng thái giỏ hàng
  const [cart, setCart] = useState({ items: {}, combos: {} });
  const [selectedItemForOptions, setSelectedItemForOptions] = useState(null);

  const getCartItemQty = (item) => {
    return Object.values(cart.items).filter(i => (i.actualId || i.id) === item.id).reduce((sum, i) => sum + i.qty, 0);
  };

  /**
   * HÀM TẢI DỮ LIỆU
   */
  const loadData = useCallback(async (showLoading = false) => {
    try {
      if (showLoading) setLoading(true);
      const [categoriesRes, menuRes, combosRes, tableRes] = await Promise.all([
        menuService.getCategories(),
        menuService.getAllMenuItems(),
        menuService.getCombos(),
        tableCode ? menuService.getTableByCode(tableCode) : Promise.resolve(null)
      ]);

      setCategories([...(categoriesRes.data || [])]);
      setMenuItems([...(menuRes.data || [])]);

      // Chỉ hiện Combo đang Active
      const activeCombos = (combosRes.data || []).filter(c => c.active !== false);
      setCombos([...activeCombos]);

      setTableInfo(tableRes?.data);
      // console.log(" Menu khách hàng đã được làm mới");
    } catch (error) {
      console.error('Lỗi tải dữ liệu API:', error);
    } finally {
      setLoading(false);
    }
  }, [tableCode]);

  useEffect(() => {
    loadData(true);

    // Gợi ý thông minh dựa trên ngữ cảnh
    const hour = new Date().getHours();
    const timeContext = hour < 11 ? "Sáng" : hour < 14 ? "Trưa" : hour < 18 ? "Chiều" : "Tối";
    const weatherContext = "Trời mát"; // Mock weather

    menuService.getPersonalizedRecommendations(timeContext, weatherContext)
      .then(res => setRecommendations(res.data))
      .catch(() => {
        menuService.getPopularItems().then(res => setRecommendations(res.data));
      });
  }, [loadData]);

  const handleRealtimeUpdate = useCallback((message) => {
    // Kiểm tra kỹ định dạng tin nhắn để tránh reload nhầm
    const signal = typeof message === 'string' ? message : message?.body;

    if (signal === 'UPDATED') {
      loadData(false); // Reload ngầm, không hiện loading quay quay gây khó chịu
    }
  }, [loadData]);

  useWebSocket('/topic/menu', handleRealtimeUpdate);
  useWebSocket('/topic/combos', handleRealtimeUpdate);
  useWebSocket('/topic/categories', handleRealtimeUpdate);
  const wsStatus = useWebSocket(); // Just to get the service reference for status check
  useEffect(() => {
    const checkInterval = setInterval(() => {
      // Kiểm tra xem socket có đang kết nối không
      setWsConnected(wsStatus?.connected || false);
    }, 2000);
    return () => clearInterval(checkInterval);
  }, [wsStatus]);

  const handleAddToCart = (product, qty, isCombo = false, needsOptions = false) => {
    if (needsOptions) {
      setSelectedItemForOptions(product);
      return;
    }

    setCart(prev => {
      const group = isCombo ? 'combos' : 'items';
      const id = product.id;

      // Copy sâu (deep copy) object group để không đụng vào state cũ
      const updatedGroup = { ...prev[group] };

      if (qty <= 0) {
        delete updatedGroup[id];
      } else {
        updatedGroup[id] = {
          ...product,
          actualId: product.id,
          qty,
          note: prev[group][id]?.note || ""
        };

        // Gợi ý món đi kèm (Cross-sell)
        if (!isCombo && qty === 1) {
          menuService.getCrossSellRecommendations(product.id)
            .then(res => setCrossSellItems(res.data))
            .catch(() => { });
        }
      }
      return { ...prev, [group]: updatedGroup };
    });
  };

  const handleAddWithOptions = (product, selectedValueIds, selectedOptionObjs, finalPrice) => {
    setCart(prev => {
      const cartId = product.id + '_' + selectedValueIds.join('_');
      const currentQty = prev.items[cartId]?.qty || 0;

      return {
        ...prev,
        items: {
          ...prev.items, // Trải phẳng dữ liệu cũ của items
          [cartId]: {    // Ghi đè item mới an toàn
            ...product,
            cartId,
            actualId: product.id,
            price: finalPrice,
            selectedOptionValueIds: selectedValueIds,
            selectedOptionObjs: selectedOptionObjs,
            qty: currentQty + 1,
            note: prev.items[cartId]?.note || ""
          }
        }
      };
    });
  };

  const handleUpdateCartItemQty = (cartId, qty) => {
    setCart(prev => {
      const updatedItems = { ...prev.items };
      if (qty <= 0) {
        delete updatedItems[cartId];
      } else {
        updatedItems[cartId] = {
          ...updatedItems[cartId],
          qty: qty
        };
      }
      return { ...prev, items: updatedItems };
    });
  };

  const handleUpdateNote = (id, note, isCombo = false) => {
    setCart(prev => {
      const group = isCombo ? 'combos' : 'items';
      return {
        ...prev,
        [group]: {
          ...prev[group],
          [id]: { ...prev[group][id], note }
        }
      };
    });
  };

  const calculateTotal = () => {
    const itTotal = Object.values(cart.items).reduce((s, i) => s + (i.qty * i.price), 0);
    const cbTotal = Object.values(cart.combos).reduce((s, c) => s + (c.qty * c.price), 0);
    return itTotal + cbTotal;
  };

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmitOrder = async () => {
    if (!tableCode) return alert('Lỗi: Vui lòng quét mã bàn để đặt món!');
    if (isSubmitting) return;

    const orderData = {
      tableCode,
      items: Object.entries(cart.items).map(([id, i]) => ({
        menuItemId: i.actualId || parseInt(id),
        quantity: i.qty,
        notes: i.note,
        selectedOptionValueIds: i.selectedOptionValueIds || []
      })),
      combos: Object.entries(cart.combos).map(([id, c]) => ({ comboId: parseInt(id), quantity: c.qty, notes: c.note }))
    };

    setIsSubmitting(true);
    try {
      await menuService.createOrder(orderData);
      alert('Đơn hàng của bạn đã được gửi thành công!');
      setCart({ items: {}, combos: {} });
      setShowOrderModal(false);
    } catch (_e) {
      alert('Có lỗi xảy ra khi gửi đơn hàng. Vui lòng thử lại!');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center text-orange-500 bg-gray-50">
      <div className="text-center">
        <Loader2 className="animate-spin mb-2 mx-auto" size={40} />
        <p className="text-gray-500 font-medium text-sm">Đang tải thực đơn...</p>
      </div>
    </div>
  );

  const displayItems = selectedCategory === 'all'
    ? menuItems
    : menuItems.filter(i => i.category?.id === parseInt(selectedCategory));

  return (
    <div className="min-h-screen bg-gray-100 flex justify-center">
      <div className="w-full max-w-md bg-white min-h-screen shadow-2xl relative flex flex-col">

        {/* Header Section */}
        <div className="bg-orange-500 text-white p-5 rounded-b-[2.5rem] shadow-lg">
          <div className="flex justify-between items-start">
            <div>
              <h1 className="text-xl font-black uppercase tracking-tighter">Sắc Màu Quán</h1>
              <div className="flex items-center gap-2 mt-1">
                <span className="bg-white/20 px-3 py-0.5 rounded-full text-[10px] font-bold border border-white/30 uppercase">
                  Bàn số: {tableInfo?.tableNumber || 'NaN'}
                </span>
              </div>
            </div>

            {/* Status Live Badge */}
            <div className={`flex items-center gap-1 px-3 py-1 rounded-full text-[10px] font-bold border transition-all duration-500 ${wsConnected ? 'bg-green-500/20 border-green-400' : 'bg-red-500/20 border-red-400'}`}>
              {wsConnected ? <Wifi size={12} className="animate-pulse" /> : <WifiOff size={12} />}
              {wsConnected ? 'LIVE' : 'OFFLINE'}
            </div>
          </div>
        </div>

        <div className="p-4 flex-1 overflow-y-auto pb-32">
          {/* Component lọc danh mục */}
          <CategoryFilter categories={categories} selectedCategory={selectedCategory} onSelectCategory={setSelectedCategory} />

          {/* Phần Combo */}
          {combos.length > 0 && (
            <div className="mt-4">
              <h2 className="text-sm font-bold text-gray-800 mb-3 flex items-center gap-2">Combo Khuyến Mãi</h2>
              <div className="flex flex-col gap-3">
                {combos.map(c => (
                  <ComboCard
                    key={c.id}
                    combo={c}
                    quantity={cart.combos[c.id]?.qty || 0}
                    onAddToCart={(cb, q) => handleAddToCart(cb, q, true)}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Phần Đề xuất thông minh */}
          {recommendations.length > 0 && selectedCategory === 'all' && (
            <div className="mt-6 mb-2">
              <h2 className="text-sm font-bold text-gray-800 mb-3 flex items-center gap-2">
                <Sparkles size={16} className="text-orange-500 fill-orange-500" /> Gợi ý cho bạn ({new Date().getHours() < 12 ? 'Bữa sáng' : 'Hôm nay'})
              </h2>
              <div className="flex overflow-x-auto gap-3 pb-2 no-scrollbar">
                {recommendations.map(item => (
                  <div key={item.id} className="min-w-[140px]">
                    <MenuCard
                      item={item}
                      quantity={getCartItemQty(item)}
                      onAddToCart={(i, q, needsOpt) => handleAddToCart(i, q, false, needsOpt)}
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Phần Món lẻ */}
          <h2 className="text-sm font-bold text-gray-800 mt-6 mb-3">Thực đơn chọn món</h2>
          <div className="grid grid-cols-2 gap-3">
            {displayItems.map(item => (
              <MenuCard
                key={item.id}
                item={item}
                quantity={getCartItemQty(item)}
                onAddToCart={(i, q, needsOpt) => handleAddToCart(i, q, false, needsOpt)}
              />
            ))}
          </div>

          {displayItems.length === 0 && (
            <div className="text-center py-10 text-gray-400 text-xs italic">
              Danh mục này hiện tại chưa có món.
            </div>
          )}
        </div>

        {/* Nút Giỏ hàng nổi */}
        <ShoppingCartButton cart={cart} onOpenCart={() => setShowOrderModal(true)} />

        {/* Modal giỏ hàng & Thanh toán */}
        <CartModal
          isOpen={showOrderModal}
          onClose={() => setShowOrderModal(false)}
          cart={cart}
          fmtVND={fmtVND}
          handleUpdateCartItemQty={handleUpdateCartItemQty}
          handleUpdateNote={handleUpdateNote}
          crossSellItems={crossSellItems}
          setCrossSellItems={setCrossSellItems}
          handleAddToCart={handleAddToCart}
          calculateTotal={calculateTotal}
          isSubmitting={isSubmitting}
          handleSubmitOrder={handleSubmitOrder}
        />

        <ItemOptionsModal
          key={selectedItemForOptions ? selectedItemForOptions.id : 'closed'}
          item={selectedItemForOptions}
          isOpen={!!selectedItemForOptions}
          onClose={() => setSelectedItemForOptions(null)}
          onConfirm={handleAddWithOptions}
        />
      </div>
    </div>
  );
};

export default MenuPage;