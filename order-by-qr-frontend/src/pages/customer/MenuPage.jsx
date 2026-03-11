import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

import { useWebSocket } from '../../hooks/useWebSocket.js'; 

import { menuService } from '../../services/customer/menuService.js';
import MenuCard from '../../components/customer/MenuCard';
import ComboCard from '../../components/customer/ComboCard';
import CategoryFilter from '../../components/customer/CategoryFilter';
import ShoppingCartButton from '../../components/customer/ShoppingCart';
import { Loader2, ShoppingBasket, X, Wifi, WifiOff } from 'lucide-react';

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
      // console.log("♻️ [Realtime] Menu khách hàng đã được làm mới");
    } catch (error) {
      console.error('Lỗi tải dữ liệu API:', error);
    } finally {
      setLoading(false);
    }
  }, [tableCode]);

  useEffect(() => {
    loadData(true);
  }, [loadData]);

  /**
   * === WEBSOCKET REALTIME ===
   * Logic: Khi Admin sửa Món/Danh mục/Combo -> Server bắn "UPDATED" -> Khách hàng tự load lại
   */
  const handleRealtimeUpdate = useCallback((message) => {
    // Kiểm tra kỹ định dạng tin nhắn để tránh reload nhầm
    const signal = typeof message === 'string' ? message : message?.body;
    
    if (signal === 'UPDATED') {
        // console.log('⚡ Có cập nhật từ Admin -> Reload menu...');
        loadData(false); // Reload ngầm, không hiện loading quay quay gây khó chịu
    }
  }, [loadData]);

  // Đăng ký nghe 3 kênh quan trọng
  const wsMenu = useWebSocket('/topic/menu', handleRealtimeUpdate);
  const wsCombo = useWebSocket('/topic/combos', handleRealtimeUpdate);
  const wsCate = useWebSocket('/topic/categories', handleRealtimeUpdate);

  // Kiểm tra kết nối (Lấy trạng thái từ bất kỳ hook nào cũng được)
  useEffect(() => {
    const checkInterval = setInterval(() => {
        // Kiểm tra xem socket có đang kết nối không
        setWsConnected(wsMenu?.connected || false);
    }, 2000);
    return () => clearInterval(checkInterval);
  }, [wsMenu]);

  // ... (Phần logic Cart, Render giữ nguyên như code của bạn) ...
  // Để tiết kiệm dòng dòng, phần dưới giữ nguyên, chỉ thay đổi phần logic WebSocket ở trên thôi nhé.
  
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

  const handleSubmitOrder = async () => {
    if (!tableCode) return alert('Lỗi: Vui lòng quét mã bàn để đặt món!');
    const orderData = {
      tableCode,
      items: Object.entries(cart.items).map(([id, i]) => ({ menuItemId: parseInt(id), quantity: i.qty, notes: i.note })),
      combos: Object.entries(cart.combos).map(([id, c]) => ({ comboId: parseInt(id), quantity: c.qty, notes: c.note }))
    };
    try {
      await menuService.createOrder(orderData);
      alert('Đơn hàng của bạn đã được gửi thành công!');
      setCart({ items: {}, combos: {} });
      setShowOrderModal(false);
    } catch (e) { 
        alert('Có lỗi xảy ra khi gửi đơn hàng. Vui lòng thử lại!'); 
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
                  Bàn số: {tableInfo?.tableNumber || '??'}
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
              <h2 className="text-sm font-bold text-gray-800 mb-3 flex items-center gap-2">🔥 Combo Khuyến Mãi</h2>
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
                  <X size={20}/>
                </button>
              </div>

              <div className="flex-1 overflow-y-auto pr-2 space-y-4">
                {/* Render món lẻ trong giỏ */}
                {Object.entries(cart.items).map(([id, item]) => (
                  <div key={id} className="border-b border-gray-100 pb-4">
                    <div className="flex justify-between items-center font-bold text-sm text-gray-800">
                      <span>{item.name} <span className="text-orange-500 ml-1">x{item.qty}</span></span>
                      <span>{(item.qty * item.price).toLocaleString()}₫</span>
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
                      <span>{(combo.qty * combo.price).toLocaleString()}₫</span>
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

              {/* Phần Tổng tiền và Submit */}
              <div className="mt-6 pt-4 border-t border-gray-200">
                <div className="flex justify-between items-center mb-4">
                  <span className="font-bold text-gray-500 uppercase text-xs tracking-wider">Tổng cộng</span>
                  <span className="text-2xl font-black text-orange-600">
                    {calculateTotal().toLocaleString()}₫
                  </span>
                </div>
                <button 
                  onClick={handleSubmitOrder} 
                  className="w-full bg-orange-500 text-white py-4 rounded-2xl font-bold shadow-lg shadow-orange-200 active:scale-95 transition-transform"
                >
                  XÁC NHẬN ĐẶT MÓN
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