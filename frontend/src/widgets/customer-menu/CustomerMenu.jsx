import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { Wifi, WifiOff, Sparkles, Moon, Sun } from 'lucide-react';

import wsService from '@shared/lib/websocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { fmtVND } from '@shared/lib/formatters.js';
import { ErrorBoundary, PullToRefresh } from '@shared/ui';

import { queryClient } from '@shared/api/queryClient.js';
import {
  CartModal,
  CurrentOrderBanner,
  CurrentOrderSheet,
  ItemOptionsModal,
  ShoppingCart as ShoppingCartButton,
  createClientRequestId,
  isTerminalSessionError,
  menuService,
  useCustomerMenuQuery,
  useRecommendationsQuery,
  useStartTableSessionMutation,
  useSubmitOrderMutation,
  useTableSessionQuery,
} from '@features/customer-ordering';
import MenuCard from './MenuCard';
import ComboCard from './ComboCard';
import CategoryFilter from './CategoryFilter';
import CustomerMenuSkeleton from './CustomerMenuSkeleton';
import { AiChatAssistant } from '@features/ai-assistant';

const defaultRestaurantSettings = {
  restaurantName: 'Sắc Màu Quán',
  restaurantPhone: '',
  logoUrl: '',
  orderingEnabled: true,
  maintenanceMode: false,
  enableAiAssistant: true,
};

const normalizeRestaurantSettings = (settings = {}) => ({
  ...defaultRestaurantSettings,
  ...settings,
  logoUrl: settings.logoUrl ?? settings.restaurantLogoUrl ?? '',
  orderingEnabled: settings.orderingEnabled ?? true,
  maintenanceMode: settings.maintenanceMode ?? false,
  enableAiAssistant: settings.enableAiAssistant ?? true,
});

const sessionStorageKey = (tableCode) => `qros:table-session:${tableCode}`;

