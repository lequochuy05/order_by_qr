import { useCallback, useMemo, useState } from 'react';

const EMPTY_CART = { items: {}, combos: {} };

const useCustomerCart = ({
  orderingUnavailable,
  orderPaymentLocked,
  restaurantSettings,
  showError,
  loadCrossSellRecommendations,
  paymentLockedMessage,
}) => {
  const [cart, setCart] = useState(EMPTY_CART);
  const [selectedItemForOptions, setSelectedItemForOptions] = useState(null);

  const assertCanOrder = useCallback(
    (product) => {
      if (orderPaymentLocked) {
        showError(paymentLockedMessage, 'Bàn đang thanh toán');
        return false;
      }
      if (orderingUnavailable) {
        showError(
          restaurantSettings.maintenanceMode
            ? 'Quán đang bảo trì, vui lòng thử lại sau.'
            : 'Quán đang tạm ngưng nhận đơn mới.',
          'Chưa thể đặt món',
        );
        return false;
      }
      if (product?.available === false) {
        showError('Món này hiện đã hết nguyên liệu.', 'Hết hàng');
        return false;
      }
      return true;
    },
    [
      orderPaymentLocked,
      orderingUnavailable,
      paymentLockedMessage,
      restaurantSettings.maintenanceMode,
      showError,
    ],
  );

  const getCartItemQty = useCallback(
    (item) =>
      Object.values(cart.items)
        .filter((cartItem) => (cartItem.actualId || cartItem.id) === item.id)
        .reduce((sum, cartItem) => sum + cartItem.qty, 0),
    [cart.items],
  );

  const handleAddToCart = useCallback(
    (product, qty, isCombo = false, needsOptions = false) => {
      if (qty > 0 && !assertCanOrder(product)) return;
      if (needsOptions) {
        setSelectedItemForOptions(product);
        return;
      }

      setCart((current) => {
        const group = isCombo ? 'combos' : 'items';
        const updatedGroup = { ...current[group] };

        if (qty <= 0) {
          delete updatedGroup[product.id];
        } else {
          updatedGroup[product.id] = {
            ...product,
            actualId: product.id,
            qty,
            note: current[group][product.id]?.note || '',
          };
          if (!isCombo && qty === 1) loadCrossSellRecommendations(product.id);
        }

        return { ...current, [group]: updatedGroup };
      });
    },
    [assertCanOrder, loadCrossSellRecommendations],
  );

  const handleAddWithOptions = useCallback(
    (product, selectedValueIds, selectedOptionObjs, finalPrice) => {
      if (!assertCanOrder(product)) return;

      setCart((current) => {
        const cartId = `${product.id}_${selectedValueIds.join('_')}`;
        const currentQty = current.items[cartId]?.qty || 0;

        return {
          ...current,
          items: {
            ...current.items,
            [cartId]: {
              ...product,
              cartId,
              actualId: product.id,
              price: finalPrice,
              selectedOptionValueIds: selectedValueIds,
              selectedOptionObjs,
              qty: currentQty + 1,
              note: current.items[cartId]?.note || '',
            },
          },
        };
      });
      loadCrossSellRecommendations(product.id);
    },
    [assertCanOrder, loadCrossSellRecommendations],
  );

  const handleUpdateCartItemQty = useCallback((cartId, qty) => {
    setCart((current) => {
      const updatedItems = { ...current.items };
      if (qty <= 0) {
        delete updatedItems[cartId];
      } else {
        updatedItems[cartId] = { ...updatedItems[cartId], qty };
      }
      return { ...current, items: updatedItems };
    });
  }, []);

  const handleUpdateNote = useCallback((id, note, isCombo = false) => {
    setCart((current) => {
      const group = isCombo ? 'combos' : 'items';
      return {
        ...current,
        [group]: {
          ...current[group],
          [id]: { ...current[group][id], note },
        },
      };
    });
  }, []);

  const total = useMemo(() => {
    const itemTotal = Object.values(cart.items).reduce(
      (sum, item) => sum + item.qty * item.price,
      0,
    );
    const comboTotal = Object.values(cart.combos).reduce(
      (sum, combo) => sum + combo.qty * combo.price,
      0,
    );
    return itemTotal + comboTotal;
  }, [cart]);

  const resetCart = useCallback(() => setCart(EMPTY_CART), []);

  return {
    cart,
    selectedItemForOptions,
    setSelectedItemForOptions,
    getCartItemQty,
    handleAddToCart,
    handleAddWithOptions,
    handleUpdateCartItemQty,
    handleUpdateNote,
    calculateTotal: () => total,
    resetCart,
  };
};

export default useCustomerCart;
