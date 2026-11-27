import { useCallback, useMemo, useState } from 'react';

const EMPTY_CART = { items: {}, combos: {} };

const resolveLiveItemPrice = (cartItem, liveItem) => {
  if (!liveItem) return cartItem.price;
  const basePrice = Number(liveItem.price ?? cartItem.price ?? 0);
  const selectedIds = cartItem.selectedOptionValueIds;
  if (!Array.isArray(selectedIds) || selectedIds.length === 0) {
    return basePrice;
  }
  if (!Array.isArray(liveItem.itemOptions) || liveItem.itemOptions.length === 0) {
    return cartItem.price;
  }
  const extraPriceByValueId = new Map();
  liveItem.itemOptions.forEach((group) => {
    (group.values || group.optionValues || []).forEach((val) => {
      if (val?.id != null) {
        extraPriceByValueId.set(Number(val.id), Number(val.extraPrice || 0));
      }
    });
  });
  if (extraPriceByValueId.size === 0) return cartItem.price;
  const optionExtra = selectedIds.reduce(
    (sum, valId) => sum + (extraPriceByValueId.get(Number(valId)) || 0),
    0,
  );
  return basePrice + optionExtra;
};

const useCustomerCart = ({
  orderingUnavailable,
  orderPaymentLocked,
  restaurantSettings,
  menuItemsById = null,
  combosById = null,
  showError,
  loadCrossSellRecommendations,
  paymentLockedMessage,
}) => {
  const [rawCart, setRawCart] = useState(EMPTY_CART);
  const [selectedItemForOptions, setSelectedItemForOptions] = useState(null);

  const cart = useMemo(() => {
    const hasMenuCatalog = menuItemsById instanceof Map && menuItemsById.size > 0;
    const hasComboCatalog = combosById instanceof Map && combosById.size > 0;
    if (!hasMenuCatalog && !hasComboCatalog) {
      return rawCart;
    }

    const reconciledItems = {};
    Object.entries(rawCart.items).forEach(([cartId, item]) => {
      const lookupId = item.actualId ?? Number(cartId);
      const liveItem = hasMenuCatalog ? menuItemsById.get(lookupId) : null;
      if (hasMenuCatalog) {
        reconciledItems[cartId] = {
          ...item,
          name: liveItem?.name ?? item.name,
          price: resolveLiveItemPrice(item, liveItem),
          available: liveItem ? liveItem.available !== false : false,
        };
      } else {
        reconciledItems[cartId] = item;
      }
    });

    const reconciledCombos = {};
    Object.entries(rawCart.combos).forEach(([comboId, combo]) => {
      const lookupId = combo.actualId ?? Number(comboId);
      const liveCombo = hasComboCatalog ? combosById.get(lookupId) : null;
      if (hasComboCatalog) {
        reconciledCombos[comboId] = {
          ...combo,
          name: liveCombo?.name ?? combo.name,
          price: liveCombo?.price ?? combo.price,
          available: liveCombo ? liveCombo.available !== false : false,
        };
      } else {
        reconciledCombos[comboId] = combo;
      }
    });

    return { items: reconciledItems, combos: reconciledCombos };
  }, [rawCart, menuItemsById, combosById]);

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

      // H3 fix: capture qty/isCombo BEFORE setCart so we can call the side-effect
      // OUTSIDE the updater. Updater functions must be pure; async calls inside
      // them run twice in React StrictMode and risk stale-closure bugs.
      const shouldLoadCrossSell = !isCombo && qty === 1;
      setRawCart((current) => {
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
        }

        return { ...current, [group]: updatedGroup };
      });

      if (shouldLoadCrossSell) loadCrossSellRecommendations(product.id);
    },
    [assertCanOrder, loadCrossSellRecommendations],
  );

  const handleAddWithOptions = useCallback(
    (product, selectedValueIds, selectedOptionObjs, finalPrice) => {
      if (!assertCanOrder(product)) return;

      // H5 fix: sort IDs numerically before building the cartId key so that the
      // same combination of options always maps to the same cart slot regardless
      // of the order they were selected in the UI.
      const sortedValueIds = [...selectedValueIds].sort((a, b) => Number(a) - Number(b));
      const cartId = `${product.id}_${sortedValueIds.join('_')}`;

      setRawCart((current) => {
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
              selectedOptionValueIds: sortedValueIds,
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
    setRawCart((current) => {
      if (!current.items[cartId]) return current;
      const updatedItems = { ...current.items };
      if (qty <= 0) {
        delete updatedItems[cartId];
      } else {
        updatedItems[cartId] = { ...updatedItems[cartId], qty };
      }
      return { ...current, items: updatedItems };
    });
  }, []);

  const handleUpdateCartComboQty = useCallback((comboId, qty) => {
    setRawCart((current) => {
      if (!current.combos[comboId]) return current;
      const updatedCombos = { ...current.combos };
      if (qty <= 0) {
        delete updatedCombos[comboId];
      } else {
        updatedCombos[comboId] = { ...updatedCombos[comboId], qty };
      }
      return { ...current, combos: updatedCombos };
    });
  }, []);

  const handleUpdateNote = useCallback((id, note, isCombo = false) => {
    const safeNote = String(note ?? '').slice(0, 255);
    setRawCart((current) => {
      const group = isCombo ? 'combos' : 'items';
      if (!current[group][id]) return current;
      return {
        ...current,
        [group]: {
          ...current[group],
          [id]: { ...current[group][id], note: safeNote },
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

  const resetCart = useCallback(() => setRawCart(EMPTY_CART), []);

  return {
    cart,
    selectedItemForOptions,
    setSelectedItemForOptions,
    getCartItemQty,
    handleAddToCart,
    handleAddWithOptions,
    handleUpdateCartItemQty,
    handleUpdateCartComboQty,
    handleUpdateNote,
    calculateTotal: () => total,
    resetCart,
  };
};

export default useCustomerCart;
