import { useCallback, useMemo, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';


import {
  CartModal,
  CurrentOrderBanner,
  CurrentOrderSheet,
  ShoppingCart as ShoppingCartButton,
  useCustomerMenuQuery,
  useRecommendationsQuery,
} from '../index.js';
import { ItemOptionsModal } from '@shared/ui';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import useFlyToCart, { FlyingImageOverlay } from '@shared/hooks/useFlyToCart.jsx';
import { fmtVND } from '@shared/lib/formatters.js';
import { ErrorBoundary, PullToRefresh } from '@shared/ui';
import CategoryFilter from './CategoryFilter.jsx';
import CustomerMenuSkeleton from './CustomerMenuSkeleton.jsx';
import {
  getTimeContext,
  normalizeRestaurantSettings,
  translateRecommendationReason,
} from '../lib/restaurantSettings.js';
import useCrossSellRecommendations from '../model/useCrossSellRecommendations.js';
import useCustomerCart from '../model/useCustomerCart.js';
import useCustomerMenuRealtime from '../model/useCustomerMenuRealtime.js';
import useCustomerMenuSession from '../model/useCustomerMenuSession.js';
import useRestaurantWeather from '../model/useRestaurantWeather.js';
import useSubmitCustomerOrder from '../model/useSubmitCustomerOrder.js';
import ComboSection from './ComboSection.jsx';
import CustomerMenuHeader from './CustomerMenuHeader.jsx';
import MenuItemGrid from './MenuItemGrid.jsx';
import OrderingUnavailableBanner from './OrderingUnavailableBanner.jsx';
import RecommendationSection from './RecommendationSection.jsx';

const PAYMENT_LOCKED_MESSAGE = 'Bàn đang trong quá trình thanh toán, vui lòng liên hệ nhân viên.';

const CustomerMenuContent = ({ tableCode, renderAiAssistant }) => {
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [showCurrentOrderSheet, setShowCurrentOrderSheet] = useState(false);
  const { statusModal, showSuccess, showError } = useStatusModal();
  const timeContext = getTimeContext();
  const { weather, weatherLine, isLoading: loadingWeather } = useRestaurantWeather();
  const { flyItem, cartRef, fly, clearFly } = useFlyToCart();

  const { data: menuData, isLoading: loadingMenu, refetch: refetchMenu } = useCustomerMenuQuery();
  const restaurantSettings = normalizeRestaurantSettings(menuData?.settings);
  const closeCurrentOrderSheet = useCallback(() => setShowCurrentOrderSheet(false), []);
  const session = useCustomerMenuSession(tableCode, {
    onSessionEnded: closeCurrentOrderSheet,
  });
  const { data: recommendationsData, refetch: refetchRecommendations } = useRecommendationsQuery(
    timeContext,
    weather,
    { enabled: Boolean(menuData) && restaurantSettings.showRecommendations !== false },
  );
  const wsConnected = useCustomerMenuRealtime({
    tableCode,
    sessionToken: session.sessionToken,
  });

  const categories = menuData?.categories || [];
  const menuItems = useMemo(() => menuData?.menuItems || [], [menuData?.menuItems]);
  const combos = menuData?.combos || [];
  const orderingUnavailable =
    restaurantSettings.maintenanceMode || restaurantSettings.orderingEnabled === false;
  const orderPaymentLocked = session.paymentInProgress;

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

  const handleRefresh = useCallback(async () => {
    crossSell.clearCrossSell();
    await Promise.allSettled([refetchMenu(), session.refetch(), refetchRecommendations()]);
  }, [crossSell, refetchMenu, refetchRecommendations, session]);

  const currentOrder = session.data?.currentOrder;
  const displayItems =
    selectedCategory === 'all'
      ? menuItems
      : menuItems.filter((item) => item.category?.id === parseInt(selectedCategory));

  if (loadingMenu || session.isLoading) return <CustomerMenuSkeleton />;

  return (
    <div className={isDarkMode ? 'dark' : ''}>
      <div className="flex h-dvh min-h-dvh justify-center overflow-hidden bg-bg-warm transition-colors duration-500 dark:bg-slate-950">
        <div className="safe-left safe-right relative flex h-dvh min-h-0 w-full max-w-md flex-col overflow-hidden bg-surface-warm shadow-2xl transition-colors duration-500 dark:bg-slate-900">
          <PullToRefresh
            className="flex-1"
            contentClassName="pb-[calc(8rem+var(--safe-area-inset-bottom))]"
            onRefresh={handleRefresh}
            disabled={
              showOrderModal ||
              showCurrentOrderSheet ||
              statusModal.isOpen ||
              Boolean(customerCart.selectedItemForOptions)
            }
          >
            <CustomerMenuHeader
              restaurantSettings={restaurantSettings}
              tableNumber={session.data?.tableInfo?.tableNumber}
              isDarkMode={isDarkMode}
              onToggleDarkMode={() => setIsDarkMode((current) => !current)}
              wsConnected={wsConnected}
              timeContext={timeContext}
              weatherLine={weatherLine}
              loadingWeather={loadingWeather}
            />

            {currentOrder && (
              <div className="sticky top-0 z-40 border-b border-gray-100 bg-white/95 px-3 py-2 shadow-sm backdrop-blur-md transition-colors dark:border-slate-800 dark:bg-slate-900/95">
                <CurrentOrderBanner
                  order={currentOrder}
                  onClick={() => setShowCurrentOrderSheet(true)}
                />
              </div>
            )}

            {orderingUnavailable && (
              <OrderingUnavailableBanner maintenanceMode={restaurantSettings.maintenanceMode} />
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

          <ShoppingCartButton ref={cartRef} cart={customerCart.cart} onOpenCart={() => setShowOrderModal(true)} />
          <CartModal
            isOpen={showOrderModal}
            onClose={() => setShowOrderModal(false)}
            cart={customerCart.cart}
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
          {restaurantSettings.enableAiAssistant !== false && renderAiAssistant &&
            renderAiAssistant(
                showOrderModal ||
                showCurrentOrderSheet ||
                statusModal.isOpen ||
                customerCart.selectedItemForOptions
            )
          }
          <CurrentOrderSheet
            isOpen={showCurrentOrderSheet}
            order={currentOrder}
            onClose={closeCurrentOrderSheet}
          />
          {flyItem && (
            <FlyingImageOverlay item={flyItem} onEnd={clearFly} />
          )}
        </div>
      </div>
    </div>
  );
};

const CustomerMenuRoute = ({ renderAiAssistant }) => {
  const { tableCode: routeTableCode } = useParams();
  const [searchParams] = useSearchParams();
  const tableCode = routeTableCode || searchParams.get('tableCode');

  return <CustomerMenuContent key={tableCode || 'no-table'} tableCode={tableCode} renderAiAssistant={renderAiAssistant} />;
};

const CustomerMenu = ({ renderAiAssistant }) => (
  <ErrorBoundary fullScreen>
    <CustomerMenuRoute renderAiAssistant={renderAiAssistant} />
  </ErrorBoundary>
);

export default CustomerMenu;
