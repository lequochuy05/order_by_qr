import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Languages, Loader2, Wifi, WifiOff, Sparkles, Moon, Sun } from 'lucide-react';

import { useWebSocket } from '../../../hooks/useWebSocket.js';
import wsService from '../../../services/websocket.js';
import { useStatusModal } from '../../../hooks/useStatusModal.js';
import { fmtVND } from '../../../utils/formatters.js';

import { menuService } from '../../../services/customer/menuService.js';
import MenuCard from './MenuCard';
import ComboCard from './ComboCard';
import CartModal from './CartModal';
import CategoryFilter from './CategoryFilter';
import CurrentOrderBanner from './CurrentOrderBanner';
import CurrentOrderSheet from './CurrentOrderSheet';
import ShoppingCartButton from './ShoppingCart';
import ItemOptionsModal from './ItemOptionsModal';
import AiChatAssistant from '../../../components/customer/AiChatAssistant';
import StatusModal from '../../../components/admin/common/StatusModal';

const defaultRestaurantSettings = {
  restaurantName: 'Sắc Màu Quán',
  restaurantPhone: '',
  restaurantLogoUrl: '',
  enableAiAssistant: true
};

const customerLanguageKey = 'customer_menu_language';

const customerCopy = {
  vi: {
    loadingMenu: 'Đang tải thực đơn...',
    table: 'Bàn số',
    live: 'LIVE',
    offline: 'OFFLINE',
    all: 'Tất cả',
    comboTitle: 'Combo Khuyến Mãi',
    recommendations: 'Gợi ý cho bạn',
    menu: 'Thực đơn',
    emptyCategory: 'Danh mục này hiện tại chưa có món.',
    time: {
      morning: 'Sáng',
      noon: 'Trưa',
      afternoon: 'Chiều',
      evening: 'Tối'
    },
    errors: {
      missingTable: 'Vui lòng quét mã QR trên bàn để đặt món.',
      missingTableTitle: 'Chưa xác định bàn',
      emptyCart: 'Giỏ hàng của bạn đang trống. Hãy chọn món trước khi đặt.',
      emptyCartTitle: 'Chưa có món',
      submitTitle: 'Không thể gửi đơn hàng'
    },
    success: {
      orderSent: 'Đơn hàng của bạn đã được gửi đến quán.',
      orderSentTitle: 'Đặt món thành công'
    },
    cartButton: 'Xem giỏ hàng',
    comboCard: {
      badge: 'COMBO',
      helper: 'Tiết kiệm hơn',
      priceLabel: 'Giá chỉ còn'
    },
    menuCard: {
      helper: 'Thơm ngon nóng hổi...',
      options: 'Tùy chọn'
    },
    itemOptions: {
      required: 'Bắt buộc',
      basePrice: 'Giá gốc',
      subtotal: 'Tạm tính',
      addToCart: 'Thêm vào giỏ',
      maxSelectionTitle: 'Vượt quá số lựa chọn',
      maxSelection: (max) => `Bạn chỉ được chọn tối đa ${max} mục cho phần này.`,
      requiredTitle: 'Thiếu lựa chọn bắt buộc',
      requiredMessage: (name) => `Vui lòng chọn ${name} trước khi thêm vào giỏ.`
    },
    cart: {
      title: 'Giỏ hàng của bạn',
      optionPrefix: 'Tùy chọn',
      itemNote: 'Ghi chú thêm (cay, ít đá...)',
      comboNote: 'Ghi chú cho combo...',
      crossSell: 'Thêm vào cho đủ vị?',
      total: 'Tổng cộng',
      processing: 'ĐANG XỬ LÝ...',
      submit: 'XÁC NHẬN ĐẶT MÓN'
    },
    order: {
      order: 'Đơn',
      items: 'món',
      subtotal: 'Tạm tính',
      empty: 'Đơn hiện tại chưa có món.',
      close: 'Đóng chi tiết đơn',
      itemFallback: 'Món đã gọi',
      orderStatus: {
        PENDING: { label: 'Đã nhận đơn', helper: 'Quán đang kiểm tra món' },
        SERVING: { label: 'Đang chuẩn bị', helper: 'Bếp đang làm món cho bàn' },
        COMPLETED: { label: 'Đã thanh toán', helper: 'Cảm ơn bạn đã dùng bữa' },
        CANCELLED: { label: 'Đã hủy', helper: 'Đơn không còn hoạt động' },
        fallback: { label: 'Đang cập nhật', helper: 'Đơn hàng đang được đồng bộ' }
      },
      itemStatus: {
        PENDING: 'Chờ bếp',
        COOKING: 'Đang làm',
        READY: 'Sẵn sàng',
        SERVED: 'Đã phục vụ',
        FINISHED: 'Hoàn tất',
        CANCELLED: 'Đã hủy',
        fallback: 'Đang cập nhật'
      }
    }
  },
  en: {
    loadingMenu: 'Loading menu...',
    table: 'Table',
    live: 'LIVE',
    offline: 'OFFLINE',
    all: 'All',
    comboTitle: 'Promotional Combos',
    recommendations: 'Recommended for you',
    menu: 'Menu',
    emptyCategory: 'No items in this category yet.',
    time: {
      morning: 'Morning',
      noon: 'Noon',
      afternoon: 'Afternoon',
      evening: 'Evening'
    },
    errors: {
      missingTable: 'Please scan the QR code on your table to order.',
      missingTableTitle: 'Table not found',
      emptyCart: 'Your cart is empty. Please choose an item first.',
      emptyCartTitle: 'No items selected',
      submitTitle: 'Unable to send order'
    },
    success: {
      orderSent: 'Your order has been sent to the restaurant.',
      orderSentTitle: 'Order placed'
    },
    cartButton: 'View cart',
    comboCard: {
      badge: 'COMBO',
      helper: 'Better value',
      priceLabel: 'Only'
    },
    menuCard: {
      helper: 'Fresh and tasty...',
      options: 'Options'
    },
    itemOptions: {
      required: 'Required',
      basePrice: 'Base price',
      subtotal: 'Subtotal',
      addToCart: 'Add to cart',
      maxSelectionTitle: 'Too many selections',
      maxSelection: (max) => `You can choose up to ${max} options here.`,
      requiredTitle: 'Required option missing',
      requiredMessage: (name) => `Please choose ${name} before adding to cart.`
    },
    cart: {
      title: 'Your cart',
      optionPrefix: 'Options',
      itemNote: 'Add a note (spicy, less ice...)',
      comboNote: 'Add a note for this combo...',
      crossSell: 'Add something extra?',
      total: 'Total',
      processing: 'PROCESSING...',
      submit: 'PLACE ORDER'
    },
    order: {
      order: 'Order',
      items: 'items',
      subtotal: 'Subtotal',
      empty: 'This order has no items yet.',
      close: 'Close order details',
      itemFallback: 'Ordered item',
      orderStatus: {
        PENDING: { label: 'Received', helper: 'The restaurant is checking your order' },
        SERVING: { label: 'Preparing', helper: 'The kitchen is preparing your table' },
        COMPLETED: { label: 'Paid', helper: 'Thank you for dining with us' },
        CANCELLED: { label: 'Cancelled', helper: 'This order is no longer active' },
        fallback: { label: 'Updating', helper: 'Your order is being synced' }
      },
      itemStatus: {
        PENDING: 'Waiting',
        COOKING: 'Cooking',
        READY: 'Ready',
        SERVED: 'Served',
        FINISHED: 'Done',
        CANCELLED: 'Cancelled',
        fallback: 'Updating'
      }
    }
  }
};

