import { useCallback, useMemo, useState } from 'react';
import { Clock3, Sparkles, WifiOff } from 'lucide-react';
import { useParams, useSearchParams } from 'react-router-dom';

import { useCustomerMenuQuery, useRecommendationsQuery } from '@entities/public-menu';
import { publicMenuService } from '@entities/public-menu/api/publicMenuService.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import useFlyToCart from '@shared/hooks/useFlyToCart.js';
import { buildErrorMessage } from '@shared/lib/errorMessages.js';
import { fmtVND } from '@shared/lib/formatters.js';
import { ErrorBoundary, ItemOptionsModal, PullToRefresh, SpotlightGrid } from '@shared/ui';
import CategoryFilter from './CategoryFilter.jsx';
import CartModal from './CartModal.jsx';
import CurrentOrderBanner from './CurrentOrderBanner.jsx';
import CurrentOrderSheet from './CurrentOrderSheet.jsx';
import CustomerMenuSkeleton from './CustomerMenuSkeleton.jsx';
import FlyingImageOverlay from './FlyingImageOverlay.jsx';
import {
  getTimeContext,
  normalizeRestaurantSettings,
  translateRecommendationReason,
} from '../lib/restaurantSettings.js';
import { selectBestActivePromotion } from '../lib/selfCheckoutHelpers.js';
import useCrossSellRecommendations from '../model/useCrossSellRecommendations.js';
import useCustomerCart from '../model/useCustomerCart.js';
import useCustomerMenuRealtime from '../model/useCustomerMenuRealtime.js';
import useCustomerMenuSession from '../model/useCustomerMenuSession.js';
import useRestaurantWeather from '../model/useRestaurantWeather.js';
import useSubmitCustomerOrder from '../model/useSubmitCustomerOrder.js';
import useTableServiceRequest from '../model/useTableServiceRequest.js';
import CallStaffModal from './CallStaffModal.jsx';
import ComboSection from './ComboSection.jsx';
import CustomerMenuHeader from './CustomerMenuHeader.jsx';
import MenuItemGrid from './MenuItemGrid.jsx';
import OrderingUnavailableBanner from './OrderingUnavailableBanner.jsx';
import RecommendationSection from './RecommendationSection.jsx';
import ShoppingCartButton from './ShoppingCart.jsx';

const PAYMENT_LOCKED_MESSAGE = 'Bàn đang trong quá trình thanh toán, vui lòng liên hệ nhân viên.';

