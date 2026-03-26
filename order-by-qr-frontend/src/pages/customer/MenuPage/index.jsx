import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useWebSocket } from '../../../hooks/useWebSocket.js';
import { fmtVND } from '../../../utils/formatters.js';

import { menuService } from '../../../services/customer/menuService.js';
import MenuCard from './MenuCard';
import ComboCard from './ComboCard';
import CategoryFilter from './CategoryFilter';
import ShoppingCartButton from './ShoppingCart';
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

  const wsMenu = useWebSocket('/topic/menu', handleRealtimeUpdate);
  const wsCombo = useWebSocket('/topic/combos', handleRealtimeUpdate);
  const wsCate = useWebSocket('/topic/categories', handleRealtimeUpdate);
  useEffect(() => {
    const checkInterval = setInterval(() => {
      // Kiểm tra xem socket có đang kết nối không
      setWsConnected(wsMenu?.connected || false);
    }, 2000);
    return () => clearInterval(checkInterval);
  }, [wsMenu]);

  const handleAddToCart = (product, qty, isCombo = false) => {
    setCart(prev => {
      const newCart = { ...prev };
      const group = isCombo ? 'combos' : 'items';
      if (qty <= 0) {
        delete newCart[group][product.id];
      } else {
        newCart[group][product.id] = {
          ...product,
          qty,
          note: prev[group][product.id]?.note || ""
        };

        // Gợi ý món đi kèm (Cross-sell)
        if (!isCombo && qty === 1) {
          menuService.getCrossSellRecommendations(product.id)
            .then(res => setCrossSellItems(res.data))
            .catch(() => { });
        }
      }
      return { ...newCart };
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
      items: Object.entries(cart.items).map(([id, i]) => ({ menuItemId: parseInt(id), quantity: i.qty, notes: i.note })),
      combos: Object.entries(cart.combos).map(([id, c]) => ({ comboId: parseInt(id), quantity: c.qty, notes: c.note }))
    };

    setIsSubmitting(true);
    try {
      await menuService.createOrder(orderData);
      alert('Đơn hàng của bạn đã được gửi thành công!');
      setCart({ items: {}, combos: {} });
      setShowOrderModal(false);
    } catch (e) {
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
                      quantity={cart.items[item.id]?.qty || 0}
                      onAddToCart={(i, q) => handleAddToCart(i, q, false)}
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
                quantity={cart.items[item.id]?.qty || 0}
                onAddToCart={(i, q) => handleAddToCart(i, q, false)}
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
        {showOrderModal && (
          <div className="fixed inset-0 bg-black/60 flex items-end z-50 animate-in fade-in duration-300">
            <div className="bg-white w-full max-w-md mx-auto rounded-t-[2rem] p-6 max-h-[85vh] flex flex-col shadow-2xl animate-in slide-in-from-bottom duration-500">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-lg font-bold flex items-center gap-2 text-gray-800">
                  <ShoppingBasket className="text-orange-500" /> Giỏ hàng của bạn
                </h3>
                <button onClick={() => setShowOrderModal(false)} className="p-2 bg-gray-100 rounded-full hover:bg-gray-200">
                  <X size={20} />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto pr-2 space-y-4">
                {/* Render món lẻ trong giỏ */}
                {Object.entries(cart.items).map(([id, item]) => (
                  <div key={id} className="border-b border-gray-100 pb-4">
                    <div className="flex justify-between items-center font-bold text-sm text-gray-800">
                      <span>{item.name} <span className="text-orange-500 ml-1">x{item.qty}</span></span>
                      <span>{fmtVND(item.qty * item.price)}</span>
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
                {Object.entries(cart.combos).map(([id, combo]) => (
                  <div key={id} className="mb-2 border-b border-gray-100 pb-4 bg-orange-50/50 p-3 rounded-2xl">
                    <div className="flex justify-between items-center font-bold text-sm text-orange-800">
                      <span>Combo {combo.name} <span className="text-orange-600 ml-1">x{combo.qty}</span></span>
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

              {/* Cross-sell trong giỏ hàng */}
              {crossSellItems.length > 0 && (
                <div className="mt-6 pt-4 border-t border-gray-100">
                  <div className="flex justify-between items-center mb-3">
                    <h4 className="text-[11px] font-bold text-orange-800 uppercase tracking-widest flex items-center gap-2">
                      <Sparkles size={14} className="fill-orange-500 text-orange-500" /> Thêm vào cho đủ vị?
                    </h4>
                    <button onClick={() => setCrossSellItems([])} className="text-gray-400 hover:text-gray-600 transition-colors">
                      <X size={16} />
                    </button>
                  </div>
                  <div className="flex gap-3 overflow-x-auto no-scrollbar pb-2">
                    {crossSellItems.map(item => (
                      <div key={item.id} className="min-w-[130px] bg-gray-50 p-3 rounded-2x border border-gray-100 hover:border-orange-200 transition-all flex flex-col justify-between">
                        <div>
                          <p className="text-[11px] font-semibold text-gray-700 leading-tight mb-2 line-clamp-2 h-8">{item.name}</p>
                        </div>
                        <button
                          onClick={() => handleAddToCart(item, (cart.items[item.id]?.qty || 0) + 1)}
                          className="w-full py-2 bg-orange-500 text-white text-[10px] font-extrabold rounded-xl shadow-md shadow-orange-100 active:scale-95 transition-all"
                        >
                          + {fmtVND(item.price)}
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Phần Tổng tiền và Submit */}
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
        )}
      </div>
    </div>
  );
};

export default MenuPage;