const MenuPage = () => {
  const [searchParams] = useSearchParams();
  const tableCode = searchParams.get('tableCode');

  // Trạng thái dữ liệu
  const [tableInfo, setTableInfo] = useState(null);
  const [categories, setCategories] = useState([]);
  const [menuItems, setMenuItems] = useState([]);
  const [combos, setCombos] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [showCurrentOrder, setShowCurrentOrder] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const [recommendations, setRecommendations] = useState([]);
  const [crossSellItems, setCrossSellItems] = useState([]);
  const [restaurantSettings, setRestaurantSettings] = useState(defaultRestaurantSettings);
  const [currentOrder, setCurrentOrder] = useState(null);
  const [language, setLanguage] = useState(() => {
    const saved = localStorage.getItem(customerLanguageKey);
    return saved === 'en' ? 'en' : 'vi';
  });
  const copy = customerCopy[language] || customerCopy.vi;
  const hour = new Date().getHours();
  const timeContext = hour < 10 ? "Sáng" : hour < 14 ? "Trưa" : hour < 18 ? "Chiều" : "Tối";
  const displayTimeContext = hour < 10
    ? copy.time.morning
    : hour < 14
      ? copy.time.noon
      : hour < 18
        ? copy.time.afternoon
        : copy.time.evening;
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

  // Trạng thái giỏ hàng
  const [cart, setCart] = useState({ items: {}, combos: {} });
  const [selectedItemForOptions, setSelectedItemForOptions] = useState(null);

  const getCartItemQty = (item) => {
    return Object.values(cart.items).filter(i => (i.actualId || i.id) === item.id).reduce((sum, i) => sum + i.qty, 0);
  };

  /**
   * HÀM TẢI DỮ LIỆU
   */
  const loadCurrentOrder = useCallback(async (tableId) => {
    if (!tableId) {
      setCurrentOrder(null);
      return null;
    }

    try {
      const order = await menuService.getCurrentOrderByTable(tableId);
      setCurrentOrder(order || null);
      return order || null;
    } catch (error) {
      console.error('Lỗi tải đơn hiện tại:', error);
      return null;
    }
  }, []);

  const loadData = useCallback(async (showLoading = false) => {
    try {
      if (showLoading) setLoading(true);
      const [categoriesRes, menuRes, combosRes, settingsRes, tableRes] = await Promise.all([
        menuService.getCategories(),
        menuService.getAllMenuItems(),
        menuService.getCombos(),
        menuService.getSettings(),
        tableCode ? menuService.getTableByCode(tableCode) : Promise.resolve(null)
      ]);

      setCategories(Array.isArray(categoriesRes) ? categoriesRes : []);
      setMenuItems(Array.isArray(menuRes) ? menuRes : []);

      // Chỉ hiện Combo đang Active
      const activeCombos = (Array.isArray(combosRes) ? combosRes : []).filter(c => c.active !== false);
      setCombos(activeCombos);
      setRestaurantSettings({
        ...defaultRestaurantSettings,
        ...(settingsRes || {})
      });

      setTableInfo(tableRes);
      if (tableRes?.id) {
        await loadCurrentOrder(tableRes.id);
      } else {
        setCurrentOrder(null);
      }
      // console.log(" Menu khách hàng đã được làm mới");
    } catch (error) {
      console.error('Lỗi tải dữ liệu API:', error);
    } finally {
      setLoading(false);
    }
  }, [tableCode, loadCurrentOrder]);

  const loadRecommendations = useCallback(async () => {
    try {
      menuService.getPersonalizedRecommendations(timeContext)
        .then(res => setRecommendations(Array.isArray(res) ? res : []))
        .catch(() => {
          menuService.getPopularItems().then(res => setRecommendations(Array.isArray(res) ? res : []));
        });
    } catch {
      // suppress
    }
  }, [timeContext]);

  useEffect(() => {
    loadData(true);
    loadRecommendations();
  }, [loadData, loadRecommendations]);

  useEffect(() => {
    localStorage.setItem(customerLanguageKey, language);
    document.documentElement.lang = language;
  }, [language]);

  useEffect(() => {
    if (!currentOrder) {
      setShowCurrentOrder(false);
    }
  }, [currentOrder]);

  const handleRealtimeUpdate = useCallback((message) => {
    // message đã được JSON.parse bởi wsService.
    // Nếu là chuỗi 'UPDATED' hoặc là một object (thông báo thay đổi cụ thể), ta tiến hành tải lại data.
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      loadData(false);
      loadRecommendations();
    }
  }, [loadData, loadRecommendations]);

  const handleSettingsRealtimeUpdate = useCallback((message) => {
    if (message?.event === 'SETTINGS_UPDATED' && message.settings) {
      setRestaurantSettings({
        ...defaultRestaurantSettings,
        ...message.settings
      });
      return;
    }

    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      loadData(false);
    }
  }, [loadData]);

  const handleOrderRealtimeUpdate = useCallback((message) => {
    if (!tableInfo?.id) return;

    if (message?.event === 'PAYMENT_SUCCESS' && message.orderId === currentOrder?.id) {
      setCurrentOrder(prev => prev ? {
        ...prev,
        status: 'COMPLETED',
        paymentStatus: 'PAID'
      } : prev);
      return;
    }

    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      loadCurrentOrder(tableInfo.id);
    }
  }, [currentOrder?.id, loadCurrentOrder, tableInfo?.id]);

  useWebSocket('/topic/menu', handleRealtimeUpdate);
  useWebSocket('/topic/combos', handleRealtimeUpdate);
  useWebSocket('/topic/categories', handleRealtimeUpdate);
  useWebSocket('/topic/settings', handleSettingsRealtimeUpdate);
  useWebSocket('/topic/tables', handleOrderRealtimeUpdate);

  // Instant WS status via listener (no polling)
  useEffect(() => {
    const wsRef = wsService;
    setWsConnected(wsRef.isConnected());
    return wsRef.addConnectListener((connected) => {
      setWsConnected(connected);
    });
  }, []);

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
            .then(res => setCrossSellItems(Array.isArray(res) ? res : []))
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

    menuService.getCrossSellRecommendations(product.id)
      .then(res => setCrossSellItems(Array.isArray(res) ? res : []))
      .catch(() => { });
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
    if (!tableCode) {
      showError(copy.errors.missingTable, copy.errors.missingTableTitle);
      return;
    }
    if (isSubmitting) return;

    if (Object.keys(cart.items).length === 0 && Object.keys(cart.combos).length === 0) {
      showError(copy.errors.emptyCart, copy.errors.emptyCartTitle);
      return;
    }

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
      const order = await menuService.createOrder(orderData);
      setCurrentOrder(order || null);
      setCart({ items: {}, combos: {} });
      setShowOrderModal(false);
      showSuccess(copy.success.orderSent, copy.success.orderSentTitle);
    } catch (e) {
      showError(e, copy.errors.submitTitle);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center text-orange-500 bg-gray-50">
      <div className="text-center">
        <Loader2 className="animate-spin mb-2 mx-auto" size={40} />
        <p className="text-gray-500 font-medium text-sm">{copy.loadingMenu}</p>
      </div>
    </div>
  );

  const displayItems = selectedCategory === 'all'
    ? menuItems
    : menuItems.filter(i => i.category?.id === parseInt(selectedCategory));

  return (
    <div className={isDarkMode ? 'dark' : ''}>
      <div className="min-h-screen bg-gray-100 dark:bg-slate-950 flex justify-center transition-colors duration-500">
        <div className="w-full max-w-md bg-white dark:bg-slate-900 min-h-screen shadow-2xl relative flex flex-col transition-colors duration-500">

          {/* Header Section */}
          <div className="bg-orange-500 text-white p-3 rounded-b-2xl shadow-lg">
            <div className="flex justify-between items-start">
              <div className="flex items-start gap-3 min-w-0">
                {restaurantSettings.restaurantLogoUrl && (
                  <img
                    src={restaurantSettings.restaurantLogoUrl}
                    alt={restaurantSettings.restaurantName}
                    className="h-8 w-8 rounded-lg border border-white/30 bg-white/20 object-cover"
                  />
                )}
                <div className="min-w-0">
                  <h1 className="text-base font-black uppercase tracking-tighter truncate">
                    {restaurantSettings.restaurantName || 'Sắc Màu Quán'}
                  </h1>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="bg-white/20 px-2.5 py-0.5 rounded-full text-[10px] font-bold border border-white/30 uppercase">
                      {copy.table}: {tableInfo?.tableNumber || 'NaN'}
                    </span>
                    {/* {restaurantSettings.restaurantPhone && (    // Tạm thời ẩn số điện thoại
                    <span className="hidden sm:inline bg-white/20 px-3 py-0.5 rounded-full text-[10px] font-bold border border-white/30">
                      {restaurantSettings.restaurantPhone}
                    </span>
                  )} */}
                  </div>
                </div>
              </div>

              {/* Trạng thái & Toggle Dark Mode */}
              <div className="flex flex-col items-end gap-1.5">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsDarkMode(!isDarkMode)}
                    className="w-7 h-7 rounded-full bg-white/20 border border-white/30 flex items-center justify-center text-white cursor-pointer hover:bg-white/30 transition-all duration-300 shadow-sm"
                    aria-label="Toggle dark mode"
                  >
                    {isDarkMode ? <Sun size={14} className="animate-spin-slow" /> : <Moon size={14} />}
                  </button>

                  <div className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold border transition-all duration-500 ${wsConnected ? 'bg-green-500/20 border-green-400' : 'bg-red-500/20 border-red-400'}`}>
                    {wsConnected ? <Wifi size={12} className="animate-pulse" /> : <WifiOff size={12} />}
                    {wsConnected ? copy.live : copy.offline}
                  </div>
                </div>

                <div className="flex items-center gap-1 rounded-full border border-white/30 bg-white/15 p-0.5 text-[10px] font-black text-white shadow-sm">
                  <Languages size={11} className="ml-1 opacity-80" />
                  {['vi', 'en'].map(option => (
                    <button
                      key={option}
                      type="button"
                      onClick={() => setLanguage(option)}
                      className={`rounded-full px-2 py-0.5 uppercase transition ${language === option
                        ? 'bg-white text-orange-600'
                        : 'text-white/80 hover:bg-white/10'
                        }`}
                      aria-pressed={language === option}
                    >
                      {option}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {currentOrder && (
            <div className="sticky top-0 z-40 bg-white/95 px-3 py-2 shadow-sm backdrop-blur-md transition-colors dark:bg-slate-900/95">
              <CurrentOrderBanner
                order={currentOrder}
                copy={copy.order}
                language={language}
                onClick={() => setShowCurrentOrder(true)}
              />
            </div>
          )}

          <div className="p-3 flex-1 overflow-y-auto pb-32">
            {/* Component lọc danh mục */}
            <CategoryFilter categories={categories} selectedCategory={selectedCategory} onSelectCategory={setSelectedCategory} labels={{ all: copy.all }} />

            {/* Phần Combo */}
            {combos.length > 0 && (
              <div className="mt-2 mb-2">
                <h2 className="text-xs font-black text-gray-800 dark:text-white mb-2 flex items-center gap-2 uppercase transition-colors">{copy.comboTitle}</h2>
                <div className="flex overflow-x-auto gap-2.5 pb-2 no-scrollbar animate-in fade-in duration-500">
                  {combos.map(c => (
                    <div key={c.id} className="min-w-[min(58vw,220px)]">
                      <ComboCard
                        combo={c}
                        quantity={cart.combos[c.id]?.qty || 0}
                        onAddToCart={(cb, q) => handleAddToCart(cb, q, true)}
                        labels={copy.comboCard}
                      />
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Phần Đề xuất thông minh */}
            {recommendations.length > 0 && selectedCategory === 'all' && (
              <div className="mt-8 mb-4 p-4 -mx-4 bg-gradient-to-r from-orange-50/80 to-transparent dark:from-slate-800/80 dark:to-transparent border-t border-b border-orange-100/50 dark:border-slate-800/50 backdrop-blur-sm transition-colors">
                <h2 className="text-sm font-bold text-orange-900 dark:text-orange-300 mb-3 flex items-center gap-2 px-4 uppercase tracking-wider transition-colors">
                  <Sparkles size={16} className="text-orange-500 fill-orange-500" /> {copy.recommendations} ({displayTimeContext})
                </h2>
                <div className="flex overflow-x-auto gap-3 pb-2 px-4 no-scrollbar">
                  {recommendations.map(item => (
                    <div key={item.id} className="min-w-[140px]">
                      <MenuCard
                        item={item}
                        quantity={getCartItemQty(item)}
                        onAddToCart={(i, q, needsOpt) => handleAddToCart(i, q, false, needsOpt)}
                        labels={copy.menuCard}
                      />
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Phần Món lẻ */}
            <h2 className="text-sm font-black text-gray-800 dark:text-white mt-6 mb-4 uppercase tracking-tight transition-colors">{copy.menu}</h2>
            <div className="grid grid-cols-2 gap-3 animate-in fade-in duration-500">
              {displayItems.map(item => (
                <MenuCard
                  key={item.id}
                  item={item}
                  quantity={getCartItemQty(item)}
                  onAddToCart={(i, q, needsOpt) => handleAddToCart(i, q, false, needsOpt)}
                  labels={copy.menuCard}
                />
              ))}
            </div>

            {displayItems.length === 0 && (
              <div className="text-center py-10 text-gray-400 text-xs italic">
                {copy.emptyCategory}
              </div>
            )}
          </div>

          {/* Nút Giỏ hàng nổi */}
          <ShoppingCartButton cart={cart} onOpenCart={() => setShowOrderModal(true)} label={copy.cartButton} />

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
            getCartItemQty={getCartItemQty}
            calculateTotal={calculateTotal}
            isSubmitting={isSubmitting}
            handleSubmitOrder={handleSubmitOrder}
            labels={copy.cart}
          />

          <ItemOptionsModal
            key={selectedItemForOptions ? selectedItemForOptions.id : 'closed'}
            item={selectedItemForOptions}
            isOpen={!!selectedItemForOptions}
            onClose={() => setSelectedItemForOptions(null)}
            onConfirm={handleAddWithOptions}
            onError={showError}
            labels={copy.itemOptions}
          />

          <CurrentOrderSheet
            isOpen={showCurrentOrder}
            order={currentOrder}
            onClose={() => setShowCurrentOrder(false)}
            copy={copy.order}
            language={language}
          />

          {/* AI Customer Assistant */}
          {restaurantSettings.enableAiAssistant !== false && (
            <AiChatAssistant hidden={showOrderModal || showCurrentOrder} language={language} />
          )}

          <StatusModal
            isOpen={statusModal.isOpen}
            onClose={closeStatusModal}
            type={statusModal.type}
            title={statusModal.title}
            message={statusModal.message}
          />
        </div>
      </div>
    </div>
  );
};

export default MenuPage;