const CustomerMenuContent = ({ tableCode, sig, renderAiAssistant }) => {
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [showCurrentOrderSheet, setShowCurrentOrderSheet] = useState(false);
  const { statusModal, showSuccess, showError } = useStatusModal();
  const timeContext = getTimeContext();
  const { weatherLine, isLoading: loadingWeather } = useRestaurantWeather();
  const { flyItem, cartRef, fly, clearFly } = useFlyToCart();

  const { data: menuData, isLoading: loadingMenu, isError: isMenuError, error: menuError, refetch: refetchMenu } = useCustomerMenuQuery();
  const restaurantSettings = normalizeRestaurantSettings(menuData?.settings);
  const closeCurrentOrderSheet = useCallback(() => setShowCurrentOrderSheet(false), []);
  const session = useCustomerMenuSession(tableCode, {
    onSessionEnded: closeCurrentOrderSheet,
    sig,
  });
  const { data: recommendationsData, refetch: refetchRecommendations } = useRecommendationsQuery(
    timeContext,
    { enabled: Boolean(menuData) && restaurantSettings.showRecommendations !== false },
  );
  const wsConnected = useCustomerMenuRealtime({
    tableCode,
    sessionToken: session.sessionToken,
  });

  const categories = menuData?.categories || [];
  const menuItems = useMemo(() => menuData?.menuItems || [], [menuData?.menuItems]);
  const combos = useMemo(() => menuData?.combos || [], [menuData?.combos]);
  const promotions = useMemo(() => menuData?.promotions || [], [menuData?.promotions]);
  const activePromotion = useMemo(() => selectBestActivePromotion(promotions), [promotions]);
  const orderingUnavailable =
    restaurantSettings.maintenanceMode || restaurantSettings.orderingEnabled === false;
  const orderPaymentLocked = session.paymentInProgress;

  const menuItemsById = useMemo(
    () => new Map(menuItems.map((item) => [item.id, item])),
    [menuItems],
  );
  const combosById = useMemo(
    () => new Map(combos.map((combo) => [combo.id, combo])),
    [combos],
  );
  const hydrateRecommendationItems = useCallback(
    (items) =>
      (Array.isArray(items) ? items : [])
        .map((recommendation) => {
          const menuItem = menuItemsById.get(recommendation.id);
          return menuItem
            ? {
                ...menuItem,
                recommendationReason: translateRecommendationReason(recommendation.reason),
                recommendationType: recommendation.type,
              }
            : null;
        })
        .filter(Boolean),
    [menuItemsById],
  );
  const recommendations = useMemo(
    () =>
      restaurantSettings.showRecommendations === false
        ? []
        : hydrateRecommendationItems(recommendationsData),
    [hydrateRecommendationItems, recommendationsData, restaurantSettings.showRecommendations],
  );
  const crossSell = useCrossSellRecommendations(
    hydrateRecommendationItems,
    restaurantSettings.showRecommendations !== false,
  );
  const customerCart = useCustomerCart({
    orderingUnavailable,
    orderPaymentLocked,
    restaurantSettings,
    showError,
    loadCrossSellRecommendations: crossSell.loadCrossSellRecommendations,
    paymentLockedMessage: PAYMENT_LOCKED_MESSAGE,
  });
  const submission = useSubmitCustomerOrder({
    tableCode,
    sessionData: session.data,
    cart: customerCart.cart,
    orderingUnavailable,
    orderPaymentLocked,
    restaurantSettings,
    paymentLockedMessage: PAYMENT_LOCKED_MESSAGE,
    ensureSessionToken: session.ensureSessionToken,
    clearSessionToken: session.clearSessionToken,
    setPaymentInProgress: session.setPaymentInProgress,
    resetCart: customerCart.resetCart,
    closeCart: () => setShowOrderModal(false),
    showSuccess,
    showError,
  });
  const serviceRequest = useTableServiceRequest({
    tableCode,
    sessionToken: session.sessionToken,
    ensureSessionToken: session.ensureSessionToken,
    clearSessionToken: session.clearSessionToken,
    showSuccess,
    showError,
  });

  const handleRefresh = useCallback(async () => {
    crossSell.clearCrossSell();
    await Promise.allSettled([
      refetchMenu(),
      session.refetch(),
      refetchRecommendations(),
      tableCode && session.sessionToken ? serviceRequest.refetch() : Promise.resolve(),
    ]);
  }, [crossSell, refetchMenu, refetchRecommendations, serviceRequest, session, tableCode]);

  const handleRequestPayment = useCallback(async () => {
    if (!tableCode || !session.sessionToken) return;
    try {
      await publicMenuService.requestPayment(tableCode, session.sessionToken);
      session.setPaymentInProgress(true);
      await session.refetch();
      showSuccess('Đã gửi yêu cầu thanh toán. Nhân viên sẽ tới bàn của bạn ngay!');
    } catch (err) {
      showError(buildErrorMessage(err, { includeDetails: false }));
      throw err;
    }
  }, [session, showSuccess, showError, tableCode]);

  const handleSelfCheckoutSuccess = useCallback(async () => {
    showSuccess('Thanh toán thành công! Cảm ơn quý khách đã dùng bữa.');
    await Promise.allSettled([session.refetch(), refetchMenu()]);
  }, [refetchMenu, session, showSuccess]);

  const handleAddAiSuggestion = useCallback(
    (suggestion) => {
      if (!suggestion) return false;
      if (suggestion.type === 'COMBO') {
        const liveCombo = combosById.get(suggestion.id);
        if (!liveCombo || liveCombo.available === false) {
          showError('Combo này hiện không còn khả dụng.', 'Tạm ngưng phục vụ');
          return false;
        }
        const currentQty = customerCart.cart.combos[liveCombo.id]?.qty || 0;
        const deltaQty = suggestion.quantity > 0 ? suggestion.quantity : 1;
        customerCart.handleAddToCart(liveCombo, currentQty + deltaQty, true, false);
        return true;
      }

      const liveItem = menuItemsById.get(suggestion.id);
      if (!liveItem || liveItem.available === false) {
        showError('Món này hiện đã hết nguyên liệu hoặc tạm ngưng.', 'Tạm ngưng phục vụ');
        return false;
      }
      const needsOptions = Boolean(liveItem.itemOptions?.length || suggestion.hasOptions);
      if (needsOptions) {
        customerCart.handleAddToCart(liveItem, 1, false, true);
        return true;
      }
      const currentQty = customerCart.cart.items[liveItem.id]?.qty || 0;
      const deltaQty = suggestion.quantity > 0 ? suggestion.quantity : 1;
      customerCart.handleAddToCart(liveItem, currentQty + deltaQty, false, false);
      return true;
    },
    [combosById, customerCart, menuItemsById, showError],
  );

  const getAiSuggestionCartQty = useCallback(
    (suggestion) => {
      if (!suggestion) return 0;
      if (suggestion.type === 'COMBO') {
        return customerCart.cart.combos[suggestion.id]?.qty || 0;
      }
      return customerCart.getCartItemQty({ id: suggestion.id });
    },
    [customerCart],
  );

  const cartItemCount = useMemo(() => {
    const itemsQty = Object.values(customerCart.cart.items).reduce((sum, item) => sum + item.qty, 0);
    const combosQty = Object.values(customerCart.cart.combos).reduce((sum, combo) => sum + combo.qty, 0);
    return itemsQty + combosQty;
  }, [customerCart.cart]);

  const aiOrderBridge = useMemo(
    () => ({
      onAddSuggestion: handleAddAiSuggestion,
      getSuggestionCartQty: getAiSuggestionCartQty,
      onOpenCart: () => setShowOrderModal(true),
      cartItemCount,
      orderingBlocked: orderingUnavailable || orderPaymentLocked,
      orderingBlockReason: orderPaymentLocked
        ? PAYMENT_LOCKED_MESSAGE
        : orderingUnavailable
          ? restaurantSettings.maintenanceMode
            ? 'Quán đang bảo trì, tạm ngưng đặt món.'
            : 'Quán đang tạm ngưng nhận đơn mới.'
          : '',
    }),
    [
      cartItemCount,
      getAiSuggestionCartQty,
      handleAddAiSuggestion,
      orderPaymentLocked,
      orderingUnavailable,
      restaurantSettings.maintenanceMode,
    ],
  );

  const currentOrder = session.data?.currentOrder;
  const displayItems =
    selectedCategory === 'all'
      ? menuItems
      : menuItems.filter((item) => item.category?.id === parseInt(selectedCategory));

  if (loadingMenu || session.isLoading) return <CustomerMenuSkeleton />;

  if (isMenuError) {
    return (
      <div className={isDarkMode ? 'dark' : ''}>
        <div className="relative flex h-dvh min-h-dvh flex-col items-center justify-center p-8 text-center">
          <SpotlightGrid />
          <div className="relative z-10 flex flex-col items-center">
            <div className="mb-4 inline-flex h-16 w-16 items-center justify-center rounded-full bg-rose-100 text-rose-600 dark:bg-rose-950/40 dark:text-rose-400">
              <WifiOff size={32} />
            </div>
            <h2 className="text-base font-black text-slate-800 dark:text-slate-100">
              Không thể tải thực đơn
            </h2>
            <p className="mt-2 max-w-xs text-xs leading-relaxed text-slate-500 dark:text-slate-400">
              {menuError?.message || 'Đã có lỗi xảy ra khi kết nối đến máy chủ. Vui lòng thử lại.'}
            </p>
            <button
              onClick={() => handleRefresh()}
              className="mt-6 rounded-2xl bg-indigo-600 px-6 py-3 text-xs font-black uppercase tracking-wider text-white shadow-lg shadow-indigo-600/25 transition-all active:scale-95 hover:bg-indigo-500"
            >
              Tải lại menu
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={isDarkMode ? 'dark' : ''}>
      <div className="relative flex h-dvh min-h-dvh justify-center overflow-hidden transition-colors duration-500">
        <SpotlightGrid />
        <div className="safe-left safe-right relative z-10 flex h-dvh min-h-0 w-full max-w-md flex-col overflow-hidden border-slate-200/70 shadow-2xl transition-colors duration-500 dark:border-slate-800/70 sm:border-x">
          <PullToRefresh
            className="flex-1"
            contentClassName="pb-[calc(8rem+var(--safe-area-inset-bottom))]"
            onRefresh={handleRefresh}
            disabled={
              showOrderModal ||
              showCurrentOrderSheet ||
              statusModal.isOpen ||
              Boolean(customerCart.selectedItemForOptions) ||
              serviceRequest.isOpen
            }
          >
            <CustomerMenuHeader
              restaurantSettings={restaurantSettings}
              tableNumber={session.data?.tableInfo?.tableNumber}
              isDarkMode={isDarkMode}
              onToggleDarkMode={() => setIsDarkMode((current) => !current)}
              wsConnected={wsConnected}
              weatherLine={weatherLine}
              loadingWeather={loadingWeather}
              onCallStaff={serviceRequest.openModal}
              openServiceRequestCount={serviceRequest.openCount}
              canCallStaff={
                Boolean(tableCode) && session.data?.tableInfo?.status !== 'INACTIVE'
              }
            />

            {currentOrder && (
              <div className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/95 px-3 py-2 shadow-sm backdrop-blur-md transition-colors dark:border-slate-800/80 dark:bg-slate-900/95">
                <CurrentOrderBanner
                  order={currentOrder}
                  activePromotion={activePromotion}
                  onClick={() => setShowCurrentOrderSheet(true)}
                />
              </div>
            )}

            {orderingUnavailable && (
              <OrderingUnavailableBanner maintenanceMode={restaurantSettings.maintenanceMode} />
            )}

            {activePromotion && (
              <div className="px-4 pt-3">
                <div className="flex items-center justify-between gap-3 rounded-2xl border border-indigo-200/80 bg-gradient-to-r from-indigo-50 via-violet-50 to-purple-50 px-3.5 py-2.5 shadow-xs dark:border-indigo-500/30 dark:from-indigo-950/60 dark:via-violet-950/50 dark:to-purple-950/40">
                  <div className="flex min-w-0 items-center gap-2.5">
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-600 text-white shadow-sm shadow-indigo-600/25">
                      <Sparkles size={15} />
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-xs font-black text-slate-900 dark:text-white">
                        {activePromotion.name}
                      </p>
                      <p className="mt-0.5 flex items-center gap-1.5 text-[11px] font-semibold text-indigo-600/90 dark:text-indigo-300">
                        <Clock3 size={11} className="shrink-0" />
                        <span className="font-mono">
                          {String(activePromotion.startTime || '').slice(0, 5)} -{' '}
                          {String(activePromotion.endTime || '').slice(0, 5)}
                        </span>
                        <span>• Tự động giảm khi thanh toán</span>
                      </p>
                    </div>
                  </div>
                  <span className="shrink-0 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-2.5 py-1 font-mono text-xs font-black text-white shadow-xs">
                    -{activePromotion.discountPercent}%
                  </span>
                </div>
              </div>
            )}

            <div className="p-4">
              <CategoryFilter
                categories={categories}
                selectedCategory={selectedCategory}
                onSelectCategory={setSelectedCategory}
              />
              <ComboSection
                combos={combos}
                cart={customerCart.cart}
                onAddToCart={customerCart.handleAddToCart}
                onFly={fly}
              />
              <RecommendationSection
                recommendations={recommendations}
                selectedCategory={selectedCategory}
                timeContext={timeContext}
                getCartItemQty={customerCart.getCartItemQty}
                onAddToCart={customerCart.handleAddToCart}
                onFly={fly}
              />
              <MenuItemGrid
                items={displayItems}
                getCartItemQty={customerCart.getCartItemQty}
                onAddToCart={customerCart.handleAddToCart}
                onFly={fly}
              />
            </div>
          </PullToRefresh>

          <ShoppingCartButton
            ref={cartRef}
            cart={customerCart.cart}
            onOpenCart={() => setShowOrderModal(true)}
          />
          <CartModal
            isOpen={showOrderModal}
            onClose={() => setShowOrderModal(false)}
            cart={customerCart.cart}
            activePromotion={activePromotion}
            fmtVND={fmtVND}
            handleUpdateCartItemQty={customerCart.handleUpdateCartItemQty}
            handleUpdateNote={customerCart.handleUpdateNote}
            crossSellItems={crossSell.crossSellItems}
            setCrossSellItems={crossSell.setCrossSellItems}
            handleAddToCart={customerCart.handleAddToCart}
            getCartItemQty={customerCart.getCartItemQty}
            calculateTotal={customerCart.calculateTotal}
            isSubmitting={submission.isSubmitting}
            orderingBlocked={orderPaymentLocked}
            orderingBlockMessage={PAYMENT_LOCKED_MESSAGE}
            handleSubmitOrder={submission.handleSubmitOrder}
          />
          <ItemOptionsModal
            key={customerCart.selectedItemForOptions?.id || 'closed'}
            item={customerCart.selectedItemForOptions}
            isOpen={Boolean(customerCart.selectedItemForOptions)}
            onClose={() => customerCart.setSelectedItemForOptions(null)}
            onConfirm={customerCart.handleAddWithOptions}
            onError={showError}
            onFly={fly}
          />
          {restaurantSettings.enableAiAssistant !== false &&
            renderAiAssistant &&
            renderAiAssistant(
              showOrderModal ||
                showCurrentOrderSheet ||
                statusModal.isOpen ||
                Boolean(customerCart.selectedItemForOptions) ||
                serviceRequest.isOpen,
              aiOrderBridge,
            )}
          <CallStaffModal
            isOpen={serviceRequest.isOpen}
            onClose={serviceRequest.closeModal}
            selectedReason={serviceRequest.selectedReason}
            onSelectReason={serviceRequest.selectReason}
            note={serviceRequest.note}
            onChangeNote={serviceRequest.setNote}
            validationError={serviceRequest.validationError}
            openRequests={serviceRequest.openRequests}
            recentClosedRequests={serviceRequest.recentClosedRequests}
            activeReasons={serviceRequest.activeReasons}
            openCount={serviceRequest.openCount}
            maxOpenRequests={serviceRequest.maxOpenRequests}
            cooldownSeconds={serviceRequest.cooldownSeconds}
            isSubmitting={serviceRequest.isSubmitting}
            cancellingId={serviceRequest.cancellingId}
            wsConnected={wsConnected}
            onSubmit={serviceRequest.handleSubmit}
            onCancelRequest={serviceRequest.handleCancelRequest}
          />
          <CurrentOrderSheet
            isOpen={showCurrentOrderSheet}
            order={currentOrder}
            activePromotion={activePromotion}
            onClose={closeCurrentOrderSheet}
            onRequestPayment={handleRequestPayment}
            onPaymentSuccess={handleSelfCheckoutSuccess}
            tableStatus={session.data?.tableInfo?.status}
            tableCode={tableCode}
            sessionToken={session.sessionToken}
            restaurantSettings={restaurantSettings}
          />
          {flyItem && <FlyingImageOverlay item={flyItem} onEnd={clearFly} />}
        </div>
      </div>
    </div>
  );
};

const CustomerMenuRoute = ({ renderAiAssistant }) => {
  const { tableCode: routeTableCode } = useParams();
  const [searchParams] = useSearchParams();
  const tableCode = routeTableCode || searchParams.get('tableCode');
  const sig = searchParams.get('sig');

  return (
    <CustomerMenuContent
      key={tableCode || 'no-table'}
      tableCode={tableCode}
      sig={sig}
      renderAiAssistant={renderAiAssistant}
    />
  );
};

const CustomerMenu = ({ renderAiAssistant }) => (
  <ErrorBoundary fullScreen>
    <CustomerMenuRoute renderAiAssistant={renderAiAssistant} />
  </ErrorBoundary>
);

export default CustomerMenu;