const CustomerMenuContent = () => {
  const { tableCode: routeTableCode } = useParams();
  const [searchParams] = useSearchParams();
  const tableCode = routeTableCode || searchParams.get('tableCode');

  const [selectedCategory, setSelectedCategory] = useState('all');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const [crossSellItems, setCrossSellItems] = useState([]);
  const crossSellCacheRef = useRef(new Map());
  const [showCurrentOrderSheet, setShowCurrentOrderSheet] = useState(false);
  const submittingRef = useRef(false);
  const [sessionToken, setSessionToken] = useState(() =>
    tableCode ? sessionStorage.getItem(sessionStorageKey(tableCode)) || '' : '',
  );
  const [paymentInProgress, setPaymentInProgress] = useState(false);

  const hour = new Date().getHours();
  const timeContext = hour < 11 ? 'Sáng' : hour < 14 ? 'Trưa' : hour < 18 ? 'Chiều' : 'Tối';
  const weather = 'Trời mát';
  const { statusModal, showSuccess, showError } = useStatusModal();

  // Trạng thái giỏ hàng
  const [cart, setCart] = useState({ items: {}, combos: {} });
  const [selectedItemForOptions, setSelectedItemForOptions] = useState(null);

  // React Query Hooks
  const { data: menuData, isLoading: loadingMenu, refetch: refetchMenu } = useCustomerMenuQuery();
  const {
    data: sessionData,
    isLoading: loadingSession,
    refetch: refetchSession,
  } = useTableSessionQuery(tableCode, sessionToken);
  const { data: recommendationsData, refetch: refetchRecommendations } = useRecommendationsQuery(
    timeContext,
    weather,
  );

  const handleRefresh = useCallback(async () => {
    crossSellCacheRef.current.clear();
    setCrossSellItems([]);

    await Promise.allSettled([refetchMenu(), refetchSession(), refetchRecommendations()]);
  }, [refetchMenu, refetchRecommendations, refetchSession]);

  const categories = menuData?.categories || [];
  const menuItems = useMemo(() => menuData?.menuItems || [], [menuData?.menuItems]);
  const combos = menuData?.combos || [];
  const restaurantSettings = normalizeRestaurantSettings(menuData?.settings);
  const menuItemsById = useMemo(
    () => new Map(menuItems.map((item) => [item.id, item])),
    [menuItems],
  );
  const hydrateRecommendationItems = useCallback(
    (items) =>
      (Array.isArray(items) ? items : [])
        .map((recommendation) => {
          const menuItem = menuItemsById.get(recommendation.id);
          return menuItem
            ? {
                ...menuItem,
                recommendationReason: recommendation.reason,
                recommendationType: recommendation.type,
              }
            : null;
        })
        .filter(Boolean),
    [menuItemsById],
  );
  const recommendations = useMemo(
    () => hydrateRecommendationItems(recommendationsData),
    [hydrateRecommendationItems, recommendationsData],
  );

  const tableInfo = sessionData?.tableInfo;
  const currentOrder = sessionData?.currentOrder;
  const sessionEnded = sessionData?.sessionEnded;
  const sessionError = sessionData?.sessionError;
  const hasTerminalSessionError = isTerminalSessionError(sessionError);

  const loading = loadingMenu || loadingSession;

  const orderingUnavailable =
    restaurantSettings.maintenanceMode || restaurantSettings.orderingEnabled === false;
  const canUseAiAssistant = restaurantSettings.enableAiAssistant !== false;
  // AWAITING_PAYMENT only means every current item is finished. Customers may
  // still add another batch; the backend will reopen the order to SERVING.
  // Only lock ordering after the backend confirms a payment transaction is active.
  const orderPaymentLocked = paymentInProgress;
  const paymentLockedMessage = 'Bàn đang trong quá trình thanh toán, vui lòng liên hệ nhân viên.';

  const getCartItemQty = (item) => {
    return Object.values(cart.items)
      .filter((i) => (i.actualId || i.id) === item.id)
      .reduce((sum, i) => sum + i.qty, 0);
  };

  const loadCrossSellRecommendations = useCallback(
    (itemId) => {
      if (!itemId) return;

      const cached = crossSellCacheRef.current.get(itemId);
      if (Array.isArray(cached)) {
        setCrossSellItems(cached);
        return;
      }

      if (cached?.then) {
        cached.then(setCrossSellItems).catch(() => {});
        return;
      }

      const request = menuService
        .getCrossSellRecommendations(itemId)
        .then((res) => {
          const items = hydrateRecommendationItems(res);
          crossSellCacheRef.current.set(itemId, items);
          return items;
        })
        .catch((error) => {
          crossSellCacheRef.current.delete(itemId);
          throw error;
        });

      crossSellCacheRef.current.set(itemId, request);
      request.then(setCrossSellItems).catch(() => {});
    },
    [hydrateRecommendationItems],
  );

  // WS status via listener (no polling)
  useEffect(() => {
    const wsRef = wsService;
    setWsConnected(wsRef.isConnected());
    return wsRef.addConnectListener((connected) => {
      setWsConnected(connected);
    });
  }, []);

  const handleAddToCart = (product, qty, isCombo = false, needsOptions = false) => {
    if (orderPaymentLocked && qty > 0) {
      showError(paymentLockedMessage, 'Bàn đang thanh toán');
      return;
    }

    if (orderingUnavailable && qty > 0) {
      showError(
        restaurantSettings.maintenanceMode
          ? 'Quán đang bảo trì, vui lòng thử lại sau.'
          : 'Quán đang tạm ngưng nhận đơn mới.',
        'Chưa thể đặt món',
      );
      return;
    }

    if (product.available === false && qty > 0) {
      showError('Món này hiện đã hết nguyên liệu.', 'Hết hàng');
      return;
    }

    if (needsOptions) {
      setSelectedItemForOptions(product);
      return;
    }

    setCart((prev) => {
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
          note: prev[group][id]?.note || '',
        };

        // Gợi ý món đi kèm (Cross-sell)
        if (!isCombo && qty === 1) {
          loadCrossSellRecommendations(product.id);
        }
      }
      return { ...prev, [group]: updatedGroup };
    });
  };

  const handleAddWithOptions = (product, selectedValueIds, selectedOptionObjs, finalPrice) => {
    if (orderPaymentLocked) {
      showError(paymentLockedMessage, 'Bàn đang thanh toán');
      return;
    }

    if (orderingUnavailable) {
      showError(
        restaurantSettings.maintenanceMode
          ? 'Quán đang bảo trì, vui lòng thử lại sau.'
          : 'Quán đang tạm ngưng nhận đơn mới.',
        'Chưa thể đặt món',
      );
      return;
    }

    if (product.available === false) {
      showError('Món này hiện đã hết nguyên liệu.', 'Hết hàng');
      return;
    }

    setCart((prev) => {
      const cartId = product.id + '_' + selectedValueIds.join('_');
      const currentQty = prev.items[cartId]?.qty || 0;

      return {
        ...prev,
        items: {
          ...prev.items, // Trải phẳng dữ liệu cũ của items
          [cartId]: {
            // Ghi đè item mới an toàn
            ...product,
            cartId,
            actualId: product.id,
            price: finalPrice,
            selectedOptionValueIds: selectedValueIds,
            selectedOptionObjs: selectedOptionObjs,
            qty: currentQty + 1,
            note: prev.items[cartId]?.note || '',
          },
        },
      };
    });

    loadCrossSellRecommendations(product.id);
  };

  const handleUpdateCartItemQty = (cartId, qty) => {
    setCart((prev) => {
      const updatedItems = { ...prev.items };
      if (qty <= 0) {
        delete updatedItems[cartId];
      } else {
        updatedItems[cartId] = {
          ...updatedItems[cartId],
          qty: qty,
        };
      }
      return { ...prev, items: updatedItems };
    });
  };

  const handleUpdateNote = (id, note, isCombo = false) => {
    setCart((prev) => {
      const group = isCombo ? 'combos' : 'items';
      return {
        ...prev,
        [group]: {
          ...prev[group],
          [id]: { ...prev[group][id], note },
        },
      };
    });
  };

  const calculateTotal = () => {
    const itTotal = Object.values(cart.items).reduce((s, i) => s + i.qty * i.price, 0);
    const cbTotal = Object.values(cart.combos).reduce((s, c) => s + c.qty * c.price, 0);
    return itTotal + cbTotal;
  };

  const [isSubmitting, setIsSubmitting] = useState(false);
  const submitOrderMutation = useSubmitOrderMutation();
  const { mutateAsync: startTableSession } = useStartTableSessionMutation();

  useEffect(() => {
    if (!tableCode) {
      setSessionToken('');
      return;
    }

    setSessionToken(sessionStorage.getItem(sessionStorageKey(tableCode)) || '');
  }, [tableCode]);

  const persistSessionToken = useCallback(
    (token) => {
      if (!tableCode || !token) return;

      sessionStorage.setItem(sessionStorageKey(tableCode), token);
      setSessionToken(token);
    },
    [tableCode],
  );

  const clearSessionToken = useCallback(() => {
    if (tableCode) {
      sessionStorage.removeItem(sessionStorageKey(tableCode));
    }
    setSessionToken('');
  }, [tableCode]);

  useEffect(() => {
    if (!sessionToken) return;
    if (!sessionEnded && !hasTerminalSessionError) return;

    clearSessionToken();
    setShowCurrentOrderSheet(false);
  }, [clearSessionToken, hasTerminalSessionError, sessionEnded, sessionToken]);

  useEffect(() => {
    if (currentOrder?.status !== 'AWAITING_PAYMENT') {
      setPaymentInProgress(false);
    }
  }, [currentOrder?.status]);

  const ensureSessionToken = useCallback(async () => {
    if (sessionToken) {
      return sessionToken;
    }

    if (!tableCode) {
      throw new Error('Vui lòng quét mã QR trên bàn để đặt món.');
    }

    const startedSession = await startTableSession(tableCode);
    persistSessionToken(startedSession.sessionToken);
    queryClient.invalidateQueries({ queryKey: ['tableSession', tableCode] });
    return startedSession.sessionToken;
  }, [persistSessionToken, sessionToken, startTableSession, tableCode]);

  useEffect(() => {
    if (!sessionToken) return undefined;

    const sendHeartbeat = () => {
      if (document.hidden) return;

      menuService.heartbeatSession(sessionToken).catch((error) => {
        if (
          error?.code === 'TABLE_SESSION_EXPIRED' ||
          error?.code === 'TABLE_SESSION_INVALID' ||
          error?.code === 'TABLE_SESSION_NOT_FOUND'
        ) {
          clearSessionToken();
          queryClient.invalidateQueries({ queryKey: ['tableSession', tableCode] });
        }
      });
    };

    const timer = window.setInterval(sendHeartbeat, 90 * 1000);

    return () => window.clearInterval(timer);
  }, [clearSessionToken, sessionToken, tableCode]);

  const handleSubmitOrder = async () => {
    if (orderPaymentLocked) {
      showError(paymentLockedMessage, 'Bàn đang thanh toán');
      return;
    }

    if (orderingUnavailable) {
      showError(
        restaurantSettings.maintenanceMode
          ? 'Quán đang bảo trì, vui lòng thử lại sau.'
          : 'Quán đang tạm ngưng nhận đơn mới.',
        'Chưa thể đặt món',
      );
      return;
    }

    if (!tableCode) {
      showError('Vui lòng quét mã QR trên bàn để đặt món.', 'Chưa xác định bàn');
      return;
    }
    if (isSubmitting || submittingRef.current) return;

    if (Object.keys(cart.items).length === 0 && Object.keys(cart.combos).length === 0) {
      showError('Giỏ hàng của bạn đang trống. Hãy chọn món trước khi đặt.', 'Chưa có món');
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);
    try {
      const activeSessionToken = await ensureSessionToken();
      const orderData = {
        tableCode,
        sessionToken: activeSessionToken,
        clientRequestId: createClientRequestId(),
        items: Object.entries(cart.items).map(([id, i]) => ({
          menuItemId: i.actualId || parseInt(id),
          quantity: i.qty,
          notes: i.note,
          selectedOptionValueIds: i.selectedOptionValueIds || [],
        })),
        combos: Object.entries(cart.combos).map(([id, c]) => ({
          comboId: parseInt(id),
          quantity: c.qty,
          notes: c.note,
        })),
      };

      await submitOrderMutation.mutateAsync(orderData);
      setPaymentInProgress(false);
      setCart({ items: {}, combos: {} });
      setShowOrderModal(false);
      showSuccess('Đơn hàng của bạn đã được gửi đến quán.', 'Đặt món thành công');
      // Invalidate the session query to load the new currentOrder
      queryClient.invalidateQueries({ queryKey: ['tableSession', tableCode] });
    } catch (e) {
      const errorMessage = e?.message || '';
      if (
        e?.status === 404 &&
        (errorMessage.includes('thông tin bàn') || errorMessage.includes('Table Code'))
      ) {
        showError(e, 'Không tìm thấy thông tin bàn');
      } else if (e?.code === 'TABLE_SESSION_EXPIRED' || e?.code === 'TABLE_SESSION_INVALID') {
        clearSessionToken();
        showError(e, 'Phiên bàn đã hết hạn');
      } else if (e?.code === 'ORDER_PAYMENT_IN_PROGRESS') {
        setPaymentInProgress(true);
        showError(e, 'Bàn đang thanh toán');
      } else {
        showError(e, 'Không thể gửi đơn hàng');
      }
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  };

  if (loading) return <CustomerMenuSkeleton />;

  const displayItems =
    selectedCategory === 'all'
      ? menuItems
      : menuItems.filter((i) => i.category?.id === parseInt(selectedCategory));

  return (
    <div className={isDarkMode ? 'dark' : ''}>
      <div className="flex h-dvh min-h-dvh justify-center overflow-hidden bg-gray-100 transition-colors duration-500 dark:bg-slate-950">
        <div className="safe-left safe-right relative flex h-dvh min-h-0 w-full max-w-md flex-col overflow-hidden bg-white shadow-2xl transition-colors duration-500 dark:bg-slate-900">
          <PullToRefresh
            className="flex-1"
            contentClassName="pb-[calc(8rem+var(--safe-area-inset-bottom))]"
            onRefresh={handleRefresh}
            disabled={
              showOrderModal ||
              showCurrentOrderSheet ||
              statusModal.isOpen ||
              Boolean(selectedItemForOptions)
            }
          >
            {/* Header Section */}
            <div
              className="rounded-b-[2.5rem] bg-orange-500 p-5 text-white shadow-lg"
              style={{ paddingTop: 'calc(1.25rem + var(--safe-area-inset-top))' }}
            >
              <div className="flex justify-between items-start">
                <div className="flex items-start gap-3 min-w-0">
                  {restaurantSettings.logoUrl && (
                    <img
                      src={restaurantSettings.logoUrl}
                      alt={restaurantSettings.restaurantName}
                      className="h-10 w-10 rounded-xl border border-white/30 bg-white/20 object-cover"
                    />
                  )}
                  <div className="min-w-0">
                    <h1 className="text-xl font-black uppercase tracking-tighter truncate">
                      {restaurantSettings.restaurantName || 'Sắc Màu Quán'}
                    </h1>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="bg-white/20 px-3 py-0.5 rounded-full text-[10px] font-bold border border-white/30 uppercase">
                        Bàn số: {tableInfo?.tableNumber || 'NaN'}
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
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsDarkMode(!isDarkMode)}
                    className="w-8 h-8 rounded-full bg-white/20 border border-white/30 flex items-center justify-center text-white cursor-pointer hover:bg-white/30 transition-all duration-300 shadow-sm"
                    aria-label="Toggle dark mode"
                  >
                    {isDarkMode ? (
                      <Sun size={14} className="animate-spin-slow" />
                    ) : (
                      <Moon size={14} />
                    )}
                  </button>

                  <div
                    className={`flex items-center gap-1 px-3 py-1 rounded-full text-[10px] font-bold border transition-all duration-500 ${wsConnected ? 'bg-green-500/20 border-green-400' : 'bg-red-500/20 border-red-400'}`}
                  >
                    {wsConnected ? (
                      <Wifi size={12} className="animate-pulse" />
                    ) : (
                      <WifiOff size={12} />
                    )}
                    {wsConnected ? 'LIVE' : 'OFFLINE'}
                  </div>
                </div>
              </div>
            </div>

            {currentOrder && (
              <div className="sticky top-0 z-40 bg-white/95 px-3 py-2 shadow-sm backdrop-blur-md transition-colors dark:bg-slate-900/95 border-b border-gray-100 dark:border-slate-800">
                <CurrentOrderBanner
                  order={currentOrder}
                  onClick={() => setShowCurrentOrderSheet(true)}
                />
              </div>
            )}

            {orderingUnavailable && (
              <div className="mx-4 mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200">
                {restaurantSettings.maintenanceMode
                  ? 'Quán đang bảo trì, hiện chưa nhận đơn mới.'
                  : 'Quán đang tạm ngưng nhận đơn mới.'}
              </div>
            )}

            <div className="p-4">
              {/* Component lọc danh mục */}
              <CategoryFilter
                categories={categories}
                selectedCategory={selectedCategory}
                onSelectCategory={setSelectedCategory}
              />

              {/* Phần Combo */}
              {combos.length > 0 && (
                <div className="mt-4 mb-2">
                  <h2 className="text-sm font-bold text-gray-800 dark:text-white mb-3 flex items-center gap-2 transition-colors">
                    Combo Khuyến Mãi
                  </h2>
                  <div className="flex overflow-x-auto gap-3 pb-2 no-scrollbar animate-in fade-in duration-500">
                    {combos.map((c) => (
                      <div key={c.id} className="min-w-[85vw] sm:min-w-[280px]">
                        <ComboCard
                          combo={c}
                          quantity={cart.combos[c.id]?.qty || 0}
                          onAddToCart={(cb, q) => handleAddToCart(cb, q, true)}
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
                    <Sparkles size={16} className="text-orange-500 fill-orange-500" /> Gợi ý cho bạn
                    ({timeContext})
                  </h2>
                  <div className="flex overflow-x-auto gap-3 pb-2 px-4 no-scrollbar">
                    {recommendations.map((item) => (
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
              <h2 className="text-sm font-black text-gray-800 dark:text-white mt-6 mb-4 uppercase tracking-tight transition-colors">
                Thực đơn
              </h2>
              <div className="grid grid-cols-2 gap-3 animate-in fade-in duration-500">
                {displayItems.map((item) => (
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
          </PullToRefresh>

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
            getCartItemQty={getCartItemQty}
            calculateTotal={calculateTotal}
            isSubmitting={isSubmitting}
            orderingBlocked={orderPaymentLocked}
            orderingBlockMessage={paymentLockedMessage}
            handleSubmitOrder={handleSubmitOrder}
          />

          <ItemOptionsModal
            key={selectedItemForOptions ? selectedItemForOptions.id : 'closed'}
            item={selectedItemForOptions}
            isOpen={!!selectedItemForOptions}
            onClose={() => setSelectedItemForOptions(null)}
            onConfirm={handleAddWithOptions}
            onError={showError}
          />

          {/* AI Customer Assistant */}
          {canUseAiAssistant && (
            <AiChatAssistant
              hidden={
                showOrderModal ||
                showCurrentOrderSheet ||
                statusModal.isOpen ||
                selectedItemForOptions
              }
            />
          )}

          <CurrentOrderSheet
            isOpen={showCurrentOrderSheet}
            order={currentOrder}
            onClose={() => setShowCurrentOrderSheet(false)}
          />
        </div>
      </div>
    </div>
  );
};

const CustomerMenu = () => (
  <ErrorBoundary fullScreen>
    <CustomerMenuContent />
  </ErrorBoundary>
);

export default CustomerMenu;
